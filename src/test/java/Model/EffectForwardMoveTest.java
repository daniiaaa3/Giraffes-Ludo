package Model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.junit.jupiter.api.Test;

import Model.EasyBoard.BoardCell;
import Model.GameSetup.PlayerColor;
import Model.GameSetup.PlayerEntry;
import Model.Piece.State;
import Model.TurnRollRules.Outcome;

/**
 * Unit tests for the forward movement caused by an effect such as a
 * correct question answer (FR-7.6): it is not a turn action, it uses the
 * shared template (capture, protection, result), and it follows the
 * normal path and finish rules (FR-3.4).
 *
 * Square arithmetic (Easy board, shared track of 52 squares): a piece
 * with start square s and progress p (up to its arrow at 50) stands on
 * track index (s + p) % 52. RED starts at 0, GREEN at 13.
 *
 * Iteration: 2
 */
public class EffectForwardMoveTest {

    private static final PlayerEntry RED =
            new PlayerEntry("Dania", PlayerColor.RED);
    private static final PlayerEntry GREEN =
            new PlayerEntry("Nay", PlayerColor.GREEN);

    private static Random firstSeat() {
        return new Random() {
            @Override
            public int nextInt(int bound) {
                return 0;
            }
        };
    }

    private static GameState game() {
        return new GameState(List.of(RED, GREEN), firstSeat());
    }

    private static Piece piece(GameState g, PlayerEntry p, int id) {
        return g.getPieces(p).get(id);
    }

    private static void place(Piece p, int progress) {
        p.moveTo(progress, progress > 50 ? State.HOME_PATH : State.TRACK);
    }

    private static void assertInCastle(Piece p) {
        assertEquals(State.CASTLE, p.getState());
        assertEquals(Piece.CASTLE_PROGRESS, p.getProgress());
    }

    /** A game that records which shared steps are used. */
    private static final class SpyGame extends GameState {
        final List<String> calls = new ArrayList<>();

        SpyGame() {
            super(List.of(RED, GREEN), firstSeat());
        }

        @Override
        void placeAt(Piece piece, int progress) {
            calls.add("place");
            super.placeAt(piece, progress);
        }

        @Override
        Optional<Piece> resolveCapture(Piece mover) {
            calls.add("capture");
            return super.resolveCapture(mover);
        }

        @Override
        MoveResult record(MoveResult result) {
            calls.add("record");
            return super.record(result);
        }
    }

    // ---------- moving forward ----------

    @Test
    void movesForwardAlongTheSharedTrack() {
        GameState g = game();
        Piece red = piece(g, RED, 0);
        place(red, 20);

        MoveResult result = g.moveForward(red, 4);

        assertEquals(24, red.getProgress());
        assertEquals(State.TRACK, red.getState());
        assertEquals(20, result.fromProgress());
        assertEquals(24, result.toProgress());
        assertFalse(result.isCapture());
        assertEquals(Optional.of(result), g.getLastMoveResult());
    }

    @Test
    void passingTheArrowContinuesOnTheHomePath() {
        GameState g = game();
        Piece red = piece(g, RED, 0);
        place(red, 49);

        g.moveForward(red, 3);              // 52 is past the arrow at 50

        assertEquals(52, red.getProgress());
        assertEquals(State.HOME_PATH, red.getState());
        assertEquals(g.getBoardCellAt(PlayerColor.RED, 52),
                g.getBoardCell(red));
    }

    @Test
    void aPieceOnTheHomePathMovesForwardBySteps() {
        GameState g = game();
        Piece red = piece(g, RED, 0);
        place(red, 51);

        g.moveForward(red, 2);

        assertEquals(53, red.getProgress());
        assertEquals(State.HOME_PATH, red.getState());
    }

    // ---------- finish rule (FR-3.4) ----------

    @Test
    void anExactDistanceToTheFinishFinishesThePiece() {
        GameState g = game();
        Piece red = piece(g, RED, 0);
        place(red, 53);

        MoveResult result = g.moveForward(red, 3);

        assertEquals(56, red.getProgress());
        assertEquals(State.FINISHED, red.getState());
        assertEquals(56, result.toProgress());
        assertFalse(result.isCapture());
    }

    @Test
    void aMoveBeyondTheFinishIsRejectedAndChangesNothing() {
        GameState g = game();
        Piece red = piece(g, RED, 0);
        place(red, 53);

        assertFalse(g.canMoveForward(red, 4));
        assertThrows(IllegalArgumentException.class,
                () -> g.moveForward(red, 4));

        assertEquals(53, red.getProgress());
        assertEquals(State.HOME_PATH, red.getState());
        assertTrue(g.getLastMoveResult().isEmpty());
    }

    // ---------- not a turn action ----------

