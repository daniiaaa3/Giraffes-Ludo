
package Model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class QuestionObserverTest {

    private QuestionEngine engine;
    private Question question;

    @BeforeEach
    void setUp() {
        engine = new QuestionEngine(
                new QuestionRepository(), new Random(42));

        question = engine.selectQuestion(
                QuestionDifficulty.MEDIUM);
    }

    @Test
    void registeredObserverReceivesNotification() {
        List<QuestionResultEvent> events = new ArrayList<>();

        engine.addObserver(events::add);

        engine.submitAnswer(
                question, question.getCorrectAnswerIndex());

        assertEquals(1, events.size());
    }

    @Test
    void multipleObserversReceiveSameEvent() {
        List<QuestionResultEvent> first = new ArrayList<>();
        List<QuestionResultEvent> second = new ArrayList<>();

        engine.addObserver(first::add);
        engine.addObserver(second::add);

        QuestionResultEvent result = engine.submitAnswer(
                question, question.getCorrectAnswerIndex());

        assertEquals(1, first.size());
        assertEquals(1, second.size());

        assertSame(result, first.get(0));
        assertSame(result, second.get(0));
    }

    @Test
    void removedObserverReceivesNoNotification() {
        List<QuestionResultEvent> events = new ArrayList<>();

        QuestionObserver observer = events::add;

        engine.addObserver(observer);
        engine.removeObserver(observer);

        engine.submitAnswer(
                question, question.getCorrectAnswerIndex());

        assertTrue(events.isEmpty());
    }

    @Test
    void duplicateObserverIsRegisteredOnlyOnce() {
        List<QuestionResultEvent> events = new ArrayList<>();

        QuestionObserver observer = events::add;

        engine.addObserver(observer);
        engine.addObserver(observer);

        engine.submitAnswer(
                question, question.getCorrectAnswerIndex());

        assertEquals(1, events.size());
    }

    @Test
    void correctAnswerEventContainsExpectedInformation() {
        int correctIndex = question.getCorrectAnswerIndex();

        QuestionResultEvent event =
                engine.submitAnswer(question, correctIndex);

        assertSame(question, event.getQuestion());
        assertEquals(correctIndex,
                event.getSelectedAnswerIndex());
        assertTrue(event.isCorrect());
        assertEquals(2, event.getMovementSteps());
    }

    @Test
    void incorrectAnswerEventContainsExpectedPenalty() {
        int wrongIndex =
                (question.getCorrectAnswerIndex() + 1) % 4;

        QuestionResultEvent event =
                engine.submitAnswer(question, wrongIndex);

        assertFalse(event.isCorrect());
        assertEquals(wrongIndex,
                event.getSelectedAnswerIndex());
        assertEquals(-2, event.getMovementSteps());
    }

    @Test
    void submissionWorksWithoutObservers() {
        QuestionResultEvent event = assertDoesNotThrow(
                () -> engine.submitAnswer(
                        question,
                        question.getCorrectAnswerIndex()));

        assertNotNull(event);
        assertTrue(event.isCorrect());
    }

    @Test
    void evaluationDoesNotNotifyObservers() {
        List<QuestionResultEvent> events = new ArrayList<>();

        engine.addObserver(events::add);

        engine.evaluateAnswer(
                question, question.getCorrectAnswerIndex());

        assertTrue(events.isEmpty());
    }

    @Test
    void invalidAnswerDoesNotNotifyObservers() {
        List<QuestionResultEvent> events = new ArrayList<>();

        engine.addObserver(events::add);

        assertThrows(IllegalArgumentException.class,
                () -> engine.submitAnswer(question, -1));

        assertThrows(IllegalArgumentException.class,
                () -> engine.submitAnswer(question, 4));

        assertTrue(events.isEmpty());
    }

    @Test
    void nullQuestionDoesNotNotifyObservers() {
        List<QuestionResultEvent> events = new ArrayList<>();

        engine.addObserver(events::add);

        assertThrows(NullPointerException.class,
                () -> engine.submitAnswer(null, 0));

        assertTrue(events.isEmpty());
    }

    @Test
    void nullObserverRegistrationIsRejected() {
        assertThrows(NullPointerException.class,
                () -> engine.addObserver(null));
    }

    @Test
    void nullObserverRemovalIsRejected() {
        assertThrows(NullPointerException.class,
                () -> engine.removeObserver(null));
    }

    @Test
    void observerCanUnregisterDuringNotification() {
        List<QuestionResultEvent> events = new ArrayList<>();

        QuestionObserver observer = new QuestionObserver() {

            @Override
            public void onQuestionAnswered(
                    QuestionResultEvent event) {

                events.add(event);
                engine.removeObserver(this);
            }
        };

        engine.addObserver(observer);

        engine.submitAnswer(
                question, question.getCorrectAnswerIndex());

        engine.submitAnswer(
                question, question.getCorrectAnswerIndex());

        assertEquals(1, events.size());
    }

    @Test
    void remainingObserverStillReceivesNotifications() {
        List<QuestionResultEvent> first = new ArrayList<>();
        List<QuestionResultEvent> second = new ArrayList<>();

        QuestionObserver firstObserver = first::add;
        QuestionObserver secondObserver = second::add;

        engine.addObserver(firstObserver);
        engine.addObserver(secondObserver);

        engine.removeObserver(firstObserver);

        engine.submitAnswer(
                question, question.getCorrectAnswerIndex());

        assertTrue(first.isEmpty());
        assertEquals(1, second.size());
    }

    @Test
    void eventMovementMatchesEveryDifficulty() {
        for (QuestionDifficulty difficulty :
                QuestionDifficulty.values()) {

            Question selected = engine.selectQuestion(difficulty);

            int correctIndex = selected.getCorrectAnswerIndex();
            int wrongIndex = (correctIndex + 1) % 4;

            int expectedSteps = switch (difficulty) {
                case EASY -> 1;
                case MEDIUM -> 2;
                case HARD -> 3;
            };

            QuestionResultEvent correctEvent =
                    engine.submitAnswer(selected, correctIndex);

            QuestionResultEvent wrongEvent =
                    engine.submitAnswer(selected, wrongIndex);

            assertEquals(expectedSteps,
                    correctEvent.getMovementSteps());

            assertEquals(-expectedSteps,
                    wrongEvent.getMovementSteps());
        }
    }
}
