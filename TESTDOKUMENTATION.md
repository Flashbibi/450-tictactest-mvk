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

Ausführen: `./gradlew test --rerun-tasks`

## Test-Code auf GitHub

<https://github.com/Flashbibi/450-tictactest-mvk/blob/main/src/test/java/ch/bbw/m450/tictactoe/TicTacToeTest.java>

## Helper und Fixture

- **Helper** `boardOf(sketch)` – baut ein `Stone[]` aus einer lesbaren Skizze wie `"XXX ... ..."` (`X` = Kreuz, `O` = Kreis, `.` = leer).
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

Zusätzlich steht `thisTestFails` auskommentiert im File. Er stammt aus dem vorherigen Auftrag (Screenshot eines Fehlschlags) und ist bewusst deaktiviert, damit die Suite grün bleibt.

## Ergebnis

```
TicTacToeTest > dummyJunit() PASSED
TicTacToeTest > dummyAssertJ() PASSED
TicTacToeTest > crossDoesNotWinOnTheDrawBoard() PASSED
TicTacToeTest > circleDoesNotWinOnTheDrawBoard() PASSED

BUILD SUCCESSFUL
```

## Screenshot
