package Controller;

import java.util.List;

import Model.BoostDice;
import Model.GameDifficulty;
import Model.GameSetup.PlayerEntry;
import Model.GameState;
import Model.Piece;
import Model.StepsDice;
import Model.TurnRollRules.Outcome;
import Model.UltraBoostDice;
import View.GameplayView;

/**
 * Connects the gameplay UI to the game model.
 *
 * Iteration 2: supports the Steps die plus Boost (Medium) or UltraBoost (Hard).
 * Turn rules always receive only the original Steps die (1-6).
 * The model, not this controller, decides move legality and distance.
 *
 * MVC: Controller
 */
public class GameplayController {

    private final GameplayView view;
    private final GameState gameState;
    private final StepsDice dice;
    private final BoostDice boostDice;
    private final UltraBoostDice ultraBoostDice;

    /** Original constructor retained for existing tests and launchers. */
    public GameplayController(GameplayView view, GameState gameState,
            StepsDice dice) {
        this(view, gameState, dice, null);
    }

    /** Original constructor retained with optional return-to-menu support. */
    public GameplayController(GameplayView view, GameState gameState,
            StepsDice dice, Runnable onReturnToMenu) {
        if (view == null || gameState == null || dice == null) {
            throw new IllegalArgumentException(
                    "Gameplay components cannot be null");
        }

        this.view = view;
        this.gameState = gameState;
        this.dice = dice;
        this.boostDice = new BoostDice();
        this.ultraBoostDice = new UltraBoostDice();

        view.getRollButton().setOnAction(event -> handleRoll());
        view.setOnPieceSelected(this::handlePieceSelection);

        if (onReturnToMenu != null) {
            view.getMainMenuButton().setDisable(false);
            view.getMainMenuButton().setOnAction(
                    event -> onReturnToMenu.run());
        }

        refreshView();
        view.showGameMessage(
                gameState.getActivePlayer().name() + " goes first");
    }

    /** Rolls the correct dice for the difficulty, then reports the roll. */
    private void handleRoll() {
        if (view.isAnimating()) {
            return;
        }
        if (!gameState.isAwaitingRoll()) {
            view.showGameMessage("Choose a highlighted piece first");
            return;
        }

        PlayerEntry player = gameState.getActivePlayer();
        int steps = dice.roll();
        GameDifficulty difficulty = gameState.getDifficulty();
        int special = switch (difficulty) {
            case EASY -> 0;
            case MEDIUM -> boostDice.roll();
            case HARD -> ultraBoostDice.roll();
        };

        view.showExtraRoll(false);
        // The roll is committed to GameState ONLY after both animations finish.
        view.animateDiceRoll(steps, special, () -> {
            boolean hasLegalMove;
            Outcome outcome;
            if (difficulty == GameDifficulty.EASY) {
                hasLegalMove = gameState.hasLegalMove(player, steps);
                outcome = gameState.recordRoll(player, steps);
                view.setDiceResult(steps);
            } else {
                hasLegalMove = gameState.hasLegalMove(player, steps, special);
                outcome = gameState.recordAdvancedRoll(player, steps, special);
                view.setDiceResult(steps, special);
            }

            handleRollOutcome(outcome, player, hasLegalMove, steps);
            refreshView();
        });
    }

    /** Selects and animates one piece after the model resolves its move. */
    private void handlePieceSelection(Piece piece) {
        if (view.isAnimating() || !gameState.isAwaitingMove()) {
            return;
        }
        if (!gameState.getSelectablePieces().contains(piece)) {
            return;
        }

        PlayerEntry player = gameState.getActivePlayer();
        int fromProgress = piece.getProgress();
        Outcome outcome = gameState.movePiece(player, piece);

        view.setSelectablePieces(List.of());
        view.setRollEnabled(false);
        view.animatePieceMove(gameState, piece, fromProgress, () -> {
            handleOutcome(outcome);
            refreshView();
        });
    }

    /** Preserves Iteration 1 feedback and extra-roll logic. */
    private void handleRollOutcome(Outcome outcome, PlayerEntry roller,
            boolean hasLegalMove, int rolled) {
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
            return roller.name()
                    + " needs a 6 to release a piece, so it is "
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

    private void refreshView() {
        view.setCurrentPlayer(gameState.getActivePlayer().name());
        view.setSelectablePieces(gameState.getSelectablePieces());
        view.setRollEnabled(gameState.isAwaitingRoll());
        view.updateBoard(gameState);
    }
}
