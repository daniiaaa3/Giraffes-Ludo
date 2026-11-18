package Model;

import Model.GameSetup.PlayerEntry;
import Model.TurnRollRules.Outcome;

/**
 * The forward move a player makes with the pending roll
 * (FR-5.1, FR-5.2). Concrete class of the Template Method pattern
 * (see {@link PieceMove}).
 *
 * What is specific to going forward:
 * - validation: the piece must be able to use the roll, including
 *   releasing from the castle on a six and an exact finish
 * - hook: it is a turn action, so the roll is used up and the turn
 *   rules decide what happens next
 * - destination: the start square for a release, otherwise the
 *   current progress plus the roll
 *
 * MVC Component: Model
 * SRS Requirements: FR-5.1, FR-5.2, FR-5.3, FR-4.4
 * Iteration: 1 (logic), 2 (restructured as a template subclass)
 */
final class ForwardMove extends PieceMove {

    private final PlayerEntry player;
    private final int steps;
    private Outcome outcome;

    ForwardMove(GameState game, PlayerEntry player, int steps) {
        super(game);
        this.player = player;
        this.steps = steps;
    }

    @Override
    void validate(Piece piece) {

        if (!game.isLegal(piece, steps)) {
            throw new IllegalArgumentException(
                    "That piece cannot move " + steps + " steps.");
        }
    }

    /** Nothing has changed yet; this is the last check. */
    @Override
    void afterValidation() {
        outcome = game.consumePendingRoll(player);
    }

    @Override
    int destination(Piece piece) {

        if (piece.isInCastle()) {
            // Release: the piece stands on its start square.
            game.requireReleaseOnStartSquare(piece.getColor(), steps);
            return 0;
        }

        return piece.getProgress() + steps;
    }

    /** @return what the turn rules decided, once executed */
    Outcome outcome() {
        return outcome;
    }
}
