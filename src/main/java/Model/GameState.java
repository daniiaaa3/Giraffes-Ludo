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
 * Iteration 2: supports Easy, Medium and Hard board geometry.
 * The existing turn, movement, capture and backward-movement logic is retained.
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
    private final Set<BoardCell> fixedProtectedSquares = new HashSet<>();
    private final ProtectedSquares additionalProtection;

    // The ordinary Steps die is kept separate from the Boost/UltraBoost result.
    // TurnRollRules must only ever see the ordinary 1..6 value.
    private int pendingRoll = 0;
    private int pendingSpecialDie = 0; // 0 means legacy / Easy roll
    private MoveResult lastMove;

    /** Existing Iteration 1 constructor: an ordinary Easy match. */
    public GameState(List<PlayerEntry> players, Random random) {
        this(players, random, ProtectedSquares.none());
    }

    /** Existing Iteration 1 constructor with additional protected squares. */
    public GameState(List<PlayerEntry> players, Random random,
            ProtectedSquares additionalProtection) {
        this(new EasyBoard(), new TurnManager(players, random),
                additionalProtection);
    }

    /** Existing constructor used by older tests and integration code. */
    public GameState(EasyBoard board, TurnManager turnManager) {
        this(board, turnManager, ProtectedSquares.none());
    }

    /** Existing constructor: preserves custom EasyBoard test doubles. */
    public GameState(EasyBoard board, TurnManager turnManager,
            ProtectedSquares additionalProtection) {
        this(board, createEasyTopology(board), turnManager,
                additionalProtection);
    }

    /**
     * Iteration 2 factory: explicitly creates a match for the chosen level.
     * A named factory avoids ambiguous old constructor calls using null.
     */
    public static GameState forDifficulty(List<PlayerEntry> players,
            Random random, GameDifficulty difficulty) {
        if (difficulty == null) {
            throw new IllegalArgumentException("Difficulty cannot be null.");
        }
        if (difficulty == GameDifficulty.EASY) {
            return new GameState(players, random);
        }
        return new GameState(new EasyBoard(),
                new GameBoardTopology(difficulty),
                new TurnManager(players, random), ProtectedSquares.none());
    }

    private static GameBoardTopology createEasyTopology(EasyBoard board) {
        if (board == null) {
            throw new IllegalArgumentException(
                    "Board, turn manager and protection cannot be null.");
        }
        return new GameBoardTopology(GameDifficulty.EASY, board);
    }

    private GameState(EasyBoard board, GameBoardTopology topology,
            TurnManager turnManager, ProtectedSquares additionalProtection) {
        if (board == null || topology == null || turnManager == null
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

    public GameDifficulty getDifficulty() {
        return topology.getDifficulty();
    }

    public GameBoardTopology getBoardTopology() {
        return topology;
    }

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

    /** Value of the pending Boost or UltraBoost die; 0 for ordinary rolls. */
    public int getPendingSpecialDie() {
        return pendingSpecialDie;
    }

    public List<Piece> getPieces(PlayerColor color) {
        List<Piece> own = color == null ? null : pieces.get(color);
        if (own == null) {
            throw new IllegalArgumentException("No player uses this color.");
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

    public Optional<BoardCell> getBoardCell(Piece piece) {
        requireOwnedPiece(piece);
        if (piece.isInCastle()) {
            return Optional.empty();
        }
        return getBoardCellAt(piece.getColor(), piece.getProgress());
    }

    /** All movement and animation coordinates now come from the chosen board. */
    public Optional<BoardCell> getBoardCellAt(PlayerColor color, int progress) {
        getPieces(color);
        return topology.getCellAt(color, progress);
    }

    public boolean isProtectedSquare(BoardCell cell) {
        return cell != null && (
                fixedProtectedSquares.contains(cell)
                || topology.isProtectedSquare(cell)
                || additionalProtection.isProtected(cell));
    }

    public Optional<MoveResult> getLastMoveResult() {
        return Optional.ofNullable(lastMove);
    }

    public boolean canMoveBackward(Piece piece) {
        requireOwnedPiece(piece);
        return piece.getState() == Piece.State.TRACK
                || piece.getState() == Piece.State.HOME_PATH;
    }

    public MoveResult moveBackward(Piece piece, int steps) {
        return new BackwardMove(this, steps).execute(piece);
    }


    /**
     * Applies the movement reward or penalty from a question.
     * This action does not change the active player or pending dice.
     */
    public MoveResult applyQuestionEffect(Piece piece, int movementSteps) {
        if (movementSteps == 0 || movementSteps < -3 || movementSteps > 3) {
            throw new IllegalArgumentException(
                    "Question movement must be between -3 and 3, excluding 0.");
        }

        if (movementSteps < 0) {
            return moveBackward(piece, -movementSteps);
        }

        return new QuestionForwardMove(this, movementSteps).execute(piece);
    }

    /**
     * Checks whether a piece can move forward due to an effect,
     * without going beyond its finish square.
     */
    public boolean canMoveForward(Piece piece, int steps) {
        requireOwnedPiece(piece);

        return steps >= 0
                && (piece.getState() == Piece.State.TRACK
                    || piece.getState() == Piece.State.HOME_PATH)
                && steps <= getFinishSteps(piece.getColor())
                        - piece.getProgress();
    }

    /**
     * Moves a piece forward due to an effect without consuming a roll.
     */
    public MoveResult moveForward(Piece piece, int steps) {
        return new EffectForwardMove(this, steps).execute(piece);
    }

    /**
     * Applies a signed movement effect.
     * Positive values move forward; negative values move backward.
     */
    public MoveResult moveByEffect(Piece piece, int signedSteps) {
        return signedSteps < 0
                ? moveBackward(piece, -signedSteps)
                : moveForward(piece, signedSteps);
    }



     public List<Piece> getLegalPieces(
        PlayerEntry player, int steps) {

    return getLegalPiecesWithBlocks(player, steps, 0);
     }

/**
 * FR-5.7: Prefer full moves over partial moves
 * stopped by an opponent Double Block.
 *
 * Supports normal and advanced dice.
 */
private List<Piece> getLegalPiecesWithBlocks(
        PlayerEntry player,
        int steps,
        int specialDieValue) {

    PlayerColor color = requireKnown(player).color();
    requireDieValue(steps);

    List<Piece> fullMoves = new ArrayList<>();
    List<Piece> partialMoves = new ArrayList<>();

    for (Piece piece : pieces.get(color)) {

        // Preserve existing movement validation.
        if (!isLegalWithDice(piece, steps, specialDieValue)) {
            continue;
        }

        // Full movement has priority.
        if (canCompleteFullMove(
                piece, steps, specialDieValue)) {

            fullMoves.add(piece);
            continue;
        }

        // Castle pieces cannot move partially.
        if (piece.isInCastle()) {
            continue;
        }

        int target = piece.getProgress()
                + movementDistance(
                        piece, steps, specialDieValue);

        DoubleBlockRules.MoveDecision decision =
                DoubleBlockGameAdapter.evaluateMovement(
                        this, piece, target, false);

        if (decision.status()
                == DoubleBlockRules.MoveStatus.STOP_BEFORE_BLOCK) {

            partialMoves.add(piece);
        }
    }

    if (!fullMoves.isEmpty()) {
        return List.copyOf(fullMoves);
    }

    return List.copyOf(partialMoves);
}


    public boolean hasLegalMove(PlayerEntry player, int steps) {
        return !getLegalPieces(player, steps).isEmpty();
    }

    /**
     * Preview legal moves for BOTH dice. The Steps die still controls
     * castle release and three-sixes turn rules.
     */

public List<Piece> getLegalPieces(
        PlayerEntry player, int steps, int specialDieValue) {

    requireKnown(player);
    requireDieValue(steps);
    requireAdvancedDice(specialDieValue);

    return getLegalPiecesWithBlocks(
            player, steps, specialDieValue);
}

    public boolean hasLegalMove(
            PlayerEntry player, int steps, int specialDieValue) {
        return !getLegalPieces(player, steps, specialDieValue).isEmpty();
    }

    public List<Piece> getSelectablePieces() {
        if (pendingRoll == 0) {
            return List.of();
        }
        if (pendingSpecialDie != 0) {
            return getLegalPieces(
                    getActivePlayer(), pendingRoll, pendingSpecialDie);
        }
        return getLegalPieces(getActivePlayer(), pendingRoll);
    }

    /** Legacy Easy / one-die API retained for Iteration 1 and old tests. */
    public Outcome recordRoll(PlayerEntry player, int value) {
        turnManager.requireActivePlayer(player);
        requireDieValue(value);

        boolean legal = hasLegalMove(player, value);
        Outcome outcome = rules.recordRoll(player, value, legal);
        pendingRoll = outcome == Outcome.MOVE_REQUIRED ? value : 0;
        pendingSpecialDie = 0;
        return outcome;
    }

    /**
     * Records a Medium/Hard roll without confusing movement distance
     * with the ordinary Steps die value used by TurnRollRules.
     *
     * A Boost/UltraBoost value of 1 may yield zero track movement,
     * while a Steps 6 can still release a castle piece.
     */
    public Outcome recordAdvancedRoll(
            PlayerEntry player, int steps, int specialDieValue) {
        turnManager.requireActivePlayer(player);
        requireDieValue(steps);
        requireAdvancedDice(specialDieValue);

        boolean legal = hasLegalMove(player, steps, specialDieValue);
        Outcome outcome = rules.recordRoll(player, steps, legal);
        pendingRoll = outcome == Outcome.MOVE_REQUIRED ? steps : 0;
        pendingSpecialDie = outcome == Outcome.MOVE_REQUIRED
                ? specialDieValue : 0;
        return outcome;
    }

    /** Validate a special die before changing any turn or pending state. */
    private void requireAdvancedDice(int specialDieValue) {
        if (getDifficulty() == GameDifficulty.EASY) {
            throw new IllegalStateException(
                    "Easy difficulty has no Boost/UltraBoost die.");
        }
        AdvancedDiceMovementCalculator.calculateTrackSteps(
                getDifficulty(), 1, specialDieValue);
    }

    public Outcome movePiece(PlayerEntry player, Piece piece) {
        turnManager.requireActivePlayer(player);
        if (pendingRoll == 0) {
            throw new IllegalStateException("There is no roll to move with.");
        }
        if (piece == null || !pieces.get(player.color()).contains(piece)) {
            throw new IllegalArgumentException(
                    "You can only move your own pieces.");
        }

        ForwardMove move = new ForwardMove(
                this, player, pendingRoll, pendingSpecialDie);
        move.execute(piece);
        return move.outcome();
    }

/**
 * FR-5.6: An opponent Double Block on the start
 * square prevents a piece from leaving the castle.
 */
private boolean canReleaseWithoutOpponentBlock(
        PlayerColor color, int steps) {

    if (!castleRules.canRelease(steps, true)) {
        return false;
    }

    BoardCell startSquare =
            topology.getCellAt(color, 0).orElseThrow();

    return DoubleBlockRules.canReleaseOnStart(
            DoubleBlockGameAdapter.getBoardPositions(this),
            color,
            startSquare
    );
}


    /**
     * Legal move after both dice have been rolled. Never treat a zero
     * track-distance roll as a move; castle release is the exception.
     */
    boolean isLegalWithDice(Piece piece, int steps, int specialDieValue) {
        if (specialDieValue == 0) {
            return isLegal(piece, steps);
        }
        return switch (piece.getState()) {
            case FINISHED -> false;
            case CASTLE -> canReleaseWithoutOpponentBlock(
                    piece.getColor(), steps);
            case TRACK, HOME_PATH -> {
                int distance = movementDistance(
                        piece, steps, specialDieValue);
                yield distance > 0 && piece.getProgress() + distance
                        <= getFinishSteps(piece.getColor());
            }
        };
    }

    /** Distance for a selected piece, based on WHERE it begins its move. */
    int movementDistance(Piece piece, int steps, int specialDieValue) {
        if (piece.isInCastle()) {
            return 0; // Release always lands on the # start square.
        }
        if (specialDieValue == 0) {
            return steps; // Legacy Easy/one-die movement.
        }
        if (piece.getState() == Piece.State.HOME_PATH) {
            return AdvancedDiceMovementCalculator.calculateHomeSteps(steps);
        }
        return AdvancedDiceMovementCalculator.calculateTrackSteps(
                getDifficulty(), steps, specialDieValue);
    }

/**
 * FR-5.7: Checks whether a piece can complete
 * its entire movement without crossing an
 * opponent's Double Block.
 *
 * Supports Easy, Medium and Hard dice.
 */
boolean canCompleteFullMove(
        Piece piece, int steps, int specialDieValue) {

    // First verify the ordinary movement rules.
    if (!isLegalWithDice(piece, steps, specialDieValue)) {
        return false;
    }

    // Castle release is already checked by
    // isLegalWithDice, including start-square blocks.
    if (piece.isInCastle()) {
        return true;
    }

    int target = piece.getProgress()
            + movementDistance(piece, steps, specialDieValue);

    DoubleBlockRules.MoveDecision decision =
            DoubleBlockGameAdapter.evaluateMovement(
                    this,
                    piece,
                    target,
                    false
            );

    return decision.status()
            == DoubleBlockRules.MoveStatus.FULL_MOVE;
}


    boolean isLegal(Piece piece, int steps) {
        return switch (piece.getState()) {
            case FINISHED -> false;
            case CASTLE -> canReleaseWithoutOpponentBlock(
                    piece.getColor(), steps);
            case TRACK, HOME_PATH -> piece.getProgress() + steps
                    <= getFinishSteps(piece.getColor());
        };
    }

    Outcome consumePendingRoll(PlayerEntry player) {
        Outcome outcome = rules.completeMove(player);
        pendingRoll = 0;
        pendingSpecialDie = 0;
        return outcome;
    }

    /** Release only on a six; progress zero is the selected board's start. */
    void requireReleaseOnStartSquare(PlayerColor color, int steps) {
        if (!castleRules.canRelease(steps, true)) {
            throw new IllegalStateException("Piece cannot leave the castle.");
        }
        if (topology.getDifficulty() == GameDifficulty.EASY) {
            int square = castleRules.getReleaseSquare(color, steps, true);
            BoardCell expectedStart = board.getSharedTrackCell(square);
            BoardCell actualStart = topology.getCellAt(color, 0).orElseThrow();
            if (!expectedStart.equals(actualStart)) {
                throw new IllegalStateException(
                        "Release square is not the start.");
            }
        } else {
            // Medium/Hard start squares are encoded in their own board paths.
            topology.getCellAt(color, 0).orElseThrow();
        }
    }

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

    /** Retained capture rule, using coordinates of the selected board. */
    Optional<Piece> resolveCapture(Piece mover) {
        if (mover.isInCastle() || mover.isFinished()) {
            return Optional.empty();
        }
        BoardCell landing = getBoardCell(mover).orElseThrow();
        if (isProtectedSquare(landing)) {
            return Optional.empty();
        }

        List<Piece> opponentsThere = new ArrayList<>();
        for (Map.Entry<PlayerColor, List<Piece>> entry : pieces.entrySet()) {
            if (entry.getKey() == mover.getColor()) {
                continue;
            }
            for (Piece other : entry.getValue()) {
                if (!other.isInCastle() && !other.isFinished()
                        && landing.equals(getBoardCell(other).orElseThrow())) {
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

    /** Preserves Iteration 1 validation of the original Easy geometry. */
    private int verifyAndGetEntrySteps(PlayerColor color) {
        List<BoardCell> track = board.getSharedTrackCells();
        BoardCell arrow = board.getArrowSquare(color);
        int arrowIndex = track.indexOf(arrow);
        if (arrowIndex < 0) {
            throw new IllegalStateException(
                    color + " arrow is not on the shared track.");
        }

        List<BoardCell> home = board.getHomePathCells(color);
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
        return Math.floorMod(arrowIndex - board.getStartSquare(color),
                EasyBoard.TRACK_SIZE);
    }

    private static boolean touches(BoardCell a, BoardCell b) {
        return Math.abs(a.row() - b.row())
                + Math.abs(a.col() - b.col()) == 1;
    }

    private PlayerEntry requireKnown(PlayerEntry player) {
        if (player == null || !turnManager.getPlayers().contains(player)) {
            throw new IllegalArgumentException("Unknown player.");
        }
        return player;
    }

    private void requireOwnedPiece(Piece piece) {
        if (piece == null) {
            throw new IllegalArgumentException("Piece cannot be null.");
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