    @Test
    void doesNotConsumeThePendingRollOrChangeTheTurn() {
        GameState g = game();
        Piece red = piece(g, RED, 0);
        Piece green = piece(g, GREEN, 0);
        place(red, 20);
        place(green, 30);

        assertEquals(Outcome.MOVE_REQUIRED, g.recordRoll(RED, 4));

        g.moveForward(green, 3);            // an opponent's piece moves

        assertEquals(33, green.getProgress());
        assertEquals(RED, g.getActivePlayer());
        assertEquals(4, g.getPendingRoll());
        assertTrue(g.isAwaitingMove());

        assertEquals(Outcome.TURN_PASSED, g.movePiece(RED, red));
        assertEquals(24, red.getProgress());
        assertEquals(GREEN, g.getActivePlayer());
    }

    @Test
    void doesNotChangeTheSixCounterOrTheBonusRoll() {
        GameState g = game();
        Piece red = piece(g, RED, 0);
        place(red, 20);

        assertEquals(Outcome.MOVE_REQUIRED, g.recordRoll(RED, 6));
        assertEquals(1, g.getConsecutiveSixes());

        g.moveForward(red, 2);

        assertEquals(1, g.getConsecutiveSixes());
        assertEquals(6, g.getPendingRoll());
        assertEquals(Outcome.BONUS_ROLL, g.movePiece(RED, red));
        assertEquals(28, red.getProgress());
    }

    @Test
    void movesAnyPieceEvenWhenItIsNotThatPlayersTurn() {
        GameState g = game();
        Piece green = piece(g, GREEN, 0);
        place(green, 10);

        assertEquals(RED, g.getActivePlayer());
        g.moveForward(green, 5);

        assertEquals(15, green.getProgress());
        assertEquals(RED, g.getActivePlayer());
        assertTrue(g.isAwaitingRoll());
    }

    // ---------- capture and protected squares ----------

    @Test
    void landingOnASingleOpponentOnAnUnprotectedSquareCapturesIt() {
        GameState g = game();
        Piece red = piece(g, RED, 0);
        Piece green = piece(g, GREEN, 0);
        place(red, 5);
        place(green, 47);                   // index 8

        MoveResult result = g.moveForward(red, 3);

        assertEquals(8, red.getProgress());
        assertInCastle(green);
        assertEquals(Optional.of(green), result.captured());
    }

    @Test
    void passingAnOpponentDoesNotCaptureIt() {
        GameState g = game();
        Piece red = piece(g, RED, 0);
        Piece green = piece(g, GREEN, 0);
        place(red, 5);
        place(green, 46);                   // index 7

        g.moveForward(red, 4);

        assertEquals(State.TRACK, green.getState());
        assertEquals(46, green.getProgress());
    }

    @Test
    void landingOnAnotherPlayersStartSquareCapturesNothing() {
        GameState g = game();
        Piece red = piece(g, RED, 0);
        Piece green = piece(g, GREEN, 0);
        place(red, 10);
        place(green, 0);                    // green's start, index 13

        g.moveForward(red, 3);

        assertEquals(13, red.getProgress());
        assertEquals(State.TRACK, green.getState());
        assertEquals(0, green.getProgress());
    }

    @Test
    void extraProtectedSquaresFromTheBoardOwnerBlockCapture() {
        BoardCell safe = new EasyBoard().getSharedTrackCell(8);
        GameState g = new GameState(List.of(RED, GREEN), firstSeat(),
                cell -> cell.equals(safe));
        Piece red = piece(g, RED, 0);
        Piece green = piece(g, GREEN, 0);
        place(red, 5);
        place(green, 47);

        MoveResult result = g.moveForward(red, 3);

        assertFalse(result.isCapture());
        assertEquals(State.TRACK, green.getState());
    }

    @Test
    void ownPiecesAreNeverCaptured() {
        GameState g = game();
        Piece first = piece(g, RED, 0);
        Piece second = piece(g, RED, 1);
        place(first, 5);
        place(second, 8);

        g.moveForward(first, 3);

        assertEquals(State.TRACK, second.getState());
        assertEquals(8, second.getProgress());
    }

    // ---------- zero distance ----------

    @Test
    void zeroStepsChangeNothingAndCaptureNothing() {
        GameState g = game();
        Piece red = piece(g, RED, 0);
        Piece green = piece(g, GREEN, 0);
        place(red, 20);
        place(green, 7);                    // index 20, same square

        MoveResult result = g.moveForward(red, 0);

        assertEquals(20, red.getProgress());
        assertEquals(State.TRACK, green.getState());
        assertFalse(result.isCapture());
        assertEquals(20, result.fromProgress());
        assertEquals(20, result.toProgress());
        assertEquals(Optional.of(result), g.getLastMoveResult());
    }

    // ---------- pieces and values that are rejected ----------

    @Test
    void aPieceInTheCastleCannotMoveForwardByAnEffect() {
        GameState g = game();
        Piece red = piece(g, RED, 0);

        assertFalse(g.canMoveForward(red, 2));
        assertThrows(IllegalArgumentException.class,
                () -> g.moveForward(red, 2));
        assertInCastle(red);
    }

