package Model;

/**
 * A piece moved backward by a question or surprise effect (FR-5.9).
 * Concrete class of the Template Method pattern (see
 * {@link PieceMove}).
 *
 * What is specific to going backward:
 * - validation: the steps cannot be negative and the piece must be on
 *   the board (not in the castle, not finished)
 * - no hook: it is not a turn action, so the active player, the
 *   pending roll and the six counter are never touched
 * - destination: back along the path, never before the start square
 *
 * ASSUMPTION (the SRS does not define backward movement from the
 * home path): the home path is the continuation of the piece's path,
 * so a piece on it walks back along it and, if the steps allow, onto
 * the shared track through its arrow square. This is decided only in
 * {@link #destination}.
 *
 * Not handled here (other owners): Double Blocks on the way back
 * (FR-5.6, FR-5.7).
 *
 * MVC Component: Model
 * SRS Requirements: FR-5.9, FR-5.4 (through the shared template)
 * Iteration: 2
 */
final class BackwardMove extends PieceMove {

    private final int steps;

    /**
     * @param game  the game
     * @param steps how many steps back; 0 or more
     * @throws IllegalArgumentException if steps is negative
     */
    BackwardMove(GameState game, int steps) {
        super(game);

        if (steps < 0) {
            throw new IllegalArgumentException(
                    "Steps cannot be negative.");
        }

        this.steps = steps;
    }

    @Override
    void validate(Piece piece) {

        if (!game.canMoveBackward(piece)) {
            throw new IllegalArgumentException(
                    "Only a piece on the board can move backward.");
        }
    }

    /**
     * Progress counts steps from the start square (0), so going back
     * is subtracting, and FR-5.9 is the floor at 0. Home-path
     * progress values are larger than the arrow's, so subtracting
     * also covers the undefined home-path case described above.
     */
    @Override
    int destination(Piece piece) {
        return Math.max(piece.getProgress() - steps, 0);
    }
}
