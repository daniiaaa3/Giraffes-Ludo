
package Model;

/**
 * Common interface for special dice used in
 * Medium and Hard difficulty.
 *
 * Factory Method Design Pattern: Product.
 *
 * MVC Component: Model
 * Iteration: 2
 */
public interface SpecialDice {

    /**
     * Rolls the special die.
     *
     * @return the rolled value
     */
    int roll();
}
