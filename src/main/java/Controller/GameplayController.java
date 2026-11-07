package Controller;

import java.util.List;

import Model.GameSetup.PlayerEntry;
import Model.GameState;
import Model.Piece;
import Model.StepsDice;
import Model.TurnRollRules.Outcome;
import View.GameplayView;

/**
 * Connects the Gameplay UI with the game rules.
 *
 * The Model (GameState) decides everything: legal pieces, how far
 * a piece moves, bonus rolls and turn changes. This Controller
 * only passes user actions to the Model, plays the animations and
 * chooses the player-facing message.
 *
 * MVC Component: Controller
 * Iteration: 1
 */
public class GameplayController {

    private final GameplayView view;
    private final GameState gameState;
    private final StepsDice dice;

    /**
     * Creates the standard gameplay controller without a
     * return-to-menu callback (for standalone UI launchers).
     *
     * @param view gameplay interface
     * @param gameState active game model
     * @param dice the game die
     */
    public GameplayController(
            GameplayView view,
            GameState gameState,
            StepsDice dice) {
        this(view, gameState, dice, null);
    }

    /**
     * Wires game controls and an optional application navigation
     * callback. The callback decides whether to leave the game.
     *
     * @param view gameplay interface
     * @param gameState active game model
     * @param dice the game die
     * @param onReturnToMenu request to leave, or null when unavailable
     */
    public GameplayController(
            GameplayView view,
            GameState gameState,
            StepsDice dice,
            Runnable onReturnToMenu) {

        if (view == null || gameState == null || dice == null) {
            throw new IllegalArgumentException(
                    "Gameplay components cannot be null");
        }

        this.view = view;
        this.gameState = gameState;
        this.dice = dice;

        view.getRollButton().setOnAction(
                event -> handleRoll());

        view.setOnPieceSelected(
                piece -> handlePieceSelection(piece));

        if (onReturnToMenu != null) {
            view.getMainMenuButton().setDisable(false);
            view.getMainMenuButton().setOnAction(
                    event -> onReturnToMenu.run());
        }

        refreshView();
        view.showGameMessage(
                gameState.getActivePlayer().name() + " goes first");
    }

    /**
     * Processes a Steps die roll.
     */
    private void handleRoll() {

        // A roll is ignored while a piece is still hopping.
        if (view.isAnimating()) {
            return;
        }

        if (!gameState.isAwaitingRoll()) {
            view.showGameMessage("Choose a highlighted piece first");
            return;
        }

        PlayerEntry player = gameState.getActivePlayer();
        int result = dice.roll();

        view.showExtraRoll(false);

        view.animateDiceRoll(result, () -> {

            // Check move availability before the roll changes
            // the active player or starts a bonus turn.
            boolean hasLegalMove =
                    gameState.hasLegalMove(player, result);

            Outcome outcome =
                    gameState.recordRoll(player, result);

            view.setDiceResult(result);
            handleRollOutcome(outcome, player, hasLegalMove, result);

            refreshView();
        });
    }

    /**
     * Moves the selected legal piece, one square at a time.
     */
    private void handlePieceSelection(Piece piece) {

        // Ignore clicks while a piece is already moving, so a
        // double-click can never move a piece twice.
        if (view.isAnimating()) {
            return;
        }

        if (!gameState.isAwaitingMove()) {
            return;
        }

        if (!gameState.getSelectablePieces().contains(piece)) {
            return;
        }

        PlayerEntry player = gameState.getActivePlayer();
        int fromProgress = piece.getProgress();

        // The Model moves the piece immediately and exactly.
        Outcome outcome = gameState.movePiece(player, piece);

        // Lock input until the hop animation has finished.
        view.setSelectablePieces(List.of());
        view.setRollEnabled(false);

        view.animatePieceMove(gameState, piece, fromProgress, () -> {
            handleOutcome(outcome);
            refreshView();
        });
    }

    /**
     * Handles a die-roll outcome, including FR-6.5 when
     * the player has no legal move available.
     *
     * The Model remains responsible for advancing turns
     * and granting bonus rolls. This Controller only
     * selects the appropriate player-facing message.
     *
     * @param outcome result produced by the GameState
     * @param roller the player who rolled the die
     * @param hasLegalMove whether that roll could move a piece
     * @param rolled the die value
     */
    private void handleRollOutcome(
            Outcome outcome,
            PlayerEntry roller,
            boolean hasLegalMove,
            int rolled) {

        // A third consecutive six always forfeits the turn.
        if (outcome == Outcome.THIRD_SIX_FORFEIT) {
            handleOutcome(outcome);
            return;
        }

        if (!hasLegalMove) {
            if (outcome == Outcome.TURN_PASSED) {
                view.showExtraRoll(false);
                view.showGameMessage(noMoveMessage(roller, rolled));
                return;
            }

            if (outcome == Outcome.BONUS_ROLL) {
                view.showExtraRoll(true);
                view.showGameMessage(
                        "No available moves for " + roller.name()
                        + ", but a 6 earns an extra roll");
                return;
            }
        }

        if (outcome == Outcome.MOVE_REQUIRED && rolled == 6
                && hasPieceInCastle(roller)) {
            view.showGameMessage(
                    "You rolled a 6, so choose a piece to release or move");
            return;
        }

        handleOutcome(outcome);
    }

    private String noMoveMessage(PlayerEntry roller, int rolled) {
        String next = gameState.getActivePlayer().name();

        if (rolled != 6 && !hasPieceOnBoard(roller)) {
            return roller.name() + " needs a 6 to release a piece, so it is "
                    + next + "'s turn";
        }

        return "No available moves for " + roller.name()
                + ", so it is " + next + "'s turn";
    }

    private boolean hasPieceInCastle(PlayerEntry player) {
        return gameState.getPieces(player).stream()
                .anyMatch(Piece::isInCastle);
    }

    private boolean hasPieceOnBoard(PlayerEntry player) {
        return gameState.getPieces(player).stream()
                .anyMatch(p -> !p.isInCastle() && !p.isFinished());
    }

    /**
     * Displays the appropriate game feedback for ordinary
     * move results and special turn outcomes.
     *
     * @param outcome the turn rule outcome
     */
    private void handleOutcome(Outcome outcome) {

        view.showExtraRoll(outcome == Outcome.BONUS_ROLL);

        switch (outcome) {

            case MOVE_REQUIRED -> view.showGameMessage(
                    "Choose a highlighted piece to move");

            case BONUS_ROLL -> view.showGameMessage(
                    "Extra roll! Roll the dice again");

            case TURN_PASSED -> view.showGameMessage(
                    gameState.getActivePlayer().name() + "'s turn");

            case THIRD_SIX_FORFEIT -> view.showGameMessage(
                    "Three sixes in a row, so the turn is lost. "
                    + gameState.getActivePlayer().name()
                    + " plays next");
        }
    }

    /**
     * Synchronizes the UI with the current model state.
     */
    private void refreshView() {

        view.setCurrentPlayer(
                gameState.getActivePlayer().name());

        view.setSelectablePieces(
                gameState.getSelectablePieces());

        view.setRollEnabled(gameState.isAwaitingRoll());

        view.updateBoard(gameState);
    }
}
