package Model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import Model.GameSetup.PlayerColor;
import Model.GameSetup.PlayerEntry;
import Model.TurnRollRules.Outcome;

/** Integration checks for Iteration 2 advanced dice and movement rules. */
public class AdvancedDiceGameStateIntegrationTest {

    private static GameState game(GameDifficulty difficulty) {
        List<PlayerEntry> players = List.of(
                new PlayerEntry("Red", PlayerColor.RED),
                new PlayerEntry("Blue", PlayerColor.BLUE));
        return GameState.forDifficulty(players, new Random(42), difficulty);
    }

    private static Piece release(GameState game,
            PlayerEntry player, int specialValue) {
        assertEquals(Outcome.MOVE_REQUIRED,
                game.recordAdvancedRoll(player, 6, specialValue));
        Piece piece = game.getSelectablePieces().get(0);
        assertEquals(Outcome.BONUS_ROLL, game.movePiece(player, piece));
        assertEquals(0, piece.getProgress());
        assertEquals(0, game.getPendingRoll());
        return piece;
    }

    @Test
    void mediumReleaseWorksEvenWithZeroMultiplier() {
        GameState game = game(GameDifficulty.MEDIUM);
        PlayerEntry active = game.getActivePlayer();
        Piece released = release(game, active, 1);
        assertFalse(released.isInCastle());
    }

    @Test
    void mediumBoostThreeMovesEightWithStepsFour() {
        GameState game = game(GameDifficulty.MEDIUM);
        PlayerEntry active = game.getActivePlayer();
        Piece released = release(game, active, 1);

        assertEquals(Outcome.MOVE_REQUIRED,
                game.recordAdvancedRoll(active, 4, 3));
        assertTrue(game.getSelectablePieces().contains(released));
        assertEquals(3, game.getPendingSpecialDie());
        assertEquals(Outcome.TURN_PASSED, game.movePiece(active, released));
        assertEquals(8, released.getProgress());
        assertEquals(0, game.getPendingSpecialDie());
    }

    @Test
    void hardUltraBoostSixMovesTwentyWithStepsFour() {
        GameState game = game(GameDifficulty.HARD);
        PlayerEntry active = game.getActivePlayer();
        Piece released = release(game, active, 1);

        assertEquals(Outcome.MOVE_REQUIRED,
                game.recordAdvancedRoll(active, 4, 6));
        game.movePiece(active, released);
        assertEquals(20, released.getProgress());
    }

    @Test
    void noTrackMovementWhenSpecialDieIsOne() {
        GameState game = game(GameDifficulty.MEDIUM);
        PlayerEntry active = game.getActivePlayer();
        Piece released = release(game, active, 1);

        assertFalse(game.hasLegalMove(active, 3, 1));
        assertEquals(Outcome.TURN_PASSED,
                game.recordAdvancedRoll(active, 3, 1));
        assertEquals(0, released.getProgress());
        assertTrue(game.getSelectablePieces().isEmpty());
    }

    @Test
    void homePathIgnoresUltraBoostMultiplier() {
        GameState game = game(GameDifficulty.HARD);
        PlayerEntry active = game.getActivePlayer();
        Piece piece = game.getPieces(active).get(0);
        int entry = game.getEntrySteps(active.color());
        game.placeAt(piece, entry + 1);

        assertEquals(Outcome.MOVE_REQUIRED,
                game.recordAdvancedRoll(active, 3, 6));
        assertTrue(game.getSelectablePieces().contains(piece));
        game.movePiece(active, piece);
        assertEquals(entry + 4, piece.getProgress());
    }

    @Test
    void invalidBoostIsRejectedWithoutChangingTurn() {
        GameState game = game(GameDifficulty.MEDIUM);
        PlayerEntry active = game.getActivePlayer();
        assertThrows(IllegalArgumentException.class,
                () -> game.recordAdvancedRoll(active, 5, 4));
        assertSame(active, game.getActivePlayer());
        assertTrue(game.isAwaitingRoll());
        assertEquals(0, game.getPendingRoll());
    }

    @Test
    void thirdConsecutiveStepsSixStillForfeitsTurn() {
        GameState game = game(GameDifficulty.HARD);
        PlayerEntry active = game.getActivePlayer();
        release(game, active, 1);
        release(game, active, 1);

        assertEquals(Outcome.THIRD_SIX_FORFEIT,
                game.recordAdvancedRoll(active, 6, 6));
        assertEquals(0, game.getPendingRoll());
        assertEquals(0, game.getPendingSpecialDie());
        assertNotEquals(active, game.getActivePlayer());
    }

    @Test
    void easyLegacyRollStillWorks() {
        GameState game = game(GameDifficulty.EASY);
        PlayerEntry active = game.getActivePlayer();
        assertEquals(Outcome.MOVE_REQUIRED, game.recordRoll(active, 6));
        assertEquals(6, game.getPendingRoll());
        assertEquals(0, game.getPendingSpecialDie());
        assertEquals(Outcome.BONUS_ROLL,
                game.movePiece(active, game.getSelectablePieces().get(0)));
    }
}
