
package Model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Model.GameSetup.PlayerColor;

class EasyBoardTest {

    private EasyBoard board;

    @BeforeEach
    void setUp() {
        board = new EasyBoard();
    }

    @Test
    void sharedTrackHas52Squares() {
        assertEquals(52, board.getSharedTrack().size());
        assertEquals(0, board.getSharedTrack().get(0));
        assertEquals(51, board.getSharedTrack().get(51));
    }

    @Test
    void eachColorHasSixHomeSquares() {
        for (PlayerColor color : PlayerColor.values()) {
            assertEquals(6, board.getHomePath(color).size());
        }
    }

    @Test
    void eachColorHasUniqueStartSquare() {
        Set<Integer> starts = new HashSet<>();

        for (PlayerColor color : PlayerColor.values()) {
            int start = board.getStartSquare(color);

            assertTrue(start >= 0 && start < 52);
            assertTrue(starts.add(start));
        }

        assertEquals(4, starts.size());
    }

    @Test
    void allStartSquaresAreProtected() {
        for (PlayerColor color : PlayerColor.values()) {
            int start = board.getStartSquare(color);

            assertTrue(board.isProtectedStartSquare(start));
        }
    }

    @Test
    void invalidSquareIsNotProtected() {
        assertFalse(board.isProtectedStartSquare(-1));
        assertFalse(board.isProtectedStartSquare(52));
    }

    @Test
    void eachColorHasValidArrowSquare() {
        for (PlayerColor color : PlayerColor.values()) {

            EasyBoard.BoardCell arrow =
                    board.getArrowSquare(color);

            assertTrue(arrow.row() >= 0 && arrow.row() < 15);
            assertTrue(arrow.col() >= 0 && arrow.col() < 15);

            assertTrue(board.isArrowSquare(
                    arrow.row(), arrow.col()));

            assertTrue(board.isHomeEntryArrow(
                    color, arrow.row(), arrow.col()));
        }
    }

    @Test
    void invalidColorIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> board.getStartSquare(null));

        assertThrows(IllegalArgumentException.class,
                () -> board.getArrowSquare(null));
    }

@Test
void trackCoordinatesMatchBoard() {

    assertEquals(52, board.getSharedTrackCells().size());

    // Every shared-track square must be unique.
    assertEquals(52, new HashSet<>(
            board.getSharedTrackCells()).size());

    // Starting positions.
    assertEquals(new EasyBoard.BoardCell(13, 6),
            board.getSharedTrackCell(0));

    assertEquals(new EasyBoard.BoardCell(6, 1),
            board.getSharedTrackCell(13));

    assertEquals(new EasyBoard.BoardCell(1, 8),
            board.getSharedTrackCell(26));

    assertEquals(new EasyBoard.BoardCell(8, 13),
            board.getSharedTrackCell(39));

    // Arrow positions along the shared track.
    assertEquals(board.getArrowSquare(PlayerColor.GREEN),
            board.getSharedTrackCell(11));

    assertEquals(board.getArrowSquare(PlayerColor.YELLOW),
            board.getSharedTrackCell(24));

    assertEquals(board.getArrowSquare(PlayerColor.BLUE),
            board.getSharedTrackCell(37));

    assertEquals(board.getArrowSquare(PlayerColor.RED),
            board.getSharedTrackCell(50));
}

@Test
void homePathsHaveCorrectCoordinates() {

    assertEquals(
            new EasyBoard.BoardCell(13, 7),
            board.getHomePathCells(PlayerColor.RED).get(0));

    assertEquals(
            new EasyBoard.BoardCell(7, 1),
            board.getHomePathCells(PlayerColor.GREEN).get(0));

    assertEquals(
            new EasyBoard.BoardCell(1, 7),
            board.getHomePathCells(PlayerColor.YELLOW).get(0));

    assertEquals(
            new EasyBoard.BoardCell(7, 13),
            board.getHomePathCells(PlayerColor.BLUE).get(0));

    for (PlayerColor color : PlayerColor.values()) {

        var path = board.getHomePathCells(color);

        assertEquals(6, path.size());
        assertEquals(6, new HashSet<>(path).size());

        for (var cell : path) {
            assertFalse(
                    board.getSharedTrackCells().contains(cell));
        }
    }
}

@Test
void fixedPositionsMatchOfficialBoard() {
    EasyBoard board = new EasyBoard();

    // Starting squares (#)
    assertEquals(new EasyBoard.BoardCell(13, 6),
        board.getSharedTrackCell(
            board.getStartSquare(GameSetup.PlayerColor.RED)));

    assertEquals(new EasyBoard.BoardCell(6, 1),
        board.getSharedTrackCell(
            board.getStartSquare(GameSetup.PlayerColor.GREEN)));

    assertEquals(new EasyBoard.BoardCell(1, 8),
        board.getSharedTrackCell(
            board.getStartSquare(GameSetup.PlayerColor.YELLOW)));

    assertEquals(new EasyBoard.BoardCell(8, 13),
        board.getSharedTrackCell(
            board.getStartSquare(GameSetup.PlayerColor.BLUE)));

    // Fixed arrow squares
    assertEquals(new EasyBoard.BoardCell(14, 7),
        board.getArrowSquare(GameSetup.PlayerColor.RED));

    assertEquals(new EasyBoard.BoardCell(7, 0),
        board.getArrowSquare(GameSetup.PlayerColor.GREEN));

    assertEquals(new EasyBoard.BoardCell(0, 7),
        board.getArrowSquare(GameSetup.PlayerColor.YELLOW));

    assertEquals(new EasyBoard.BoardCell(7, 14),
        board.getArrowSquare(GameSetup.PlayerColor.BLUE));
}

@Test
void homePathCoordinatesMatchEasyBoard() {
    EasyBoard board = new EasyBoard();

    assertEquals(java.util.List.of(
        new EasyBoard.BoardCell(13, 7),
        new EasyBoard.BoardCell(12, 7),
        new EasyBoard.BoardCell(11, 7),
        new EasyBoard.BoardCell(10, 7),
        new EasyBoard.BoardCell(9, 7),
        new EasyBoard.BoardCell(8, 7)
    ), board.getHomePathCells(GameSetup.PlayerColor.RED));

    assertEquals(java.util.List.of(
        new EasyBoard.BoardCell(7, 1),
        new EasyBoard.BoardCell(7, 2),
        new EasyBoard.BoardCell(7, 3),
        new EasyBoard.BoardCell(7, 4),
        new EasyBoard.BoardCell(7, 5),
        new EasyBoard.BoardCell(7, 6)
    ), board.getHomePathCells(GameSetup.PlayerColor.GREEN));

    assertEquals(java.util.List.of(
        new EasyBoard.BoardCell(1, 7),
        new EasyBoard.BoardCell(2, 7),
        new EasyBoard.BoardCell(3, 7),
        new EasyBoard.BoardCell(4, 7),
        new EasyBoard.BoardCell(5, 7),
        new EasyBoard.BoardCell(6, 7)
    ), board.getHomePathCells(GameSetup.PlayerColor.YELLOW));

    assertEquals(java.util.List.of(
        new EasyBoard.BoardCell(7, 13),
        new EasyBoard.BoardCell(7, 12),
        new EasyBoard.BoardCell(7, 11),
        new EasyBoard.BoardCell(7, 10),
        new EasyBoard.BoardCell(7, 9),
        new EasyBoard.BoardCell(7, 8)
    ), board.getHomePathCells(GameSetup.PlayerColor.BLUE));
}

}
