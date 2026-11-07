
package Controller;

import Model.GameState;
import Model.Piece;
import Model.StepsDice;
import Model.GameSetup.PlayerEntry;
import Model.TurnRollRules.Outcome;
import View.GameplayView;

/**
 * Connects the Gameplay UI with the game rules.
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
                    "Gameplay components cannot be null.");
        }

        this.view = view;
        this.gameState = gameState;
        this.dice = dice;

        // Connect UI actions to the controller.
        view.getRollButton().setOnAction(
                event -> handleRoll());

        view.setOnPieceSelected(
                piece -> handlePieceSelection(piece));

        if (onReturnToMenu != null) {
            view.getMainMenuButton().setDisable(false);
            view.getMainMenuButton().setOnAction(
                    event -> onReturnToMenu.run());
        }

        // Display the initial game state.
        refreshView();
        view.showGameMessage(
                gameState.getActivePlayer().name()
                + " starts the game!");
    }

    /**
     * Processes a Steps die roll.
     */
    private void handleRoll() {

        if (!gameState.isAwaitingRoll()) {
            view.showGameMessage(
                    "Select a highlighted piece first.");
            return;
        }

        PlayerEntry player = gameState.getActivePlayer();
        int result = dice.roll();

        // Play the animation first.
        view.animateDiceRoll(result, () -> {

            // Check move availability before the roll changes
            // the active player or starts a bonus turn.
            boolean hasLegalMove =
                    gameState.hasLegalMove(player, result);

            Outcome outcome =
                    gameState.recordRoll(player, result);

            view.setDiceResult(result);
            handleRollOutcome(outcome, player, hasLegalMove);

            // Refresh the board and button state.
            refreshView();
        });
    }

    /**
     * Moves the selected legal piece.
     */
    private void handlePieceSelection(Piece piece) {

        if (!gameState.isAwaitingMove()) {
            return;
        }

        if (!gameState.getSelectablePieces().contains(piece)) {
            return;
        }

        PlayerEntry player = gameState.getActivePlayer();

        Outcome outcome = gameState.movePiece(player, piece);

        handleOutcome(outcome);
        refreshView();
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
     */
    private void handleRollOutcome(
            Outcome outcome,
            PlayerEntry roller,
            boolean hasLegalMove) {

        // A third consecutive six always forfeits the turn.
        // It must not be displayed as an ordinary no-move roll.
        if (outcome == Outcome.THIRD_SIX_FORFEIT) {
            handleOutcome(outcome);
            return;
        }

        if (!hasLegalMove) {
            if (outcome == Outcome.TURN_PASSED) {
                view.showGameMessage(
                        "No available moves for " + roller.name()
                        + ". "
                        + gameState.getActivePlayer().name()
                        + " plays next.");
                return;
            }

            if (outcome == Outcome.BONUS_ROLL) {
                view.showGameMessage(
                        "No available moves for " + roller.name()
                        + ". Bonus roll! Roll again.");
                return;
            }
        }

        handleOutcome(outcome);
    }

    /**
     * Displays the appropriate game feedback for ordinary
     * move results and special turn outcomes.
     *
     * @param outcome the turn rule outcome
     */
    private void handleOutcome(Outcome outcome) {

        switch (outcome) {

            case MOVE_REQUIRED -> {
                view.showGameMessage(
                        "Select a highlighted piece to move.");
            }

            case BONUS_ROLL -> {
                view.showGameMessage(
                        "Bonus roll! Roll the dice again.");
            }

            case TURN_PASSED -> {
                view.showGameMessage(
                        "Turn passed to "
                        + gameState.getActivePlayer().name()
                        + ".");
            }

            case THIRD_SIX_FORFEIT -> {
                view.showGameMessage(
                        "Three consecutive sixes! "
                        + "Turn forfeited. "
                        + gameState.getActivePlayer().name()
                        + " plays next.");
            }
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
