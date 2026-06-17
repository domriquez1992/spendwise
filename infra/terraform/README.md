# Infrastructure (AWS, Terraform)

Infrastructure-as-Code for deploying Spendwise to **AWS ECS Fargate**, behind an Application Load
Balancer, with a managed **RDS PostgreSQL** database. This is a second, distinct deployment target
alongside the Kubernetes manifests in [`../../k8s`](../../k8s): ECS Fargate is serverless containers
rather than a cluster you operate.

## What CI checks vs. what you run

CI (the `terraform-validate` job) runs **`terraform fmt -check`**, **`terraform validate`**, and
**`tflint`** on every push. These are static checks — `validate` verifies the configuration against
the AWS provider schema **without contacting AWS**, so no credentials are involved.

Actually creating the infrastructure (`terraform apply`) provisions real, billable AWS resources, so
it is **run by you against your own AWS account**, not by CI. The steps are below.

## What it provisions

- An **ECS cluster** running the application as a **Fargate service** (2 tasks by default) behind an
  **Application Load Balancer**, with the ALB health check pointed at `/actuator/health`.
- An **RDS PostgreSQL 17** instance, reachable only from the application's security group.
- **Secrets Manager** entries for the database password, the JWT signing secret, and the MongoDB URI,
  injected into the task by ARN (never as plaintext environment values).
- A **CloudWatch Logs** group for the container logs, and least-privilege **IAM** roles for the task.

MongoDB, Redis, and Kafka are treated as **managed services you point at** (e.g. MongoDB Atlas,
ElastiCache, and MSK or Confluent Cloud) and supplied as variables, rather than self-hosted here.

## Deploy it (to your own AWS account)

Prerequisites: an AWS account, credentials configured (e.g. `aws configure` or environment
variables), and Terraform ≥ 1.6.

```bash
cd infra/terraform

# Provide the sensitive values (or export TF_VAR_db_password / TF_VAR_jwt_secret instead).
cp terraform.tfvars.example terraform.tfvars
# ...edit terraform.tfvars...

terraform init
terraform plan
terraform apply

# The public URL is printed as an output:
terraform output application_url
```

When you are done, remove everything to stop incurring charges:

```bash
terraform destroy
```

## Notes and trade-offs

- **Image source.** `app_image` defaults to the image CI publishes to GHCR
  (`ghcr.io/domriquez1992/spendwise:latest`). If that package is private, either make it public or
  add `repositoryCredentials` to the task definition pointing at a Secrets Manager secret holding a
  registry token. Alternatively, push the image to ECR and set `app_image` accordingly.
- **Networking.** For focus, this uses the account's **default VPC** and its subnets, placing tasks
  in public subnets with a public IP so they can pull the image without a NAT gateway. A production
  setup would provision a dedicated VPC with private subnets for the tasks and database and a NAT
  gateway for egress — a natural next iteration.
- **Cost.** An ALB and an RDS instance accrue charges while running; `db.t3.micro` is small and may
  be free-tier-eligible, but the ALB is not. Run `terraform destroy` when finished.
- **Hardening for production.** Enable RDS Multi-AZ, automated backups with retention, deletion
  protection and a final snapshot; add HTTPS (ACM certificate + an HTTPS listener) and restrict the
  ALB; and manage Terraform state in a remote backend (e.g. S3 + DynamoDB lock) instead of locally.
