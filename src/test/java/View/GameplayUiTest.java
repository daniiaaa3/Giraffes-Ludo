package View;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import Model.EasyBoard.BoardCell;
import Model.GameSetup.PlayerEntry;
import Model.Piece;
import javafx.scene.Node;
import javafx.scene.control.Label;

/**
 * Scripted UI tests of the real gameplay screen. The test plays
 * through the real Roll button and clicks highlighted pieces, then
 * compares what is DISPLAYED with the Model after every action.
 *
 * Scripted JavaFX tests, not a human click-through.
 */
public class GameplayUiTest {

    @BeforeAll
    static void toolkit() throws Exception {
        FxSupport.init();
    }

    private static void assertBoardMatchesModel(GameplayFixture f) throws Exception {
        Map<BoardCell, Map<String, Integer>> shown = FxSupport.fx(f::displayed);
        Map<BoardCell, Map<String, Integer>> want = FxSupport.fx(f::expected);
        assertEquals(want, shown, "Displayed pieces must equal the Model positions");
    }

    @Test
    void everyPlayerStartsWithFourPiecesInTheirCastle() throws Exception {
        for (int n = 2; n <= 4; n++) {
            GameplayFixture f = new GameplayFixture(n, n);
            Map<BoardCell, Map<String, Integer>> shown = FxSupport.fx(f::displayed);
            int total = shown.values().stream()
                    .mapToInt(m -> m.values().stream().mapToInt(i -> i).sum()).sum();
            assertEquals(4 * n, total, n + " players show 4*n pieces");
            for (PlayerEntry p : GameplayFixture.PLAYERS.subList(0, n)) {
                for (int id = 0; id < 4; id++) {
                    BoardCell c = GameplayFixture.castleCell(p.color(), id);
                    assertEquals(1, shown.get(c).get(p.color().name()),
                            p.color() + " piece " + id + " sits in its castle");
                }
            }
            assertBoardMatchesModel(f);
        }
    }

    @Test
    void firstPlayerIsRandomAndShownOnScreen() throws Exception {
        Set<String> seen = new HashSet<>();
        for (int seed = 0; seed < 40; seed++) {
            GameplayFixture f = new GameplayFixture(seed, 4);
            String name = FxSupport.fx(() -> f.state.getActivePlayer().name());
            assertTrue(FxSupport.fx(f::turnText).contains(name));
            seen.add(name);
        }
        assertEquals(4, seen.size(), "Every one of 4 players can start");
    }

    @Test
    void rollButtonReflectsTurnState() throws Exception {
        GameplayFixture f = new GameplayFixture(
                GameplayFixture.firstSeat(),
                new GameplayFixture.ScriptedRandom(6), 2);
        assertFalse(FxSupport.fx(() -> f.rollButton().isDisable()));
        f.roll();                                    // a 6: a piece must be chosen
        assertTrue(FxSupport.fx(() -> f.rollButton().isDisable()),
                "Roll is blocked while a legal move is pending");
        assertEquals(4, FxSupport.fx(() -> f.highlightedCircles().size()),
                "all four castle pieces are highlighted after a 6");
    }

    @Test
    void rollOfSixReleasesPieceToItsStartSquare() throws Exception {
        GameplayFixture f = new GameplayFixture(
                GameplayFixture.firstSeat(),
                new GameplayFixture.ScriptedRandom(6), 2);
        f.roll();
        BoardCell clicked = f.clickHighlighted(0);
        f.waitIdle();
        Piece p = FxSupport.fx(() -> f.state.getPieces(
                GameplayFixture.PLAYERS.get(0)).get(0));
        assertEquals(0, p.getProgress(), "release puts the piece on its start square");
        assertEquals(new BoardCell(13, 6),
                FxSupport.fx(() -> f.state.getBoardCell(p).orElseThrow()));
        assertBoardMatchesModel(f);
        // a six gives the same player another roll
        assertEquals("Dania", FxSupport.fx(() -> f.state.getActivePlayer().name()));
        assertFalse(FxSupport.fx(() -> f.rollButton().isDisable()));
    }

