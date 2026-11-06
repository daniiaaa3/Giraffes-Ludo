
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
 * Unit tests for GameState: legal piece selection (FR-5.1),
 * one move per roll (FR-5.2), castle release, track and home
 * path movement, exact finish, and integration with
 * TurnRollRules.
 *
 * Iteration: 1
 */
public class GameStateTest {

    private static final PlayerEntry A =
            new PlayerEntry("Dania", PlayerColor.RED);
    private static final PlayerEntry B =
            new PlayerEntry("Marwa", PlayerColor.BLUE);
    private static final PlayerEntry C =
            new PlayerEntry("Nay", PlayerColor.GREEN);
    private static final PlayerEntry D =
            new PlayerEntry("Yara", PlayerColor.YELLOW);

    private static final List<PlayerEntry> ALL =
            List.of(A, B, C, D);

    /** Always starts the first listed player. */
    private static Random firstSeat() {
        return new Random() {
            @Override
            public int nextInt(int bound) {
                return 0;
            }
        };
    }

    private static GameState game(int players) {
        return new GameState(
                ALL.subList(0, players), firstSeat());
    }

    private static Piece piece(GameState g, PlayerEntry p, int id) {
        return g.getPieces(p).get(id);
    }

    // Puts a piece on the board without playing the moves.
    private static void place(Piece p, int progress, State s) {
        p.moveTo(progress, s);
    }

    private static void assertUnchanged(
            GameState g, PlayerEntry active, int pending,
            Piece p, int progress, State state) {

        assertEquals(active, g.getActivePlayer());
        assertEquals(pending, g.getPendingRoll());
        assertEquals(progress, p.getProgress());
        assertEquals(state, p.getState());
    }

    // ---------- setup ----------

    @Test
    void eachPlayerGetsFourCastlePiecesForTwoToFourPlayers() {
        for (int n = 2; n <= 4; n++) {
            GameState g = game(n);

            assertEquals(n, g.getPlayers().size());

            for (PlayerEntry p : g.getPlayers()) {
                List<Piece> own = g.getPieces(p);
                assertEquals(4, own.size());

                for (int i = 0; i < 4; i++) {
                    assertEquals(i, own.get(i).getId());
                    assertEquals(p.color(), own.get(i).getColor());
                    assertTrue(own.get(i).isInCastle());
                }
            }
        }
    }

    @Test
    void gameStartsAwaitingRollForFirstPlayer() {
        GameState g = game(3);

        assertEquals(A, g.getActivePlayer());
        assertTrue(g.isAwaitingRoll());
        assertFalse(g.isAwaitingMove());
        assertEquals(0, g.getPendingRoll());
        assertTrue(g.getSelectablePieces().isEmpty());
    }

    @Test
    void rejectsInvalidConstruction() {
        TurnManager tm = new TurnManager(
                List.of(A, B), firstSeat());

        assertThrows(IllegalArgumentException.class,
                () -> new GameState(null, tm));
        assertThrows(IllegalArgumentException.class,
                () -> new GameState(new EasyBoard(), null));

        // Distinct players that share a color.
        PlayerEntry clash =
                new PlayerEntry("Other", PlayerColor.RED);
        TurnManager same = new TurnManager(
                List.of(A, clash), firstSeat());

        assertThrows(IllegalArgumentException.class,
                () -> new GameState(new EasyBoard(), same));
    }

    @Test
    void unknownColorOrPlayerIsRejected() {
        GameState g = game(2); // RED and BLUE only

        assertThrows(IllegalArgumentException.class,
                () -> g.getPieces(PlayerColor.GREEN));
        assertThrows(IllegalArgumentException.class,
                () -> g.getPieces((PlayerColor) null));
        assertThrows(IllegalArgumentException.class,
                () -> g.getPieces(C));
        assertThrows(IllegalArgumentException.class,
                () -> g.getLegalPieces(C, 6));
    }

    // ---------- geometry verification ----------

    @Test
    void entryAndFinishStepsComeFromBoardGeometry() {
        GameState g = game(4);

        for (PlayerColor color : PlayerColor.values()) {
            assertEquals(50, g.getEntrySteps(color));
            assertEquals(56, g.getFinishSteps(color));
        }
    }

