package Model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import Model.EasyBoard.BoardCell;
import Model.GameSetup.PlayerColor;
import Model.GameSetup.PlayerEntry;
import Model.Piece.State;
import Model.TurnRollRules.Outcome;

/**
 * Integration tests of capture, protected squares, backward movement and
 * effect movement on the Medium and Hard boards, using the real boards
 * and the Boost / UltraBoost dice API.
 *
 * The special squares of these boards are placed at random for every
 * game, so the tests do not assume fixed positions. They read every
 * position from the game itself (getBoardCellAt, the topology) and look
 * for places where two colors' paths meet on a given square.
 *
 * Iteration: 2
 */
public class MediumHardMovementIntegrationTest {

    private static final PlayerColor[] COLORS = PlayerColor.values();

    private static Random firstSeat() {
        return new Random() {
            @Override
            public int nextInt(int bound) {
                return 0;
            }
        };
    }

    /** All four colors play; RED moves first. */
    private static GameState game(GameDifficulty difficulty) {
        List<PlayerEntry> players = new ArrayList<>();
        for (PlayerColor color : COLORS) {
            players.add(new PlayerEntry(color.name(), color));
        }
        return GameState.forDifficulty(players, firstSeat(), difficulty);
    }

    private static PlayerEntry player(GameState g, PlayerColor color) {
        for (PlayerEntry p : g.getPlayers()) {
            if (p.color() == color) {
                return p;
            }
        }
        throw new AssertionError("no player " + color);
    }

    private static Piece piece(GameState g, PlayerColor c, int id) {
        return g.getPieces(c).get(id);
    }

    private static void place(GameState g, Piece p, int progress) {
        p.moveTo(progress, progress > g.getEntrySteps(p.getColor())
                ? State.HOME_PATH : State.TRACK);
    }

    private static void resetAllToCastle(GameState g) {
        for (PlayerColor color : COLORS) {
            for (Piece p : g.getPieces(color)) {
                p.moveTo(Piece.CASTLE_PROGRESS, State.CASTLE);
            }
        }
    }

    private static void assertInCastle(Piece p) {
        assertEquals(State.CASTLE, p.getState());
        assertEquals(Piece.CASTLE_PROGRESS, p.getProgress());
    }

    /** Progress of a color on a square of its shared stretch, or -1. */
    private static int progressOn(GameState g, PlayerColor color,
                                  BoardCell cell, int lowest, int highest) {
        for (int p = lowest; p <= highest; p++) {
            if (g.getBoardCellAt(color, p).orElseThrow().equals(cell)) {
                return p;
            }
        }
        return -1;
    }

    /** Two colors whose paths meet on one square. */
    private record Spot(PlayerColor mover, int moverProgress,
                        PlayerColor victim, int victimProgress,
                        BoardCell cell) { }

    /**
     * For every square in the set, finds a mover and a victim color that
     * both pass over it. The mover needs {@code room} squares before it
     * (so it can be placed behind the square) and {@code after} squares
     * after it on its shared stretch.
     */
    private static List<Spot> spots(GameState g, Set<BoardCell> cells,
                                    int room, int after,
                                    PlayerColor onlyMover) {
        List<Spot> found = new ArrayList<>();
        for (BoardCell cell : cells) {
            Spot spot = null;
            for (PlayerColor a : COLORS) {
                if (onlyMover != null && a != onlyMover) {
                    continue;
                }
                int pa = progressOn(g, a, cell, room,
                        g.getEntrySteps(a) - 1 - after);
                if (pa < 0) {
                    continue;
                }
                for (PlayerColor b : COLORS) {
                    if (b == a) {
                        continue;
                    }
                    int pb = progressOn(g, b, cell, 1,
                            g.getEntrySteps(b) - 1);
                    if (pb >= 0) {
                        spot = new Spot(a, pa, b, pb, cell);
                        break;
                    }
                }
                if (spot != null) {
                    break;
                }
            }
            if (spot != null) {
                found.add(spot);
            }
        }
        return found;
    }

