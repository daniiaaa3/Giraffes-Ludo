
package Model;

import Model.GameSetup.PlayerEntry;

/**
 * Applies the extra-roll rules on top of {@link TurnManager}.
 *
 * - A regular Steps die result of 6 grants an additional
 *   roll, even when no legal piece movement exists (FR-6.3).
 * - The third consecutive 6 by the same player is cancelled,
 *   its movement is not performed, and the turn passes to the
 *   next player (FR-6.4).
 * - A turn never ends while a legally available move is still
 *   unresolved: the caller reports the roll together with
 *   whether a legal move exists, and must call
 *   {@link #completeMove(PlayerEntry)} before play continues.
 *
 * This is the main API for turn progression. Gameplay
 * controllers should hold this class and must not advance
 * turns through {@link TurnManager} (its endTurn is
 * package-private for that reason).
 *
 * The die value is supplied by the caller, so this class does
 * not depend on any dice implementation, the board, pieces or
 * JavaFX. Every rejected call leaves the state unchanged.
 *
 * MVC Component: Model
 * SRS Requirements: FR-6.3, FR-6.4
 * Iteration: 1
 */
public class TurnRollRules {

    /** Highest value on the regular Steps die. */
    public static final int BONUS_VALUE = 6;

    /** Number of consecutive sixes that forfeits the turn. */
    public static final int MAX_CONSECUTIVE_SIXES = 3;

    /**
     * Result of reporting a roll or a completed move.
     */
    public enum Outcome {
        /** A legal move exists; the player must resolve it. */
        MOVE_REQUIRED,
        /** The same player must roll again. */
        BONUS_ROLL,
        /** The turn ended normally; the next player is active. */
        TURN_PASSED,
        /** Third six: roll and move cancelled, turn passed. */
        THIRD_SIX_FORFEIT
    }

    private enum Phase { AWAITING_ROLL, AWAITING_MOVE }

    private final TurnManager turnManager;

    private Phase phase = Phase.AWAITING_ROLL;
    private int consecutiveSixes = 0;
    private boolean bonusAfterMove = false;

    /**
     * @param turnManager turn order and active-player source
     */
    public TurnRollRules(TurnManager turnManager) {

        if (turnManager == null) {
            throw new IllegalArgumentException(
                    "Turn manager cannot be null.");
        }

        this.turnManager = turnManager;
    }

    /**
     * Records a regular Steps die result for the active player.
     *
     * @param player        the player who rolled
     * @param value         die result, 1-6
     * @param hasLegalMove  whether the board offers at least
     *                      one legal move for this roll
     * @return what must happen next
     * @throws IllegalArgumentException for an unknown player
     *         or a value outside 1-6
     * @throws IllegalStateException if it is not the player's
     *         turn, or a move is still waiting to be resolved
     */
    public Outcome recordRoll(PlayerEntry player,
                              int value,
                              boolean hasLegalMove) {

        // All validation happens before any state changes.
        turnManager.requireActivePlayer(player);

        if (value < 1 || value > BONUS_VALUE) {
            throw new IllegalArgumentException(
                    "Die value must be between 1 and 6.");
        }

        if (phase != Phase.AWAITING_ROLL) {
            throw new IllegalStateException(
                    "Resolve the current move before rolling.");
        }

        boolean six = value == BONUS_VALUE;

        // FR-6.4: cancel the third consecutive six.
        if (six && consecutiveSixes + 1 >= MAX_CONSECUTIVE_SIXES) {
            passTurn(player);
            return Outcome.THIRD_SIX_FORFEIT;
        }

        // A non-six breaks the streak.
        consecutiveSixes = six ? consecutiveSixes + 1 : 0;

        // FR-6.3: a six earns a bonus roll, move or no move.
        if (hasLegalMove) {
            phase = Phase.AWAITING_MOVE;
            bonusAfterMove = six;
            return Outcome.MOVE_REQUIRED;
        }

        return finishRoll(player, six);
    }

    /**
     * Reports that the pending legal move was resolved.
     *
     * @param player the player who made the move
     * @return {@link Outcome#BONUS_ROLL} or
     *         {@link Outcome#TURN_PASSED}
     * @throws IllegalArgumentException for an unknown player
     * @throws IllegalStateException if it is not the player's
     *         turn or no move is pending
     */
    public Outcome completeMove(PlayerEntry player) {

        turnManager.requireActivePlayer(player);

        if (phase != Phase.AWAITING_MOVE) {
            throw new IllegalStateException(
                    "There is no move to complete.");
        }

        boolean bonus = bonusAfterMove;
        phase = Phase.AWAITING_ROLL;
        bonusAfterMove = false;

        return finishRoll(player, bonus);
    }

    /** @return the active player (delegates to TurnManager) */
    public PlayerEntry getActivePlayer() {
        return turnManager.getActivePlayer();
    }

    /** @return sixes rolled in a row by the active player */
    public int getConsecutiveSixes() {
        return consecutiveSixes;
    }

    /** @return true while a legal move must still be resolved */
    public boolean isAwaitingMove() {
        return phase == Phase.AWAITING_MOVE;
    }

    /** @return true when the active player should roll next */
    public boolean isAwaitingRoll() {
        return phase == Phase.AWAITING_ROLL;
    }

    // Bonus roll for the same player, or hand over the turn.
    private Outcome finishRoll(PlayerEntry player, boolean bonus) {

        if (bonus) {
            return Outcome.BONUS_ROLL;
        }

        passTurn(player);
        return Outcome.TURN_PASSED;
    }

    // A new regular turn starts with a fresh streak.
    private void passTurn(PlayerEntry player) {
        turnManager.endTurn(player);
        consecutiveSixes = 0;
        bonusAfterMove = false;
        phase = Phase.AWAITING_ROLL;
    }
}
