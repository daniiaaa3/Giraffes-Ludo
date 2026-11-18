
package Model;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import Model.EasyBoard.BoardCell;
import Model.GameSetup.PlayerColor;
import Model.GameSetup.PlayerEntry;
import Model.TurnRollRules.Outcome;

/**
 * The state of one running game: pieces, whose turn it is,
 * and the rules for selecting and moving pieces.
 *
 * GameState is the single entry point for gameplay
 * controllers. It owns the {@link TurnManager} and
 * {@link TurnRollRules}, so rolling, moving, extra rolls and
 * turn changes can never get out of step:
 *
 * 1. {@link #recordRoll} reports a Steps die result. The game
 *    works out whether the player has a legal move and lets
 *    TurnRollRules apply the six / three-sixes rules.
 * 2. If the outcome is MOVE_REQUIRED, the roll is "pending"
 *    and the player must call {@link #movePiece} with one of
 *    {@link #getSelectablePieces()}. No new roll is accepted
 *    before that (FR-5.2: a legal move must be performed).
 * 3. {@link #movePiece} uses the whole pending roll for the
 *    chosen piece. Steps cannot be split, and the roll is
 *    consumed, so a second piece cannot use it.
 *
 * Board geometry comes from {@link EasyBoard}; the castle
 * release rule comes from {@link CastleReleaseRules}. Neither
 * is reimplemented here.
 *
 * Movement model (from EasyBoard's geometry, verified when
 * the game is created):
 * - A released piece stands on its start square (progress 0).
 * - It walks the shared track to its color's arrow square
 *   (entry steps), then through the six home-path squares.
 * - The last home-path square is the finish. A move is legal
 *   only if it does not go beyond it (exact finish).
 *
 * Iteration 2 adds capture (FR-5.4) and protected squares
 * (FR-3.1, FR-3.2, FR-3.4): a piece that lands on an
 * unprotected square holding one opponent piece sends it back to
 * its castle. Extra protected squares (random Safe squares,
 * Surprise squares) are supplied through {@link ProtectedSquares}.
 * {@link #moveBackward} moves a piece back along its path, stopping
 * at its start square (FR-5.9) and capturing on landing (FR-5.4);
 * later question and surprise features call it.
 *
 * Not implemented here: Double Blocks, winner detection, and the
 * difficulty levels other than Easy.
 *
 * MVC Component: Model
 * SRS Requirements: FR-5.1, FR-5.2 (uses FR-4.4 / FR-5.3 via
 * CastleReleaseRules), FR-3.1, FR-3.2, FR-3.4, FR-5.4
 * Iteration: 1 and 2
 */
public class GameState {

    private final EasyBoard board;
    private final CastleReleaseRules castleRules;
    private final TurnManager turnManager;
    private final TurnRollRules rules;

    private final Map<PlayerColor, List<Piece>> pieces =
            new EnumMap<>(PlayerColor.class);

    /** Steps from a color's start square to its arrow. */
    private final Map<PlayerColor, Integer> entrySteps =
            new EnumMap<>(PlayerColor.class);

    /** The roll waiting for a piece move, or 0 for none. */
    private int pendingRoll = 0;

    /** Start squares and last home squares (FR-3.2, FR-3.4). */
    private final Set<BoardCell> fixedProtectedSquares =
            new HashSet<>();

    /** Safe / surprise squares supplied by the board owner. */
    private final ProtectedSquares additionalProtection;

    /** The most recent completed move, or null before any. */
    private MoveResult lastMove;

    /**
     * Creates a new game on the Easy board with four pieces
     * per player, all in their castles. This is the normal
     * way for the gameplay controller to start a game.
     *
     * @param players validated players from GameSetup
     * @param random  random source for the first player
     */
    public GameState(List<PlayerEntry> players, Random random) {
        this(players, random, ProtectedSquares.none());
    }

    /**
     * Creates a new game on the Easy board with extra protected
     * squares, such as the random Safe squares (FR-2.6).
     *
     * @param players              validated players
     * @param random               random source for the first player
     * @param additionalProtection protected squares besides the
     *                             start and final home squares
     */
    public GameState(List<PlayerEntry> players, Random random,
                     ProtectedSquares additionalProtection) {
        this(new EasyBoard(), new TurnManager(players, random),
                additionalProtection);
    }