    /** Unprotected squares of RED's shared stretch. */
    private static Set<BoardCell> unprotectedSquares(GameState g) {
        Set<BoardCell> cells = new LinkedHashSet<>();
        for (int p = 3; p < g.getEntrySteps(PlayerColor.RED) - 4; p++) {
            BoardCell cell =
                    g.getBoardCellAt(PlayerColor.RED, p).orElseThrow();
            if (!g.isProtectedSquare(cell)) {
                cells.add(cell);
            }
        }
        return cells;
    }

    private static <T> List<T> first(List<T> all, int count) {
        return all.subList(0, Math.min(count, all.size()));
    }

    // ---------- capture (FR-5.4) ----------

    @ParameterizedTest
    @EnumSource(value = GameDifficulty.class,
            names = {"MEDIUM", "HARD"})
    void aMoveWithTheBoostDiceCapturesOnAnUnprotectedSquare(
            GameDifficulty difficulty) {
        GameState g = game(difficulty);
        List<Spot> candidates = spots(g, unprotectedSquares(g),
                3, 4, PlayerColor.RED);
        assertFalse(candidates.isEmpty(), "no meeting square found");

        Spot s = candidates.get(0);
        PlayerEntry red = player(g, PlayerColor.RED);
        Piece mover = piece(g, PlayerColor.RED, 0);
        Piece victim = piece(g, s.victim(), 0);
        place(g, mover, s.moverProgress() - 2);
        place(g, victim, s.victimProgress());

        // distance = steps x (N - 1) = 2 x 1 = 2 on both boards
        assertEquals(Outcome.MOVE_REQUIRED,
                g.recordAdvancedRoll(red, 2, 2));
        assertEquals(Outcome.TURN_PASSED, g.movePiece(red, mover));

        MoveResult result = g.getLastMoveResult().orElseThrow();
        assertEquals(s.moverProgress(), mover.getProgress());
        assertInCastle(victim);
        assertEquals(Optional.of(victim), result.captured());
        assertEquals(s.moverProgress() - 2, result.fromProgress());
        assertEquals(s.moverProgress(), result.toProgress());
        assertNotEquals(red, g.getActivePlayer(),
                "the turn passed after the dice move");
    }

    @ParameterizedTest
    @EnumSource(value = GameDifficulty.class,
            names = {"MEDIUM", "HARD"})
    void aQuestionSquareIsNotProtectedSoItCapturesOnLanding(
            GameDifficulty difficulty) {
        GameState g = game(difficulty);
        Set<BoardCell> questions =
                g.getBoardTopology().getQuestionSquares().keySet();
        assertFalse(questions.isEmpty());

        List<Spot> found = spots(g, questions, 3, 4, null);
        assertFalse(found.isEmpty(), "no question square on two paths");

        for (Spot s : found) {
            resetAllToCastle(g);
            Piece mover = piece(g, s.mover(), 0);
            Piece victim = piece(g, s.victim(), 0);
            place(g, mover, s.moverProgress() - 2);
            place(g, victim, s.victimProgress());

            assertFalse(g.isProtectedSquare(s.cell()));
            MoveResult result = g.moveForward(mover, 2);

            // capture happens first; the question itself comes later
            assertEquals(Optional.of(victim), result.captured());
            assertInCastle(victim);
        }
    }

    // ---------- protected squares (FR-3.1, FR-3.2, FR-3.4, FR-3.6) ----------

    @ParameterizedTest
    @EnumSource(value = GameDifficulty.class,
            names = {"MEDIUM", "HARD"})
    void safeSquaresProtectAgainstCapture(GameDifficulty difficulty) {
        GameState g = game(difficulty);
        Set<BoardCell> safe = g.getBoardTopology().getSafeSquares();

        List<Spot> found = spots(g, safe, 3, 4, null);
        assertFalse(found.isEmpty(), "no safe square on two paths");

        for (Spot s : found) {
            resetAllToCastle(g);
            Piece mover = piece(g, s.mover(), 0);
            Piece victim = piece(g, s.victim(), 0);
            place(g, mover, s.moverProgress() - 2);
            place(g, victim, s.victimProgress());

            assertTrue(g.isProtectedSquare(s.cell()));
            MoveResult result = g.moveForward(mover, 2);

            assertFalse(result.isCapture(), "safe square " + s.cell());
            assertEquals(State.TRACK, victim.getState());
            assertEquals(s.victimProgress(), victim.getProgress());
            assertEquals(g.getBoardCell(mover), g.getBoardCell(victim),
                    "both pieces share the safe square");
        }
    }

