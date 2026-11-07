package View;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import Model.EasyBoard.BoardCell;
import Model.GameSetup.PlayerColor;
import Model.GameSetup.PlayerEntry;
import Model.Piece;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.shape.Circle;

/**
 * The visible # start markers (FR-2.5, FR-3.2), clear display of
 * several pieces on one protected square, and the extra-roll cue.
 */
public class StartSquareUiTest {

    @BeforeAll
    static void toolkit() throws Exception {
        FxSupport.init();
    }

    private static BoardCell expectedStart(PlayerColor c) {
        return switch (c) {
            case RED -> new BoardCell(13, 6);
            case GREEN -> new BoardCell(6, 1);
            case YELLOW -> new BoardCell(1, 8);
            case BLUE -> new BoardCell(8, 13);
        };
    }

    private static BoardCell cellOf(Node cellNode) {
        Integer r = GridPane.getRowIndex(cellNode);
        Integer c = GridPane.getColumnIndex(cellNode);
        return new BoardCell(r == null ? 0 : r, c == null ? 0 : c);
    }

    @Test
    void fourHashMarkersSitOnTheFourStartSquaresAndNoStarsRemain() throws Exception {
        GameplayFixture f = new GameplayFixture(1, 4);

        FxSupport.fx(() -> {
            List<Node> markers = FxSupport.find(f.boardView(), n ->
                    n instanceof Label l && "#".equals(l.getText()));
            assertEquals(4, markers.size(), "exactly four # markers");

            for (Node marker : markers) {
                String tag = (String) marker.getUserData();
                PlayerColor color = PlayerColor.valueOf(tag.substring("start-marker:".length()));
                BoardCell where = cellOf(marker.getParent());
                assertEquals(expectedStart(color), where,
                        color + " # is on its own start square");
            }

            assertTrue(FxSupport.find(f.boardView(), n ->
                    n instanceof Label l && l.getText() != null
                            && (l.getText().contains("★") || l.getText().contains("☆"))).isEmpty(),
                    "no star symbols are used for start squares");
        });
    }

    @Test
    void releasedPieceAppearsOnTheHashSquareOfItsColor() throws Exception {
        for (int seat = 0; seat < 4; seat++) {
            int target = seat;
            GameplayFixture f = new GameplayFixture(
                    new java.util.Random() {
                        @Override
                        public int nextInt(int bound) {
                            return target;
                        }
                    },
                    new GameplayFixture.ScriptedRandom(6), 4);
            PlayerEntry active = FxSupport.fx(() -> f.state.getActivePlayer());
            f.roll();
            f.clickHighlighted(0);
            f.waitIdle();

            Map<BoardCell, Map<String, Integer>> shown = FxSupport.fx(f::displayed);
            BoardCell start = expectedStart(active.color());
            assertEquals(1, shown.get(start).get(active.color().name()),
                    active.color() + " piece is displayed on its # square");
        }
    }

    @Test
    void fourDifferentColoredPiecesShareAProtectedSquareWithoutOverlap() throws Exception {
        GameplayFixture f = new GameplayFixture(
                GameplayFixture.firstSeat(), new GameplayFixture.ScriptedRandom(3), 4);

        // Put all four colors on RED's start square (progress 0 / 13 / 39 / 26)
        Method moveTo = Piece.class.getDeclaredMethod(
                "moveTo", int.class, Piece.State.class);
        moveTo.setAccessible(true);
        int[] progress = {0, 13, 39, 26};          // RED, BLUE, GREEN, YELLOW
        FxSupport.fx(() -> {
            for (int i = 0; i < 4; i++) {
                Piece p = f.state.getPieces(GameplayFixture.PLAYERS.get(i)).get(0);
                try {
                    moveTo.invoke(p, progress[i], Piece.State.TRACK);
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException(e);
                }
            }
            f.view.updateBoard(f.state);
            f.scene.getRoot().applyCss();
            f.scene.getRoot().layout();
        });

        FxSupport.fx(() -> {
            BoardCell start = expectedStart(PlayerColor.RED);
            Map<String, Integer> here = f.displayed().get(start);
            assertEquals(4, here.size(), "four different colors on the square");

            List<Circle> circles = new ArrayList<>();
            for (Node n : FxSupport.find(f.boardView(), x -> x instanceof Circle c
                    && c.getParent() instanceof GridPane g
                    && "pieces".equals(g.getUserData()))) {
                Circle c = (Circle) n;
                if (cellOf(c.getParent().getParent()).equals(start)) {
                    circles.add(c);
                }
            }
            assertEquals(4, circles.size());

            Bounds cellBounds = circles.get(0).getParent().getParent()
                    .localToScene(circles.get(0).getParent().getParent().getBoundsInLocal());
            for (int i = 0; i < circles.size(); i++) {
                Bounds a = circles.get(i).localToScene(circles.get(i).getLayoutBounds());
                assertTrue(a.getMinX() >= cellBounds.getMinX() - 0.5
                        && a.getMaxX() <= cellBounds.getMaxX() + 0.5
                        && a.getMinY() >= cellBounds.getMinY() - 0.5
                        && a.getMaxY() <= cellBounds.getMaxY() + 0.5,
                        "piece stays inside its square");
                for (int j = i + 1; j < circles.size(); j++) {
                    Bounds b = circles.get(j).localToScene(circles.get(j).getLayoutBounds());
                    assertFalse(a.intersects(b),
                            "pieces on the shared square must not overlap");
                }
            }
        });
    }

    @Test
    void extraRollBadgeAppearsAfterASixAndDisappearsOnTheNextRoll() throws Exception {
        GameplayFixture f = new GameplayFixture(
                GameplayFixture.firstSeat(), new GameplayFixture.ScriptedRandom(6, 2), 2);

        assertFalse(FxSupport.fx(f::extraRollBadgeShown));
        f.roll();                                   // 6: choose a piece
        f.clickHighlighted(0);
        f.waitIdle();
        assertTrue(FxSupport.fx(f::extraRollBadgeShown), "extra roll is shown after a 6");
        assertTrue(FxSupport.fx(f::message).startsWith("Extra roll"));

        f.roll();                                   // 2: normal roll
        assertFalse(FxSupport.fx(f::extraRollBadgeShown));
    }

    @Test
    void movesAreAnimatedSquareBySquareAndInputIsLockedMeanwhile() throws Exception {
        GameplayFixture f = new GameplayFixture(
                GameplayFixture.firstSeat(), new GameplayFixture.ScriptedRandom(6, 5), 2);
        f.roll();
        f.clickHighlighted(0);
        f.waitIdle();
        f.roll();                                   // 5
        f.clickHighlighted(0);
        // the model has moved at once, the roll button stays locked until the hop ends
        assertEquals(5, FxSupport.fx(() -> f.state.getPieces(
                GameplayFixture.PLAYERS.get(0)).get(0).getProgress()));
        f.waitIdle();
        assertEquals(5, FxSupport.fx(() -> f.state.getPieces(
                GameplayFixture.PLAYERS.get(0)).get(0).getProgress()));
        assertEquals(FxSupport.fx(f::expected), FxSupport.fx(f::displayed));
        assertTrue(GameplayFixture.FX_ERRORS.isEmpty(), GameplayFixture.FX_ERRORS.toString());
    }
}
