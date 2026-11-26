
package Model;

import java.util.Objects;
import java.util.Random;

/**
 * Concrete Creator in the Factory Method Design Pattern.
 *
 * Creates UltraBoostDice objects for Hard difficulty.
 *
 * MVC Component: Model
 * Iteration: 2
 * Design Pattern: Factory Method
 * Implemented by: Yara Jaber
 */
public final class UltraBoostDiceCreator extends SpecialDiceCreator {

    private final Random random;

    /**
     * Creates an UltraBoostDiceCreator with a random generator.
     */
    public UltraBoostDiceCreator() {
        this(new Random());
    }

    /**
     * Allows injecting Random for deterministic testing.
     *
     * @param random random number generator
     */
    public UltraBoostDiceCreator(Random random) {
        this.random = Objects.requireNonNull(
                random, "Random cannot be null");
    }

    /**
     * Factory Method implementation.
     *
     * @return an UltraBoostDice instance
     */
    @Override
    protected SpecialDice createDice() {
        return new UltraBoostDice(random);
    }
}