    /**
     * Creates a game from an existing board and turn manager.
     *
     * @param board       the Easy board
     * @param turnManager turn order; its players are used
     * @throws IllegalArgumentException if an argument is null
     *         or two players share a color
     * @throws IllegalStateException if the board's geometry
     *         is inconsistent (arrow or home path misplaced)
     */
    public GameState(EasyBoard board, TurnManager turnManager) {
        this(board, turnManager, ProtectedSquares.none());
    }

    /**
     * Creates a game from an existing board and turn manager,
     * with extra protected squares.
     *
     * @param board                the Easy board
     * @param turnManager          turn order; its players are used
     * @param additionalProtection protected squares besides the
     *                             start and final home squares
     * @throws IllegalArgumentException if an argument is null
     *         or two players share a color
     * @throws IllegalStateException if the board's geometry
     *         is inconsistent (arrow or home path misplaced)
     */
    public GameState(EasyBoard board, TurnManager turnManager,
                     ProtectedSquares additionalProtection) {

        if (board == null || turnManager == null ||
                additionalProtection == null) {
            throw new IllegalArgumentException(
                    "Board, turn manager and protection cannot " +
                    "be null.");
        }

        this.additionalProtection = additionalProtection;
        this.board = board;
        this.castleRules = new CastleReleaseRules(board);
        this.turnManager = turnManager;
        this.rules = new TurnRollRules(turnManager);

        for (PlayerEntry player : turnManager.getPlayers()) {

            PlayerColor color = player.color();

            if (pieces.containsKey(color)) {
                throw new IllegalArgumentException(
                        "Each player must have a unique color.");
            }

            List<Piece> own = new ArrayList<>();

            for (int i = 0; i < Piece.PIECES_PER_PLAYER; i++) {
                own.add(new Piece(color, i));
            }

            pieces.put(color, List.copyOf(own));
            entrySteps.put(color, verifyAndGetEntrySteps(color));
        }

        // FR-3.2: every start square is protected, and
        // FR-3.4: so is the last square of every home path.
        for (PlayerColor color : PlayerColor.values()) {
            fixedProtectedSquares.add(board.getSharedTrackCell(
                    board.getStartSquare(color)));
            List<BoardCell> home = board.getHomePathCells(color);
            fixedProtectedSquares.add(home.get(home.size() - 1));
        }
    }

    // ------------------------------------------------------
    // Queries
    // ------------------------------------------------------

    /** @return the players in turn order */
    public List<PlayerEntry> getPlayers() {
        return turnManager.getPlayers();
    }

    /** @return the player whose turn it is */
    public PlayerEntry getActivePlayer() {
        return rules.getActivePlayer();
    }

    /** @return true when the active player should roll */
    public boolean isAwaitingRoll() {
        return rules.isAwaitingRoll();
    }

    /** @return true while a piece move must be made */
    public boolean isAwaitingMove() {
        return rules.isAwaitingMove();
    }

    /** @return sixes rolled in a row by the active player */
    public int getConsecutiveSixes() {
        return rules.getConsecutiveSixes();
    }

    /** @return the roll waiting for a piece move, or 0 */
    public int getPendingRoll() {
        return pendingRoll;
    }

    /**
     * @param color a color in this game
     * @return that color's four pieces (unmodifiable)
     * @throws IllegalArgumentException if no player uses it
     */
    public List<Piece> getPieces(PlayerColor color) {

        List<Piece> own = color == null ? null : pieces.get(color);

        if (own == null) {
            throw new IllegalArgumentException(
                    "No player uses this color.");
        }

        return own;
    }

    /**
     * @param player a player in this game
     * @return that player's four pieces
     */
    public List<Piece> getPieces(PlayerEntry player) {
        return getPieces(requireKnown(player).color());
    }

    /**
     * @param color a color in this game
     * @return steps from the start square to the arrow
     */
    public int getEntrySteps(PlayerColor color) {
        getPieces(color);
        return entrySteps.get(color);
    }

    /**
     * @param color a color in this game
     * @return steps from the start square to the final home
     *         square; a piece must land on it exactly
     */
    public int getFinishSteps(PlayerColor color) {
        return getEntrySteps(color) + EasyBoard.HOME_PATH_SIZE;
    }

    /**
     * Returns where a piece stands on the 15x15 board.
     *
     * @param piece a piece of this game
     * @return its cell, or empty while it is in the castle
     */
    public Optional<BoardCell> getBoardCell(Piece piece) {

        requireOwnedPiece(piece);

        if (piece.isInCastle()) {
            return Optional.empty();
        }

        return getBoardCellAt(piece.getColor(), piece.getProgress());
    }

