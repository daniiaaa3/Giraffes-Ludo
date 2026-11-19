
package Model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import Model.EasyBoard.BoardCell;
import Model.SpecialSquarePlacementStrategy.Placement;
import Model.SpecialSquarePlacementStrategy.QuestionDifficulty;

public class SpecialSquarePlacementStrategyTest {

    private SpecialSquarePlacementStrategy strategy() {
        return new RandomSpecialSquarePlacementStrategy(
                new Random(42));
    }

    @Test
    void testMediumPlacementCounts() {

        MediumBoard board = new MediumBoard(new Random(42));

        assertEquals(8, board.getSafeSquares().size());
        assertEquals(6, board.getQuestionSquares().size());
        assertEquals(2, board.getSurpriseSquares().size());
    }

    @Test
    void testHardPlacementCounts() {

        HardBoard board = new HardBoard(new Random(42));

        assertEquals(12, board.getSafeSquares().size());
        assertEquals(9, board.getQuestionSquares().size());
        assertEquals(3, board.getSurpriseSquares().size());
    }

    @Test
    void testNoOverlapBetweenSpecialSquares() {

        MediumBoard medium = new MediumBoard(new Random(42));
        HardBoard hard = new HardBoard(new Random(42));

        checkNoOverlap(
                medium.getSafeSquares(),
                medium.getQuestionSquares().keySet(),
                medium.getSurpriseSquares());

        checkNoOverlap(
                hard.getSafeSquares(),
                hard.getQuestionSquares().keySet(),
                hard.getSurpriseSquares());
    }

    @Test
    void testFixedSquaresAreExcluded() {

        List<BoardCell> track = List.of(
                new BoardCell(0, 0),
                new BoardCell(0, 1),
                new BoardCell(0, 2),
                new BoardCell(0, 3),
                new BoardCell(0, 4),
                new BoardCell(0, 5));

        BoardCell fixed = track.get(0);

        Placement result = strategy().place(
                track,
                Set.of(fixed),
                1,
                1,
                1);

        assertFalse(result.safeSquares().contains(fixed));
        assertFalse(result.questionSquares().containsKey(fixed));
        assertFalse(result.surpriseSquares().contains(fixed));
    }

    @Test
    void testQuestionDifficultyDistribution() {

        MediumBoard medium = new MediumBoard(new Random(42));
        HardBoard hard = new HardBoard(new Random(42));

        for (MediumBoard.QuestionLevel level
                : MediumBoard.QuestionLevel.values()) {

            assertEquals(2,
                    medium.getQuestionSquares()
                            .values()
                            .stream()
                            .filter(value -> value == level)
                            .count());
        }

        for (HardBoard.QuestionLevel level
                : HardBoard.QuestionLevel.values()) {

            assertEquals(3,
                    hard.getQuestionSquares()
                            .values()
                            .stream()
                            .filter(value -> value == level)
                            .count());
        }
    }

    @Test
    void testSameSeedProducesSamePlacement() {

        MediumBoard first = new MediumBoard(new Random(42));
        MediumBoard second = new MediumBoard(new Random(42));

        assertEquals(
                first.getSafeSquares(),
                second.getSafeSquares());

        assertEquals(
                first.getQuestionSquares(),
                second.getQuestionSquares());

        assertEquals(
                first.getSurpriseSquares(),
                second.getSurpriseSquares());
    }

    @Test
    void testInvalidPlacementRejected() {

        List<BoardCell> track = List.of(
                new BoardCell(0, 0),
                new BoardCell(0, 1));

        assertThrows(IllegalArgumentException.class,
                () -> strategy().place(
                        track, Set.of(), -1, 0, 0));

        assertThrows(IllegalArgumentException.class,
                () -> strategy().place(
                        track, Set.of(), 3, 0, 0));

        assertThrows(IllegalArgumentException.class,
                () -> strategy().place(
                        List.of(track.get(0), track.get(0)),
                        Set.of(), 1, 0, 0));
    }

    @Test
    void testAlternativeStrategyCanBeInjected() {

        AtomicInteger calls = new AtomicInteger();

        // Alternative deterministic Strategy:
        // selects squares in their original order
        // instead of shuffling them.
        SpecialSquarePlacementStrategy orderedStrategy =
                (track, fixed, safeCount,
                        questionsPerDifficulty, surpriseCount) -> {

                    calls.incrementAndGet();

                    List<BoardCell> available =
                            new ArrayList<>(track);

                    available.removeAll(fixed);

                    int index = 0;

                    Set<BoardCell> safe = new HashSet<>();

                    for (int i = 0; i < safeCount; i++) {
                        safe.add(available.get(index++));
                    }

                    Map<BoardCell, QuestionDifficulty> questions =
                            new LinkedHashMap<>();

                    for (QuestionDifficulty level
                            : QuestionDifficulty.values()) {

                        for (int i = 0;
                                i < questionsPerDifficulty;
                                i++) {

                            questions.put(
                                    available.get(index++),
                                    level);
                        }
                    }

                    Set<BoardCell> surprise = new HashSet<>();

                    for (int i = 0; i < surpriseCount; i++) {
                        surprise.add(available.get(index++));
                    }

                    return new Placement(
                            safe, questions, surprise);
                };

        MediumBoard medium = new MediumBoard(orderedStrategy);
        HardBoard hard = new HardBoard(orderedStrategy);

        assertEquals(2, calls.get());

        assertEquals(8, medium.getSafeSquares().size());
        assertEquals(12, hard.getSafeSquares().size());

        assertEquals(6, medium.getQuestionSquares().size());
        assertEquals(9, hard.getQuestionSquares().size());
    }

    private void checkNoOverlap(
            Set<BoardCell> safe,
            Set<BoardCell> questions,
            Set<BoardCell> surprise) {

        Set<BoardCell> combined = new HashSet<>();

        combined.addAll(safe);
        combined.addAll(questions);
        combined.addAll(surprise);

        assertEquals(
                safe.size() + questions.size() + surprise.size(),
                combined.size());
    }
}
