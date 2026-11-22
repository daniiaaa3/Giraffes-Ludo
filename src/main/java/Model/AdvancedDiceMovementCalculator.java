package Model;

import java.util.Objects;

/**
 * Calculates movement distances from the dice for all difficulty levels.
 *
 * This class does not roll dice, change a turn or move a piece.
 * It only calculates distances so GameState can apply the movement rules.
 *
 * SRS: FR-4.1 through FR-4.6.
 */
public final class AdvancedDiceMovementCalculator {

    private static final int MIN_STEPS = 1;
    private static final int MAX_STEPS = 6;

    private AdvancedDiceMovementCalculator() {
        // Utility class: no instances.
    }

    /**
     * Calculates movement while the piece is on a shared track.
     *
     * Easy: Steps.
     * Medium: Steps * (Boost - 1), with Boost in [1, 3].
     * Hard: Steps * (UltraBoost - 1), with UltraBoost in [1, 6].
     *
     * For Easy, pass specialDieValue = 0 because no special die is rolled.
     */
    public static int calculateTrackSteps(
            GameDifficulty difficulty, int stepsDieValue, int specialDieValue) {

        Objects.requireNonNull(difficulty, "Difficulty cannot be null.");
        validateStepsDie(stepsDieValue);

        return switch (difficulty) {
            case EASY -> {
                if (specialDieValue != 0) {
                    throw new IllegalArgumentException(
                            "Easy does not use a special die; pass 0.");
                }
                yield stepsDieValue;
            }
            case MEDIUM -> {
                validateSpecialDie(specialDieValue, 3, "Boost");
                yield stepsDieValue * (specialDieValue - 1);
            }
            case HARD -> {
                validateSpecialDie(specialDieValue, 6, "UltraBoost");
                yield stepsDieValue * (specialDieValue - 1);
            }
        };
    }

    /**
     * A piece already on its personal home path moves by Steps only.
     * Boost and UltraBoost do not affect home-path movement (FR-4.5).
     */
    public static int calculateHomeSteps(int stepsDieValue) {
        validateStepsDie(stepsDieValue);
        return stepsDieValue;
    }

    /**
     * A piece can leave its castle only when the ordinary Steps die is 6.
     * This remains true even if the special die would yield 0 track steps.
     */
    public static boolean canReleaseFromCastle(int stepsDieValue) {
        validateStepsDie(stepsDieValue);
        return stepsDieValue == 6;
    }

    private static void validateStepsDie(int value) {
        if (value < MIN_STEPS || value > MAX_STEPS) {
            throw new IllegalArgumentException(
                    "Steps die must be between 1 and 6.");
        }
    }

    private static void validateSpecialDie(int value, int max, String name) {
        if (value < 1 || value > max) {
            throw new IllegalArgumentException(
                    name + " die must be between 1 and " + max + ".");
        }
    }
}
