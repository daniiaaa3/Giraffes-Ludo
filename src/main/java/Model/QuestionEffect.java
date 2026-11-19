
package Model;

import java.util.Objects;

/**
 * Calculates movement rewards and penalties
 * based on question difficulty and answer correctness.
 */
public final class QuestionEffect {

    private QuestionEffect() {
    }

    /**
     * Returns positive steps for a correct answer
     * and negative steps for an incorrect answer.
     */
    public static int calculateSteps(
            QuestionDifficulty difficulty,
            boolean correct) {

        Objects.requireNonNull(difficulty);

        int steps = switch (difficulty) {
            case EASY -> 1;
            case MEDIUM -> 2;
            case HARD -> 3;
        };

        return correct ? steps : -steps;
    }
}
