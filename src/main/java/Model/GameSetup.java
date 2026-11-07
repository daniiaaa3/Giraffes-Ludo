
package Model;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Validates player configurations before starting a game.
 *
 * Ensures that the game contains 2-4 players,
 * each with a non-empty, unique name and a unique color.
 *
 * MVC Component: Model
 * SRS Requirements: FR-1.2, FR-1.4, FR-1.5
 * Iteration: 1
 */
public class GameSetup {

    public static final int MIN_PLAYERS = 2;
    public static final int MAX_PLAYERS = 4;

    /**
     * Available colors for Ludo players.
     */
    public enum PlayerColor {
        RED, BLUE, GREEN, YELLOW
    }

    /**
     * Represents a player's setup information.
     *
     * @param name the player's name
     * @param color the selected piece color
     */
    public record PlayerEntry(
            String name,
            PlayerColor color) {
    }

    /**
     * Validates the player list and returns a clean copy.
     *
     * @param players players entered in the setup screen
     * @return validated players with trimmed names
     * @throws IllegalArgumentException if setup is invalid
     */
    public List<PlayerEntry> validate(
            List<PlayerEntry> players) {

        // Check the number of players.
        if (players == null ||
                players.size() < MIN_PLAYERS ||
                players.size() > MAX_PLAYERS) {
            throw new IllegalArgumentException(
                    "Choose between 2 and 4 players");
        }

        Set<String> names = new HashSet<>();
        Set<PlayerColor> colors =
                EnumSet.noneOf(PlayerColor.class);

        List<PlayerEntry> result = new ArrayList<>();

        for (PlayerEntry player : players) {

            // Check that the player entry exists.
            if (player == null) {
                throw new IllegalArgumentException(
                        "Some player details are missing");
            }

            // Reject empty player names.
            String name = player.name();

            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException(
                        "Every player needs a name");
            }

            name = name.trim();

            // Names must be unique, ignoring case.
            if (!names.add(
                    name.toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException(
                        "Each player needs a different name");
            }

            // Every player must choose a color, and colors differ.
            if (player.color() == null) {
                throw new IllegalArgumentException(
                        "Every player needs to choose a color");
            }

            if (!colors.add(player.color())) {
                throw new IllegalArgumentException(
                        "Each player needs a different color");
            }

            result.add(
                    new PlayerEntry(name, player.color()));
        }

        return List.copyOf(result);
    }
}
