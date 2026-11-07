package View;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.stage.Stage;

/**
 * Drives the real application: Main Menu, Game Setup and the
 * opening of the playable board. Every action goes through the
 * real controllers; nothing is mocked.
 *
 * These are scripted UI tests, not manual click-throughs.
 */
public class ApplicationFlowUiTest {

    @BeforeAll
    static void startToolkit() throws Exception {
        FxSupport.init();
    }

    @AfterEach
    void closeStages() throws Exception {
        FxSupport.closeAllWindows();
    }

    private static Scene launch() throws Exception {
        return FxSupport.fx(() -> {
            Stage stage = new Stage();
            new LudoApplication().start(stage);
            return stage.getScene();
        });
    }

    @SuppressWarnings("unchecked")
    private static void fillSetup(Scene scene, int count, String[] names,
                                  String[] colors) throws Exception {
        FxSupport.fx(() -> {
            ComboBox<Integer> box = (ComboBox<Integer>) FxSupport.find(
                    scene.getRoot(), n -> n instanceof ComboBox)
                    .get(0);
            box.setValue(count);
        });
        FxSupport.settle(scene);
        FxSupport.fx(() -> {
            List<Node> fields = FxSupport.find(scene.getRoot(),
                    n -> n instanceof TextField && FxSupport.shown(n));
            assertEquals(count, fields.size(),
                    "Only the selected number of name fields is shown");
            for (int i = 0; i < count; i++) {
                ((TextField) fields.get(i)).setText(names[i]);
            }
        });
        FxSupport.fx(() -> {
            // each player card owns four colour tiles; pick by label
            List<Node> tiles = FxSupport.find(scene.getRoot(),
                    n -> n instanceof ToggleButton t && t.getUserData() != null);
            int perPlayer = 4;
            for (int p = 0; p < count; p++) {
                if (colors[p] == null) {
                    continue;
                }
                for (int k = 0; k < perPlayer; k++) {
                    ToggleButton t = (ToggleButton) tiles.get(p * perPlayer + k);
                    if (t.getUserData().toString().equals(colors[p])) {
                        t.setSelected(true);
                    }
                }
            }
        });
    }

    @Test
    void mainMenuOffersThreeOptions() throws Exception {
        Scene scene = launch();
        FxSupport.fx(() -> {
            assertNotNull(FxSupport.button(scene.getRoot(), "New Game"));
            assertNotNull(FxSupport.button(scene.getRoot(), "Game History"));
            assertNotNull(FxSupport.button(scene.getRoot(), "Question Management"));
        });
    }

    @Test
    void startGameOpensThePlayableBoardNotAPlaceholder() throws Exception {
        Scene scene = launch();
        FxSupport.click(scene.getRoot(), "New Game");
        FxSupport.settle(scene);
        fillSetup(scene, 2, new String[] {"Dania", "Marwa"},
                new String[] {"RED", "BLUE"});
        FxSupport.click(scene.getRoot(), "Start Game");

        FxSupport.fx(() -> {
            Button roll = FxSupport.button(scene.getRoot(), "Roll Dice");
            assertFalse(roll.isDisable(), "Roll button is enabled for the first player");
            assertNotNull(FxSupport.button(scene.getRoot(), "Main Menu"));
        });
    }

    @Test
    void setupButtonsStayVisibleWithFourPlayersAt1280x720() throws Exception {
        Scene scene = launch();
        FxSupport.click(scene.getRoot(), "New Game");
        FxSupport.settle(scene);
        fillSetup(scene, 4, new String[] {"A", "B", "C", "D"},
                new String[] {"RED", "BLUE", "GREEN", "YELLOW"});
        FxSupport.settle(scene);
        FxSupport.fx(() -> {
            for (String label : new String[] {"Back to Menu", "Start Game"}) {
                javafx.scene.control.Button b = FxSupport.button(scene.getRoot(), label);
                javafx.geometry.Bounds bounds = b.localToScene(b.getBoundsInLocal());
                assertTrue(bounds.getMaxY() <= scene.getHeight() + 0.5
                        && bounds.getMinY() >= 0,
                        label + " must be fully visible: " + bounds
                                + " in " + scene.getWidth() + "x" + scene.getHeight());
            }
            assertEquals(1280.0, scene.getWidth(), 0.5);
            assertEquals(720.0, scene.getHeight(), 0.5);
        });
    }

    @Test
    void invalidSetupsAreRejectedAndKeepTheUserOnTheSetupScreen() throws Exception {
        String[][] names = {
            {"Dania", "dania"},          // duplicate names (ignoring case)
            {"Dania", "   "},            // blank name
            {"", "Marwa"},               // empty name
        };
        for (String[] pair : names) {
            Scene scene = launch();
            FxSupport.click(scene.getRoot(), "New Game");
            FxSupport.settle(scene);
            fillSetup(scene, 2, pair, new String[] {"RED", "BLUE"});
            Thread t = new Thread(() -> {
                try {
                    FxSupport.click(scene.getRoot(), "Start Game");
                } catch (Exception ignored) {
                    // the dialog blocks the click until it is closed
                }
            });
            t.start();
            FxSupport.waitUntil("error dialog with Try Again", () ->
                    FxSupport.dialogHasButton(scene.getWindow(), "Try Again"), 8000);
            FxSupport.closeDialogs("Try Again");
            t.join(8000);
            assertFalse(t.isAlive(), "closing the dialog releases the Start Game click");
            // still on the setup screen: there is a Start Game button and no Roll Dice
            FxSupport.fx(() -> {
                assertNotNull(FxSupport.button(scene.getRoot(), "Start Game"));
                assertTrue(FxSupport.find(scene.getRoot(), n -> n instanceof javafx.scene.control.Button b
                        && "Roll Dice".equals(b.getText())).isEmpty());
            });
        }
    }

