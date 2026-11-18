
package Model;

import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Handles random question selection and answer evaluation.
 */
public final class QuestionEngine {

    private final QuestionRepository repository;
    private final Random random;

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
     * Evaluates the player's selected answer.
     * Answer indices range from 0 to 3.
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
}
