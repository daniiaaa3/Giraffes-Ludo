
package Model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the Boost die.
 *
 * Iteration: 2
 * SRS: FR-4.2
 */
public class BoostDiceTest {

    @Test
    void rollAlwaysReturnsValueBetween1And3() {

        BoostDice dice = new BoostDice(new Random(42));

        for (int i = 0; i < 1000; i++) {

            int result = dice.roll();

            assertTrue(result >= 1 && result <= 3,
                    "Boost die result must be between 1 and 3");
        }
    }

    @Test
    void allThreeValuesCanAppear() {

        BoostDice dice = new BoostDice(new Random(42));

        Set<Integer> results = new HashSet<>();

        for (int i = 0; i < 1000; i++) {
            results.add(dice.roll());
        }

        assertEquals(Set.of(1, 2, 3), results);
    }

    @Test
    void nullRandomIsRejected() {

        assertThrows(NullPointerException.class,
                () -> new BoostDice(null));
    }
}
