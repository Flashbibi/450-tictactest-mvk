# Testdokumentation – TicTacToe (M450)

## Setup

`build.gradle`:

```groovy
plugins {
    id 'java'
    id 'application'
    id 'jacoco'
    id 'info.solidsoft.pitest' version '1.19.0'
}

dependencies {
    testImplementation 'org.assertj:assertj-core:3.27.7'
    testImplementation 'org.junit.jupiter:junit-jupiter:6.1.3'
    testImplementation 'org.junit-pioneer:junit-pioneer:2.3.0'
    testImplementation 'com.tngtech.archunit:archunit-junit6:1.5.1'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

test {
    useJUnitPlatform {
        excludeTags 'e2e'
    }
    finalizedBy jacocoTestReport
}

tasks.register('e2eTest', Test) {
    testClassesDirs = sourceSets.test.output.classesDirs
    classpath = sourceSets.test.runtimeClasspath
    useJUnitPlatform {
        includeTags 'e2e'
    }
}

check.dependsOn e2eTest
```

`junit-jupiter` ist das Aggregat-Artefakt und enthält `junit-jupiter-params` bereits – für `@ParameterizedTest` braucht es keine zusätzliche Dependency.

Ausführen:

```bash
./gradlew check --rerun-tasks   # Unit-Tests, ArchUnit, End-to-End-Tests, Coverage-Report und Prüfung auf 90 % Branch Coverage
./gradlew e2eTest               # nur die End-to-End-Tests
./gradlew pitest                # Mutation Testing
```

Wo die Reports liegen, steht in [`TESTKONZEPT.md`](TESTKONZEPT.md), Abschnitt 8.

## Test-Code auf GitHub

<https://github.com/Flashbibi/450-tictactest-mvk/tree/main/src/test/java/ch/bbw/m450/tictactoe>

## Hilfsstrukturen

| Baustein | Wo | Wofür |
|---|---|---|
| `boardOf(sketch)` | `Boards.java` | baut ein `Stone[]` aus einer lesbaren Skizze wie `"XXX ... ..."` (`X` = Kreuz, `O` = Kreis, `.` = leer) |
| `assertThatBoard(...)` | `BoardAssert.java` | eigene AssertJ-Assertion: `assertThatBoard("XXX ... ...").isWonBy(CROSS)` statt `assertThat(isWin(...)).isTrue()` |
| Fixture `setUp()` | `TicTacToeTest` | baut vor jedem Test ein frisches Board aus `DRAW_BOARD` (`"XXO OOX XOX"`, volles Brett ohne Sieger) |
| `playing(0, 1, 2)` | `TicTacToeTest` | Test-Double für einen Spieler: spielt die angegebenen Felder der Reihe nach. Ein Lambda, kein Mocking-Framework |
| `@StdIo` und `StdOut` | JUnit Pioneer | `@StdIo("4")` ersetzt die Tastatur durch die Eingabe „4", ein Parameter `StdOut out` fängt die Konsolenausgabe ab |
| `@Tag("e2e")` und `@Timeout` | `TicTacToeE2ETest` | der Tag ordnet die Klasse dem Gradle-Task `e2eTest` zu, der Timeout bricht einen hängenden Test nach 5 Sekunden ab |

## Tests (GIVEN_WHEN_THEN)

### `TicTacToeTest` – Siegerkennung `isWin`

| Test | GIVEN | WHEN | THEN |
|---|---|---|---|
| `nobodyWinsTheDrawBoard` (2 Fälle) | das Fixture-Board `XXO / OOX / XOX` | `isWin` für `CROSS` und für `CIRCLE` | niemand gewinnt |
| `detectsWinningLine` (8 Fälle) | je eine der 8 Gewinnlinien (3 Zeilen, 3 Spalten, 2 Diagonalen), abwechselnd mit `CROSS` und `CIRCLE` | `isWin` für die Farbe der Linie und für den Gegner | die Farbe der Linie gewinnt, der Gegner nicht |
| `detectsNoWinningLine` (3 Fälle) | ein leeres Board, ein Board mit nur zwei in einer Reihe, ein Board mit `XXX` oben, abgefragt für `CIRCLE` | `isWin` | kein Sieg – die Erkennung ist farbabhängig und braucht drei in einer Linie |
| `twoInALineIsNoWin` (24 Fälle) | jede der 8 Linien mit nur zwei Steinen, jeder der drei Steine fehlt einmal | `isWin` für beide Farben | kein Sieg. So wird jede Teilbedingung in `isWin` einmal falsch |

### `TicTacToeTest` – Ausgabe `toString`

