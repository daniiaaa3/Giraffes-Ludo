
package Model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QuestionEffectTest {

    @Test
    void correctEasyAnswerMovesOneStepForward() {
        assertEquals(1, QuestionEffect.calculateSteps(
                QuestionDifficulty.EASY, true));
    }

    @Test
    void incorrectEasyAnswerMovesOneStepBackward() {
        assertEquals(-1, QuestionEffect.calculateSteps(
                QuestionDifficulty.EASY, false));
    }

    @Test
    void correctMediumAnswerMovesTwoStepsForward() {
        assertEquals(2, QuestionEffect.calculateSteps(
                QuestionDifficulty.MEDIUM, true));
    }

    @Test
    void incorrectMediumAnswerMovesTwoStepsBackward() {
        assertEquals(-2, QuestionEffect.calculateSteps(
                QuestionDifficulty.MEDIUM, false));
    }

    @Test
    void correctHardAnswerMovesThreeStepsForward() {
        assertEquals(3, QuestionEffect.calculateSteps(
                QuestionDifficulty.HARD, true));
    }

    @Test
    void incorrectHardAnswerMovesThreeStepsBackward() {
        assertEquals(-3, QuestionEffect.calculateSteps(
                QuestionDifficulty.HARD, false));
    }

    @Test
    void nullDifficultyIsRejected() {
        assertThrows(NullPointerException.class,
                () -> QuestionEffect.calculateSteps(
                        null, true));
    }
}
