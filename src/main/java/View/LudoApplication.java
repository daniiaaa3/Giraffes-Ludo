package View;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Random;

import Controller.GameSetupController;
import Controller.GameplayController;
import Controller.MainMenuController;
import Model.GameSetup;
import Model.GameSetup.PlayerEntry;
import Model.GameState;
import Model.StepsDice;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.stage.Stage;

/**
 * Entry point and screen coordinator for Giraffes Ludo.
 *
 * Connects the main menu and player setup with the actual
 * Easy gameplay screen. Game rules remain in the Model,
 * while user actions are handled by the Controllers.
 *
 * MVC Responsibility: Application startup and navigation
 * SRS Requirements: FR-1.1 and US-01 through US-04 integration
 * Iteration: 1
 */
public class LudoApplication extends Application {

    private GameplayView activeGameplayView;

    /**
     * Starts JavaFX and opens the main menu.
     *
     * @param primaryStage application window
     */
    @Override
    public void start(Stage primaryStage) {
        loadOptionalFonts();

        Scene scene = new Scene(new StackPane(), 1280, 720);

        URL stylesheet = LudoApplication.class.getResource(
                "/styles/game.css");

        if (stylesheet == null) {
            throw new IllegalStateException(
                    "Missing stylesheet: /styles/game.css");
        }

        scene.getStylesheets().add(stylesheet.toExternalForm());

        showMainMenu(scene);

        primaryStage.setTitle("Giraffes Ludo");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(850);
        primaryStage.setMinHeight(600);
        primaryStage.show();
    }

    /**
     * Loads Nunito or Fredoka when their font files are bundled in
     * /fonts. When they are absent the stylesheet falls back to the
     * best installed system font, so the game always looks fine.
     */
    private static void loadOptionalFonts() {
        String[] files = {
            "Nunito-Regular", "Nunito-SemiBold", "Nunito-Bold",
            "Nunito-ExtraBold", "Fredoka-Regular", "Fredoka-SemiBold"
        };

        for (String file : files) {
            try (InputStream in = LudoApplication.class
                    .getResourceAsStream("/fonts/" + file + ".ttf")) {
                if (in != null) {
                    Font.loadFont(in, 14);
                }
            } catch (IOException ignored) {
                // A missing or unreadable font only changes the look.
            }
        }
    }

    /**
     * Displays the main menu and connects its navigation.
     *
     * @param scene the shared JavaFX scene
     */
    private void showMainMenu(Scene scene) {
        MainMenuView view = new MainMenuView();

        new MainMenuController(
                view,
                () -> showGameSetup(scene));

        scene.setRoot(view.getRoot());
    }

    /**
     * Displays the player setup screen. The setup controller
     * validates the selected players before starting gameplay.
     *
     * @param scene the shared JavaFX scene
     */
    private void showGameSetup(Scene scene) {
        GameSetupView view = new GameSetupView();
        GameSetup model = new GameSetup();

        new GameSetupController(
                view,
                model,
                () -> showMainMenu(scene),
                players -> showGameplay(scene, players));

        scene.setRoot(view.getRoot());
    }

    /**
     * Starts a real Easy Ludo game from validated players.
     *
     * Reuses the existing game model, gameplay view, and
     * gameplay controller instead of duplicating their logic.
     *
     * @param scene the shared JavaFX scene
     * @param players validated names and colors from GameSetup
     */
    private void showGameplay(
            Scene scene, List<PlayerEntry> players) {

        // Existing model: board, pieces, turn order, and rules.
        GameState gameState = new GameState(players, new Random());

        // Existing view: board, die, player status, and timer.
        GameplayView view = new GameplayView();

        // GameplayController delegates navigation to this screen
        // coordinator; the existing game rules stay unchanged.
        new GameplayController(
                view,
                gameState,
                new StepsDice(),
                () -> confirmReturnToMenu(scene, view));

        activeGameplayView = view;
        scene.setRoot(view.getRoot());
        view.startTimer();
    }

    /**
     * Confirms abandoning a match and returns to the main menu.
     * The timer is paused while the modal is open, resumed when
     * staying, and stopped before leaving. A new match creates
     * entirely new GameState and GameplayView instances.
     *
     * @param scene the application scene
     * @param view gameplay view requesting navigation
     */
    private void confirmReturnToMenu(
            Scene scene, GameplayView view) {

        // Ignore callbacks from a game that is no longer active.
        if (activeGameplayView != view) {
            return;
        }

        view.pauseTimer();

        boolean leaveGame;
        try {
            leaveGame = GameDialog.confirmLeaveGame(scene.getWindow());
        } catch (RuntimeException ex) {
            view.resumeTimer();
            throw ex;
        }

        if (!leaveGame) {
            view.resumeTimer();
            return;
        }

        view.stopTimer();
        activeGameplayView = null;
        showMainMenu(scene);
    }

    /**
     * Stops the gameplay timer when the application closes.
     */
    @Override
    public void stop() {
        if (activeGameplayView != null) {
            activeGameplayView.stopTimer();
        }
    }

    /**
     * Launches the JavaFX application.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
}
