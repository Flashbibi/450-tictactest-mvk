package ch.bbw.m450.tictactoe;

import static ch.bbw.m450.tictactoe.BoardAssert.assertThatBoard;
import static ch.bbw.m450.tictactoe.Boards.boardOf;

import java.util.Arrays;

import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import ch.bbw.m450.tictactoe.TicTacToePlayer.Stone;

class TicTacToeTest implements WithAssertions {

	private static final String DRAW_BOARD = "XXO OOX XOX";

	private Stone[] board;

	@BeforeEach
	void setUp() {
		board = boardOf(DRAW_BOARD);
	}

	// ---- isWin ----

	@ParameterizedTest(name = "{0} gewinnt nicht auf dem vollen Brett ohne Linie")
	@EnumSource(Stone.class)
	void nobodyWinsTheDrawBoard(Stone color) {
		assertThatBoard(board).isNotWonBy(color);
	}

	@ParameterizedTest(name = "{0} gewonnen von {1}")
	@CsvSource({
			"XXX ... ..., CROSS",
			"... OOO ..., CIRCLE",
			"... ... XXX, CROSS",
			"O.. O.. O.., CIRCLE",
			".X. .X. .X., CROSS",
			"..O ..O ..O, CIRCLE",
			"X.. .X. ..X, CROSS",
			"..O .O. O.., CIRCLE"
	})
	void detectsWinningLine(String sketch, Stone color) {
		assertThatBoard(sketch)
				.isWonBy(color)
				.isNotWonBy(color.opponent());
	}

	@ParameterizedTest(name = "{0} nicht gewonnen von {1}")
	@CsvSource({
			"... ... ..., CROSS",
			"XX. OO. ..., CROSS",
			"XXX ... ..., CIRCLE"
	})
	void detectsNoWinningLine(String sketch, Stone color) {
		assertThatBoard(sketch).isNotWonBy(color);
	}

	/** Zwei Steine einer Linie, der dritte fehlt. So wird jede Teilbedingung in isWin einmal falsch. */
	@ParameterizedTest(name = "{0} ist kein Sieg")
	@ValueSource(strings = {
			".XX ... ...", "X.X ... ...", "XX. ... ...", // obere Zeile
			"... .XX ...", "... X.X ...", "... XX. ...", // mittlere Zeile
			"... ... .XX", "... ... X.X", "... ... XX.", // untere Zeile
			"... X.. X..", "X.. ... X..", "X.. X.. ...", // linke Spalte
			"... .X. .X.", ".X. ... .X.", ".X. .X. ...", // mittlere Spalte
			"... ..X ..X", "..X ... ..X", "..X ..X ...", // rechte Spalte
			"... .X. ..X", "X.. ... ..X", "X.. .X. ...", // Diagonale von links oben
			"... .X. X..", "..X ... X..", "..X .X. ..."  // Diagonale von rechts oben
	})
	void twoInALineIsNoWin(String sketch) {
		assertThatBoard(sketch).hasNoWinner();
	}

	// ---- toString ----

	@Test
	void showsStonesAndTheNumbersOfFreeFields() {
		var text = TicTacToeMain.toString(boardOf("XO. ..X O.."));

		assertThat(withoutColors(text).lines())
				.containsExactly("X  O  2  ", "3  4  X  ", "O  7  8  ");
	}

	@Test
	void printsStonesBoldAndFreeFieldsGray() {
		var text = TicTacToeMain.toString(boardOf("XO. ... ..."));

		assertThat(text).startsWith("\033[1mX\033[0m  \033[1mO\033[0m  \033[37m2\033[0m  \n");
	}

	// ---- play ----

	@Test
	void crossWinsWithTheTopRow() {
		var winner = TicTacToeMain.play(playing(0, 1, 2), playing(3, 4));

		assertThat(winner).isEqualTo(Stone.CROSS);
	}

	@Test
	void circleWinsWithTheMiddleRow() {
		var winner = TicTacToeMain.play(playing(0, 1, 8), playing(3, 4, 5));

		assertThat(winner).isEqualTo(Stone.CIRCLE);
	}

	@Test
	void fullBoardWithoutLineIsADraw() {
		// ergibt das DRAW_BOARD XXO / OOX / XOX, ohne dass unterwegs jemand drei in einer Linie hat
		var winner = TicTacToeMain.play(playing(0, 8, 6, 5, 1), playing(4, 2, 3, 7));

		assertThat(winner).isNull();
	}

	@ParameterizedTest(name = "Zug auf Feld {0}")
	@ValueSource(ints = { -1, 9 })
	void rejectsMovesOutsideTheBoard(int field) {
		var x = playing(field);
		var o = playing();

		assertThatThrownBy(() -> TicTacToeMain.play(x, o))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("cannot play to position " + field);
	}

	@Test
	void rejectsMovesOnATakenField() {
		var x = playing(0);
		var o = playing(0);

		assertThatThrownBy(() -> TicTacToeMain.play(x, o))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("cannot play to position 0");
	}

	@Test
	void aPlayerCannotPlayAgainstItself() {
		var both = playing(0, 1, 2);

		assertThatThrownBy(() -> TicTacToeMain.play(both, both))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("players must differ");
	}

	// ---- Hilfsmethoden ----

	/** Ein Spieler, der die angegebenen Felder der Reihe nach spielt. Test-Double ohne Mocking-Framework. */
	private static TicTacToePlayer playing(int... moves) {
		var nextMove = Arrays.stream(moves).iterator();
		return (board, colorToPlay) -> nextMove.nextInt();
	}

	/** Entfernt die ANSI-Farbcodes, z.B. {@code \033[1m}. */
	private static String withoutColors(String text) {
		return text.replaceAll("\033\\[\\d+m", "");
	}
}
