# Infrastruktur: TicTacTest auf AWS mit OpenTofu

M324 Webapp, Auftrag 2 · Stand: 24.11.2026

OpenTofu stellt bereit, **wohin und wie** deployt wird: Instanz, Laufzeitumgebung, Dienst, Netzwerk und Hosting fürs Frontend. **Welche Version** läuft, entscheidet allein das Deployment ([`docs/deployment.md`](docs/deployment.md)). Ein neuer Release braucht deshalb weder `tofu apply` noch eine neue Instanz.

## Aufbau

```
Browser
 ├─ http://<Frontend-Bucket>.s3-website-us-east-1.amazonaws.com      S3 Static Website: Frontend aus dem Release
 │    ├─ config.js ──► von OpenTofu geschrieben, enthält die Adresse der Instanz
 │    ├─ iframe    ──► http://<Elastic IP>/             nginx :80 ──► ttyd 127.0.0.1:7681 ──► java -jar tictactest.jar
 │    └─ fetch     ──► http://<Elastic IP>/version.txt  nginx, mit CORS-Header
 │
EC2-Instanz (Amazon Linux 2023, t2.micro, LabInstanceProfile)
 └─ Dienst tictactest: holt vor jedem Start tictactest.jar und version.txt aus dem privaten Artefakt-Bucket

Deployment (von Hand, in Auftrag 3 die Pipeline)
 ├─ JAR und Version ──► privater Artefakt-Bucket, dann per SSM: systemctl restart tictactest
 └─ Frontend-Archiv ──► öffentlicher Frontend-Bucket, ohne config.js
```

## Dateien

| Datei | Inhalt |
|---|---|
| [`infra/main.tf`](infra/main.tf) | Provider AWS, Region, Tag `Project=tictactest` für alle Ressourcen |
| [`infra/variables.tf`](infra/variables.tf) | Region, Instanztyp, höchstens gleichzeitige Spiele, Version und Prüfsumme von ttyd |
| [`infra/instance.tf`](infra/instance.tf) | AMI, Instance Profile, Security Group, Instanz, Elastic IP |
| [`infra/setup.sh.tftpl`](infra/setup.sh.tftpl) | Einrichtung der Instanz beim ersten Start (`user_data`) |
| [`infra/buckets.tf`](infra/buckets.tf) | privater Artefakt-Bucket und Frontend-Bucket (angelegt mit der AWS CLI, siehe „Buckets im Learner Lab“), Website-Einstellungen, `config.js` |
| [`infra/outputs.tf`](infra/outputs.tf) | Werte für Benutzer und Pipeline |
| `infra/.terraform.lock.hcl` | genaue Version und Prüfsummen des AWS-Providers. Gehört ins Repository, damit alle dieselbe Version verwenden |

Nicht ins Repository gehören der State (`terraform.tfstate`), der Ordner `.terraform/` mit dem heruntergeladenen Provider und die Zugangsdaten. `.gitignore` schliesst sie aus.

```bash
tofu -chdir=infra init       # Provider herunterladen, einmal pro Arbeitsplatz
tofu -chdir=infra plan       # zeigen, was sich ändern würde
tofu -chdir=infra apply      # Infrastruktur erstellen oder anpassen
tofu -chdir=infra output     # Adressen und Namen anzeigen
```

OpenTofu braucht dafür auch die AWS CLI, denn sie legt die beiden Buckets an.

## Artefakt-Bezug: privater S3-Bucket

Das Deployment kopiert das JAR eines Releases in einen privaten Bucket. Die Instanz holt es dort mit dem Instance Profile.

