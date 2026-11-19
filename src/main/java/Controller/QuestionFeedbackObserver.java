
package Controller;

import java.util.Objects;
import java.util.function.Consumer;

import Model.QuestionObserver;
import Model.QuestionResultEvent;

/**
 * Concrete Observer that generates feedback
 * whenever a player answers an educational question.
 */
public final class QuestionFeedbackObserver
        implements QuestionObserver {

    private final Consumer<String> feedbackHandler;

    public QuestionFeedbackObserver(
            Consumer<String> feedbackHandler) {

        this.feedbackHandler =
                Objects.requireNonNull(feedbackHandler);
    }

    @Override
    public void onQuestionAnswered(
            QuestionResultEvent event) {

        Objects.requireNonNull(event);

        int steps = Math.abs(event.getMovementSteps());

        String unit = steps == 1
                ? " step" : " steps";

        String message;

        if (event.isCorrect()) {
            message = "Correct answer! Move forward "
                    + steps + unit + ".";
        } else {
            message = "Incorrect answer! Move backward "
                    + steps + unit + ".";
        }

        feedbackHandler.accept(message);
    }
}
