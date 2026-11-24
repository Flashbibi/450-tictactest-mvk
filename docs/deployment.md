# Deployment von Hand

M324 Webapp, Auftrag 2 · Vorlage für die Pipeline in Auftrag 3

Diese Anleitung bringt einen Release auf die Infrastruktur aus [`infra/`](../infra). OpenTofu stellt bereit, **wohin** deployt wird: Instanz, Dienst und Buckets. **Welche Version** läuft, bestimmt allein dieses Deployment. Für einen neuen Release braucht es deshalb weder `tofu apply` noch eine neue Instanz. Aufbau und Entscheidungen stehen in [`INFRASTRUKTUR.md`](../INFRASTRUKTUR.md).

Alle Befehle laufen in einer Bash im Hauptordner des Repositorys, unter Windows in der Git Bash.

## Voraussetzungen

- Werkzeuge: OpenTofu, AWS CLI v2, GitHub CLI `gh` (angemeldet), `curl` und `unzip`
- Das Learner Lab ist gestartet. Die Zugangsdaten aus *AWS Details → AWS CLI* stehen in `~/.aws/credentials`. Sie gelten nur für diese Session und kommen nie ins Repository.
- Prüfen: `aws sts get-caller-identity` zeigt ein Konto und die Rolle `voclabs`.

## 0. Infrastruktur erstellen

Nur beim ersten Mal und nach `tofu destroy`:

```bash
tofu -chdir=infra init
tofu -chdir=infra apply
```

`apply` legt die beiden Buckets mit der AWS CLI an, der Grund steht in [`INFRASTRUKTUR.md`](../INFRASTRUKTUR.md). Eine neue Instanz richtet sich beim ersten Start selbst ein (`infra/setup.sh.tftpl`). Das dauert einige Minuten. Fertig ist sie, wenn die Web-Konsole antwortet:

```bash
until curl -fs -o /dev/null "$(tofu -chdir=infra output -raw web_console_url)"; do sleep 10; done
```

## 1. Version wählen und Werte aus OpenTofu holen

```bash
VERSION=1.1.0                      # Version des Releases, ohne „v“

INSTANCE_ID=$(tofu -chdir=infra output -raw instance_id)
ARTIFACTS_BUCKET=$(tofu -chdir=infra output -raw artifacts_bucket)
FRONTEND_BUCKET=$(tofu -chdir=infra output -raw frontend_bucket)
export AWS_DEFAULT_REGION=$(tofu -chdir=infra output -raw region)
```

Die Outputs sind die Schnittstelle zwischen Infrastruktur und Deployment. Die Namen der Buckets enthalten die Konto-ID, darum stehen sie nirgends fest im Code.

## 2. Release herunterladen

```bash
WORK=$(mktemp -d)
gh release download "v$VERSION" --repo Flashbibi/450-tictactest-mvk --dir "$WORK"
ls "$WORK"                         # tictactest-<Version>.jar und tictactest-frontend-<Version>.zip
```

Deployt wird nur, was im Release liegt. Gebaut wird hier nichts.

## 3. Backend deployen

```bash
aws s3 cp "$WORK/tictactest-$VERSION.jar" "s3://$ARTIFACTS_BUCKET/backend/tictactest.jar"
echo "$VERSION" | aws s3 cp - "s3://$ARTIFACTS_BUCKET/backend/version.txt"

COMMAND_ID=$(aws ssm send-command \
  --instance-ids "$INSTANCE_ID" \
  --document-name AWS-RunShellScript \
  --parameters 'commands=["systemctl restart tictactest"]' \
  --query Command.CommandId --output text | tr -d '\r')
aws ssm wait command-executed --command-id "$COMMAND_ID" --instance-id "$INSTANCE_ID"
```

- JAR und Version kommen in den **privaten** Artefakt-Bucket, immer unter demselben Namen. Was dort liegt, ist die deployte Version.
- Der Dienst holt beides vor **jedem** Start aus dem Bucket (`ExecStartPre`). Der Neustart lädt also die neue Version. Auch nach einem Neustart der Instanz läuft so wieder genau diese Version.
- Den Neustart schickt **SSM** (AWS Systems Manager) an die Instanz, ohne SSH. `wait` wartet, bis der Befehl fertig ist, und bricht mit einem Fehler ab, wenn er fehlschlägt.
- `tr -d '\r'`: Unter Windows endet die Ausgabe der AWS CLI mit einem Wagenrücklauf. Ohne `tr` gehört er zur Command-ID, und `wait` findet den Befehl nicht.
- Laufende Spiele werden beim Neustart beendet.

