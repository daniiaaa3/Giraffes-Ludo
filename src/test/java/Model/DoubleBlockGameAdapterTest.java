
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

}
