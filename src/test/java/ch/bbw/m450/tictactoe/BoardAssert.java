package ch.bbw.m450.tictactoe;

import java.util.Arrays;

import org.assertj.core.api.AbstractAssert;

import ch.bbw.m450.tictactoe.TicTacToePlayer.Stone;

/**
 * Eigene AssertJ-Assertion für ein Spielfeld. Statt
 * {@code assertThat(TicTacToeMain.isWin(board, CROSS)).isTrue()} heisst es
 * {@code assertThatBoard("XXX ... ...").isWonBy(CROSS)}.
 */
public final class BoardAssert extends AbstractAssert<BoardAssert, Stone[]> {

	private BoardAssert(Stone[] board) {
		super(board, BoardAssert.class);
	}

	public static BoardAssert assertThatBoard(Stone[] board) {
		return new BoardAssert(board);
	}

	public static BoardAssert assertThatBoard(String sketch) {
		return new BoardAssert(Boards.boardOf(sketch));
	}

	public BoardAssert isWonBy(Stone color) {
		if (!TicTacToeMain.isWin(actual, color)) {
			failWithMessage("Erwartet: %s gewinnt auf %s", color, Arrays.toString(actual));
		}
		return this;
	}

	public BoardAssert isNotWonBy(Stone color) {
		if (TicTacToeMain.isWin(actual, color)) {
			failWithMessage("Erwartet: %s gewinnt nicht auf %s", color, Arrays.toString(actual));
		}
		return this;
	}

	public BoardAssert hasNoWinner() {
		return isNotWonBy(Stone.CROSS).isNotWonBy(Stone.CIRCLE);
	}
}
