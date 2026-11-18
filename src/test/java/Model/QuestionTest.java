
package Model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QuestionTest {

    private Question createQuestion() {
        return new Question(
                "Q01",
                "What does MVC stand for?",
                QuestionDifficulty.EASY,
                List.of(
                        "Model View Controller",
                        "Main Visual Component",
                        "Model Variable Class",
                        "Multiple View Control"
                ),
                0,
                "Course lecture"
        );
    }

    @Test
    void validQuestionIsCreatedCorrectly() {
        Question question = createQuestion();

        assertEquals("Q01", question.getId());
        assertEquals("What does MVC stand for?",
                question.getText());
        assertEquals(QuestionDifficulty.EASY,
                question.getDifficulty());
        assertEquals(4, question.getAnswers().size());
        assertEquals(0, question.getCorrectAnswerIndex());
        assertEquals("Course lecture", question.getSource());
    }

    @Test
    void correctAnswerReturnsTrue() {
        assertTrue(createQuestion().isCorrect(0));
    }

    @Test
    void incorrectAnswersReturnFalse() {
        Question question = createQuestion();

        assertFalse(question.isCorrect(1));
        assertFalse(question.isCorrect(2));
        assertFalse(question.isCorrect(3));
    }

    @Test
    void invalidAnswerIndicesReturnFalse() {
        Question question = createQuestion();

        assertFalse(question.isCorrect(-1));
        assertFalse(question.isCorrect(4));
    }

    @Test
    void emptyQuestionIdIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new Question(
                        "",
                        "Question text",
                        QuestionDifficulty.EASY,
                        List.of("A", "B", "C", "D"),
                        0,
                        "Source"
                ));
    }

    @Test
    void emptyQuestionTextIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new Question(
                        "Q01",
                        " ",
                        QuestionDifficulty.EASY,
                        List.of("A", "B", "C", "D"),
                        0,
                        "Source"
                ));
    }

    @Test
    void nullDifficultyIsRejected() {
        assertThrows(NullPointerException.class, () ->
                new Question(
                        "Q01",
                        "Question text",
                        null,
                        List.of("A", "B", "C", "D"),
                        0,
                        "Source"
                ));
    }

    @Test
    void fewerThanFourAnswersAreRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new Question(
                        "Q01",
                        "Question text",
                        QuestionDifficulty.EASY,
                        List.of("A", "B", "C"),
                        0,
                        "Source"
                ));
    }

    @Test
    void moreThanFourAnswersAreRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new Question(
                        "Q01",
                        "Question text",
                        QuestionDifficulty.EASY,
                        List.of("A", "B", "C", "D", "E"),
                        0,
                        "Source"
                ));
    }

    @Test
    void emptyAnswerIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new Question(
                        "Q01",
                        "Question text",
                        QuestionDifficulty.EASY,
                        List.of("A", "", "C", "D"),
                        0,
                        "Source"
                ));
    }

    @Test
    void invalidCorrectAnswerIndexIsRejected() {
        for (int index : new int[]{-1, 4}) {
            assertThrows(IllegalArgumentException.class, () ->
                    new Question(
                            "Q01",
                            "Question text",
                            QuestionDifficulty.EASY,
                            List.of("A", "B", "C", "D"),
                            index,
                            "Source"
                    ));
        }
    }

    @Test
    void emptySourceIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new Question(
                        "Q01",
                        "Question text",
                        QuestionDifficulty.EASY,
                        List.of("A", "B", "C", "D"),
                        0,
                        ""
                ));
    }

    @Test
    void answersCannotBeModified() {
        Question question = createQuestion();

        assertThrows(UnsupportedOperationException.class,
                () -> question.getAnswers().add("New answer"));
    }

    @Test
    void originalAnswerListCannotChangeQuestion() {
        List<String> originalAnswers =
                new ArrayList<>(List.of("A", "B", "C", "D"));

        Question question = new Question(
                "Q01",
                "Question text",
                QuestionDifficulty.EASY,
                originalAnswers,
                0,
                "Source"
        );

        originalAnswers.set(0, "Changed answer");

        assertEquals("A", question.getAnswers().get(0));
    }
}
