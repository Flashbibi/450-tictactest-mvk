# Testkonzept – TicTacToe (M450)

Stand: 26.10.2026 · Repository: <https://github.com/Flashbibi/450-tictactest-mvk>

## 1. Zweck und Geltungsbereich

Dieses Konzept beschreibt, **was** im TicTacToe-Projekt getestet wird, **wie** die Tests aufgebaut sind und **wann** sie laufen. Es gilt für den gesamten Testcode unter `src/test/java`.

Die konkrete Beschreibung der einzelnen Testfälle nach GIVEN_WHEN_THEN steht in [`TESTDOKUMENTATION.md`](TESTDOKUMENTATION.md). Dieses Dokument beantwortet die Frage darüber: welche Strategie hinter der Auswahl steht.

## 2. Testgegenstand

| Klasse | Rolle | Testbarkeit |
|---|---|---|
| `TicTacToeMain.isWin(Stone[], Stone)` | Siegerkennung, prüft 8 Linien | rein, statisch, ohne Seiteneffekte – **primäres Testziel** |
| `TicTacToeMain.play(...)` | Spielschleife über 9 Runden | braucht zwei Spieler – im Test ein Test-Double mit festen Zügen; die Ausgabe auf `System.out` fängt JUnit Pioneer ab |
| `TicTacToeMain.toString(Stone[])` | Board-Ausgabe mit ANSI-Codes | reine Funktion |
| `GreedyPlayer` | Computerspieler, erstes freies Feld | rein |
| `HumanPlayer` | liest von `System.in` | mit JUnit Pioneer testbar, das die Tastatur durch feste Eingaben ersetzt |
| `PerfectPlayer` | neuer Computerspieler, verliert nie (Minimax) | rein; die Behauptung „verliert nie" wird über alle möglichen Partien geprüft |
| `Stone.opponent()` | Farbwechsel | trivial |

Das Board ist ein flaches `Stone[9]`; Index `i` entspricht Zeile `i / 3` und Spalte `i % 3`. `null` heisst „Feld leer".

Der Originalcode wird nicht verändert. Neu ist nur der `PerfectPlayer`.

## 3. Teststufen und Abgrenzung

Getestet wird auf **Unit-Ebene**: einzelne Methoden, ohne Netzwerk und ohne Dateisystem. Tastatur und Konsole ersetzt JUnit Pioneer.

`play(...)` bekommt im Test keine echten Spieler, sondern das Test-Double `playing(...)` mit festen Zügen. So bleibt auch die Spielschleife ein Unit-Test. Nur wenige Tests lassen zwei echte Spieler gegeneinander antreten (`PerfectPlayer` gegen `GreedyPlayer` und gegen sich selbst).

Zusätzlich prüft **ArchUnit** eine Architekturregel und **PITest** die Qualität der Tests selbst (Abschnitt 8).

Bewusst **nicht** Teil dieses Konzepts:
- ein ganzes Spiel über `main(...)` mit Tastatureingaben – scheitert am Scanner-Fehler in `HumanPlayer` (Abschnitt 10)
- Performance- und Lasttests – bei einem 3×3-Brett ohne Nutzen

## 4. Werkzeuge und Umgebung

| | |
|---|---|
| Testframework | JUnit Jupiter 6.1.3 (`org.junit.jupiter:junit-jupiter`) |
| Assertions | AssertJ 3.27.7, eingebunden über `implements WithAssertions` |
| StdIn/StdOut | JUnit Pioneer 2.3.0 (`@StdIo`, `StdOut`) |
| Zusätzliches Framework | ArchUnit 1.5.1 (`archunit-junit6`) |
| Coverage | JaCoCo 0.8.16 |
| Mutation Testing | PITest 1.30.1 mit Gradle-Plugin `info.solidsoft.pitest` 1.19.0 und `pitest-junit5-plugin` 1.2.3 |
| Build | Gradle 9.8.2 via Wrapper (`./gradlew`) |
| JDK | Java 25 (lokal Zulu 25, CI im DevContainer mit Zulu 25) |
| CI | GitHub Actions, `.github/workflows/build.yml` |

Es werden jeweils die aktuellsten stabilen Versionen verwendet. AssertJ 4.0.0-M1 ist nur ein Milestone und deshalb nicht eingesetzt. Mocking-Frameworks wie Mockito werden nicht verwendet.