| Variante | Repository privat | Version nach einem Neustart | Version wechseln |
|---|---|---|---|
| Instanz lädt vom GitHub-Release | Die Instanz bräuchte ein GitHub-Token. Es stünde in `user_data`, und das kann jeder lesen, der die Instanz in der Konsole sieht | Die Versionsnummer müsste irgendwo auf der Instanz stehen | Versionsnummer auf der Instanz ändern und neu herunterladen |
| **privater S3-Bucket (gewählt)** | kein Problem: Das Token braucht nur das Deployment, die Instanz spricht nur mit S3 | Im Bucket liegt immer die deployte Version. Der Dienst holt sie vor jedem Start, auch auf einer neuen Instanz | JAR in den Bucket, Dienst neu starten |
| Kopie per SSH | kein Problem | Liegt auf der Platte. Eine neue Instanz ist leer | Port 22 offen, Schlüssel in der Pipeline |

Ein JAR mit rund 7 KB kostet in S3 praktisch nichts.

**Warum das JAR nicht in `user_data` herunterladen?** `user_data` läuft nur beim allerersten Start einer Instanz. Eine Version, die dort steht, wäre eingefroren. Für eine neue Version müsste man `user_data` ändern, also die Infrastruktur, und genau das darf ein Release nicht brauchen. Darum steht in [`setup.sh.tftpl`](infra/setup.sh.tftpl) keine Version, nur die Laufzeitumgebung und der Dienst.

**`user_data_replace_on_change = true`:** Ändert sich die Einrichtung, zum Beispiel die Unit des Dienstes, ersetzt OpenTofu die Instanz. Ohne die Option würde OpenTofu nur `user_data` austauschen und die Instanz neu starten. cloud-init führt das Skript aber nicht noch einmal aus: Die Änderung käme nie an, und Konfiguration und Instanz würden still auseinanderlaufen. Dank Elastic IP und Artefakt-Bucket bleiben beim Ersetzen Adresse und Version gleich (Experiment 2 unten).

## Zugriff ohne Access Keys: Instance Profile

Eigene IAM-Rollen sind im Learner Lab nicht erlaubt. Die Instanz bekommt deshalb das vorhandene `LabInstanceProfile` mit der Rolle `LabRole`, gelesen über `data "aws_iam_instance_profile"`. EC2 stellt der Instanz damit kurzlebige Zugangsdaten über den Metadata-Dienst bereit. Die AWS CLI auf der Instanz findet sie von selbst. Es liegen keine Access Keys auf der Instanz, auch nicht in `user_data`.

Gebraucht wird das Instance Profile für zwei Dinge: Der Dienst liest den Artefakt-Bucket, und der SSM-Agent nimmt den Neustart-Befehl des Deployments an.

## Laufzeitumgebung

| Was | Woher | Version fest durch |
|---|---|---|
| Java 25 (Amazon Corretto) | `dnf install java-25-amazon-corretto-headless` | Der Paketname legt Java 25 fest. Amazon Linux 2023 friert die Paket-Repositories pro AMI-Version ein, die Patch-Version hängt also an der AMI. dnf prüft die Signatur jedes Pakets |
| nginx | `dnf install nginx` | ebenfalls die AMI-Version |
| ttyd | GitHub-Release, nicht im Repository von Amazon Linux | Variable `ttyd_version` (1.7.7) und `ttyd_sha256`. `sha256sum -c` bricht die Einrichtung ab, wenn die Datei nicht genau stimmt |

Die AMI ist die neueste Amazon Linux 2023 beim Erstellen der Instanz. `ignore_changes = [ami]` verhindert, dass eine neue AMI-Version die laufende Instanz ersetzt. Eine neue Instanz (nach einer Änderung der Einrichtung oder `destroy`) bekommt die dann neueste AMI und damit allenfalls einen neueren Patch von Java 25. Soll auch das fest sein, setzt man die AMI-ID als Variable.

`-headless` lässt Grafik und Sound weg, die ein Konsolenspiel nicht braucht.

## Dienst

`setup.sh.tftpl` schreibt die Unit `/etc/systemd/system/tictactest.service`:

