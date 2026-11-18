package Model;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.junit.jupiter.api.Test;

import Model.GameSetup.PlayerColor;
import Model.GameSetup.PlayerEntry;
import Model.Piece.State;

/**
 * Tests for the Template Method pattern in the movement code:
 * PieceMove.execute is the fixed template, ForwardMove and
 * BackwardMove fill in the varying steps, and both reach the same
 * shared steps (place, capture, record).
 *
 * Square arithmetic: a piece with start square s and progress p
 * (up to its arrow at 50) stands on track index (s + p) % 52. RED
 * starts at 0, GREEN at 13.
 *
 * Iteration: 2
 */
public class TemplateMethodMovementTest {

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

    /** A game that counts how often the shared steps are used. */
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

    /** A new kind of move, to show the template is reusable. */
    private static final class LoggedMove extends PieceMove {
        final List<String> log;
        final int jump;
        final boolean reject;

        LoggedMove(GameState game, List<String> log, int jump,
                   boolean reject) {
            super(game);
            this.log = log;
            this.jump = jump;
            this.reject = reject;
        }

        @Override
        void validate(Piece piece) {
            log.add("validate");
            if (reject) {
                throw new IllegalArgumentException("rejected");
            }
        }

        @Override
        void afterValidation() {
            log.add("hook");
        }

        @Override
        int destination(Piece piece) {
            log.add("destination at " + piece.getProgress());
            return piece.getProgress() + jump;
        }
    }

    private static Piece piece(GameState g, PlayerEntry p, int id) {
        return g.getPieces(p).get(id);
    }

    private static void place(Piece p, int progress) {
        p.moveTo(progress, State.TRACK);
    }

    // ---------- structure of the pattern ----------

    @Test
    void theTemplateMethodIsFinalAndTheVaryingStepsAreAbstract()
            throws Exception {
        Method execute = PieceMove.class
                .getDeclaredMethod("execute", Piece.class);
        Method validate = PieceMove.class
                .getDeclaredMethod("validate", Piece.class);
        Method destination = PieceMove.class
                .getDeclaredMethod("destination", Piece.class);
        Method hook = PieceMove.class
                .getDeclaredMethod("afterValidation");

        assertTrue(Modifier.isAbstract(PieceMove.class.getModifiers()));
        assertTrue(Modifier.isFinal(execute.getModifiers()));
        assertTrue(Modifier.isAbstract(validate.getModifiers()));
        assertTrue(Modifier.isAbstract(destination.getModifiers()));
        assertFalse(Modifier.isAbstract(hook.getModifiers()),
                "the hook has a default that does nothing");
    }

    @Test
    void forwardAndBackwardMovesAreTemplateSubclasses() {
        assertTrue(PieceMove.class.isAssignableFrom(ForwardMove.class));
        assertTrue(PieceMove.class.isAssignableFrom(BackwardMove.class));
        assertNotEquals(ForwardMove.class, BackwardMove.class);
    }

    // ---------- the fixed order of the steps ----------

    @Test
    void theTemplateRunsValidateHookThenDestinationAndThenAppliesTheMove() {
        GameState g = new GameState(List.of(RED, GREEN), firstSeat());
        Piece red = piece(g, RED, 0);
        place(red, 5);
        List<String> log = new ArrayList<>();

        MoveResult result = new LoggedMove(g, log, 3, false).execute(red);

        assertEquals(List.of("validate", "hook", "destination at 5"), log);
        assertEquals(8, red.getProgress());
        assertEquals(5, result.fromProgress());
        assertEquals(8, result.toProgress());
        assertEquals(Optional.of(result), g.getLastMoveResult());
    }

    @Test
    void aRejectedValidationStopsTheTemplateBeforeAnythingChanges() {
        GameState g = new GameState(List.of(RED, GREEN), firstSeat());
        Piece red = piece(g, RED, 0);
        place(red, 5);
        List<String> log = new ArrayList<>();

        assertThrows(IllegalArgumentException.class,
                () -> new LoggedMove(g, log, 3, true).execute(red));

        assertEquals(List.of("validate"), log,
                "neither the hook nor the destination ran");
        assertEquals(5, red.getProgress());
        assertTrue(g.getLastMoveResult().isEmpty());
    }

