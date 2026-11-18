
package Model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class QuestionRepositoryTest {

    private QuestionRepository repository;

    @BeforeEach
    void setUp() {
        repository = new QuestionRepository();
    }

    @Test
    void repositoryLoadsThirtyQuestions() {
        assertEquals(30, repository.size());
        assertEquals(30, repository.getAllQuestions().size());
    }

    @Test
    void repositoryContainsTenEasyQuestions() {
        assertEquals(10,
                repository.getQuestionsByDifficulty(
                        QuestionDifficulty.EASY).size());
    }

    @Test
    void repositoryContainsTenMediumQuestions() {
        assertEquals(10,
                repository.getQuestionsByDifficulty(
                        QuestionDifficulty.MEDIUM).size());
    }

    @Test
    void repositoryContainsTenHardQuestions() {
        assertEquals(10,
                repository.getQuestionsByDifficulty(
                        QuestionDifficulty.HARD).size());
    }

    @Test
    void difficultyFilteringReturnsMatchingQuestions() {
        for (QuestionDifficulty difficulty :
                QuestionDifficulty.values()) {

            List<Question> questions =
                    repository.getQuestionsByDifficulty(difficulty);

            for (Question question : questions) {
                assertEquals(difficulty, question.getDifficulty());
            }
        }
    }

    @Test
    void allQuestionsHaveUniqueIds() {
        Set<String> ids = new HashSet<>();

        for (Question question : repository.getAllQuestions()) {
            assertTrue(ids.add(question.getId()),
                    "Duplicate ID: " + question.getId());
        }
    }

    @Test
    void allQuestionsHaveFourAnswers() {
        for (Question question : repository.getAllQuestions()) {
            assertEquals(4, question.getAnswers().size(),
                    question.getId());
        }
    }

    @Test
    void allCorrectAnswerIndicesAreValid() {
        for (Question question : repository.getAllQuestions()) {
            int index = question.getCorrectAnswerIndex();

            assertTrue(index >= 0 && index < 4,
                    question.getId());
        }
    }

    @Test
    void allQuestionsHaveSources() {
        for (Question question : repository.getAllQuestions()) {
            assertNotNull(question.getSource());
            assertFalse(question.getSource().isBlank());
        }
    }

    @Test
    void questionBankCannotBeModifiedExternally() {
        assertThrows(UnsupportedOperationException.class,
                () -> repository.getAllQuestions().clear());
    }

    @Test
    void nullDifficultyIsRejected() {
        assertThrows(NullPointerException.class,
                () -> repository.getQuestionsByDifficulty(null));
    }
}
