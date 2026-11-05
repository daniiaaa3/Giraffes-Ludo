
package Model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Model.GameSetup.PlayerColor;

class CastleReleaseRulesTest {

    private EasyBoard board;
    private CastleReleaseRules rules;

    @BeforeEach
    void setUp() {
        board = new EasyBoard();
        rules = new CastleReleaseRules(board);
    }

    @Test
    void releaseAllowedWhenSixAndInCastle() {
        assertTrue(rules.canRelease(6, true));
    }

    @Test
    void releaseNotAllowedWithoutSix() {
        assertFalse(rules.canRelease(3, true));
    }

    @Test
    void releaseNotAllowedIfPieceAlreadyOutside() {
        assertFalse(rules.canRelease(6, false));
    }

    @Test
    void invalidDiceValueRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> rules.canRelease(7, true));
    }

    @Test
    void releaseReturnsCorrectStartSquare() {
        int expected = board.getStartSquare(
                PlayerColor.RED);

        int actual = rules.getReleaseSquare(
                PlayerColor.RED, 6, true);

        assertEquals(expected, actual);
    }

    @Test
    void illegalReleaseRejected() {
        assertThrows(IllegalStateException.class,
                () -> rules.getReleaseSquare(
                        PlayerColor.RED, 4, true));
    }
}
