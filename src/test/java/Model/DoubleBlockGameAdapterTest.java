
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
}