    /**
     * Returns the board cell for a given progress of one color.
     * The View uses this to animate a piece through every square
     * of its path, so each step is shown as one real square.
     *
     * Progress 0 is the start square (#), progress up to the
     * entry steps stays on the shared track, and higher values
     * walk the six home-path squares. The finish is the last
     * home-path square.
     *
     * @param color a color in this game
     * @param progress steps from the start square, 0 or more
     * @return the cell, or empty for progress below 0
     * @throws IllegalArgumentException if the color is not in this
     *         game or the progress is beyond the finish
     */
    public Optional<BoardCell> getBoardCellAt(PlayerColor color,
                                              int progress) {

        int finish = getFinishSteps(color);   // validates the color

        if (progress < 0) {
            return Optional.empty();
        }

        if (progress > finish) {
            throw new IllegalArgumentException(
                    "Progress is beyond the finish square");
        }

        int entry = entrySteps.get(color);

        if (progress <= entry) {
            return Optional.of(board.getSharedTrackCell(
                    (board.getStartSquare(color) + progress)
                            % EasyBoard.TRACK_SIZE));
        }

        return Optional.of(
                board.getHomePathCells(color).get(progress - entry - 1));
    }

    // ------------------------------------------------------
    // FR-3.1 / FR-3.2 / FR-3.4: protected squares
    // ------------------------------------------------------

    /**
     * Tells whether pieces on a square are safe from capture.
     * Protected squares are the four start squares, the last
     * home square of each color, and any square the
     * {@link ProtectedSquares} rule given to this game protects
     * (random Safe squares, Surprise squares). Arrow squares
     * and question squares are regular, capturable squares.
     *
     * @param cell a board square; null is not protected
     * @return true if a piece standing there cannot be captured
     */
    public boolean isProtectedSquare(BoardCell cell) {

        if (cell == null) {
            return false;
        }

        return fixedProtectedSquares.contains(cell) ||
                additionalProtection.isProtected(cell);
    }

    /**
     * @return what the most recent completed move did,
     *         or empty before the first move
     */
    public Optional<MoveResult> getLastMoveResult() {
        return Optional.ofNullable(lastMove);
    }

    // ------------------------------------------------------
    // FR-5.9 / FR-5.4: backward movement
    // ------------------------------------------------------

    /**
     * Tells whether a piece can be moved backward: it must be on
     * the board. Pieces in the castle and finished pieces cannot.
     * A piece standing on its start square counts as on the board;
     * moving it backward leaves it there (FR-5.9).
     *
     * @param piece a piece of this game
     * @return true if {@link #moveBackward} accepts the piece
     * @throws IllegalArgumentException if the piece is null or
     *         does not belong to this game
     */
    public boolean canMoveBackward(Piece piece) {
        requireOwnedPiece(piece);
        return piece.getState() == Piece.State.TRACK ||
                piece.getState() == Piece.State.HOME_PATH;
    }

    /**
     * Moves a piece backward along its own path, the reverse of
     * forward movement.
     *
     * - A piece that would go back past its start square stops on
     *   the start square (FR-5.9).
     * - The piece lands on one square; pieces passed on the way
     *   are not affected. If the landing square is unprotected
     *   and holds one opponent piece, that piece is captured
     *   (FR-5.4, "forward, backward, ...").
     * - A move of 0 steps, or one that leaves the piece where it
     *   is (already on its start square), changes nothing and
     *   captures nothing (FR-4.6: the square is not activated
     *   again).
     *
     * This is not a turn action: it does not read or change the
     * active player, the pending roll or the six counter, and it
     * can move any piece of the game, because questions and
     * surprise events decide who is moved. The caller (a later
     * question or surprise feature) supplies the number of steps.
     *
     * Not handled here (other owners): Double Blocks. A block on
     * the way back (FR-5.6, FR-5.7) is not checked; the block
     * rules will need to hook in before the piece is placed.
     *
     * ASSUMPTION (the SRS does not define backward movement from
     * the home path): the home path is simply the continuation of
     * the piece's path, so a piece on it walks back along it and,
     * if the steps allow, onto the shared track through its arrow
     * square. See {@link BackwardMove#destination}.
     *
     * @param piece a piece of this game that is on the board
     * @param steps how many steps back; 0 or more
     * @return what the move did
     * @throws IllegalArgumentException if steps is negative, or the
     *         piece is null, foreign, in the castle or finished
     */
    public MoveResult moveBackward(Piece piece, int steps) {
        return new BackwardMove(this, steps).execute(piece);
    }