| Zeile | Wozu |
|---|---|
| `User=tictactest` | eigener Systembenutzer ohne Login, nicht root |
| `Environment=AWS_DEFAULT_REGION=…` | Region für die AWS CLI, aus der Variable `region` |
| `ExecStartPre=/usr/bin/aws s3 cp --recursive s3://<Artefakt-Bucket>/backend/ /opt/tictactest/` | holt vor jedem Start die deployte Version: `tictactest.jar` und `version.txt` |
| `ExecStart=/usr/local/bin/ttyd … java -Xmx32m -jar /opt/tictactest/tictactest.jar` | dieselben Optionen wie lokal (siehe [`WEBKONSOLE.md`](WEBKONSOLE.md)). ttyd startet nur das JAR, nie eine Shell |
| `Restart=always`, `RestartSec=5` | stürzt ttyd ab, startet systemd es nach 5 Sekunden neu |
| `WantedBy=multi-user.target` | `systemctl enable` startet den Dienst bei jedem Booten |

nginx läuft als normaler Systemdienst auf Port 80, gleich konfiguriert wie lokal: `/version.txt` aus `/opt/tictactest` mit CORS-Header, alles andere an ttyd. Das JAR selbst liefert nginx nicht aus.

## Security Group

| Richtung | Port | Gegenstelle | Begründung |
|---|---|---|---|
| eingehend | 80/tcp | alle (0.0.0.0/0) | nginx: Terminal mit WebSocket und `/version.txt`. Öffentlich, weil jeder spielen darf |
| ausgehend | 443/tcp | alle (0.0.0.0/0) | Pakete mit dnf, ttyd von GitHub, S3 und SSM laufen alle über HTTPS |

Bewusst nicht offen:

- **22 (SSH):** Deployt und geprüft wird über SSM. Der SSM-Agent baut die Verbindung von der Instanz aus auf, über Port 443.
- **7681 (ttyd):** ttyd hört nur auf `127.0.0.1`, von aussen geht alles über nginx.
- **443 eingehend (HTTPS):** Dafür bräuchte es ein Zertifikat für eine eigene Domain, und die gibt es im Learner Lab nicht. Auch die S3-Website kann nur HTTP. Darum läuft hier alles über HTTP, und auf Port 443 hört nichts.
- **Alle anderen ausgehenden Ports:** DNS, Zeit und Metadaten-Dienst des VPC filtert eine Security Group nicht, dafür braucht es keine Regel.

## Buckets im Learner Lab: angelegt mit der AWS CLI

Üblich wäre die Ressource `aws_s3_bucket` mit `bucket_prefix` für einen eindeutigen Namen und `force_destroy = true`. Im Learner Lab scheitert sie: Nach dem Erstellen und bei jedem `plan` liest `aws_s3_bucket` die Object-Lock-Einstellung des Buckets (`s3:GetBucketObjectLockConfiguration`). Genau das verbietet eine Service Control Policy der Organisation des Learner Labs. `tofu apply` bricht deshalb mit `AccessDenied … with an explicit deny in a service control policy` ab, obwohl der Bucket schon angelegt ist. Im eigenen Konto lässt sich das nicht ändern.

Darum legt die AWS CLI die beiden Buckets an:

- `terraform_data.bucket` führt beim Erstellen `aws s3 mb` aus und beim Löschen `aws s3 rb --force`, das den Bucket samt Inhalt entfernt wie `force_destroy`.
- Alle Einstellungen verwaltet OpenTofu weiter selbst, mit eigenen Ressourcen: Website, Public Access Block, Bucket-Policy und `config.js`. Diese Ressourcen lesen die Object-Lock-Einstellung nicht.
- Die Namen sind `tictactest-artifacts-<Konto-ID>` und `tictactest-frontend-<Konto-ID>`. `bucket_prefix` gibt es nur bei `aws_s3_bucket`. Die Konto-ID macht die Namen weltweit eindeutig, ohne Zufall. Sie ist laut AWS kein Geheimnis.

Was das bedeutet:

- Nach `tofu destroy` und `tofu apply` heissen die Buckets wieder gleich, die `frontend_url` bleibt also dieselbe.
- OpenTofu verwaltet die Einstellungen der Buckets, aber nicht die Buckets selbst. Löscht jemand einen Bucket von Hand, sieht `tofu plan` das nicht.
- Wo `tofu apply` läuft, braucht es die AWS CLI. Für das Deployment braucht es sie sowieso.

