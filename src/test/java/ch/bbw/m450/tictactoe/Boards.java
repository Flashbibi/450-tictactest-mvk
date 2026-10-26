package ch.bbw.m450.tictactoe;

import ch.bbw.m450.tictactoe.TicTacToePlayer.Stone;

/**
 * Baut ein Spielfeld aus einer lesbaren Skizze, z.B. {@code boardOf("XXO OOX XOX")}:
 * X = Kreuz, O = Kreis, . = leer. Leerzeichen trennen nur optisch die drei Zeilen.
 */
public final class Boards {

	private Boards() {
		// nur statische Hilfsmethoden
	}

	public static Stone[] boardOf(String sketch) {
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
}
