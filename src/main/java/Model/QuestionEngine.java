
package Model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Handles random question selection, answer evaluation
 * and notification of registered question observers.
 *
 * Subject in the Observer Design Pattern.
 */
public final class QuestionEngine {

    private final QuestionRepository repository;
    private final Random random;

    private final List<QuestionObserver> observers =
            new ArrayList<>();

    public QuestionEngine(QuestionRepository repository) {
        this(repository, new Random());
    }

    public QuestionEngine(QuestionRepository repository,
                          Random random) {

        this.repository = Objects.requireNonNull(repository);
        this.random = Objects.requireNonNull(random);
    }

    /**
     * Selects a random question of the requested difficulty.
     */
    public Question selectQuestion(
            QuestionDifficulty difficulty) {

        Objects.requireNonNull(difficulty);

        List<Question> availableQuestions =
                repository.getQuestionsByDifficulty(difficulty);

        if (availableQuestions.isEmpty()) {
            throw new IllegalStateException(
                    "No questions available for difficulty: "
                            + difficulty);
        }

        int index = random.nextInt(availableQuestions.size());

        return availableQuestions.get(index);
    }

    /**
     * Evaluates an answer without notifying observers.
     */
    public boolean evaluateAnswer(Question question,
                                  int selectedAnswerIndex) {

        Objects.requireNonNull(question);

        if (selectedAnswerIndex < 0
                || selectedAnswerIndex >= 4) {

            throw new IllegalArgumentException(
                    "Answer index must be between 0 and 3.");
        }

        return question.isCorrect(selectedAnswerIndex);
    }

    /**
     * Registers an observer for question result events.
     */
    public void addObserver(QuestionObserver observer) {

        Objects.requireNonNull(observer);

        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    /**
     * Unregisters an observer.
     */
    public void removeObserver(QuestionObserver observer) {

        observers.remove(Objects.requireNonNull(observer));
    }

    /**
     * Notifies all registered observers.
     */
    private void notifyObservers(QuestionResultEvent event) {

        for (QuestionObserver observer :
                List.copyOf(observers)) {

            observer.onQuestionAnswered(event);
        }
    }

    /**
     * Processes a submitted answer, calculates its movement
     * effect and notifies registered observers.
     */
    public QuestionResultEvent submitAnswer(
            Question question, int selectedAnswerIndex) {

        QuestionResultEvent event =
                new QuestionResultEvent(
                        question, selectedAnswerIndex);

        notifyObservers(event);

        return event;
    }
}
