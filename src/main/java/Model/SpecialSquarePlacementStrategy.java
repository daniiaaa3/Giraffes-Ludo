
package Model;

import java.util.List;
import java.util.Map;
import java.util.Set;

import Model.EasyBoard.BoardCell;

/**
 * Strategy Pattern:
 * Defines how special squares are placed
 * on a Ludo board.
 *
 * SRS: FR-2.6
 */
public interface SpecialSquarePlacementStrategy {

    enum QuestionDifficulty {
        EASY,
        MEDIUM,
        HARD
    }

    record Placement(
            Set<BoardCell> safeSquares,
            Map<BoardCell, QuestionDifficulty> questionSquares,
            Set<BoardCell> surpriseSquares) {

        public Placement {
            safeSquares = Set.copyOf(safeSquares);
            questionSquares = Map.copyOf(questionSquares);
            surpriseSquares = Set.copyOf(surpriseSquares);
        }
    }

    Placement place(
            List<BoardCell> sharedTrack,
            Set<BoardCell> fixedSquares,
            int safeCount,
            int questionsPerDifficulty,
            int surpriseCount);
}