`junit-jupiter` ist das Aggregat-Artefakt und enthält `junit-jupiter-params` bereits – für `@ParameterizedTest` braucht es keine zusätzliche Dependency.

Alle Assertions sind AssertJ. Für das Spielfeld gibt es eine eigene AssertJ-Assertion `BoardAssert` (Abschnitt 5), damit kein Test bei `assertThat(isWin(...)).isTrue()` stehen bleibt.

## 5. Aufbau der Tests

Fünf Bausteine tragen die Suite:

### Helper `boardOf(String sketch)`

Übersetzt eine lesbare Skizze in ein `Stone[]`:

```java
boardOf("XXO OOX XOX")   // X = Kreuz, O = Kreis, . = leer, Leerzeichen trennen nur optisch
```

Der Aufbau eines Boards steht damit an **einer** Stelle statt in jeder Testmethode. Ein neues Szenario kostet eine Zeile Text statt eines neunstelligen Array-Literals, und man sieht der Zeile das Spielfeld direkt an. Der Helper liegt in `Boards.java`, damit alle Testklassen ihn nutzen können.

### Eigene Assertion `BoardAssert`

```java
assertThatBoard("XXX ... ...").isWonBy(CROSS).isNotWonBy(CIRCLE);
```

Liest sich wie ein Satz und meldet bei einem Fehlschlag das Board und die erwartete Farbe statt nur „expected true but was false".

### Fixture `@BeforeEach setUp()`

Baut vor **jedem** Test ein frisches Board aus der Konstanten `DRAW_BOARD` (`"XXO OOX XOX"`, volles Brett ohne Sieger). Jeder Test arbeitet auf einer eigenen Instanz – kein Test kann einen anderen beeinflussen.

Bewusst **nicht** als `private static final Stone[]` gelöst: ein statisches Array wäre eine geteilte, veränderbare Instanz. Solange kein Test hineinschreibt, fällt das nicht auf; sobald doch, entstehen Fehler, die von der Ausführungsreihenfolge abhängen und schwer zu finden sind.

### Parametrisierung `@ParameterizedTest`

Gleiche Prüfung, viele Fälle – mit der Quelle, die am besten passt:

| Quelle | Beispiel |
|---|---|
| `@CsvSource` | `detectsWinningLine`: Skizze und Farbe pro Zeile |
| `@ValueSource` | `twoInALineIsNoWin`: 24 Skizzen; `rejectsMovesOutsideTheBoard`: Felder -1 und 9 |
| `@EnumSource` | `nobodyWinsTheDrawBoard`: einmal für `CROSS`, einmal für `CIRCLE` |
| `@CsvSource(textBlock = …)` | `findsTheBestMove`: Stellung, Farbe, erwarteter Zug und Begründung als Tabelle |

Der `name`-Parameter sorgt dafür, dass im Report jeder Fall einzeln und lesbar erscheint statt als `[1]`, `[2]`, `[3]`.

### Test-Double `playing(int... moves)`

```java
TicTacToeMain.play(playing(0, 1, 2), playing(3, 4));   // X gewinnt mit der oberen Zeile
```

Ein Spieler, der die angegebenen Felder der Reihe nach spielt. `TicTacToePlayer` hat nur eine Methode, deshalb reicht ein Lambda – ein Mocking-Framework braucht es nicht.

## 6. Konventionen

- **Struktur:** GIVEN (Arrange), WHEN (Act), THEN (Assert), im Code durch Leerzeilen getrennt statt durch Kommentare
- **Namen:** Methodennamen beschreiben das erwartete Verhalten, nicht die Implementierung – `nobodyWinsTheDrawBoard` statt `testIsWin2`
- **Umfang:** eine Testmethode prüft einen Sachverhalt; mehrere Konstellationen desselben Sachverhalts werden parametrisiert, nicht kopiert
- **Codestyle:** `var` wo möglich, Tabs wie im Originalcode, keine SonarLint-Warnungen
- **Dokumentation:** jeder Test ist in `TESTDOKUMENTATION.md` nach GIVEN_WHEN_THEN beschrieben

## 7. Abdeckung – Ist-Stand

