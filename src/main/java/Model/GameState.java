
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
 * State and rules for one Ludo match.
 *
 * Iteration 2: supports Easy, Medium and Hard board geometry.
 * Retains the existing turn, movement, capture and
 * backward-movement logic.
 *
 * MVC Component: Model
 */
public class GameState {

    private final EasyBoard board;
    private final GameBoardTopology topology;
    private final CastleReleaseRules castleRules;
    private final TurnManager turnManager;
    private final TurnRollRules rules;

    private final Map<PlayerColor, List<Piece>> pieces =
            new EnumMap<>(PlayerColor.class);

    private final Map<PlayerColor, Integer> entrySteps =
            new EnumMap<>(PlayerColor.class);

    private final Set<BoardCell> fixedProtectedSquares =
            new HashSet<>();

    private final ProtectedSquares additionalProtection;

    private int pendingRoll = 0;
    private MoveResult lastMove;

    /**
     * Existing Iteration 1 constructor.
     * Starts an Easy game.
     */
    public GameState(List<PlayerEntry> players, Random random) {
        this(players, random, ProtectedSquares.none());
    }

    /**
     * Existing constructor with additional protected squares.
     */
    public GameState(
            List<PlayerEntry> players,
            Random random,
            ProtectedSquares additionalProtection) {

        this(new EasyBoard(),
                new TurnManager(players, random),
                additionalProtection);
    }

    /**
     * Existing constructor used by previous tests.
     */
    public GameState(EasyBoard board, TurnManager turnManager) {
        this(board, turnManager, ProtectedSquares.none());
    }

    /**
     * Existing constructor with custom EasyBoard.
     */
    public GameState(
            EasyBoard board,
            TurnManager turnManager,
            ProtectedSquares additionalProtection) {

        this(board,
                createEasyTopology(board),
                turnManager,
                additionalProtection);
    }

    /**
     * Iteration 2:
     * Creates a game according to the selected difficulty.
     */
    public static GameState forDifficulty(
            List<PlayerEntry> players,
            Random random,
            GameDifficulty difficulty) {

        if (difficulty == null) {
            throw new IllegalArgumentException(
                    "Difficulty cannot be null.");
        }

        if (difficulty == GameDifficulty.EASY) {
            return new GameState(players, random);
        }

        return new GameState(
                new EasyBoard(),
                new GameBoardTopology(difficulty),
                new TurnManager(players, random),
                ProtectedSquares.none());
    }

    private static GameBoardTopology createEasyTopology(
            EasyBoard board) {

        if (board == null) {
            throw new IllegalArgumentException(
                    "Board, turn manager and protection cannot be null.");
        }

        return new GameBoardTopology(
                GameDifficulty.EASY, board);
    }

    /**
     * Common game initialization for every difficulty.
     */
    private GameState(
            EasyBoard board,
            GameBoardTopology topology,
            TurnManager turnManager,
            ProtectedSquares additionalProtection) {

        if (board == null || topology == null
                || turnManager == null
                || additionalProtection == null) {
            throw new IllegalArgumentException(
                    "Board, turn manager and protection cannot be null.");
        }

        this.board = board;
        this.topology = topology;
        this.castleRules = new CastleReleaseRules(board);
        this.turnManager = turnManager;
        this.rules = new TurnRollRules(turnManager);
        this.additionalProtection = additionalProtection;

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

            int entry = topology.getEntrySteps(color);

            if (topology.getDifficulty() == GameDifficulty.EASY
                    && entry != verifyAndGetEntrySteps(color)) {
                throw new IllegalStateException(
                        "Easy board path is inconsistent for " + color);
            }

            entrySteps.put(color, entry);
        }