    // ------------------------------------------------------
    // FR-5.1: legal piece selection
    // ------------------------------------------------------

    /**
     * Returns the player's own pieces that could legally use
     * a roll. Other players' pieces are never included.
     *
     * @param player a player in this game
     * @param steps  Steps die result, 1-6
     * @return legal pieces (possibly empty)
     * @throws IllegalArgumentException for an unknown player
     *         or a value outside 1-6
     */
    public List<Piece> getLegalPieces(PlayerEntry player,
                                      int steps) {

        PlayerColor color = requireKnown(player).color();
        requireDieValue(steps);

        List<Piece> legal = new ArrayList<>();

        for (Piece piece : pieces.get(color)) {
            if (isLegal(piece, steps)) {
                legal.add(piece);
            }
        }

        return List.copyOf(legal);
    }

    /**
     * @param player a player in this game
     * @param steps  Steps die result, 1-6
     * @return true if at least one piece can use the roll
     */
    public boolean hasLegalMove(PlayerEntry player, int steps) {
        return !getLegalPieces(player, steps).isEmpty();
    }

    /**
     * @return the active player's pieces that may take the
     *         pending roll; empty when no move is pending
     */
    public List<Piece> getSelectablePieces() {

        if (pendingRoll == 0) {
            return List.of();
        }

        return getLegalPieces(getActivePlayer(), pendingRoll);
    }

    // ------------------------------------------------------
    // FR-5.2: rolling and moving
    // ------------------------------------------------------

    /**
     * Records a Steps die result for the active player.
     *
     * @param player the player who rolled
     * @param value  die result, 1-6
     * @return what must happen next (see TurnRollRules)
     * @throws IllegalArgumentException for an unknown player
     *         or a value outside 1-6
     * @throws IllegalStateException if it is not the player's
     *         turn or a move is still pending
     *         (a rejected call changes nothing)
     */
    public Outcome recordRoll(PlayerEntry player, int value) {

        turnManager.requireActivePlayer(player);
        requireDieValue(value);

        boolean legal = hasLegalMove(player, value);

        Outcome outcome = rules.recordRoll(player, value, legal);

        // Only a legal-move roll stays pending.
        pendingRoll =
                outcome == Outcome.MOVE_REQUIRED ? value : 0;

        return outcome;
    }

    /**
     * Moves one piece by the whole pending roll. The roll is
     * then used up: a second piece cannot move, and the steps
     * cannot be split.
     *
     * @param player the active player
     * @param piece  one of {@link #getSelectablePieces()}
     * @return BONUS_ROLL after a six, otherwise TURN_PASSED
     * @throws IllegalStateException if it is not the player's
     *         turn or no move is pending
     * @throws IllegalArgumentException if the piece is null,
     *         not the player's own, or not legal for the roll
     *         (a rejected call changes nothing)
     */
    public Outcome movePiece(PlayerEntry player, Piece piece) {

        turnManager.requireActivePlayer(player);

        if (pendingRoll == 0) {
            throw new IllegalStateException(
                    "There is no roll to move with.");
        }

        if (piece == null ||
                !pieces.get(player.color()).contains(piece)) {
            throw new IllegalArgumentException(
                    "You can only move your own pieces.");
        }

        // Validation, using up the roll, moving and capture all
        // run through the shared template (see PieceMove).
        ForwardMove move = new ForwardMove(this, player, pendingRoll);
        move.execute(piece);

        return move.outcome();
    }

    // ------------------------------------------------------
    // Internal rules
    // ------------------------------------------------------

    boolean isLegal(Piece piece, int steps) {

        return switch (piece.getState()) {
            case FINISHED -> false;
            case CASTLE ->
                    castleRules.canRelease(steps, true);
            case TRACK, HOME_PATH ->
                    piece.getProgress() + steps <=
                            getFinishSteps(piece.getColor());
        };
    }

    // ------------------------------------------------------
    // Shared steps of every move (called from PieceMove)
    // ------------------------------------------------------

    /**
     * Uses up the pending roll when a forward move is accepted.
     * Nothing has changed yet; this is the last check.
     */
    Outcome consumePendingRoll(PlayerEntry player) {
        Outcome outcome = rules.completeMove(player);
        pendingRoll = 0;
        return outcome;
    }

    /** CastleReleaseRules confirms a release ends on the start square. */
    void requireReleaseOnStartSquare(PlayerColor color, int steps) {

        int square = castleRules.getReleaseSquare(color, steps, true);

        if (square != board.getStartSquare(color)) {
            throw new IllegalStateException(
                    "Release square is not the start.");
        }
    }