## Frontend

- Der Bucket heisst `tictactest-frontend-<Konto-ID>`, siehe oben.
- `aws_s3_bucket_website_configuration` macht ihn zur Static Website mit `index.html`.
- Der Public Access Block erlaubt öffentliche Bucket-Policies, öffentliche ACLs bleiben gesperrt. Die Bucket-Policy erlaubt allen nur `s3:GetObject`, also lesen.
- **`config.js`** schreibt OpenTofu selbst als `aws_s3_object`, mit der Elastic IP. Nur die Infrastruktur kennt diese Adresse, im Code steht sie nirgends.
- Damit ein Deployment sie nicht überschreibt, lädt es das Frontend-Archiv mit `--exclude config.js` hoch. Würde jemand sie trotzdem ändern, zeigt `tofu plan` das als Abweichung, und `tofu apply` stellt sie wieder her.

**Was nie in diesen Bucket darf:** alles, was nicht jeder sehen darf. Was dort liegt, kann die ganze Welt lesen. Also keine Zugangsdaten und Tokens, kein State, keine `.tfvars`, keine Konfiguration mit Passwörtern. Auch `config.js` darf deshalb nur öffentliche Werte enthalten, hier nur die Adresse der Web-Konsole. Das JAR liegt im privaten Artefakt-Bucket, weil das Repository privat sein könnte.

## Wie viele Spiele gleichzeitig?

Jedes Spiel ist eine eigene JVM mit rund 45 MB (RSS, gemessen in Auftrag 1). Eine t2.micro hat 1 GB Arbeitsspeicher. Betriebssystem, SSM-Agent, nginx und ttyd brauchen geschätzt 200 bis 250 MB. Es bleiben rund 750 MB, das reicht für etwa 16 Spiele. `max_clients = 10` lässt Reserve. Der elfte Besucher bekommt keine Verbindung, statt dass der Instanz der Speicher ausgeht.

Gemessen auf der Instanz am 24.11.2026: ohne Spiel 656 MB verfügbar (`available` von `free -m`, nach der Einrichtung), eine laufende JVM mit einem offenen Spiel 42 MB RSS.

t2.micro statt t3.micro: Beide haben 1 GB. t2 läuft standardmässig mit festen CPU-Credits, t3 im Modus *unlimited*, der bei Dauerlast zusätzlich kostet.

## Outputs

| Output | Für | Vertraulich? |
|---|---|---|
| `frontend_url` | Spielerinnen und Spieler | nein, öffentlich |
| `web_console_url` | direkt zum Terminal, für Tests | nein, öffentlich |
| `version_url` | Prüfen nach dem Deployment | nein, öffentlich |
| `region` | Deployment | nein |
| `instance_id` | Deployment: SSM-Befehl an die Instanz | nein. Intern, aber ohne Zugangsdaten nutzlos |
| `artifacts_bucket` | Deployment: hierhin kommt das JAR | nein. Der Name enthält nur die Konto-ID, und der Bucket ist privat |
| `frontend_bucket` | Deployment: hierhin kommt das Frontend | nein, er steht ohnehin in der öffentlichen Adresse |

Keiner der Outputs ist ein Geheimnis, keiner braucht `sensitive = true`. Vertraulich sind dagegen:

- die **AWS-Zugangsdaten** in `~/.aws/credentials`. Im Learner Lab gelten sie nur für eine Session. Sie kommen nie ins Repository und nie auf die Instanz.
- der **State** `terraform.tfstate`. Er enthält alle Details der Ressourcen, in anderen Projekten oft auch Passwörter. Er liegt nur lokal und nie im Repository. Für die Pipeline in Auftrag 3 muss er an einen gemeinsamen Ort, zum Beispiel in einen S3-Bucket als Backend.

## Kosten im Learner Lab

