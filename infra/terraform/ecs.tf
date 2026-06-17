resource "aws_ecs_cluster" "main" {
  name = local.name

  setting {
    name  = "containerInsights"
    value = "enabled"
  }
}

# --- IAM ---

# Execution role: used by the ECS agent to pull the image, write logs, and read the secrets that the
# task definition references.
resource "aws_iam_role" "execution" {
  name = "${local.name}-execution"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "ecs-tasks.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })
}

resource "aws_iam_role_policy_attachment" "execution" {
  role       = aws_iam_role.execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

# Allow the execution role to read exactly the three secrets this task needs.
resource "aws_iam_role_policy" "execution_secrets" {
  name = "${local.name}-execution-secrets"
  role = aws_iam_role.execution.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = ["secretsmanager:GetSecretValue"]
      Resource = [
        aws_secretsmanager_secret.db_password.arn,
        aws_secretsmanager_secret.jwt_secret.arn,
        aws_secretsmanager_secret.mongodb_uri.arn,
      ]
    }]
  })
}

# Task role: the identity the application itself runs as. It needs no AWS permissions here, but
# defining it explicitly is good practice (and the place to grant access if the app later calls AWS).
resource "aws_iam_role" "task" {
  name = "${local.name}-task"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "ecs-tasks.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })
}

# --- Task definition ---

resource "aws_ecs_task_definition" "app" {
  family                   = local.name
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.task_cpu
  memory                   = var.task_memory
  execution_role_arn       = aws_iam_role.execution.arn
  task_role_arn            = aws_iam_role.task.arn

  container_definitions = jsonencode([
    {
      name      = local.name
      image     = var.app_image
      essential = true

      portMappings = [
        { containerPort = var.app_port, protocol = "tcp" }
      ]

      # Non-secret configuration. The base application.yml reads each of these via environment
      # placeholders / Spring relaxed binding, so no special profile is needed on ECS.
      environment = [
        { name = "SPRING_DATASOURCE_URL", value = "jdbc:postgresql://${aws_db_instance.postgres.address}:5432/${var.db_name}" },
        { name = "SPRING_DATASOURCE_USERNAME", value = var.db_username },
        { name = "SPRING_DATA_REDIS_HOST", value = var.redis_host },
        { name = "SPRING_DATA_REDIS_PORT", value = tostring(var.redis_port) },
        { name = "KAFKA_BOOTSTRAP_SERVERS", value = var.kafka_bootstrap_servers },
        { name = "APP_BUDGET_MONTHLY_CATEGORY_LIMIT", value = "1000" },
      ]

      # Secrets are injected from Secrets Manager by ARN, never as plaintext.
      secrets = [
        { name = "SPRING_DATASOURCE_PASSWORD", valueFrom = aws_secretsmanager_secret.db_password.arn },
        { name = "JWT_SECRET", valueFrom = aws_secretsmanager_secret.jwt_secret.arn },
        { name = "SPRING_MONGODB_URI", valueFrom = aws_secretsmanager_secret.mongodb_uri.arn },
      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.app.name
          "awslogs-region"        = var.aws_region
          "awslogs-stream-prefix" = "app"
        }
      }
    }
  ])
}

# --- Service ---

resource "aws_ecs_service" "app" {
  name            = local.name
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.app.arn
  desired_count   = var.desired_count
  launch_type     = "FARGATE"

  network_configuration {
    subnets         = data.aws_subnets.default.ids
    security_groups = [aws_security_group.app.id]
    # Default subnets are public; a public IP lets tasks pull the image without a NAT gateway.
    assign_public_ip = true
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.app.arn
    container_name   = local.name
    container_port   = var.app_port
  }

  # Give the app generous time to register as healthy behind the ALB before the service health check
  # starts counting against it.
  health_check_grace_period_seconds = 120

  depends_on = [aws_lb_listener.http]
}
