
package Model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import Model.GameSetup.PlayerColor;
import Model.Piece.State;

/**
 * Unit tests for Piece.
 *
 * SRS Requirements: FR-5.1, FR-5.2
 * Iteration: 1
 */
public class PieceTest {

    @Test
    void newPieceWaitsInCastle() {
        Piece piece = new Piece(PlayerColor.GREEN, 2);

        assertEquals(PlayerColor.GREEN, piece.getColor());
        assertEquals(2, piece.getId());
        assertEquals(State.CASTLE, piece.getState());
        assertEquals(Piece.CASTLE_PROGRESS, piece.getProgress());
        assertTrue(piece.isInCastle());
        assertFalse(piece.isFinished());
    }

    @Test
    void acceptsIdsZeroToThree() {
        for (int id = 0; id < 4; id++) {
            assertEquals(id,
                    new Piece(PlayerColor.RED, id).getId());
        }
    }

    @Test
    void rejectsInvalidIdsAndNullColor() {
        assertThrows(IllegalArgumentException.class,
                () -> new Piece(PlayerColor.RED, -1));
        assertThrows(IllegalArgumentException.class,
                () -> new Piece(PlayerColor.RED, 4));
        assertThrows(IllegalArgumentException.class,
                () -> new Piece(null, 0));
    }

    @Test
    void moveToUpdatesStateAndProgress() {
        Piece piece = new Piece(PlayerColor.RED, 0);

        piece.moveTo(0, State.TRACK);
        assertEquals(State.TRACK, piece.getState());
        assertEquals(0, piece.getProgress());
        assertFalse(piece.isInCastle());

        piece.moveTo(52, State.HOME_PATH);
        assertEquals(State.HOME_PATH, piece.getState());

        piece.moveTo(56, State.FINISHED);
        assertTrue(piece.isFinished());
    }

    @Test
    void inconsistentMoveIsRejectedAndKeepsPosition() {
        Piece piece = new Piece(PlayerColor.RED, 0);
        piece.moveTo(5, State.TRACK);

        // Castle needs progress -1; other states need >= 0.
        assertThrows(IllegalArgumentException.class,
                () -> piece.moveTo(3, State.CASTLE));
        assertThrows(IllegalArgumentException.class,
                () -> piece.moveTo(-1, State.TRACK));
        assertThrows(IllegalArgumentException.class,
                () -> piece.moveTo(-2, State.CASTLE));
        assertThrows(IllegalArgumentException.class,
                () -> piece.moveTo(4, null));

        assertEquals(5, piece.getProgress());
        assertEquals(State.TRACK, piece.getState());
    }

    @Test
    void toStringDescribesPiece() {
        String text = new Piece(PlayerColor.BLUE, 1).toString();

        assertTrue(text.contains("BLUE"));
        assertTrue(text.contains("CASTLE"));
    }
}
