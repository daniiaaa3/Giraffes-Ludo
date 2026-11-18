package Model;

import static org.junit.jupiter.api.Assertions.*;

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
 * Unit tests for backward movement (FR-5.9) and its capture
 * (FR-5.4): path reversal, clamping at the start square, zero
 * steps, protection, and pieces that cannot move backward.
 *
 * Square arithmetic (shared track of 52 squares): a piece of a
 * color with start square s and progress p (up to its arrow at
 * progress 50) stands on track index (s + p) % 52. RED starts at
 * 0, GREEN at 13, YELLOW at 26, BLUE at 39.
 *
 * Iteration: 2
 */
public class BackwardMovementTest {

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
        p.moveTo(progress, State.TRACK);
    }

    private static void assertInCastle(Piece p) {
        assertEquals(State.CASTLE, p.getState());
        assertEquals(Piece.CASTLE_PROGRESS, p.getProgress());
    }

    // ---------- path reversal ----------

    @Test
    void movesBackAlongTheSharedTrack() {
        GameState g = game();
        Piece red = piece(g, RED, 0);
        place(red, 20);

        MoveResult result = g.moveBackward(red, 3);

        assertEquals(17, red.getProgress());
        assertEquals(State.TRACK, red.getState());
        assertEquals(20, result.fromProgress());
        assertEquals(17, result.toProgress());
        assertFalse(result.isCapture());
        assertEquals(Optional.of(result), g.getLastMoveResult());
    }

    @Test
    void backwardMovementIsTheExactReverseOfForwardMovement() {
        GameState g = game();
        Piece red = piece(g, RED, 0);
        place(red, 20);

        g.moveBackward(red, 4);
        g.recordRoll(RED, 4);
        g.movePiece(RED, red);

        assertEquals(20, red.getProgress());
    }

    @Test
    void everyBackwardMoveMatchesTheBoardPath() {
        for (PlayerColor color : PlayerColor.values()) {
            for (int from = 0; from < 56; from++) {
                for (int steps = 0; steps <= 10; steps++) {
                    PlayerEntry owner = new PlayerEntry("P", color);
                    GameState g = new GameState(
                            List.of(owner,
                                    new PlayerEntry("Q", other(color))),
                            firstSeat());
                    Piece p = g.getPieces(owner).get(0);
                    State s = from > g.getEntrySteps(color)
                            ? State.HOME_PATH : State.TRACK;
                    p.moveTo(from, s);

                    g.moveBackward(p, steps);

                    int expected = Math.max(from - steps, 0);
                    String label = color + " " + from + " -" + steps;
                    assertEquals(expected, p.getProgress(), label);
                    assertEquals(g.getBoardCellAt(color, expected),
                            g.getBoardCell(p), label);
                    assertEquals(expected > g.getEntrySteps(color)
                            ? State.HOME_PATH : State.TRACK,
                            p.getState(), label);
                }
            }
        }
    }

    private static PlayerColor other(PlayerColor c) {
        return c == PlayerColor.RED
                ? PlayerColor.GREEN : PlayerColor.RED;
    }

    // ---------- FR-5.9: start square clamping ----------

    @Test
    void landingExactlyOnTheStartSquareIsAllowed() {
        GameState g = game();
        Piece red = piece(g, RED, 0);
        place(red, 3);

        g.moveBackward(red, 3);

        assertEquals(0, red.getProgress());
        assertEquals(State.TRACK, red.getState());
    }

    @Test
    void movingBackPastTheStartSquareStopsOnTheStartSquare() {
        GameState g = game();
        Piece red = piece(g, RED, 0);
        place(red, 2);

        MoveResult result = g.moveBackward(red, 5);

        assertEquals(0, red.getProgress());
        assertEquals(State.TRACK, red.getState());
        assertEquals(2, result.fromProgress());
        assertEquals(0, result.toProgress());
    }

    @Test
    void aPieceOnItsStartSquareStaysThereAndIsNotSentToTheCastle() {
        GameState g = game();
        Piece red = piece(g, RED, 0);
        place(red, 0);

        MoveResult result = g.moveBackward(red, 3);

        assertEquals(0, red.getProgress());
        assertEquals(State.TRACK, red.getState());
        assertEquals(0, result.fromProgress());
        assertEquals(0, result.toProgress());
        assertFalse(result.isCapture());
    }

    @Test
    void clampingNeverWrapsAroundTheTrack() {
        GameState g = game();
        Piece green = piece(g, GREEN, 0);
        place(green, 1);        // index 14, green start is 13

        g.moveBackward(green, 50);

        assertEquals(0, green.getProgress());
        assertEquals(g.getBoardCellAt(PlayerColor.GREEN, 0),
                g.getBoardCell(green));
    }

    // ---------- zero distance ----------

    @Test
    void zeroStepsChangeNothingAndCaptureNothing() {
        GameState g = game();
        Piece red = piece(g, RED, 0);
        Piece green = piece(g, GREEN, 0);
        place(red, 20);          // index 20
        place(green, 7);         // (13 + 7) % 52 = 20, same square

        MoveResult result = g.moveBackward(red, 0);

        assertEquals(20, red.getProgress());
        assertEquals(State.TRACK, green.getState());
        assertEquals(7, green.getProgress());
        assertFalse(result.isCapture());
        assertEquals(20, result.fromProgress());
        assertEquals(20, result.toProgress());
    }

    @Test
    void negativeStepsAreRejectedWithoutChangingAnything() {
        GameState g = game();
        Piece red = piece(g, RED, 0);
        place(red, 20);

        assertThrows(IllegalArgumentException.class,
                () -> g.moveBackward(red, -1));

        assertEquals(20, red.getProgress());
        assertTrue(g.getLastMoveResult().isEmpty());
    }

    // ---------- FR-5.4: capture on a backward landing ----------

    @Test
    void landingBackwardOnASingleOpponentCapturesIt() {
        GameState g = game();
        Piece red = piece(g, RED, 0);
        Piece green = piece(g, GREEN, 0);
        place(red, 20);
        place(green, 4);         // (13 + 4) % 52 = 17

        MoveResult result = g.moveBackward(red, 3);   // red -> 17

        assertEquals(17, red.getProgress());
        assertInCastle(green);
        assertEquals(Optional.of(green), result.captured());
    }

    @Test
    void backwardCaptureWorksAcrossTheWrapAroundOfTheTrack() {
        GameState g = game();
        Piece green = piece(g, GREEN, 0);
        Piece red = piece(g, RED, 0);
        place(green, 40);        // index 1
        place(red, 50);          // index 50 (red's arrow)

        g.moveBackward(green, 3);    // index 1 -> 50, via 0 and 51

        assertEquals(37, green.getProgress());
        assertInCastle(red);
    }

    @Test
    void passingBackOverAnOpponentDoesNotCaptureIt() {
        GameState g = game();
        Piece red = piece(g, RED, 0);
        Piece green = piece(g, GREEN, 0);
        place(red, 20);
        place(green, 5);         // index 18, between 20 and 17

        g.moveBackward(red, 3);

        assertEquals(State.TRACK, green.getState());
        assertEquals(5, green.getProgress());
    }

    @Test
    void ownPiecesAreNeverCapturedByABackwardMove() {
        GameState g = game();
        Piece first = piece(g, RED, 0);
        Piece second = piece(g, RED, 1);
        place(first, 20);
        place(second, 17);

        g.moveBackward(first, 3);

        assertEquals(17, first.getProgress());
        assertEquals(State.TRACK, second.getState());
        assertEquals(17, second.getProgress());
    }

    @Test
    void twoOpponentPiecesOnTheLandingSquareAreNotCaptured() {
        // Double Blocks belong to another owner; capture needs ONE piece.
        GameState g = game();
        Piece red = piece(g, RED, 0);
        place(red, 20);
        place(piece(g, GREEN, 0), 4);
        place(piece(g, GREEN, 1), 4);

        MoveResult result = g.moveBackward(red, 3);

        assertFalse(result.isCapture());
        assertEquals(State.TRACK, piece(g, GREEN, 0).getState());
        assertEquals(State.TRACK, piece(g, GREEN, 1).getState());
    }

    // ---------- protected squares ----------

    @Test
    void landingBackwardOnAnotherPlayersStartSquareCapturesNothing() {
        GameState g = game();
        Piece red = piece(g, RED, 0);
        Piece green = piece(g, GREEN, 0);
        place(red, 16);          // index 16
        place(green, 0);         // green's start, index 13

        g.moveBackward(red, 3);  // red -> 13

        assertEquals(13, red.getProgress());
        assertEquals(State.TRACK, green.getState());
        assertEquals(0, green.getProgress());
    }

    @Test
    void clampingOntoTheStartSquareDoesNotCaptureAnOpponentThere() {
        GameState g = game();
        Piece red = piece(g, RED, 0);
        Piece green = piece(g, GREEN, 0);
        place(red, 2);
        place(green, 39);        // (13 + 39) % 52 = 0 = red's start

        g.moveBackward(red, 9);

        assertEquals(0, red.getProgress());
        assertEquals(State.TRACK, green.getState());
        assertEquals(39, green.getProgress());
    }

    @Test
    void extraProtectedSquaresFromTheBoardOwnerBlockBackwardCapture() {
        BoardCell safe = new EasyBoard().getSharedTrackCell(17);
        GameState g = new GameState(List.of(RED, GREEN), firstSeat(),
                cell -> cell.equals(safe));
        Piece red = piece(g, RED, 0);
        Piece green = piece(g, GREEN, 0);
        place(red, 20);
        place(green, 4);         // index 17 = the safe square

        MoveResult result = g.moveBackward(red, 3);

        assertEquals(17, red.getProgress());
        assertFalse(result.isCapture());
        assertEquals(State.TRACK, green.getState());
    }

    // ---------- pieces that cannot move backward ----------

    @Test
    void aPieceInTheCastleCannotMoveBackward() {
        GameState g = game();
        Piece red = piece(g, RED, 0);

        assertFalse(g.canMoveBackward(red));
        assertThrows(IllegalArgumentException.class,
                () -> g.moveBackward(red, 2));
        assertInCastle(red);
        assertTrue(g.getLastMoveResult().isEmpty());
    }

    @Test
    void aFinishedPieceCannotMoveBackward() {
        GameState g = game();
        Piece red = piece(g, RED, 0);
        red.moveTo(56, State.FINISHED);

        assertFalse(g.canMoveBackward(red));
        assertThrows(IllegalArgumentException.class,
                () -> g.moveBackward(red, 2));
        assertEquals(56, red.getProgress());
        assertEquals(State.FINISHED, red.getState());
    }

    @Test
    void piecesOnTheBoardCanMoveBackward() {
        GameState g = game();
        Piece onTrack = piece(g, RED, 0);
        Piece onHomePath = piece(g, RED, 1);
        place(onTrack, 10);
        onHomePath.moveTo(53, State.HOME_PATH);

        assertTrue(g.canMoveBackward(onTrack));
        assertTrue(g.canMoveBackward(onHomePath));
    }

    @Test
    void nullAndForeignPiecesAreRejected() {
        GameState g = game();
        GameState other = game();

        assertThrows(IllegalArgumentException.class,
                () -> g.canMoveBackward(null));
        assertThrows(IllegalArgumentException.class,
                () -> g.moveBackward(null, 1));
        assertThrows(IllegalArgumentException.class,
                () -> g.moveBackward(piece(other, RED, 0), 1));
    }

    // ---------- ASSUMPTION: home path (not defined by the SRS) ----------

    @Test
    void homePathPieceWalksBackAlongTheHomePath() {
        GameState g = game();
        Piece red = piece(g, RED, 0);
        red.moveTo(54, State.HOME_PATH);

        g.moveBackward(red, 2);

        assertEquals(52, red.getProgress());
        assertEquals(State.HOME_PATH, red.getState());
        assertEquals(g.getBoardCellAt(PlayerColor.RED, 52),
                g.getBoardCell(red));
    }

    @Test
    void homePathPieceCanLeaveTheHomePathBackToTheArrowAndTrack() {
        GameState g = game();
        Piece red = piece(g, RED, 0);
        red.moveTo(52, State.HOME_PATH);

        g.moveBackward(red, 2);              // exactly the arrow, 50
        assertEquals(50, red.getProgress());
        assertEquals(State.TRACK, red.getState());

        g.moveBackward(red, 3);              // onto the shared track
        assertEquals(47, red.getProgress());
        assertEquals(State.TRACK, red.getState());
    }

    // ---------- turn rules are untouched ----------

    @Test
    void backwardMovementDoesNotChangeTurnOrPendingRoll() {
        GameState g = game();
        Piece red = piece(g, RED, 0);
        Piece green = piece(g, GREEN, 0);
        place(red, 20);
        place(green, 30);

        assertEquals(Outcome.MOVE_REQUIRED, g.recordRoll(RED, 4));

        // an opponent's piece is moved while red's roll is pending
        g.moveBackward(green, 5);

        assertEquals(25, green.getProgress());
        assertEquals(RED, g.getActivePlayer());
        assertEquals(4, g.getPendingRoll());
        assertTrue(g.isAwaitingMove());
        assertEquals(0, g.getConsecutiveSixes());

        assertEquals(Outcome.TURN_PASSED, g.movePiece(RED, red));
        assertEquals(24, red.getProgress());
    }

    @Test
    void forwardMovementAndCaptureStillWorkAfterABackwardMove() {
        GameState g = game();
        Piece red = piece(g, RED, 0);
        Piece green = piece(g, GREEN, 0);
        place(red, 10);
        place(green, 47);        // index 8

        g.moveBackward(red, 3);              // red 10 -> 7
        g.recordRoll(RED, 1);
        g.movePiece(RED, red);               // red 7 -> 8, captures

        assertEquals(8, red.getProgress());
        assertInCastle(green);
    }
}