74 Testfälle: 73 aus JUnit Jupiter, 1 aus ArchUnit.

| Testklasse | Fälle | Deckt ab |
|---|---|---|
| `TicTacToeTest` | 49 | `isWin` (37 Fälle, alle 8 Linien, alle Teilbedingungen), `toString`, `play` mit Sieg, Unentschieden und ungültigen Zügen, Konsolenausgabe |
| `TicTacToePlayerTest` | 2 | `Stone.opponent()` |
| `GreedyPlayerTest` | 5 | erstes freies Feld, volles Brett |
| `HumanPlayerTest` | 4 | Tastatureingabe, Ausgabe, ungültige Eingabe, Scanner-Fehler |
| `PerfectPlayerTest` | 13 | beste Züge, alle möglichen Partien ohne Niederlage, Spiele gegen andere Spieler |
| `ArchitectureTest` | 1 | Spieler hängen nicht voneinander ab |

JaCoCo, Stand 26.10.2026:

| Metrik | Wert |
|---|---|
| Branch Coverage | 100 % (98 von 98) |
| Instruction Coverage | 97.6 % (484 von 496) |

Nicht abgedeckt ist nur `TicTacToeMain.main(...)`: es startet ein Spiel mit Tastatureingaben, siehe Abschnitt 10.

## 8. Durchführung und Quality Gates

Lokal:

```bash
./gradlew check --rerun-tasks   # Tests, ArchUnit, Coverage-Report und 90-%-Prüfung
./gradlew pitest                # Mutation Testing
```

`--rerun-tasks` umgeht den Gradle-Cache; ohne die Option meldet Gradle bei unverändertem Code nur `UP-TO-DATE` und gibt keine Testnamen aus.

| Gate | Wo | Wirkung |
|---|---|---|
| JaCoCo Coverage Verification | `jacocoTestCoverageVerification` in `build.gradle`, hängt an `check` | der Build schlägt fehl, wenn die Branch Coverage unter 90 % liegt |
| Mutation Testing mit PITest | `./gradlew pitest` | baut kleine Fehler (Mutanten) in den Produktionscode ein und prüft, ob ein Test sie bemerkt. Kein Schwellenwert, der Score ist eine Kennzahl für die Qualität der Tests |
| Coverage Gate | `.github/workflows/coverage-gate.yml` | ein Pull Request darf die Coverage von `main` nicht senken, siehe [`COVERAGE-GATE.md`](COVERAGE-GATE.md) |

Reports:

| Report | Pfad | Artifact in der CI |
|---|---|---|
| Testergebnisse | `build/reports/tests/test/index.html` | `test-report` |
| Coverage | `build/reports/jacoco/test/html/index.html` | `jacoco-report` |
| Mutation Testing | `build/reports/pitest/index.html` | `pitest-report` |

In der CI läuft bei jedem Push und jedem Pull Request zuerst `./gradlew assemble` (Kompilieren ohne Tests), danach im separaten Job `./gradlew check pitest`. Alle drei Reports werden als Artifacts gespeichert, auch wenn ein Schritt fehlschlägt.

## 9. Akzeptanzkriterien

Ein Stand gilt als abgenommen, wenn:

1. `./gradlew check` mit `BUILD SUCCESSFUL` endet – damit sind alle Tests grün und die Branch Coverage liegt bei mindestens 90 %
2. der CI-Workflow auf dem Branch grün ist
3. jeder neue Test in `TESTDOKUMENTATION.md` nach GIVEN_WHEN_THEN dokumentiert ist
4. kein Test von der Ausführungsreihenfolge eines anderen abhängt

## 10. Offene Punkte

| Thema | Warum offen |
|---|---|
| Scanner-Fehler in `HumanPlayer` | `HumanPlayer` erstellt pro Zug einen neuen `Scanner`. Kommen alle Eingaben auf einmal (Pioneer, Pipe), geht ab dem zweiten Zug die Eingabe verloren. An der Tastatur fällt es nicht auf. Der Originalcode soll nicht verändert werden, deshalb ist der Fehler nur im Test `losesTheSecondMoveWhenAllInputArrivesAtOnce` dokumentiert |
| `main(...)` ungetestet | ein Spiel über `main` mit festen Eingaben scheitert am Scanner-Fehler |
