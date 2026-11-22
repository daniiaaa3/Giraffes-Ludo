
package Model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the UltraBoost die.
 *
 * SRS: FR-4.2
 * Iteration: 2
 */
public class UltraBoostDiceTest {

    @Test
    void rollAlwaysReturnsValueBetween1And6() {

        UltraBoostDice dice =
                new UltraBoostDice(new Random(42));

        for (int i = 0; i < 1000; i++) {

            int result = dice.roll();

            assertTrue(result >= 1 && result <= 6,
                    "UltraBoost result must be between 1 and 6");
        }
    }

    @Test
    void allSixValuesCanAppear() {

        UltraBoostDice dice =
                new UltraBoostDice(new Random(42));

        Set<Integer> results = new HashSet<>();

        for (int i = 0; i < 1000; i++) {
            results.add(dice.roll());
        }

        assertEquals(
                Set.of(1, 2, 3, 4, 5, 6),
                results);
    }

    @Test
    void nullRandomIsRejected() {

        assertThrows(NullPointerException.class,
                () -> new UltraBoostDice(null));
    }
}
