
package View;

import java.net.URL;
import java.util.List;

import Controller.GameSetupController;
import Controller.MainMenuController;
import Model.GameSetup;
import Model.GameSetup.PlayerEntry;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Main entry point and screen coordinator for Giraffes Ludo.
 *
 * Initializes JavaFX and connects the application's
 * views and controllers.
 *
 * MVC Responsibility: Application initialization
 * SRS Requirements: FR-1.1, FR-1.2, FR-1.4, FR-1.5
 * Iteration: 1
 */
public class LudoApplication extends Application {

    /**
     * Initializes the application window.
     *
     * @param primaryStage main JavaFX window
     */
    @Override
    public void start(Stage primaryStage) {

        // Create one Scene for the entire application.
        Scene scene = new Scene(
                new StackPane(), 1000, 700);

        // Load the shared pink stylesheet.
        URL stylesheet = LudoApplication.class.getResource(
                "/styles/game.css");

        if (stylesheet == null) {
            throw new IllegalStateException(
                    "Missing stylesheet: /styles/game.css");
        }

        scene.getStylesheets().add(
                stylesheet.toExternalForm());

        // Display the first screen.
        showMainMenu(scene);

        primaryStage.setTitle(
                "Giraffes Ludo - Iteration 1");

        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        primaryStage.show();
    }

    /**
     * Displays the main menu and connects its controller.
     *
     * @param scene the application's shared Scene
     */
    private void showMainMenu(Scene scene) {

        MainMenuView view = new MainMenuView();

        // The controller requests navigation to Game Setup.
        new MainMenuController(
                view,
                () -> showGameSetup(scene));

        scene.setRoot(view.getRoot());
    }

    /**
     * Displays Game Setup and connects validation actions.
     *
     * @param scene the application's shared Scene
     */
    private void showGameSetup(Scene scene) {

        GameSetupView view = new GameSetupView();
        GameSetup model = new GameSetup();

        new GameSetupController(
                view,
                model,
                () -> showMainMenu(scene),
                players -> showSetupConfirmation(players)
        );

        scene.setRoot(view.getRoot());
    }

    /**
     * Temporarily displays the validated player setup.
     *
     * The actual gameplay screen will be connected when
     * the board and turn-management components are ready.
     *
     * @param players validated player configurations
     */
    private void showSetupConfirmation(
            List<PlayerEntry> players) {

        StringBuilder details = new StringBuilder();

        for (PlayerEntry player : players) {
            details.append(player.name())
                   .append(" - ")
                   .append(player.color())
                   .append("\n");
        }

        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Game Setup Complete");
        alert.setHeaderText("Player configuration is valid!");
        alert.setContentText(
                details.toString() +
                "\nGameplay will be connected next.");

        alert.showAndWait();
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
