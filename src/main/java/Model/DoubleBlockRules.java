
package Model;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;

import Model.EasyBoard.BoardCell;
import Model.GameSetup.PlayerColor;

/**
 * Handles Double Block rules.
 *
 * FR-5.5 - Detect Double Blocks.
 * FR-5.6 - Prevent opponents from crossing blocks.
 * FR-5.7 - Stop before a block when no full move exists.
 * FR-5.8 - Allow breaking own blocks.
 *
 * This class does not modify GameState.
 */
public final class DoubleBlockRules {

    private DoubleBlockRules() {
    }

    /**
     * Represents a piece currently on the board.
     * Pieces inside castles are not included.
     */
    public record PiecePosition(
            PlayerColor owner,
            BoardCell cell) {

        public PiecePosition {
            Objects.requireNonNull(owner, "owner");
            Objects.requireNonNull(cell, "cell");
        }
    }

    public enum MoveStatus {
        FULL_MOVE,
        CHOOSE_OTHER_PIECE,
        STOP_BEFORE_BLOCK,
        BLOCKED_IMMEDIATELY
    }

    /**
     * destinationIndex is an index in the
     * moving player's path.
     */
    public record MoveDecision(
            MoveStatus status,
            int destinationIndex,
            int unusedSteps) {
    }

    /**
     * FR-5.5:
     * Two or more pieces of the same player
     * on the same square form a Double Block.
     */
    public static boolean hasDoubleBlock(
            Collection<PiecePosition> pieces,
            PlayerColor owner,
            BoardCell cell) {

        Objects.requireNonNull(pieces, "pieces");
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(cell, "cell");

        int count = 0;

        for (PiecePosition piece : pieces) {

            if (piece.owner() == owner
                    && piece.cell().equals(cell)) {

                count++;

                if (count >= 2) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * FR-5.6:
     * Checks whether an opponent has a
     * Double Block on the given square.
     *
     * The owner may pass their own block.
     */
    public static boolean hasOpponentBlock(
            Collection<PiecePosition> pieces,
            PlayerColor movingPlayer,
            BoardCell cell) {

        Objects.requireNonNull(
                movingPlayer, "movingPlayer");

        for (PlayerColor other : PlayerColor.values()) {

            if (other != movingPlayer
                    && hasDoubleBlock(pieces, other, cell)) {

                return true;
            }
        }

        return false;
    }

    /**
     * FR-5.6:
     * An opposing Double Block on a starting
     * square prevents release from the castle.
     */
    public static boolean canReleaseOnStart(
            Collection<PiecePosition> pieces,
            PlayerColor movingPlayer,
            BoardCell startSquare) {

        return !hasOpponentBlock(
                pieces, movingPlayer, startSquare);
    }

    /**
     * Finds the first opposing Double Block
     * encountered along the requested movement.
     *
     * Supports forward and backward movement.
     * The current square is excluded.
     */
    public static OptionalInt findFirstOpponentBlock(
            List<BoardCell> playerPath,
            int fromIndex,
            int toIndex,
            PlayerColor movingPlayer,
            Collection<PiecePosition> pieces) {

        Objects.requireNonNull(
                playerPath, "playerPath");

        Objects.requireNonNull(
                movingPlayer, "movingPlayer");

        Objects.requireNonNull(pieces, "pieces");

        if (fromIndex < 0
                || fromIndex >= playerPath.size()
                || toIndex < 0
                || toIndex >= playerPath.size()) {

            throw new IllegalArgumentException(
                    "Path indices are out of range");
        }

        int direction =
                Integer.compare(toIndex, fromIndex);

        for (int i = fromIndex + direction;
                direction != 0
                && (direction > 0
                        ? i <= toIndex
                        : i >= toIndex);
                i += direction) {

            BoardCell cell = playerPath.get(i);

            if (hasOpponentBlock(
                    pieces, movingPlayer, cell)) {

                return OptionalInt.of(i);
            }
        }

        return OptionalInt.empty();
    }

    /**
     * FR-5.7:
     *
     * If there is no opposing Double Block,
     * the requested movement is allowed by
     * the Double Block rules.
     *
     * If another piece can complete a full
     * legal move, that piece must be chosen.
     *
     * Otherwise, stop immediately before
     * the opposing Double Block.
     *
     * The caller must calculate whether
     * another piece can make a full legal move.
     */
    public static MoveDecision evaluateMove(
            List<BoardCell> playerPath,
            int fromIndex,
            int toIndex,
            PlayerColor movingPlayer,
            Collection<PiecePosition> pieces,
            boolean anotherPieceCanMakeFullMove) {

        OptionalInt blocked =
                findFirstOpponentBlock(
                        playerPath,
                        fromIndex,
                        toIndex,
                        movingPlayer,
                        pieces);

        if (blocked.isEmpty()) {

            return new MoveDecision(
                    MoveStatus.FULL_MOVE,
                    toIndex,
                    0);
        }

        int totalSteps =
                Math.abs(toIndex - fromIndex);

        if (anotherPieceCanMakeFullMove) {

            return new MoveDecision(
                    MoveStatus.CHOOSE_OTHER_PIECE,
                    fromIndex,
                    totalSteps);
        }

        int direction =
                Integer.compare(toIndex, fromIndex);

        int stopIndex =
                blocked.getAsInt() - direction;

        if (stopIndex == fromIndex) {

            return new MoveDecision(
                    MoveStatus.BLOCKED_IMMEDIATELY,
                    fromIndex,
                    totalSteps);
        }

        return new MoveDecision(
                MoveStatus.STOP_BEFORE_BLOCK,
                stopIndex,
                Math.abs(toIndex - stopIndex));
    }

    /**
     * FR-5.8:
     *
     * When no piece outside the player's
     * own Double Blocks has a legal move,
     * a legal piece from an own block
     * must be selected.
     *
     * piecesWithLegalMoves must contain
     * only pieces with legal moves.
     */
    public static boolean mustMoveFromOwnBlock(
            Collection<PiecePosition> allPieces,
            PlayerColor player,
            Collection<PiecePosition> piecesWithLegalMoves) {

        Objects.requireNonNull(
                allPieces, "allPieces");

        Objects.requireNonNull(player, "player");

        Objects.requireNonNull(
                piecesWithLegalMoves,
                "piecesWithLegalMoves");

        boolean blockedPieceCanMove = false;

        for (PiecePosition piece : piecesWithLegalMoves) {

            if (piece.owner() != player) {
                continue;
            }

            if (hasDoubleBlock(
                    allPieces, player, piece.cell())) {

                blockedPieceCanMove = true;

            } else {
                // Another legal piece exists.
                return false;
            }
        }

        return blockedPieceCanMove;
    }
}
