
package Model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

import Model.GameSetup.PlayerColor;
import Model.GameSetup.PlayerEntry;

/**
 * Unit tests for turn management.
 *
 * SRS Requirements: FR-1.6, FR-6.1, FR-6.2
 * Iteration: 1
 */
public class TurnManagerTest {

    private static final PlayerEntry DANIA =
            new PlayerEntry("Dania", PlayerColor.RED);
    private static final PlayerEntry MARWA =
            new PlayerEntry("Marwa", PlayerColor.BLUE);
    private static final PlayerEntry NAY =
            new PlayerEntry("Nay", PlayerColor.GREEN);
    private static final PlayerEntry YARA =
            new PlayerEntry("Yara", PlayerColor.YELLOW);

    private static final List<PlayerEntry> ALL =
            List.of(DANIA, MARWA, NAY, YARA);

    /** Random that always returns a fixed index. */
    private static Random fixed(int index) {
        return new Random() {
            @Override
            public int nextInt(int bound) {
                return index;
            }
        };
    }

    // ---------- FR-1.6: random first player ----------

    @Test
    void firstPlayerComesFromSuppliedRandom() {
        for (int i = 0; i < 4; i++) {
            TurnManager tm = new TurnManager(ALL, fixed(i));
            assertEquals(ALL.get(i), tm.getActivePlayer());
            assertEquals(i, tm.getActiveIndex());
        }
    }

    @Test
    void sameSeedGivesSameFirstPlayer() {
        TurnManager a = new TurnManager(ALL, new Random(42));
        TurnManager b = new TurnManager(ALL, new Random(42));
        assertEquals(a.getActivePlayer(), b.getActivePlayer());
    }

    @Test
    void everyPlayerCanBeSelectedFirst() {
        Set<PlayerEntry> seen = new HashSet<>();
        Random random = new Random(1);

        for (int i = 0; i < 200; i++) {
            seen.add(new TurnManager(ALL, random)
                    .getActivePlayer());
        }

        assertEquals(4, seen.size());
    }

    // ---------- FR-6.1: cyclic order, 2-4 players ----------

    @Test
    void twoPlayersAlternate() {
        List<PlayerEntry> two = List.of(DANIA, MARWA);
        TurnManager tm = new TurnManager(two, fixed(0));

        assertEquals(MARWA, tm.endTurn(DANIA));
        assertEquals(DANIA, tm.endTurn(MARWA));
        assertEquals(MARWA, tm.endTurn(DANIA));
    }

    @Test
    void threePlayersCycle() {
        List<PlayerEntry> three = List.of(DANIA, MARWA, NAY);
        TurnManager tm = new TurnManager(three, fixed(1));

        assertEquals(NAY, tm.endTurn(MARWA));
        assertEquals(DANIA, tm.endTurn(NAY));
        assertEquals(MARWA, tm.endTurn(DANIA));
    }

    @Test
    void fourPlayersCycleFromAnyStart() {
        for (int start = 0; start < 4; start++) {
            TurnManager tm = new TurnManager(ALL, fixed(start));
            List<PlayerEntry> order = new ArrayList<>();

            for (int i = 0; i < 8; i++) {
                PlayerEntry current = tm.getActivePlayer();
                order.add(current);
                tm.endTurn(current);
            }

            for (int i = 0; i < 8; i++) {
                assertEquals(ALL.get((start + i) % 4),
                        order.get(i));
            }
        }
    }

    // ---------- FR-6.2: active player only ----------

    @Test
    void onlyActivePlayerIsActive() {
        TurnManager tm = new TurnManager(ALL, fixed(2));

        assertTrue(tm.isActivePlayer(NAY));
        assertFalse(tm.isActivePlayer(DANIA));
        assertFalse(tm.isActivePlayer(MARWA));
        assertFalse(tm.isActivePlayer(YARA));
        assertFalse(tm.isActivePlayer(null));
    }

    @Test
    void outOfTurnEndTurnIsRejectedAndStateUnchanged() {
        TurnManager tm = new TurnManager(ALL, fixed(0));

        assertThrows(IllegalStateException.class,
                () -> tm.endTurn(MARWA));

        assertEquals(DANIA, tm.getActivePlayer());
        assertEquals(0, tm.getActiveIndex());
    }

