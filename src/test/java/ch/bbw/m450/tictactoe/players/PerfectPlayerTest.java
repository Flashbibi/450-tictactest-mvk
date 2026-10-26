package ch.bbw.m450.tictactoe.players;

import static ch.bbw.m450.tictactoe.Boards.boardOf;

import java.util.Arrays;
import java.util.Objects;

import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junitpioneer.jupiter.StdIo;
import org.junitpioneer.jupiter.StdOut;

import ch.bbw.m450.tictactoe.TicTacToeMain;
import ch.bbw.m450.tictactoe.TicTacToePlayer.Stone;

class PerfectPlayerTest implements WithAssertions {

	private final PerfectPlayer perfect = new PerfectPlayer();

	@ParameterizedTest(name = "{3}: {0} -> Feld {2}")
	@CsvSource(delimiter = '|', textBlock = """
			XX. OO. ... | CROSS  | 2 | gewinnt sofort
			XX. OO. X.. | CIRCLE | 5 | gewinnt lieber, als zu blocken
			XX. .O. ... | CIRCLE | 2 | blockt die offene Zeile
			X.. .X. O.O | CROSS  | 7 | blockt die untere Zeile
			OX. XO. ... | CROSS  | 8 | blockt die Diagonale
			X.. ... ... | CIRCLE | 4 | antwortet auf eine Ecke mit der Mitte
			X.. .O. ..X | CIRCLE | 1 | verteidigt gegenüberliegende Ecken mit einer Kante
			""")
	void findsTheBestMove(String sketch, Stone color, int expected, String reason) {
		var move = perfect.play(boardOf(sketch), color);

		assertThat(move).as(reason).isEqualTo(expected);
	}

	@Test
	void takesTheLowestFieldAmongEquallyGoodMoves() {
		// auf dem leeren Brett führt jeder Zug zu einem Unentschieden
		var move = perfect.play(boardOf("... ... ..."), Stone.CROSS);

		assertThat(move).isZero();
	}

	@Test
	void givesUpOnAFullBoard() {
		var full = boardOf("XOX XOO OXX");

		assertThatThrownBy(() -> perfect.play(full, Stone.CROSS))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("cannot play at all");
	}

	/** Der Gegner probiert an jedem Zug jedes freie Feld aus. Keine dieser Partien darf verloren gehen. */
	@ParameterizedTest(name = "als {0}")
	@EnumSource(Stone.class)
	void neverLosesAgainstAnyOpponent(Stone perfectColor) {
		var losses = lossesInAllGames(boardOf("... ... ..."), Stone.CROSS, perfectColor);

		assertThat(losses).isZero();
	}

	@Test
	@StdIo
	void drawsAgainstItself(StdOut out) {
		var winner = TicTacToeMain.play(new PerfectPlayer(), new PerfectPlayer());

		assertThat(winner).isNull();
		assertThat(out.capturedLines()).containsExactly("it's a draw!");
	}

	@Test
	@StdIo
	void beatsTheGreedyPlayer(StdOut out) {
		assertThat(TicTacToeMain.play(new PerfectPlayer(), new GreedyPlayer())).isEqualTo(Stone.CROSS);
		assertThat(TicTacToeMain.play(new GreedyPlayer(), new PerfectPlayer())).isEqualTo(Stone.CIRCLE);
		assertThat(out.capturedString())
				.contains("...and the winner is: CROSS")
				.contains("...and the winner is: CIRCLE");
	}

	/** Spielt von {@code board} aus jede mögliche Partie gegen den PerfectPlayer und zählt seine Niederlagen. */
	private int lossesInAllGames(Stone[] board, Stone toMove, Stone perfectColor) {
		if (TicTacToeMain.isWin(board, perfectColor.opponent())) {
			return 1;
		}
		if (TicTacToeMain.isWin(board, perfectColor) || Arrays.stream(board).noneMatch(Objects::isNull)) {
			return 0; // gewonnen oder unentschieden
		}
		if (toMove == perfectColor) {
			var move = perfect.play(board, toMove);
			return lossesInAllGames(withMove(board, move, toMove), toMove.opponent(), perfectColor);
		}
		var losses = 0;
		for (var field = 0; field < board.length; field++) {
			if (board[field] == null) {
				losses += lossesInAllGames(withMove(board, field, toMove), toMove.opponent(), perfectColor);
			}
		}
		return losses;
	}

	private static Stone[] withMove(Stone[] board, int field, Stone stone) {
		var next = Arrays.copyOf(board, board.length);
		next[field] = stone;
		return next;
	}
}
