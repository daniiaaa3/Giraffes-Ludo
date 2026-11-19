
package Model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

import Model.EasyBoard.BoardCell;
import Model.GameSetup.PlayerColor;

public class HardBoardTest {

    private final HardBoard board =
            new HardBoard(new Random(42));

    @Test
    void testTrackSizes() {
        assertEquals(72, board.getOuterTrackCells().size());
        assertEquals(64, board.getMiddleTrackCells().size());
        assertEquals(52, board.getInnerTrackCells().size());

        assertEquals(188,
                board.getAllSharedTrackCells().size());

        assertEquals(188, new HashSet<>(
                board.getAllSharedTrackCells()).size());
    }

    @Test
    void testOuterAndMiddleTrackContinuity() {
        checkClosedTrack(board.getOuterTrackCells());
        checkClosedTrack(board.getMiddleTrackCells());
    }

    @Test
    void testEasyBoardOrientationIsPreserved() {
        List<BoardCell> original =
                board.getEasyBoard().getSharedTrackCells();

        List<BoardCell> inner =
                board.getInnerTrackCells();

        for (int i = 0; i < original.size(); i++) {
            BoardCell expected = new BoardCell(
                    original.get(i).row() + 2,
                    original.get(i).col() + 2);

            assertEquals(expected, inner.get(i));
        }
    }

    @Test
    void testStartingSquares() {
        assertEquals(new BoardCell(18, 0),
                board.getStartSquareCell(PlayerColor.RED));

        assertEquals(new BoardCell(0, 0),
                board.getStartSquareCell(PlayerColor.GREEN));

        assertEquals(new BoardCell(0, 18),
                board.getStartSquareCell(PlayerColor.YELLOW));

        assertEquals(new BoardCell(18, 18),
                board.getStartSquareCell(PlayerColor.BLUE));
    }

    @Test
    void testArrowSquares() {
        assertEquals(12, board.getArrowSquares().size());

        for (PlayerColor color : PlayerColor.values()) {

            assertTrue(board.getOuterTrackCells().contains(
                    board.getOuterArrowSquare(color)));

            assertTrue(board.getMiddleTrackCells().contains(
                    board.getMiddleArrowSquare(color)));

            assertTrue(board.getInnerTrackCells().contains(
                    board.getInnerArrowSquare(color)));
        }
    }

    @Test
    void testPlayerPathsAndTransitions() {
        for (PlayerColor color : PlayerColor.values()) {

            List<BoardCell> path =
                    board.getPlayerPath(color);

            assertEquals(188, path.size());
            assertEquals(187,
                    board.getFinishSteps(color));

            assertEquals(board.getStartSquareCell(color),
                    path.get(0));

            assertEquals(board.getOuterArrowSquare(color),
                    path.get(65));

            assertEquals(board.getMiddleArrowSquare(color),
                    path.get(129));

            assertEquals(board.getInnerArrowSquare(color),
                    path.get(181));

            List<BoardCell> home =
                    board.getHomePathCells(color);

            assertEquals(home, path.subList(182, 188));

            // Every consecutive step must be adjacent.
            for (int i = 1; i < path.size(); i++) {
                assertEquals(1,
                        maxDistance(
                                path.get(i - 1),
                                path.get(i)),
                        "Disconnected path for "
                                + color + " at step " + i);
            }

            // Track transfers must share an edge.
            assertEquals(1,
                    distance(path.get(65), path.get(66)));

            assertEquals(1,
                    distance(path.get(129), path.get(130)));

            assertEquals(1,
                    distance(path.get(181), path.get(182)));
        }
    }

    @Test
    void testSpecialSquareCountsAndNoOverlap() {

        assertEquals(12, board.getSafeSquares().size());
        assertEquals(9, board.getQuestionSquares().size());
        assertEquals(3, board.getSurpriseSquares().size());

        Set<BoardCell> special = new HashSet<>();

        special.addAll(board.getSafeSquares());
        special.addAll(board.getQuestionSquares().keySet());
        special.addAll(board.getSurpriseSquares());

        assertEquals(24, special.size());

        assertTrue(board.getAllSharedTrackCells()
                .containsAll(special));

        for (BoardCell arrow : board.getArrowSquares()) {
            assertFalse(special.contains(arrow));
        }

        for (PlayerColor color : PlayerColor.values()) {
            assertFalse(special.contains(
                    board.getStartSquareCell(color)));
        }
    }

    @Test
    void testQuestionDifficultyDistribution() {

        for (HardBoard.QuestionLevel level :
                HardBoard.QuestionLevel.values()) {

            long count = board.getQuestionSquares()
                    .values()
                    .stream()
                    .filter(value -> value == level)
                    .count();

            assertEquals(3, count);
        }
    }

    @Test
    void testProtectedSquares() {

        for (BoardCell cell : board.getSafeSquares()) {
            assertTrue(board.isProtectedSquare(cell));
        }

        for (BoardCell cell : board.getSurpriseSquares()) {
            assertTrue(board.isProtectedSquare(cell));
        }

        for (BoardCell cell :
                board.getQuestionSquares().keySet()) {
            assertFalse(board.isProtectedSquare(cell));
        }

        for (PlayerColor color : PlayerColor.values()) {

            assertTrue(board.isProtectedSquare(
                    board.getStartSquareCell(color)));

            for (BoardCell cell :
                    board.getHomePathCells(color)) {
                assertTrue(board.isProtectedSquare(cell));
            }
        }
    }

    @Test
    void testRandomPlacementIsReproducible() {

        HardBoard first =
                new HardBoard(new Random(42));

        HardBoard second =
                new HardBoard(new Random(42));

        assertEquals(first.getSafeSquares(),
                second.getSafeSquares());

        assertEquals(first.getQuestionSquares(),
                second.getQuestionSquares());

        assertEquals(first.getSurpriseSquares(),
                second.getSurpriseSquares());
    }

    private void checkClosedTrack(List<BoardCell> cells) {

        for (int i = 0; i < cells.size(); i++) {

            BoardCell current = cells.get(i);

            BoardCell next = cells.get(
                    (i + 1) % cells.size());

            assertEquals(1, distance(current, next));
        }
    }

    private int distance(BoardCell a, BoardCell b) {
        return Math.abs(a.row() - b.row())
                + Math.abs(a.col() - b.col());
    }

    private int maxDistance(BoardCell a, BoardCell b) {
        return Math.max(
                Math.abs(a.row() - b.row()),
                Math.abs(a.col() - b.col()));
    }
}
