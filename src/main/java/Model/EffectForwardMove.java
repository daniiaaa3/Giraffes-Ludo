package Model;

/**
 * A piece moved forward by an effect, such as a correct question answer
 * (FR-7.6). Concrete class of the Template Method pattern (see
 * {@link PieceMove}).
 *
 * What is specific to this move:
 * - validation: the steps cannot be negative, and the piece must be on
 *   the board and not go beyond the finish square
 * - no hook: it is not a turn action, so the active player, the pending
 *   roll and the six counter are never touched
 * - destination: the current progress plus the steps
 *
 * Capture, protected squares, the state of the piece and the result come
 * from the shared template, so this move treats landings like every
 * other move. Not handled here (other owners): Double Blocks and
 * question or surprise events on the landing square.
 *
 * MVC Component: Model
 * SRS Requirements: FR-7.6, FR-5.4 and FR-3.4 (through the template)
 * Iteration: 2
 */
final class EffectForwardMove extends PieceMove {

    private final int steps;

    /**
     * @param game  the game
     * @param steps how many steps forward; 0 or more
     * @throws IllegalArgumentException if steps is negative
     */
    EffectForwardMove(GameState game, int steps) {
        super(game);

        if (steps < 0) {
            throw new IllegalArgumentException(
                    "Steps cannot be negative.");
        }

        this.steps = steps;
    }

    @Override
    void validate(Piece piece) {

        if (!game.canMoveForward(piece, steps)) {
            throw new IllegalArgumentException(
                    "That piece cannot move forward " + steps
                    + " steps.");
        }
    }


@Override
int destination(Piece piece) {

    // Preserve zero-step movement behavior.
    if (steps == 0) {
        return piece.getProgress();
    }

    int target = piece.getProgress() + steps;

    // Stop before an opponent's Double Block.
    DoubleBlockRules.MoveDecision decision =
            DoubleBlockGameAdapter.evaluateMovement(
                    game,
                    piece,
                    target,
                    false
            );

    return decision.destinationIndex();
}

}
