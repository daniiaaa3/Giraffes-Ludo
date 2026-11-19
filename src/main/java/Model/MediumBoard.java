
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
 * Represents the Medium-level Ludo board.
 *
 * Keeps the Easy board in its original orientation
 * and adds a 64-square outer track.
 *
 * SRS: FR-2.2, FR-2.4, FR-2.5, FR-2.6
 * Iteration: 2
 */
public final class MediumBoard {

    public static final int BOARD_SIZE = 17;
    public static final int ADDITIONAL_TRACK_SIZE = 64;

    public static final int SAFE_SQUARE_COUNT = 8;
    public static final int ARROW_SQUARE_COUNT = 8;
    public static final int QUESTION_SQUARE_COUNT = 6;
    public static final int SURPRISE_SQUARE_COUNT = 2;

    public enum QuestionLevel {
        EASY, MEDIUM, HARD
    }

    private final EasyBoard easyBoard;

    private final List<BoardCell> outerTrack;
    private final List<BoardCell> innerTrack;

    private final Map<PlayerColor, List<BoardCell>> playerPaths;

    private final Set<BoardCell> safeSquares;
    private final Map<BoardCell, QuestionLevel> questionSquares;
    private final Set<BoardCell> surpriseSquares;

   
  


/**
 * Creates a Medium board with random
 * special-square placement.
 */
public MediumBoard() {
    this(new RandomSpecialSquarePlacementStrategy());
}

/**
 * Allows reproducible random placement
 * for JUnit tests.
 */
public MediumBoard(Random random) {
    this(new RandomSpecialSquarePlacementStrategy(random));
}

/**
 * Strategy Pattern:
 * The placement algorithm is supplied
 * from outside the MediumBoard class.
 */
public MediumBoard(SpecialSquarePlacementStrategy strategy) {

    Objects.requireNonNull(
            strategy, "Placement strategy cannot be null");

    easyBoard = new EasyBoard();

    // Outer track: 64 squares.
    outerTrack = createOuterTrack();

    // Original Easy track, without rotation.
    innerTrack = easyBoard.getSharedTrackCells()
            .stream()
            .map(MediumBoard::centerEasyCell)
            .toList();

    Map<PlayerColor, List<BoardCell>> paths =
            new EnumMap<>(PlayerColor.class);

    Set<BoardCell> fixedSquares = new HashSet<>();

    for (PlayerColor color : PlayerColor.values()) {

        paths.put(color, createPlayerPath(color));

        fixedSquares.add(getStartSquareCell(color));
        fixedSquares.add(getOuterArrowSquare(color));
        fixedSquares.add(getInnerArrowSquare(color));
    }

    playerPaths = Collections.unmodifiableMap(paths);

    // Delegate special-square placement
    // to the selected Strategy.
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
     * Creates the 64-square outer track.
     *
     * Clockwise movement:
     * RED -> GREEN -> YELLOW -> BLUE -> RED.
     *
     * Coordinates are on a 17x17 grid.
     */
    private static List<BoardCell> createOuterTrack() {

        List<BoardCell> cells =
                new ArrayList<>(ADDITIONAL_TRACK_SIZE);

        // Left edge: RED -> GREEN
        for (int row = 16; row >= 0; row--) {
            cells.add(new BoardCell(row, 0));
        }

        // Top edge: GREEN -> YELLOW
        for (int col = 1; col <= 16; col++) {
            cells.add(new BoardCell(0, col));
        }

        // Right edge: YELLOW -> BLUE
        for (int row = 1; row <= 16; row++) {
            cells.add(new BoardCell(row, 16));
        }

        // Bottom edge: BLUE -> RED
        for (int col = 15; col >= 1; col--) {
            cells.add(new BoardCell(16, col));
        }

        if (cells.size() != ADDITIONAL_TRACK_SIZE
                || new HashSet<>(cells).size()
                        != ADDITIONAL_TRACK_SIZE) {

            throw new IllegalStateException(
                    "Outer track must contain 64 unique squares");
        }

        return List.copyOf(cells);
    }

    /**
     * Places the 15x15 Easy board inside
     * the 17x17 Medium board.
     *
     * No rotation or color changes.
     */
    private static BoardCell centerEasyCell(BoardCell cell) {

        return new BoardCell(
                cell.row() + 1,
                cell.col() + 1);
    }

    /**
     * Returns the starting index of a color
     * on the outer 64-square track.
     */
    public int getStartSquare(PlayerColor color) {

        Objects.requireNonNull(
                color, "Player color cannot be null");

        return switch (color) {

            case RED -> 0;       // Bottom-left
            case GREEN -> 16;    // Top-left
            case YELLOW -> 32;   // Top-right
            case BLUE -> 48;     // Bottom-right
        };
    }

    /**
     * Returns the actual starting cell
     * on the Medium board.
     */
    public BoardCell getStartSquareCell(PlayerColor color) {

        return outerTrack.get(getStartSquare(color));
    }

    /**
     * Returns the outer arrow of a color.
     * This arrow leads to the inner track.
     */
    public BoardCell getOuterArrowSquare(PlayerColor color) {

        int index = (getStartSquare(color) + 57)
                % ADDITIONAL_TRACK_SIZE;

        return outerTrack.get(index);
    }

    /**
     * Returns the inner arrow leading
     * toward the player's home path.
     */
    public BoardCell getInnerArrowSquare(PlayerColor color) {

        return centerEasyCell(
                easyBoard.getArrowSquare(color));
    }

    /**
     * Returns the original six home-path cells
     * in the Medium board coordinates.
     */
    public List<BoardCell> getHomePathCells(PlayerColor color) {

        Objects.requireNonNull(
                color, "Player color cannot be null");

        return easyBoard.getHomePathCells(color)
                .stream()
                .map(MediumBoard::centerEasyCell)
                .toList();
    }

    /**
     * Creates the complete route for one player:
     *
     * 1. Outer track, from start to outer arrow.
     * 2. Inner shared track.
     * 3. Six home-path squares.
     *
     * Progress 0 is the starting square.
     */
    private List<BoardCell> createPlayerPath(PlayerColor color) {

        List<BoardCell> path = new ArrayList<>();

        int start = getStartSquare(color);

        // Outer track: 58 cells including
        // the start square and outer arrow.
        for (int step = 0; step <= 57; step++) {

            int index = (start + step)
                    % ADDITIONAL_TRACK_SIZE;

            path.add(outerTrack.get(index));
        }

        // Find the inner home-entry arrow.
        int innerArrowIndex = innerTrack.indexOf(
                getInnerArrowSquare(color));

        if (innerArrowIndex < 0) {

            throw new IllegalStateException(
                    "Missing inner arrow for " + color);
        }

        // Continue on the 52-square inner track.
        // Begin after the inner arrow, make one lap,
        // and return to the arrow before entering home.
        for (int step = 1;
                step <= EasyBoard.TRACK_SIZE;
                step++) {

            int index = (innerArrowIndex + step)
                    % EasyBoard.TRACK_SIZE;

            path.add(innerTrack.get(index));
        }

        // Add the six home-path squares.
        List<BoardCell> homePath =
                getHomePathCells(color);

        path.addAll(homePath);

        // Verify the transfers between the tracks.
        if (!areNeighbors(path.get(57), path.get(58))
                || !areNeighbors(path.get(109),
                        homePath.get(0))) {

            throw new IllegalStateException(
                    "Broken track transfer for " + color);
        }

        return List.copyOf(path);
    }

    /**
     * Returns true when two cells share an edge.
     */
    private static boolean areNeighbors(
            BoardCell a, BoardCell b) {

        return Math.abs(a.row() - b.row())
                + Math.abs(a.col() - b.col()) == 1;
    }

    /**
     * Returns a player's complete movement path.
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
     * Returns all 64 outer-track cells.
     */
    public List<BoardCell> getOuterTrackCells() {

        return outerTrack;
    }

    /**
     * Returns all 52 inner-track cells.
     */
    public List<BoardCell> getInnerTrackCells() {

        return innerTrack;
    }

    /**
     * Returns the additional Medium track.
     */
    public List<BoardCell> getAdditionalTrackCells() {

        return outerTrack;
    }

    /**
     * Returns one outer-track cell by index.
     */
    public BoardCell getAdditionalTrackCell(int index) {

        return outerTrack.get(index);
    }

    /**
     * Returns the indices 0-63.
     */
    public List<Integer> getAdditionalTrack() {

        return IntStream.range(
                0, ADDITIONAL_TRACK_SIZE)
                .boxed()
                .toList();
    }

    /**
     * Returns all shared-track cells:
     * 64 outer + 52 inner.
     */
    public List<BoardCell> getAllSharedTrackCells() {

        List<BoardCell> all =
                new ArrayList<>(outerTrack);

        all.addAll(innerTrack);

        return List.copyOf(all);
    }

    /**
     * Returns all eight fixed arrow squares.
     */
    public Set<BoardCell> getArrowSquares() {

        Set<BoardCell> arrows = new HashSet<>();

        for (PlayerColor color : PlayerColor.values()) {

            arrows.add(getOuterArrowSquare(color));
            arrows.add(getInnerArrowSquare(color));
        }

        return Set.copyOf(arrows);
    }

    /**
     * Returns the eight random Safe Squares.
     * Fixed starting squares are separate.
     */
    public Set<BoardCell> getSafeSquares() {

        return safeSquares;
    }

    /**
     * Returns the six Question Squares
     * and their question difficulty.
     */
    public Map<BoardCell, QuestionLevel> getQuestionSquares() {

        return questionSquares;
    }

    /**
     * Returns the two Surprise Squares.
     */
    public Set<BoardCell> getSurpriseSquares() {

        return surpriseSquares;
    }

    /**
     * Checks whether a cell is protected
     * from capturing.
     *
     * Protected cells include:
     * - Safe Squares
     * - Surprise Squares
     * - Starting Squares
     * - Home-path Squares
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
