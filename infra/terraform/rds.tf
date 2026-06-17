resource "aws_db_subnet_group" "main" {
  name       = "${local.name}-db"
  subnet_ids = data.aws_subnets.default.ids
}

resource "aws_db_instance" "postgres" {
  identifier     = "${local.name}-postgres"
  engine         = "postgres"
  engine_version = "17"
  instance_class = var.db_instance_class

  allocated_storage = var.db_allocated_storage
  storage_type      = "gp3"
  storage_encrypted = true

  db_name  = var.db_name
  username = var.db_username
  password = var.db_password

  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.rds.id]

  # Demo-oriented settings. A production database would enable multi-AZ, automated backups with a
  # longer retention window, deletion protection, and a final snapshot.
  multi_az            = false
  publicly_accessible = false
  skip_final_snapshot = true
  apply_immediately   = true
}
