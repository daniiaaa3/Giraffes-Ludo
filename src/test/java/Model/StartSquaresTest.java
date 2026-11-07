package Model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.junit.jupiter.api.Test;

import Model.EasyBoard.BoardCell;
import Model.GameSetup.PlayerColor;
import Model.GameSetup.PlayerEntry;

/**
 * The four fixed start squares (#) of FR-2.5 and FR-3.2, and the
 * step-by-step path used to animate movement.
 */
public class StartSquaresTest {

    private static final List<PlayerEntry> ALL = List.of(
            new PlayerEntry("A", PlayerColor.RED),
            new PlayerEntry("B", PlayerColor.BLUE),
            new PlayerEntry("C", PlayerColor.GREEN),
            new PlayerEntry("D", PlayerColor.YELLOW));

    private static Random seat(int index) {
        return new Random() {
            @Override
            public int nextInt(int bound) {
                return index;
            }
        };
    }

    private static BoardCell expectedStart(PlayerColor c) {
        return switch (c) {
            case RED -> new BoardCell(13, 6);
            case GREEN -> new BoardCell(6, 1);
            case YELLOW -> new BoardCell(1, 8);
            case BLUE -> new BoardCell(8, 13);
        };
    }

    @Test
    void eachColorHasItsOwnProtectedStartSquareAtTheExpectedCell() {
        EasyBoard board = new EasyBoard();

        for (PlayerColor color : PlayerColor.values()) {
            int index = board.getStartSquare(color);
            assertTrue(board.isProtectedStartSquare(index),
                    color + " start square is protected");
            assertEquals(expectedStart(color), board.getSharedTrackCell(index),
                    color + " start square sits at its board cell");
        }
    }

    @Test
    void exactlyFourSquaresAreProtectedStartSquares() {
        EasyBoard board = new EasyBoard();
        int protectedCount = 0;

        for (int i = 0; i < EasyBoard.TRACK_SIZE; i++) {
            if (board.isProtectedStartSquare(i)) {
                protectedCount++;
            }
        }

        assertEquals(4, protectedCount,
                "only the four # start squares are protected in Iteration 1");
    }

    @Test
    void aSixReleasesEveryColorOntoItsOwnStartSquare() {
        for (int seatIndex = 0; seatIndex < 4; seatIndex++) {
            GameState game = new GameState(ALL, seat(seatIndex));
            PlayerEntry player = game.getActivePlayer();
            Piece piece = game.getPieces(player).get(2);

            game.recordRoll(player, 6);
            game.movePiece(player, piece);

            assertEquals(0, piece.getProgress());
            assertEquals(Optional.of(expectedStart(player.color())),
                    game.getBoardCell(piece),
                    player.color() + " lands on its own # square");
        }
    }

    @Test
    void piecesOfDifferentColorsMayShareAProtectedStartSquare() {
        // BLUE progress 13 = track index 0 = RED's start square
        GameState game = new GameState(ALL, seat(0));
        Piece blue = game.getPieces(ALL.get(1)).get(0);
        Piece red = game.getPieces(ALL.get(0)).get(0);

        red.moveTo(0, Piece.State.TRACK);
        blue.moveTo(13, Piece.State.TRACK);

        assertEquals(game.getBoardCell(red), game.getBoardCell(blue));
        assertEquals(Optional.of(expectedStart(PlayerColor.RED)),
                game.getBoardCell(blue));
    }

    // ---------------------------------------------- step-by-step path

    @Test
    void cellAtProgressMatchesTheBoardCellOfAPieceEverywhere() {
        GameState game = new GameState(ALL, seat(0));

        for (PlayerEntry p : ALL) {
            Piece piece = game.getPieces(p).get(0);
            int finish = game.getFinishSteps(p.color());
            int entry = game.getEntrySteps(p.color());

            for (int progress = 0; progress <= finish; progress++) {
                Piece.State st = progress >= finish ? Piece.State.FINISHED
                        : progress > entry ? Piece.State.HOME_PATH
                        : Piece.State.TRACK;
                piece.moveTo(progress, st);

                assertEquals(game.getBoardCell(piece),
                        game.getBoardCellAt(p.color(), progress));
            }
        }
    }

    @Test
    void everyStepOfEveryRollIsOneNeighbouringSquare() {
        GameState game = new GameState(ALL, seat(0));

        for (PlayerColor color : PlayerColor.values()) {
            for (int roll = 1; roll <= 6; roll++) {
                for (int from = 0; from + roll <= game.getFinishSteps(color); from++) {
                    int hops = 0;
                    for (int p = from; p < from + roll; p++) {
                        BoardCell a = game.getBoardCellAt(color, p).orElseThrow();
                        BoardCell b = game.getBoardCellAt(color, p + 1).orElseThrow();
                        assertTrue(Math.max(Math.abs(a.row() - b.row()),
                                Math.abs(a.col() - b.col())) == 1,
                                color + " hop " + p + "->" + (p + 1)
                                        + " must reach a neighbouring square");
                        hops++;
                    }
                    assertEquals(roll, hops);
                }
            }
        }
    }

    @Test
    void castleHasNoCellAndOutOfRangeProgressIsRejected() {
        GameState game = new GameState(ALL, seat(0));

        assertEquals(Optional.empty(),
                game.getBoardCellAt(PlayerColor.RED, -1));
        assertThrows(IllegalArgumentException.class,
                () -> game.getBoardCellAt(PlayerColor.RED, 57));
        assertThrows(IllegalArgumentException.class,
                () -> game.getBoardCellAt(null, 3));
    }
}