    @Test
    void arrowSitsOnTrackFiftyStepsAfterStart() {
        EasyBoard board = new EasyBoard();

        for (PlayerColor color : PlayerColor.values()) {
            int arrowIndex = board.getSharedTrackCells()
                    .indexOf(board.getArrowSquare(color));

            assertTrue(arrowIndex >= 0);
            assertEquals(50, Math.floorMod(
                    arrowIndex - board.getStartSquare(color), 52));
        }
    }

    @Test
    void arrowTouchesFirstHomeSquareAndHomePathIsConnected() {
        EasyBoard board = new EasyBoard();

        for (PlayerColor color : PlayerColor.values()) {
            BoardCell arrow = board.getArrowSquare(color);
            List<BoardCell> home = board.getHomePathCells(color);

            assertEquals(1, distance(arrow, home.get(0)));

            for (int i = 1; i < home.size(); i++) {
                assertEquals(1,
                        distance(home.get(i - 1), home.get(i)));
            }
        }
    }

    @Test
    void sharedTrackIsConnectedAroundTheBoard() {
        List<BoardCell> track =
                new EasyBoard().getSharedTrackCells();

        // Squares are next to each other, including diagonal
        // steps at the corners and the 51 -> 0 wrap-around.
        for (int i = 0; i < track.size(); i++) {
            BoardCell a = track.get(i);
            BoardCell b = track.get((i + 1) % track.size());

            assertTrue(Math.abs(a.row() - b.row()) <= 1 &&
                    Math.abs(a.col() - b.col()) <= 1,
                    "Track squares " + i + " and "
                            + ((i + 1) % 52) + " are not adjacent");
        }
    }

    private static int distance(BoardCell a, BoardCell b) {
        return Math.abs(a.row() - b.row()) +
                Math.abs(a.col() - b.col());
    }

    // ---------- castle release ----------

    @Test
    void nonSixCannotLeaveCastleAndPassesTurn() {
        for (int roll = 1; roll <= 5; roll++) {
            GameState g = game(2);

            assertTrue(g.getLegalPieces(A, roll).isEmpty());
            assertFalse(g.hasLegalMove(A, roll));

            assertEquals(Outcome.TURN_PASSED,
                    g.recordRoll(A, roll));
            assertEquals(B, g.getActivePlayer());
            assertEquals(0, g.getPendingRoll());
        }
    }

    @Test
    void sixOffersAllCastlePiecesAndWaitsForSelection() {
        GameState g = game(2);

        assertEquals(Outcome.MOVE_REQUIRED, g.recordRoll(A, 6));

        assertTrue(g.isAwaitingMove());
        assertEquals(6, g.getPendingRoll());
        assertEquals(4, g.getSelectablePieces().size());
        assertEquals(A, g.getActivePlayer());
    }

    @Test
    void releaseMovesPieceToStartAndGivesBonusRoll() {
        GameState g = game(2);
        Piece p = piece(g, A, 1);

        g.recordRoll(A, 6);

        assertEquals(Outcome.BONUS_ROLL, g.movePiece(A, p));

        assertEquals(State.TRACK, p.getState());
        assertEquals(0, p.getProgress());
        assertEquals(Optional.of(
                new EasyBoard().getSharedTrackCell(0)),
                g.getBoardCell(p));

        // Same player rolls again; only the one piece moved.
        assertEquals(A, g.getActivePlayer());
        assertTrue(g.isAwaitingRoll());
        assertTrue(piece(g, A, 0).isInCastle());
        assertTrue(piece(g, A, 2).isInCastle());
    }

    @Test
    void releaseUsesEachColorsOwnStartSquare() {
        EasyBoard board = new EasyBoard();

        for (PlayerEntry player : ALL) {
            GameState g = game(4);

            // Hand the turn to this player with plain rolls.
            while (!g.getActivePlayer().equals(player)) {
                g.recordRoll(g.getActivePlayer(), 1);
            }

            Piece p = piece(g, player, 0);
            g.recordRoll(player, 6);
            g.movePiece(player, p);

            assertEquals(Optional.of(board.getSharedTrackCell(
                    board.getStartSquare(player.color()))),
                    g.getBoardCell(p));
        }
    }

    // ---------- FR-5.1: ownership and selection ----------

    @Test
    void onlyOwnPiecesAreEverLegal() {
        GameState g = game(4);

        for (Piece p : g.getLegalPieces(A, 6)) {
            assertEquals(PlayerColor.RED, p.getColor());
        }
    }

