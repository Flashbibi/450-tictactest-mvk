# Changelog

Alle wichtigen Änderungen an TicTacTest, die neueste Version zuoberst. Wie die Versionsnummer entsteht, steht in [`RELEASE.md`](RELEASE.md).

## 1.2.0 - 2026-11-18

### Added
- TicTacTest auf AWS: OpenTofu richtet in `infra/` eine EC2-Instanz mit der Web-Konsole als Dienst, eine feste Adresse (Elastic IP) und das Frontend als S3-Website ein. Aufbau und Entscheidungen stehen in [`INFRASTRUKTUR.md`](INFRASTRUKTUR.md)
- Deployment von Hand: Jeder Befehl steht in [`docs/deployment.md`](docs/deployment.md). Ein neuer Release braucht weder `tofu apply` noch eine neue Instanz
- Frontend: Link, der das Terminal in einem eigenen Tab öffnet

## 1.1.0 - 2026-11-17

### Added
- TicTacTest im Browser: Die Web-Konsole startet für jeden Besucher das JAR aus dem Release in einem eigenen Terminal. Das Spiel selbst ist unverändert. Start und Aufbau stehen in [`WEBKONSOLE.md`](WEBKONSOLE.md)
- Frontend: eine Webseite mit Spielregeln, eingebettetem Terminal und den Versionen von Frontend und Backend. Sie liegt jedem Release als `tictactest-frontend-<Version>.zip` bei
- Die Version des laufenden Backends ist per HTTP abfragbar: `/version.txt`

## 1.0.2 - 2026-10-26

### Fixed
- Kommen mehrere Eingaben auf einmal an, zum Beispiel über eine Pipe, gehen keine Züge mehr verloren. Bisher brach das Spiel dann beim zweiten Zug ab.

### Added
- End-to-End-Tests: ganze Spiele über die Konsole, ungültige und böswillige Eingaben, im eigenen Gradle-Task `e2eTest`

## 1.0.1 - 2026-10-26

### Fixed
- Tippfehler in der Eingabeaufforderung: statt „where to to put the next …" heisst es jetzt „where to put the next …"

## 1.0.0 - 2026-10-26

### Added
- Spielbares TicTacToe in der Konsole: ein Mensch (X) gegen den Computerspieler GreedyPlayer (O)
- Ausführbares JAR, Start mit `java -jar tictactest-1.0.0.jar`
- Neuer Computerspieler PerfectPlayer, der nie verliert
- Automatisierte Tests mit JUnit, AssertJ, JUnit Pioneer und ArchUnit, Mutation Testing mit PITest
- Coverage-Messung mit JaCoCo: der Build bricht unter 90 % Branch Coverage ab
- Verlauf der Coverage von `main` auf GitHub Pages und ein Coverage Gate für Pull Requests
- DevContainer mit Java 25, versioniert in der GitHub Container Registry
