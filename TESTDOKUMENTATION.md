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
- **Fixture** `@BeforeEach setUp()` – legt vor jedem Test ein frisches Board an, das sich mehrere Tests teilen, ohne sich gegenseitig zu beeinflussen.
- Die Skizze steht als benannte Konstante `X_WINS_TOP_ROW` am Klassenanfang.

Alle Tests sind reine Unit-Tests: Sie prüfen `TicTacToeMain.isWin(...)` bzw. die Frameworks selbst, ohne weitere Klassen einzubinden.

## Tests (GIVEN_WHEN_THEN)

**1. `dummyJunit`** – Dummy-Test für JUnit
- **GIVEN** der Wert `false`
- **WHEN** JUnit ihn mit `assertFalse(false)` prüft
- **THEN** läuft der Test durch – JUnit ist eingebunden

**2. `dummyAssertJ`** – Dummy-Test für AssertJ
- **GIVEN** der Text `"TicTacToe"`
- **WHEN** AssertJ ihn mit `assertThat(text).isNotBlank()` prüft
- **THEN** läuft der Test durch – AssertJ ist eingebunden

**3. `xWinsWithTopRow`**
- **GIVEN** das Fixture-Board `XXX / ... / ...`
- **WHEN** `TicTacToeMain.isWin(board, CROSS)` aufgerufen wird
- **THEN** ist das Ergebnis `true`

**4. `oDoesNotWinOnTheSameBoard`**
- **GIVEN** dasselbe Fixture-Board `XXX / ... / ...`
- **WHEN** `TicTacToeMain.isWin(board, CIRCLE)` aufgerufen wird
- **THEN** ist das Ergebnis `false` – die Siegerkennung ist farbabhängig

**5. `thisTestFails`** – schlägt absichtlich fehl
- **GIVEN** der Wert `true`
- **WHEN** JUnit mit `assertFalse(true)` prüft, ob er falsch ist
- **THEN** schlägt der Test fehl

## Ergebnis

```
TicTacToeTest > dummyAssertJ() PASSED
TicTacToeTest > oDoesNotWinOnTheSameBoard() PASSED
TicTacToeTest > dummyJunit() PASSED
TicTacToeTest > xWinsWithTopRow() PASSED
TicTacToeTest > thisTestFails() FAILED

5 tests completed, 1 failed
```

## Screenshot