    @Test
    void nonSixWithAllPiecesInCastlePassesTheTurn() throws Exception {
        GameplayFixture f = new GameplayFixture(
                GameplayFixture.firstSeat(),
                new GameplayFixture.ScriptedRandom(3), 2);
        f.roll();
        assertEquals("Marwa", FxSupport.fx(() -> f.state.getActivePlayer().name()));
        String msg = FxSupport.fx(f::message);
        assertTrue(msg.contains("needs a 6 to release a piece"), msg);
        assertFalse(msg.endsWith("."), "UI messages carry no trailing period");
    }

    @Test
    void threeConsecutiveSixesForfeitTheTurnAndMoveNothing() throws Exception {
        GameplayFixture f = new GameplayFixture(
                GameplayFixture.firstSeat(),
                new GameplayFixture.ScriptedRandom(6, 6, 6), 2);
        f.roll();
        f.clickHighlighted(0);
        f.waitIdle();
        f.roll();
        // second six: release another piece or move the first; pick the first highlighted
        f.clickHighlighted(0);
        f.waitIdle();
        Map<Piece, Integer> before = FxSupport.fx(f::progressSnapshot);
        f.roll();                                    // third six
        Map<Piece, Integer> after = FxSupport.fx(f::progressSnapshot);
        assertEquals(before, after, "the third six moves nothing");
        assertEquals("Marwa", FxSupport.fx(() -> f.state.getActivePlayer().name()));
        assertEquals(0, FxSupport.fx(() -> f.highlightedCircles().size()));
        assertTrue(FxSupport.fx(f::message).contains("Three sixes in a row"));
    }

    @Test
    void sixThenNonSixKeepsCountingCorrectly() throws Exception {
        GameplayFixture f = new GameplayFixture(
                GameplayFixture.firstSeat(),
                new GameplayFixture.ScriptedRandom(6, 4), 2);
        f.roll();
        f.clickHighlighted(0);
        f.waitIdle();
        f.roll();                                    // 4: moves the released piece
        f.clickHighlighted(0);
        f.waitIdle();
        Piece p = FxSupport.fx(() -> f.state.getPieces(
                GameplayFixture.PLAYERS.get(0)).get(0));
        assertEquals(4, p.getProgress());
        assertEquals("Marwa", FxSupport.fx(() -> f.state.getActivePlayer().name()));
        assertBoardMatchesModel(f);
    }

    @Test
    void doubleClickingAPieceMovesItOnlyOnce() throws Exception {
        GameplayFixture f = new GameplayFixture(
                GameplayFixture.firstSeat(),
                new GameplayFixture.ScriptedRandom(6, 5), 2);
        f.roll();
        f.clickHighlighted(0);
        f.waitIdle();
        f.roll();                                    // 5
        Map<Piece, Integer> before = FxSupport.fx(f::progressSnapshot);
        f.doubleClickHighlighted(0);
        f.waitIdle();
        Map<Piece, Integer> after = FxSupport.fx(f::progressSnapshot);
        int moved = 0;
        for (Piece p : before.keySet()) {
            if (!before.get(p).equals(after.get(p))) {
                moved++;
                assertEquals(5, after.get(p) - before.get(p));
            }
        }
        assertEquals(1, moved, "a double-click is a single move of exactly 5");
    }

    @Test
    void clickingAnUnhighlightedPieceDoesNothing() throws Exception {
        GameplayFixture f = new GameplayFixture(
                GameplayFixture.firstSeat(),
                new GameplayFixture.ScriptedRandom(6), 2);
        f.roll();
        Map<Piece, Integer> before = FxSupport.fx(f::progressSnapshot);
        // opponent pieces carry no click handler at all
        FxSupport.fx(() -> {
            for (Node n : FxSupport.find(f.boardView(),
                    x -> x instanceof javafx.scene.shape.Circle c
                            && !javafx.scene.paint.Color.GOLD.equals(c.getStroke()))) {
                assertNull(((javafx.scene.shape.Circle) n).getOnMouseClicked());
            }
        });
        assertEquals(before, FxSupport.fx(f::progressSnapshot));
    }

