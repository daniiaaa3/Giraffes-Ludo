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
 * Unit tests for capture (FR-5.4) and protected squares (FR-3.1,
 * FR-3.2, FR-3.4).
 *
 * Square arithmetic used below (shared track of 52 squares):
 * a piece of a color with start square s and progress p (up to
 * its arrow at progress 50) stands on track index (s + p) % 52.
 * RED starts at 0, GREEN at 13, YELLOW at 26, BLUE at 39.
 *
 * Iteration: 2
 */
public class CaptureAndProtectionTest {

    private static final PlayerEntry RED =
            new PlayerEntry("Dania", PlayerColor.RED);
    private static final PlayerEntry GREEN =
            new PlayerEntry("Nay", PlayerColor.GREEN);
    private static final PlayerEntry BLUE =
            new PlayerEntry("Marwa", PlayerColor.BLUE);
    private static final PlayerEntry YELLOW =
            new PlayerEntry("Yara", PlayerColor.YELLOW);

    /** Always starts the first listed player. */
    private static Random firstSeat() {
        return new Random() {
            @Override
            public int nextInt(int bound) {
                return 0;
            }
        };
    }

    /** The first listed player moves first. */
    private static GameState game(PlayerEntry... players) {
        return new GameState(List.of(players), firstSeat());
    }

    private static Piece piece(GameState g, PlayerEntry p, int id) {
        return g.getPieces(p).get(id);
    }

    private static void place(Piece p, int progress) {
        p.moveTo(progress, State.TRACK);
    }

    /** Rolls and moves the given piece with the given roll. */
    private static Outcome play(GameState g, PlayerEntry player,
                                int roll, Piece moved) {
        g.recordRoll(player, roll);
        return g.movePiece(player, moved);
    }

    private static void assertInCastle(Piece p) {
        assertEquals(State.CASTLE, p.getState());
        assertEquals(Piece.CASTLE_PROGRESS, p.getProgress());
    }

    // ---------- capture (FR-5.4) ----------

    @Test
    void landingOnASingleOpponentOnAnUnprotectedSquareCapturesIt() {
        GameState g = game(RED, GREEN);
        Piece red = piece(g, RED, 0);
        Piece green = piece(g, GREEN, 0);
        place(red, 5);              // track index 5
        place(green, 47);           // (13 + 47) % 52 = 8

        Outcome outcome = play(g, RED, 3, red);   // red lands on 8

        assertEquals(Outcome.TURN_PASSED, outcome);
        assertEquals(8, red.getProgress());
        assertInCastle(green);
        assertEquals(g.getBoardCell(red),
                Optional.of(new EasyBoard().getSharedTrackCell(8)));
    }

    @Test
    void capturedPieceCanBeReleasedAgainWithASix() {
        GameState g = game(RED, GREEN);
        Piece red = piece(g, RED, 0);
        Piece green = piece(g, GREEN, 0);
        place(red, 5);
        place(green, 47);
        play(g, RED, 3, red);

        assertEquals(GREEN, g.getActivePlayer());
        assertEquals(Outcome.MOVE_REQUIRED, g.recordRoll(GREEN, 6));
        assertTrue(g.getLegalPieces(GREEN, 6).contains(green));
    }

    @Test
    void captureGivesNoExtraRollByItself() {
        GameState g = game(RED, GREEN);
        Piece red = piece(g, RED, 0);
        place(red, 5);
        place(piece(g, GREEN, 0), 47);

        assertEquals(Outcome.TURN_PASSED, play(g, RED, 3, red));
        assertEquals(GREEN, g.getActivePlayer());
    }

    @Test
    void aSixThatCapturesStillGivesItsNormalBonusRoll() {
        GameState g = game(RED, GREEN);
        Piece red = piece(g, RED, 0);
        Piece green = piece(g, GREEN, 0);
        place(red, 5);
        place(green, 50);           // (13 + 50) % 52 = 11

        Outcome outcome = play(g, RED, 6, red);   // red lands on 11

        assertEquals(Outcome.BONUS_ROLL, outcome);
        assertInCastle(green);
        assertEquals(RED, g.getActivePlayer());
    }

    @Test
    void captureWorksAcrossTheWrapAroundOfTheSharedTrack() {
        GameState g = game(GREEN, RED);
        Piece green = piece(g, GREEN, 0);
        Piece red = piece(g, RED, 0);
        place(green, 36);           // index 49
        place(red, 1);              // index 1

        play(g, GREEN, 4, green);   // 49 + 4 = 53 -> index 1

        assertEquals(40, green.getProgress());
        assertInCastle(red);
    }