    @Test
    void cannotMoveAnotherPlayersPiece() {
        GameState g = game(2);
        Piece theirs = piece(g, B, 0);
        Piece mine = piece(g, A, 0);

        g.recordRoll(A, 6);

        assertThrows(IllegalArgumentException.class,
                () -> g.movePiece(A, theirs));

        assertUnchanged(g, A, 6, theirs, -1, State.CASTLE);
        assertTrue(g.isAwaitingMove());

        // The legitimate move is still possible afterwards.
        assertDoesNotThrow(() -> g.movePiece(A, mine));
    }

    @Test
    void cannotMoveNullOrForeignGamePiece() {
        GameState g = game(2);
        Piece outsider = new Piece(PlayerColor.RED, 0);

        g.recordRoll(A, 6);

        assertThrows(IllegalArgumentException.class,
                () -> g.movePiece(A, null));
        assertThrows(IllegalArgumentException.class,
                () -> g.movePiece(A, outsider));

        assertEquals(6, g.getPendingRoll());
        assertTrue(outsider.isInCastle());
    }

    @Test
    void illegalPieceForTheRollIsRejected() {
        GameState g = game(2);
        Piece out = piece(g, A, 0);
        Piece inCastle = piece(g, A, 1);

        place(out, 10, State.TRACK);

        g.recordRoll(A, 3);

        // Castle piece cannot move on a 3.
        assertThrows(IllegalArgumentException.class,
                () -> g.movePiece(A, inCastle));

        assertUnchanged(g, A, 3, inCastle, -1, State.CASTLE);
        assertEquals(10, out.getProgress());
    }

    // ---------- FR-5.2: one move per roll ----------

    @Test
    void selectablePiecesAreOnlyThoseThatCanUseTheRoll() {
        GameState g = game(2);
        Piece onTrack = piece(g, A, 0);
        place(onTrack, 20, State.TRACK);

        g.recordRoll(A, 4);

        // Castle pieces cannot use a 4; the track piece can.
        assertEquals(List.of(onTrack), g.getSelectablePieces());
    }

    @Test
    void movePiecesUsesTheWholeRollAndPassesTurn() {
        GameState g = game(2);
        Piece p = piece(g, A, 0);
        place(p, 20, State.TRACK);

        g.recordRoll(A, 4);

        assertEquals(Outcome.TURN_PASSED, g.movePiece(A, p));
        assertEquals(24, p.getProgress());
        assertEquals(B, g.getActivePlayer());
        assertEquals(0, g.getPendingRoll());
    }

    @Test
    void secondPieceCannotUseTheSameRoll() {
        GameState g = game(2);
        Piece first = piece(g, A, 0);
        Piece second = piece(g, A, 1);
        place(first, 20, State.TRACK);
        place(second, 30, State.TRACK);

        g.recordRoll(A, 4);
        g.movePiece(A, first);

        // Turn passed, so A is out of turn now...
        assertThrows(IllegalStateException.class,
                () -> g.movePiece(A, second));
        assertEquals(30, second.getProgress());
    }

    @Test
    void secondMoveAfterBonusRollNeedsANewRoll() {
        GameState g = game(2);
        Piece first = piece(g, A, 0);
        Piece second = piece(g, A, 1);
        place(first, 20, State.TRACK);
        place(second, 30, State.TRACK);

        g.recordRoll(A, 6);
        assertEquals(Outcome.BONUS_ROLL, g.movePiece(A, first));

        // A is still active but has no roll to spend.
        assertThrows(IllegalStateException.class,
                () -> g.movePiece(A, second));

        assertEquals(26, first.getProgress());
        assertEquals(30, second.getProgress());
        assertEquals(A, g.getActivePlayer());

        // A fresh roll authorizes exactly one new move.
        g.recordRoll(A, 2);
        g.movePiece(A, second);
        assertEquals(32, second.getProgress());
        assertEquals(26, first.getProgress());
    }

    @Test
    void cannotRollWhileLegalMoveIsPending() {
        GameState g = game(2);
        Piece p = piece(g, A, 0);
        place(p, 20, State.TRACK);

        g.recordRoll(A, 4);

        assertThrows(IllegalStateException.class,
                () -> g.recordRoll(A, 2));

        assertUnchanged(g, A, 4, p, 20, State.TRACK);
        assertTrue(g.isAwaitingMove());
    }

