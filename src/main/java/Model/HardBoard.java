
package Model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;

import Model.EasyBoard.BoardCell;
import Model.GameSetup.PlayerColor;

/**
 * Hard-level Ludo board:
 * 72 outer, 64 middle and 52 inner shared-track squares,
 * plus each player's six home squares.
 *
 * Keeps EasyBoard's original color positions.
 *
 * SRS: FR-2.3 through FR-2.6.
 */
public final class HardBoard {

    public static final int BOARD_SIZE = 19;
    public static final int OUTER_TRACK_SIZE = 72;
    public static final int MIDDLE_TRACK_SIZE = 64;
    public static final int INNER_TRACK_SIZE =
            EasyBoard.TRACK_SIZE;

    public static final int SAFE_SQUARE_COUNT = 12;
    public static final int ARROW_SQUARE_COUNT = 12;
    public static final int QUESTION_SQUARE_COUNT = 9;
    public static final int SURPRISE_SQUARE_COUNT = 3;

    private static final int OUTER_ARROW_STEP = 65;
    private static final int MIDDLE_ARROW_STEP = 57;

    public enum QuestionLevel {
        EASY, MEDIUM, HARD
    }

    private final EasyBoard easyBoard;

    private final List<BoardCell> outerTrack;
    private final List<BoardCell> middleTrack;
    private final List<BoardCell> innerTrack;

    private final Map<PlayerColor, List<BoardCell>> playerPaths;

    private final Set<BoardCell> safeSquares;
    private final Map<BoardCell, QuestionLevel> questionSquares;
    private final Set<BoardCell> surpriseSquares;

 


/**
 * Creates a Hard board with random
 * special-square placement.
 */
public HardBoard() {
    this(new RandomSpecialSquarePlacementStrategy());
}

/**
 * Allows reproducible random placement
 * for JUnit tests.
 */
public HardBoard(Random random) {
    this(new RandomSpecialSquarePlacementStrategy(random));
}

/**
 * Strategy Pattern:
 * Delegates special-square placement
 * to the selected strategy.
 */
public HardBoard(SpecialSquarePlacementStrategy strategy) {

    Objects.requireNonNull(
            strategy, "Placement strategy cannot be null");

    easyBoard = new EasyBoard();

    // Outer track: 72 squares.
    outerTrack = createPerimeter(BOARD_SIZE);

    // Middle track: 64 squares.
    middleTrack = createPerimeter(17)
            .stream()
            .map(cell -> shift(cell, 1))
            .toList();

    // Inner Easy track: 52 squares.
    // No rotation or color changes.
    innerTrack = easyBoard.getSharedTrackCells()
            .stream()
            .map(cell -> shift(cell, 2))
            .toList();

    Map<PlayerColor, List<BoardCell>> paths =
            new EnumMap<>(PlayerColor.class);

    Set<BoardCell> fixedSquares = new HashSet<>();

    for (PlayerColor color : PlayerColor.values()) {

        paths.put(color, createPlayerPath(color));

        fixedSquares.add(getStartSquareCell(color));
        fixedSquares.add(getOuterArrowSquare(color));
        fixedSquares.add(getMiddleArrowSquare(color));
        fixedSquares.add(getInnerArrowSquare(color));
    }

    playerPaths = Collections.unmodifiableMap(paths);

    // Strategy Pattern:
    // Generate special squares without overlaps.
    SpecialSquarePlacementStrategy.Placement placement =
            strategy.place(
                    getAllSharedTrackCells(),
                    fixedSquares,
                    SAFE_SQUARE_COUNT,
                    QUESTION_SQUARE_COUNT / 3,
                    SURPRISE_SQUARE_COUNT
            );

    safeSquares = placement.safeSquares();

    Map<BoardCell, QuestionLevel> questions =
            new LinkedHashMap<>();

    placement.questionSquares().forEach(
            (cell, difficulty) ->
                    questions.put(
                            cell,
                            QuestionLevel.valueOf(
                                    difficulty.name()))
    );

    questionSquares =
            Collections.unmodifiableMap(questions);

    surpriseSquares = placement.surpriseSquares();
}

   

