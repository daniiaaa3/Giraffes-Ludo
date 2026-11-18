package Model;

import Model.EasyBoard.BoardCell;

/**
 * Tells GameState whether a board square is protected, meaning a
 * piece standing on it cannot be captured (FR-3.1).
 *
 * GameState always protects the squares it can derive from the
 * Easy board: the four start squares (FR-3.2) and the last home
 * square of every color (FR-3.4). Everything else that is
 * protected is supplied through this interface, so the board
 * classes do not need to be changed:
 * - the random Safe squares of every level (FR-2.6, FR-3.1)
 * - the Surprise squares of Medium and Hard (FR-3.6)
 *
 * Whoever places those squares only has to provide an
 * implementation, for example
 * {@code cell -> safeSquares.contains(cell)}, and pass it to the
 * GameState constructor.
 *
 * MVC Component: Model
 * SRS Requirements: FR-3.1, FR-3.2, FR-3.4, FR-3.6
 * Iteration: 2
 */
@FunctionalInterface
public interface ProtectedSquares {

    /**
     * @param cell a board square
     * @return true if pieces on this square cannot be captured
     */
    boolean isProtected(BoardCell cell);

    /** @return a rule that protects nothing */
    static ProtectedSquares none() {
        return cell -> false;
    }

    /**
     * Combines two rules.
     *
     * @param other another rule; must not be null
     * @return a rule that protects a square if either rule does
     * @throws IllegalArgumentException if other is null
     */
    default ProtectedSquares or(ProtectedSquares other) {

        if (other == null) {
            throw new IllegalArgumentException(
                    "The other rule cannot be null.");
        }

        return cell -> isProtected(cell) || other.isProtected(cell);
    }
}
