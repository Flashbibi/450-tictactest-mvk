# Changelog

Alle wichtigen Änderungen an TicTacTest, die neueste Version zuoberst. Wie die Versionsnummer entsteht, steht in [`RELEASE.md`](RELEASE.md).

## 1.0.0 - 2026-10-26

### Added
- Spielbares TicTacToe in der Konsole: ein Mensch (X) gegen den Computerspieler GreedyPlayer (O)
- Ausführbares JAR, Start mit `java -jar tictactest-1.0.0.jar`
- Neuer Computerspieler PerfectPlayer, der nie verliert
- Automatisierte Tests mit JUnit, AssertJ, JUnit Pioneer und ArchUnit, Mutation Testing mit PITest
- Coverage-Messung mit JaCoCo: der Build bricht unter 90 % Branch Coverage ab
- Verlauf der Coverage von `main` auf GitHub Pages und ein Coverage Gate für Pull Requests
- DevContainer mit Java 25, versioniert in der GitHub Container Registry