    @ParameterizedTest
    @EnumSource(value = GameDifficulty.class,
            names = {"MEDIUM", "HARD"})
    void surpriseSquaresProtectAgainstCapture(GameDifficulty difficulty) {
        GameState g = game(difficulty);
        Set<BoardCell> surprise =
                g.getBoardTopology().getSurpriseSquares();
        assertFalse(surprise.isEmpty());

        List<Spot> found = spots(g, surprise, 3, 4, null);
        assertFalse(found.isEmpty(), "no surprise square on two paths");

        for (Spot s : found) {
            resetAllToCastle(g);
            Piece mover = piece(g, s.mover(), 0);
            Piece victim = piece(g, s.victim(), 0);
            place(g, mover, s.moverProgress() - 2);
            place(g, victim, s.victimProgress());

            assertTrue(g.isProtectedSquare(s.cell()));
            MoveResult result = g.moveForward(mover, 2);

            assertFalse(result.isCapture(), "surprise square " + s.cell());
            assertEquals(State.TRACK, victim.getState());
        }
    }

    @ParameterizedTest
    @EnumSource(value = GameDifficulty.class,
            names = {"MEDIUM", "HARD"})
    void startSquaresAreProtectedAndTheFinalSquareToo(
            GameDifficulty difficulty) {
        GameState g = game(difficulty);
        int tested = 0;

        for (PlayerColor owner : COLORS) {
            BoardCell start = g.getBoardCellAt(owner, 0).orElseThrow();
            assertTrue(g.isProtectedSquare(start), owner + " start");
            assertTrue(g.isProtectedSquare(g.getBoardCellAt(
                    owner, g.getFinishSteps(owner)).orElseThrow()),
                    owner + " final square");

            for (PlayerColor mover : COLORS) {
                if (mover == owner) {
                    continue;
                }
                int p = progressOn(g, mover, start, 3,
                        g.getEntrySteps(mover) - 4);
                if (p < 0) {
                    continue;
                }
                resetAllToCastle(g);
                Piece victim = piece(g, owner, 0);
                Piece runner = piece(g, mover, 0);
                place(g, victim, 0);               // on its start square
                place(g, runner, p - 2);

                MoveResult result = g.moveForward(runner, 2);

                assertFalse(result.isCapture(), owner + " start");
                assertEquals(State.TRACK, victim.getState());
                assertEquals(0, victim.getProgress());
                tested++;
            }
        }

        assertTrue(tested > 0, "no start square was on another path");
    }

    // ---------- backward movement (FR-5.9) ----------

    @ParameterizedTest
    @EnumSource(value = GameDifficulty.class,
            names = {"MEDIUM", "HARD"})
    void aBackwardLandingCapturesOnAnUnprotectedSquare(
            GameDifficulty difficulty) {
        GameState g = game(difficulty);
        List<Spot> found = first(spots(g, unprotectedSquares(g), 1, 4,
                null), 5);
        assertFalse(found.isEmpty());

        for (Spot s : found) {
            resetAllToCastle(g);
            Piece mover = piece(g, s.mover(), 0);
            Piece victim = piece(g, s.victim(), 0);
            place(g, mover, s.moverProgress() + 3);
            place(g, victim, s.victimProgress());

            MoveResult result = g.moveBackward(mover, 3);

            assertEquals(s.moverProgress(), mover.getProgress());
            assertEquals(Optional.of(victim), result.captured());
            assertInCastle(victim);
            assertEquals(s.moverProgress() + 3, result.fromProgress());
            assertEquals(s.moverProgress(), result.toProgress());
        }
    }

    @ParameterizedTest
    @EnumSource(value = GameDifficulty.class,
            names = {"MEDIUM", "HARD"})
    void movingBackPastTheStartStopsOnTheStartSquare(
            GameDifficulty difficulty) {
        GameState g = game(difficulty);

        for (PlayerColor color : COLORS) {
            Piece p = piece(g, color, 0);
            place(g, p, 2);

            MoveResult result = g.moveBackward(p, 9);

            assertEquals(0, p.getProgress(), color + " clamps at start");
            assertEquals(State.TRACK, p.getState());
            assertEquals(g.getBoardCellAt(color, 0), g.getBoardCell(p));
            assertEquals(2, result.fromProgress());
            assertEquals(0, result.toProgress());
        }
    }

