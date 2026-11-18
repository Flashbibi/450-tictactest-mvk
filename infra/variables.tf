variable "region" {
  description = "AWS-Region. Das Learner Lab erlaubt nur us-east-1 und us-west-2"
  type        = string
  default     = "us-east-1"
}

variable "instance_type" {
  description = "Instanztyp. t2.micro hat 1 GB RAM, das reicht für rund 10 Spiele gleichzeitig"
  type        = string
  default     = "t2.micro"
}

variable "max_clients" {
  description = "Höchstens so viele Spiele gleichzeitig. Jedes Spiel ist eine eigene JVM mit rund 45 MB"
  type        = number
  default     = 10
}

variable "ttyd_version" {
  description = "Version von ttyd. ttyd ist nicht im Repository von Amazon Linux, es kommt vom GitHub-Release"
  type        = string
  default     = "1.7.7"
}

variable "ttyd_sha256" {
  description = "SHA-256 von ttyd.x86_64 dieser Version, aus der Datei SHA256SUMS des Releases"
  type        = string
  default     = "8a217c968aba172e0dbf3f34447218dc015bc4d5e59bf51db2f2cd12b7be4f55"
}
