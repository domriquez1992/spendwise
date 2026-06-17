variable "aws_region" {
  description = "AWS region to deploy into."
  type        = string
  default     = "eu-north-1" # Stockholm
}

variable "project_name" {
  description = "Name prefix applied to all resources."
  type        = string
  default     = "spendwise"
}

variable "app_image" {
  description = "Container image for the application (e.g. the image published to GHCR by CI)."
  type        = string
  default     = "ghcr.io/domriquez1992/spendwise:latest"
}

variable "app_port" {
  description = "Port the application listens on."
  type        = number
  default     = 8080
}

variable "desired_count" {
  description = "Number of ECS task replicas to run."
  type        = number
  default     = 2
}

variable "task_cpu" {
  description = "Fargate task CPU units (1024 = 1 vCPU)."
  type        = number
  default     = 512
}

variable "task_memory" {
  description = "Fargate task memory in MiB."
  type        = number
  default     = 1024
}

# --- Database (RDS PostgreSQL) ---

variable "db_name" {
  description = "PostgreSQL database name."
  type        = string
  default     = "spendwise"
}

variable "db_username" {
  description = "PostgreSQL master username."
  type        = string
  default     = "spendwise"
}

variable "db_password" {
  description = "PostgreSQL master password. Provide via TF_VAR_db_password or a tfvars file; never commit it."
  type        = string
  sensitive   = true
}

variable "db_instance_class" {
  description = "RDS instance class."
  type        = string
  default     = "db.t3.micro"
}

variable "db_allocated_storage" {
  description = "RDS allocated storage in GiB."
  type        = number
  default     = 20
}

# --- Application secrets ---

variable "jwt_secret" {
  description = "JWT signing secret. Provide via TF_VAR_jwt_secret or a tfvars file; never commit it."
  type        = string
  sensitive   = true
}

# --- External managed datastores ---
# MongoDB, Redis, and Kafka are expected to be provided as managed services (e.g. MongoDB Atlas,
# ElastiCache, MSK or Confluent Cloud). Supply their connection details here; they are injected into
# the task as environment variables and secrets.

variable "mongodb_uri" {
  description = "MongoDB connection URI (e.g. a MongoDB Atlas SRV string)."
  type        = string
  default     = "mongodb://mongo:27017/spendwise"
  sensitive   = true
}

variable "redis_host" {
  description = "Redis host (e.g. an ElastiCache primary endpoint)."
  type        = string
  default     = "redis"
}

variable "redis_port" {
  description = "Redis port."
  type        = number
  default     = 6379
}

variable "kafka_bootstrap_servers" {
  description = "Kafka bootstrap servers (e.g. an MSK or Confluent Cloud endpoint)."
  type        = string
  default     = "kafka:9092"
}