| Test | GIVEN | WHEN | THEN |
|---|---|---|---|
| `showsStonesAndTheNumbersOfFreeFields` | Board `XO. ..X O..` | `toString` | drei Zeilen, Steine als `X`/`O`, freie Felder als ihre Nummer |
| `printsStonesBoldAndFreeFieldsGray` | Board `XO. ... ...` | `toString` | Steine stehen fett (`\033[1m`), freie Felder grau (`\033[37m`) |

### `TicTacToeTest` – Spielschleife `play`

| Test | GIVEN | WHEN | THEN |
|---|---|---|---|
| `crossWinsWithTheTopRow` | X spielt 0, 1, 2 – O spielt 3, 4 | `play` | `CROSS` gewinnt |
| `circleWinsWithTheMiddleRow` | X spielt 0, 1, 8 – O spielt 3, 4, 5 | `play` | `CIRCLE` gewinnt |
| `fullBoardWithoutLineIsADraw` | Züge, die das `DRAW_BOARD` ergeben | `play` | Unentschieden, Rückgabe `null` |
| `rejectsMovesOutsideTheBoard` (2 Fälle) | X spielt auf Feld -1 bzw. 9 | `play` | `IllegalStateException` „cannot play to position …" |
| `rejectsMovesOnATakenField` | X und O spielen beide auf Feld 0 | `play` | `IllegalStateException` „cannot play to position 0" |
| `aPlayerCannotPlayAgainstItself` | derselbe Spieler als X und O | `play` | `IllegalArgumentException` „players must differ" |

### `TicTacToeTest` – Konsolenausgabe von `play` (Pioneer)

| Test | GIVEN | WHEN | THEN |
|---|---|---|---|
| `announcesTheWinnerBelowTheFinalBoard` | X spielt 0, 1, 2 – O spielt 3, 4 | `play`, Ausgabe abgefangen | unter dem letzten Board steht „...and the winner is: CROSS" |
| `announcesADraw` | Züge, die das `DRAW_BOARD` ergeben | `play`, Ausgabe abgefangen | die Ausgabe ist genau „it's a draw!" |
| `printsTheBoardBeforeRejectingAMove` | X spielt zweimal auf Feld 4 | `play`, Ausgabe abgefangen | zuerst wird das Board ausgegeben, dann folgt die `IllegalStateException` |

### `HumanPlayerTest` (Pioneer)

| Test | GIVEN | WHEN | THEN |
|---|---|---|---|
| `playsTheFieldTypedOnTheKeyboard` | Eingabe „4" | `play` | Rückgabe 4 |
| `showsTheBoardAndAsksForTheNextMove` | Eingabe „7", Board `X.. .O. ...` | `play` | Ausgabe: das Board, darunter „where to put the next CROSS? (0-8): " |
| `crashesOnInputThatIsNotANumber` | Eingabe „vier" | `play` | `NumberFormatException` |
| `readsOneLinePerMove` | Eingaben „2" und „6", beide schon da | zwei Züge nacheinander | der erste Zug liefert 2, der zweite 6 – Regressionstest zum Scanner-Fehler, siehe unten |

### `TicTacToePlayerTest` und `GreedyPlayerTest`

| Test | GIVEN | WHEN | THEN |
|---|---|---|---|
| `opponentIsTheOtherColor` (2 Fälle) | `CROSS` bzw. `CIRCLE` | `opponent()` | die andere Farbe |
| `playsTheFirstFreeField` (4 Fälle) | Boards, auf denen das erste freie Feld 0, 2, 6 bzw. 8 ist | `GreedyPlayer.play` | genau dieses Feld |
| `givesUpOnAFullBoard` | ein volles Board | `GreedyPlayer.play` | `IllegalStateException` „cannot play at all" |

### `PerfectPlayerTest`

Der `PerfectPlayer` probiert mit Minimax jede mögliche Fortsetzung der Partie durch und wählt den Zug, der für ihn am besten ausgeht, wenn auch der Gegner perfekt spielt. Bei gleich guten Zügen nimmt er das kleinste Feld.

| Test | GIVEN | WHEN | THEN |
|---|---|---|---|
| `findsTheBestMove` (7 Fälle) | Stellungen mit genau einem besten Zug: sofort gewinnen, blocken, auf eine Ecke mit der Mitte antworten, … | `PerfectPlayer.play` | genau dieser Zug |
| `takesTheLowestFieldAmongEquallyGoodMoves` | leeres Board, jeder Zug führt zu einem Unentschieden | `play` als X | Feld 0 |
| `givesUpOnAFullBoard` | ein volles Board | `play` | `IllegalStateException` „cannot play at all" |
| `neverLosesAgainstAnyOpponent` (2 Fälle) | ein Gegner, der an jedem Zug jedes freie Feld ausprobiert | jede mögliche Partie, einmal mit dem PerfectPlayer als X (73 Partien) und einmal als O (569 Partien) | keine einzige Niederlage |
| `drawsAgainstItself` | PerfectPlayer gegen PerfectPlayer | `play`, Ausgabe abgefangen | Unentschieden, Ausgabe „it's a draw!" |
| `beatsTheGreedyPlayer` | PerfectPlayer gegen GreedyPlayer, einmal als X und einmal als O | `play`, Ausgabe abgefangen | der PerfectPlayer gewinnt beide Partien |

