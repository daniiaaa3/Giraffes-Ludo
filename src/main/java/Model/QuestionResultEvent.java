
package Model;

import java.util.Objects;

/**
 * Represents the result of an answered educational question.
 * This event is sent to registered question observers.
 */
public final class QuestionResultEvent {

    private final Question question;
    private final int selectedAnswerIndex;
    private final boolean correct;
    private final int movementSteps;

    public QuestionResultEvent(Question question,
                               int selectedAnswerIndex) {

        this.question = Objects.requireNonNull(question);

        if (selectedAnswerIndex < 0
                || selectedAnswerIndex >= 4) {

            throw new IllegalArgumentException(
                    "Answer index must be between 0 and 3.");
        }

        this.selectedAnswerIndex = selectedAnswerIndex;

        this.correct =
                question.isCorrect(selectedAnswerIndex);

        this.movementSteps =
                QuestionEffect.calculateSteps(
                        question.getDifficulty(), correct);
    }

    public Question getQuestion() {
        return question;
    }

    public int getSelectedAnswerIndex() {
        return selectedAnswerIndex;
    }

    public boolean isCorrect() {
        return correct;
    }

    public int getMovementSteps() {
        return movementSteps;
    }
}
