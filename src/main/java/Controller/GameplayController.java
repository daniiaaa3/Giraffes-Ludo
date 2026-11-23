package Controller;

import java.util.List;
import java.util.Optional;

import Model.BoostDice;
import Model.GameDifficulty;
import Model.GameSetup.PlayerEntry;
import Model.GameState;
import Model.Piece;
import Model.QuestionDifficulty;
import Model.QuestionEngine;
import Model.QuestionRepository;
import Model.QuestionResultEvent;
import Model.QuestionSquareResolver;
import Model.StepsDice;
import Model.TurnRollRules.Outcome;
import Model.UltraBoostDice;
import View.GameplayView;
import View.QuestionDialog;
import javafx.application.Platform;
import javafx.stage.Window;

/**
 * Connects the gameplay UI to the game model.
 *
 * Iteration 2: Steps + Boost/UltraBoost dice, plus educational question
 * dialogs for landing on a question square.
 *
 * Questions are shown only after an ordinary piece move finishes animating.
 * A question's extra movement is not a new dice move and does not consume
 * a roll or switch turns. Reaching another question square triggers another
 * question (SRS FR-3.7).
 *
 * MVC: Controller
 */
public class GameplayController {

    private final GameplayView view;
    private final GameState gameState;
    private final StepsDice dice;
    private final BoostDice boostDice;
    private final UltraBoostDice ultraBoostDice;

    // Created only when the first question square is reached. This avoids
    // loading the question bank for Easy games and ordinary UI tests.
    private QuestionDialog questionDialog;
    private boolean questionInProgress;

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
        if (questionInProgress || view.isAnimating()) {
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
        if (questionInProgress || view.isAnimating()
                || !gameState.isAwaitingMove()) {
            return;
        }
        if (!gameState.getSelectablePieces().contains(piece)) {
            return;
        }

        // Keep the original player: completeMove() may already have advanced
        // the active player before the piece animation is over.
        PlayerEntry mover = gameState.getActivePlayer();
        int fromProgress = piece.getProgress();
        Outcome outcome = gameState.movePiece(mover, piece);

        view.setSelectablePieces(List.of());
        view.setRollEnabled(false);
        view.animatePieceMove(gameState, piece, fromProgress,
                () -> checkQuestionLanding(piece, mover, outcome));
    }

    /**
     * Checks the precise square drawn on the board. This runs after the
     * ordinary move AND after every question-induced move (FR-3.7).
     */
    private void checkQuestionLanding(Piece piece, PlayerEntry mover,
            Outcome originalMoveOutcome) {
        Optional<QuestionDifficulty> difficulty =
                QuestionSquareResolver.forPiece(gameState, piece);

        if (difficulty.isEmpty()) {
            finishQuestion(originalMoveOutcome);
            return;
        }

        questionInProgress = true;
        view.setSelectablePieces(List.of());
        view.setRollEnabled(false);
        view.showGameMessage(
                mover.name() + " landed on a question square!");

        // Defer the modal dialog to a new JavaFX pulse, rather than opening
        // a nested event loop in a piece-animation completion callback.
        Platform.runLater(() -> resolveQuestion(
                piece, mover, difficulty.get(), originalMoveOutcome));
    }

    /** Opens a question, applies its movement, and checks the new landing. */
    private void resolveQuestion(Piece piece, PlayerEntry mover,
            QuestionDifficulty difficulty, Outcome originalMoveOutcome) {
        try {
            if (questionDialog == null) {
                QuestionRepository repository = new QuestionRepository();
                QuestionEngine engine = new QuestionEngine(repository);
                questionDialog = new QuestionDialog(
                        new QuestionController(engine));
            }

            Window owner = view.getRoot().getScene() == null
                    ? null : view.getRoot().getScene().getWindow();
            Optional<QuestionResultEvent> result =
                    questionDialog.showQuestion(difficulty, owner);

            // The dialog normally requires a submitted answer before it can
            // close, but this handles a window teardown safely as well.
            if (result.isEmpty()) {
                finishQuestion(originalMoveOutcome);
                return;
            }

            int fromProgress = piece.getProgress();
            gameState.applyQuestionEffect(
                    piece, result.get().getMovementSteps());
            int toProgress = piece.getProgress();

            if (toProgress > fromProgress) {
                // Existing piece animation supports forward movement.
                view.animatePieceMove(gameState, piece, fromProgress,
                        () -> checkQuestionLanding(
                                piece, mover, originalMoveOutcome));
            } else if (toProgress < fromProgress) {
                // Backward movement is supported by the model, but the board
                // does not yet provide a backward animation. Render at once.
                view.updateBoard(gameState);
                checkQuestionLanding(piece, mover, originalMoveOutcome);
            } else {
                // Exact-finish rule may prevent a forward question bonus;
                // an unchanged square is NOT a fresh landing.
                finishQuestion(originalMoveOutcome);
            }
        } catch (RuntimeException error) {
            finishQuestion(originalMoveOutcome);
            view.showGameMessage("Could not complete the question: "
                    + error.getMessage());
        }
    }

    /** Unlocks gameplay only after all linked question landings are done. */
    private void finishQuestion(Outcome originalMoveOutcome) {
        handleOutcome(originalMoveOutcome);
        questionInProgress = false;
        refreshView();
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
        view.setSelectablePieces(questionInProgress
                ? List.of() : gameState.getSelectablePieces());
        view.setRollEnabled(!questionInProgress && gameState.isAwaitingRoll());
        view.updateBoard(gameState);
    }
}