    @ParameterizedTest
    @EnumSource(value = GameDifficulty.class,
            names = {"MEDIUM", "HARD"})
    void clampingOntoAStartSquareDoesNotCaptureAnOpponentThere(
            GameDifficulty difficulty) {
        GameState g = game(difficulty);
        int tested = 0;

        for (PlayerColor owner : COLORS) {
            BoardCell start = g.getBoardCellAt(owner, 0).orElseThrow();
            for (PlayerColor guest : COLORS) {
                if (guest == owner) {
                    continue;
                }
                int q = progressOn(g, guest, start, 1,
                        g.getEntrySteps(guest) - 1);
                if (q < 0) {
                    continue;
                }
                resetAllToCastle(g);
                Piece home = piece(g, owner, 0);
                Piece visitor = piece(g, guest, 0);
                place(g, home, 2);
                place(g, visitor, q);               // on owner's start

                MoveResult result = g.moveBackward(home, 7);

                assertEquals(0, home.getProgress());
                assertFalse(result.isCapture());
                assertEquals(State.TRACK, visitor.getState());
                assertEquals(q, visitor.getProgress());
                tested++;
            }
        }

        assertTrue(tested > 0, "no start square was on another path");
    }

    @ParameterizedTest
    @EnumSource(value = GameDifficulty.class,
            names = {"MEDIUM", "HARD"})
    void aHomePathPieceWalksBackAlongItsPath(GameDifficulty difficulty) {
        // ASSUMPTION (not defined by the SRS): the home path continues
        // the piece's path, so going back follows it in reverse.
        GameState g = game(difficulty);
        PlayerColor color = PlayerColor.GREEN;
        int entry = g.getEntrySteps(color);
        Piece p = piece(g, color, 0);
        place(g, p, entry + 3);

        g.moveBackward(p, 2);
        assertEquals(entry + 1, p.getProgress());
        assertEquals(State.HOME_PATH, p.getState());

        g.moveBackward(p, 2);                       // back onto the track
        assertEquals(entry - 1, p.getProgress());
        assertEquals(State.TRACK, p.getState());
        assertEquals(g.getBoardCellAt(color, entry - 1), g.getBoardCell(p));
    }

    // ---------- zero distance ----------

    @ParameterizedTest
    @EnumSource(value = GameDifficulty.class,
            names = {"MEDIUM", "HARD"})
    void zeroDistanceMovesNeverCapture(GameDifficulty difficulty) {
        GameState g = game(difficulty);
        List<Spot> found = first(spots(g, unprotectedSquares(g), 1, 4,
                null), 3);
        assertFalse(found.isEmpty());

        for (Spot s : found) {
            resetAllToCastle(g);
            Piece mover = piece(g, s.mover(), 0);
            Piece victim = piece(g, s.victim(), 0);
            place(g, mover, s.moverProgress());
            place(g, victim, s.victimProgress());    // same square

            MoveResult forward = g.moveForward(mover, 0);
            MoveResult backward = g.moveBackward(mover, 0);
            MoveResult signed = g.moveByEffect(mover, 0);

            for (MoveResult r : List.of(forward, backward, signed)) {
                assertFalse(r.isCapture());
                assertEquals(s.moverProgress(), r.fromProgress());
                assertEquals(s.moverProgress(), r.toProgress());
            }
            assertEquals(State.TRACK, victim.getState());
            assertEquals(s.victimProgress(), victim.getProgress());
        }
    }

