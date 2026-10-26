package ch.bbw.m450.tictactoe.players;

import static ch.bbw.m450.tictactoe.Boards.boardOf;

import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.StdIo;
import org.junitpioneer.jupiter.StdOut;

import ch.bbw.m450.tictactoe.TicTacToePlayer.Stone;

/**
 * StdIn/StdOut-Tests mit JUnit Pioneer: {@code @StdIo("4")} ersetzt die Tastatur durch die Eingabe "4",
 * ein Parameter vom Typ {@link StdOut} fängt die Konsolenausgabe ab. Der Spieler wird erst im Test erzeugt, damit er
 * schon die ersetzte Tastatur liest.
 */
class HumanPlayerTest implements WithAssertions {

	@Test
	@StdIo("4")
	void playsTheFieldTypedOnTheKeyboard() {
		var human = new HumanPlayer();

		var move = human.play(boardOf("... ... ..."), Stone.CROSS);

		assertThat(move).isEqualTo(4);
	}

	@Test
	@StdIo("7")
	void showsTheBoardAndAsksForTheNextMove(StdOut out) {
		var human = new HumanPlayer();

		human.play(boardOf("X.. .O. ..."), Stone.CROSS);

		var shown = out.capturedString().replaceAll("\033\\[\\d+m", ""); // ohne ANSI-Farbcodes
		assertThat(shown.lines())
				.containsExactly("X  1  2  ", "3  O  5  ", "6  7  8  ", "where to put the next CROSS? (0-8): ");
	}

	@Test
	@StdIo("vier")
	void crashesOnInputThatIsNotANumber() {
		var human = new HumanPlayer();
		var board = boardOf("... ... ...");

		assertThatThrownBy(() -> human.play(board, Stone.CROSS))
				.isInstanceOf(NumberFormatException.class)
				.hasMessageContaining("vier");
	}

	/**
	 * Regressionstest für den Scanner-Fehler: Kommen mehrere Eingaben auf einmal, ging früher ab dem zweiten Zug die
	 * Eingabe verloren, weil HumanPlayer bei jedem Zug einen neuen Scanner erstellt hat.
	 */
	@Test
	@StdIo({ "2", "6" })
	void readsOneLinePerMove() {
		var human = new HumanPlayer();

		var first = human.play(boardOf("... ... ..."), Stone.CIRCLE);
		var second = human.play(boardOf("..O ... ..."), Stone.CIRCLE);

		assertThat(first).isEqualTo(2);
		assertThat(second).isEqualTo(6);
	}
}
