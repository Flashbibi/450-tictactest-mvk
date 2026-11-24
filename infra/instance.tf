# Amazon Linux 2023, die neueste AMI beim Erstellen der Instanz. Danach bleibt sie fest (ignore_changes
# unten), sonst würde jede neue AMI-Version die Instanz ersetzen. Die AMI legt auch die Paketversionen fest.
data "aws_ami" "al2023" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-2023.*-kernel-*-x86_64"]
  }
}

# Das vorhandene Instance Profile des Learner Labs. Eigene IAM-Rollen sind dort nicht erlaubt.
# Damit liest die Instanz den privaten Bucket und nimmt SSM-Befehle an, ohne Access Keys.
data "aws_iam_instance_profile" "lab" {
  name = "LabInstanceProfile"
}

resource "aws_security_group" "web" {
  name_prefix = "tictactest-"
  description = "TicTacTest: nur HTTP von aussen"

  ingress {
    description = "nginx: Terminal, WebSocket und /version.txt"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    description = "nur HTTPS: Pakete, ttyd von GitHub, S3 und SSM"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_instance" "web" {
  ami                    = data.aws_ami.al2023.id
  instance_type          = var.instance_type
  iam_instance_profile   = data.aws_iam_instance_profile.lab.name
  vpc_security_group_ids = [aws_security_group.web.id]

  # Einrichtung beim ersten Start: Java, nginx, ttyd und der Dienst. Keine Version des Spiels.
  user_data = templatefile("${path.module}/setup.sh.tftpl", {
    artifacts_bucket = terraform_data.bucket["artifacts"].output.name
    region           = var.region
    max_clients      = var.max_clients
    ttyd_version     = var.ttyd_version
    ttyd_sha256      = var.ttyd_sha256
  })

  # user_data läuft nur beim allerersten Start. Ändert sich die Einrichtung, braucht es deshalb eine
  # neue Instanz. Ohne diese Option würde OpenTofu die Instanz nur neu starten, und die Änderung käme nie an.
  user_data_replace_on_change = true

  tags = {
    Name = "tictactest"
  }

  lifecycle {
    ignore_changes = [ami]
  }
}

# Feste öffentliche Adresse. Sie bleibt beim Neustart der Instanz und auch bei einer neuen Instanz gleich.
resource "aws_eip" "web" {
  instance = aws_instance.web.id
  domain   = "vpc"

  tags = {
    Name = "tictactest"
  }
}
