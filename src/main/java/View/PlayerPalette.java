package View;

import Model.GameSetup.PlayerColor;

/**
 * The four player colors used consistently by every screen.
 *
 * MVC Component: View
 * Iteration: 1
 */
final class PlayerPalette {

    private PlayerPalette() {
    }

    /**
     * @param color a player color
     * @return the strong accent color as a CSS hex value
     */
    static String hex(PlayerColor color) {
        return switch (color) {
            case RED -> "#E85561";
            case BLUE -> "#4A90E2";
            case GREEN -> "#49B77C";
            case YELLOW -> "#F0C541";
        };
    }

    /**
     * @param color a player color
     * @return a friendly, capitalised color name
     */
    static String displayName(PlayerColor color) {
        return switch (color) {
            case RED -> "Red";
            case BLUE -> "Blue";
            case GREEN -> "Green";
            case YELLOW -> "Yellow";
        };
    }
}
