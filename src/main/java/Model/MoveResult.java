package Model;

import java.util.Optional;

/**
 * What a completed move did, so controllers and views can show
 * it without re-deriving the rules.
 *
 * @param piece        the piece that moved
 * @param fromProgress its progress before the move
 *                     (-1 when it left the castle)
 * @param toProgress   its progress after the move
 * @param captured     the opponent piece sent back to its castle
 *                     by landing on it, if any (FR-5.4)
 *
 * MVC Component: Model
 * SRS Requirements: FR-5.4
 * Iteration: 2
 */
public record MoveResult(Piece piece,
                         int fromProgress,
                         int toProgress,
                         Optional<Piece> captured) {

    /**
     * @throws IllegalArgumentException if piece or captured is null
     */
    public MoveResult {

        if (piece == null || captured == null) {
            throw new IllegalArgumentException(
                    "Piece and captured piece cannot be null.");
        }
    }

    /** @return true if this move captured an opponent piece */
    public boolean isCapture() {
        return captured.isPresent();
    }
}
