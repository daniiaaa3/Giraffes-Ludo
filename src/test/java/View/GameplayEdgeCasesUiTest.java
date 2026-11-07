package View;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import Model.EasyBoard.BoardCell;
import Model.GameSetup.PlayerColor;
import Model.Piece;

/**
 * Edge cases on the real gameplay screen: repeated clicks, exact
 * finishing on the home path, and the turn after a finish.
 */
public class GameplayEdgeCasesUiTest {

    @BeforeAll
    static void toolkit() throws Exception {
        FxSupport.init();
    }

    private static void place(Piece p, int progress, Piece.State state) throws Exception {
        Method m = Piece.class.getDeclaredMethod("moveTo", int.class, Piece.State.class);
        m.setAccessible(true);
        m.invoke(p, progress, state);
    }

    @Test
    void pressingRollTwiceAtOnceRollsOnlyOnce() throws Exception {
        GameplayFixture f = new GameplayFixture(
                GameplayFixture.firstSeat(),
                new GameplayFixture.ScriptedRandom(3, 4), 2);

        FxSupport.fx(() -> {
            f.rollButton().fire();
            f.rollButton().fire();          // ignored: the button is now locked
        });
        FxSupport.waitUntil("dice done", () -> !f.diceView().isRolling(), 8000);
        f.waitIdle();

        // exactly one roll happened: a 3 with all pieces in the castle passes the turn once
        assertEquals("Marwa", FxSupport.fx(() -> f.state.getActivePlayer().name()));
        assertEquals("Rolled 3", FxSupport.fx(() -> f.label("Rolled ")));
        assertTrue(GameplayFixture.FX_ERRORS.isEmpty(), GameplayFixture.FX_ERRORS.toString());
    }

    @Test
    void homePathFinishNeedsTheExactNumberAndOvershootPassesTheTurn() throws Exception {
        GameplayFixture f = new GameplayFixture(
                GameplayFixture.firstSeat(),
                new GameplayFixture.ScriptedRandom(3, 5, 2), 2);
        Piece red = FxSupport.fx(() -> f.state.getPieces(GameplayFixture.PLAYERS.get(0)).get(0));

        // two steps before the finish (finish = 56): progress 54 on the home path
        FxSupport.fx(() -> {
            try {
                place(red, 54, Piece.State.HOME_PATH);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            f.view.updateBoard(f.state);
        });

        f.roll();                           // 3 would overshoot: no legal move
        assertEquals(54, red.getProgress(), "overshoot is not allowed");
        assertEquals("Marwa", FxSupport.fx(() -> f.state.getActivePlayer().name()));
        assertEquals(0, FxSupport.fx(() -> f.highlightedCircles().size()));

        f.roll();                           // Marwa rolls 5: nothing to move
        assertEquals("Dania", FxSupport.fx(() -> f.state.getActivePlayer().name()));

        f.roll();                           // exact 2 finishes the piece
        assertEquals(1, FxSupport.fx(() -> f.highlightedCircles().size()));
        f.clickHighlighted(0);
        f.waitIdle();

        assertEquals(56, red.getProgress());
        assertTrue(red.isFinished());
        Optional<BoardCell> cell = FxSupport.fx(() -> f.state.getBoardCell(red));
        assertEquals(Optional.of(new BoardCell(8, 7)), cell, "finished at the last red home square");
        assertEquals(FxSupport.fx(f::expected), FxSupport.fx(f::displayed));
        assertTrue(GameplayFixture.FX_ERRORS.isEmpty(), GameplayFixture.FX_ERRORS.toString());
    }

    @Test
    void everyDieValueMovesExactlyThatManySquaresOnTheTrack() throws Exception {
        for (int roll = 1; roll <= 6; roll++) {
            GameplayFixture f = new GameplayFixture(
                    GameplayFixture.firstSeat(),
                    new GameplayFixture.ScriptedRandom(roll), 2);
            Piece red = FxSupport.fx(() -> f.state.getPieces(GameplayFixture.PLAYERS.get(0)).get(0));
            // 3 squares before RED's first corner turn at (9,6) -> (8,5)
            FxSupport.fx(() -> {
                try {
                    place(red, 2, Piece.State.TRACK);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                f.view.updateBoard(f.state);
            });
            f.roll();
            BoardCell here = FxSupport.fx(() -> f.state.getBoardCell(red).orElseThrow());
            int index = f.highlightedIndexAt(here);
            assertTrue(index >= 0, "the track piece is highlighted for roll " + roll);
            f.clickHighlighted(index);
            f.waitIdle();
            assertEquals(2 + roll, red.getProgress(), "roll " + roll);
            assertEquals(FxSupport.fx(f::expected), FxSupport.fx(f::displayed), "roll " + roll);
        }
    }

    @Test
    void sixOnTheFinalApproachStillGivesAnExtraRollEvenWithoutAMove() throws Exception {
        GameplayFixture f = new GameplayFixture(
                GameplayFixture.firstSeat(),
                new GameplayFixture.ScriptedRandom(6), 2);
        // three pieces finished, the last one 1 step from home: a 6 has no legal move
        for (int i = 0; i < 3; i++) {
            Piece p = FxSupport.fx(() -> f.state.getPieces(GameplayFixture.PLAYERS.get(0)).get(0));
            // use distinct pieces
        }
        FxSupport.fx(() -> {
            try {
                for (int i = 0; i < 3; i++) {
                    place(f.state.getPieces(GameplayFixture.PLAYERS.get(0)).get(i), 56, Piece.State.FINISHED);
                }
                place(f.state.getPieces(GameplayFixture.PLAYERS.get(0)).get(3), 55, Piece.State.HOME_PATH);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            f.view.updateBoard(f.state);
        });
        f.roll();
        assertEquals("Dania", FxSupport.fx(() -> f.state.getActivePlayer().name()),
                "a 6 grants an extra roll even when no piece can move");
        assertTrue(FxSupport.fx(f::extraRollBadgeShown));
        assertFalse(FxSupport.fx(() -> f.rollButton().isDisable()));
    }
}
