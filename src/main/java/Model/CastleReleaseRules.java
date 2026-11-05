
package Model;

import Model.GameSetup.PlayerColor;

/**
 * Checks the rules for releasing a piece from its castle.
 *
 * FR-4.4, FR-5.3
 * MVC Component: Model
 * Iteration: 1
 */
public class CastleReleaseRules {

    private final EasyBoard board;

    public CastleReleaseRules(EasyBoard board) {
        if (board == null) {
            throw new IllegalArgumentException(
                    "Board cannot be null.");
        }

        this.board = board;
    }

    /**
     * A piece can leave its castle only when
     * the Steps die shows 6.
     */
    public boolean canRelease(
            int steps, boolean pieceInCastle) {

        if (steps < 1 || steps > 6) {
            throw new IllegalArgumentException(
                    "Steps die must be between 1 and 6.");
        }

        return steps == 6 && pieceInCastle;
    }

    /**
     * Returns the player's starting square
     * when castle release is allowed.
     */
    public int getReleaseSquare(
            PlayerColor color,
            int steps,
            boolean pieceInCastle) {

        if (color == null) {
            throw new IllegalArgumentException(
                    "Player color cannot be null.");
        }

        if (!canRelease(steps, pieceInCastle)) {
            throw new IllegalStateException(
                    "Piece cannot leave the castle.");
        }

        return board.getStartSquare(color);
    }
}
