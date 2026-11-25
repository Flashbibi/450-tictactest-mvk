#!/usr/bin/env bash
# Überträgt die AWS-Zugangsdaten der aktuellen Learner-Lab-Session in die GitHub Secrets des Repositorys.
# Nach jedem "Start Lab" ausführen, sobald die neuen Zugangsdaten in ~/.aws/credentials stehen.
# Das Skript gibt keinen der Werte aus. Braucht die GitHub CLI, angemeldet als Besitzer des Repositorys.
# Aufruf im Hauptordner: scripts/github-secrets.sh
set -euo pipefail

repo=Flashbibi/450-tictactest-mvk
file=${AWS_SHARED_CREDENTIALS_FILE:-$HOME/.aws/credentials}

for name in aws_access_key_id aws_secret_access_key aws_session_token; do
  # der Wert aus dem Abschnitt [default], ohne Windows-Zeilenende
  value=$(sed -n "/^\[default\]/,/^\[/ s/^$name *= *//p" "$file" | tr -d '\r')
  if [ -z "$value" ]; then
    echo "$name fehlt in $file im Abschnitt [default]"
    exit 1
  fi
  # gh liest den Wert von der Standardeingabe, so steht er nie auf der Kommandozeile
  printf '%s' "$value" | gh secret set "${name^^}" --repo "$repo"
done

echo "AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY und AWS_SESSION_TOKEN gesetzt. Sie gelten bis zum Ende dieser Session."