### `ArchitectureTest` (ArchUnit)

ArchUnit ist das zusätzlich gewählte Testing-Framework. Es läuft über die JUnit Platform automatisch mit `./gradlew test`.

| Test | GIVEN | WHEN | THEN |
|---|---|---|---|
| `PLAYERS_DO_NOT_KNOW_EACH_OTHER` | der kompilierte Produktionscode, ohne Tests | ArchUnit prüft die Abhängigkeiten aller Klassen im Paket `players` | keine Klasse in `players` verwendet eine andere Klasse aus `players` – ein Spieler spricht nur über `TicTacToePlayer` und `TicTacToeMain` mit dem Spiel |

### `TicTacToeE2ETest` (End-to-End, Pioneer)

Ganze Spiele über `main(...)`: Mensch (X) gegen GreedyPlayer (O), der immer das erste freie Feld nimmt. Erwartetes und tatsächliches Verhalten stehen in [`TESTKONZEPT.md`](TESTKONZEPT.md), Abschnitt 9.

| Test | GIVEN | WHEN | THEN |
|---|---|---|---|
| `crossWinsAGame` (E2E-01) | Eingaben 3, 4, 5 | `main` | die Ausgabe endet mit dem Brett `OO. XXX ...` und „...and the winner is: CROSS" |
| `circleWinsAGame` (E2E-02) | Eingaben 8, 7, 5 | `main` | die Ausgabe endet mit dem Brett `OOO ..X .XX` und „...and the winner is: CIRCLE" |
| `gameEndsInADraw` (E2E-03) | Eingaben 1, 3, 4, 6, 8 | `main` | die Ausgabe endet mit „it's a draw!" |
| `stopsOnTextInsteadOfANumber` (E2E-04) | Eingabe „vier" | `main` | `NumberFormatException` |
| `stopsOnAFieldOutsideTheBoard` (E2E-05) | Eingabe „9" | `main` | `IllegalStateException` „cannot play to position 9" |
| `stopsOnAFieldThatIsTaken` (E2E-06) | Eingabe 4, dann 0 – das Feld hat der GreedyPlayer schon genommen | `main` | `IllegalStateException` „cannot play to position 0" |
| `doesNotPrintTerminalControlCharacters` (E2E-07) | Eingabe ESC[2J, das leert das Terminal | `main` | `NumberFormatException`, die Steuerzeichen erscheinen nicht in der Ausgabe |
| `stopsInsteadOfWaitingWhenTheInputEnds` (E2E-08) | nur die Eingabe 4, danach endet die Eingabe | `main` | `NoSuchElementException` beim zweiten Zug, innerhalb des Timeouts |

## Gefundener Fehler im Originalcode

`HumanPlayer` hat bei jedem Zug einen neuen `Scanner` auf `System.in` erstellt. Ein Scanner liest mehr als eine Zeile auf einmal in seinen Puffer. Kamen alle Eingaben gleichzeitig an, wie bei `@StdIo({"2", "6"})`, gingen die weiteren Zeilen mit dem ersten Scanner verloren, und der zweite Zug warf `NoSuchElementException`. An der Tastatur fiel das nicht auf, weil jede Zeile erst nach Enter kommt.

Der Fehler ist in Release 1.0.2 test-first behoben: Zuerst kamen die End-to-End-Tests und `readsOneLinePerMove` dazu, fünf davon waren rot. Danach liest `HumanPlayer` alle Züge mit einem einzigen Scanner, und alle Tests wurden grün. Ein weiterer gefundener Fehler steht in [`TESTKONZEPT.md`](TESTKONZEPT.md), Abschnitt 9.

## Ergebnis

74 Unit-Testfälle (73 JUnit Jupiter, 1 ArchUnit) und 8 End-to-End-Tests, alle grün. JaCoCo misst die Unit-Tests: Branch Coverage 100 %, Instruction Coverage 97.6 %. Die JaCoCo-Prüfung im Build verlangt mindestens 90 % Branch Coverage. Von den Unit-Tests nicht abgedeckt ist nur `main(...)`, das führen die End-to-End-Tests aus.

## Screenshot

Fehlschlag des Tests `thisTestFails` aus dem vorherigen Auftrag. Der Test ist inzwischen entfernt.

![Testlauf mit einem fehlschlagenden Test](image.png)
