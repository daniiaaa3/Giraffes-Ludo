
package Model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

import Model.EasyBoard.BoardCell;
import Model.GameSetup.PlayerColor;

public class MediumBoardTest {

    private final MediumBoard board =
            new MediumBoard(new Random(42));

    @Test
    void testTrackSizes() {
        assertEquals(64, board.getOuterTrackCells().size());
        assertEquals(52, board.getInnerTrackCells().size());
        assertEquals(116, board.getAllSharedTrackCells().size());

        assertEquals(116, new HashSet<>(
                board.getAllSharedTrackCells()).size());
    }

    @Test
    void testEasyBoardOrientationIsPreserved() {
        List<BoardCell> original =
                board.getEasyBoard().getSharedTrackCells();

        List<BoardCell> inner =
                board.getInnerTrackCells();

        for (int i = 0; i < original.size(); i++) {
            BoardCell old = original.get(i);
            BoardCell expected =
                    new BoardCell(old.row() + 1, old.col() + 1);

            assertEquals(expected, inner.get(i));
        }
    }

    @Test
    void testStartingSquares() {
        assertEquals(new BoardCell(16, 0),
                board.getStartSquareCell(PlayerColor.RED));

        assertEquals(new BoardCell(0, 0),
                board.getStartSquareCell(PlayerColor.GREEN));

        assertEquals(new BoardCell(0, 16),
                board.getStartSquareCell(PlayerColor.YELLOW));

        assertEquals(new BoardCell(16, 16),
                board.getStartSquareCell(PlayerColor.BLUE));
    }

    @Test
    void testPlayerPathsAndTransitions() {
        for (PlayerColor color : PlayerColor.values()) {
            List<BoardCell> path = board.getPlayerPath(color);

            assertEquals(116, path.size());
            assertEquals(115, board.getFinishSteps(color));

            assertEquals(board.getStartSquareCell(color),
                    path.get(0));

            assertEquals(board.getOuterArrowSquare(color),
                    path.get(57));

            assertEquals(board.getInnerArrowSquare(color),
                    path.get(109));

            assertEquals(board.getHomePathCells(color).get(0),
                    path.get(110));

            assertEquals(1, distance(
                    path.get(57), path.get(58)));

            assertEquals(1, distance(
                    path.get(109), path.get(110)));
        }
    }

    @Test
    void testSpecialSquareCountsAndNoOverlap() {
        assertEquals(8, board.getSafeSquares().size());
        assertEquals(6, board.getQuestionSquares().size());
        assertEquals(2, board.getSurpriseSquares().size());
        assertEquals(8, board.getArrowSquares().size());

        Set<BoardCell> specials = new HashSet<>();

        specials.addAll(board.getSafeSquares());
        specials.addAll(board.getQuestionSquares().keySet());
        specials.addAll(board.getSurpriseSquares());

        assertEquals(16, specials.size());

        assertTrue(board.getAllSharedTrackCells()
                .containsAll(specials));

        for (PlayerColor color : PlayerColor.values()) {
            assertFalse(specials.contains(
                    board.getStartSquareCell(color)));
        }

        for (BoardCell arrow : board.getArrowSquares()) {
            assertFalse(specials.contains(arrow));
        }
    }

    @Test
    void testQuestionDifficultyDistribution() {
        for (MediumBoard.QuestionLevel level :
                MediumBoard.QuestionLevel.values()) {

            long count = board.getQuestionSquares()
                    .values()
                    .stream()
                    .filter(value -> value == level)
                    .count();

            assertEquals(2, count);
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
        }
    }

    @Test
    void testRandomPlacementIsReproducible() {
        MediumBoard first = new MediumBoard(new Random(42));
        MediumBoard second = new MediumBoard(new Random(42));

        assertEquals(first.getSafeSquares(),
                second.getSafeSquares());

        assertEquals(first.getQuestionSquares(),
                second.getQuestionSquares());

        assertEquals(first.getSurpriseSquares(),
                second.getSurpriseSquares());
    }

    private int distance(BoardCell a, BoardCell b) {
        return Math.abs(a.row() - b.row())
                + Math.abs(a.col() - b.col());
    }
}