## 4. Frontend deployen

```bash
unzip -q "$WORK/tictactest-frontend-$VERSION.zip" -d "$WORK/frontend"
aws s3 cp "$WORK/frontend/" "s3://$FRONTEND_BUCKET/" --recursive --exclude config.js --cache-control no-cache
```

- `--exclude config.js`: Die `config.js` im Archiv zeigt auf `localhost`. Die richtige schreibt OpenTofu mit der Adresse der Instanz in den Bucket. Das Deployment darf sie nicht überschreiben.
- `cp --recursive` statt `sync`: `sync` vergleicht nur Grösse und Datum. `version.txt` ist bei 1.1.0 und 1.2.0 gleich gross, bei einem Rollback auf eine ältere Version würde `sync` sie überspringen. `cp` lädt immer alle Dateien hoch.
- `--cache-control no-cache`: Der Browser fragt bei jedem Laden nach, ob es eine neue Version gibt.

## 5. Prüfen

```bash
curl -s "$(tofu -chdir=infra output -raw version_url)"                 # Backend: die neue Version
curl -s "$(tofu -chdir=infra output -raw frontend_url)/version.txt"    # Frontend: die neue Version
curl -s "$(tofu -chdir=infra output -raw frontend_url)/config.js"      # die Elastic IP, nicht localhost
tofu -chdir=infra plan                                                 # No changes.
```

Danach `frontend_url` im Browser öffnen und eine Partie spielen. Unten müssen Frontend und Backend die neue Version zeigen.

`tofu plan` muss „No changes“ melden: Das Deployment hat nur Dateien in Buckets geändert, die OpenTofu nicht verwaltet, und `config.js` nicht angefasst.

## 6. Aufräumen

```bash
rm -r "$WORK"
```

## Fehlersuche

Befehle auf der Instanz laufen ebenfalls über SSM. Die Ausgabe holt `get-command-invocation`:

```bash
COMMAND_ID=$(aws ssm send-command \
  --instance-ids "$INSTANCE_ID" \
  --document-name AWS-RunShellScript \
  --parameters 'commands=["systemctl status tictactest nginx --no-pager","journalctl -u tictactest -n 30 --no-pager","tail -n 30 /var/log/cloud-init-output.log"]' \
  --query Command.CommandId --output text | tr -d '\r')
aws ssm wait command-executed --command-id "$COMMAND_ID" --instance-id "$INSTANCE_ID"
aws ssm get-command-invocation --command-id "$COMMAND_ID" --instance-id "$INSTANCE_ID" \
  --query StandardOutputContent --output text
```

| Symptom | Ursache | Lösung |
|---|---|---|
| `curl` auf die Web-Konsole antwortet nicht | die Einrichtung läuft noch oder ist fehlgeschlagen | warten, sonst `/var/log/cloud-init-output.log` lesen |
| `version.txt` gibt 404 | noch nichts deployt, der Artefakt-Bucket ist leer | Schritte 2 und 3 |
| `send-command` meldet `InvalidInstanceId` | der SSM-Agent auf der Instanz hat sich noch nicht gemeldet | eine Minute warten |
| `ExpiredToken` | die Learner-Lab-Session ist abgelaufen | Lab starten, neue Zugangsdaten eintragen |

## SSM oder SSH für die Pipeline?

| | SSM (gewählt) | SSH |
|---|---|---|
| Offene Ports | keine. Der SSM-Agent baut die Verbindung von der Instanz aus auf | Port 22, für GitHub Actions praktisch für alle (die Adressen der Runner wechseln) |
| Geheimnisse in GitHub | nur die AWS-Zugangsdaten, die das Deployment für S3 sowieso braucht | zusätzlich ein privater SSH-Schlüssel |
| Berechtigung | über IAM, jeder Befehl wird in AWS protokolliert | wer den Schlüssel hat, kann alles, was der Benutzer darf |
| Bedienung | asynchron: Befehl schicken, warten, Ausgabe abholen | direkt, die Ausgabe kommt sofort |
| Voraussetzung | SSM-Agent (in Amazon Linux vorinstalliert) und Instance Profile | Schlüsselpaar und Benutzer auf der Instanz |

Im Learner Lab gelten die AWS-Zugangsdaten nur für eine Session. Die Pipeline in Auftrag 3 braucht sie trotzdem für S3, mit SSM kommt kein weiteres Geheimnis dazu.
