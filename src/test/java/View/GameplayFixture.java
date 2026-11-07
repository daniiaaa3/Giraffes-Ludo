package View;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import Controller.GameplayController;
import Model.EasyBoard.BoardCell;
import Model.GameSetup.PlayerColor;
import Model.GameSetup.PlayerEntry;
import Model.GameState;
import Model.Piece;
import Model.StepsDice;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.paint.RadialGradient;
import javafx.scene.shape.Circle;

/**
 * A real GameplayView + GameplayController + GameState, wired
 * exactly like LudoApplication wires them, with helpers that
 * act like a user (roll, click a highlighted piece) and read
 * back what the board actually displays.
 */
final class GameplayFixture {

    static final List<PlayerEntry> PLAYERS = List.of(
            new PlayerEntry("Dania", PlayerColor.RED),
            new PlayerEntry("Marwa", PlayerColor.BLUE),
            new PlayerEntry("Nay", PlayerColor.GREEN),
            new PlayerEntry("Yara", PlayerColor.YELLOW));

    static final List<Throwable> FX_ERRORS = new CopyOnWriteArrayList<>();

    final Scene scene;
    final GameplayView view;
    final GameState state;
    final GameplayController controller;
    final int playerCount;
    int menuRequests;

    /** Dice and first-player randomness taken from one seed. */
    GameplayFixture(long seed, int playerCount) throws Exception {
        this(new Random(seed * 0x9E3779B97F4A7C15L), new Random(seed * 31 + 7), playerCount);
    }

    /** Dice that return the scripted values first, then random ones. */
    static final class ScriptedRandom extends Random {
        private final java.util.ArrayDeque<Integer> queue =
                new java.util.ArrayDeque<>();

        ScriptedRandom(int... values) {
            super(12345);
            for (int v : values) {
                queue.add(v);
            }
        }

        void add(int... values) {
            for (int v : values) {
                queue.add(v);
            }
        }

        @Override
        public int nextInt(int bound) {
            return queue.isEmpty() ? super.nextInt(bound) : queue.poll() - 1;
        }
    }

    /** A Random that always picks the first player. */
    static Random firstSeat() {
        return new Random() {
            @Override
            public int nextInt(int bound) {
                return 0;
            }
        };
    }

    GameplayFixture(Random firstPlayerRandom, Random diceRandom,
                    int playerCount) throws Exception {
        this.playerCount = playerCount;
        Object[] built = FxSupport.fx(() -> {
            Thread.currentThread().setUncaughtExceptionHandler(
                    (t, e) -> FX_ERRORS.add(e));
            GameState gs = new GameState(PLAYERS.subList(0, playerCount),
                    firstPlayerRandom);
            GameplayView gv = new GameplayView();
            return new Object[] {gs, gv};
        });
        this.state = (GameState) built[0];
        this.view = (GameplayView) built[1];
        GameplayController[] holder = new GameplayController[1];
        this.scene = FxSupport.fx(() -> {
            holder[0] = new GameplayController(view, state,
                    new StepsDice(diceRandom), () -> menuRequests++);
            Scene s = new Scene(view.getRoot(), 1280, 720);
            s.getStylesheets().add(GameplayFixture.class
                    .getResource("/styles/game.css").toExternalForm());
            view.startTimer();
            return s;
        });
        this.controller = holder[0];
        FxSupport.settle(scene);
    }

    // ----------------------------------------------------- node lookups
    EasyBoardView boardView() {
        return (EasyBoardView) FxSupport.find(scene.getRoot(),
                n -> n instanceof EasyBoardView).get(0);
    }

    DiceView diceView() {
        return (DiceView) FxSupport.find(scene.getRoot(),
                n -> n instanceof DiceView).get(0);
    }

    Button rollButton() {
        return FxSupport.button(scene.getRoot(), "Roll Dice");
    }

    String label(String startsWith) {
        for (Node n : FxSupport.find(scene.getRoot(), x -> x instanceof Label l
                && l.getText() != null && l.getText().startsWith(startsWith))) {
            return ((Label) n).getText();
        }
        return null;
    }

    /** Text of the turn indicator in the top bar. */
    String turnText() {
        for (Node n : FxSupport.find(scene.getRoot(), x -> x instanceof Label l
                && l.getStyleClass().contains("turn-label"))) {
            return ((Label) n).getText();
        }
        return null;
    }

    /** Text of the message bar at the bottom. */
    String message() {
        for (Node n : FxSupport.find(scene.getRoot(), x -> x instanceof Label l
                && l.getStyleClass().contains("message-text"))) {
            return ((Label) n).getText();
        }
        return null;
    }

    /** True while the "Extra roll" badge is shown. */
    boolean extraRollBadgeShown() {
        return !FxSupport.find(scene.getRoot(), x -> x instanceof Label l
                && l.getStyleClass().contains("extra-roll-badge")
                && l.isVisible() && l.isManaged()).isEmpty();
    }

