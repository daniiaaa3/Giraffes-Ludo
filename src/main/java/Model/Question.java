
package Model;

import java.util.List;
import java.util.Objects;

/**
 * Represents an educational multiple-choice question.
 */
public final class Question {

    private final String id;
    private final String text;
    private final QuestionDifficulty difficulty;
    private final List<String> answers;
    private final int correctAnswerIndex;
    private final String source;

    public Question(String id,
                    String text,
                    QuestionDifficulty difficulty,
                    List<String> answers,
                    int correctAnswerIndex,
                    String source) {

        this.id = requireText(id, "Question ID");
        this.text = requireText(text, "Question text");
        this.difficulty = Objects.requireNonNull(difficulty);

        if (answers == null || answers.size() != 4) {
            throw new IllegalArgumentException(
                    "A question must have exactly four answers.");
        }

        for (String answer : answers) {
            requireText(answer, "Answer");
        }

        if (correctAnswerIndex < 0 || correctAnswerIndex >= 4) {
            throw new IllegalArgumentException(
                    "Correct answer index must be between 0 and 3.");
        }

        this.answers = List.copyOf(answers);
        this.correctAnswerIndex = correctAnswerIndex;
        this.source = requireText(source, "Source");
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    field + " cannot be empty.");
        }

        return value.trim();
    }

    public String getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public QuestionDifficulty getDifficulty() {
        return difficulty;
    }

    public List<String> getAnswers() {
        return answers;
    }

    public int getCorrectAnswerIndex() {
        return correctAnswerIndex;
    }

    public String getSource() {
        return source;
    }

    public boolean isCorrect(int selectedAnswerIndex) {
        return selectedAnswerIndex == correctAnswerIndex;
    }
}
