# Bucket-Namen sind weltweit eindeutig. Die Konto-ID im Namen macht sie eindeutig, ohne Zufall.
data "aws_caller_identity" "current" {}

locals {
  buckets = {
    # Privat: hierhin legt das Deployment das JAR und seine Version, die Instanz holt beides vor jedem Start
    artifacts = "tictactest-artifacts-${data.aws_caller_identity.current.account_id}"
    # Öffentlich: das Frontend als Static Website
    frontend = "tictactest-frontend-${data.aws_caller_identity.current.account_id}"
  }
}

# Die beiden Buckets legt die AWS CLI an, nicht die Ressource aws_s3_bucket. aws_s3_bucket liest nach dem
# Erstellen und bei jedem plan die Object-Lock-Einstellung (s3:GetBucketObjectLockConfiguration), und genau
# das verbietet eine Service Control Policy des Learner Labs. Alle Einstellungen der Buckets verwaltet
# OpenTofu weiter selbst, mit den Ressourcen weiter unten. Neue Buckets sind ohne weitere Einstellung privat.
resource "terraform_data" "bucket" {
  for_each = local.buckets

  input = {
    name   = each.value
    region = var.region
  }

  provisioner "local-exec" {
    command = "aws s3 mb s3://${self.input.name} --region ${self.input.region}"
  }

  # Beim Löschen samt Inhalt, sonst könnte tofu destroy einen Bucket mit Dateien nicht entfernen
  provisioner "local-exec" {
    when    = destroy
    command = "aws s3 rb s3://${self.input.name} --region ${self.input.region} --force"
  }
}

resource "aws_s3_bucket_website_configuration" "frontend" {
  bucket = terraform_data.bucket["frontend"].output.name

  index_document {
    suffix = "index.html"
  }
}

# Öffentliche Bucket-Policies erlauben, öffentliche ACLs weiterhin sperren
resource "aws_s3_bucket_public_access_block" "frontend" {
  bucket = terraform_data.bucket["frontend"].output.name

  block_public_acls       = true
  ignore_public_acls      = true
  block_public_policy     = false
  restrict_public_buckets = false
}

# Jeder darf die Dateien lesen, niemand schreiben
resource "aws_s3_bucket_policy" "frontend" {
  bucket = terraform_data.bucket["frontend"].output.name

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Sid       = "PublicRead"
      Effect    = "Allow"
      Principal = "*"
      Action    = "s3:GetObject"
      Resource  = "arn:aws:s3:::${terraform_data.bucket["frontend"].output.name}/*"
    }]
  })

  depends_on = [aws_s3_bucket_public_access_block.frontend]
}

# Die Konfiguration des Frontends schreibt OpenTofu selbst, denn nur die Infrastruktur kennt die Adresse
# der Instanz. Das Frontend-Deployment lädt alle Dateien ausser config.js hoch.
resource "aws_s3_object" "frontend_config" {
  bucket        = terraform_data.bucket["frontend"].output.name
  key           = "config.js"
  content_type  = "application/javascript"
  cache_control = "no-store"

  content = <<-EOT
    // Von OpenTofu geschrieben (infra/buckets.tf). Nicht von Hand ändern und nicht mit dem Frontend deployen.
    window.TICTACTEST_CONFIG = {
      webConsoleUrl: "http://${aws_eip.web.public_ip}/"
    };
  EOT
}
