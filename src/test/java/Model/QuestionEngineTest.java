
package Model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class QuestionEngineTest {

    private QuestionRepository repository;
    private QuestionEngine engine;

    @BeforeEach
    void setUp() {
        repository = new QuestionRepository();
        engine = new QuestionEngine(
                repository, new Random(42));
    }

    @Test
    void selectsEasyQuestion() {
        Question question =
                engine.selectQuestion(QuestionDifficulty.EASY);

        assertNotNull(question);
        assertEquals(QuestionDifficulty.EASY,
                question.getDifficulty());
    }

    @Test
    void selectsMediumQuestion() {
        Question question =
                engine.selectQuestion(QuestionDifficulty.MEDIUM);

        assertNotNull(question);
        assertEquals(QuestionDifficulty.MEDIUM,
                question.getDifficulty());
    }

    @Test
    void selectsHardQuestion() {
        Question question =
                engine.selectQuestion(QuestionDifficulty.HARD);

        assertNotNull(question);
        assertEquals(QuestionDifficulty.HARD,
                question.getDifficulty());
    }

    @Test
    void repeatedSelectionRespectsDifficulty() {
        for (QuestionDifficulty difficulty :
                QuestionDifficulty.values()) {

            for (int i = 0; i < 100; i++) {
                Question question =
                        engine.selectQuestion(difficulty);

                assertEquals(difficulty,
                        question.getDifficulty());
            }
        }
    }

    @Test
    void selectedQuestionBelongsToRepository() {
        for (QuestionDifficulty difficulty :
                QuestionDifficulty.values()) {

            Question question =
                    engine.selectQuestion(difficulty);

            assertTrue(repository.getQuestionsByDifficulty(
                    difficulty).contains(question));
        }
    }

    @Test
    void randomSelectionProducesDifferentQuestions() {
        Set<String> selectedIds = new HashSet<>();

        for (int i = 0; i < 100; i++) {
            Question question =
                    engine.selectQuestion(
                            QuestionDifficulty.EASY);

            selectedIds.add(question.getId());
        }

        assertTrue(selectedIds.size() > 1);
    }

    @Test
    void sameRandomSeedProducesSameSequence() {
        QuestionEngine first = new QuestionEngine(
                repository, new Random(42));

        QuestionEngine second = new QuestionEngine(
                repository, new Random(42));

        for (int i = 0; i < 20; i++) {
            assertEquals(
                    first.selectQuestion(
                            QuestionDifficulty.MEDIUM).getId(),
                    second.selectQuestion(
                            QuestionDifficulty.MEDIUM).getId()
            );
        }
    }

    @Test
    void correctAnswerIsEvaluatedCorrectly() {
        for (Question question :
                repository.getAllQuestions()) {

            assertTrue(engine.evaluateAnswer(
                    question,
                    question.getCorrectAnswerIndex()));
        }
    }

    @Test
    void incorrectAnswersAreEvaluatedCorrectly() {
        for (Question question :
                repository.getAllQuestions()) {

            for (int i = 0; i < 4; i++) {
                if (i != question.getCorrectAnswerIndex()) {
                    assertFalse(
                            engine.evaluateAnswer(question, i));
                }
            }
        }
    }

    @Test
    void negativeAnswerIndexIsRejected() {
        Question question =
                engine.selectQuestion(QuestionDifficulty.EASY);

        assertThrows(IllegalArgumentException.class,
                () -> engine.evaluateAnswer(question, -1));
    }

    @Test
    void answerIndexAboveRangeIsRejected() {
        Question question =
                engine.selectQuestion(QuestionDifficulty.EASY);

        assertThrows(IllegalArgumentException.class,
                () -> engine.evaluateAnswer(question, 4));
    }

    @Test
    void nullQuestionIsRejected() {
        assertThrows(NullPointerException.class,
                () -> engine.evaluateAnswer(null, 0));
    }

    @Test
    void nullDifficultyIsRejected() {
        assertThrows(NullPointerException.class,
                () -> engine.selectQuestion(null));
    }

    @Test
    void nullRepositoryIsRejected() {
        assertThrows(NullPointerException.class,
                () -> new QuestionEngine(null));
    }

    @Test
    void nullRandomGeneratorIsRejected() {
        assertThrows(NullPointerException.class,
                () -> new QuestionEngine(repository, null));
    }

    @Test
    void defaultConstructorSupportsQuestionSelection() {
        QuestionEngine defaultEngine =
                new QuestionEngine(repository);

        assertNotNull(defaultEngine.selectQuestion(
                QuestionDifficulty.EASY));
    }
}
