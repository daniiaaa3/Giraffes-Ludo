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

    // Preserve the existing movement validation.
    if (!game.isLegalWithDice(piece, steps, specialDieValue)) {
        throw new IllegalArgumentException(
                "That piece cannot move with the current dice roll.");
    }

    // FR-5.7: Only pieces selected by the Double Block
    // rules may be moved.
    boolean selectable;

    if (specialDieValue == 0) {
        selectable = game.getLegalPieces(player, steps)
                .contains(piece);
    } else {
        selectable = game.getLegalPieces(
                player, steps, specialDieValue)
                .contains(piece);
    }

    if (!selectable) {
        throw new IllegalArgumentException(
                "Another piece must be selected due to Double Block rules.");
    }
}


    @Override
    void afterValidation() {
        outcome = game.consumePendingRoll(player);
    }

@Override
int destination(Piece piece) {

    // Preserve the existing castle release behavior.
    if (piece.isInCastle()) {
        game.requireReleaseOnStartSquare(
                piece.getColor(), steps);
        return 0;
    }

    // Preserve the original movement calculation,
    // including Boost and UltraBoost dice.
    int target = piece.getProgress()
            + game.movementDistance(
                    piece, steps, specialDieValue);

    // Check whether an opponent Double Block
    // prevents reaching the requested destination.
    DoubleBlockRules.MoveDecision decision =
            DoubleBlockGameAdapter.evaluateMovement(
                    game, piece, target, false);

    if (decision.status()
            == DoubleBlockRules.MoveStatus.FULL_MOVE
            || decision.status()
            == DoubleBlockRules.MoveStatus.STOP_BEFORE_BLOCK) {

        return decision.destinationIndex();
    }

    throw new IllegalStateException(
            "The selected piece cannot move due to Double Block rules.");
}

Outcome outcome() {
    return outcome;
}
}
