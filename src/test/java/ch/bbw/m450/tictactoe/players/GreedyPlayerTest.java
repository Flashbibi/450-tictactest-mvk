package ch.bbw.m450.tictactoe.players;

import static ch.bbw.m450.tictactoe.Boards.boardOf;

import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ch.bbw.m450.tictactoe.TicTacToePlayer.Stone;

class GreedyPlayerTest implements WithAssertions {

	private final GreedyPlayer greedy = new GreedyPlayer();

	@ParameterizedTest(name = "{0} -> Feld {1}")
	@CsvSource({
			"... ... ..., 0",
			"XO. ... ..., 2",
			"XOX OXO ..., 6",
			"XOX OXO XO., 8"
	})
	void playsTheFirstFreeField(String sketch, int expected) {
		var move = greedy.play(boardOf(sketch), Stone.CIRCLE);

		assertThat(move).isEqualTo(expected);
	}

	@Test
	void givesUpOnAFullBoard() {
		var full = boardOf("XOX OXO OXO");

		assertThatThrownBy(() -> greedy.play(full, Stone.CROSS))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("cannot play at all");
	}
}
