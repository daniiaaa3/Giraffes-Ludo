package Model;

import java.util.Objects;
import java.util.Optional;

import Model.EasyBoard.BoardCell;

/**
 * Resolves an educational question difficulty from a board landing square.
 * Uses the same topology that draws the question squares in AdvancedBoardView.
 */
public final class QuestionSquareResolver {

    private QuestionSquareResolver() {
        // Utility class.
    }

    /**
     * Returns the question difficulty at a cell, if that cell is a question square.
     */
    public static Optional<QuestionDifficulty> atCell(
            GameBoardTopology topology, BoardCell cell) {

        Objects.requireNonNull(topology, "Topology is required");
        if (cell == null) {
            return Optional.empty();
        }

        GameDifficulty squareLevel = topology.getQuestionSquares().get(cell);
        if (squareLevel == null) {
            return Optional.empty();
        }

        // GameBoardTopology currently represents question levels with
        // GameDifficulty, while the question bank expects QuestionDifficulty.
        return Optional.of(QuestionDifficulty.valueOf(squareLevel.name()));
    }

    /**
     * Returns the question difficulty where an owned piece currently stands.
     * Pieces in the castle or outside the board do not trigger questions.
     */
    public static Optional<QuestionDifficulty> forPiece(
            GameState gameState, Piece piece) {

        Objects.requireNonNull(gameState, "Game state is required");
        Objects.requireNonNull(piece, "Piece is required");

        return gameState.getBoardCell(piece)
                .flatMap(cell -> atCell(gameState.getBoardTopology(), cell));
    }
}
