# Testkonzept – TicTacToe (M450)

Stand: 09.09.2026 · Repository: <https://github.com/Flashbibi/450-tictactest-mvk>

## 1. Zweck und Geltungsbereich

Dieses Konzept beschreibt, **was** im TicTacToe-Projekt getestet wird, **wie** die Tests aufgebaut sind und **wann** sie laufen. Es gilt für den gesamten Testcode unter `src/test/java`.

Die konkrete Beschreibung der einzelnen Testfälle nach GIVEN_WHEN_THEN steht in [`TESTDOKUMENTATION.md`](TESTDOKUMENTATION.md). Dieses Dokument beantwortet die Frage darüber: welche Strategie hinter der Auswahl steht.

## 2. Testgegenstand

| Klasse | Rolle | Testbarkeit |
|---|---|---|
| `TicTacToeMain.isWin(Stone[], Stone)` | Siegerkennung, prüft 8 Linien | rein, statisch, ohne Seiteneffekte – **primäres Testziel** |
| `TicTacToeMain.play(...)` | Spielschleife über 9 Runden | schreibt auf `System.out`, braucht zwei Spieler-Instanzen |
| `TicTacToeMain.toString(Stone[])` | Board-Ausgabe mit ANSI-Codes | reine Funktion, aber Ausgabeformat ist kosmetisch |
| `GreedyPlayer` | Computerspieler, erstes freies Feld | rein, aber Zusammenspiel mit dem Board |
| `HumanPlayer` | liest von `System.in` | ohne Umleitung von stdin nicht sinnvoll testbar |
| `Stone.opponent()` | Farbwechsel | trivial |

Das Board ist ein flaches `Stone[9]`; Index `i` entspricht Zeile `i / 3` und Spalte `i % 3`. `null` heisst „Feld leer".

## 3. Teststufen und Abgrenzung

Getestet wird ausschliesslich auf **Unit-Ebene**: einzelne Methoden, ohne Netzwerk, Dateisystem oder Benutzereingabe.

Bewusst **nicht** Teil dieses Konzepts:
- Integrationstests über die Spielschleife (`play(...)` mit zwei Spielern) – ein solcher Test wurde begonnen und wieder entfernt, weil er zwei Klassen im Zusammenspiel prüft und damit über die Unit-Ebene hinausgeht
- Tests gegen `System.out`/`System.in`
- Performance- und Lasttests – bei einem 3×3-Brett ohne Nutzen

## 4. Werkzeuge und Umgebung

| | |
|---|---|
| Testframework | JUnit Jupiter 6.1.3 (`org.junit.jupiter:junit-jupiter`) |
| Assertions | AssertJ 3.27.7, eingebunden über `implements WithAssertions` |
| Build | Gradle 9.7.0 via Wrapper (`./gradlew`) |
| JDK | Java 25 (lokal Zulu 25.0.4, CI `zulu` 25) |
| CI | GitHub Actions, `.github/workflows/build.yml` |

`junit-jupiter` ist das Aggregat-Artefakt und enthält `junit-jupiter-params` bereits – für `@ParameterizedTest` braucht es keine zusätzliche Dependency.

AssertJ ist der Standard für Assertions (`assertThat(x).isTrue()`), weil es bei Fehlschlägen aussagekräftigere Meldungen liefert. JUnit-eigene Assertions (`assertFalse`) kommen nur in den Dummy-Tests vor, die belegen sollen, dass beide Frameworks eingebunden sind.

## 5. Aufbau der Tests

Drei Bausteine tragen die gesamte Suite:

### Helper `boardOf(String sketch)`

Übersetzt eine lesbare Skizze in ein `Stone[]`:

```java
boardOf("XXO OOX XOX")   // X = Kreuz, O = Kreis, . = leer, Leerzeichen trennen nur optisch
```

Der Aufbau eines Boards steht damit an **einer** Stelle statt in jeder Testmethode. Ein neues Szenario kostet eine Zeile Text statt eines neunstelligen Array-Literals, und man sieht der Zeile das Spielfeld direkt an.

### Fixture `@BeforeEach setUp()`

Baut vor **jedem** Test ein frisches Board aus der Konstanten `DRAW_BOARD` (`"XXO OOX XOX"`, volles Brett ohne Sieger). Zwei Tests teilen sich diese Ausgangslage, arbeiten aber je auf einer eigenen Instanz – kein Test kann einen anderen beeinflussen.

Bewusst **nicht** als `private static final Stone[]` gelöst: ein statisches Array wäre eine geteilte, veränderbare Instanz. Solange kein Test hineinschreibt, fällt das nicht auf; sobald doch, entstehen Fehler, die von der Ausführungsreihenfolge abhängen und schwer zu finden sind.