| Ressource | Preis in us-east-1 | Wann |
|---|---|---|
| EC2 t2.micro | rund 0.012 USD pro Stunde | nur solange die Instanz läuft |
| Elastic IP | 0.005 USD pro Stunde | immer, auch wenn die Instanz gestoppt ist |
| EBS-Volume 8 GB | rund 0.64 USD pro Monat | immer |
| S3 | wenige Cent | praktisch nichts |

Am Ende einer Session stoppt das Learner Lab die Instanz. Was dann noch kostet, sind Elastic IP und Volume, zusammen rund 4 USD im Monat. Wird die Infrastruktur länger nicht gebraucht: `tofu -chdir=infra destroy`. Neu aufbauen und deployen dauert danach rund 10 Minuten.

## Protokoll

Durchgeführt am 24.11.2026 im AWS Learner Lab, Region us-east-1.

### Einrichtung

| Was | Beobachtung |
|---|---|
| Erster `apply` mit `aws_s3_bucket` | abgebrochen: Die Service Control Policy des Learner Labs verbietet `s3:GetBucketObjectLockConfiguration` („is not authorized to perform: s3:GetBucketObjectLockConfiguration on resource: "arn:aws:s3:::tictactest-artifacts-…" with an explicit deny in a service control policy“, ebenso beim Frontend-Bucket). Die Security Group und die zwei leeren Buckets waren schon angelegt. Lösung: Buckets mit der AWS CLI, siehe „Buckets im Learner Lab“. Die zwei leeren Buckets wurden gelöscht, die Security Group weiterverwendet |
| `tofu apply` | 9 Ressourcen: die Security Group im ersten Versuch (5 s), die übrigen 8 im zweiten Versuch in 28 s. Die Bucket-Policy ging ohne `AccessDenied` durch |
| Erster Start bis die Web-Konsole antwortet | rund 1 Minute |
| Installierte Versionen | Java 25.0.5.1 (`java-25-amazon-corretto-headless-25.0.5.1.2-1.amzn2023.1`), nginx 1.30.7 (`nginx-1.30.7-1.amzn2023.0.1`), ttyd 1.7.7 |
| Dienst | `enabled`, ttyd läuft als `tictactest`, nginx-Worker als `nginx`, nginx-Master als `root`. cloud-init `status: done`, SSM `Online` |
| Port 7681 von aussen | nicht erreichbar |
| Absturz (`systemctl kill -s KILL tictactest`) | nach 5,2 Sekunden neu gestartet und nach 5,9 Sekunden wieder aktiv, `NRestarts=1`, Version weiter 1.1.0 |

### Deployments

| Release | `tofu apply` | Instanz | Backend | Frontend | Im Browser gespielt | `tofu plan` danach |
|---|---|---|---|---|---|---|
| A: 1.1.0 | nur einmal, für die Infrastruktur | `i-0fc896785881f5d84` | 1.1.0 | 1.1.0 | ja | No changes |
| B: 1.2.0 | nein | dieselbe (`i-0fc896785881f5d84`) | 1.2.0 | 1.2.0, mit Link | ja, der Link öffnet das Terminal | No changes |

Ein Deployment nach [`docs/deployment.md`](docs/deployment.md) dauerte 27 Sekunden (1.1.0) und 25 Sekunden (1.2.0). `tofu plan` meldete beide Male wörtlich: „No changes. Your infrastructure matches the configuration.“

### Experiment 1: Learner-Lab-Session beenden und neu starten

| Frage | Beobachtung |
|---|---|
| Läuft die App wieder, ohne dass man etwas tut? | ja. Das Learner Lab hat die Instanz beim Beenden gestoppt und bei „Start Lab“ von selbst wieder gestartet (`LaunchTime` 12:34:02 → 13:10:17 UTC). `start-instances` war nicht nötig. Der Dienst war 15 Sekunden nach dem Start aktiv und hat vorher `tictactest.jar` und `version.txt` aus dem Artefakt-Bucket geholt (`download: s3://…/backend/tictactest.jar`), `NRestarts=0` |
| Ist die URL gleich geblieben? | ja, `http://32.201.240.253/` (Elastic IP) |
| Läuft dieselbe Version wie vorher? | ja, 1.2.0, ohne neues Deployment |
| `tofu plan` danach | No changes |

