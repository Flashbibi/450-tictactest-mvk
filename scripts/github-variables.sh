#!/usr/bin/env bash
# Überträgt die Outputs von OpenTofu als GitHub Variables. Die Pipeline kennt den State nicht, sie liest die Werte dort.
# Nach jedem tofu apply ausführen, das eine Adresse oder die Instanz ändert, zum Beispiel nach destroy und apply.
# Keiner der Werte ist geheim (siehe docs/deployment.md). Braucht OpenTofu und die GitHub CLI.
# Aufruf im Hauptordner: scripts/github-variables.sh
set -euo pipefail

repo=Flashbibi/450-tictactest-mvk

for output in region instance_id artifacts_bucket frontend_bucket frontend_url web_console_url version_url; do
  value=$(tofu -chdir=infra output -raw "$output")
  gh variable set "${output^^}" --repo "$repo" --body "$value"
  echo "${output^^} = $value"
done
