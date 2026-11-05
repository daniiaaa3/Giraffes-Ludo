
package Model;

import java.util.List;
import java.util.Random;

import Model.GameSetup.PlayerEntry;

/**
 * Manages whose turn it is during a game.
 *
 * Picks a random starting player, keeps a cyclic turn order
 * and only accepts turn actions from the active player.
 * Invalid or out-of-turn requests are rejected with an
 * exception and never change the state.
 *
 * This class does not know about pieces, dice or the board,
 * so later gameplay rules can ask it who may act.
 *
 * Turn progression is done through {@link TurnRollRules},
 * the main API for gameplay controllers. Controllers should
 * use this class only for read-only queries such as
 * {@link #getActivePlayer()} and {@link #isActivePlayer}.
 *
 * MVC Component: Model
 * SRS Requirements: FR-1.6, FR-6.1, FR-6.2
 * Iteration: 1
 */
public class TurnManager {

    private final List<PlayerEntry> players;
    private int activeIndex;

    /**
     * Creates a turn manager and randomly selects the
     * first player.
     *
     * @param players validated players in seating order
     *                (see {@link GameSetup#validate})
     * @param random  random source, injectable for tests
     * @throws IllegalArgumentException if the player list
     *         does not contain 2-4 non-null, distinct
     *         players, or the random source is null
     */
    public TurnManager(List<PlayerEntry> players,
                       Random random) {

        if (random == null) {
            throw new IllegalArgumentException(
                    "Random source cannot be null.");
        }

        if (players == null ||
                players.size() < GameSetup.MIN_PLAYERS ||
                players.size() > GameSetup.MAX_PLAYERS) {
            throw new IllegalArgumentException(
                    "A game needs 2 to 4 players.");
        }

        if (players.stream().anyMatch(p -> p == null)) {
            throw new IllegalArgumentException(
                    "Player information is missing.");
        }

        // Equal entries would make turns ambiguous.
        if (players.stream().distinct().count() != players.size()) {
            throw new IllegalArgumentException(
                    "Players must be unique.");
        }

        // Defensive immutable copy of the turn order.
        this.players = List.copyOf(players);

        // FR-1.6: every player is equally likely to start.
        this.activeIndex = random.nextInt(this.players.size());
    }

    /**
     * @return the player whose turn it currently is
     */
    public PlayerEntry getActivePlayer() {
        return players.get(activeIndex);
    }

    /**
     * @return zero-based index of the active player
     */
    public int getActiveIndex() {
        return activeIndex;
    }

    /**
     * @return the players in turn order (unmodifiable)
     */
    public List<PlayerEntry> getPlayers() {
        return players;
    }

    /**
     * Checks whether the given player may act now.
     *
     * @param player the player attempting an action
     * @return true only for the active player
     */
    public boolean isActivePlayer(PlayerEntry player) {
        return player != null &&
                getActivePlayer().equals(player);
    }

    /**
     * Guard for turn actions such as rolling or moving.
     * Does nothing when the player is active.
     *
     * @param player the player attempting an action
     * @throws IllegalArgumentException if the player is
     *         null or is not part of this game
     * @throws IllegalStateException if it is not that
     *         player's turn
     */
    public void requireActivePlayer(PlayerEntry player) {

        if (player == null || !players.contains(player)) {
            throw new IllegalArgumentException(
                    "Unknown player.");
        }

        if (!isActivePlayer(player)) {
            throw new IllegalStateException(
                    "It is not " + player.name() +
                    "'s turn.");
        }
    }

    /**
     * Ends the active player's turn and passes it to the
     * next player in cyclic order.
     *
     * Package-private on purpose: controllers must progress
     * turns through {@link TurnRollRules}, which keeps the
     * six-streak and pending-move state in step with this
     * class. Calling this directly would bypass those rules.
     *
     * @param player the player ending the turn
     * @return the player whose turn starts next
     * @throws IllegalArgumentException if the player is
     *         null or not in this game
     * @throws IllegalStateException if it is not that
     *         player's turn
     */
    PlayerEntry endTurn(PlayerEntry player) {

        // Validate first so a rejected call changes nothing.
        requireActivePlayer(player);

        activeIndex = (activeIndex + 1) % players.size();

        return getActivePlayer();
    }
}
