
package Model;

import java.util.Objects;
import java.util.Random;

/**
 * Represents the UltraBoost die used in Hard difficulty.
 *
 * Generates a random value between 1 and 6
 * with equal probability.
 *
 * MVC Component: Model
 * SRS Requirement: FR-4.2
 * Iteration: 2
 */
public final class UltraBoostDice {

    public static final int MIN_VALUE = 1;
    public static final int MAX_VALUE = 6;

    private final Random random;

    /**
     * Creates an UltraBoost die.
     */
    public UltraBoostDice() {
        this(new Random());
    }

    /**
     * Allows injecting Random for deterministic testing.
     */
    public UltraBoostDice(Random random) {
        this.random = Objects.requireNonNull(
                random, "Random cannot be null");
    }

    /**
     * Rolls the UltraBoost die.
     *
     * @return a value between 1 and 6
     */
    public int roll() {
        return random.nextInt(MAX_VALUE) + MIN_VALUE;
    }
}
