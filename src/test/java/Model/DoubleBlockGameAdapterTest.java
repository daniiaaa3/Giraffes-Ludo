
package Model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import Model.DoubleBlockRules.PiecePosition;
import Model.GameSetup.PlayerColor;
import Model.GameSetup.PlayerEntry;
import Model.Piece.State;

public class DoubleBlockGameAdapterTest {

    private GameState createGame() {
        return new GameState(
            List.of(
                new PlayerEntry("Red", PlayerColor.RED),
                new PlayerEntry("Blue", PlayerColor.BLUE)
            ),
            new Random(1)
        );
    }

    @Test
    void testCastleAndFinishedPiecesAreExcluded() {
        GameState game = createGame();

        Piece red = game.getPieces(PlayerColor.RED).get(0);
        Piece blue = game.getPieces(PlayerColor.BLUE).get(0);

        // Put one red piece on its start square.
        red.moveTo(0, State.TRACK);

        // Mark one blue piece as finished.
        blue.moveTo(
            game.getFinishSteps(PlayerColor.BLUE),
            State.FINISHED
        );

        List<PiecePosition> positions =
            DoubleBlockGameAdapter.getBoardPositions(game);

        // Only the active red piece should be included.
        assertEquals(1, positions.size());
        assertEquals(PlayerColor.RED, positions.get(0).owner());
        assertEquals(
            game.getBoardCell(red).orElseThrow(),
            positions.get(0).cell()
        );
    }

