
package Model;

import java.util.Objects;
import java.util.Random;

/**
 * Concrete Creator in the Factory Method Design Pattern.
 *
 * Creates BoostDice objects for Medium difficulty.
 *
 * MVC Component: Model
 * Iteration: 2
 * Design Pattern: Factory Method
 * Implemented by: Yara Jaber
 */
public final class BoostDiceCreator extends SpecialDiceCreator {

    private final Random random;

    /**
     * Creates a BoostDiceCreator with a random generator.
     */
    public BoostDiceCreator() {
        this(new Random());
    }

    /**
     * Allows injecting Random for deterministic testing.
     *
     * @param random random number generator
     */
    public BoostDiceCreator(Random random) {
        this.random = Objects.requireNonNull(
                random, "Random cannot be null");
    }

    /**
     * Factory Method implementation.
     *
     * @return a BoostDice instance
     */
    @Override
    protected SpecialDice createDice() {
        return new BoostDice(random);
    }
}
