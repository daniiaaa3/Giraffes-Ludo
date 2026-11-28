
package Controller;

import Model.Question;
import Model.QuestionDifficulty;
import Model.QuestionEngine;
import Model.QuestionRepository;
import Model.QuestionResultEvent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class QuestionControllerTest {

    private QuestionEngine engine;
    private QuestionController controller;

    @BeforeEach
    void setUp() {
        engine = new QuestionEngine(
                new QuestionRepository(), new Random(42));

        controller = new QuestionController(engine);
    }

    @Test
    void initiallyThereIsNoActiveQuestion() {
        assertFalse(controller.hasActiveQuestion());
        assertNull(controller.getActiveQuestion());
    }

    @Test
    void startsQuestionWithRequestedDifficulty() {
        for (QuestionDifficulty difficulty :
                QuestionDifficulty.values()) {

            Question question =
                    controller.startQuestion(difficulty);

            assertNotNull(question);
            assertEquals(difficulty,
                    question.getDifficulty());

            controller.submitAnswer(
                    question.getCorrectAnswerIndex());
        }
    }

    @Test
    void startingQuestionStoresItAsActive() {
        Question question = controller.startQuestion(
                QuestionDifficulty.EASY);

        assertTrue(controller.hasActiveQuestion());
        assertSame(question,
                controller.getActiveQuestion());
    }

    @Test
    void cannotStartAnotherQuestionWhileOneIsActive() {
        Question original = controller.startQuestion(
                QuestionDifficulty.EASY);

        assertThrows(IllegalStateException.class,
                () -> controller.startQuestion(
                        QuestionDifficulty.HARD));

        assertSame(original,
                controller.getActiveQuestion());
    }

    @Test
    void correctAnswerReturnsExpectedReward() {
        Question question = controller.startQuestion(
                QuestionDifficulty.MEDIUM);

        QuestionResultEvent result =
                controller.submitAnswer(
                        question.getCorrectAnswerIndex());

        assertTrue(result.isCorrect());
        assertEquals(2, result.getMovementSteps());
        assertSame(question, result.getQuestion());
    }

    @Test
    void incorrectAnswerReturnsExpectedPenalty() {
        Question question = controller.startQuestion(
                QuestionDifficulty.HARD);

        int wrongIndex =
                (question.getCorrectAnswerIndex() + 1) % 4;

        QuestionResultEvent result =
                controller.submitAnswer(wrongIndex);

        assertFalse(result.isCorrect());
        assertEquals(-3, result.getMovementSteps());
    }

    @Test
    void submittingAnswerClearsActiveQuestion() {
        Question question = controller.startQuestion(
                QuestionDifficulty.EASY);

        controller.submitAnswer(
                question.getCorrectAnswerIndex());

        assertFalse(controller.hasActiveQuestion());
        assertNull(controller.getActiveQuestion());
    }

    @Test
    void cannotSubmitWithoutActiveQuestion() {
        assertThrows(IllegalStateException.class,
                () -> controller.submitAnswer(0));
    }

    @Test
    void duplicateSubmissionIsRejected() {
        List<QuestionResultEvent> events =
                new ArrayList<>();

        engine.addObserver(events::add);

        Question question = controller.startQuestion(
                QuestionDifficulty.MEDIUM);

        int answer = question.getCorrectAnswerIndex();

        controller.submitAnswer(answer);

        assertThrows(IllegalStateException.class,
                () -> controller.submitAnswer(answer));

        assertEquals(1, events.size());
    }

    @Test
    void negativeAnswerIndexIsRejected() {
        Question question = controller.startQuestion(
                QuestionDifficulty.EASY);

        assertThrows(IllegalArgumentException.class,
                () -> controller.submitAnswer(-1));

        assertSame(question,
                controller.getActiveQuestion());
    }

    @Test
    void answerIndexAboveRangeIsRejected() {
        Question question = controller.startQuestion(
                QuestionDifficulty.EASY);

        assertThrows(IllegalArgumentException.class,
                () -> controller.submitAnswer(4));

        assertSame(question,
                controller.getActiveQuestion());
    }

    @Test
    void invalidAnswerDoesNotNotifyObservers() {
        List<QuestionResultEvent> events =
                new ArrayList<>();

        engine.addObserver(events::add);

        controller.startQuestion(
                QuestionDifficulty.MEDIUM);

        assertThrows(IllegalArgumentException.class,
                () -> controller.submitAnswer(-1));

        assertTrue(events.isEmpty());
        assertTrue(controller.hasActiveQuestion());
    }

    @Test
    void validSubmissionNotifiesObservers() {
        List<QuestionResultEvent> events =
                new ArrayList<>();

        engine.addObserver(events::add);

        Question question = controller.startQuestion(
                QuestionDifficulty.HARD);

        QuestionResultEvent result =
                controller.submitAnswer(
                        question.getCorrectAnswerIndex());

        assertEquals(1, events.size());
        assertSame(result, events.get(0));
    }

    @Test
    void canStartNewQuestionAfterPreviousAnswer() {
        Question first = controller.startQuestion(
                QuestionDifficulty.EASY);

        controller.submitAnswer(
                first.getCorrectAnswerIndex());

        Question second = controller.startQuestion(
                QuestionDifficulty.HARD);

        assertNotNull(second);
        assertTrue(controller.hasActiveQuestion());
        assertEquals(QuestionDifficulty.HARD,
                second.getDifficulty());
    }

    @Test
    void activeQuestionIsClearedBeforeNotification() {
        engine.addObserver(event -> {
            assertFalse(controller.hasActiveQuestion());
            assertNull(controller.getActiveQuestion());
        });

        Question question = controller.startQuestion(
                QuestionDifficulty.EASY);

        controller.submitAnswer(
                question.getCorrectAnswerIndex());
    }

    @Test
    void reentrantSubmissionIsRejected() {
        List<QuestionResultEvent> events =
                new ArrayList<>();

        engine.addObserver(event -> {
            events.add(event);

            assertThrows(IllegalStateException.class,
                    () -> controller.submitAnswer(0));
        });

        Question question = controller.startQuestion(
                QuestionDifficulty.MEDIUM);

        controller.submitAnswer(
                question.getCorrectAnswerIndex());

        assertEquals(1, events.size());
    }

    @Test
    void nullEngineIsRejected() {
        assertThrows(NullPointerException.class,
                () -> new QuestionController(null));
    }

    @Test
    void nullDifficultyIsRejected() {
        assertThrows(NullPointerException.class,
                () -> controller.startQuestion(null));

        assertFalse(controller.hasActiveQuestion());
    }
    

    @Test
    void cancellingQuestionClearsActiveQuestion() {
        controller.startQuestion(
                QuestionDifficulty.EASY);

        controller.cancelQuestion();

        assertFalse(controller.hasActiveQuestion());
        assertNull(controller.getActiveQuestion());
    }

    @Test
    void cancellationDoesNotNotifyObservers() {
        List<QuestionResultEvent> events =
                new ArrayList<>();

        engine.addObserver(events::add);

        controller.startQuestion(
                QuestionDifficulty.MEDIUM);

        controller.cancelQuestion();

        assertTrue(events.isEmpty());
    }

    @Test
    void cannotSubmitAnswerAfterCancellation() {
        Question question = controller.startQuestion(
                QuestionDifficulty.HARD);

        controller.cancelQuestion();

        assertThrows(IllegalStateException.class,
                () -> controller.submitAnswer(
                        question.getCorrectAnswerIndex()));
    }

    @Test
    void canStartNewQuestionAfterCancellation() {
        controller.startQuestion(
                QuestionDifficulty.EASY);

        controller.cancelQuestion();

        Question newQuestion = controller.startQuestion(
                QuestionDifficulty.HARD);

        assertNotNull(newQuestion);
        assertTrue(controller.hasActiveQuestion());

        assertEquals(QuestionDifficulty.HARD,
                newQuestion.getDifficulty());

        assertSame(newQuestion,
                controller.getActiveQuestion());
    }

    @Test
    void cancellationWithoutActiveQuestionIsSafe() {
        assertDoesNotThrow(
                () -> controller.cancelQuestion());

        assertFalse(controller.hasActiveQuestion());
    }

    @Test
    void repeatedCancellationIsSafe() {
        controller.startQuestion(
                QuestionDifficulty.MEDIUM);

        controller.cancelQuestion();

        assertDoesNotThrow(
                () -> controller.cancelQuestion());

        assertFalse(controller.hasActiveQuestion());
        assertNull(controller.getActiveQuestion());
    }

    @Test
    void observersStillWorkAfterCancellation() {
        List<QuestionResultEvent> events =
                new ArrayList<>();

        engine.addObserver(events::add);

        controller.startQuestion(
                QuestionDifficulty.EASY);

        controller.cancelQuestion();

        assertTrue(events.isEmpty());

        Question newQuestion = controller.startQuestion(
                QuestionDifficulty.HARD);

        QuestionResultEvent result =
                controller.submitAnswer(
                        newQuestion.getCorrectAnswerIndex());

        assertEquals(1, events.size());
        assertSame(result, events.get(0));
        assertTrue(result.isCorrect());
        assertEquals(3, result.getMovementSteps());
    }
    

    @Test
    void registeredObserverReceivesQuestionResult() {
        List<QuestionResultEvent> events =
                new ArrayList<>();

        controller.addObserver(events::add);

        Question question = controller.startQuestion(
                QuestionDifficulty.MEDIUM);

        QuestionResultEvent result =
                controller.submitAnswer(
                        question.getCorrectAnswerIndex());

        assertEquals(1, events.size());
        assertSame(result, events.get(0));
    }

    @Test
    void removedObserverDoesNotReceiveQuestionResult() {
        List<QuestionResultEvent> events =
                new ArrayList<>();

        Model.QuestionObserver observer = events::add;

        controller.addObserver(observer);
        controller.removeObserver(observer);

        Question question = controller.startQuestion(
                QuestionDifficulty.HARD);

        controller.submitAnswer(
                question.getCorrectAnswerIndex());

        assertTrue(events.isEmpty());
    }

}
