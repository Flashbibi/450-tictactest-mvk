# Zum Spielen

output "frontend_url" {
  description = "Hier spielt man: das Frontend als S3-Website"
  value       = "http://${aws_s3_bucket_website_configuration.frontend.website_endpoint}"
}

output "web_console_url" {
  description = "Die Web-Konsole direkt, ohne Frontend"
  value       = "http://${aws_eip.web.public_ip}/"
}

output "version_url" {
  description = "Version des laufenden Backends"
  value       = "http://${aws_eip.web.public_ip}/version.txt"
}

# Für das Deployment (docs/deployment.md) und später die Pipeline

output "region" {
  description = "Region aller Ressourcen"
  value       = var.region
}

output "instance_id" {
  description = "Instanz, an die das Deployment per SSM den Neustart des Dienstes schickt"
  value       = aws_instance.web.id
}

output "artifacts_bucket" {
  description = "Privater Bucket: hierhin kommen das JAR und seine Version"
  value       = terraform_data.bucket["artifacts"].output.name
}

output "frontend_bucket" {
  description = "Öffentlicher Bucket: hierhin kommt das Frontend, ausser config.js"
  value       = terraform_data.bucket["frontend"].output.name
}