    @Test
    void missingColorIsRejected() throws Exception {
        Scene scene = launch();
        FxSupport.click(scene.getRoot(), "New Game");
        FxSupport.settle(scene);
        fillSetup(scene, 2, new String[] {"Dania", "Marwa"}, new String[] {"RED", null});
        Thread t = new Thread(() -> {
            try {
                FxSupport.click(scene.getRoot(), "Start Game");
            } catch (Exception ignored) {
                // dialog blocks until closed
            }
        });
        t.start();
        FxSupport.waitUntil("error dialog with Try Again", () ->
                FxSupport.dialogHasButton(scene.getWindow(), "Try Again"), 8000);
        FxSupport.closeDialogs("Try Again");
        t.join(8000);
        assertFalse(t.isAlive(), "closing the dialog releases the Start Game click");
        FxSupport.fx(() -> assertNotNull(FxSupport.button(scene.getRoot(), "Start Game")));
    }

    private static void startGame(Scene scene, int players) throws Exception {
        FxSupport.click(scene.getRoot(), "New Game");
        FxSupport.settle(scene);
        String[] names = {"Dania", "Marwa", "Nay", "Yara"};
        String[] colors = {"RED", "BLUE", "GREEN", "YELLOW"};
        fillSetup(scene, players, java.util.Arrays.copyOf(names, players),
                java.util.Arrays.copyOf(colors, players));
        FxSupport.click(scene.getRoot(), "Start Game");
        FxSupport.settle(scene);
    }

    /** Presses Main Menu (its dialog blocks), then answers the dialog. */
    private static void pressMainMenuAndAnswer(Scene scene, String answer) throws Exception {
        Thread t = new Thread(() -> {
            try {
                FxSupport.click(scene.getRoot(), "Main Menu");
            } catch (Exception ignored) {
                // the modal dialog holds this call until it is answered
            }
        });
        t.start();
        FxSupport.waitUntil("leave dialog", () ->
                FxSupport.dialogHasButton(scene.getWindow(), answer), 8000);
        FxSupport.closeDialogs(answer);
        t.join(8000);
        assertFalse(t.isAlive(), "dialog answered and released");
    }

    @Test
    void stayingInGameKeepsItRunningAndLeavingStartsAFreshGame() throws Exception {
        Scene scene = launch();
        startGame(scene, 3);

        FxSupport.waitUntil("timer reaches 1 s", () -> {
            return FxSupport.find(scene.getRoot(), n -> n instanceof javafx.scene.control.Label l
                    && "Time 00:01".equals(l.getText())).size() == 1;
        }, 6000);

        // Stay: still on the board, timer keeps counting
        pressMainMenuAndAnswer(scene, "Stay in Game");
        FxSupport.fx(() -> assertNotNull(FxSupport.button(scene.getRoot(), "Roll Dice")));
        FxSupport.waitUntil("timer continues after staying", () ->
                FxSupport.find(scene.getRoot(), n -> n instanceof javafx.scene.control.Label l
                        && ("Time 00:02".equals(l.getText()) || "Time 00:03".equals(l.getText())
                            || "Time 00:04".equals(l.getText()))).size() == 1, 8000);

        // Leave: back to the main menu
        pressMainMenuAndAnswer(scene, "Leave Game");
        FxSupport.fx(() -> {
            assertNotNull(FxSupport.button(scene.getRoot(), "New Game"));
            assertTrue(FxSupport.find(scene.getRoot(), n -> n instanceof javafx.scene.control.Button b
                    && "Roll Dice".equals(b.getText())).isEmpty());
        });

        // A second game starts from scratch
        startGame(scene, 2);
        FxSupport.fx(() -> {
            assertNotNull(FxSupport.button(scene.getRoot(), "Roll Dice"));
            assertEquals(1, FxSupport.find(scene.getRoot(), n -> n instanceof javafx.scene.control.Label l
                    && "Time 00:00".equals(l.getText())).size(),
                    "timer restarts at zero");
            long pieces = FxSupport.find(scene.getRoot(), n -> n instanceof javafx.scene.shape.Circle c
                    && c.getParent() instanceof javafx.scene.layout.GridPane g
                    && "pieces".equals(g.getUserData())).size();
            assertEquals(8, pieces, "two players start with 8 pieces in castles");
        });
    }

    @Test
    void threeAndFourPlayerGamesOpenWithAllPiecesInCastles() throws Exception {
        for (int players = 3; players <= 4; players++) {
            Scene scene = launch();
            startGame(scene, players);
            int expected = players * 4;
            FxSupport.fx(() -> {
                long pieces = FxSupport.find(scene.getRoot(), n -> n instanceof javafx.scene.shape.Circle c
                        && c.getParent() instanceof javafx.scene.layout.GridPane g
                        && "pieces".equals(g.getUserData())).size();
                assertEquals(expected, pieces);
            });
            FxSupport.closeAllWindows();
        }
    }
}
