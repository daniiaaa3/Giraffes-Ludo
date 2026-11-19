
package Controller;

import Model.Question;
import Model.QuestionDifficulty;
import Model.QuestionEngine;
import Model.QuestionObserver;
import Model.QuestionRepository;
import Model.QuestionResultEvent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the complete educational
 * question subsystem.
 */
class QuestionWorkflowTest {

    private QuestionRepository repository;
    private QuestionEngine engine;
    private QuestionController controller;

    private List<String> feedbackMessages;
    private List<Integer> movementEffects;
    private List<QuestionResultEvent> results;

    @BeforeEach
    void setUp() {
        repository = new QuestionRepository();

        engine = new QuestionEngine(
                repository, new Random(42));

        controller = new QuestionController(engine);

        feedbackMessages = new ArrayList<>();
        movementEffects = new ArrayList<>();
        results = new ArrayList<>();

        engine.addObserver(
                new QuestionFeedbackObserver(
                        feedbackMessages::add));

        engine.addObserver(event ->
                movementEffects.add(
                        event.getMovementSteps()));

        engine.addObserver(results::add);
    }

    @Test
    void completeCorrectAnswerWorkflow() {
        Question question = controller.startQuestion(
                QuestionDifficulty.MEDIUM);

        assertTrue(repository.getAllQuestions()
                .contains(question));

        QuestionResultEvent result =
                controller.submitAnswer(
                        question.getCorrectAnswerIndex());

        assertTrue(result.isCorrect());
        assertFalse(controller.hasActiveQuestion());

        assertEquals(List.of(2), movementEffects);

        assertEquals(List.of(
                "Correct answer! Move forward 2 steps."),
                feedbackMessages);

        assertEquals(1, results.size());
        assertSame(result, results.get(0));
    }

    @Test
    void completeIncorrectAnswerWorkflow() {
        Question question = controller.startQuestion(
                QuestionDifficulty.HARD);

        int wrongIndex =
                (question.getCorrectAnswerIndex() + 1) % 4;

        QuestionResultEvent result =
                controller.submitAnswer(wrongIndex);

        assertFalse(result.isCorrect());
        assertFalse(controller.hasActiveQuestion());

        assertEquals(List.of(-3), movementEffects);

        assertEquals(List.of(
                "Incorrect answer! Move backward 3 steps."),
                feedbackMessages);

        assertEquals(1, results.size());
        assertSame(result, results.get(0));
    }

    @Test
    void consecutiveQuestionsProduceIndependentResults() {
        Question easy = controller.startQuestion(
                QuestionDifficulty.EASY);

        controller.submitAnswer(
                easy.getCorrectAnswerIndex());

        Question medium = controller.startQuestion(
                QuestionDifficulty.MEDIUM);

        controller.submitAnswer(
                (medium.getCorrectAnswerIndex() + 1) % 4);

        Question hard = controller.startQuestion(
                QuestionDifficulty.HARD);

        controller.submitAnswer(
                hard.getCorrectAnswerIndex());

        assertEquals(List.of(1, -2, 3), movementEffects);

        assertEquals(List.of(
                "Correct answer! Move forward 1 step.",
                "Incorrect answer! Move backward 2 steps.",
                "Correct answer! Move forward 3 steps."),
                feedbackMessages);

        assertEquals(3, results.size());
        assertFalse(controller.hasActiveQuestion());
    }

    @Test
    void cancelledQuestionProducesNoSideEffects() {
        controller.startQuestion(
                QuestionDifficulty.HARD);

        controller.cancelQuestion();

        assertTrue(feedbackMessages.isEmpty());
        assertTrue(movementEffects.isEmpty());
        assertTrue(results.isEmpty());

        assertFalse(controller.hasActiveQuestion());
    }

    @Test
    void duplicateSubmissionDoesNotDuplicateEffects() {
        Question question = controller.startQuestion(
                QuestionDifficulty.MEDIUM);

        int answer = question.getCorrectAnswerIndex();

        controller.submitAnswer(answer);

        assertThrows(IllegalStateException.class,
                () -> controller.submitAnswer(answer));

        assertEquals(List.of(2), movementEffects);
        assertEquals(1, feedbackMessages.size());
        assertEquals(1, results.size());
    }

    @Test
    void invalidAnswerCanBeFollowedByValidAnswer() {
        Question question = controller.startQuestion(
                QuestionDifficulty.EASY);

        assertThrows(IllegalArgumentException.class,
                () -> controller.submitAnswer(4));

        assertTrue(controller.hasActiveQuestion());
        assertTrue(feedbackMessages.isEmpty());
        assertTrue(movementEffects.isEmpty());
        assertTrue(results.isEmpty());

        controller.submitAnswer(
                question.getCorrectAnswerIndex());

        assertEquals(List.of(1), movementEffects);
        assertEquals(1, feedbackMessages.size());
        assertEquals(1, results.size());

        assertFalse(controller.hasActiveQuestion());
    }

    @Test
    void removingOneObserverDoesNotAffectOthers() {
        List<String> temporaryMessages =
                new ArrayList<>();

        QuestionObserver temporaryObserver =
                new QuestionFeedbackObserver(
                        temporaryMessages::add);

        engine.addObserver(temporaryObserver);

        Question first = controller.startQuestion(
                QuestionDifficulty.EASY);

        controller.submitAnswer(
                first.getCorrectAnswerIndex());

        engine.removeObserver(temporaryObserver);

        Question second = controller.startQuestion(
                QuestionDifficulty.HARD);

        controller.submitAnswer(
                second.getCorrectAnswerIndex());

        assertEquals(1, temporaryMessages.size());
        assertEquals(2, feedbackMessages.size());
        assertEquals(2, results.size());

        assertEquals(List.of(1, 3), movementEffects);
    }

    @Test
    void questionRecoveryAfterCancellationWorks() {
        controller.startQuestion(
                QuestionDifficulty.EASY);

        controller.cancelQuestion();

        Question question = controller.startQuestion(
                QuestionDifficulty.HARD);

        controller.submitAnswer(
                question.getCorrectAnswerIndex());

        assertEquals(List.of(3), movementEffects);
        assertEquals(1, feedbackMessages.size());
        assertEquals(1, results.size());

        assertFalse(controller.hasActiveQuestion());
    }
}