    @Test
    void moveWithoutAPendingRollIsRejected() {
        GameState g = game(2);
        Piece p = piece(g, A, 0);
        place(p, 20, State.TRACK);

        assertThrows(IllegalStateException.class,
                () -> g.movePiece(A, p));

        assertUnchanged(g, A, 0, p, 20, State.TRACK);
    }

    // ---------- track and home path ----------

    @Test
    void trackWrapsAroundTheSharedTrack() {
        GameState g = game(3);
        Piece p = piece(g, C, 0); // GREEN starts at index 13
        EasyBoard board = new EasyBoard();

        // Progress 38 is track index 51; two more steps wrap.
        place(p, 38, State.TRACK);
        assertEquals(Optional.of(board.getSharedTrackCell(51)),
                g.getBoardCell(p));

        g.recordRoll(A, 1);
        g.recordRoll(B, 1);
        g.recordRoll(C, 2);
        g.movePiece(C, p);

        assertEquals(40, p.getProgress());
        assertEquals(State.TRACK, p.getState());
        assertEquals(Optional.of(board.getSharedTrackCell(1)),
                g.getBoardCell(p));
    }

    @Test
    void pieceEntersHomePathAfterTheArrow() {
        GameState g = game(2);
        Piece p = piece(g, A, 0);
        EasyBoard board = new EasyBoard();

        place(p, 48, State.TRACK);
        g.recordRoll(A, 3);
        g.movePiece(A, p);

        assertEquals(51, p.getProgress());
        assertEquals(State.HOME_PATH, p.getState());
        assertEquals(Optional.of(
                board.getHomePathCells(PlayerColor.RED).get(0)),
                g.getBoardCell(p));
    }

    @Test
    void pieceOnTheArrowSquareIsStillOnTheTrack() {
        GameState g = game(2);
        Piece p = piece(g, A, 0);
        EasyBoard board = new EasyBoard();

        place(p, 47, State.TRACK);
        g.recordRoll(A, 3);
        g.movePiece(A, p);

        assertEquals(50, p.getProgress());
        assertEquals(State.TRACK, p.getState());
        assertEquals(Optional.of(
                board.getArrowSquare(PlayerColor.RED)),
                g.getBoardCell(p));
    }

    @Test
    void passingTheArrowContinuesRemainingStepsOnHomePath() {
        EasyBoard board = new EasyBoard();

        // progress before the roll, roll, expected home index
        int[][] cases = {
                {49, 2, 0},   // 1 step to arrow, 1 left over
                {49, 5, 3},   // 1 step to arrow, 4 left over
                {47, 6, 2},   // 3 steps to arrow, 3 left over
                {45, 6, 0},   // 5 steps to arrow, 1 left over
                {50, 1, 0},   // standing on the arrow
                {50, 4, 3}
        };

        for (PlayerColor color : PlayerColor.values()) {
            for (int[] c : cases) {
                GameState g = new GameState(
                        List.of(new PlayerEntry("P", color),
                                new PlayerEntry("Q",
                                        color == PlayerColor.RED
                                                ? PlayerColor.BLUE
                                                : PlayerColor.RED)),
                        firstSeat());
                PlayerEntry me = g.getPlayers().get(0);
                Piece p = g.getPieces(me).get(0);

                place(p, c[0], State.TRACK);
                g.recordRoll(me, c[1]);
                g.movePiece(me, p);

                assertEquals(State.HOME_PATH, p.getState());
                assertEquals(Optional.of(board
                        .getHomePathCells(color).get(c[2])),
                        g.getBoardCell(p));
            }
        }
    }

    @Test
    void landingExactlyOnTheArrowKeepsPieceOnTheTrack() {
        EasyBoard board = new EasyBoard();

        for (PlayerColor color : PlayerColor.values()) {
            GameState g = new GameState(
                    List.of(new PlayerEntry("P", color),
                            new PlayerEntry("Q",
                                    color == PlayerColor.RED
                                            ? PlayerColor.BLUE
                                            : PlayerColor.RED)),
                    firstSeat());
            PlayerEntry me = g.getPlayers().get(0);
            Piece p = g.getPieces(me).get(0);

            place(p, 46, State.TRACK);
            g.recordRoll(me, 4);
            g.movePiece(me, p);

            assertEquals(State.TRACK, p.getState());
            assertEquals(Optional.of(board.getArrowSquare(color)),
                    g.getBoardCell(p));
        }
    }