    @ParameterizedTest
    @EnumSource(value = GameDifficulty.class,
            names = {"MEDIUM", "HARD"})
    void aBoostDieOfOneGivesZeroDistanceAndCapturesNothing(
            GameDifficulty difficulty) {
        GameState g = game(difficulty);
        List<Spot> found = spots(g, unprotectedSquares(g), 1, 4,
                PlayerColor.RED);
        assertFalse(found.isEmpty());
        Spot s = found.get(0);
        PlayerEntry red = player(g, PlayerColor.RED);
        Piece mover = piece(g, PlayerColor.RED, 0);
        Piece victim = piece(g, s.victim(), 0);
        place(g, mover, s.moverProgress());
        place(g, victim, s.victimProgress());       // same square

        // Boost / UltraBoost 1 means 0 steps; a 3 cannot release a piece
        Outcome outcome = g.recordAdvancedRoll(red, 3, 1);

        assertEquals(Outcome.TURN_PASSED, outcome);
        assertEquals(s.moverProgress(), mover.getProgress());
        assertEquals(State.TRACK, victim.getState());
        assertEquals(s.victimProgress(), victim.getProgress());
        assertTrue(g.getLastMoveResult().isEmpty(),
                "no move was made at all");
    }

    // ---------- effect forward movement on the advanced boards ----------

    @ParameterizedTest
    @EnumSource(value = GameDifficulty.class,
            names = {"MEDIUM", "HARD"})
    void effectMovementDoesNotTouchThePendingAdvancedRoll(
            GameDifficulty difficulty) {
        GameState g = game(difficulty);
        PlayerEntry red = player(g, PlayerColor.RED);
        Piece mover = piece(g, PlayerColor.RED, 0);
        Piece other = piece(g, PlayerColor.GREEN, 0);
        place(g, mover, 10);
        place(g, other, 10);

        assertEquals(Outcome.MOVE_REQUIRED,
                g.recordAdvancedRoll(red, 2, 2));

        g.moveForward(other, 3);                    // question reward

        assertEquals(13, other.getProgress());
        assertEquals(red, g.getActivePlayer());
        assertEquals(2, g.getPendingRoll());
        assertEquals(2, g.getPendingSpecialDie());
        assertTrue(g.isAwaitingMove());

        assertEquals(Outcome.TURN_PASSED, g.movePiece(red, mover));
        assertEquals(12, mover.getProgress());
    }

    @ParameterizedTest
    @EnumSource(value = GameDifficulty.class,
            names = {"MEDIUM", "HARD"})
    void effectMovementFollowsTheHomePathAndTheExactFinishRule(
            GameDifficulty difficulty) {
        GameState g = game(difficulty);
        PlayerColor color = PlayerColor.BLUE;
        int entry = g.getEntrySteps(color);
        int finish = g.getFinishSteps(color);
        Piece p = piece(g, color, 0);

        place(g, p, entry - 1);
        g.moveForward(p, 3);            // crosses into the home path
        assertEquals(entry + 2, p.getProgress());
        assertEquals(State.HOME_PATH, p.getState());

        place(g, p, finish - 2);
        assertFalse(g.canMoveForward(p, 3));
        assertThrows(IllegalArgumentException.class,
                () -> g.moveForward(p, 3));
        assertEquals(finish - 2, p.getProgress());

        MoveResult result = g.moveForward(p, 2);
        assertEquals(State.FINISHED, p.getState());
        assertEquals(finish, result.toProgress());
        assertEquals(finish - 2, result.fromProgress());
        assertFalse(result.isCapture());
    }

    // ---------- result reporting ----------

    @ParameterizedTest
    @EnumSource(value = GameDifficulty.class,
            names = {"MEDIUM", "HARD"})
    void everyKindOfMoveReportsItsResult(GameDifficulty difficulty) {
        GameState g = game(difficulty);
        Piece p = piece(g, PlayerColor.YELLOW, 0);
        place(g, p, 20);

        MoveResult forward = g.moveForward(p, 3);
        assertSame(p, forward.piece());
        assertEquals(20, forward.fromProgress());
        assertEquals(23, forward.toProgress());
        assertEquals(Optional.of(forward), g.getLastMoveResult());

        MoveResult backward = g.moveBackward(p, 5);
        assertEquals(23, backward.fromProgress());
        assertEquals(18, backward.toProgress());
        assertEquals(Optional.of(backward), g.getLastMoveResult());

        MoveResult signed = g.moveByEffect(p, -30);
        assertEquals(18, signed.fromProgress());
        assertEquals(0, signed.toProgress());
        assertEquals(Optional.of(signed), g.getLastMoveResult());
    }
}
