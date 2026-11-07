package View;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;

/**
 * Renders every real screen at 1280 x 720 (and a larger size) to
 * PNG files in target/ui-screenshots so the layout can be reviewed.
 * Each image must be non-blank.
 */
public class UiScreenshotsTest {

    static final File OUT = new File("target/ui-screenshots");

    @BeforeAll
    static void toolkit() throws Exception {
        FxSupport.init();
        OUT.mkdirs();
    }

    static void shot(Scene scene, String name) throws Exception {
        if (SIZED != null && scene.getRoot() instanceof javafx.scene.layout.Region r
                && r.getClass() == javafx.scene.layout.Region.class) {
            scene = SIZED;
        }
        final Scene target = scene;
        FxSupport.settle(target);
        FxSupport.fx(() -> {
            WritableImage img = target.snapshot(null);
            PixelReader pr = img.getPixelReader();
            int w = (int) img.getWidth(), h = (int) img.getHeight();
            BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            int distinct = 0, first = pr.getArgb(0, 0);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int px = pr.getArgb(x, y);
                    bi.setRGB(x, y, px);
                    if (px != first) {
                        distinct++;
                    }
                }
            }
            assertTrue(distinct > w * h / 20, name + " must not be blank");
            try {
                ImageIO.write(bi, "png", new File(OUT, name + ".png"));
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static Scene launch(int w, int h) throws Exception {
        return FxSupport.fx(() -> {
            Stage stage = new Stage();
            new LudoApplication().start(stage);
            Scene s = stage.getScene();
            stage.setWidth(w + 16);
            stage.setHeight(h + 39);
            return s;
        });
    }

    @Test
    void captureAllScreens() throws Exception {
        for (int[] size : new int[][] {{1280, 720}, {1920, 1080}}) {
            String tag = size[0] + "x" + size[1];
            Scene scene = launch(size[0], size[1]);
            FxSupport.fx(() -> {
                scene.getWindow().setWidth(size[0] + 16);
                scene.getWindow().setHeight(size[1] + 39);
            });
            Thread.sleep(900);          // let the entrance animation finish
            shot(scene, "1-main-menu-" + tag);

            FxSupport.click(scene.getRoot(), "New Game");
            FxSupport.settle(scene);
            Thread.sleep(900);
            shot(scene, "2-setup-empty-" + tag);

            fill(scene, 4);
            Thread.sleep(500);          // let the card entrance animation end
            shot(scene, "3-setup-4-players-" + tag);
            FxSupport.click(scene.getRoot(), "Start Game");
            FxSupport.settle(scene);
            shot(scene, "4-gameplay-start-" + tag);
            FxSupport.closeAllWindows();
        }

        // gameplay with 2, 3 and 4 players, mid-game, at both sizes
        for (int players = 2; players <= 4; players++) {
            for (int[] size : new int[][] {{1280, 720}, {1920, 1080}}) {
                GameplayFixture f = new GameplayFixture(
                        GameplayFixture.firstSeat(),
                        new GameplayFixture.ScriptedRandom(6, 5, 6, 6, 4, 6, 3, 2, 6, 6, 6, 4), players);
                FxSupport.fx(() -> {
                    f.scene.getWindow();
                });
                for (int i = 0; i < 9; i++) {
                    f.roll();
                    if (FxSupport.fx(() -> f.state.isAwaitingMove())) {
                        f.clickHighlighted(0);
                        f.waitIdle();
                    }
                }
                f.roll();
                resize(f.scene, size[0], size[1]);
                shot(f.scene, "5-gameplay-" + players + "p-" + size[0] + "x" + size[1]);
            }
        }
    }

    private static void resize(Scene scene, int w, int h) throws Exception {
        // the fixture scene is not shown; re-host the same root in a sized scene
        FxSupport.fx(() -> {
            javafx.scene.Parent root = scene.getRoot();
            scene.setRoot(new javafx.scene.layout.Region());
            Scene sized = new Scene(root, w, h);
            sized.getStylesheets().addAll(scene.getStylesheets());
            SIZED = sized;
        });
        FxSupport.settle(SIZED);
    }

    private static Scene SIZED;

    @SuppressWarnings("unchecked")
    private static void fill(Scene scene, int count) throws Exception {
        String[] names = {"Dania", "Marwa", "Nay", "Yara"};
        String[] colors = {"RED", "BLUE", "GREEN", "YELLOW"};
        FxSupport.fx(() -> ((ComboBox<Integer>) FxSupport.find(scene.getRoot(),
                n -> n instanceof ComboBox).get(0)).setValue(count));
        FxSupport.settle(scene);
        FxSupport.fx(() -> {
            List<Node> fields = FxSupport.find(scene.getRoot(),
                    n -> n instanceof TextField && FxSupport.shown(n));
            for (int i = 0; i < count; i++) {
                ((TextField) fields.get(i)).setText(names[i]);
            }
            List<Node> tiles = FxSupport.find(scene.getRoot(),
                    n -> n instanceof ToggleButton t && t.getUserData() != null);
            for (int p = 0; p < count; p++) {
                for (int k = 0; k < 4; k++) {
                    ToggleButton t = (ToggleButton) tiles.get(p * 4 + k);
                    if (t.getUserData().toString().equals(colors[p])) {
                        t.setSelected(true);
                    }
                }
            }
        });
    }
}