    /** Puts the piece on a progress and sets its matching state. */
    void placeAt(Piece piece, int progress) {

        PlayerColor color = piece.getColor();

        Piece.State state;

        if (progress >= getFinishSteps(color)) {
            state = Piece.State.FINISHED;
        } else if (progress > entrySteps.get(color)) {
            state = Piece.State.HOME_PATH;
        } else {
            state = Piece.State.TRACK;
        }

        piece.moveTo(progress, state);
    }

    /** Keeps the move as the game's last move. */
    MoveResult record(MoveResult result) {
        lastMove = result;
        return result;
    }

    /**
     * FR-5.4: if the piece that just landed stands on an
     * unprotected square occupied by exactly one opponent
     * piece, that piece returns to its castle.
     *
     * This is called after every move, so later movement
     * (backward moves, question and surprise effects) can reuse
     * it instead of copying the rule. Only the landing square
     * matters: pieces passed on the way are not captured.
     *
     * Not handled here (other owners): Double Blocks. Two or
     * more opponent pieces on one square are never captured by
     * this rule; the block rules (FR-5.5 to FR-5.8) decide
     * whether a piece may land there at all.
     *
     * @param mover the piece that just moved
     * @return the captured piece, or empty if nothing happened
     */
    Optional<Piece> resolveCapture(Piece mover) {

        if (mover.isInCastle() || mover.isFinished()) {
            return Optional.empty();
        }

        BoardCell landing = getBoardCell(mover).orElseThrow();

        if (isProtectedSquare(landing)) {
            return Optional.empty();
        }

        List<Piece> opponentsThere = new ArrayList<>();

        for (Map.Entry<PlayerColor, List<Piece>> entry :
                pieces.entrySet()) {

            if (entry.getKey() == mover.getColor()) {
                continue;
            }

            for (Piece other : entry.getValue()) {
                if (!other.isInCastle() && !other.isFinished() &&
                        landing.equals(
                                getBoardCell(other).orElseThrow())) {
                    opponentsThere.add(other);
                }
            }
        }

        if (opponentsThere.size() != 1) {
            return Optional.empty();
        }

        Piece victim = opponentsThere.get(0);
        victim.moveTo(Piece.CASTLE_PROGRESS, Piece.State.CASTLE);
        return Optional.of(victim);
    }

    // Reads the arrow and home path from EasyBoard instead of
    // assuming a distance, and rejects inconsistent geometry.
    private int verifyAndGetEntrySteps(PlayerColor color) {

        List<BoardCell> track = board.getSharedTrackCells();
        BoardCell arrow = board.getArrowSquare(color);
        int arrowIndex = track.indexOf(arrow);

        if (arrowIndex < 0) {
            throw new IllegalStateException(
                    color + " arrow is not on the shared track.");
        }

        List<BoardCell> home = board.getHomePathCells(color);

        if (home.size() != EasyBoard.HOME_PATH_SIZE ||
                !touches(arrow, home.get(0))) {
            throw new IllegalStateException(
                    color + " home path does not start " +
                    "next to its arrow.");
        }

        for (int i = 1; i < home.size(); i++) {
            if (!touches(home.get(i - 1), home.get(i))) {
                throw new IllegalStateException(
                        color + " home path is not connected.");
            }
        }

        return Math.floorMod(
                arrowIndex - board.getStartSquare(color),
                EasyBoard.TRACK_SIZE);
    }

    // Orthogonal neighbours (arrow -> home path).
    private static boolean touches(BoardCell a, BoardCell b) {
        return Math.abs(a.row() - b.row()) +
                Math.abs(a.col() - b.col()) == 1;
    }

    private PlayerEntry requireKnown(PlayerEntry player) {

        if (player == null ||
                !turnManager.getPlayers().contains(player)) {
            throw new IllegalArgumentException("Unknown player.");
        }

        return player;
    }

    private void requireOwnedPiece(Piece piece) {

        if (piece == null) {
            throw new IllegalArgumentException(
                    "Piece cannot be null.");
        }

        List<Piece> own = pieces.get(piece.getColor());

        if (own == null || !own.contains(piece)) {
            throw new IllegalArgumentException(
                    "Piece does not belong to this game.");
        }
    }

    private static void requireDieValue(int value) {

        if (value < 1 || value > TurnRollRules.BONUS_VALUE) {
            throw new IllegalArgumentException(
                    "Die value must be between 1 and 6.");
        }
    }
}