    /**
     * Creates a clockwise perimeter.
     *
     * RED -> GREEN -> YELLOW -> BLUE -> RED.
     *
     * 19x19: 72 cells.
     * 17x17: 64 cells.
     */
    private static List<BoardCell> createPerimeter(int size) {

        List<BoardCell> cells =
                new ArrayList<>(4 * (size - 1));

        // Left edge: bottom to top.
        for (int row = size - 1; row >= 0; row--) {
            cells.add(new BoardCell(row, 0));
        }

        // Top edge: left to right.
        for (int col = 1; col < size; col++) {
            cells.add(new BoardCell(0, col));
        }

        // Right edge: top to bottom.
        for (int row = 1; row < size; row++) {
            cells.add(new BoardCell(row, size - 1));
        }

        // Bottom edge: right to left.
        for (int col = size - 2; col >= 1; col--) {
            cells.add(new BoardCell(size - 1, col));
        }

        if (cells.size() != 4 * (size - 1)
                || new HashSet<>(cells).size() != cells.size()) {

            throw new IllegalStateException(
                    "Invalid perimeter for size " + size);
        }

        return List.copyOf(cells);
    }

    /**
     * Shifts coordinates without rotating the board.
     */
    private static BoardCell shift(
            BoardCell cell, int offset) {

        return new BoardCell(
                cell.row() + offset,
                cell.col() + offset);
    }

    /**
     * Returns the starting index on the outer track.
     *
     * The colors stay in the same positions as Easy.
     */
    public int getStartSquare(PlayerColor color) {

        Objects.requireNonNull(
                color, "Player color cannot be null");

        return switch (color) {

            case RED -> 0;       // Bottom-left
            case GREEN -> 18;    // Top-left
            case YELLOW -> 36;   // Top-right
            case BLUE -> 54;     // Bottom-right
        };
    }

    public BoardCell getStartSquareCell(PlayerColor color) {

        return outerTrack.get(getStartSquare(color));
    }

    /**
     * Arrow from the 72-square track
     * to the 64-square track.
     */
    public BoardCell getOuterArrowSquare(PlayerColor color) {

        int index =
                (getStartSquare(color) + OUTER_ARROW_STEP)
                % OUTER_TRACK_SIZE;

        return outerTrack.get(index);
    }

    /**
     * Arrow from the 64-square track
     * to the 52-square track.
     */
    public BoardCell getMiddleArrowSquare(PlayerColor color) {

        Objects.requireNonNull(
                color, "Player color cannot be null");

        int middleStart = switch (color) {

            case RED -> 0;
            case GREEN -> 16;
            case YELLOW -> 32;
            case BLUE -> 48;
        };

        int index =
                (middleStart + MIDDLE_ARROW_STEP)
                % MIDDLE_TRACK_SIZE;

        return middleTrack.get(index);
    }

    /**
     * Arrow from the inner Easy track
     * to the player's home path.
     */
    public BoardCell getInnerArrowSquare(PlayerColor color) {

        Objects.requireNonNull(
                color, "Player color cannot be null");

        return shift(
                easyBoard.getArrowSquare(color), 2);
    }

    /**
     * Returns the original six home-path cells,
     * shifted into the 19x19 Hard board.
     */
    public List<BoardCell> getHomePathCells(PlayerColor color) {

        Objects.requireNonNull(
                color, "Player color cannot be null");

        return easyBoard.getHomePathCells(color)
                .stream()
                .map(cell -> shift(cell, 2))
                .toList();
    }

    /**
     * Creates a player's complete movement route:
     *
     * 1. Outer track to the outer arrow.
     * 2. Middle 64-square track.
     * 3. Inner 52-square track.
     * 4. Six home-path cells.
     *
     * Progress zero is the starting square.
     */
    private List<BoardCell> createPlayerPath(PlayerColor color) {

        List<BoardCell> path = new ArrayList<>();

        int start = getStartSquare(color);

        // Outer track: 66 cells including the start
        // and the outer arrow.
        for (int step = 0;
                step <= OUTER_ARROW_STEP;
                step++) {

            int index = (start + step) % OUTER_TRACK_SIZE;

            path.add(outerTrack.get(index));
        }

        // Enter the middle track immediately
        // after its arrow, complete one lap,
        // and return to that arrow.
        int middleArrowIndex =
                middleTrack.indexOf(
                        getMiddleArrowSquare(color));

        if (middleArrowIndex < 0) {
            throw new IllegalStateException(
                    "Missing middle arrow for " + color);
        }

        for (int step = 1;
                step <= MIDDLE_TRACK_SIZE;
                step++) {

            int index =
                    (middleArrowIndex + step)
                    % MIDDLE_TRACK_SIZE;

            path.add(middleTrack.get(index));
        }

        // Enter the inner Easy track.
        int innerArrowIndex =
                innerTrack.indexOf(
                        getInnerArrowSquare(color));

        if (innerArrowIndex < 0) {
            throw new IllegalStateException(
                    "Missing inner arrow for " + color);
        }

        for (int step = 1;
                step <= INNER_TRACK_SIZE;
                step++) {

            int index =
                    (innerArrowIndex + step)
                    % INNER_TRACK_SIZE;

            path.add(innerTrack.get(index));
        }

        // Finish on the player's home path.
        path.addAll(getHomePathCells(color));

        // Check the three transitions between tracks.
        if (!areNeighbors(path.get(65), path.get(66))
                || !areNeighbors(path.get(129), path.get(130))
                || !areNeighbors(path.get(181), path.get(182))) {

            throw new IllegalStateException(
                    "Broken track transfer for " + color);
        }

        // EasyBoard includes diagonal one-cell turns.
        // Preserve its original movement geometry.
        for (int i = 1; i < path.size(); i++) {

            if (!areConsecutive(
                    path.get(i - 1), path.get(i))) {

                throw new IllegalStateException(
                        "Disconnected path cells for " + color
                        + " at steps " + (i - 1) + " and " + i);
            }
        }

        if (path.size() != 188) {
            throw new IllegalStateException(
                    "Hard player path must have 188 cells");
        }

        return List.copyOf(path);
    }