    @Test
    void aNewKindOfMoveGetsCaptureAndProtectionFromTheTemplate() {
        GameState g = new GameState(List.of(RED, GREEN), firstSeat());
        Piece red = piece(g, RED, 0);
        Piece green = piece(g, GREEN, 0);
        place(red, 5);
        place(green, 47);                  // index 8

        MoveResult result = new LoggedMove(g, new ArrayList<>(), 3, false)
                .execute(red);             // red lands on index 8

        assertEquals(Optional.of(green), result.captured());
        assertEquals(State.CASTLE, green.getState());

        // landing on green's start square (index 13) captures nothing
        Piece other = piece(g, GREEN, 1);
        place(other, 0);
        place(red, 10);
        MoveResult safe = new LoggedMove(g, new ArrayList<>(), 3, false)
                .execute(red);

        assertTrue(safe.captured().isEmpty());
        assertEquals(State.TRACK, other.getState());
    }

    @Test
    void aMoveThatStaysInPlaceCapturesNothingAndIsStillRecorded() {
        GameState g = new GameState(List.of(RED, GREEN), firstSeat());
        Piece red = piece(g, RED, 0);
        Piece green = piece(g, GREEN, 0);
        place(red, 20);
        place(green, 7);                   // index 20, same square

        MoveResult result = new LoggedMove(g, new ArrayList<>(), 0, false)
                .execute(red);

        assertFalse(result.isCapture());
        assertEquals(State.TRACK, green.getState());
        assertEquals(Optional.of(result), g.getLastMoveResult());
    }

    // ---------- both real moves use the shared steps ----------

    @Test
    void forwardMoveUsesTheSharedPlaceCaptureAndRecordSteps() {
        SpyGame g = new SpyGame();
        Piece red = piece(g, RED, 0);
        place(red, 5);

        g.recordRoll(RED, 3);
        g.movePiece(RED, red);

        assertEquals(List.of("place", "capture", "record"), g.calls);
        assertEquals(0, g.getPendingRoll(), "the forward hook ran");
    }

    @Test
    void backwardMoveUsesTheSameSharedPlaceCaptureAndRecordSteps() {
        SpyGame g = new SpyGame();
        Piece red = piece(g, RED, 0);
        place(red, 20);

        g.moveBackward(red, 3);

        assertEquals(List.of("place", "capture", "record"), g.calls);
    }

    @Test
    void forwardAndBackwardLandingsCaptureThroughTheSameSharedStep() {
        SpyGame g = new SpyGame();
        Piece red = piece(g, RED, 0);
        Piece green = piece(g, GREEN, 0);
        place(red, 5);
        place(green, 47);                  // index 8

        g.recordRoll(RED, 3);
        g.movePiece(RED, red);             // forward capture
        assertEquals(State.CASTLE, green.getState());

        place(green, 4);                   // index 17
        place(red, 20);
        g.calls.clear();
        g.moveBackward(red, 3);            // backward capture

        assertEquals(State.CASTLE, green.getState());
        assertEquals(List.of("place", "capture", "record"), g.calls);
    }

    @Test
    void aBackwardMoveThatChangesNothingSkipsPlacingAndCapture() {
        SpyGame g = new SpyGame();
        Piece red = piece(g, RED, 0);
        place(red, 0);

        g.moveBackward(red, 4);

        assertEquals(List.of("record"), g.calls);
    }

    @Test
    void rejectedMovesNeverReachTheSharedSteps() {
        SpyGame g = new SpyGame();
        Piece inCastle = piece(g, RED, 0);

        assertThrows(IllegalArgumentException.class,
                () -> g.moveBackward(inCastle, 2));
        assertThrows(IllegalArgumentException.class,
                () -> g.moveBackward(inCastle, -1));

        g.recordRoll(RED, 6);
        assertThrows(IllegalArgumentException.class,
                () -> g.movePiece(RED, null));

        assertTrue(g.calls.isEmpty());
    }

    // ---------- what stays different between the two moves ----------

    @Test
    void onlyTheForwardMoveIsATurnAction() {
        SpyGame g = new SpyGame();
        Piece red = piece(g, RED, 0);
        Piece green = piece(g, GREEN, 0);
        place(red, 20);
        place(green, 30);

        g.recordRoll(RED, 4);
        g.moveBackward(green, 5);          // not a turn action

        assertEquals(4, g.getPendingRoll());
        assertEquals(RED, g.getActivePlayer());

        g.movePiece(RED, red);             // a turn action

        assertEquals(0, g.getPendingRoll());
        assertEquals(GREEN, g.getActivePlayer());
    }
}
