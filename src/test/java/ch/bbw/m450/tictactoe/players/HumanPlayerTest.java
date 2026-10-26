package ch.bbw.m450.tictactoe.players;

import static ch.bbw.m450.tictactoe.Boards.boardOf;

import java.util.NoSuchElementException;

import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.StdIo;
import org.junitpioneer.jupiter.StdOut;

import ch.bbw.m450.tictactoe.TicTacToePlayer.Stone;

/**
 * StdIn/StdOut-Tests mit JUnit Pioneer: {@code @StdIo("4")} ersetzt die Tastatur durch die Eingabe "4",
 * ein Parameter vom Typ {@link StdOut} fängt die Konsolenausgabe ab.
 */
class HumanPlayerTest implements WithAssertions {

	private final HumanPlayer human = new HumanPlayer();

	@Test
	@StdIo("4")
	void playsTheFieldTypedOnTheKeyboard() {
		var move = human.play(boardOf("... ... ..."), Stone.CROSS);

		assertThat(move).isEqualTo(4);
	}

	@Test
	@StdIo("7")
	void showsTheBoardAndAsksForTheNextMove(StdOut out) {
		human.play(boardOf("X.. .O. ..."), Stone.CROSS);

		var shown = out.capturedString().replaceAll("\033\\[\\d+m", ""); // ohne ANSI-Farbcodes
		assertThat(shown.lines())
				.containsExactly("X  1  2  ", "3  O  5  ", "6  7  8  ", "where to to put the next CROSS? (0-8): ");
	}

	@Test
	@StdIo("vier")
	void crashesOnInputThatIsNotANumber() {
		var board = boardOf("... ... ...");

		assertThatThrownBy(() -> human.play(board, Stone.CROSS))
				.isInstanceOf(NumberFormatException.class)
				.hasMessageContaining("vier");
	}

	/**
	 * Hält einen Fehler im Originalcode fest: HumanPlayer erstellt bei jedem Zug einen neuen Scanner. Der erste
	 * Scanner liest mehr als eine Zeile in seinen Puffer, deshalb findet der zweite keine Eingabe mehr. An der
	 * Tastatur fällt das nicht auf, weil jede Zeile einzeln kommt. Der Originalcode soll nicht verändert werden,
	 * deshalb ist der Fehler nur dokumentiert, nicht behoben.
	 */
	@Test
	@StdIo({ "2", "6" })
	void losesTheSecondMoveWhenAllInputArrivesAtOnce() {
		var first = human.play(boardOf("... ... ..."), Stone.CIRCLE);
		var board = boardOf("..O ... ...");

		assertThat(first).isEqualTo(2);
		assertThatThrownBy(() -> human.play(board, Stone.CIRCLE))
				.isInstanceOf(NoSuchElementException.class);
	}
}