    /**
     * Checks horizontal/vertical adjacency.
     */
    private static boolean areNeighbors(
            BoardCell a, BoardCell b) {

        return Math.abs(a.row() - b.row())
                + Math.abs(a.col() - b.col()) == 1;
    }

    /**
     * Allows the original one-cell diagonal turns
     * from EasyBoard.
     */
    private static boolean areConsecutive(
            BoardCell a, BoardCell b) {

        return Math.max(
                Math.abs(a.row() - b.row()),
                Math.abs(a.col() - b.col())) == 1;
    }

    /**
     * Returns the complete route for one player.
     */
    public List<BoardCell> getPlayerPath(PlayerColor color) {

        Objects.requireNonNull(
                color, "Player color cannot be null");

        return playerPaths.get(color);
    }

    /**
     * Returns the final legal progress index.
     */
    public int getFinishSteps(PlayerColor color) {

        return getPlayerPath(color).size() - 1;
    }

    /**
     * Returns the original Easy board.
     */
    public EasyBoard getEasyBoard() {

        return easyBoard;
    }

    /**
     * Returns the 72 outer-track cells.
     */
    public List<BoardCell> getOuterTrackCells() {

        return outerTrack;
    }

    /**
     * Returns the 64 middle-track cells.
     */
    public List<BoardCell> getMiddleTrackCells() {

        return middleTrack;
    }

    /**
     * Returns the 52 inner-track cells.
     */
    public List<BoardCell> getInnerTrackCells() {

        return innerTrack;
    }

    /**
     * Returns the new Hard track.
     */
    public List<BoardCell> getAdditionalTrackCells() {

        return outerTrack;
    }

    public BoardCell getAdditionalTrackCell(int index) {

        return outerTrack.get(index);
    }

    /**
     * Returns outer-track indices 0-71.
     */
    public List<Integer> getAdditionalTrack() {

        return IntStream.range(0, OUTER_TRACK_SIZE)
                .boxed()
                .toList();
    }

    /**
     * Returns all shared-track cells:
     * 72 + 64 + 52 = 188.
     */
    public List<BoardCell> getAllSharedTrackCells() {

        List<BoardCell> cells =
                new ArrayList<>(outerTrack);

        cells.addAll(middleTrack);
        cells.addAll(innerTrack);

        return List.copyOf(cells);
    }

    /**
     * Returns twelve fixed arrow squares.
     */
    public Set<BoardCell> getArrowSquares() {

        Set<BoardCell> arrows = new HashSet<>();

        for (PlayerColor color : PlayerColor.values()) {

            arrows.add(getOuterArrowSquare(color));
            arrows.add(getMiddleArrowSquare(color));
            arrows.add(getInnerArrowSquare(color));
        }

        return Set.copyOf(arrows);
    }

    /**
     * Returns twelve random Safe Squares.
     */
    public Set<BoardCell> getSafeSquares() {

        return safeSquares;
    }

    /**
     * Returns nine Question Squares:
     * three of each difficulty.
     */
    public Map<BoardCell, QuestionLevel> getQuestionSquares() {

        return questionSquares;
    }

    /**
     * Returns three random Surprise Squares.
     */
    public Set<BoardCell> getSurpriseSquares() {

        return surpriseSquares;
    }

    /**
     * Checks whether a square is protected
     * from capturing.
     *
     * Protected:
     * Safe, Surprise, Start and Home-path squares.
     */
    public boolean isProtectedSquare(BoardCell cell) {

        if (safeSquares.contains(cell)
                || surpriseSquares.contains(cell)) {

            return true;
        }

        for (PlayerColor color : PlayerColor.values()) {

            if (getStartSquareCell(color).equals(cell)
                    || getHomePathCells(color).contains(cell)) {

                return true;
            }
        }

        return false;
    }
}
