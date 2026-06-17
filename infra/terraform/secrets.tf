# Sensitive values are stored in Secrets Manager and injected into the task definition by ARN, so
# they never appear in the task definition's plaintext environment or in CI logs.

resource "aws_secretsmanager_secret" "db_password" {
  name        = "${local.name}/db-password"
  description = "PostgreSQL master password for ${local.name}"
}

resource "aws_secretsmanager_secret_version" "db_password" {
  secret_id     = aws_secretsmanager_secret.db_password.id
  secret_string = var.db_password
}

resource "aws_secretsmanager_secret" "jwt_secret" {
  name        = "${local.name}/jwt-secret"
  description = "JWT signing secret for ${local.name}"
}

resource "aws_secretsmanager_secret_version" "jwt_secret" {
  secret_id     = aws_secretsmanager_secret.jwt_secret.id
  secret_string = var.jwt_secret
}

resource "aws_secretsmanager_secret" "mongodb_uri" {
  name        = "${local.name}/mongodb-uri"
  description = "MongoDB connection URI for ${local.name}"
}

resource "aws_secretsmanager_secret_version" "mongodb_uri" {
  secret_id     = aws_secretsmanager_secret.mongodb_uri.id
  secret_string = var.mongodb_uri
}