    @Test
    void sixFromTheArrowFinishesExactlyAndSevenStepsWouldNot() {
        GameState g = game(2);
        Piece onArrow = piece(g, A, 0);
        Piece justPast = piece(g, A, 1);

        place(onArrow, 50, State.TRACK);
        place(justPast, 51, State.HOME_PATH);

        // 50 + 6 = 56 finishes; 51 + 6 = 57 overshoots.
        assertTrue(g.getLegalPieces(A, 6).contains(onArrow));
        assertFalse(g.getLegalPieces(A, 6).contains(justPast));

        g.recordRoll(A, 6);
        g.movePiece(A, onArrow);

        assertTrue(onArrow.isFinished());
        assertEquals(56, onArrow.getProgress());
    }

    @Test
    void trackPieceCannotOvershootTheFinishInOneRoll() {
        GameState g = game(2);
        Piece p = piece(g, A, 0);

        // 52 + 6 = 58 would pass the last home square (56).
        place(p, 52, State.HOME_PATH);
        assertFalse(g.getLegalPieces(A, 6).contains(p));

        place(p, 50, State.TRACK);
        assertTrue(g.getLegalPieces(A, 6).contains(p));
    }

    @Test
    void pieceMovesAlongTheHomePath() {
        GameState g = game(2);
        Piece p = piece(g, A, 0);

        place(p, 51, State.HOME_PATH);
        g.recordRoll(A, 2);
        g.movePiece(A, p);

        assertEquals(53, p.getProgress());
        assertEquals(State.HOME_PATH, p.getState());
    }

    // ---------- exact finish ----------

    @Test
    void exactRollFinishesThePiece() {
        GameState g = game(2);
        Piece p = piece(g, A, 0);
        EasyBoard board = new EasyBoard();

        place(p, 53, State.HOME_PATH);
        g.recordRoll(A, 3);
        g.movePiece(A, p);

        assertEquals(56, p.getProgress());
        assertTrue(p.isFinished());
        assertEquals(Optional.of(board.getHomePathCells(
                PlayerColor.RED).get(5)), g.getBoardCell(p));
    }

    @Test
    void overshootingTheFinishIsNotLegal() {
        GameState g = game(2);
        Piece p = piece(g, A, 0);

        place(p, 54, State.HOME_PATH);

        assertFalse(g.getLegalPieces(A, 3).contains(p));
        assertTrue(g.getLegalPieces(A, 2).contains(p));

        // Nothing else can move, so the turn just passes.
        assertEquals(Outcome.TURN_PASSED, g.recordRoll(A, 3));
        assertEquals(54, p.getProgress());
        assertEquals(B, g.getActivePlayer());
    }

    @Test
    void finishedPiecesAreNeverLegal() {
        GameState g = game(2);
        Piece p = piece(g, A, 0);
        place(p, 56, State.FINISHED);

        for (int roll = 1; roll <= 6; roll++) {
            assertFalse(g.getLegalPieces(A, roll).contains(p));
        }
    }

    @Test
    void sixWithNoLegalMoveStillGivesBonusRoll() {
        GameState g = game(2);

        // Three pieces finished; the fourth would overshoot.
        for (int i = 0; i < 3; i++) {
            place(piece(g, A, i), 56, State.FINISHED);
        }
        Piece last = piece(g, A, 3);
        place(last, 55, State.HOME_PATH);

        assertFalse(g.hasLegalMove(A, 6));
        assertEquals(Outcome.BONUS_ROLL, g.recordRoll(A, 6));

        assertEquals(A, g.getActivePlayer());
        assertEquals(0, g.getPendingRoll());
        assertEquals(55, last.getProgress());
    }

    // ---------- integration with TurnRollRules ----------

    @Test
    void threeSixesCancelTheThirdRollAndMoveNothing() {
        GameState g = game(2);
        Piece p = piece(g, A, 0);
        place(p, 10, State.TRACK);

        g.recordRoll(A, 6);
        g.movePiece(A, p);                       // 16
        g.recordRoll(A, 6);
        g.movePiece(A, p);                       // 22

        assertEquals(Outcome.THIRD_SIX_FORFEIT,
                g.recordRoll(A, 6));

        assertEquals(22, p.getProgress());       // not moved
        assertEquals(0, g.getPendingRoll());
        assertEquals(B, g.getActivePlayer());
        assertEquals(0, g.getConsecutiveSixes());
        assertTrue(g.getSelectablePieces().isEmpty());
    }

