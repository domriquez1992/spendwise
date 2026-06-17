provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project   = var.project_name
      ManagedBy = "terraform"
    }
  }
}

# Use the account's default VPC and its subnets to keep this configuration focused on the deployment
# itself. A production setup would provision a dedicated VPC with public/private subnets and a NAT
# gateway (noted in the infra README as a next step).
data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}

locals {
  name = var.project_name
}
