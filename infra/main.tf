# TicTacTest in der Cloud: eine EC2-Instanz mit der Web-Konsole, das Frontend als S3-Website und ein
# privater Bucket für das JAR. OpenTofu stellt bereit, WOHIN und WIE deployt wird.
# WELCHE Version läuft, entscheidet das Deployment (docs/deployment.md).

terraform {
  required_version = ">= 1.8"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
  }
}

# Die Zugangsdaten kommen aus ~/.aws/credentials (Learner Lab), nie aus dem Code
provider "aws" {
  region = var.region

  default_tags {
    tags = {
      Project = "tictactest"
    }
  }
}
