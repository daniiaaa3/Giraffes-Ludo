
package Controller;

import Model.Question;
import Model.QuestionDifficulty;
import Model.QuestionEngine;
import Model.QuestionRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class QuestionFeedbackObserverTest {

    private QuestionEngine engine;
    private QuestionController controller;
    private QuestionFeedbackObserver observer;
    private List<String> messages;

    @BeforeEach
    void setUp() {
        engine = new QuestionEngine(
                new QuestionRepository(), new Random(42));

        controller = new QuestionController(engine);

        messages = new ArrayList<>();

        observer = new QuestionFeedbackObserver(
                messages::add);

        engine.addObserver(observer);
    }

    private void answerQuestion(
            QuestionDifficulty difficulty,
            boolean correct) {

        Question question =
                controller.startQuestion(difficulty);

        int index = question.getCorrectAnswerIndex();

        if (!correct) {
            index = (index + 1) % 4;
        }

        controller.submitAnswer(index);
    }

    @Test
    void correctEasyAnswerProducesFeedback() {
        answerQuestion(QuestionDifficulty.EASY, true);

        assertEquals(List.of(
                "Correct answer! Move forward 1 step."),
                messages);
    }

    @Test
    void incorrectEasyAnswerProducesFeedback() {
        answerQuestion(QuestionDifficulty.EASY, false);

        assertEquals(List.of(
                "Incorrect answer! Move backward 1 step."),
                messages);
    }

    @Test
    void correctMediumAnswerProducesFeedback() {
        answerQuestion(QuestionDifficulty.MEDIUM, true);

        assertEquals(List.of(
                "Correct answer! Move forward 2 steps."),
                messages);
    }

    @Test
    void incorrectMediumAnswerProducesFeedback() {
        answerQuestion(QuestionDifficulty.MEDIUM, false);

        assertEquals(List.of(
                "Incorrect answer! Move backward 2 steps."),
                messages);
    }

    @Test
    void correctHardAnswerProducesFeedback() {
        answerQuestion(QuestionDifficulty.HARD, true);

        assertEquals(List.of(
                "Correct answer! Move forward 3 steps."),
                messages);
    }

    @Test
    void incorrectHardAnswerProducesFeedback() {
        answerQuestion(QuestionDifficulty.HARD, false);

        assertEquals(List.of(
                "Incorrect answer! Move backward 3 steps."),
                messages);
    }

    @Test
    void cancellationProducesNoFeedback() {
        controller.startQuestion(
                QuestionDifficulty.EASY);

        controller.cancelQuestion();

        assertTrue(messages.isEmpty());
    }

    @Test
    void removedObserverProducesNoFeedback() {
        engine.removeObserver(observer);

        answerQuestion(QuestionDifficulty.MEDIUM, true);

        assertTrue(messages.isEmpty());
    }

    @Test
    void multipleAnswersProduceMultipleMessages() {
        answerQuestion(QuestionDifficulty.EASY, true);

        answerQuestion(QuestionDifficulty.HARD, false);

        assertEquals(List.of(
                "Correct answer! Move forward 1 step.",
                "Incorrect answer! Move backward 3 steps."),
                messages);
    }

    @Test
    void nullFeedbackHandlerIsRejected() {
        assertThrows(NullPointerException.class,
                () -> new QuestionFeedbackObserver(null));
    }

    @Test
    void nullEventIsRejected() {
        assertThrows(NullPointerException.class,
                () -> observer.onQuestionAnswered(null));
    }
}
