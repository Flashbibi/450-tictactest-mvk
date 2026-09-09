package ch.bbw.m450.tictactoe;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ch.bbw.m450.tictactoe.TicTacToePlayer.Stone;

class TicTacToeTest implements WithAssertions {

	private static final String DRAW_BOARD = "XXO OOX XOX";

	private Stone[] board;

	@BeforeEach
	void setUp() {
		board = boardOf(DRAW_BOARD);
	}

	/** Baut ein Board aus einer Skizze: X = Kreuz, O = Kreis, . = leer. */
	private static Stone[] boardOf(String sketch) {
		var fields = sketch.replace(" ", "");
		var board = new Stone[TicTacToeMain.BOARD_SIZE];
		for (var i = 0; i < board.length; i++) {
			board[i] = switch (fields.charAt(i)) {
				case 'X' -> Stone.CROSS;
				case 'O' -> Stone.CIRCLE;
				default -> null;
			};
		}
		return board;
	}

	@Test
	void dummyJunit() {
		assertFalse(false);
	}

	@Test
	void dummyAssertJ() {
		assertThat("TicTacToe").isNotBlank();
	}

	@Test
	void crossDoesNotWinOnTheDrawBoard() {
		assertThat(TicTacToeMain.isWin(board, Stone.CROSS)).isFalse();
	}

	@Test
	void circleDoesNotWinOnTheDrawBoard() {
		assertThat(TicTacToeMain.isWin(board, Stone.CIRCLE)).isFalse();
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
		assertThat(TicTacToeMain.isWin(boardOf(sketch), color)).isTrue();
	}

	@ParameterizedTest(name = "{0} nicht gewonnen von {1}")
	@CsvSource({
			"... ... ..., CROSS",
			"XX. OO. ..., CROSS",
			"XXX ... ..., CIRCLE"
	})
	void detectsNoWinningLine(String sketch, Stone color) {
		assertThat(TicTacToeMain.isWin(boardOf(sketch), color)).isFalse();
	}

//	@Test
//	void thisTestFails() {
//		assertFalse(true);
//	}
//
}
