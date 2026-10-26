package ch.bbw.m450.tictactoe;

import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ch.bbw.m450.tictactoe.TicTacToePlayer.Stone;

class TicTacToePlayerTest implements WithAssertions {

	@ParameterizedTest(name = "Gegner von {0} ist {1}")
	@CsvSource({
			"CROSS, CIRCLE",
			"CIRCLE, CROSS"
	})
	void opponentIsTheOtherColor(Stone stone, Stone opponent) {
		assertThat(stone.opponent()).isEqualTo(opponent);
	}
}
