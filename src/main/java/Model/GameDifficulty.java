
package Model;

/**
 * Represents the available Ludo difficulty levels.
 *
 * MVC Component: Model
 * Iteration: 2
 */
public enum GameDifficulty {

    EASY(15),
    MEDIUM(17),
    HARD(19);

    private final int boardSize;

    GameDifficulty(int boardSize) {
        this.boardSize = boardSize;
    }

    public int getBoardSize() {
        return boardSize;
    }

    /**
     * Converts the Game Setup selection into a
     * strongly typed difficulty level.
     */
    public static GameDifficulty fromSelection(String value) {

        if (value == null) {
            throw new IllegalArgumentException(
                    "Difficulty cannot be null.");
        }

        for (GameDifficulty difficulty : values()) {
            if (difficulty.name().equalsIgnoreCase(value.trim())) {
                return difficulty;
            }
        }

        throw new IllegalArgumentException(
                "Unknown difficulty: " + value);
    }
}
