
package Model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

import Model.EasyBoard.BoardCell;

/**
 * Random implementation of the special-square
 * placement Strategy.
 *
 * FR-2.6:
 * Special squares are randomly positioned at
 * the beginning of each game.
 *
 * Start squares and arrows remain fixed.
 */
public final class RandomSpecialSquarePlacementStrategy
        implements SpecialSquarePlacementStrategy {

    private final Random random;

    /**
     * Used during normal gameplay.
     */
    public RandomSpecialSquarePlacementStrategy() {
        this(new Random());
    }

    /**
     * Allows reproducible placement in JUnit tests.
     */
    public RandomSpecialSquarePlacementStrategy(
            Random random) {

        this.random = Objects.requireNonNull(
                random, "Random cannot be null");
    }

    @Override
    public Placement place(
            List<BoardCell> sharedTrack,
            Set<BoardCell> fixedSquares,
            int safeCount,
            int questionsPerDifficulty,
            int surpriseCount) {

        Objects.requireNonNull(
                sharedTrack, "Shared track cannot be null");

        Objects.requireNonNull(
                fixedSquares, "Fixed squares cannot be null");

        if (safeCount < 0
                || questionsPerDifficulty < 0
                || surpriseCount < 0) {

            throw new IllegalArgumentException(
                    "Square counts cannot be negative");
        }

        // Prevent duplicate board coordinates.
        if (new HashSet<>(sharedTrack).size()
                != sharedTrack.size()) {

            throw new IllegalArgumentException(
                    "Shared track contains duplicate cells");
        }

        // Only ordinary shared-track squares
        // can be selected.
        List<BoardCell> available =
                new ArrayList<>(sharedTrack);

        // Exclude starts and arrows.
        available.removeAll(fixedSquares);

        long required =
                (long) safeCount
                + 3L * questionsPerDifficulty
                + surpriseCount;

        if (required > available.size()) {

            throw new IllegalArgumentException(
                    "Not enough available squares");
        }

        // Randomize once per placement.
        Collections.shuffle(available, random);

        int index = 0;

        // Safe squares.
        Set<BoardCell> safeSquares =
                new HashSet<>();

        for (int i = 0; i < safeCount; i++) {
            safeSquares.add(available.get(index++));
        }

        // Question squares.
        // Equal number for each difficulty.
        Map<BoardCell, QuestionDifficulty>
                questionSquares = new LinkedHashMap<>();

        for (QuestionDifficulty difficulty
                : QuestionDifficulty.values()) {

            for (int i = 0;
                    i < questionsPerDifficulty;
                    i++) {

                BoardCell cell =
                        available.get(index++);

                questionSquares.put(
                        cell, difficulty);
            }
        }

        // Surprise squares.
        Set<BoardCell> surpriseSquares =
                new HashSet<>();

        for (int i = 0; i < surpriseCount; i++) {
            surpriseSquares.add(
                    available.get(index++));
        }

        return new Placement(
                safeSquares,
                questionSquares,
                surpriseSquares);
    }
}