    @Test
    void testDetectsDoubleBlockFromGameState() {
        GameState game = createGame();

        Piece first = game.getPieces(PlayerColor.RED).get(0);
        Piece second = game.getPieces(PlayerColor.RED).get(1);

        // Two red pieces occupy the same start square.
        first.moveTo(0, State.TRACK);
        second.moveTo(0, State.TRACK);

        List<PiecePosition> positions =
            DoubleBlockGameAdapter.getBoardPositions(game);

        assertTrue(DoubleBlockRules.hasDoubleBlock(
            positions,
            PlayerColor.RED,
            game.getBoardCell(first).orElseThrow()
        ));
    }

@Test
void testOpponentDoubleBlockPreventsCastleRelease() {
    GameState game = createGame();

    PlayerEntry redPlayer =
            new PlayerEntry("Red", PlayerColor.RED);

    // Find RED's starting square.
    var redStart = game.getBoardCellAt(
            PlayerColor.RED, 0).orElseThrow();

    // Find the same square on BLUE's movement path.
    int blueProgress = game.getBoardTopology()
            .getPlayerPath(PlayerColor.BLUE)
            .indexOf(redStart);

    assertTrue(blueProgress >= 0);
    assertTrue(blueProgress <=
            game.getEntrySteps(PlayerColor.BLUE));

    Piece blue1 =
            game.getPieces(PlayerColor.BLUE).get(0);
    Piece blue2 =
            game.getPieces(PlayerColor.BLUE).get(1);

    // Two BLUE pieces form a Double Block
    // on RED's starting square.
    blue1.moveTo(blueProgress, State.TRACK);
    blue2.moveTo(blueProgress, State.TRACK);

    // RED rolls 6 but cannot leave the castle.
    assertTrue(game.getLegalPieces(redPlayer, 6).isEmpty());

    // Remove one BLUE piece: the block disappears.
    blue2.moveTo(Piece.CASTLE_PROGRESS, State.CASTLE);

    // RED can release its four castle pieces again.
    assertEquals(4, game.getLegalPieces(redPlayer, 6).size());
}

@Test
void testStopsBeforeOpponentDoubleBlock() {

    GameState game = createGame();

    Piece red =
            game.getPieces(PlayerColor.RED).get(0);

    Piece blue1 =
            game.getPieces(PlayerColor.BLUE).get(0);
    Piece blue2 =
            game.getPieces(PlayerColor.BLUE).get(1);

    // RED starts at progress 0.
    red.moveTo(0, State.TRACK);

    // Choose a square three steps ahead of RED.
    var blockCell = game.getBoardCellAt(
            PlayerColor.RED, 3).orElseThrow();

    // Find the same square on BLUE's path.
    int blueProgress = game.getBoardTopology()
            .getPlayerPath(PlayerColor.BLUE)
            .indexOf(blockCell);

    assertTrue(blueProgress >= 0);
    assertTrue(blueProgress <=
            game.getEntrySteps(PlayerColor.BLUE));

    // Two BLUE pieces create a Double Block.
    blue1.moveTo(blueProgress, State.TRACK);
    blue2.moveTo(blueProgress, State.TRACK);

    // RED attempts to move six steps.
    var decision =
            DoubleBlockGameAdapter.evaluateMovement(
                    game,
                    red,
                    6,
                    false
            );

    // RED must stop at progress 2,
    // immediately before BLUE's block.
    assertEquals(
            DoubleBlockRules.MoveStatus.STOP_BEFORE_BLOCK,
            decision.status()
    );

    assertEquals(2, decision.destinationIndex());
    assertEquals(4, decision.unusedSteps());
}

@Test
void testFullMoveDetectionWithOpponentBlock() {

    GameState game = createGame();

    Piece red = game.getPieces(
            PlayerColor.RED).get(0);

    Piece blue1 = game.getPieces(
            PlayerColor.BLUE).get(0);

    Piece blue2 = game.getPieces(
            PlayerColor.BLUE).get(1);

    red.moveTo(0, State.TRACK);

    var blockCell = game.getBoardCellAt(
            PlayerColor.RED, 3).orElseThrow();

    int blueProgress = game.getBoardTopology()
            .getPlayerPath(PlayerColor.BLUE)
            .indexOf(blockCell);

    assertTrue(blueProgress >= 0);
    assertTrue(blueProgress <=
            game.getEntrySteps(PlayerColor.BLUE));

    blue1.moveTo(blueProgress, State.TRACK);
    blue2.moveTo(blueProgress, State.TRACK);

    // RED cannot complete a six-step move.
    assertFalse(game.canCompleteFullMove(red, 6, 0));

    // Removing one BLUE piece removes the block.
    blue2.moveTo(Piece.CASTLE_PROGRESS, State.CASTLE);

    // RED can now complete the full movement.
    assertTrue(game.canCompleteFullMove(red, 6, 0));
}

@Test
void testFullMoveHasPriorityOverBlockedMove() {

    GameState game = createGame();

    PlayerEntry redPlayer =
            new PlayerEntry("Red", PlayerColor.RED);

    Piece blockedRed =
            game.getPieces(PlayerColor.RED).get(0);

    Piece freeRed =
            game.getPieces(PlayerColor.RED).get(1);

    Piece blue1 =
            game.getPieces(PlayerColor.BLUE).get(0);

    Piece blue2 =
            game.getPieces(PlayerColor.BLUE).get(1);

    // First red piece will encounter a block.
    blockedRed.moveTo(0, State.TRACK);

    // Second red piece has a clear path.
    freeRed.moveTo(10, State.TRACK);

    // Create a BLUE Double Block at RED progress 3.
    var blockCell = game.getBoardCellAt(
            PlayerColor.RED, 3).orElseThrow();

    int blueProgress = game.getBoardTopology()
            .getPlayerPath(PlayerColor.BLUE)
            .indexOf(blockCell);

    assertTrue(blueProgress >= 0);
    assertTrue(blueProgress <=
            game.getEntrySteps(PlayerColor.BLUE));

    blue1.moveTo(blueProgress, State.TRACK);
    blue2.moveTo(blueProgress, State.TRACK);

    // RED rolls 5.
    List<Piece> legal =
            game.getLegalPieces(redPlayer, 5);

    // Only the piece with a full move is selectable.
    assertEquals(List.of(freeRed), legal);
}

@Test
void testActualMoveStopsBeforeOpponentDoubleBlock() {

    GameState game = createGame();

    PlayerEntry currentPlayer = game.getActivePlayer();

    PlayerColor opponentColor =
            currentPlayer.color() == PlayerColor.RED
                    ? PlayerColor.BLUE
                    : PlayerColor.RED;

    Piece movingPiece =
            game.getPieces(currentPlayer).get(0);

    movingPiece.moveTo(0, State.TRACK);

    // Place an opponent Double Block at progress 3.
    var blockCell = game.getBoardCellAt(
            currentPlayer.color(), 3).orElseThrow();

    int opponentProgress = game.getBoardTopology()
            .getPlayerPath(opponentColor)
            .indexOf(blockCell);

    assertTrue(opponentProgress >= 0);
    assertTrue(opponentProgress <=
            game.getEntrySteps(opponentColor));

    var opponentPieces = game.getPieces(opponentColor);

    opponentPieces.get(0).moveTo(
            opponentProgress, State.TRACK);
    opponentPieces.get(1).moveTo(
            opponentProgress, State.TRACK);

    // Roll 5 and perform an actual game move.
    assertEquals(
            TurnRollRules.Outcome.MOVE_REQUIRED,
            game.recordRoll(currentPlayer, 5)
    );

    assertTrue(
            game.getSelectablePieces().contains(movingPiece)
    );

    game.movePiece(currentPlayer, movingPiece);

    // Must stop immediately before the block.
    assertEquals(2, movingPiece.getProgress());
}

@Test
void testBackwardMoveStopsBeforeOpponentDoubleBlock() {

    GameState game = createGame();

    Piece red = game.getPieces(PlayerColor.RED).get(0);
    Piece blue1 = game.getPieces(PlayerColor.BLUE).get(0);
    Piece blue2 = game.getPieces(PlayerColor.BLUE).get(1);

    // RED starts at progress 6.
    red.moveTo(6, State.TRACK);

    // BLUE creates a Double Block at RED progress 3.
    var blockCell = game.getBoardCellAt(
            PlayerColor.RED, 3).orElseThrow();

    int blueProgress = game.getBoardTopology()
            .getPlayerPath(PlayerColor.BLUE)
            .indexOf(blockCell);

    assertTrue(blueProgress >= 0);
    assertTrue(blueProgress <=
            game.getEntrySteps(PlayerColor.BLUE));

    blue1.moveTo(blueProgress, State.TRACK);
    blue2.moveTo(blueProgress, State.TRACK);

    // RED tries to move backward by 5 steps.
    game.moveBackward(red, 5);

    // Must stop at 4, before the block at 3.
    assertEquals(4, red.getProgress());
}

@Test
void testQuestionBonusStopsBeforeOpponentDoubleBlock() {

    GameState game = createGame();

    Piece red = game.getPieces(PlayerColor.RED).get(0);
    Piece blue1 = game.getPieces(PlayerColor.BLUE).get(0);
    Piece blue2 = game.getPieces(PlayerColor.BLUE).get(1);

    // RED is on the board.
    red.moveTo(0, State.TRACK);

    // Create a BLUE Double Block at RED progress 3.
    var blockCell = game.getBoardCellAt(
            PlayerColor.RED, 3).orElseThrow();

    int blueProgress = game.getBoardTopology()
            .getPlayerPath(PlayerColor.BLUE)
            .indexOf(blockCell);

    assertTrue(blueProgress >= 0);
    assertTrue(blueProgress <=
            game.getEntrySteps(PlayerColor.BLUE));

    blue1.moveTo(blueProgress, State.TRACK);
    blue2.moveTo(blueProgress, State.TRACK);

    // A correct answer gives RED 3 forward steps.
    game.applyQuestionEffect(red, 3);

    // RED must stop before the Double Block.
    assertEquals(2, red.getProgress());
}


}