    @Test
    void randomGamesStayConsistentBetweenModelAndDisplay() throws Exception {
        for (int players = 2; players <= 4; players++) {
            for (long seed = 1; seed <= 3; seed++) {
                GameplayFixture f = new GameplayFixture(seed * 100 + players, players);
                Random choice = new Random(seed);
                for (int turn = 0; turn < 120; turn++) {
                    PlayerEntry active = FxSupport.fx(() -> f.state.getActivePlayer());
                    Map<Piece, Integer> before = FxSupport.fx(f::progressSnapshot);
                    int roll = f.roll();
                    boolean mustMove = FxSupport.fx(() -> f.state.isAwaitingMove());
                    if (mustMove) {
                        List<Piece> selectable = FxSupport.fx(() -> f.state.getSelectablePieces());
                        assertEquals(selectable.size(),
                                FxSupport.fx(() -> f.highlightedCircles().size()),
                                "highlighted pieces == legal pieces");
                        f.clickHighlighted(choice.nextInt(selectable.size()));
                        f.waitIdle();
                        Map<Piece, Integer> after = FxSupport.fx(f::progressSnapshot);
                        List<Piece> changed = new ArrayList<>();
                        for (Piece p : before.keySet()) {
                            if (!before.get(p).equals(after.get(p))) {
                                changed.add(p);
                            }
                        }
                        assertEquals(1, changed.size(), "exactly one piece moves per roll");
                        Piece moved = changed.get(0);
                        assertTrue(selectable.contains(moved));
                        int was = before.get(moved);
                        if (was == Piece.CASTLE_PROGRESS) {
                            assertEquals(6, roll);
                            assertEquals(0, moved.getProgress());
                        } else {
                            assertEquals(roll, moved.getProgress() - was,
                                    "progress changes by exactly the die value");
                        }
                    } else {
                        assertEquals(before, FxSupport.fx(f::progressSnapshot),
                                "no legal move: nothing moves");
                    }
                    assertBoardMatchesModel(f);
                    String shownName = FxSupport.fx(f::turnText);
                    String modelName = FxSupport.fx(() -> f.state.getActivePlayer().name());
                    assertTrue(shownName.contains(modelName));
                    assertEquals(!FxSupport.fx(() -> f.state.isAwaitingRoll()),
                            FxSupport.fx(() -> f.rollButton().isDisable()));
                }
            }
        }
        assertTrue(GameplayFixture.FX_ERRORS.isEmpty(),
                "no uncaught exception on the FX thread: " + GameplayFixture.FX_ERRORS);
    }

    @Test
    void timerCountsUpDuringTheGame() throws Exception {
        GameplayFixture f = new GameplayFixture(5, 2);
        assertEquals("Time 00:00", FxSupport.fx(() -> f.label("Time ")));
        FxSupport.waitUntil("timer reaches 2 s",
                () -> "Time 00:02".equals(f.label("Time ")), 6000);
        FxSupport.fx(f.view::stopTimer);
    }

    @Test
    void gameplayFitsA1280By720Window() throws Exception {
        GameplayFixture f = new GameplayFixture(9, 4);
        FxSupport.fx(() -> {
            f.scene.getRoot().applyCss();
            f.scene.getRoot().layout();
            for (Node n : FxSupport.find(f.scene.getRoot(), x ->
                    x instanceof javafx.scene.control.Button
                            || x instanceof EasyBoardView || x instanceof DiceView
                            || (x instanceof Label l
                                && (l.getStyleClass().contains("turn-label")
                                    || l.getStyleClass().contains("timer-label")
                                    || l.getStyleClass().contains("message-text")
                                    || l.getStyleClass().contains("roster-name"))))) {
                javafx.geometry.Bounds b = n.localToScene(n.getBoundsInLocal());
                assertTrue(b.getMinX() >= -0.5 && b.getMinY() >= -0.5
                        && b.getMaxX() <= 1280.5 && b.getMaxY() <= 720.5,
                        n.getClass().getSimpleName() + " is clipped: " + b);
            }
        });
    }
}
