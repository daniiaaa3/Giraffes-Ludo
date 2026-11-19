
package Model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.OptionalInt;

import org.junit.jupiter.api.Test;

import Model.DoubleBlockRules.MoveDecision;
import Model.DoubleBlockRules.MoveStatus;
import Model.DoubleBlockRules.PiecePosition;
import Model.EasyBoard.BoardCell;
import Model.GameSetup.PlayerColor;

public class DoubleBlockRulesTest {

    private final List<BoardCell> path = List.of(
            new BoardCell(0, 0),
            new BoardCell(0, 1),
            new BoardCell(0, 2),
            new BoardCell(0, 3),
            new BoardCell(0, 4),
            new BoardCell(0, 5),
            new BoardCell(0, 6)
    );

    private final BoardCell blockCell =
            new BoardCell(0, 3);

    private List<PiecePosition> blueBlock() {
        return List.of(
                new PiecePosition(
                        PlayerColor.BLUE, blockCell),
                new PiecePosition(
                        PlayerColor.BLUE, blockCell)
        );
    }

    @Test
    void testTwoPiecesCreateDoubleBlock() {

        assertTrue(DoubleBlockRules.hasDoubleBlock(
                blueBlock(),
                PlayerColor.BLUE,
                blockCell));
    }

    @Test
    void testOnePieceDoesNotCreateBlock() {

        List<PiecePosition> pieces = List.of(
                new PiecePosition(
                        PlayerColor.BLUE, blockCell));

        assertFalse(DoubleBlockRules.hasDoubleBlock(
                pieces,
                PlayerColor.BLUE,
                blockCell));
    }

    @Test
    void testDifferentPlayersDoNotCreateBlock() {

        List<PiecePosition> pieces = List.of(
                new PiecePosition(
                        PlayerColor.BLUE, blockCell),
                new PiecePosition(
                        PlayerColor.RED, blockCell));

        assertFalse(DoubleBlockRules.hasDoubleBlock(
                pieces,
                PlayerColor.BLUE,
                blockCell));
    }

    @Test
    void testThreePiecesAlsoCreateBlock() {

        List<PiecePosition> pieces = List.of(
                new PiecePosition(
                        PlayerColor.BLUE, blockCell),
                new PiecePosition(
                        PlayerColor.BLUE, blockCell),
                new PiecePosition(
                        PlayerColor.BLUE, blockCell));

        assertTrue(DoubleBlockRules.hasDoubleBlock(
                pieces,
                PlayerColor.BLUE,
                blockCell));
    }

