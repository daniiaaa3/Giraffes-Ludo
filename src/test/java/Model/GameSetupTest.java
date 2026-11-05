
package Model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import Model.GameSetup.PlayerColor;
import Model.GameSetup.PlayerEntry;

/**
 * Unit tests for game setup validation.
 *
 * Verifies valid player configurations and rejects
 * invalid names, colors, and player counts.
 *
 * SRS Requirements: FR-1.2, FR-1.4, FR-1.5
 * Iteration: 1
 */
public class GameSetupTest {

    private final GameSetup setup = new GameSetup();

    /**
     * Verifies that two players with unique names
     * and colors are accepted.
     */
    @Test
    void validTwoPlayers() {
        List<PlayerEntry> players = List.of(
            new PlayerEntry("Dania", PlayerColor.RED),
            new PlayerEntry("Marwa", PlayerColor.BLUE)
        );

        assertEquals(2, setup.validate(players).size());
    }

    /**
     * Verifies that four players are accepted.
     */
    @Test
    void validFourPlayers() {
        List<PlayerEntry> players = List.of(
            new PlayerEntry("Dania", PlayerColor.RED),
            new PlayerEntry("Marwa", PlayerColor.BLUE),
            new PlayerEntry("Nay", PlayerColor.GREEN),
            new PlayerEntry("Yara", PlayerColor.YELLOW)
        );

        assertEquals(4, setup.validate(players).size());
    }

    /**
     * Rejects fewer than two players.
     */
    @Test
    void rejectsOnePlayer() {
        List<PlayerEntry> players = List.of(
            new PlayerEntry("Dania", PlayerColor.RED)
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> setup.validate(players)
        );
    }

    /**
     * Rejects empty player names.
     */
    @Test
    void rejectsEmptyName() {
        List<PlayerEntry> players = List.of(
            new PlayerEntry("", PlayerColor.RED),
            new PlayerEntry("Marwa", PlayerColor.BLUE)
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> setup.validate(players)
        );
    }

    /**
     * Rejects duplicate names regardless of case
     * or extra spaces.
     */
    @Test
    void rejectsDuplicateNames() {
        List<PlayerEntry> players = List.of(
            new PlayerEntry("Dania", PlayerColor.RED),
            new PlayerEntry(" dania ", PlayerColor.BLUE)
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> setup.validate(players)
        );
    }

    /**
     * Rejects two players choosing the same color.
     */
    @Test
    void rejectsDuplicateColors() {
        List<PlayerEntry> players = List.of(
            new PlayerEntry("Dania", PlayerColor.RED),
            new PlayerEntry("Marwa", PlayerColor.RED)
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> setup.validate(players)
        );
    }

    /**
     * Verifies that valid names are trimmed.
     */
    @Test
    void trimsPlayerNames() {
        List<PlayerEntry> players = List.of(
            new PlayerEntry(" Dania ", PlayerColor.RED),
            new PlayerEntry("Marwa", PlayerColor.BLUE)
        );

        List<PlayerEntry> result = setup.validate(players);

        assertEquals("Dania", result.get(0).name());
    }
}
