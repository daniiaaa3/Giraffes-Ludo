
package Controller;

import java.util.Objects;

import Model.Question;
import Model.QuestionDifficulty;
import Model.QuestionEngine;
import Model.QuestionResultEvent;

/**
 * Coordinates educational question interactions
 * and prevents duplicate answer submissions.
 */
public final class QuestionController {

    private final QuestionEngine engine;

    private Question activeQuestion;

    public QuestionController(QuestionEngine engine) {
        this.engine = Objects.requireNonNull(engine);
    }

    /**
     * Starts a new question interaction.
     */
    public Question startQuestion(
            QuestionDifficulty difficulty) {

        if (hasActiveQuestion()) {
            throw new IllegalStateException(
                    "A question is already active.");
        }

        activeQuestion = engine.selectQuestion(difficulty);

        return activeQuestion;
    }

    /**
     * Returns the currently active question.
     */
    public Question getActiveQuestion() {
        return activeQuestion;
    }

    /**
     * Checks whether a question is awaiting an answer.
     */
    public boolean hasActiveQuestion() {
        return activeQuestion != null;
    }

    /**
     * Submits an answer to the active question.
     */
    public QuestionResultEvent submitAnswer(
            int selectedAnswerIndex) {

        if (!hasActiveQuestion()) {
            throw new IllegalStateException(
                    "There is no active question.");
        }

        if (selectedAnswerIndex < 0
                || selectedAnswerIndex >= 4) {

            throw new IllegalArgumentException(
                    "Answer index must be between 0 and 3.");
        }

        Question answeredQuestion = activeQuestion;

        // Clear before notifying observers to prevent
        // duplicate or reentrant answer submissions.
        activeQuestion = null;

        return engine.submitAnswer(
                answeredQuestion, selectedAnswerIndex);
    }
    

    /**
     * Cancels the active question interaction without
     * evaluating an answer or notifying observers.
     *
     * Used when the current game is abandoned or reset.
     */
    public void cancelQuestion() {
        activeQuestion = null;
    }

}
