package ch.bbw.m450.tictactoe;

import static ch.bbw.m450.tictactoe.Boards.boardOf;

import java.util.NoSuchElementException;

import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.Timeout.ThreadMode;
import org.junitpioneer.jupiter.StdIo;
import org.junitpioneer.jupiter.StdOut;

/**
 * End-to-End-Tests: ein ganzes Spiel über {@code main(...)}, so wie es ein Benutzer startet. JUnit Pioneer ersetzt die
 * Tastatur ({@code @StdIo}) und fängt die Konsolenausgabe ab ({@link StdOut}). Der Mensch spielt X, der GreedyPlayer
 * spielt O und nimmt immer das erste freie Feld. Die Tests laufen im eigenen Gradle-Task {@code e2eTest}.
 *
 * <p>Schutz vor Denial of Service: Bleibt ein Test hängen, zum Beispiel weil das Spiel auf eine Eingabe wartet, die nie
 * kommt, bricht JUnit ihn nach 5 Sekunden ab. Dafür läuft jeder Test in einem eigenen Thread.
 */
@Tag("e2e")
@Timeout(value = 5, threadMode = ThreadMode.SEPARATE_THREAD)
class TicTacToeE2ETest implements WithAssertions {

	private static final String[] NO_ARGS = {};

	// ---- ganze Spiele ----

	// E2E-01
	@Test
	@StdIo({ "3", "4", "5" })
	void crossWinsAGame(StdOut out) {
		TicTacToeMain.main(NO_ARGS);

		assertThat(out.capturedString())
				.endsWith(TicTacToeMain.toString(boardOf("OO. XXX ...")) + "...and the winner is: CROSS" + System.lineSeparator());
	}

	// E2E-02
	@Test
	@StdIo({ "8", "7", "5" })
	void circleWinsAGame(StdOut out) {
		TicTacToeMain.main(NO_ARGS);

		assertThat(out.capturedString())
				.endsWith(TicTacToeMain.toString(boardOf("OOO ..X .XX")) + "...and the winner is: CIRCLE" + System.lineSeparator());
	}

	// E2E-03
	@Test
	@StdIo({ "1", "3", "4", "6", "8" })
	void gameEndsInADraw(StdOut out) {
		TicTacToeMain.main(NO_ARGS);

		assertThat(out.capturedString()).endsWith("it's a draw!" + System.lineSeparator());
	}

	// ---- ungültige und böswillige Eingaben ----

	// E2E-04
	@Test
	@StdIo("vier")
	void stopsOnTextInsteadOfANumber() {
		assertThatThrownBy(() -> TicTacToeMain.main(NO_ARGS))
				.isInstanceOf(NumberFormatException.class);
	}

	// E2E-05
	@Test
	@StdIo("9")
	void stopsOnAFieldOutsideTheBoard() {
		assertThatThrownBy(() -> TicTacToeMain.main(NO_ARGS))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("cannot play to position 9");
	}

	// E2E-06
	@Test
	@StdIo({ "4", "0" })
	void stopsOnAFieldThatIsTaken() {
		// nach dem ersten Zug hat der GreedyPlayer Feld 0 genommen
		assertThatThrownBy(() -> TicTacToeMain.main(NO_ARGS))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("cannot play to position 0");
	}

	// E2E-07
	@Test
	@StdIo("\033[2J")
	void doesNotPrintTerminalControlCharacters(StdOut out) {
		// ESC[2J würde das Terminal leeren, wenn das Spiel die Eingabe wieder ausgeben würde
		assertThatThrownBy(() -> TicTacToeMain.main(NO_ARGS))
				.isInstanceOf(NumberFormatException.class);
		assertThat(out.capturedString()).doesNotContain("\033[2J");
	}

	// ---- Denial of Service ----

	// E2E-08
	@Test
	@StdIo("4")
	void stopsInsteadOfWaitingWhenTheInputEnds() {
		// nach dem ersten Zug kommt keine Eingabe mehr (wie bei Ctrl+D): das Spiel darf nicht ewig warten
		assertThatThrownBy(() -> TicTacToeMain.main(NO_ARGS))
				.isInstanceOf(NoSuchElementException.class);
	}
}
