
package Model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import Model.DoubleBlockRules.PiecePosition;
import Model.GameSetup.PlayerEntry;

/**
 * Connects the current GameState piece positions
 * to the DoubleBlockRules logic.
 *
 * MVC Component: Model
 * SRS Requirements: FR-5.5, FR-5.6
 * Iteration: 2
 */
public final class DoubleBlockGameAdapter {

    private DoubleBlockGameAdapter() {
        // Utility class.
    }

    /**
     * Creates a snapshot of all active pieces
     * currently positioned on the board.
     *
     * Castle and finished pieces are excluded.
     */
    public static List<PiecePosition> getBoardPositions(
            GameState game) {

        Objects.requireNonNull(game, "Game cannot be null");

        List<PiecePosition> positions = new ArrayList<>();

        for (PlayerEntry player : game.getPlayers()) {

            for (Piece piece : game.getPieces(player)) {

                if (piece.isInCastle() || piece.isFinished()) {
                    continue;
                }

                positions.add(new PiecePosition(
                        piece.getColor(),
                        game.getBoardCell(piece).orElseThrow()
                ));
            }
        }

        return List.copyOf(positions);
    }

/**
 * FR-5.6, FR-5.7:
 * Evaluates movement against Double Blocks
 * using the real game board and piece positions.
 *
 * @param game current game
 * @param piece moving piece
 * @param targetProgress intended destination
 * @param anotherPieceCanMakeFullMove whether another
 *        piece can complete its full movement
 * @return movement decision
 */
public static DoubleBlockRules.MoveDecision evaluateMovement(
        GameState game,
        Piece piece,
        int targetProgress,
        boolean anotherPieceCanMakeFullMove) {

    Objects.requireNonNull(game, "Game cannot be null");
    Objects.requireNonNull(piece, "Piece cannot be null");

    // Validate that the piece belongs to this game.
    game.getBoardCell(piece);

    if (piece.isInCastle() || piece.isFinished()) {
        throw new IllegalArgumentException(
                "Only an active board piece can move.");
    }

    return DoubleBlockRules.evaluateMove(
            game.getBoardTopology().getPlayerPath(
                    piece.getColor()),
            piece.getProgress(),
            targetProgress,
            piece.getColor(),
            getBoardPositions(game),
            anotherPieceCanMakeFullMove
    );
}

}