        for (PlayerColor color : PlayerColor.values()) {
            List<BoardCell> path = topology.getPlayerPath(color);
            fixedProtectedSquares.add(path.get(0));
            fixedProtectedSquares.add(path.get(path.size() - 1));
        }
    }

    // ------------------------------------------------
    // Difficulty and board information
    // ------------------------------------------------

    public GameDifficulty getDifficulty() {
        return topology.getDifficulty();
    }

    public GameBoardTopology getBoardTopology() {
        return topology;
    }

    // ------------------------------------------------
    // Player and game state queries
    // ------------------------------------------------

    public List<PlayerEntry> getPlayers() {
        return turnManager.getPlayers();
    }

    public PlayerEntry getActivePlayer() {
        return rules.getActivePlayer();
    }

    public boolean isAwaitingRoll() {
        return rules.isAwaitingRoll();
    }

    public boolean isAwaitingMove() {
        return rules.isAwaitingMove();
    }

    public int getConsecutiveSixes() {
        return rules.getConsecutiveSixes();
    }

    public int getPendingRoll() {
        return pendingRoll;
    }

    public List<Piece> getPieces(PlayerColor color) {

        List<Piece> own =
                color == null ? null : pieces.get(color);

        if (own == null) {
            throw new IllegalArgumentException(
                    "No player uses this color.");
        }

        return own;
    }

    public List<Piece> getPieces(PlayerEntry player) {
        return getPieces(requireKnown(player).color());
    }

    public int getEntrySteps(PlayerColor color) {
        getPieces(color);
        return entrySteps.get(color);
    }

    public int getFinishSteps(PlayerColor color) {
        getPieces(color);
        return topology.getFinishSteps(color);
    }

    // ------------------------------------------------
    // Board coordinates
    // ------------------------------------------------

    public Optional<BoardCell> getBoardCell(Piece piece) {

        requireOwnedPiece(piece);

        if (piece.isInCastle()) {
            return Optional.empty();
        }

        return getBoardCellAt(
                piece.getColor(), piece.getProgress());
    }

    /**
     * Returns coordinates from the selected board topology.
     */
    public Optional<BoardCell> getBoardCellAt(
            PlayerColor color, int progress) {

        getPieces(color);
        return topology.getCellAt(color, progress);
    }

    // ------------------------------------------------
    // Protected squares and capture
    // ------------------------------------------------

    public boolean isProtectedSquare(BoardCell cell) {

        return cell != null && (
                fixedProtectedSquares.contains(cell)
                || topology.isProtectedSquare(cell)
                || additionalProtection.isProtected(cell));
    }

    public Optional<MoveResult> getLastMoveResult() {
        return Optional.ofNullable(lastMove);
    }

    // ------------------------------------------------
    // Backward movement
    // ------------------------------------------------

    public boolean canMoveBackward(Piece piece) {

        requireOwnedPiece(piece);

        return piece.getState() == Piece.State.TRACK
                || piece.getState() == Piece.State.HOME_PATH;
    }

    public MoveResult moveBackward(Piece piece, int steps) {
        return new BackwardMove(this, steps).execute(piece);
    }

    // ------------------------------------------------
    // Legal piece selection
    // ------------------------------------------------

    public List<Piece> getLegalPieces(
            PlayerEntry player, int steps) {

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

    public boolean hasLegalMove(
            PlayerEntry player, int steps) {

        return !getLegalPieces(player, steps).isEmpty();
    }

    public List<Piece> getSelectablePieces() {

        if (pendingRoll == 0) {
            return List.of();
        }

        return getLegalPieces(
                getActivePlayer(), pendingRoll);
    }

    // ------------------------------------------------
    // Dice and turns
    // ------------------------------------------------

    public Outcome recordRoll(
            PlayerEntry player, int value) {

        turnManager.requireActivePlayer(player);
        requireDieValue(value);

        boolean legal = hasLegalMove(player, value);

        Outcome outcome =
                rules.recordRoll(player, value, legal);

        pendingRoll =
                outcome == Outcome.MOVE_REQUIRED ? value : 0;

        return outcome;
    }

    public Outcome movePiece(
            PlayerEntry player, Piece piece) {

        turnManager.requireActivePlayer(player);

        if (pendingRoll == 0) {
            throw new IllegalStateException(
                    "There is no roll to move with.");
        }

        if (piece == null
                || !pieces.get(player.color()).contains(piece)) {
            throw new IllegalArgumentException(
                    "You can only move your own pieces.");
        }

        ForwardMove move =
                new ForwardMove(this, player, pendingRoll);

        move.execute(piece);

        return move.outcome();
    }

    boolean isLegal(Piece piece, int steps) {

        return switch (piece.getState()) {

            case FINISHED -> false;

            case CASTLE ->
                    castleRules.canRelease(steps, true);

            case TRACK, HOME_PATH ->
                    piece.getProgress() + steps
                            <= getFinishSteps(piece.getColor());
        };
    }

    Outcome consumePendingRoll(PlayerEntry player) {

        Outcome outcome = rules.completeMove(player);
        pendingRoll = 0;

        return outcome;
    }

    /**
     * A piece is released only on a 6.
     * Its position is the starting square of its selected board.
     */
    void requireReleaseOnStartSquare(
            PlayerColor color, int steps) {

        if (!castleRules.canRelease(steps, true)) {
            throw new IllegalStateException(
                    "Piece cannot leave the castle.");
        }

        if (topology.getDifficulty() == GameDifficulty.EASY) {

            int square = castleRules.getReleaseSquare(
                    color, steps, true);

            BoardCell expectedStart =
                    board.getSharedTrackCell(square);

            BoardCell actualStart =
                    topology.getCellAt(color, 0).orElseThrow();

            if (!expectedStart.equals(actualStart)) {
                throw new IllegalStateException(
                        "Release square is not the start.");
            }

        } else {
            topology.getCellAt(color, 0).orElseThrow();
        }
    }

    // ------------------------------------------------
    // Piece placement and movement results
    // ------------------------------------------------

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

    MoveResult record(MoveResult result) {
        lastMove = result;
        return result;
    }

    /**
     * Capture rule:
     * Landing on an unprotected square occupied
     * by exactly one opponent captures that piece.
     */
    Optional<Piece> resolveCapture(Piece mover) {

        if (mover.isInCastle() || mover.isFinished()) {
            return Optional.empty();
        }

        BoardCell landing =
                getBoardCell(mover).orElseThrow();

        if (isProtectedSquare(landing)) {
            return Optional.empty();
        }

        List<Piece> opponentsThere = new ArrayList<>();

        for (Map.Entry<PlayerColor, List<Piece>> entry
                : pieces.entrySet()) {

            if (entry.getKey() == mover.getColor()) {
                continue;
            }

            for (Piece other : entry.getValue()) {

                if (!other.isInCastle()
                        && !other.isFinished()
                        && landing.equals(
                                getBoardCell(other).orElseThrow())) {

                    opponentsThere.add(other);
                }
            }
        }

        if (opponentsThere.size() != 1) {
            return Optional.empty();
        }

        Piece victim = opponentsThere.get(0);

        victim.moveTo(
                Piece.CASTLE_PROGRESS, Piece.State.CASTLE);

        return Optional.of(victim);
    }

    // ------------------------------------------------
    // Easy board geometry validation
    // ------------------------------------------------

    private int verifyAndGetEntrySteps(PlayerColor color) {

        List<BoardCell> track =
                board.getSharedTrackCells();

        BoardCell arrow =
                board.getArrowSquare(color);

        int arrowIndex = track.indexOf(arrow);

        if (arrowIndex < 0) {
            throw new IllegalStateException(
                    color + " arrow is not on the shared track.");
        }

        List<BoardCell> home =
                board.getHomePathCells(color);

        if (home.size() != EasyBoard.HOME_PATH_SIZE
                || !touches(arrow, home.get(0))) {

            throw new IllegalStateException(
                    color + " home path does not start next to its arrow.");
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

    private static boolean touches(
            BoardCell a, BoardCell b) {

        return Math.abs(a.row() - b.row())
                + Math.abs(a.col() - b.col()) == 1;
    }

    // ------------------------------------------------
    // Validation helpers
    // ------------------------------------------------

    private PlayerEntry requireKnown(PlayerEntry player) {

        if (player == null
                || !turnManager.getPlayers().contains(player)) {

            throw new IllegalArgumentException(
                    "Unknown player.");
        }

        return player;
    }

    private void requireOwnedPiece(Piece piece) {

        if (piece == null) {
            throw new IllegalArgumentException(
                    "Piece cannot be null.");
        }

        List<Piece> own =
                pieces.get(piece.getColor());

        if (own == null || !own.contains(piece)) {
            throw new IllegalArgumentException(
                    "Piece does not belong to this game.");
        }
    }

    private static void requireDieValue(int value) {

        if (value < 1
                || value > TurnRollRules.BONUS_VALUE) {

            throw new IllegalArgumentException(
                    "Die value must be between 1 and 6.");
        }
    }
}
