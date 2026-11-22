package Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Unit tests for SRS FR-4.3 through FR-4.6. */
public class AdvancedDiceMovementCalculatorTest {

    @Test
    void easyUsesOnlyStepsDie() {
        assertEquals(4, AdvancedDiceMovementCalculator.calculateTrackSteps(
                GameDifficulty.EASY, 4, 0));
    }

    @Test
    void mediumBoostOneGivesZeroTrackSteps() {
        assertEquals(0, AdvancedDiceMovementCalculator.calculateTrackSteps(
                GameDifficulty.MEDIUM, 6, 1));
    }

    @Test
    void mediumBoostTwoUsesMultiplierOne() {
        assertEquals(5, AdvancedDiceMovementCalculator.calculateTrackSteps(
                GameDifficulty.MEDIUM, 5, 2));
    }

    @Test
    void mediumBoostThreeUsesMultiplierTwo() {
        assertEquals(10, AdvancedDiceMovementCalculator.calculateTrackSteps(
                GameDifficulty.MEDIUM, 5, 3));
    }

    @Test
    void hardUltraBoostOneGivesZeroTrackSteps() {
        assertEquals(0, AdvancedDiceMovementCalculator.calculateTrackSteps(
                GameDifficulty.HARD, 4, 1));
    }

    @Test
    void hardUltraBoostSixCanMoveThirtySteps() {
        assertEquals(30, AdvancedDiceMovementCalculator.calculateTrackSteps(
                GameDifficulty.HARD, 6, 6));
    }

    @Test
    void homePathUsesStepsDieOnly() {
        assertEquals(3, AdvancedDiceMovementCalculator.calculateHomeSteps(3));
    }

    @Test
    void castleReleaseRequiresOrdinarySix() {
        assertTrue(AdvancedDiceMovementCalculator.canReleaseFromCastle(6));
        assertFalse(AdvancedDiceMovementCalculator.canReleaseFromCastle(5));
        assertFalse(AdvancedDiceMovementCalculator.canReleaseFromCastle(1));
    }

    @Test
    void castleReleaseWorksEvenWhenBoostMultiplierIsZero() {
        assertEquals(0, AdvancedDiceMovementCalculator.calculateTrackSteps(
                GameDifficulty.MEDIUM, 6, 1));
        assertTrue(AdvancedDiceMovementCalculator.canReleaseFromCastle(6));
    }

    @Test
    void rejectsInvalidOrdinaryDieValues() {
        assertThrows(IllegalArgumentException.class,
                () -> AdvancedDiceMovementCalculator.calculateTrackSteps(
                        GameDifficulty.EASY, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> AdvancedDiceMovementCalculator.calculateHomeSteps(7));
        assertThrows(IllegalArgumentException.class,
                () -> AdvancedDiceMovementCalculator.canReleaseFromCastle(0));
    }

    @Test
    void rejectsBoostOutsideOneToThree() {
        assertThrows(IllegalArgumentException.class,
                () -> AdvancedDiceMovementCalculator.calculateTrackSteps(
                        GameDifficulty.MEDIUM, 4, 0));
        assertThrows(IllegalArgumentException.class,
                () -> AdvancedDiceMovementCalculator.calculateTrackSteps(
                        GameDifficulty.MEDIUM, 4, 4));
    }

    @Test
    void rejectsUltraBoostOutsideOneToSix() {
        assertThrows(IllegalArgumentException.class,
                () -> AdvancedDiceMovementCalculator.calculateTrackSteps(
                        GameDifficulty.HARD, 4, 0));
        assertThrows(IllegalArgumentException.class,
                () -> AdvancedDiceMovementCalculator.calculateTrackSteps(
                        GameDifficulty.HARD, 4, 7));
    }

    @Test
    void rejectsSpecialDieInEasy() {
        assertThrows(IllegalArgumentException.class,
                () -> AdvancedDiceMovementCalculator.calculateTrackSteps(
                        GameDifficulty.EASY, 4, 2));
    }

    @Test
    void rejectsNullDifficulty() {
        assertThrows(NullPointerException.class,
                () -> AdvancedDiceMovementCalculator.calculateTrackSteps(
                        null, 4, 2));
    }
}
