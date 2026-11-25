# Deployment

M324 Webapp, Aufträge 2 und 3 · Stand: 25.11.2026

OpenTofu stellt bereit, **wohin** deployt wird: Instanz, Dienst und Buckets (siehe [`INFRASTRUKTUR.md`](../INFRASTRUKTUR.md)). **Welche Version** läuft, bestimmt allein das Deployment. Ein neuer Release braucht deshalb weder `tofu apply` noch eine neue Instanz.

Seit Auftrag 3 deployt eine Pipeline in GitHub Actions jeden Release automatisch, und zwar nur die Teile, die sich geändert haben. Jede veröffentlichte Version lässt sich ausserdem von Hand deployen. Die Schritte der Pipeline sind dieselben wie beim Deployment von Hand aus Auftrag 2, das am Ende dieser Datei steht.

- [Automatisch bei jedem Release](#automatisch-bei-jedem-release)
- [Secrets-Konzept](#secrets-konzept)
- [Änderungen zwischen Releases erkennen](#änderungen-zwischen-releases-erkennen)
- [Backend deployen](#backend-deployen)
- [Frontend deployen](#frontend-deployen)
- [Gezielt eine Version deployen](#gezielt-eine-version-deployen)
- [Nachweis](#nachweis)
- [Reflexion und Ausblick: Umgebungen und Promotion](#reflexion-und-ausblick-umgebungen-und-promotion)
- [Deployment von Hand](#deployment-von-hand)

## Automatisch bei jedem Release

```
git tag v1.3.0 && git push origin v1.3.0
  └─ Workflow release (.github/workflows/release.yml)
       ├─ Job release:  Tests, JAR, Frontend-Archiv, GitHub-Release v1.3.0 (wie bisher)
       ├─ Job changes:  scripts/changes.sh vergleicht v1.3.0 mit dem vorherigen Release-Tag
       │                → backend=true|false, frontend=true|false
       └─ Job deploy:   nur wenn backend oder frontend true ist, ruft deploy.yml auf
            └─ Workflow deploy (.github/workflows/deploy.yml)
                 ├─ Variables und AWS-Zugangsdaten prüfen, Release v1.3.0 herunterladen
                 ├─ Backend:  Starttest des JARs → JAR in den Artefakt-Bucket → SSM: Dienst neu starten
                 │            → /version.txt meldet 1.3.0?
                 └─ Frontend: Archiv in den Frontend-Bucket, ohne config.js → alte Dateien löschen
                              → version.txt meldet 1.3.0, config.js zeigt noch auf die Instanz?

Von Hand: Actions → deploy → Run workflow → Version → deployt Backend und Frontend dieser Version
```

| Regel aus dem Auftrag | So ist sie umgesetzt |
|---|---|
| Deployt wird nur bei einem Release, ein Push auf `main` deployt nichts | Nur `release.yml` ruft das Deployment auf, und `release.yml` startet nur bei einem Tag `v*` |
| Ein Release deployt nur, was sich seit dem letzten Release geändert hat | Job `changes` mit [`scripts/changes.sh`](../scripts/changes.sh), siehe unten |
| Deployt werden die Artefakte aus dem Release, nicht neu gebaut | `deploy.yml` lädt JAR und Frontend-Archiv mit `gh release download`. Es checkt nicht einmal das Repository aus |
| Ein Deployment erstellt die Infrastruktur nicht neu | In der Pipeline gibt es kein `tofu`. `tofu apply` läuft weiterhin nur lokal, wenn sich `infra/` ändert |
| Jede veröffentlichte Version lässt sich erneut deployen | `deploy.yml` hat einen zweiten Auslöser, `workflow_dispatch` mit der Version |

`release.yml` ruft `deploy.yml` direkt als Job auf (`workflow_call`). Ein eigener Workflow, der auf das Event „Release veröffentlicht“ wartet, würde nie starten. GitHub löst keine weiteren Workflows aus, wenn das Event mit dem `GITHUB_TOKEN` eines anderen Workflows entsteht, und genau so erstellt `release.yml` den Release.

## Secrets-Konzept

### Welche Zugangsdaten braucht die Pipeline?

| Zugangsdaten | wofür | woher | gültig | wo abgelegt |
|---|---|---|---|---|
| `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_SESSION_TOKEN` | JAR und Frontend nach S3, Neustart des Dienstes über SSM | Learner Lab, *AWS Details → AWS CLI* | bis zum Ende der Learner-Lab-Session, höchstens 4 Stunden | **GitHub Secrets** |
| `GITHUB_TOKEN` | Release-Dateien herunterladen | GitHub erzeugt es für jeden Lauf selbst | bis der Job endet | nirgends, nur `contents: read` |

Mehr braucht es nicht. SSH-Schlüssel gibt es keinen, weil die Pipeline über SSM mit der Instanz spricht (siehe [SSM oder SSH](#ssm-oder-ssh-für-die-pipeline)). Die Release-Dateien sind auch ohne Token öffentlich, das `GITHUB_TOKEN` ist nur für die GitHub CLI.

### Secrets und Variables

| | GitHub Secrets | GitHub Variables |
|---|---|---|
| Speicherung | verschlüsselt. Nach dem Speichern kann sie niemand mehr anzeigen, auch der Besitzer nicht | als Klartext, sichtbar für alle mit Zugriff auf die Einstellungen |
| in Logs | GitHub ersetzt den Wert durch `***` | erscheint so, wie er ist |
| bei uns | die drei AWS-Zugangsdaten | die Outputs von OpenTofu: `REGION`, `INSTANCE_ID`, `ARTIFACTS_BUCKET`, `FRONTEND_BUCKET`, `FRONTEND_URL`, `WEB_CONSOLE_URL`, `VERSION_URL` |

Die Outputs sind nicht geheim (siehe `INFRASTRUKTUR.md`, Abschnitt Outputs). Darum gehören sie in Variables. Die Pipeline kennt den State von OpenTofu nicht, der liegt nur lokal. Sie liest die Werte deshalb aus den Variables. [`scripts/github-variables.sh`](../scripts/github-variables.sh) überträgt sie nach jedem `tofu apply`, das eine Adresse oder die Instanz ändert.

Zugangsdaten stehen nie im Repository, nie in einer Workflow-Datei und nie in einem Log. `deploy.yml` gibt sie nur den drei Schritten, die AWS brauchen, nicht dem ganzen Job. Die Antwort von `aws sts get-caller-identity` gibt der Workflow nicht aus, weil sie den Namen des Lab-Benutzers enthält.

### Learner Lab: neue Zugangsdaten nach jeder Session

1. Learner Lab starten, die Zugangsdaten wie bisher in `~/.aws/credentials` eintragen.
2. `scripts/github-secrets.sh` ausführen. Es liest die drei Werte aus `~/.aws/credentials` und setzt sie mit der GitHub CLI als Secrets. Ausgegeben wird keiner davon, und auf der Kommandozeile steht keiner, weil `gh` sie von der Standardeingabe liest.

**Release mit abgelaufenen Zugangsdaten:** Der Release selbst entsteht trotzdem, denn dafür braucht es kein AWS. Im Deployment bricht schon der erste AWS-Schritt „AWS-Zugangsdaten prüfen“ ab, mit `ExpiredToken` und einem Hinweis, was zu tun ist. Nichts wird deployt, die bisherige Version läuft weiter. Auf GitHub ist der Lauf rot, und GitHub schickt eine Mail.

**Nachdeployen:** Lab starten, `scripts/github-secrets.sh` ausführen, dann beim roten Lauf *Re-run jobs*. Ein neuer Lauf liest die Secrets neu. Alternativ die Version von Hand deployen (siehe unten).

### Wer kann die Secrets sehen oder benutzen?

- **Sehen:** niemand, auch nicht im Repository. Setzen und löschen kann nur, wer Admin des Repositorys ist.
- **Benutzen:** jeder Workflow in diesem Repository. Wer Schreibrechte hat, könnte einen Workflow schreiben, der die Secrets missbraucht. Schreibrechte hat nur der Besitzer.
- **Logs:** Das Repository ist öffentlich, also auch die Logs. GitHub ersetzt bekannte Secrets durch `***`, aber nicht, wenn ein Wert verändert ausgegeben wird, zum Beispiel Base64-kodiert. Darum gibt kein Schritt Zugangsdaten aus.
- **Forks und Pull Requests:** Workflows aus Pull Requests von Forks bekommen keine Secrets und nur ein `GITHUB_TOKEN` zum Lesen. Das Deployment läuft ohnehin nie bei einem Pull Request, nur bei einem Tag oder von Hand.

### Was wäre der Schaden, wenn ein Secret öffentlich würde?

Mit den drei AWS-Werten kann jeder im Learner-Lab-Konto handeln wie wir, bis die Session endet: die Infrastruktur löschen, Befehle auf der Instanz ausführen (SSM), die Buckets lesen und überschreiben, das Budget mit teuren Ressourcen aufbrauchen. Ein einzelner Wert allein nützt nichts, es braucht alle drei.

Reaktion: Session sofort beenden, die Secrets in GitHub löschen, im Konto nach fremden Ressourcen und Kosten schauen, die Lehrperson informieren. In einem echten Konto zusätzlich: die aktiven Sitzungen der Rolle widerrufen (IAM, *Revoke active sessions*), dauerhafte Schlüssel rotieren und in CloudTrail nachsehen, was mit den Zugangsdaten passiert ist.

### Wie man es ohne Learner Lab löst: OIDC

In einem echten Unternehmen speichert man keine AWS-Zugangsdaten in GitHub:

1. In AWS gibt es einen Identity Provider für `token.actions.githubusercontent.com` und eine IAM-Rolle für das Deployment.
2. Die Vertrauensrichtlinie der Rolle erlaubt nur Tokens dieses Repositorys, zum Beispiel nur für Tags `v*` oder nur für die Environment `prod`.
3. Der Workflow bekommt mit `permissions: id-token: write` von GitHub ein kurzlebiges OIDC-Token. `aws-actions/configure-aws-credentials` tauscht es gegen Zugangsdaten der Rolle, die nach rund einer Stunde ablaufen.

So gibt es kein Secret, das man rotieren muss oder das gestohlen werden kann. Im Learner Lab geht das nicht, weil wir dort weder Identity Provider noch Rollen anlegen dürfen.

## Änderungen zwischen Releases erkennen

[`scripts/changes.sh`](../scripts/changes.sh) vergleicht den neuen Tag mit dem vorherigen Release-Tag. Den vorherigen findet `git describe --tags --abbrev=0 --match 'v*' <Tag>^`. Damit das geht, checkt der Job `changes` die ganze Historie mit allen Tags aus (`fetch-depth: 0`). Danach entscheidet `git diff --name-only <vorher> <Tag>`:

| Pfad | zählt zu | Warum |
|---|---|---|
| `src/main/` | Backend | daraus entsteht das JAR |
| `build.gradle` | Backend, **ausser** die Zeile `version = …` | Build des JARs. Die Versionsnummer ändert sich bei jedem Release, sie allein ändert das Spiel nicht. Sonst würde jeder Release auch das Backend deployen |
| `settings.gradle`, `gradle.properties`, `gradle/`, `gradlew` | Backend | Build des JARs |
| `frontend/` | Frontend | Inhalt des Frontend-Archivs |
| `src/test/` | nichts | Tests ändern das JAR nicht |
| `*.md`, `docs/` | nichts | Dokumentation |
| `.github/workflows/` | nichts | Betrieb: Sie ändern, **wie** deployt wird, nicht **was**. Ändert sich, wie ein Artefakt gebaut wird, deployt man die Version von Hand |
| `infra/` | nichts | Infrastruktur, dafür `tofu apply` von Hand |
| `webkonsole/`, `.devcontainer/`, `scripts/` | nichts | lokale Entwicklung und Werkzeuge |

Weil die Versionsnummer allein nichts auslöst, können Frontend und Backend verschiedene Versionen zeigen: Nach einem Release, der nur das Frontend ändert, steht unten zum Beispiel „Frontend 1.2.1 · Backend 1.2.0“.

**Warum mit dem vorherigen Release-Tag und nicht mit dem letzten Commit?** Ein Release fasst alle Commits seit dem letzten Release zusammen. Ändert ein früherer Commit das Backend und der letzte nur die Doku, würde der Vergleich mit dem letzten Commit die Änderung am Backend übersehen. In der Umgebung läuft der vorherige Release, also ist genau der Unterschied zwischen den beiden Releases neu.

**Allererster Release:** Es gibt keinen vorherigen Tag, also wird beides deployt.

**Warum der `paths`-Filter von GitHub Actions nicht hilft:** Bei einem Push von Tags wertet GitHub `paths` gar nicht aus. Ausserdem vergleicht `paths` nur die Commits dieses einen Pushs, nicht die beiden Releases. Und er kann nur den ganzen Workflow starten oder auslassen, nicht pro Teil entscheiden.

Geprüft mit den bisherigen Releases:

| Release | vorheriger Release | geändert | Ergebnis |
|---|---|---|---|
| v1.0.0 | – | erster Release | Backend und Frontend |
| v1.0.1 | v1.0.0 | Tippfehler in `HumanPlayer.java` | nur Backend |
| v1.0.2 | v1.0.1 | Eingabe über eine Pipe in `HumanPlayer.java` | nur Backend |
| v1.1.0 | v1.0.2 | neues Frontend, in `build.gradle` nur die Version | nur Frontend |
| v1.2.0 | v1.1.0 | Link im Frontend, `infra/`, Doku | nur Frontend |

## Backend deployen

Wenn sich das Backend geändert hat:

1. **Starttest:** Das JAR aus dem Release bekommt eine leere Eingabe. Ein lauffähiges Spiel zeigt das Spielfeld und fragt nach dem ersten Zug. Fehlt diese Frage, bricht der Workflow ab, bevor das JAR auf die Instanz kommt.
2. **Deployen:** JAR und `version.txt` in den privaten Artefakt-Bucket, dann über SSM `systemctl restart tictactest`. Der Dienst holt beides vor dem Start (`ExecStartPre`). Die Instanz bleibt dieselbe.
3. **Versionsprüfung:** Die Pipeline fragt `VERSION_URL` alle 2 Sekunden ab. Meldet die Instanz nach 60 Sekunden nicht genau die neue Version, schlägt der Workflow fehl.

**Nie zwei Deployments gleichzeitig:** Der Job steht in der `concurrency`-Gruppe `deploy`. Ein zweites Deployment wartet, bis das erste fertig ist (`cancel-in-progress: false`). GitHub hält pro Gruppe höchstens ein wartendes Deployment. Kommt ein drittes dazu, ersetzt es das wartende.

**Was passiert mit Spielern, die gerade eine Partie spielen?** Der Neustart beendet ttyd und alle Java-Prozesse. Die Partie bricht ab, im Browser erscheint „Press ⏎ to Reconnect“, Enter startet ein neues Spiel mit der neuen Version. Verhindern könnte man das, umgesetzt ist es nicht:

- Vor dem Neustart prüfen, ob gerade jemand spielt (laufende Java-Prozesse), und warten, bis alle fertig sind.
- Die neue Version neben der alten starten (Blue-Green): Ein zweites ttyd auf einem anderen Port, nginx schickt nur neue Verbindungen dorthin, laufende Partien spielen auf der alten Version fertig.
- Deployments ankündigen oder in Zeiten legen, in denen niemand spielt.

**Woher weiss die Instanz nach einem Neustart im Learner Lab, welche Version sie starten soll?** Aus dem Artefakt-Bucket: Der Dienst holt vor jedem Start `tictactest.jar` und `version.txt`, und dort liegt immer die zuletzt deployte Version. Das hat Experiment 1 in Auftrag 2 gezeigt.

## Frontend deployen

Wenn sich das Frontend geändert hat:

1. Das Archiv aus dem Release entpacken.
2. `aws s3 cp --recursive … --exclude config.js` lädt alle Dateien hoch. Danach löscht `aws s3 sync --delete … --exclude config.js` alle Dateien, die es in dieser Version nicht mehr gibt. `sync` allein reicht nicht, weil es nur Grösse und Datum vergleicht: `version.txt` ist bei 1.3.0 und 1.2.0 gleich gross, und bei einem Rollback hätte die ältere Datei auch das ältere Datum.
3. Die Pipeline prüft, dass `FRONTEND_URL/version.txt` die neue Version liefert und `config.js` noch auf `WEB_CONSOLE_URL` zeigt.

**Was passiert, wenn das Frontend-Deployment die Konfiguration aus der Infrastruktur löscht?** Ohne `config.js` kennt das Frontend die Adresse der Web-Konsole nicht. Die Seite lädt, aber `app.js` bricht ab: kein Terminal, keine Versionen. Überschreibt das Deployment `config.js` mit der aus dem Archiv, zeigt sie auf `localhost`, und für Besucher bleibt das Terminal leer. So ist das verhindert:

- `--exclude config.js` bei `cp` und bei `sync`. Ausgeschlossene Dateien lädt `sync` weder hoch, noch löscht es sie.
- Die Prüfung in Schritt 3 macht den Workflow rot, falls `config.js` trotzdem fehlt oder falsch ist.
- `config.js` gehört OpenTofu. Ein `tofu apply` würde sie wiederherstellen, weil `tofu plan` jede Abweichung zeigt.

## Gezielt eine Version deployen

Auf GitHub unter *Actions → deploy → Run workflow* die Version eingeben, zum Beispiel `1.2.0`. Oder auf der Kommandozeile:

```bash
gh workflow run deploy --repo Flashbibi/450-tictactest-mvk -f version=1.2.0
```

Deployt werden Backend und Frontend dieser Version, mit denselben Prüfungen wie nach einem Release. Das braucht man in drei Fällen:

- **Rollback:** Läuft 1.3.0 und ist kaputt, deployt man 1.2.0. Danach zeigt das Frontend 1.2.0.
- **Kaputter Release:** Startet das JAR nicht, bricht der Starttest ab, bevor etwas deployt ist. Die bisherige Version läuft ohne Unterbruch weiter. Repariert wird mit einem neuen Release. Ist ein Fehler erst nach dem Deployment aufgefallen, deployt man die letzte funktionierende Version von Hand, ein Lauf dauert rund eine Minute.
- **Nach `tofu destroy` und `tofu apply`:** Die Buckets sind leer, und Instanz und Elastic IP sind neu. In GitHub ändern sich `INSTANCE_ID`, `WEB_CONSOLE_URL` und `VERSION_URL`, also `scripts/github-variables.sh` ausführen. Bucket-Namen und `FRONTEND_URL` bleiben dank der Konto-ID gleich. Danach die aktuelle Version von Hand deployen.

## Nachweis

Durchgeführt am 25.11.2026 im AWS Learner Lab. Die Links führen zu den Läufen auf GitHub Actions, Uhrzeiten in UTC.

### Die vier Fälle aus Teil 6

| Fall | Auslöser | Lauf | `changes` | deployt | danach Frontend · Backend |
|---|---|---|---|---|---|
| nur Frontend | Tag v1.2.1 | [release](https://github.com/Flashbibi/450-tictactest-mvk/actions/runs/45585324563) | backend=false, frontend=true | nur Frontend | 1.2.1 · 1.2.0 |
| nur Backend | Tag v1.2.2 | [release](https://github.com/Flashbibi/450-tictactest-mvk/actions/runs/45586431162) | backend=true, frontend=false | nur Backend | 1.2.1 · 1.2.2 |
| beides | Tag v1.3.0 | [release](https://github.com/Flashbibi/450-tictactest-mvk/actions/runs/45587892163) | backend=true, frontend=true | beides | 1.3.0 · 1.3.0 |
| Push auf main ohne Tag | Merge von PR #20 | [build](https://github.com/Flashbibi/450-tictactest-mvk/actions/runs/45584590514) | – | nichts: weder release noch deploy gestartet | unverändert |

Linus hat nach jedem Release im Browser geprüft: bei 1.2.1 und 1.2.2 die Versionen unten auf der Seite, die blaue Überschrift und X rot, O blau im Terminal, bei 1.3.0 die farbigen X und O in den Spielregeln und die grauen freien Felder. Alles stimmte.

Bei 1.2.1 sperrte zuerst der Webfilter des Netzwerks (FortiGuard) die Adresse der Web-Konsole. Die Version des Backends wurde darum nach diesem Release über SSM auf der Instanz gelesen. Danach lief alles über ein anderes Netz. Die Pipeline war nicht betroffen, sie prüft von GitHub aus.

### Tests aus Teil 5 und weitere Prüfungen

| Test | Lauf | Ergebnis |
|---|---|---|
| Rollback: 1.3.0 läuft, 1.2.0 von Hand deployt | [deploy](https://github.com/Flashbibi/450-tictactest-mvk/actions/runs/45588587504) | grün in 37 Sekunden. Frontend und Backend 1.2.0, im Browser schwarze Überschrift und keine Farben |
| zwei Deployments kurz hintereinander | [deploy](https://github.com/Flashbibi/450-tictactest-mvk/actions/runs/45588780420), [deploy](https://github.com/Flashbibi/450-tictactest-mvk/actions/runs/45588798013) | Der zweite Lauf war `pending`, solange der erste lief (09:47:44 bis 09:48:17), und lief danach (09:48:18 bis 09:48:40). Beide grün, danach überall 1.3.0 |
| kaputter Release v1.3.1 | [release](https://github.com/Flashbibi/450-tictactest-mvk/actions/runs/45589393361) | wie gewollt rot: `release` und `changes` grün (backend=true, frontend=false), im Job `deploy` scheiterte „Starttest des JARs“ mit „Error: Could not find or load main class ch.bbw.m450.tictactoe.Main“ und „Das JAR aus Release v1.3.1 startet nicht. Nichts wurde deployt, die bisherige Version läuft weiter“. Alle Schritte danach übersprungen, Frontend und Backend weiter 1.3.0 |
| Fix v1.3.2 | [release](https://github.com/Flashbibi/450-tictactest-mvk/actions/runs/45590147198) | grün, nur Backend deployt, danach Frontend 1.3.0 und Backend 1.3.2. Vom Start des Laufs nach dem Push des Tags bis zur grünen Versionsprüfung 66 Sekunden |
| ungültige Zugangsdaten, danach scripts/github-secrets.sh und Re-run | [deploy](https://github.com/Flashbibi/450-tictactest-mvk/actions/runs/45590721711) | Versuch 1 rot bei „AWS-Zugangsdaten prüfen“ (`InvalidClientTokenId`, „The security token included in the request is invalid“), alles danach übersprungen. Versuch 2 nach `scripts/github-secrets.sh` grün, Frontend und Backend 1.3.2. Getestet mit einem ungültigen Token, nicht mit einem echt abgelaufenen. Weil das Token zum Test „abgelaufen“ hiess, hat GitHub dieses Wort in der Fehlermeldung als `***` maskiert |
| nach tofu destroy und tofu apply | [deploy](https://github.com/Flashbibi/450-tictactest-mvk/actions/runs/45593612308) | 9 Ressourcen gelöscht und 9 neu erstellt. In den Variables änderten sich nur `INSTANCE_ID`, `WEB_CONSOLE_URL` und `VERSION_URL`. Danach grün, Frontend und Backend 1.3.2, im Browser gespielt, `tofu plan` meldet „No changes“ |
| Logs | – | in keinem Log von release und deploy Zugangsdaten (11 Logs durchsucht, 0 Treffer) |

## Reflexion und Ausblick: Umgebungen und Promotion

### Der Weg einer Änderung

```
Backend-Änderung (z.B. TicTacToeMain.java)
  Branch → Pull Request → build und coverage-gate grün → Merge auf main (deployt nichts)
  → Version und CHANGELOG (Pull Request) → Tag v1.x.y
  → release: Tests, JAR, Release → changes: backend=true → deploy: Starttest, S3, SSM-Neustart, /version.txt
  → Spieler bekommen beim nächsten Spiel die neue Version

Frontend-Änderung (z.B. style.css)
  Branch → Pull Request → build grün → Merge auf main (deployt nichts)
  → Version und CHANGELOG (Pull Request) → Tag v1.x.y
  → release: Frontend-Archiv, Release → changes: frontend=true → deploy: S3 ohne config.js, alte Dateien weg, prüfen
  → Spieler sehen die neue Seite beim nächsten Laden
```

| automatisch | noch von Hand |
|---|---|
| Tests, JAR, Frontend-Archiv, Release | den Tag setzen: ein Release ist ein bewusster Entscheid |
| erkennen, was sich geändert hat | `tofu apply`, wenn sich `infra/` ändert |
| Backend und Frontend deployen und prüfen | Learner Lab starten und `scripts/github-secrets.sh` nach jeder Session |
| | `scripts/github-variables.sh` nach einem `tofu apply`, das Adressen ändert |
| | Rollback: die Version wählen und starten |

### Mehrere Umgebungen

Damit dieselbe Infrastruktur dreimal entsteht (dev, staging, prod), bräuchte die OpenTofu-Konfiguration:

- eine Variable `environment`, die in jeden Namen und jedes Tag kommt, zum Beispiel `tictactest-dev-artifacts-<Konto-ID>`
- einen eigenen State pro Umgebung, zum Beispiel ein S3-Backend mit einem Schlüssel pro Umgebung oder Workspaces
- eine `.tfvars`-Datei pro Umgebung für das, was sich unterscheidet

| gleich in jeder Umgebung | pro Umgebung verschieden |
|---|---|
| die Konfiguration in `infra/` und damit der Aufbau | Namen, Instanz-ID und Adressen |
| Laufzeit und Dienst (`setup.sh.tftpl`) | Grösse, zum Beispiel `t2.micro` in dev und ein grösserer Typ in prod |
| der Ablauf des Deployments | Zugangsdaten, idealerweise ein eigenes AWS-Konto pro Umgebung |
| | wer deployen darf |

### Promotion-Strategie

```
Tag v1.4.0 → Release (einmal gebaut)
  → dev:      automatisch, sobald der Release steht
  → staging:  automatisch, wenn in dev Starttest und End-to-End-Test grün sind
  → prod:     Knopf mit Freigabe durch eine zweite Person (GitHub Environment prod mit Required Reviewers)
```

- **Dasselbe Artefakt weiterreichen, nicht neu bauen:** Nach prod kommt genau das JAR, das in dev und staging getestet wurde. Ein neuer Build könnte sich unterscheiden, zum Beispiel durch eine andere Version einer Abhängigkeit oder des Compilers, und wäre dann ungetestet. So machen wir es schon heute: deployt wird das JAR aus dem Release.
- **Was ein Deployment auslöst:** dev der Tag. staging ein grüner Test in dev. prod ein Mensch per Knopf. Ein Push auf `main` löst nirgends ein Deployment aus.
- **Prüfung vor der nächsten Umgebung:** der Starttest und die Versionsprüfung wie heute, dazu ein End-to-End-Test gegen die laufende Umgebung, zum Beispiel ein Playwright-Test, der im Browser über das Frontend eine Partie spielt.
- **Wer darf nach prod deployen?** Eine GitHub Environment `prod` mit *Required Reviewers*: Der Job wartet, bis eine berechtigte Person freigibt. Die Zugangsdaten für prod stehen nur in dieser Environment, andere Jobs bekommen sie nicht.
- **Änderungserkennung bei mehreren Umgebungen:** Unser Vergleich mit dem vorherigen Release-Tag setzt voraus, dass die Umgebung den vorherigen Release fährt. Hinkt prod drei Releases hinterher, wäre der Vergleich falsch. Man müsste mit der Version vergleichen, die in dieser Umgebung wirklich läuft, also mit ihrer `/version.txt`. Oder man deployt bei einer Promotion einfach immer beides.

## Deployment von Hand

Das Vorgehen aus Auftrag 2 und die Vorlage für die Pipeline. Alle Befehle laufen in einer Bash im Hauptordner des Repositorys, unter Windows in der Git Bash.

### Voraussetzungen

- Werkzeuge: OpenTofu, AWS CLI v2, GitHub CLI `gh` (angemeldet), `curl` und `unzip`
- Das Learner Lab ist gestartet. Die Zugangsdaten aus *AWS Details → AWS CLI* stehen in `~/.aws/credentials`. Sie gelten nur für diese Session und kommen nie ins Repository.
- Prüfen: `aws sts get-caller-identity` zeigt ein Konto und die Rolle `voclabs`.

### 0. Infrastruktur erstellen

Nur beim ersten Mal und nach `tofu destroy`:

```bash
tofu -chdir=infra init
tofu -chdir=infra apply
scripts/github-variables.sh        # für die Pipeline: Outputs als GitHub Variables
```

`apply` legt die beiden Buckets mit der AWS CLI an, der Grund steht in [`INFRASTRUKTUR.md`](../INFRASTRUKTUR.md). Eine neue Instanz richtet sich beim ersten Start selbst ein (`infra/setup.sh.tftpl`). Das dauert einige Minuten. Fertig ist sie, wenn die Web-Konsole antwortet:

```bash
until curl -fs -o /dev/null "$(tofu -chdir=infra output -raw web_console_url)"; do sleep 10; done
```

### 1. Version wählen und Werte aus OpenTofu holen

```bash
VERSION=1.1.0                      # Version des Releases, ohne „v“

INSTANCE_ID=$(tofu -chdir=infra output -raw instance_id)
ARTIFACTS_BUCKET=$(tofu -chdir=infra output -raw artifacts_bucket)
FRONTEND_BUCKET=$(tofu -chdir=infra output -raw frontend_bucket)
export AWS_DEFAULT_REGION=$(tofu -chdir=infra output -raw region)
```

Die Outputs sind die Schnittstelle zwischen Infrastruktur und Deployment. Die Namen der Buckets enthalten die Konto-ID, darum stehen sie nirgends fest im Code.

### 2. Release herunterladen

```bash
WORK=$(mktemp -d)
gh release download "v$VERSION" --repo Flashbibi/450-tictactest-mvk --dir "$WORK"
ls "$WORK"                         # tictactest-<Version>.jar und tictactest-frontend-<Version>.zip
```

Deployt wird nur, was im Release liegt. Gebaut wird hier nichts.

### 3. Backend deployen

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

### 4. Frontend deployen

```bash
unzip -q "$WORK/tictactest-frontend-$VERSION.zip" -d "$WORK/frontend"
aws s3 cp "$WORK/frontend/" "s3://$FRONTEND_BUCKET/" --recursive --exclude config.js --cache-control no-cache
aws s3 sync "$WORK/frontend/" "s3://$FRONTEND_BUCKET/" --delete --exclude config.js --cache-control no-cache
```

- `--exclude config.js`: Die `config.js` im Archiv zeigt auf `localhost`. Die richtige schreibt OpenTofu mit der Adresse der Instanz in den Bucket. Das Deployment darf sie weder überschreiben noch löschen.
- Zuerst `cp`, dann `sync --delete`: `cp` lädt immer alle Dateien hoch. `sync` allein vergleicht nur Grösse und Datum und würde bei einem Rollback die gleich grosse, aber ältere `version.txt` überspringen. Danach löscht `sync --delete` nur noch, was es in dieser Version nicht mehr gibt.
- `--cache-control no-cache`: Der Browser fragt bei jedem Laden nach, ob es eine neue Version gibt.

### 5. Prüfen

```bash
curl -s "$(tofu -chdir=infra output -raw version_url)"                 # Backend: die neue Version
curl -s "$(tofu -chdir=infra output -raw frontend_url)/version.txt"    # Frontend: die neue Version
curl -s "$(tofu -chdir=infra output -raw frontend_url)/config.js"      # die Elastic IP, nicht localhost
tofu -chdir=infra plan                                                 # No changes.
```

Danach `frontend_url` im Browser öffnen und eine Partie spielen. Unten müssen Frontend und Backend die neue Version zeigen.

`tofu plan` muss „No changes“ melden: Das Deployment hat nur Dateien in Buckets geändert, die OpenTofu nicht verwaltet, und `config.js` nicht angefasst.

### 6. Aufräumen

```bash
rm -r "$WORK"
```

### Fehlersuche

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
| `send-command` meldet `InvalidInstanceId` | der SSM-Agent auf der Instanz hat sich noch nicht gemeldet, oder die Instanz ist gestoppt | eine Minute warten, Learner Lab starten |
| `ExpiredToken` | die Learner-Lab-Session ist abgelaufen | Lab starten, neue Zugangsdaten eintragen, für die Pipeline `scripts/github-secrets.sh` |

### SSM oder SSH für die Pipeline?

| | SSM (gewählt) | SSH |
|---|---|---|
| Offene Ports | keine. Der SSM-Agent baut die Verbindung von der Instanz aus auf | Port 22, für GitHub Actions praktisch für alle (die Adressen der Runner wechseln) |
| Geheimnisse in GitHub | nur die AWS-Zugangsdaten, die das Deployment für S3 sowieso braucht | zusätzlich ein privater SSH-Schlüssel |
| Berechtigung | über IAM, jeder Befehl wird in AWS protokolliert | wer den Schlüssel hat, kann alles, was der Benutzer darf |
| Bedienung | asynchron: Befehl schicken, warten, Ausgabe abholen | direkt, die Ausgabe kommt sofort |
| Voraussetzung | SSM-Agent (in Amazon Linux vorinstalliert) und Instance Profile | Schlüsselpaar und Benutzer auf der Instanz |

Im Learner Lab gelten die AWS-Zugangsdaten nur für eine Session. Die Pipeline braucht sie trotzdem für S3, mit SSM kommt kein weiteres Geheimnis dazu.
