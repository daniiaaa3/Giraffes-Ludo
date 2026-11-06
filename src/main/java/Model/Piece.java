
package Model;

import Model.GameSetup.PlayerColor;

/**
 * A single Ludo piece.
 *
 * A piece knows who owns it, which of the owner's four pieces
 * it is, where it is in its journey, and nothing else. Its
 * position is stored as "progress": the number of steps from
 * the owner's start square.
 *
 * - CASTLE: progress is {@link #CASTLE_PROGRESS} (-1)
 * - TRACK: 0 (the start square) up to the board's home-entry
 *   arrow
 * - HOME_PATH: the six-square home path
 * - FINISHED: the last home-path square
 *
 * Controllers and views do not create pieces: GameState
 * creates the four pieces of every player when a game starts
 * (see its constructor), so there is one initialization path.
 *
 * Pieces are read-only for everyone except {@link GameState},
 * which owns the rules for changing them (the mutator is
 * package-private).
 *
 * MVC Component: Model
 * SRS Requirements: FR-5.1, FR-5.2
 * Iteration: 1
 */
public class Piece {

    /** Number of pieces each player owns. */
    public static final int PIECES_PER_PLAYER = 4;

    /** Progress value of a piece waiting in its castle. */
    public static final int CASTLE_PROGRESS = -1;

    /**
     * Where a piece currently is.
     */
    public enum State {
        CASTLE, TRACK, HOME_PATH, FINISHED
    }

    private final PlayerColor color;
    private final int id;

    private State state = State.CASTLE;
    private int progress = CASTLE_PROGRESS;

    /**
     * Creates a piece waiting in its castle.
     *
     * @param color owner's color
     * @param id    0-3, which of the owner's pieces this is
     * @throws IllegalArgumentException if the color is null
     *         or the id is outside 0-3
     */
    public Piece(PlayerColor color, int id) {

        if (color == null) {
            throw new IllegalArgumentException(
                    "Piece color cannot be null.");
        }

        if (id < 0 || id >= PIECES_PER_PLAYER) {
            throw new IllegalArgumentException(
                    "Piece id must be between 0 and 3.");
        }

        this.color = color;
        this.id = id;
    }

    /** @return the owner's color */
    public PlayerColor getColor() {
        return color;
    }

    /** @return 0-3, the piece's number within its owner */
    public int getId() {
        return id;
    }

    /** @return the piece's current state */
    public State getState() {
        return state;
    }

    /**
     * @return steps from the owner's start square, or
     *         {@link #CASTLE_PROGRESS} while in the castle
     */
    public int getProgress() {
        return progress;
    }

    /** @return true while the piece waits in its castle */
    public boolean isInCastle() {
        return state == State.CASTLE;
    }

    /** @return true once the piece reached its final square */
    public boolean isFinished() {
        return state == State.FINISHED;
    }

    /**
     * Changes the piece's position. Only GameState applies
     * rules, so this is not public.
     *
     * @param newProgress steps from the start square, or -1
     * @param newState    matching state
     * @throws IllegalArgumentException if the two disagree
     */
    void moveTo(int newProgress, State newState) {

        if (newState == null) {
            throw new IllegalArgumentException(
                    "State cannot be null.");
        }

        boolean castle = newState == State.CASTLE;

        if (castle != (newProgress == CASTLE_PROGRESS) ||
                newProgress < CASTLE_PROGRESS) {
            throw new IllegalArgumentException(
                    "Progress does not match the state.");
        }

        this.progress = newProgress;
        this.state = newState;
    }

    @Override
    public String toString() {
        return color + " piece " + id +
                " (" + state + ", progress " + progress + ")";
    }
}
