# Privater Bucket für das Backend. Das Deployment legt hier das JAR und seine Version ab, die Instanz
# holt beides vor jedem Start über das Instance Profile. Der Bucket ist nicht öffentlich.
resource "aws_s3_bucket" "artifacts" {
  bucket_prefix = "tictactest-artifacts-"
  force_destroy = true
}

# Öffentlicher Bucket für das Frontend, als Static Website
resource "aws_s3_bucket" "frontend" {
  bucket_prefix = "tictactest-frontend-"
  force_destroy = true
}

resource "aws_s3_bucket_website_configuration" "frontend" {
  bucket = aws_s3_bucket.frontend.id

  index_document {
    suffix = "index.html"
  }
}

# Öffentliche Bucket-Policies erlauben, öffentliche ACLs weiterhin sperren
resource "aws_s3_bucket_public_access_block" "frontend" {
  bucket = aws_s3_bucket.frontend.id

  block_public_acls       = true
  ignore_public_acls      = true
  block_public_policy     = false
  restrict_public_buckets = false
}

# Jeder darf die Dateien lesen, niemand schreiben
resource "aws_s3_bucket_policy" "frontend" {
  bucket = aws_s3_bucket.frontend.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Sid       = "PublicRead"
      Effect    = "Allow"
      Principal = "*"
      Action    = "s3:GetObject"
      Resource  = "${aws_s3_bucket.frontend.arn}/*"
    }]
  })

  depends_on = [aws_s3_bucket_public_access_block.frontend]
}

# Die Konfiguration des Frontends schreibt OpenTofu selbst, denn nur die Infrastruktur kennt die Adresse
# der Instanz. Das Frontend-Deployment lädt alle Dateien ausser config.js hoch.
resource "aws_s3_object" "frontend_config" {
  bucket        = aws_s3_bucket.frontend.id
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
