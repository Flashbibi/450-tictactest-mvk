# TicTacTest

TicTacToe in der Konsole: Du spielst X, der Computer O. Projekt der Module M450 und M324 an der BBW. Das Spiel läuft unverändert als JAR. Eine Web-Konsole macht es im Browser spielbar.

## Web-App

**Spielen:** <http://tictactest-frontend-827848230032.s3-website-us-east-1.amazonaws.com>

Ins Terminal klicken, die Nummer eines freien Feldes (0 bis 8) tippen und Enter drücken. Unten auf der Seite stehen die Versionen von Frontend und Backend.

- Die Web-App läuft im AWS Learner Lab. Ausserhalb einer Lab-Session ist die EC2-Instanz gestoppt: Die Seite lädt, aber das Terminal bleibt leer, und das Backend ist „nicht erreichbar“. Nach dem Start des Labs läuft alles von selbst wieder, mit derselben Adresse und derselben Version.
- Die Web-Konsole direkt, ohne Frontend: <http://32.194.131.21/>. Die laufende Version steht unter <http://32.194.131.21/version.txt>.

## Stand

- **Auftrag 1, Web-Konsole:** fertig, siehe [`WEBKONSOLE.md`](WEBKONSOLE.md)
- **Auftrag 2, Infrastruktur mit OpenTofu:** fertig. Die Konfiguration steht in [`infra/`](infra), die Entscheidungen und das Protokoll der Deployments und Experimente in [`INFRASTRUKTUR.md`](INFRASTRUKTUR.md), jeder Befehl eines Deployments in [`docs/deployment.md`](docs/deployment.md)
- **Auftrag 3, automatisches Deployment bei einem Release:** Jeder Release deployt nur die Teile, die sich seit dem vorherigen Release geändert haben ([`deploy.yml`](.github/workflows/deploy.yml), aufgerufen von [`release.yml`](.github/workflows/release.yml)). Jede veröffentlichte Version lässt sich von Hand erneut deployen. Secrets-Konzept, Ablauf, Nachweis und Reflexion stehen in [`docs/deployment.md`](docs/deployment.md)
- Vom 17. bis 24.11.2026 hatte GitHub Actions für dieses Repository gesperrt. Release 1.2.0 ist deshalb ohne Actions entstanden, mit denselben Schritten wie der Release-Workflow, und der Pull Request für Auftrag 2 wurde nach einer lokalen Prüfung ohne CI-Checks gemergt (siehe `INFRASTRUKTUR.md`, Protokoll)

## Dokumentation

| Thema | Datei |
|---|---|
| Release-Prozess und Versionsnummern | [`RELEASE.md`](RELEASE.md) |
| Änderungen pro Version | [`CHANGELOG.md`](CHANGELOG.md) |
| Web-Konsole, lokal im DevContainer | [`WEBKONSOLE.md`](WEBKONSOLE.md) |
| Infrastruktur auf AWS | [`INFRASTRUKTUR.md`](INFRASTRUKTUR.md) |
| Deployment: automatisch, von Hand, Secrets-Konzept | [`docs/deployment.md`](docs/deployment.md) |
| DevContainer | [`DEVCONTAINER.md`](DEVCONTAINER.md), [`DEVCONTAINER-KONZEPT.md`](DEVCONTAINER-KONZEPT.md) |
| Tests | [`TESTKONZEPT.md`](TESTKONZEPT.md), [`TESTDOKUMENTATION.md`](TESTDOKUMENTATION.md) |
| Test Coverage | [`COVERAGE-DESIGN.md`](COVERAGE-DESIGN.md), [`COVERAGE-GATE.md`](COVERAGE-GATE.md) |
