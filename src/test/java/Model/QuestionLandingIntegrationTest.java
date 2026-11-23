package Model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

import Model.EasyBoard.BoardCell;
import Model.GameSetup.PlayerColor;
import Model.GameSetup.PlayerEntry;

/**
 * Integration tests for the model side of question-square landings.
 *
 * These tests use the actual Medium/Hard board topology, the question-square
 * resolver and the question-result movement API. The modal JavaFX dialog
 * and automatic opening of a second dialog still require UI verification.
 */
class QuestionLandingIntegrationTest {

    private record Landing(Piece piece, int progress,
            QuestionDifficulty difficulty, int steps) {}

    private static GameState game(GameDifficulty difficulty) {
        return GameState.forDifficulty(List.of(
                new PlayerEntry("Red", PlayerColor.RED),
                new PlayerEntry("Blue", PlayerColor.BLUE),
                new PlayerEntry("Green", PlayerColor.GREEN),
                new PlayerEntry("Yellow", PlayerColor.YELLOW)),
                new Random(42), difficulty);
    }

    /**
     * Finds a real question square that can be reached with the reward or
     * penalty specified by its question difficulty. No fixed coordinates are
     * assumed, so the test remains valid when board placements change.
     */
    private static Landing findLanding(GameState game, boolean forward) {
        GameBoardTopology topology = game.getBoardTopology();
        Map<BoardCell, GameDifficulty> squares = topology.getQuestionSquares();

        for (PlayerEntry player : game.getPlayers()) {
            List<BoardCell> path = topology.getPlayerPath(player.color());
            int entry = game.getEntrySteps(player.color());

            for (int progress = 1; progress <= entry; progress++) {
                GameDifficulty boardLevel = squares.get(path.get(progress));
                if (boardLevel == null) {
                    continue;
                }
                QuestionDifficulty questionLevel =
                        QuestionDifficulty.valueOf(boardLevel.name());
                int steps = QuestionEffect.calculateSteps(questionLevel, true);
                boolean reachable = forward
                        ? progress >= steps
                        : progress + steps <= entry;
                if (reachable) {
                    return new Landing(game.getPieces(player).get(0),
                            progress, questionLevel, steps);
                }
            }
        }
        throw new AssertionError(
                "No reachable question square was found on the board");
    }

    private static QuestionResultEvent answer(
            QuestionDifficulty difficulty, boolean correct) {
        Question question = new Question("integration-question",
                "A sample educational question", difficulty,
                List.of("A", "B", "C", "D"), 0, "Test");
        return new QuestionResultEvent(question, correct ? 0 : 1);
    }

    @Test
    void correctAnswerCanLandOnQuestionSquareOnMediumBoard() {
        GameState game = game(GameDifficulty.MEDIUM);
        Landing target = findLanding(game, true);
        game.placeAt(target.piece(), target.progress() - target.steps());
        PlayerEntry activeBefore = game.getActivePlayer();

        QuestionResultEvent result = answer(target.difficulty(), true);
        assertEquals(target.steps(), result.getMovementSteps());
        game.applyQuestionEffect(target.piece(), result.getMovementSteps());

        assertEquals(target.progress(), target.piece().getProgress());
        assertEquals(target.difficulty(),
                QuestionSquareResolver.forPiece(game, target.piece())
                        .orElseThrow());
        assertEquals(activeBefore, game.getActivePlayer());
        assertTrue(game.isAwaitingRoll());
    }

    @Test
    void wrongAnswerCanLandOnQuestionSquareOnHardBoard() {
        GameState game = game(GameDifficulty.HARD);
        Landing target = findLanding(game, false);
        game.placeAt(target.piece(), target.progress() + target.steps());
        PlayerEntry activeBefore = game.getActivePlayer();

        QuestionResultEvent result = answer(target.difficulty(), false);
        assertEquals(-target.steps(), result.getMovementSteps());
        game.applyQuestionEffect(target.piece(), result.getMovementSteps());

        assertEquals(target.progress(), target.piece().getProgress());
        assertEquals(target.difficulty(),
                QuestionSquareResolver.forPiece(game, target.piece())
                        .orElseThrow());
        assertEquals(activeBefore, game.getActivePlayer());
        assertTrue(game.isAwaitingRoll());
    }

    @Test
    void secondQuestionResultMovesSamePieceWithoutConsumingTurn() {
        GameState game = game(GameDifficulty.MEDIUM);
        Landing target = findLanding(game, true);
        int initialProgress = target.progress() - target.steps();
        game.placeAt(target.piece(), initialProgress);
        PlayerEntry activeBefore = game.getActivePlayer();

        // The first answer lands on a real question square.
        game.applyQuestionEffect(target.piece(),
                answer(target.difficulty(), true).getMovementSteps());
        QuestionDifficulty landedLevel =
                QuestionSquareResolver.forPiece(game, target.piece())
                        .orElseThrow();

        // Simulate the next question's answer. The gameplay controller is
        // responsible for displaying this second question to the player.
        game.applyQuestionEffect(target.piece(),
                answer(landedLevel, false).getMovementSteps());

        assertEquals(initialProgress, target.piece().getProgress());
        assertEquals(activeBefore, game.getActivePlayer());
        assertEquals(0, game.getPendingRoll());
        assertEquals(0, game.getPendingSpecialDie());
        assertTrue(game.isAwaitingRoll());
    }
}
