package Model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import Model.GameSetup.PlayerColor;
import Model.GameSetup.PlayerEntry;
import Model.TurnRollRules.Outcome;

/** Model-level checks for movement earned by answering question squares. */
class QuestionEffectMovementTest {

    private static GameState game(GameDifficulty difficulty) {
        return GameState.forDifficulty(List.of(
                new PlayerEntry("Red", PlayerColor.RED),
                new PlayerEntry("Blue", PlayerColor.BLUE)),
                new Random(42), difficulty);
    }

    private static Piece at(GameState game, int progress) {
        Piece piece = game.getPieces(game.getActivePlayer()).get(0);
        game.placeAt(piece, progress);
        return piece;
    }

    @Test
    void correctEasyQuestionAdvancesOneWithoutRolling() {
        GameState game = game(GameDifficulty.MEDIUM);
        Piece piece = at(game, 8);
        PlayerEntry active = game.getActivePlayer();

        game.applyQuestionEffect(piece, 1);

        assertEquals(9, piece.getProgress());
        assertSame(active, game.getActivePlayer());
        assertEquals(0, game.getPendingRoll());
        assertEquals(0, game.getPendingSpecialDie());
        assertTrue(game.isAwaitingRoll());
        assertTrue(game.getLastMoveResult().isPresent());
    }

    @Test
    void correctHardQuestionAdvancesThreeOnHardBoard() {
        GameState game = game(GameDifficulty.HARD);
        Piece piece = at(game, 9);
        game.applyQuestionEffect(piece, 3);
        assertEquals(12, piece.getProgress());
    }

    @Test
    void wrongMediumQuestionMovesBackwardTwo() {
        GameState game = game(GameDifficulty.MEDIUM);
        Piece piece = at(game, 8);
        game.applyQuestionEffect(piece, -2);
        assertEquals(6, piece.getProgress());
    }

    @Test
    void backwardPenaltyStopsAtStartSquare() {
        GameState game = game(GameDifficulty.HARD);
        Piece piece = at(game, 1);
        game.applyQuestionEffect(piece, -3);
        assertEquals(0, piece.getProgress());
    }

    @Test
    void penaltyCanReturnFromHomePathToSharedTrack() {
        GameState game = game(GameDifficulty.MEDIUM);
        PlayerEntry player = game.getActivePlayer();
        int entry = game.getEntrySteps(player.color());
        Piece piece = at(game, entry + 1);
        assertEquals(Piece.State.HOME_PATH, piece.getState());

        game.applyQuestionEffect(piece, -2);

        assertEquals(entry - 1, piece.getProgress());
        assertEquals(Piece.State.TRACK, piece.getState());
    }

    @Test
    void exactFinishIsAllowed() {
        GameState game = game(GameDifficulty.HARD);
        int finish = game.getFinishSteps(game.getActivePlayer().color());
        Piece piece = at(game, finish - 2);
        game.applyQuestionEffect(piece, 2);
        assertEquals(finish, piece.getProgress());
        assertTrue(piece.isFinished());
    }

    @Test
    void bonusCannotOvershootFinish() {
        GameState game = game(GameDifficulty.MEDIUM);
        int finish = game.getFinishSteps(game.getActivePlayer().color());
        Piece piece = at(game, finish - 1);
        game.applyQuestionEffect(piece, 3);
        assertEquals(finish - 1, piece.getProgress());
    }

    @Test
    void questionDoesNotConsumeAnOutstandingRoll() {
        GameState game = game(GameDifficulty.EASY);
        PlayerEntry active = game.getActivePlayer();
        Piece piece = at(game, 8);
        assertEquals(Outcome.MOVE_REQUIRED, game.recordRoll(active, 4));

        game.applyQuestionEffect(piece, 1);

        assertEquals(9, piece.getProgress());
        assertSame(active, game.getActivePlayer());
        assertEquals(4, game.getPendingRoll());
        assertTrue(game.isAwaitingMove());
    }

    @Test
    void cannotApplyToCastleOrFinishedPiece() {
        GameState game = game(GameDifficulty.EASY);
        Piece piece = game.getPieces(game.getActivePlayer()).get(0);
        assertThrows(IllegalArgumentException.class,
                () -> game.applyQuestionEffect(piece, 1));
        game.placeAt(piece, game.getFinishSteps(piece.getColor()));
        assertThrows(IllegalArgumentException.class,
                () -> game.applyQuestionEffect(piece, -1));
    }

    @Test
    void invalidSignedStepsAreRejected() {
        GameState game = game(GameDifficulty.MEDIUM);
        Piece piece = at(game, 5);
        assertThrows(IllegalArgumentException.class,
                () -> game.applyQuestionEffect(piece, 0));
        assertThrows(IllegalArgumentException.class,
                () -> game.applyQuestionEffect(piece, 4));
        assertThrows(IllegalArgumentException.class,
                () -> game.applyQuestionEffect(piece, -4));
        assertEquals(5, piece.getProgress());
    }

    @Test
    void correctAnswerEventAppliesItsCalculatedReward() {
        GameState game = game(GameDifficulty.MEDIUM);
        Piece piece = at(game, 10);
        Question question = new Question("q-1", "Choose the answer",
                QuestionDifficulty.HARD,
                List.of("A", "B", "C", "D"), 2, "Course");
        QuestionResultEvent answer = new QuestionResultEvent(question, 2);

        assertTrue(answer.isCorrect());
        game.applyQuestionEffect(piece, answer.getMovementSteps());
        assertEquals(13, piece.getProgress());
    }

    @Test
    void incorrectAnswerEventAppliesItsCalculatedPenalty() {
        GameState game = game(GameDifficulty.HARD);
        Piece piece = at(game, 10);
        Question question = new Question("q-2", "Choose the answer",
                QuestionDifficulty.MEDIUM,
                List.of("A", "B", "C", "D"), 2, "Course");
        QuestionResultEvent answer = new QuestionResultEvent(question, 0);

        assertFalse(answer.isCorrect());
        game.applyQuestionEffect(piece, answer.getMovementSteps());
        assertEquals(8, piece.getProgress());
    }

    @Test
    void foreignPieceCannotReceiveQuestionEffect() {
        GameState first = game(GameDifficulty.MEDIUM);
        GameState second = game(GameDifficulty.MEDIUM);
        Piece foreign = at(second, 10);
        assertThrows(IllegalArgumentException.class,
                () -> first.applyQuestionEffect(foreign, 1));
    }
}
