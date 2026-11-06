
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

    public GameplayController(
            GameplayView view,
            GameState gameState,
            StepsDice dice) {

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

            // Apply the real result after the animation.
            Outcome outcome =
                    gameState.recordRoll(player, result);

            view.setDiceResult(result);
            handleOutcome(outcome);

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
     * Displays the appropriate game feedback.
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
