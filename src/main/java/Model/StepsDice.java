
package Model;

import java.util.Random;

/**
 * Represents the regular Steps die used in Ludo.
 *
 * Rolls a random value between 1 and 6.
 *
 * MVC Component: Model
 * Iteration: 1
 */
public class StepsDice {

    private final Random random;

    public StepsDice() {
        this(new Random());
    }

    public StepsDice(Random random) {
        if (random == null) {
            throw new IllegalArgumentException(
                    "Random source cannot be null.");
        }

        this.random = random;
    }

    /**
     * Rolls the Steps die.
     *
     * @return a random integer between 1 and 6
     */
    public int roll() {
        return random.nextInt(6) + 1;
    }
}
