
package Model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import Model.GameSetup.PlayerColor;
import Model.GameSetup.PlayerEntry;
import Model.TurnRollRules.Outcome;

/**
 * Unit tests for extra rolls and the three-sixes rule.
 *
 * SRS Requirements: FR-6.3, FR-6.4
 * Iteration: 1
 */
public class TurnRollRulesTest {

    private static final PlayerEntry A =
            new PlayerEntry("Dania", PlayerColor.RED);
    private static final PlayerEntry B =
            new PlayerEntry("Marwa", PlayerColor.BLUE);
    private static final PlayerEntry C =
            new PlayerEntry("Nay", PlayerColor.GREEN);
    private static final PlayerEntry D =
            new PlayerEntry("Yara", PlayerColor.YELLOW);

    private static final List<PlayerEntry> ALL =
            List.of(A, B, C, D);

    /** Random that always starts the first listed player. */
    private static Random firstSeat() {
        return new Random() {
            @Override
            public int nextInt(int bound) {
                return 0;
            }
        };
    }

    private static TurnRollRules rulesFor(int players) {
        return new TurnRollRules(new TurnManager(
                ALL.subList(0, players), firstSeat()));
    }

    // ---------- normal roll ----------

    @Test
    void normalRollPassesToNextPlayer() {
        TurnRollRules rules = rulesFor(2);

        assertEquals(Outcome.TURN_PASSED,
                rules.recordRoll(A, 4, false));
        assertEquals(B, rules.getActivePlayer());
    }

    @Test
    void normalRollWithLegalMoveWaitsForMove() {
        TurnRollRules rules = rulesFor(2);

        assertEquals(Outcome.MOVE_REQUIRED,
                rules.recordRoll(A, 4, true));

        // Turn must not end before the move is resolved.
        assertEquals(A, rules.getActivePlayer());
        assertTrue(rules.isAwaitingMove());

        assertEquals(Outcome.TURN_PASSED,
                rules.completeMove(A));
        assertEquals(B, rules.getActivePlayer());
        assertTrue(rules.isAwaitingRoll());
    }

    // ---------- FR-6.3 ----------

    @Test
    void oneSixGivesExtraRoll() {
        TurnRollRules rules = rulesFor(2);

        assertEquals(Outcome.BONUS_ROLL,
                rules.recordRoll(A, 6, false));
        assertEquals(A, rules.getActivePlayer());
        assertEquals(1, rules.getConsecutiveSixes());
    }

    @Test
    void sixWithLegalMoveGivesBonusOnlyAfterMove() {
        TurnRollRules rules = rulesFor(2);

        assertEquals(Outcome.MOVE_REQUIRED,
                rules.recordRoll(A, 6, true));

        // Cannot roll again before resolving the move.
        assertThrows(IllegalStateException.class,
                () -> rules.recordRoll(A, 3, false));

        assertEquals(Outcome.BONUS_ROLL,
                rules.completeMove(A));
        assertEquals(A, rules.getActivePlayer());
        assertTrue(rules.isAwaitingRoll());
    }

    @Test
    void sixWithNoLegalMoveStillGivesBonusRoll() {
        TurnRollRules rules = rulesFor(3);

        assertEquals(Outcome.BONUS_ROLL,
                rules.recordRoll(A, 6, false));
        assertEquals(A, rules.getActivePlayer());
        assertTrue(rules.isAwaitingRoll());
    }

    @Test
    void twoConsecutiveSixesGiveFurtherRolls() {
        TurnRollRules rules = rulesFor(2);

        assertEquals(Outcome.BONUS_ROLL,
                rules.recordRoll(A, 6, false));
        assertEquals(Outcome.BONUS_ROLL,
                rules.recordRoll(A, 6, false));

        assertEquals(A, rules.getActivePlayer());
        assertEquals(2, rules.getConsecutiveSixes());

        // A following non-six ends the turn.
        assertEquals(Outcome.TURN_PASSED,
                rules.recordRoll(A, 2, false));
        assertEquals(B, rules.getActivePlayer());
    }

    // ---------- FR-6.4 ----------

    @Test
    void threeSixesCancelThirdRollAndPassTurn() {
        TurnRollRules rules = rulesFor(2);

        rules.recordRoll(A, 6, false);
        rules.recordRoll(A, 6, false);

        assertEquals(Outcome.THIRD_SIX_FORFEIT,
                rules.recordRoll(A, 6, false));

        assertEquals(B, rules.getActivePlayer());
        assertEquals(0, rules.getConsecutiveSixes());
        assertTrue(rules.isAwaitingRoll());
    }

    @Test
    void thirdSixCancelsMovementEvenIfLegalMoveExists() {
        TurnRollRules rules = rulesFor(2);

        // Earlier sixes had their moves resolved.
        rules.recordRoll(A, 6, true);
        rules.completeMove(A);
        rules.recordRoll(A, 6, true);
        rules.completeMove(A);

        // Third six: no move is offered at all.
        assertEquals(Outcome.THIRD_SIX_FORFEIT,
                rules.recordRoll(A, 6, true));

        assertFalse(rules.isAwaitingMove());
        assertEquals(B, rules.getActivePlayer());
        assertThrows(IllegalStateException.class,
                () -> rules.completeMove(A));
    }

