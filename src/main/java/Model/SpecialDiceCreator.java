
package Model;

import java.util.Objects;

/**
 * Abstract Creator in the Factory Method Design Pattern.
 *
 * Defines a factory method for creating a special die,
 * while allowing subclasses to decide which concrete
 * die implementation to instantiate.
 *
 * MVC Component: Model
 * Iteration: 2
 * Design Pattern: Factory Method
 * Implemented by: Yara Jaber
 */
public abstract class SpecialDiceCreator {

    private SpecialDice dice;

    /**
     * Factory Method.
     *
     * Each concrete creator determines which
     * SpecialDice implementation to instantiate.
     *
     * @return a special die
     */
    protected abstract SpecialDice createDice();

    /**
     * Rolls the special die created by the factory.
     *
     * The die is created once and reused for
     * subsequent rolls.
     *
     * @return the rolled value
     */
    public final int roll() {
        if (dice == null) {
            dice = Objects.requireNonNull(
                    createDice(),
                    "Factory cannot return a null die");
        }

        return dice.roll();
    }
}
