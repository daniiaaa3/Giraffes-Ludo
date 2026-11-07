package View;

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
        Scene scene = new Scene(new StackPane(), 1280, 720);

        URL stylesheet = LudoApplication.class.getResource(
                "/styles/game.css");

        if (stylesheet == null) {
            throw new IllegalStateException(
                    "Missing stylesheet: /styles/game.css");
        }

        scene.getStylesheets().add(stylesheet.toExternalForm());

        showMainMenu(scene);

        primaryStage.setTitle("Giraffes Ludo - Iteration 1");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(850);
        primaryStage.setMinHeight(600);
        primaryStage.show();
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

        // Existing controller: dice rolls and legal piece moves.
        new GameplayController(view, gameState, new StepsDice());

        activeGameplayView = view;
        scene.setRoot(view.getRoot());
        view.startTimer();
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