    @Test
    void aFinishedPieceCannotMoveForward() {
        GameState g = game();
        Piece red = piece(g, RED, 0);
        red.moveTo(56, State.FINISHED);

        assertFalse(g.canMoveForward(red, 1));
        assertThrows(IllegalArgumentException.class,
                () -> g.moveForward(red, 1));
        assertEquals(State.FINISHED, red.getState());
    }

    @Test
    void negativeStepsNullAndForeignPiecesAreRejected() {
        GameState g = game();
        GameState other = game();
        Piece red = piece(g, RED, 0);
        place(red, 20);

        assertFalse(g.canMoveForward(red, -1));
        assertThrows(IllegalArgumentException.class,
                () -> g.moveForward(red, -1));
        assertThrows(IllegalArgumentException.class,
                () -> g.moveForward(null, 1));
        assertThrows(IllegalArgumentException.class,
                () -> g.moveForward(piece(other, RED, 0), 1));
        assertThrows(IllegalArgumentException.class,
                () -> g.canMoveForward(null, 1));

        assertEquals(20, red.getProgress());
        assertTrue(g.getLastMoveResult().isEmpty());
    }

    @Test
    void canMoveForwardAcceptsOnBoardPiecesWithinTheFinish() {
        GameState g = game();
        Piece red = piece(g, RED, 0);
        place(red, 20);

        assertTrue(g.canMoveForward(red, 0));
        assertTrue(g.canMoveForward(red, 3));
        assertTrue(g.canMoveForward(red, 36));      // exactly the finish
        assertFalse(g.canMoveForward(red, 37));
    }

    @Test
    void veryLargeStepValuesDoNotOverflowTheFinishCheck() {
        GameState g = game();
        Piece red = piece(g, RED, 0);
        place(red, 20);

        for (int huge : new int[] {Integer.MAX_VALUE,
                Integer.MAX_VALUE - 10, 1 << 30}) {
            assertFalse(g.canMoveForward(red, huge), "steps " + huge);
            assertThrows(IllegalArgumentException.class,
                    () -> g.moveForward(red, huge));
        }

        assertEquals(20, red.getProgress());
        assertEquals(State.TRACK, red.getState());
        assertTrue(g.getLastMoveResult().isEmpty());
    }

    @Test
    void theLargestAcceptedStepValueIsTheExactDistanceToTheFinish() {
        GameState g = game();
        Piece red = piece(g, RED, 0);
        place(red, 20);                     // finish is at progress 56

        assertTrue(g.canMoveForward(red, 36));
        assertFalse(g.canMoveForward(red, 37));
        assertFalse(g.canMoveForward(red, Integer.MAX_VALUE));
    }

    // ---------- signed effects ----------

    @Test
    void aSignedEffectMovesForwardBackwardOrNotAtAll() {
        GameState g = game();
        Piece red = piece(g, RED, 0);
        place(red, 20);

        g.moveByEffect(red, 2);
        assertEquals(22, red.getProgress());

        g.moveByEffect(red, -3);
        assertEquals(19, red.getProgress());

        MoveResult none = g.moveByEffect(red, 0);
        assertEquals(19, red.getProgress());
        assertEquals(19, none.fromProgress());
        assertEquals(19, none.toProgress());
    }

    @Test
    void aWrongAnswerEffectStopsAtTheStartSquare() {
        GameState g = game();
        Piece red = piece(g, RED, 0);
        place(red, 2);

        g.moveByEffect(red, -3);

        assertEquals(0, red.getProgress());
        assertEquals(State.TRACK, red.getState());
    }

    // ---------- the shared template ----------

    @Test
    void theEffectMoveIsATemplateSubclassUsingTheSharedSteps() {
        assertTrue(PieceMove.class
                .isAssignableFrom(EffectForwardMove.class));

        SpyGame g = new SpyGame();
        Piece red = piece(g, RED, 0);
        place(red, 20);

        g.moveForward(red, 3);

        assertEquals(List.of("place", "capture", "record"), g.calls);
    }

    @Test
    void aZeroStepEffectSkipsPlacingAndCapture() {
        SpyGame g = new SpyGame();
        Piece red = piece(g, RED, 0);
        place(red, 20);

        g.moveForward(red, 0);

        assertEquals(List.of("record"), g.calls);
    }

    @Test
    void aRejectedEffectNeverReachesTheSharedSteps() {
        SpyGame g = new SpyGame();
        Piece inCastle = piece(g, RED, 0);

        assertThrows(IllegalArgumentException.class,
                () -> g.moveForward(inCastle, 2));
        assertThrows(IllegalArgumentException.class,
                () -> g.moveForward(inCastle, -2));

        assertTrue(g.calls.isEmpty());
    }
}
