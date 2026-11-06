
package Model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import Model.GameSetup.PlayerColor;

/**
 * Represents the Easy-level Ludo board.
 * FR-2.1: 52 shared squares and 6 home-path squares
 * for each player color.
 */
public class EasyBoard {

    public static final int TRACK_SIZE = 52;
    public static final int HOME_PATH_SIZE = 6;
    

/**
 * Coordinates of the 52 shared-track squares.
 * The order starts at RED's starting square.
 *
 * Each BoardCell stores a row and column
 * on the 15x15 Easy board.
 */
private static final List<BoardCell> TRACK_CELLS = List.of(

    // RED start -> GREEN start
    new BoardCell(13, 6),
    new BoardCell(12, 6),
    new BoardCell(11, 6),
    new BoardCell(10, 6),
    new BoardCell(9, 6),
    new BoardCell(8, 5),
    new BoardCell(8, 4),
    new BoardCell(8, 3),
    new BoardCell(8, 2),
    new BoardCell(8, 1),
    new BoardCell(8, 0),
    new BoardCell(7, 0),
    new BoardCell(6, 0),

    // GREEN start -> YELLOW start
    new BoardCell(6, 1),
    new BoardCell(6, 2),
    new BoardCell(6, 3),
    new BoardCell(6, 4),
    new BoardCell(6, 5),
    new BoardCell(5, 6),
    new BoardCell(4, 6),
    new BoardCell(3, 6),
    new BoardCell(2, 6),
    new BoardCell(1, 6),
    new BoardCell(0, 6),
    new BoardCell(0, 7),
    new BoardCell(0, 8),

    // YELLOW start -> BLUE start
    new BoardCell(1, 8),
    new BoardCell(2, 8),
    new BoardCell(3, 8),
    new BoardCell(4, 8),
    new BoardCell(5, 8),
    new BoardCell(6, 9),
    new BoardCell(6, 10),
    new BoardCell(6, 11),
    new BoardCell(6, 12),
    new BoardCell(6, 13),
    new BoardCell(6, 14),
    new BoardCell(7, 14),
    new BoardCell(8, 14),

    // BLUE start -> RED start
    new BoardCell(8, 13),
    new BoardCell(8, 12),
    new BoardCell(8, 11),
    new BoardCell(8, 10),
    new BoardCell(8, 9),
    new BoardCell(9, 8),
    new BoardCell(10, 8),
    new BoardCell(11, 8),
    new BoardCell(12, 8),
    new BoardCell(13, 8),
    new BoardCell(14, 8),
    new BoardCell(14, 7),
    new BoardCell(14, 6)
);


    private final List<Integer> sharedTrack;
    private final Map<PlayerColor, List<Integer>> homePaths;

    public EasyBoard() {
        sharedTrack = createPath(TRACK_SIZE);

        Map<PlayerColor, List<Integer>> paths =
                new EnumMap<>(PlayerColor.class);

        for (PlayerColor color : PlayerColor.values()) {
            paths.put(color, createPath(HOME_PATH_SIZE));
        }

        homePaths = Collections.unmodifiableMap(paths);
    }

    private List<Integer> createPath(int size) {
        List<Integer> path = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            path.add(i);
        }

        return List.copyOf(path);
    }

    public List<Integer> getSharedTrack() {
        return sharedTrack;
    }

    public List<Integer> getHomePath(PlayerColor color) {
        if (color == null) {
            throw new IllegalArgumentException(
                    "Player color cannot be null.");
        }

        return homePaths.get(color);
    }

/**
 * Returns the starting square for each player.
 * Indices follow the shared track from 0 to 51,
 * starting at RED and moving clockwise.
 */
public int getStartSquare(PlayerColor color) {
    if (color == null) {
        throw new IllegalArgumentException(
                "Player color cannot be null.");
    }

    return switch (color) {
        case RED -> 0;
        case GREEN -> 13;
        case YELLOW -> 26;
        case BLUE -> 39;
    };
}

/**
 * Checks whether a shared-track square
 * is a protected starting square.
 */