    @Test
    void nextPlayerStartsWithFreshStreak() {
        TurnRollRules rules = rulesFor(2);

        rules.recordRoll(A, 6, false);
        rules.recordRoll(A, 6, false);
        rules.recordRoll(A, 6, false); // forfeit, B active

        assertEquals(Outcome.BONUS_ROLL,
                rules.recordRoll(B, 6, false));
        assertEquals(1, rules.getConsecutiveSixes());
    }

    // ---------- streak reset ----------

    @Test
    void nonSixResetsStreakForLaterTurns() {
        TurnRollRules rules = rulesFor(2);

        rules.recordRoll(A, 6, false);
        rules.recordRoll(A, 3, false); // resets, B active
        assertEquals(0, rules.getConsecutiveSixes());

        rules.recordRoll(B, 1, false); // A active again

        // Two sixes now are NOT a third consecutive six.
        assertEquals(Outcome.BONUS_ROLL,
                rules.recordRoll(A, 6, false));
        assertEquals(Outcome.BONUS_ROLL,
                rules.recordRoll(A, 6, false));
        assertEquals(A, rules.getActivePlayer());
    }

    @Test
    void sixesSplitByNonSixDoNotAccumulateAcrossTurns() {
        TurnRollRules rules = rulesFor(2);

        for (int round = 0; round < 3; round++) {
            assertEquals(Outcome.BONUS_ROLL,
                    rules.recordRoll(A, 6, false));
            assertEquals(Outcome.TURN_PASSED,
                    rules.recordRoll(A, 5, false));
            rules.recordRoll(B, 2, false);
        }

        assertEquals(A, rules.getActivePlayer());
    }

    // ---------- rejected actions keep state ----------

    @Test
    void outOfTurnRollIsRejectedWithoutChangingState() {
        TurnRollRules rules = rulesFor(3);
        rules.recordRoll(A, 6, false); // A has 1 six

        assertThrows(IllegalStateException.class,
                () -> rules.recordRoll(B, 6, true));

        assertEquals(A, rules.getActivePlayer());
        assertEquals(1, rules.getConsecutiveSixes());
        assertTrue(rules.isAwaitingRoll());
    }

    @Test
    void outOfTurnCompleteMoveIsRejectedWithoutChangingState() {
        TurnRollRules rules = rulesFor(2);
        rules.recordRoll(A, 4, true);

        assertThrows(IllegalStateException.class,
                () -> rules.completeMove(B));

        assertTrue(rules.isAwaitingMove());
        assertEquals(A, rules.getActivePlayer());
    }

    @Test
    void invalidValuesAreRejectedWithoutChangingState() {
        TurnRollRules rules = rulesFor(2);
        rules.recordRoll(A, 6, false);

        assertThrows(IllegalArgumentException.class,
                () -> rules.recordRoll(A, 0, false));
        assertThrows(IllegalArgumentException.class,
                () -> rules.recordRoll(A, 7, false));
        assertThrows(IllegalArgumentException.class,
                () -> rules.recordRoll(A, -1, true));

        assertEquals(1, rules.getConsecutiveSixes());
        assertEquals(A, rules.getActivePlayer());
        assertTrue(rules.isAwaitingRoll());
    }

    @Test
    void unknownOrNullPlayerIsRejected() {
        TurnRollRules rules = rulesFor(2);
        PlayerEntry stranger =
                new PlayerEntry("Stranger", PlayerColor.GREEN);

        assertThrows(IllegalArgumentException.class,
                () -> rules.recordRoll(stranger, 3, false));
        assertThrows(IllegalArgumentException.class,
                () -> rules.recordRoll(null, 3, false));
        assertThrows(IllegalArgumentException.class,
                () -> rules.completeMove(null));

        assertEquals(A, rules.getActivePlayer());
    }

    @Test
    void completeMoveWithoutPendingMoveIsRejected() {
        TurnRollRules rules = rulesFor(2);

        assertThrows(IllegalStateException.class,
                () -> rules.completeMove(A));
        assertEquals(A, rules.getActivePlayer());
    }

    @Test
    void rejectsNullTurnManager() {
        assertThrows(IllegalArgumentException.class,
                () -> new TurnRollRules(null));
    }

    // ---------- 2-4 players ----------

    @Test
    void threeSixesRotateCorrectlyForEveryPlayerCount() {
        for (int players = 2; players <= 4; players++) {
            TurnRollRules rules = rulesFor(players);

            rules.recordRoll(A, 6, false);
            rules.recordRoll(A, 6, false);
            assertEquals(Outcome.THIRD_SIX_FORFEIT,
                    rules.recordRoll(A, 6, false));

            assertEquals(ALL.get(1), rules.getActivePlayer());
        }
    }

    @Test
    void normalRollsCycleBackToFirstPlayerForEveryCount() {
        for (int players = 2; players <= 4; players++) {
            TurnRollRules rules = rulesFor(players);

            for (int i = 0; i < players; i++) {
                assertEquals(ALL.get(i), rules.getActivePlayer());
                rules.recordRoll(ALL.get(i), 3, false);
            }

            assertEquals(A, rules.getActivePlayer());
        }
    }

    @Test
    void worksWithValidatedSetupOutput() {
        List<PlayerEntry> validated = new GameSetup().validate(
                List.of(new PlayerEntry(" Dania ", PlayerColor.RED),
                        new PlayerEntry("Marwa", PlayerColor.BLUE)));

        TurnRollRules rules = new TurnRollRules(
                new TurnManager(validated, firstSeat()));

        assertEquals(Outcome.BONUS_ROLL,
                rules.recordRoll(validated.get(0), 6, false));
    }
}
