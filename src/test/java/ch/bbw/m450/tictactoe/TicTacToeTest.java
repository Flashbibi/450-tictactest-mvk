package ch.bbw.m450.tictactoe;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.Test;

import ch.bbw.m450.tictactoe.TicTacToePlayer.Stone;
import ch.bbw.m450.tictactoe.players.GreedyPlayer;

class TicTacToeTest implements WithAssertions {

	@Test
	void dummyJunit() {
		assertFalse(false);
	}

	@Test
	void dummyAssertJ() {
		assertThat("TicTacToe").isNotBlank();
	}

	@Test
	void xWinsWithTopRow() {
		var board = new Stone[] {Stone.CROSS, Stone.CROSS, Stone.CROSS, null, null, null, null, null, null};

		assertThat(TicTacToeMain.isWin(board, Stone.CROSS)).isTrue();
	}

	@Test
	void greedyPlayerPlaysFirstFreeField() {
		var board = new Stone[] {Stone.CROSS, Stone.CIRCLE, null, null, null, null, null, null, null};

		assertThat(new GreedyPlayer().play(board, Stone.CROSS)).isEqualTo(2);
	}

	@Test
	void thisTestFails() {
		assertFalse(true);
	}
}