public boolean isProtectedStartSquare(int index) {
    if (index < 0 || index >= TRACK_SIZE) {
        return false;
    }

    for (PlayerColor color : PlayerColor.values()) {
        if (getStartSquare(color) == index) {
            return true;
        }
    }

    return false;
}

/**
 * Represents a cell position on the 15x15 Easy board.
 */
public record BoardCell(int row, int col) {
}

/**
 * Returns the home-entry arrow position
 * for the given player color.
 *
 * SRS: FR-2.5
 */
public BoardCell getArrowSquare(PlayerColor color) {

    if (color == null) {
        throw new IllegalArgumentException(
                "Player color cannot be null.");
    }

    return switch (color) {
        case RED -> new BoardCell(14, 7);
        case GREEN -> new BoardCell(7, 0);
        case YELLOW -> new BoardCell(0, 7);
        case BLUE -> new BoardCell(7, 14);
    };
}

/**
 * Checks whether a board position is
 * one of the four arrow squares.
 */
public boolean isArrowSquare(int row, int col) {

    if (row < 0 || row >= 15 ||
            col < 0 || col >= 15) {
        return false;
    }

    for (PlayerColor color : PlayerColor.values()) {
        if (getArrowSquare(color).equals(
                new BoardCell(row, col))) {
            return true;
        }
    }

    return false;
}

/**
 * Checks whether this is the arrow
 * belonging to a specific player.
 */
public boolean isHomeEntryArrow(
        PlayerColor color, int row, int col) {

    return getArrowSquare(color).equals(
            new BoardCell(row, col));
}

/**
 * Returns the ordered coordinates of
 * all 52 shared-track squares.
 */
public List<BoardCell> getSharedTrackCells() {
    return TRACK_CELLS;
}

/**
 * Returns the board coordinate for
 * a shared-track index (0-51).
 */
public BoardCell getSharedTrackCell(int index) {
    if (index < 0 || index >= TRACK_SIZE) {
        throw new IllegalArgumentException(
                "Track index must be between 0 and 51.");
    }

    return TRACK_CELLS.get(index);
}

/**
 * Coordinates of the six home-path positions
 * for each player, including the final center.
 *
 * SRS: FR-2.1
 */
private static final Map<PlayerColor, List<BoardCell>>
        HOME_PATH_CELLS = createHomePathCells();

private static Map<PlayerColor, List<BoardCell>>
        createHomePathCells() {

    Map<PlayerColor, List<BoardCell>> paths =
            new EnumMap<>(PlayerColor.class);

    paths.put(PlayerColor.RED, List.of(
            new BoardCell(13, 7),
            new BoardCell(12, 7),
            new BoardCell(11, 7),
            new BoardCell(10, 7),
            new BoardCell(9, 7),
            new BoardCell(8, 7)
    ));

    paths.put(PlayerColor.GREEN, List.of(
            new BoardCell(7, 1),
            new BoardCell(7, 2),
            new BoardCell(7, 3),
            new BoardCell(7, 4),
            new BoardCell(7, 5),
            new BoardCell(7, 6)
    ));

    paths.put(PlayerColor.YELLOW, List.of(
            new BoardCell(1, 7),
            new BoardCell(2, 7),
            new BoardCell(3, 7),
            new BoardCell(4, 7),
            new BoardCell(5, 7),
            new BoardCell(6, 7)
    ));

    paths.put(PlayerColor.BLUE, List.of(
            new BoardCell(7, 13),
            new BoardCell(7, 12),
            new BoardCell(7, 11),
            new BoardCell(7, 10),
            new BoardCell(7, 9),
            new BoardCell(7, 8)
    ));

    return Collections.unmodifiableMap(paths);
}

/**
 * Returns the ordered home-path coordinates.
 */
public List<BoardCell> getHomePathCells(
        PlayerColor color) {

    if (color == null) {
        throw new IllegalArgumentException(
                "Player color cannot be null.");
    }

    return HOME_PATH_CELLS.get(color);
}


}
