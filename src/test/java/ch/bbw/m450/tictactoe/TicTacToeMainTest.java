package ch.bbw.m450.tictactoe;

import static ch.bbw.m450.tictactoe.TicTacToeMain.isWin;

import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.Test;

import ch.bbw.m450.tictactoe.TicTacToePlayer.Stone;

class TicTacToeMainTest implements WithAssertions {

	/**
	 * Hilfsstruktur fuers Testen: baut ein Board aus einem lesbaren Sketch wie {@code "XOO OX. XOX"},
	 * wobei {@code X} ein Kreuz, {@code O} einen Kreis und {@code .} ein leeres Feld bedeutet.
	 * Leerzeichen trennen nur die Zeilen und werden ignoriert.
	 */
	static Stone[] toBoard(String sketch) {
		var fields = sketch.replace(" ", "");
		if (fields.length() != TicTacToeMain.BOARD_SIZE) {
			throw new IllegalArgumentException("ein Board braucht genau " + TicTacToeMain.BOARD_SIZE
					+ " Felder, erhalten: " + fields.length());
		}
		var board = new Stone[TicTacToeMain.BOARD_SIZE];
		for (var i = 0; i < board.length; i++) {
			board[i] = switch (fields.charAt(i)) {
				case 'X' -> Stone.CROSS;
				case 'O' -> Stone.CIRCLE;
				case '.' -> null;
				default -> throw new IllegalArgumentException("unerwartetes Feld: " + fields.charAt(i));
			};
		}
		return board;
	}

	@Test
	void isWinningDiagonaleForX() {
		assertThat(isWin(toBoard("XOO OX. XOX"), Stone.CROSS)).isTrue();
	}

	@Test
	void isNotWinningForOOnTheSameBoard() {
		assertThat(isWin(toBoard("XOO OX. XOX"), Stone.CIRCLE)).isFalse();
	}

	@Test
	void isWinningAntiDiagonaleForO() {
		assertThat(isWin(toBoard("XXO .O. OX."), Stone.CIRCLE)).isTrue();
	}

	@Test
	void isWinningRowForX() {
		assertThat(isWin(toBoard("XXX OO. ..."), Stone.CROSS)).isTrue();
	}

	@Test
	void isWinningColumnForO() {
		assertThat(isWin(toBoard("OXX O.X O.."), Stone.CIRCLE)).isTrue();
	}

	@Test
	void isNotWinningOnAnEmptyBoard() {
		assertThat(isWin(toBoard("... ... ..."), Stone.CROSS)).isFalse();
	}

	@Test
	void isNotWinningWithOnlyTwoInARow() {
		assertThat(isWin(toBoard("XX. OO. ..."), Stone.CROSS)).isFalse();
	}

	@Test
	void isNotWinningOnAFullDrawnBoard() {
		assertThat(isWin(toBoard("XXO OOX XOX"), Stone.CROSS)).isFalse();
	}

	/**
	 * Das Given-When-Then-Pattern als Gegenbeispiel: korrekt, aber deutlich laenger als die
	 * Einzeiler oben. Das Tutorial empfiehlt es bewusst nicht.
	 */
	@Test
	void givenWhenThenPatternJunit() {
		// given
		var boardWithDiagonal = toBoard("XOO OX. XOX");
		// when
		var winning = isWin(boardWithDiagonal, Stone.CROSS);
		// then
		assertThat(winning).isTrue();
	}
}