### Experiment 2: Einrichtung ändern (`max_clients` 10 → 8)

| Frage | Beobachtung |
|---|---|
| Was zeigt `tofu plan`? | `Plan: 1 to add, 1 to change, 1 to destroy.` `aws_instance.web` must be replaced, weil sich `user_data` ändert (`# forces replacement`): in der Unit `--max-clients 10` → `--max-clients 8`. `aws_eip.web` wird nur angepasst (update in-place, neue Instanz) |
| Was passiert mit der Instanz? | Sie wird ersetzt: `i-0fc896785881f5d84` gelöscht (31 s), `i-0f7a07e53237859e7` neu erstellt (15 s) und eingerichtet |
| Und mit der IP? | bleibt gleich, `32.201.240.253`. Die Elastic IP wird nur der neuen Instanz zugeordnet |
| Und mit der laufenden Version? | bleibt 1.2.0, ohne Deployment. Der Dienst holt das JAR beim ersten Start aus dem Artefakt-Bucket |
| Wie lange war die Web-Konsole weg? | rund 2 Minuten (Prüfung alle 5 Sekunden: weg ab 14:14:23, wieder da um 14:16:26) |

Danach zurück auf `max_clients = 10`: wieder 1 to add, 1 to change, 1 to destroy, neue Instanz `i-0caaadc72dd2d8e85`, Version 1.2.0, `tofu plan` danach No changes.

### Experiment 3: `tofu destroy` und danach `tofu apply`

| Wert | vorher | nachher |
|---|---|---|
| `frontend_url` | `http://tictactest-frontend-827848230032.s3-website-us-east-1.amazonaws.com` | gleich |
| `web_console_url` | `http://32.201.240.253/` | `http://32.194.131.21/` |
| `instance_id` | `i-0caaadc72dd2d8e85` | `i-04243f6a918a83990` |
| `artifacts_bucket` | `tictactest-artifacts-827848230032` | gleich |
| `frontend_bucket` | `tictactest-frontend-827848230032` | gleich |

`destroy` löschte alle 9 Ressourcen, die Buckets samt Inhalt (`delete: …` und `remove_bucket: …` von `aws s3 rb --force`). Nach 2 Minuten Wartezeit legte `apply` die 9 Ressourcen wieder an, `make_bucket` ging beim ersten Versuch durch. Die Bucket-Namen und damit die `frontend_url` sind gleich geblieben, neu sind nur die Instanz und die Elastic IP.

Was nötig war, bis wieder dieselbe Version lief: Die neue Instanz richtete sich selbst ein, die Web-Konsole antwortete rund 45 Sekunden nach dem `apply`. Ohne Deployment gaben `version.txt` und das Frontend aber 404, weil beide Buckets leer waren. Erst ein neues Deployment von 1.2.0 nach `docs/deployment.md` (24 Sekunden) brachte alles zurück, mit `config.js` auf die neue IP. Von `destroy` bis wieder 1.2.0 lief: 4 Minuten 46 Sekunden, davon 2 Minuten Wartezeit.

### GitHub Actions gesperrt

| Was | Beobachtung |
|---|---|
| Checks für PR #19 | keine. GitHub hat Actions für das Repository gesperrt, im Actions-Tab steht „GitHub Actions is currently disabled for this repository. Please reach out to GitHub Support for assistance.“ Der letzte Lauf war am 17.11.2026 |
| Ersatz für die Checks | lokal am 24.11.2026: `./gradlew clean check` BUILD SUCCESSFUL, Instruction Coverage main 97.6 %, Branch 97.6 % |
| Release 1.2.0 | ohne Actions erstellt, mit denselben Schritten wie `release.yml`, vom Commit `e0b32fc` auf `infrastruktur`. PR #19 bleibt offen, bis Actions wieder läuft |