    @Test
    void sixThenNonSixSpendsTheBonusRollAndPassesTurn() {
        GameState g = game(2);
        Piece p = piece(g, A, 0);
        place(p, 10, State.TRACK);

        g.recordRoll(A, 6);
        assertEquals(Outcome.BONUS_ROLL, g.movePiece(A, p));
        assertEquals(1, g.getConsecutiveSixes());

        g.recordRoll(A, 3);
        assertEquals(Outcome.TURN_PASSED, g.movePiece(A, p));

        assertEquals(19, p.getProgress());
        assertEquals(B, g.getActivePlayer());
        assertEquals(0, g.getConsecutiveSixes());
    }

    @Test
    void outOfTurnRollIsRejectedWithoutChangingState() {
        GameState g = game(3);
        Piece p = piece(g, B, 0);

        assertThrows(IllegalStateException.class,
                () -> g.recordRoll(B, 6));

        assertUnchanged(g, A, 0, p, -1, State.CASTLE);
        assertTrue(g.isAwaitingRoll());
    }

    @Test
    void outOfTurnMoveIsRejectedWithoutChangingState() {
        GameState g = game(2);
        Piece mine = piece(g, A, 0);
        Piece theirs = piece(g, B, 0);

        g.recordRoll(A, 6);

        assertThrows(IllegalStateException.class,
                () -> g.movePiece(B, theirs));

        assertUnchanged(g, A, 6, theirs, -1, State.CASTLE);
        assertEquals(-1, mine.getProgress());
        assertTrue(g.isAwaitingMove());
    }

    @Test
    void invalidRollsAreRejectedWithoutChangingState() {
        GameState g = game(2);
        Piece p = piece(g, A, 0);

        assertThrows(IllegalArgumentException.class,
                () -> g.recordRoll(A, 0));
        assertThrows(IllegalArgumentException.class,
                () -> g.recordRoll(A, 7));
        assertThrows(IllegalArgumentException.class,
                () -> g.getLegalPieces(A, 9));
        assertThrows(IllegalArgumentException.class,
                () -> g.recordRoll(null, 3));

        assertUnchanged(g, A, 0, p, -1, State.CASTLE);
        assertTrue(g.isAwaitingRoll());
    }

    @Test
    void turnsCycleThroughAllPlayersForTwoToFourPlayers() {
        for (int n = 2; n <= 4; n++) {
            GameState g = game(n);

            for (int i = 0; i < n; i++) {
                assertEquals(ALL.get(i), g.getActivePlayer());
                g.recordRoll(ALL.get(i), 2); // no legal move
            }

            assertEquals(A, g.getActivePlayer());
        }
    }

    @Test
    void aFullTurnSequenceKeepsStateConsistent() {
        GameState g = game(2);

        // A: 2 (nothing), B: 6 -> release, 6 -> release, 4 ->
        // move one piece, then A again.
        g.recordRoll(A, 2);
        assertEquals(B, g.getActivePlayer());

        g.recordRoll(B, 6);
        g.movePiece(B, piece(g, B, 0));
        g.recordRoll(B, 6);
        g.movePiece(B, piece(g, B, 1));
        g.recordRoll(B, 4);

        assertEquals(List.of(piece(g, B, 0), piece(g, B, 1)),
                g.getSelectablePieces());

        g.movePiece(B, piece(g, B, 1));

        assertEquals(0, piece(g, B, 0).getProgress());
        assertEquals(4, piece(g, B, 1).getProgress());
        assertEquals(A, g.getActivePlayer());
    }

    @Test
    void getBoardCellRejectsUnknownPieces() {
        GameState g = game(2);

        assertThrows(IllegalArgumentException.class,
                () -> g.getBoardCell(null));
        assertThrows(IllegalArgumentException.class,
                () -> g.getBoardCell(
                        new Piece(PlayerColor.RED, 0)));
        assertEquals(Optional.empty(),
                g.getBoardCell(piece(g, A, 0)));
    }
}