    @Test
    void anotherColorsArrowSquareIsARegularSquare() {
        GameState g = game(GREEN, RED);
        Piece green = piece(g, GREEN, 0);
        Piece red = piece(g, RED, 0);
        place(green, 33);           // index 46
        place(red, 50);             // red's own arrow, index 50

        play(g, GREEN, 4, green);   // green lands on index 50

        assertInCastle(red);
    }

    @Test
    void onlyThePieceOnTheLandingSquareIsCaptured() {
        GameState g = game(RED, GREEN, BLUE);
        Piece red = piece(g, RED, 0);
        Piece green = piece(g, GREEN, 0);
        Piece blue = piece(g, BLUE, 0);
        place(red, 5);
        place(green, 47);           // index 8
        place(blue, 22);            // (39 + 22) % 52 = 9

        play(g, RED, 3, red);

        assertInCastle(green);
        assertEquals(22, blue.getProgress());
        assertEquals(State.TRACK, blue.getState());
        assertEquals(Optional.of(green),
                g.getLastMoveResult().orElseThrow().captured());
    }

    @Test
    void passingOverAnOpponentDoesNotCaptureIt() {
        GameState g = game(RED, GREEN);
        Piece red = piece(g, RED, 0);
        Piece green = piece(g, GREEN, 0);
        place(red, 5);
        place(green, 46);           // index 7, between 5 and 9

        play(g, RED, 4, red);       // red ends on 9

        assertEquals(46, green.getProgress());
        assertEquals(State.TRACK, green.getState());
        assertTrue(g.getLastMoveResult().orElseThrow()
                .captured().isEmpty());
    }

    @Test
    void ownPiecesAreNeverCaptured() {
        GameState g = game(RED, GREEN);
        Piece first = piece(g, RED, 0);
        Piece second = piece(g, RED, 1);
        place(first, 5);
        place(second, 8);

        play(g, RED, 3, first);

        assertEquals(8, first.getProgress());
        assertEquals(8, second.getProgress());
        assertEquals(State.TRACK, second.getState());
    }

    @Test
    void twoOpponentPiecesOnOneSquareAreNotCapturedByThisRule() {
        // Two pieces of one player form a Double Block. Whether a
        // piece may land there is decided by the block rules
        // (FR-5.5 to FR-5.8); capture only applies to ONE piece.
        GameState g = game(RED, GREEN);
        Piece red = piece(g, RED, 0);
        Piece greenOne = piece(g, GREEN, 0);
        Piece greenTwo = piece(g, GREEN, 1);
        place(red, 5);
        place(greenOne, 47);
        place(greenTwo, 47);

        play(g, RED, 3, red);

        assertEquals(State.TRACK, greenOne.getState());
        assertEquals(State.TRACK, greenTwo.getState());
    }

    @Test
    void castleAndFinishedPiecesAreNeverCaptured() {
        GameState g = game(RED, GREEN);
        Piece red = piece(g, RED, 0);
        place(red, 5);
        piece(g, GREEN, 1).moveTo(56, State.FINISHED);
        // the other three green pieces stay in the castle

        play(g, RED, 3, red);

        assertEquals(State.FINISHED, piece(g, GREEN, 1).getState());
        assertInCastle(piece(g, GREEN, 0));
    }

    // ---------- protected squares (FR-3.1, FR-3.2, FR-3.4) ----------

    @Test
    void everyStartSquareIsProtectedForAllPlayers() {
        GameState g = game(RED, GREEN, BLUE, YELLOW);

        for (PlayerColor color : PlayerColor.values()) {
            BoardCell start = g.getBoardCellAt(color, 0).orElseThrow();
            assertTrue(g.isProtectedSquare(start), color + " start");
        }
    }

    @Test
    void theFinalHomeSquareOfEveryColorIsProtected() {
        GameState g = game(RED, GREEN, BLUE, YELLOW);

        for (PlayerColor color : PlayerColor.values()) {
            BoardCell last = g.getBoardCellAt(
                    color, g.getFinishSteps(color)).orElseThrow();
            BoardCell before = g.getBoardCellAt(
                    color, g.getFinishSteps(color) - 1).orElseThrow();

            assertTrue(g.isProtectedSquare(last), color + " final");
            assertFalse(g.isProtectedSquare(before),
                    color + " square before the final one");
        }
    }

    @Test
    void ordinarySquaresAndArrowSquaresAreNotProtected() {
        GameState g = game(RED, GREEN);
        EasyBoard board = new EasyBoard();

        assertFalse(g.isProtectedSquare(board.getSharedTrackCell(5)));
        assertFalse(g.isProtectedSquare(board.getSharedTrackCell(8)));

        for (PlayerColor color : PlayerColor.values()) {
            BoardCell arrow = board.getArrowSquare(color);
            assertFalse(g.isProtectedSquare(arrow), color + " arrow");
        }

        assertFalse(g.isProtectedSquare(null));
    }