    // ---------------------------------------------------- user actions
    /** Presses Roll Dice and waits for the animation + rule handling. */
    int roll() throws Exception {
        FxSupport.fx(() -> rollButton().fire());
        FxSupport.waitUntil("dice animation", () -> !diceView().isRolling(), 8000);
        waitIdle();
        String text = FxSupport.fx(() -> label("Rolled "));
        return Integer.parseInt(text.substring("Rolled ".length()).trim());
    }

    /** Waits until no dice or piece animation is running. */
    void waitIdle() throws Exception {
        FxSupport.waitUntil("idle", () -> !diceView().isRolling()
                && !view.isAnimating(), 15000);
    }

    /** Clicks the n-th highlighted (selectable) piece like a user. */
    BoardCell clickHighlighted(int n) throws Exception {
        return FxSupport.fx(() -> {
            List<Circle> gold = highlightedCircles();
            Circle c = gold.get(n);
            BoardCell cell = cellOf(c);
            Event.fireEvent(c, mouseClick(c));
            return cell;
        });
    }

    /** Index of the highlighted piece standing on the given cell, or -1. */
    int highlightedIndexAt(BoardCell cell) throws Exception {
        return FxSupport.fx(() -> {
            List<Circle> gold = highlightedCircles();
            for (int i = 0; i < gold.size(); i++) {
                if (cellOf(gold.get(i)).equals(cell)) {
                    return i;
                }
            }
            return -1;
        });
    }

    /** Fires two clicks on the same highlighted piece (a double-click). */
    void doubleClickHighlighted(int n) throws Exception {
        FxSupport.fx(() -> {
            Circle c = highlightedCircles().get(n);
            Event.fireEvent(c, mouseClick(c));
            Event.fireEvent(c, mouseClick(c));
        });
    }

    private static MouseEvent mouseClick(Node target) {
        return new MouseEvent(MouseEvent.MOUSE_CLICKED, 5, 5, 5, 5,
                MouseButton.PRIMARY, 1, false, false, false, false,
                true, false, false, true, false, false, null);
    }

    // ------------------------------------------------- what is displayed
    List<Circle> highlightedCircles() {
        List<Circle> out = new ArrayList<>();
        for (Node n : FxSupport.find(boardView(), x -> x instanceof Circle c
                && Color.GOLD.equals(c.getStroke()))) {
            out.add((Circle) n);
        }
        return out;
    }

    private BoardCell cellOf(Circle c) {
        Node cell = c.getParent().getParent();      // GridPane -> cell
        Integer r = GridPane.getRowIndex(cell);
        Integer col = GridPane.getColumnIndex(cell);
        return new BoardCell(r == null ? 0 : r, col == null ? 0 : col);
    }

    static String colorOf(Circle c) {
        Paint fill = c.getFill();
        Color mid = ((RadialGradient) fill).getStops().get(1).getColor();
        for (PlayerColor pc : PlayerColor.values()) {
            if (Color.web(hex(pc)).equals(mid)) {
                return pc.name();
            }
        }
        return "?" + mid;
    }

    static String hex(PlayerColor c) {
        return switch (c) {
            case RED -> "#E85561";
            case BLUE -> "#4A90E2";
            case GREEN -> "#49B77C";
            case YELLOW -> "#F0C541";
        };
    }

    /** cell -> "COLOR" -> number of displayed pieces. */
    Map<BoardCell, Map<String, Integer>> displayed() {
        Map<BoardCell, Map<String, Integer>> out = new HashMap<>();
        for (Node n : FxSupport.find(boardView(), x -> x instanceof Circle c
                && c.getParent() instanceof GridPane g
                && "pieces".equals(g.getUserData()))) {
            Circle c = (Circle) n;
            out.computeIfAbsent(cellOf(c), k -> new HashMap<>())
               .merge(colorOf(c), 1, Integer::sum);
        }
        return out;
    }

    /** The same map computed from the model. */
    Map<BoardCell, Map<String, Integer>> expected() {
        Map<BoardCell, Map<String, Integer>> out = new HashMap<>();
        for (PlayerEntry p : state.getPlayers()) {
            for (Piece piece : state.getPieces(p)) {
                BoardCell cell = piece.isInCastle()
                        ? castleCell(piece.getColor(), piece.getId())
                        : state.getBoardCell(piece).orElseThrow();
                out.computeIfAbsent(cell, k -> new HashMap<>())
                   .merge(piece.getColor().name(), 1, Integer::sum);
            }
        }
        return out;
    }

    static BoardCell castleCell(PlayerColor color, int id) {
        int baseRow = switch (color) {
            case GREEN, YELLOW -> 0;
            case RED, BLUE -> 9;
        };
        int baseCol = switch (color) {
            case GREEN, RED -> 0;
            case YELLOW, BLUE -> 9;
        };
        return new BoardCell(baseRow + (id / 2 == 0 ? 1 : 4),
                baseCol + (id % 2 == 0 ? 1 : 4));
    }

    Map<Piece, Integer> progressSnapshot() {
        Map<Piece, Integer> m = new HashMap<>();
        for (PlayerEntry p : state.getPlayers()) {
            for (Piece x : state.getPieces(p)) {
                m.put(x, x.getProgress());
            }
        }
        return m;
    }
}
