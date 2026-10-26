package ch.bbw.m450.tictactoe.players;

import java.util.ArrayList;
import java.util.Collections;

import ch.bbw.m450.tictactoe.TicTacToeMain;
import ch.bbw.m450.tictactoe.TicTacToePlayer;

/**
 * Perfect player using minimax: it tries every possible continuation of the game and therefore never loses.
 */
public class PerfectPlayer implements TicTacToePlayer {

	/** A win scores more than a game has moves, so every win counts more than a draw (0). */
	private static final int WIN = 10;

	@Override
	public int play(Stone[] board, Stone colorToPlay) {
		var bestMove = -1;
		var bestScore = Integer.MIN_VALUE;
		for (var field = 0; field < board.length; field++) {
			if (board[field] == null) {
				board[field] = colorToPlay; // try the move ...
				var score = minimax(board, colorToPlay, colorToPlay.opponent(), 1);
				board[field] = null; // ... and take it back
				if (score > bestScore) { // on equal scores the lower field stays
					bestScore = score;
					bestMove = field;
				}
			}
		}
		if (bestMove < 0) {
			throw new IllegalStateException("cannot play at all");
		}
		return bestMove;
	}

	/**
	 * Rates the board for {@code me}, assuming both players play perfectly from now on: positive if {@code me} wins,
	 * negative if {@code me} loses, 0 for a draw. The sooner a result is reached, the more it counts.
	 *
	 * @param toMove whose turn it is
	 * @param depth number of moves tried since the search started
	 */
	private static int minimax(Stone[] board, Stone me, Stone toMove, int depth) {
		if (TicTacToeMain.isWin(board, me)) {
			return WIN - depth;
		}
		if (TicTacToeMain.isWin(board, me.opponent())) {
			return depth - WIN;
		}
		var scores = new ArrayList<Integer>();
		for (var field = 0; field < board.length; field++) {
			if (board[field] == null) {
				board[field] = toMove;
				scores.add(minimax(board, me, toMove.opponent(), depth + 1));
				board[field] = null;
			}
		}
		if (scores.isEmpty()) {
			return 0; // board is full: draw
		}
		// I take the best move for me, the opponent takes the worst move for me
		return toMove == me ? Collections.max(scores) : Collections.min(scores);
	}
}
