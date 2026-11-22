package Model;

import Model.GameSetup.PlayerEntry;
import Model.TurnRollRules.Outcome;

/**
 * Forward movement action. Keeps Marwa's PieceMove Template Method:
 * validate -> consume pending roll -> destination -> update game state.
 *
 * Steps controls castle release and turn rules; a special die controls
 * movement on shared tracks only. The home path uses Steps alone.
 */
final class ForwardMove extends PieceMove {

    private final PlayerEntry player;
    private final int steps;
    private final int specialDieValue;
    private Outcome outcome;

    // Keep the original constructor for backward compatibility.
    ForwardMove(GameState game, PlayerEntry player, int steps) {
        this(game, player, steps, 0);
    }

    ForwardMove(GameState game, PlayerEntry player,
            int steps, int specialDieValue) {
        super(game);
        this.player = player;
        this.steps = steps;
        this.specialDieValue = specialDieValue;
    }

    @Override
    void validate(Piece piece) {
        if (!game.isLegalWithDice(piece, steps, specialDieValue)) {
            throw new IllegalArgumentException(
                    "That piece cannot move with the current dice roll.");
        }
    }

    @Override
    void afterValidation() {
        outcome = game.consumePendingRoll(player);
    }

    @Override
    int destination(Piece piece) {
        if (piece.isInCastle()) {
            // A regular Steps 6 releases the piece, regardless of Boost.
            game.requireReleaseOnStartSquare(piece.getColor(), steps);
            return 0;
        }
        return piece.getProgress()
                + game.movementDistance(piece, steps, specialDieValue);
    }

    Outcome outcome() {
        return outcome;
    }
}
