output "application_url" {
  description = "Public URL of the application behind the load balancer."
  value       = "http://${aws_lb.main.dns_name}"
}

output "alb_dns_name" {
  description = "DNS name of the Application Load Balancer."
  value       = aws_lb.main.dns_name
}

output "ecs_cluster_name" {
  description = "Name of the ECS cluster."
  value       = aws_ecs_cluster.main.name
}

output "ecs_service_name" {
  description = "Name of the ECS service."
  value       = aws_ecs_service.app.name
}

output "rds_endpoint" {
  description = "Connection endpoint of the RDS PostgreSQL instance."
  value       = aws_db_instance.postgres.endpoint
}