    @Test
    void outOfTurnGuardThrows() {
        TurnManager tm = new TurnManager(ALL, fixed(0));

        assertThrows(IllegalStateException.class,
                () -> tm.requireActivePlayer(YARA));
        assertDoesNotThrow(
                () -> tm.requireActivePlayer(DANIA));
    }

    @Test
    void nullPlayerIsRejectedAndStateUnchanged() {
        TurnManager tm = new TurnManager(ALL, fixed(1));

        assertThrows(IllegalArgumentException.class,
                () -> tm.endTurn(null));
        assertThrows(IllegalArgumentException.class,
                () -> tm.requireActivePlayer(null));

        assertEquals(MARWA, tm.getActivePlayer());
    }

    @Test
    void unknownPlayerIsRejectedAndStateUnchanged() {
        TurnManager tm = new TurnManager(
                List.of(DANIA, MARWA), fixed(0));
        PlayerEntry stranger =
                new PlayerEntry("Stranger", PlayerColor.GREEN);

        assertThrows(IllegalArgumentException.class,
                () -> tm.endTurn(stranger));

        assertEquals(DANIA, tm.getActivePlayer());
    }

    @Test
    void repeatedOutOfTurnAttemptsNeverAdvance() {
        TurnManager tm = new TurnManager(ALL, fixed(3));

        for (int i = 0; i < 5; i++) {
            assertThrows(IllegalStateException.class,
                    () -> tm.endTurn(DANIA));
        }

        assertEquals(YARA, tm.getActivePlayer());
    }

    // ---------- construction validation ----------

    @Test
    void rejectsInvalidPlayerLists() {
        Random r = new Random(1);

        assertThrows(IllegalArgumentException.class,
                () -> new TurnManager(null, r));
        assertThrows(IllegalArgumentException.class,
                () -> new TurnManager(List.of(), r));
        assertThrows(IllegalArgumentException.class,
                () -> new TurnManager(List.of(DANIA), r));

        List<PlayerEntry> five = new ArrayList<>(ALL);
        five.add(new PlayerEntry("Extra", PlayerColor.RED));
        assertThrows(IllegalArgumentException.class,
                () -> new TurnManager(five, r));

        List<PlayerEntry> withNull = new ArrayList<>();
        withNull.add(DANIA);
        withNull.add(null);
        assertThrows(IllegalArgumentException.class,
                () -> new TurnManager(withNull, r));
    }

    @Test
    void rejectsDuplicatePlayerEntries() {
        Random r = new Random(1);

        assertThrows(IllegalArgumentException.class,
                () -> new TurnManager(List.of(DANIA, DANIA), r));
        assertThrows(IllegalArgumentException.class,
                () -> new TurnManager(
                        List.of(DANIA, MARWA, NAY, MARWA), r));
        assertThrows(IllegalArgumentException.class,
                () -> new TurnManager(
                        List.of(DANIA, MARWA,
                                new PlayerEntry("Marwa",
                                        PlayerColor.BLUE)), r));
    }

    @Test
    void acceptsFourDistinctPlayers() {
        assertDoesNotThrow(() -> new TurnManager(ALL, fixed(0)));
    }

    @Test
    void rejectsNullRandom() {
        assertThrows(IllegalArgumentException.class,
                () -> new TurnManager(ALL, null));
    }

    @Test
    void laterChangesToSourceListDoNotAffectManager() {
        List<PlayerEntry> source =
                new ArrayList<>(List.of(DANIA, MARWA));
        TurnManager tm = new TurnManager(source, fixed(0));

        source.clear();

        assertEquals(2, tm.getPlayers().size());
        assertEquals(DANIA, tm.getActivePlayer());
    }

    @Test
    void worksWithValidatedSetupOutput() {
        List<PlayerEntry> validated = new GameSetup().validate(
                List.of(new PlayerEntry(" Dania ", PlayerColor.RED),
                        new PlayerEntry("Marwa", PlayerColor.BLUE)));

        TurnManager tm = new TurnManager(validated, fixed(0));

        assertEquals("Dania", tm.getActivePlayer().name());
    }
}
