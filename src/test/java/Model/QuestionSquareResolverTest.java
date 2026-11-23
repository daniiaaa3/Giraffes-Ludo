package Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Checks that detection matches the question squares used by the board UI. */
class QuestionSquareResolverTest {

    @Test
    void easyBoardHasNoQuestionSquares() {
        GameBoardTopology topology = new GameBoardTopology(GameDifficulty.EASY);
        assertTrue(topology.getQuestionSquares().isEmpty());
    }

    @Test
    void mediumQuestionSquaresResolveToTheirOwnDifficulty() {
        verifyQuestionSquares(GameDifficulty.MEDIUM);
    }

    @Test
    void hardQuestionSquaresResolveToTheirOwnDifficulty() {
        verifyQuestionSquares(GameDifficulty.HARD);
    }

    private static void verifyQuestionSquares(GameDifficulty difficulty) {
        GameBoardTopology topology = new GameBoardTopology(difficulty);
        assertFalse(topology.getQuestionSquares().isEmpty());

        topology.getQuestionSquares().forEach((cell, squareLevel) -> {
            QuestionDifficulty expected =
                    QuestionDifficulty.valueOf(squareLevel.name());
            assertEquals(expected,
                    QuestionSquareResolver.atCell(topology, cell).orElseThrow());
        });

        assertTrue(QuestionSquareResolver.atCell(topology, null).isEmpty());
    }
}
