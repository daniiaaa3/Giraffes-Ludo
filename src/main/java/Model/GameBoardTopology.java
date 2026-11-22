
package Model;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import Model.EasyBoard.BoardCell;
import Model.GameSetup.PlayerColor;

/**
 * Provides board geometry for all difficulty levels.
 *
 * Reuses the existing EasyBoard, MediumBoard and HardBoard
 * models without duplicating their movement paths.
 *
 * MVC Component: Model
 * Iteration: 2
 */
public final class GameBoardTopology {

    private final GameDifficulty difficulty;

    private final Map<PlayerColor, List<BoardCell>> paths =
            new EnumMap<>(PlayerColor.class);

    private final Set<BoardCell> protectedSquares =
            new HashSet<>();

    private final Set<BoardCell> safeSquares;
    private final Set<BoardCell> surpriseSquares;

    private final Map<BoardCell, GameDifficulty> questionSquares;

    public GameBoardTopology(GameDifficulty difficulty) {
        this(difficulty, new EasyBoard());
    }

    /**
     * Supports an existing EasyBoard instance so older
     * GameState constructors and tests remain compatible.
     */
    public GameBoardTopology(
            GameDifficulty difficulty, EasyBoard easyBoard) {

        this.difficulty = Objects.requireNonNull(difficulty);
        Objects.requireNonNull(easyBoard);

        Set<BoardCell> safes = Set.of();
        Set<BoardCell> surprises = Set.of();
        Map<BoardCell, GameDifficulty> questions = new HashMap<>();

        switch (difficulty) {

            case EASY -> {
                for (PlayerColor color : PlayerColor.values()) {
                    paths.put(color, createEasyPath(easyBoard, color));
                }
            }

            case MEDIUM -> {
                MediumBoard board = new MediumBoard();

                for (PlayerColor color : PlayerColor.values()) {
                    paths.put(color, board.getPlayerPath(color));
                    protectedSquares.addAll(
                            board.getHomePathCells(color));
                }

                safes = board.getSafeSquares();
                surprises = board.getSurpriseSquares();

                board.getQuestionSquares().forEach(
                        (cell, level) -> questions.put(
                                cell,
                                GameDifficulty.valueOf(level.name())));
            }

            case HARD -> {
                HardBoard board = new HardBoard();

                for (PlayerColor color : PlayerColor.values()) {
                    paths.put(color, board.getPlayerPath(color));
                    protectedSquares.addAll(
                            board.getHomePathCells(color));
                }

                safes = board.getSafeSquares();
                surprises = board.getSurpriseSquares();

                board.getQuestionSquares().forEach(
                        (cell, level) -> questions.put(
                                cell,
                                GameDifficulty.valueOf(level.name())));
            }
        }

        safeSquares = Set.copyOf(safes);
        surpriseSquares = Set.copyOf(surprises);
        questionSquares = Map.copyOf(questions);

        protectedSquares.addAll(safeSquares);
        protectedSquares.addAll(surpriseSquares);

        for (PlayerColor color : PlayerColor.values()) {
            List<BoardCell> path = paths.get(color);

            protectedSquares.add(path.get(0));
            protectedSquares.add(path.get(path.size() - 1));
        }
    }

    /**
     * Builds the original Easy movement path.
     */
    private static List<BoardCell> createEasyPath(
            EasyBoard board, PlayerColor color) {

        List<BoardCell> path = new ArrayList<>();

        List<BoardCell> track = board.getSharedTrackCells();

        int start = board.getStartSquare(color);
        int arrow = track.indexOf(board.getArrowSquare(color));

        if (arrow < 0) {
            throw new IllegalStateException(
                    "Missing home arrow for " + color);
        }

        int entrySteps = Math.floorMod(
                arrow - start, EasyBoard.TRACK_SIZE);

        for (int step = 0; step <= entrySteps; step++) {
            path.add(board.getSharedTrackCell(
                    (start + step) % EasyBoard.TRACK_SIZE));
        }

        path.addAll(board.getHomePathCells(color));

        return List.copyOf(path);
    }

    public GameDifficulty getDifficulty() {
        return difficulty;
    }

    public int getBoardSize() {
        return difficulty.getBoardSize();
    }

    public List<BoardCell> getPlayerPath(PlayerColor color) {

        List<BoardCell> path = paths.get(color);

        if (path == null) {
            throw new IllegalArgumentException(
                    "Invalid player color.");
        }

        return List.copyOf(path);
    }

    public int getEntrySteps(PlayerColor color) {
        return getPlayerPath(color).size()
                - EasyBoard.HOME_PATH_SIZE - 1;
    }

    public int getFinishSteps(PlayerColor color) {
        return getPlayerPath(color).size() - 1;
    }

    public Optional<BoardCell> getCellAt(
            PlayerColor color, int progress) {

        List<BoardCell> path = getPlayerPath(color);

        if (progress < 0) {
            return Optional.empty();
        }

        if (progress >= path.size()) {
            throw new IllegalArgumentException(
                    "Progress exceeds the finish square.");
        }

        return Optional.of(path.get(progress));
    }

    public boolean isProtectedSquare(BoardCell cell) {
        return cell != null && protectedSquares.contains(cell);
    }

    public Set<BoardCell> getSafeSquares() {
        return safeSquares;
    }

    public Set<BoardCell> getSurpriseSquares() {
        return surpriseSquares;
    }

    public Map<BoardCell, GameDifficulty> getQuestionSquares() {
        return questionSquares;
    }
}
