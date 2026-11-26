
package Model;

import java.util.Objects;
import java.util.Random;

/**
 * Represents the Boost die used in Medium difficulty.
 *
 * The Boost die generates a random value between 1 and 3.
 * Each value has an equal probability.
 *
 * Factory Method Design Pattern:
 * Concrete Product implementing SpecialDice.
 *
 * The movement multiplier is calculated separately
 * according to the game rules.
 *
 * MVC Component: Model
 * SRS Requirement: FR-4.2
 * Iteration: 2
 */
public final class BoostDice implements SpecialDice {

    public static final int MIN_VALUE = 1;
    public static final int MAX_VALUE = 3;

    private final Random random;

    /**
     * Creates a Boost die with a random generator.
     */
    public BoostDice() {
        this(new Random());
    }

    /**
     * Allows injecting a Random instance for testing.
     *
     * @param random random number generator
     */
    public BoostDice(Random random) {
        this.random = Objects.requireNonNull(
                random, "Random cannot be null");
    }

    /**
     * Rolls the Boost die.
     *
     * @return an integer between 1 and 3
     */
    @Override
    public int roll() {
        return random.nextInt(MAX_VALUE) + MIN_VALUE;
    }
}
