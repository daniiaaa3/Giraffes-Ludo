package View;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import Controller.GameplayController;
import Model.EasyBoard.BoardCell;
import Model.GameBoardTopology;
import Model.GameDifficulty;
import Model.GameSetup.PlayerColor;
import Model.GameSetup.PlayerEntry;
import Model.GameState;
import Model.Piece;
import Model.StepsDice;
import Model.TurnRollRules.Outcome;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * JavaFX integration check for FR-3.7 / FR-7.8: landing on a question square
 * because of a previous question must open another real question dialog.
 *
 * The test injects two EXTRA question-square markers into a fresh, test-only
 * board. It does NOT edit production board placement or the question logic.
 * Adding destinations on BOTH sides means the test works regardless of whether
 * the randomly drawn question is answered correctly or incorrectly.
 *
 * No TestFX or Mockito dependency is required.
 */
class QuestionChainingUiTest {

    private record Fixture(GameState game, GameplayView view,
            GameplayController controller, Piece piece, PlayerEntry mover,
            PlayerEntry activeBefore, Stage stage, int firstProgress,
            int answerSteps, Field questionSquaresField) {}

    private record DialogControls(Window window, Button answer,
            Button continueButton) {}

    @BeforeAll
    static void startJavaFx() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        try {
            Platform.startup(started::countDown);
        } catch (IllegalStateException alreadyStarted) {
            started.countDown();
        }
        assertTrue(started.await(10, TimeUnit.SECONDS),
                "JavaFX toolkit did not start");
        onFx(() -> {
            Platform.setImplicitExit(false);
            return null;
        });
    }

    @Test
    void bonusOrPenaltyLandingOpensSecondQuestionWithoutChangingTurn()
            throws Exception {
        Fixture fixture = onFx(QuestionChainingUiTest::createFixture);
        DialogControls first = null;
        DialogControls second = null;
        try {
            // Simulate completion of a normal move on a question square.
            // Actual board animations are not needed for this entry point.
            onFx(() -> {
                invokeLanding(fixture);
                return null;
            });

            first = awaitDialog(fixture.stage(), d -> true, 10);
            assertNotNull(first, "First question dialog never opened");
            assertTrue(onFx(() -> fixture.view().getRollButton().isDisabled()),
                    "Rolling should be blocked while a question is open");

            DialogControls firstDialog = first;
            onFx(() -> {
                firstDialog.answer().fire(); // Answer A, right or wrong
                assertFalse(firstDialog.continueButton().isDisabled());
                firstDialog.continueButton().fire();
                return null;
            });

            // The first answer moves one piece by +/- its question level.
            // Both destinations are specially marked in this test fixture.
            Window firstWindow = first.window();
            second = awaitDialog(fixture.stage(),
                    d -> d.window() != firstWindow, 12);
            assertNotNull(second,
                    "No SECOND question opened after the first answer moved the piece");

            assertEquals(fixture.answerSteps(),
                    Math.abs(onFx(() -> fixture.piece().getProgress())
                            - fixture.firstProgress()),
                    "The first answer must move the piece before question two");
            assertTrue(onFx(() -> fixture.view().getRollButton().isDisabled()),
                    "Rolling must remain disabled during question two");
            assertEquals(fixture.activeBefore(),
                    onFx(() -> fixture.game().getActivePlayer()),
                    "The question chain must not change the active player");

            // Remove only the TEST-INJECTED question markers before answering
            // question two. This avoids starting an irrelevant third dialog.
            DialogControls secondDialog = second;
            onFx(() -> {
                replaceQuestionSquares(fixture.game().getBoardTopology(),
                        fixture.questionSquaresField(), Map.of());
                secondDialog.answer().fire();
                secondDialog.continueButton().fire();
                return null;
            });

            assertTrue(awaitCondition(() -> onFx(() ->
                    !secondDialog.window().isShowing()
                    && !fixture.view().getRollButton().isDisabled()), 12),
                    "Gameplay did not resume after the second answer");
            assertEquals(fixture.activeBefore(),
                    onFx(() -> fixture.game().getActivePlayer()),
                    "Question answers must not advance the turn");
            assertTrue(onFx(() -> fixture.game().isAwaitingRoll()),
                    "Game must still be waiting for the same normal roll/turn state");
        } finally {
            // Always release modal dialogs, even after an assertion fails.
            onFx(() -> {
                replaceQuestionSquares(fixture.game().getBoardTopology(),
                        fixture.questionSquaresField(), Map.of());
                for (int attempt = 0; attempt < 3; attempt++) {
                    DialogControls open = findDialog(fixture.stage());
                    if (open == null) {
                        break;
                    }
                    if (!open.answer().isMouseTransparent()) {
                        open.answer().fire();
                    }
                    if (!open.continueButton().isDisabled()) {
                        open.continueButton().fire();
                    }
                }
                fixture.stage().close();
                return null;
            });
        }
    }

    private static Fixture createFixture() throws Exception {
        List<PlayerEntry> players = List.of(
                new PlayerEntry("Red", PlayerColor.RED),
                new PlayerEntry("Blue", PlayerColor.BLUE),
                new PlayerEntry("Green", PlayerColor.GREEN),
                new PlayerEntry("Yellow", PlayerColor.YELLOW));
        GameState game = GameState.forDifficulty(players,
                new Random(42), GameDifficulty.HARD);
        GameBoardTopology topology = game.getBoardTopology();
        Field squares = GameBoardTopology.class.getDeclaredField("questionSquares");
        squares.setAccessible(true);

        // Pick a REAL question on a safe interior part of a player's path.
        // Inject one extra question at both possible answer destinations.
        for (PlayerEntry player : game.getPlayers()) {
            List<BoardCell> path = topology.getPlayerPath(player.color());
            int entry = game.getEntrySteps(player.color());
            for (int p = 4; p <= entry - 4; p++) {
                GameDifficulty level = topology.getQuestionSquares().get(path.get(p));
                if (level == null) {
                    continue;
                }
                int steps = switch (level) {
                    case EASY -> 1;
                    case MEDIUM -> 2;
                    case HARD -> 3;
                };
                Map<BoardCell, GameDifficulty> modified =
                        new HashMap<>(topology.getQuestionSquares());
                modified.put(path.get(p - steps), GameDifficulty.EASY);
                modified.put(path.get(p + steps), GameDifficulty.EASY);
                replaceQuestionSquares(topology, squares, modified);

                Piece piece = game.getPieces(player).get(0);
                placePiece(game, piece, p);

                GameplayView view = new GameplayView(game);
                Stage stage = new Stage();
                stage.setScene(new Scene(view.getRoot(), 1280, 720));
                stage.show();
                GameplayController controller = new GameplayController(
                        view, game, new StepsDice());

                return new Fixture(game, view, controller, piece, player,
                        game.getActivePlayer(), stage, p, steps, squares);
            }
        }
        throw new AssertionError(
                "Could not locate an interior question square on Hard board");
    }

    private static void placePiece(GameState game, Piece piece, int progress)
            throws Exception {
        Method placeAt = GameState.class.getDeclaredMethod(
                "placeAt", Piece.class, int.class);
        placeAt.setAccessible(true);
        placeAt.invoke(game, piece, progress);
    }

    private static void invokeLanding(Fixture fixture) throws Exception {
        Method check = GameplayController.class.getDeclaredMethod(
                "checkQuestionLanding", Piece.class, PlayerEntry.class,
                Outcome.class);
        check.setAccessible(true);
        try {
            check.invoke(fixture.controller(), fixture.piece(),
                    fixture.mover(), Outcome.TURN_PASSED);
        } catch (InvocationTargetException ex) {
            throw new AssertionError("Question landing handler failed",
                    ex.getCause());
        }
    }

    private static void replaceQuestionSquares(GameBoardTopology topology,
            Field field, Map<BoardCell, GameDifficulty> newSquares)
            throws IllegalAccessException {
        field.set(topology, Map.copyOf(newSquares));
    }

    private static DialogControls findDialog(Stage mainStage) {
        for (Window window : new ArrayList<>(Window.getWindows())) {
            if (window == mainStage || !window.isShowing()
                    || window.getScene() == null) {
                continue;
            }
            Parent root = window.getScene().getRoot();
            Button firstOption = null;
            Button continueButton = null;
            for (javafx.scene.Node node : root.lookupAll(".button")) {
                if (node instanceof Button button) {
                    if (button.getText() != null
                            && button.getText().startsWith("A.")) {
                        firstOption = button;
                    }
                    if ("Continue".equals(button.getText())) {
                        continueButton = button;
                    }
                }
            }
            if (firstOption != null && continueButton != null) {
                return new DialogControls(window, firstOption, continueButton);
            }
        }
        return null;
    }

    private static DialogControls awaitDialog(Stage stage,
            Predicate<DialogControls> matches, int timeoutSeconds)
            throws Exception {
        long deadline = System.nanoTime()
                + TimeUnit.SECONDS.toNanos(timeoutSeconds);
        while (System.nanoTime() < deadline) {
            DialogControls dialog = onFx(() -> findDialog(stage));
            if (dialog != null && matches.test(dialog)) {
                return dialog;
            }
            Thread.sleep(60);
        }
        return null;
    }

    private static boolean awaitCondition(Callable<Boolean> condition,
            int timeoutSeconds) throws Exception {
        long deadline = System.nanoTime()
                + TimeUnit.SECONDS.toNanos(timeoutSeconds);
        while (System.nanoTime() < deadline) {
            if (condition.call()) {
                return true;
            }
            Thread.sleep(60);
        }
        return false;
    }

    private static <T> T onFx(Callable<T> work) throws Exception {
        if (Platform.isFxApplicationThread()) {
            return work.call();
        }
        CompletableFuture<T> completed = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                completed.complete(work.call());
            } catch (Throwable error) {
                completed.completeExceptionally(error);
            }
        });
        return completed.get(20, TimeUnit.SECONDS);
    }
}
