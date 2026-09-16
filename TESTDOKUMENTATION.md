# Testdokumentation – TicTacToe (M450)

## Setup

`build.gradle`:

```groovy
dependencies {
    testImplementation 'org.assertj:assertj-core:3.27.7'
    testImplementation 'org.junit.jupiter:junit-jupiter:6.1.3'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

test {
    useJUnitPlatform()
}
```

`junit-jupiter` ist das Aggregat-Artefakt und enthält `junit-jupiter-params` bereits – für `@ParameterizedTest` braucht es keine zusätzliche Dependency.

Ausführen: `./gradlew test --rerun-tasks`

## Test-Code auf GitHub

<https://github.com/Flashbibi/450-tictactest-mvk/blob/main/src/test/java/ch/bbw/m450/tictactoe/TicTacToeTest.java>

## Helper und Fixture

- **Helper** `boardOf(sketch)` – baut ein `Stone[]` aus einer lesbaren Skizze wie `"XXX ... ..."` (`X` = Kreuz, `O` = Kreis, `.` = leer). Wird sowohl von der Fixture als auch von den parametrisierten Tests genutzt.
- **Fixture** `@BeforeEach setUp()` – baut vor jedem Test ein frisches Board aus der Konstanten `DRAW_BOARD` (`"XXO OOX XOX"`, volles Brett ohne Sieger). Zwei Tests teilen sich diese Fixture, jeder bekommt aber seine eigene Instanz.

## Tests (GIVEN_WHEN_THEN)

**1. `dummyJunit`** – Dummy-Test für JUnit
- **GIVEN** der Wert `false`
- **WHEN** JUnit ihn mit `assertFalse(false)` prüft
- **THEN** läuft der Test durch – JUnit ist eingebunden

**2. `dummyAssertJ`** – Dummy-Test für AssertJ
- **GIVEN** der Text `"TicTacToe"`
- **WHEN** AssertJ ihn mit `assertThat(text).isNotBlank()` prüft
- **THEN** läuft der Test durch – AssertJ ist eingebunden

**3. `crossDoesNotWinOnTheDrawBoard`** – nutzt die Fixture
- **GIVEN** das Fixture-Board `XXO / OOX / XOX`
- **WHEN** `TicTacToeMain.isWin(board, CROSS)` aufgerufen wird
- **THEN** ist das Ergebnis `false`

**4. `circleDoesNotWinOnTheDrawBoard`** – nutzt dieselbe Fixture
- **GIVEN** dasselbe Fixture-Board
- **WHEN** `TicTacToeMain.isWin(board, CIRCLE)` aufgerufen wird
- **THEN** ist das Ergebnis `false`

**5. `detectsWinningLine`** – parametrisiert, 8 Fälle via `@CsvSource`
- **GIVEN** je eine der 8 möglichen Gewinnlinien als Skizze (3 Zeilen, 3 Spalten, 2 Diagonalen), abwechselnd mit `CROSS` und `CIRCLE` besetzt
- **WHEN** `TicTacToeMain.isWin(boardOf(sketch), color)` für jeden Fall aufgerufen wird
- **THEN** ist das Ergebnis jedes Mal `true`

**6. `detectsNoWinningLine`** – parametrisiert, 3 Fälle via `@CsvSource`
- **GIVEN** ein leeres Board, ein Board mit nur zwei in einer Reihe, und ein Board mit `XXX` oben, aber abgefragt für `CIRCLE`
- **WHEN** `TicTacToeMain.isWin(boardOf(sketch), color)` für jeden Fall aufgerufen wird
- **THEN** ist das Ergebnis jedes Mal `false` – die Siegerkennung ist farbabhängig und braucht drei in einer Linie

Zusätzlich steht `thisTestFails` auskommentiert im File. Er stammt aus dem vorherigen Auftrag (Screenshot eines Fehlschlags) und ist bewusst deaktiviert, damit die Suite grün bleibt.

## Ergebnis

15 Tests, alle grün – 4 einfache plus 11 aus den beiden parametrisierten Tests.

```
TicTacToeTest > dummyJunit() PASSED
TicTacToeTest > dummyAssertJ() PASSED
TicTacToeTest > crossDoesNotWinOnTheDrawBoard() PASSED
TicTacToeTest > circleDoesNotWinOnTheDrawBoard() PASSED
TicTacToeTest > detectsWinningLine(String, Stone) > "XXX ... ..." gewonnen von "CROSS" PASSED
TicTacToeTest > detectsWinningLine(String, Stone) > "... OOO ..." gewonnen von "CIRCLE" PASSED
TicTacToeTest > detectsWinningLine(String, Stone) > "... ... XXX" gewonnen von "CROSS" PASSED
TicTacToeTest > detectsWinningLine(String, Stone) > "O.. O.. O.." gewonnen von "CIRCLE" PASSED
TicTacToeTest > detectsWinningLine(String, Stone) > ".X. .X. .X." gewonnen von "CROSS" PASSED
TicTacToeTest > detectsWinningLine(String, Stone) > "..O ..O ..O" gewonnen von "CIRCLE" PASSED
TicTacToeTest > detectsWinningLine(String, Stone) > "X.. .X. ..X" gewonnen von "CROSS" PASSED
TicTacToeTest > detectsWinningLine(String, Stone) > "..O .O. O.." gewonnen von "CIRCLE" PASSED
TicTacToeTest > detectsNoWinningLine(String, Stone) > "... ... ..." nicht gewonnen von "CROSS" PASSED
TicTacToeTest > detectsNoWinningLine(String, Stone) > "XX. OO. ..." nicht gewonnen von "CROSS" PASSED
TicTacToeTest > detectsNoWinningLine(String, Stone) > "XXX ... ..." nicht gewonnen von "CIRCLE" PASSED

BUILD SUCCESSFUL
```

## Screenshot