### Parametrisierung `@ParameterizedTest` + `@CsvSource`

Gleiche Prüfung, viele Board-Konstellationen:

```java
@ParameterizedTest(name = "{0} gewonnen von {1}")
@CsvSource({
        "XXX ... ..., CROSS",
        "O.. O.. O.., CIRCLE",
        ...
})
void detectsWinningLine(String sketch, Stone color) {
    assertThat(TicTacToeMain.isWin(boardOf(sketch), color)).isTrue();
}
```

`@CsvSource` statt `@MethodSource`, weil der Helper Skizzen als Text entgegennimmt – die Testdaten stehen damit direkt bei der Methode und brauchen keine separate Provider-Methode. JUnit konvertiert die zweite Spalte automatisch in das Enum `Stone`. Der `name`-Parameter sorgt dafür, dass im Report jeder Fall einzeln und lesbar erscheint statt als `[1]`, `[2]`, `[3]`.

## 6. Konventionen

- **Struktur:** GIVEN (Arrange), WHEN (Act), THEN (Assert), im Code durch Leerzeilen getrennt statt durch Kommentare
- **Namen:** Methodennamen beschreiben das erwartete Verhalten, nicht die Implementierung – `circleDoesNotWinOnTheDrawBoard` statt `testIsWin2`
- **Umfang:** eine Testmethode prüft einen Sachverhalt; mehrere Konstellationen desselben Sachverhalts werden parametrisiert, nicht kopiert
- **Dokumentation:** jeder Test ist in `TESTDOKUMENTATION.md` nach GIVEN_WHEN_THEN beschrieben

## 7. Abdeckung – Ist-Stand

6 Testmethoden, **15 ausgeführte Testfälle**:

| Testmethode | Fälle | Deckt ab |
|---|---|---|
| `dummyJunit` | 1 | JUnit ist eingebunden |
| `dummyAssertJ` | 1 | AssertJ ist eingebunden |
| `crossDoesNotWinOnTheDrawBoard` | 1 | volles Brett, X gewinnt nicht |
| `circleDoesNotWinOnTheDrawBoard` | 1 | volles Brett, O gewinnt nicht |
| `detectsWinningLine` | 8 | alle 8 Gewinnlinien: 3 Zeilen, 3 Spalten, 2 Diagonalen |
| `detectsNoWinningLine` | 3 | leeres Brett, nur zwei in Reihe, richtige Linie aber falsche Farbe |

Damit ist `isWin(...)` vollständig abgedeckt: jede der 8 Bedingungen im Methodenrumpf wird mindestens einmal wahr, und die Gegenrichtung wird über die Negativfälle geprüft. Der Fall „richtige Linie, falsche Farbe" ist dabei der wichtigste – er stellt sicher, dass die Methode nicht bloss *irgendeine* Dreierreihe meldet.

**Nicht abgedeckt:** `play(...)`, `toString(...)`, `GreedyPlayer`, `HumanPlayer`, `Stone.opponent()`. Siehe Abschnitt 3 und 10.

## 8. Durchführung

Lokal:

```bash
./gradlew test --rerun-tasks
```

`--rerun-tasks` umgeht den Gradle-Cache; ohne die Option meldet Gradle bei unverändertem Code nur `UP-TO-DATE` und gibt keine Testnamen aus. Ein HTML-Report entsteht unter `build/reports/tests/test/index.html`.

In der CI läuft bei jedem Push und jedem Pull Request zuerst `./gradlew assemble` (Kompilieren ohne Tests), danach im separaten Job `./gradlew test`.

## 9. Akzeptanzkriterien

Ein Stand gilt als abgenommen, wenn:

1. `./gradlew test` mit `BUILD SUCCESSFUL` endet
2. der CI-Workflow auf dem Branch grün ist
3. jeder neue Test in `TESTDOKUMENTATION.md` nach GIVEN_WHEN_THEN dokumentiert ist
4. kein Test von der Ausführungsreihenfolge eines anderen abhängt

## 10. Offene Punkte

| Thema | Warum offen |
|---|---|
| `thisTestFails` liegt auskommentiert im Testfile | stammt aus dem vorherigen Auftrag (Screenshot eines Fehlschlags); auskommentierter Code sollte mittelfristig weg |
| `play(...)` ungetestet | braucht ein Test-Double für `TicTacToePlayer` und wäre ein Integrationstest – bewusst zurückgestellt |
| `GreedyPlayer` ungetestet | ein Test dazu existierte, wurde aber als integrationsnah wieder entfernt |
| Code-Coverage wird nicht gemessen | kein JaCoCo eingebunden; Abdeckung ist bisher per Hand hergeleitet |
