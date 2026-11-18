package Model;

import java.util.Optional;

/**
 * Design pattern: TEMPLATE METHOD (Iteration 2, Marwa).
 *
 * Every movement of a piece follows the same process:
 *
 * 1. validate the move                       (varies: abstract step)
 * 2. let the caller react once it is accepted (varies: optional hook)
 * 3. work out the destination                (varies: abstract step)
 * 4. stop if the piece would not change square (shared: FR-4.6)
 * 5. place the piece and set its state       (shared)
 * 6. resolve capture and protected squares   (shared: FR-3.1, FR-5.4)
 * 7. record the result                       (shared)
 *
 * {@link #execute} is the template method. It is final, so the
 * order of the steps and the shared steps cannot be changed by a
 * subclass. Subclasses only fill in what really differs:
 *
 * - {@link ForwardMove}: a turn action. It is validated against the
 *   rolled steps, uses up the pending roll, and goes forward.
 * - {@link BackwardMove}: not a turn action. It goes back and stops
 *   at the start square (FR-5.9).
 *
 * Capture, protected squares and the state of the piece are written
 * once, in GameState, and reached only through this template, so a
 * forward and a backward landing can never be treated differently.
 * A future kind of move (for example a question or surprise effect)
 * becomes one more small subclass.
 *
 * MVC Component: Model
 * SRS Requirements: FR-5.4, FR-5.9, FR-3.1, FR-4.6
 * Iteration: 2
 */
abstract class PieceMove {

    /** The game this move is played in. */
    protected final GameState game;

    PieceMove(GameState game) {

        if (game == null) {
            throw new IllegalArgumentException(
                    "Game cannot be null.");
        }

        this.game = game;
    }

    /**
     * THE TEMPLATE METHOD: runs the whole move in a fixed order.
     *
     * @param piece the piece to move
     * @return what the move did (also kept by the game as its last
     *         move)
     * @throws IllegalArgumentException if {@link #validate} rejects
     *         the move; nothing has changed in that case
     */
    final MoveResult execute(Piece piece) {

        validate(piece);                       // 1. varies
        afterValidation();                     // 2. hook
        int from = piece.getProgress();
        int to = destination(piece);           // 3. varies

        if (to == from) {                      // 4. shared
            return game.record(
                    new MoveResult(piece, from, from, Optional.empty()));
        }

        game.placeAt(piece, to);               // 5. shared
        Optional<Piece> captured =
                game.resolveCapture(piece);    // 6. shared
        return game.record(                    // 7. shared
                new MoveResult(piece, from, to, captured));
    }

    /**
     * Step 1: rejects a move that is not allowed. Must not change
     * anything.
     *
     * @param piece the piece to move
     * @throws IllegalArgumentException if the move is not allowed
     */
    abstract void validate(Piece piece);

    /**
     * Step 2 (hook): called once the move is accepted and before the
     * piece moves. Does nothing unless a subclass needs it.
     */
    void afterValidation() {
    }

    /**
     * Step 3: the progress the piece ends on.
     *
     * @param piece the piece to move
     * @return its new progress (the current one if it stays)
     */
    abstract int destination(Piece piece);
}
