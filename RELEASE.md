# Release-Prozess

Stand: 26.10.2026 · Workflow: [`.github/workflows/release.yml`](.github/workflows/release.yml) · Änderungen: [`CHANGELOG.md`](CHANGELOG.md)

## Ablauf

```
Entwicklung
    ↓
Änderungen nach main (Pull Request)
    ↓
Tests und Build grün
    ↓
CHANGELOG aktualisieren und Version in build.gradle festlegen (Pull Request)
    ↓
Git-Tag erstellen und zu GitHub pushen
    ↓
GitHub Actions: Tests → Build → JAR → GitHub Release mit JAR
    ↓
JAR herunterladen und testen
```

## Einen Release erstellen

### 1. Vorbereiten

Diese Bedingungen müssen erfüllt sein:

| Bedingung | So wird sie geprüft |
|---|---|
| Die Anwendung ist auf dem gewünschten Stand, alle vorgesehenen Änderungen sind auf `main` | alle Pull Requests für den Release sind gemerged |
| Das Projekt baut, alle Tests sind grün | der letzte Lauf des Workflows `build` auf `main` ist grün. Lokal: `./gradlew check` |
| Die Versionsnummer ist festgelegt | `version = '…'` in `build.gradle`, nach den Regeln unten |
| Das Changelog ist aktualisiert | `CHANGELOG.md` hat einen Abschnitt `## <Version> - <Datum>` |

Version und Changelog werden wie jede andere Änderung über einen Pull Request nach `main` gebracht. Die letzten beiden Bedingungen prüft der Workflow zusätzlich selbst.

### 2. Tag erstellen und pushen

```bash
git switch main
git pull
git tag -a v1.0.0 -m "Release v1.0.0"
git push origin v1.0.0
```

### 3. Automatisch: Workflow `release`

1. Repository auschecken und Java 25 installieren
2. prüfen, dass `build.gradle` die Version des Tags hat und `CHANGELOG.md` einen Eintrag dafür
3. `./gradlew check`: alle Tests und die Coverage-Prüfung
4. `./gradlew jar`: baut `build/libs/tictactest-<Version>.jar`
5. GitHub Release erstellen: Release Notes mit Tag, Commit, den Änderungen aus dem Changelog und dem Namen des Artefakts, das JAR als Anhang

### 4. Release testen

Ein Release gilt erst als erfolgreich, wenn das veröffentlichte JAR getestet ist:

1. die Release-Seite öffnen: <https://github.com/Flashbibi/450-tictactest-mvk/releases>
2. `tictactest-<Version>.jar` herunterladen
3. starten und eine Partie spielen (braucht Java 25):
   ```bash
   java -jar tictactest-1.0.0.jar
   ```
   Die Züge einzeln eintippen. Bis Version 1.0.1 gingen Eingaben, die alle auf einmal kamen, zum Beispiel über eine Pipe, ab dem zweiten Zug verloren (siehe [`TESTDOKUMENTATION.md`](TESTDOKUMENTATION.md)).
4. das Ergebnis im Protokoll unten eintragen

## Versionsnummer

`MAJOR.MINOR.PATCH`, der Git-Tag hat zusätzlich ein `v` davor: Version `1.0.0`, Tag `v1.0.0`.

| Stelle | Erhöhen, wenn … | Beispiel |
|---|---|---|
| MAJOR | das Spiel sich nicht mehr wie bisher bedienen lässt | Eingabe als Zeile und Spalte statt als Feldnummer |
| MINOR | eine neue Funktion dazukommt und alles Bisherige weiter funktioniert | Spielmodus „Mensch gegen PerfectPlayer" |
| PATCH | ein Fehler behoben wird, ohne neue Funktion | Tippfehler in einer Ausgabe |

## Fragen und Antworten

| Frage | Antwort |
|---|---|
| Wann wird ein Release erstellt? | wenn auf `main` ein Stand liegt, der veröffentlicht werden soll. Das wird bewusst entschieden, nicht bei jeder Änderung |
| Wer bzw. was startet einen Release? | eine Person pusht einen Tag `v…`. Der Tag startet den Workflow `release` |
| Warum löst nicht jeder Push auf `main` einen Release aus? | `main` ändert sich bei jedem Merge, aber nicht jeder Merge ist eine Version für die Benutzer. Der Workflow reagiert deshalb nur auf Tags (`on: push: tags: 'v*'`) |
| Wie wird die Versionsnummer festgelegt? | von Hand in `build.gradle`, nach den Regeln oben. Passt der Tag nicht zur Version, bricht der Workflow ab |
| Wie wird ein Release-Tag erstellt? | `git tag -a v1.0.0 -m "Release v1.0.0"`, danach `git push origin v1.0.0` |
| Welcher Commit gehört zu einem Release? | der Commit, auf den der Tag zeigt. Er steht in den Release Notes und auf GitHub beim Tag |
| Wie wird das JAR gebaut? | mit `./gradlew jar`. Das Manifest enthält die `Main-Class`, deshalb startet `java -jar` das Spiel. Der Dateiname kommt aus `settings.gradle` (`tictactest`) und der Version |
| Wo kann das JAR heruntergeladen werden? | auf der Release-Seite: <https://github.com/Flashbibi/450-tictactest-mvk/releases> |
| Was enthält das Changelog? | pro Version die Nummer, das Datum und die Änderungen, aufgeteilt in Added (neu), Changed (geändert) und Fixed (behoben) |
| Was passiert, wenn beim Release ein Test fehlschlägt? | der Workflow bricht bei `./gradlew check` ab. Es entsteht kein Release und kein JAR. Den Fehler über einen Pull Request beheben. Weil nichts veröffentlicht wurde, darf der Tag danach gelöscht (`git push --delete origin v1.0.0` und `git tag -d v1.0.0`) und auf dem neuen Stand von `main` noch einmal gesetzt werden |

## Protokoll der Release-Tests

| Version | Datum | Getestet von | JAR heruntergeladen | Gestartet und eine Partie gespielt |
|---|---|---|---|---|