    @Test
    void testOpponentBlockDetectedForward() {

        OptionalInt result =
                DoubleBlockRules.findFirstOpponentBlock(
                        path,
                        0,
                        6,
                        PlayerColor.RED,
                        blueBlock());

        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());
    }

    @Test
    void testOpponentBlockDetectedBackward() {

        OptionalInt result =
                DoubleBlockRules.findFirstOpponentBlock(
                        path,
                        6,
                        0,
                        PlayerColor.RED,
                        blueBlock());

        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());
    }

    @Test
    void testOwnBlockDoesNotPreventMovement() {

        List<PiecePosition> redBlock = List.of(
                new PiecePosition(
                        PlayerColor.RED, blockCell),
                new PiecePosition(
                        PlayerColor.RED, blockCell));

        MoveDecision result =
                DoubleBlockRules.evaluateMove(
                        path,
                        0,
                        6,
                        PlayerColor.RED,
                        redBlock,
                        false);

        assertEquals(MoveStatus.FULL_MOVE,
                result.status());

        assertEquals(6, result.destinationIndex());
    }

    @Test
    void testFullMoveWithoutBlock() {

        MoveDecision result =
                DoubleBlockRules.evaluateMove(
                        path,
                        0,
                        6,
                        PlayerColor.RED,
                        List.of(),
                        false);

        assertEquals(MoveStatus.FULL_MOVE,
                result.status());

        assertEquals(6, result.destinationIndex());
        assertEquals(0, result.unusedSteps());
    }

    @Test
    void testMustChooseAnotherPieceIfFullMoveExists() {

        MoveDecision result =
                DoubleBlockRules.evaluateMove(
                        path,
                        0,
                        6,
                        PlayerColor.RED,
                        blueBlock(),
                        true);

        assertEquals(
                MoveStatus.CHOOSE_OTHER_PIECE,
                result.status());

        assertEquals(0, result.destinationIndex());
        assertEquals(6, result.unusedSteps());
    }

    @Test
    void testStopBeforeOpponentBlock() {

        MoveDecision result =
                DoubleBlockRules.evaluateMove(
                        path,
                        0,
                        6,
                        PlayerColor.RED,
                        blueBlock(),
                        false);

        assertEquals(
                MoveStatus.STOP_BEFORE_BLOCK,
                result.status());

        assertEquals(2, result.destinationIndex());
        assertEquals(4, result.unusedSteps());
    }

    @Test
    void testStopBeforeBlockWhenMovingBackward() {

        MoveDecision result =
                DoubleBlockRules.evaluateMove(
                        path,
                        6,
                        0,
                        PlayerColor.RED,
                        blueBlock(),
                        false);

        assertEquals(
                MoveStatus.STOP_BEFORE_BLOCK,
                result.status());

        assertEquals(4, result.destinationIndex());
        assertEquals(4, result.unusedSteps());
    }

    @Test
    void testBlockImmediatelyNextToPiece() {

        BoardCell nextCell = new BoardCell(0, 1);

        List<PiecePosition> pieces = List.of(
                new PiecePosition(
                        PlayerColor.BLUE, nextCell),
                new PiecePosition(
                        PlayerColor.BLUE, nextCell));

        MoveDecision result =
                DoubleBlockRules.evaluateMove(
                        path,
                        0,
                        6,
                        PlayerColor.RED,
                        pieces,
                        false);

        assertEquals(
                MoveStatus.BLOCKED_IMMEDIATELY,
                result.status());

        assertEquals(0, result.destinationIndex());
        assertEquals(6, result.unusedSteps());
    }

    @Test
    void testOpponentBlockPreventsCastleRelease() {

        BoardCell start = new BoardCell(0, 0);

        List<PiecePosition> pieces = List.of(
                new PiecePosition(
                        PlayerColor.BLUE, start),
                new PiecePosition(
                        PlayerColor.BLUE, start));

        assertFalse(DoubleBlockRules.canReleaseOnStart(
                pieces,
                PlayerColor.RED,
                start));

        assertTrue(DoubleBlockRules.canReleaseOnStart(
                pieces,
                PlayerColor.BLUE,
                start));
    }

    @Test
    void testMustMoveFromOwnBlockWhenOnlyLegalOption() {

        List<PiecePosition> pieces = List.of(
                new PiecePosition(
                        PlayerColor.RED, blockCell),
                new PiecePosition(
                        PlayerColor.RED, blockCell));

        List<PiecePosition> legalMoves =
                List.of(pieces.get(0));

        assertTrue(
                DoubleBlockRules.mustMoveFromOwnBlock(
                        pieces,
                        PlayerColor.RED,
                        legalMoves));
    }

    @Test
    void testNotForcedToBreakBlockIfOtherPieceCanMove() {

        PiecePosition first =
                new PiecePosition(
                        PlayerColor.RED, blockCell);

        PiecePosition second =
                new PiecePosition(
                        PlayerColor.RED, blockCell);

        PiecePosition freePiece =
                new PiecePosition(
                        PlayerColor.RED,
                        new BoardCell(0, 1));

        List<PiecePosition> pieces =
                List.of(first, second, freePiece);

        List<PiecePosition> legalMoves =
                List.of(first, freePiece);

        assertFalse(
                DoubleBlockRules.mustMoveFromOwnBlock(
                        pieces,
                        PlayerColor.RED,
                        legalMoves));
    }

    @Test
    void testInvalidPathIndicesRejected() {

        assertThrows(
                IllegalArgumentException.class,
                () -> DoubleBlockRules.findFirstOpponentBlock(
                        path,
                        -1,
                        6,
                        PlayerColor.RED,
                        blueBlock()));
    }
}
