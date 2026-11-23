package Model;

/**
 * Advances a piece after a correct educational answer.
 *
 * Unlike an ordinary ForwardMove, this move does not consume a roll,
 * switch turns or change the consecutive-six counter. It still uses
 * Marwa's PieceMove Template Method for placement, captures and recording.
 *
 * A question movement follows the usual exact-finish rule: if the bonus
 * would go beyond the finish, the piece stays where it is.
 *
 * MVC Component: Model
 * Iteration: 2
 */
final class QuestionForwardMove extends PieceMove {

    private final int steps;

    QuestionForwardMove(GameState game, int steps) {
        super(game);
        if (steps < 1 || steps > 3) {
            throw new IllegalArgumentException(
                    "Question forward movement must be between 1 and 3.");
        }
        this.steps = steps;
    }

    @Override
    void validate(Piece piece) {
        // This also checks that the piece belongs to this game.
        if (!game.canMoveBackward(piece)) {
            throw new IllegalArgumentException(
                    "Only a piece on the board can receive a question bonus.");
        }
    }


@Override
int destination(Piece piece) {

    int target = piece.getProgress() + steps;
    int finish = game.getFinishSteps(piece.getColor());

    // Preserve Yara's exact-finish rule.
    if (target > finish) {
        return piece.getProgress();
    }

    // FR-5.6: Check Double Blocks along
    // the question bonus movement path.
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