    @Test
    void anOpponentOnMyStartSquareIsNotCapturedAndBothCoexist() {
        // RED start = index 0. GREEN progress 39 is index 0 too.
        GameState g = game(GREEN, RED);
        Piece green = piece(g, GREEN, 0);
        Piece red = piece(g, RED, 0);
        place(green, 36);           // index 49
        place(red, 0);              // red stands on its own start

        play(g, GREEN, 3, green);   // green lands on index 52 % 52 = 0

        assertEquals(39, green.getProgress());
        assertEquals(State.TRACK, red.getState());
        assertEquals(0, red.getProgress());
        assertEquals(g.getBoardCell(green), g.getBoardCell(red));
    }

    @Test
    void landingOnAnotherPlayersStartSquareCapturesNothing() {
        GameState g = game(RED, GREEN);
        Piece red = piece(g, RED, 0);
        Piece green = piece(g, GREEN, 0);
        place(red, 10);
        place(green, 0);            // green's own start, index 13

        play(g, RED, 3, red);       // red lands on index 13

        assertEquals(13, red.getProgress());
        assertEquals(State.TRACK, green.getState());
        assertEquals(0, green.getProgress());
    }

    @Test
    void releasingOntoAStartSquareHoldingAnOpponentDoesNotCapture() {
        GameState g = game(RED, GREEN);
        Piece red = piece(g, RED, 0);          // in the castle
        Piece green = piece(g, GREEN, 0);
        place(green, 39);           // (13 + 39) % 52 = 0 = red's start

        play(g, RED, 6, red);

        assertEquals(0, red.getProgress());
        assertEquals(39, green.getProgress());
        assertEquals(State.TRACK, green.getState());
    }

    @Test
    void extraProtectedSquaresFromTheBoardOwnerBlockCapture() {
        BoardCell safe = new EasyBoard().getSharedTrackCell(8);
        GameState g = new GameState(List.of(RED, GREEN), firstSeat(),
                cell -> cell.equals(safe));
        Piece red = piece(g, RED, 0);
        Piece green = piece(g, GREEN, 0);
        place(red, 5);
        place(green, 47);           // index 8 = the safe square

        play(g, RED, 3, red);

        assertTrue(g.isProtectedSquare(safe));
        assertEquals(8, red.getProgress());
        assertEquals(State.TRACK, green.getState());
        assertEquals(47, green.getProgress());
        assertTrue(g.getLastMoveResult().orElseThrow()
                .captured().isEmpty());
    }

    @Test
    void protectionRulesCanBeCombined() {
        BoardCell one = new EasyBoard().getSharedTrackCell(3);
        BoardCell two = new EasyBoard().getSharedTrackCell(4);
        ProtectedSquares rule = ((ProtectedSquares) c -> c.equals(one))
                .or(c -> c.equals(two));

        assertTrue(rule.isProtected(one));
        assertTrue(rule.isProtected(two));
        assertFalse(rule.isProtected(new EasyBoard().getSharedTrackCell(5)));
        assertFalse(ProtectedSquares.none().isProtected(one));
        assertThrows(IllegalArgumentException.class,
                () -> rule.or(null));
    }

    @Test
    void protectionRuleCannotBeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new GameState(List.of(RED, GREEN), firstSeat(),
                        null));
    }

    // ---------- move result ----------

    @Test
    void lastMoveResultStartsEmptyAndDescribesEachMove() {
        GameState g = game(RED, GREEN);
        Piece red = piece(g, RED, 0);
        place(red, 20);

        assertTrue(g.getLastMoveResult().isEmpty());

        play(g, RED, 4, red);
        MoveResult result = g.getLastMoveResult().orElseThrow();

        assertSame(red, result.piece());
        assertEquals(20, result.fromProgress());
        assertEquals(24, result.toProgress());
        assertFalse(result.isCapture());
    }

    @Test
    void releaseIsReportedAsAMoveFromTheCastle() {
        GameState g = game(RED, GREEN);
        Piece red = piece(g, RED, 0);

        play(g, RED, 6, red);
        MoveResult result = g.getLastMoveResult().orElseThrow();

        assertEquals(Piece.CASTLE_PROGRESS, result.fromProgress());
        assertEquals(0, result.toProgress());
        assertFalse(result.isCapture());
    }

    @Test
    void moveResultRejectsNulls() {
        GameState g = game(RED, GREEN);

        assertThrows(IllegalArgumentException.class,
                () -> new MoveResult(null, 0, 1, Optional.empty()));
        assertThrows(IllegalArgumentException.class,
                () -> new MoveResult(piece(g, RED, 0), 0, 1, null));
    }
}
