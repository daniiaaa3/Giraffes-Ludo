
package Model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the Factory Method Design Pattern.
 *
 * Verifies concrete product creation, dice behavior,
 * product reuse, and invalid factory inputs.
 *
 * Iteration: 2
 * Implemented by: Yara Jaber
 */
class SpecialDiceFactoryMethodTest {

    @Test
    void boostCreatorCreatesBoostDice() {
        SpecialDice dice =
                new BoostDiceCreator().createDice();

        assertInstanceOf(BoostDice.class, dice);
    }

    @Test
    void ultraBoostCreatorCreatesUltraBoostDice() {
        SpecialDice dice =
                new UltraBoostDiceCreator().createDice();

        assertInstanceOf(UltraBoostDice.class, dice);
    }

    @Test
    void boostCreatorRollsBetweenOneAndThree() {
        SpecialDiceCreator creator =
                new BoostDiceCreator(new Random(42));

        for (int i = 0; i < 200; i++) {
            int result = creator.roll();

            assertTrue(result >= 1 && result <= 3);
        }
    }

    @Test
    void ultraBoostCreatorRollsBetweenOneAndSix() {
        SpecialDiceCreator creator =
                new UltraBoostDiceCreator(new Random(42));

        for (int i = 0; i < 200; i++) {
            int result = creator.roll();

            assertTrue(result >= 1 && result <= 6);
        }
    }

    @Test
    void factoryPreservesSeededDiceBehavior() {
        BoostDice expected =
                new BoostDice(new Random(123));

        SpecialDiceCreator creator =
                new BoostDiceCreator(new Random(123));

        for (int i = 0; i < 20; i++) {
            assertEquals(expected.roll(), creator.roll());
        }
    }

    @Test
    void factoryCreatesProductOnlyOnce() {
        class CountingCreator
                extends SpecialDiceCreator {

            int creationCount = 0;

            @Override
            protected SpecialDice createDice() {
                creationCount++;
                return () -> 2;
            }
        }

        CountingCreator creator = new CountingCreator();

        assertEquals(2, creator.roll());
        assertEquals(2, creator.roll());
        assertEquals(2, creator.roll());

        assertEquals(1, creator.creationCount);
    }

    @Test
    void factoryRejectsNullProduct() {
        SpecialDiceCreator creator =
                new SpecialDiceCreator() {

                    @Override
                    protected SpecialDice createDice() {
                        return null;
                    }
                };

        assertThrows(
                NullPointerException.class,
                creator::roll);
    }

    @Test
    void creatorsRejectNullRandom() {
        assertThrows(
                NullPointerException.class,
                () -> new BoostDiceCreator(null));

        assertThrows(
                NullPointerException.class,
                () -> new UltraBoostDiceCreator(null));
    }
}
