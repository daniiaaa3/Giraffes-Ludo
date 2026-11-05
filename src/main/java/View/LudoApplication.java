package View;

import java.net.URL;
import java.util.List;

import Controller.GameSetupController;
import Controller.MainMenuController;
import Model.GameSetup;
import Model.GameSetup.PlayerEntry;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * JavaFX entry point and screen coordinator for Giraffes Ludo.
 *
 * Creates the shared Scene and connects the main menu and setup
 * interfaces to their controllers. Gameplay integration follows
 * when the board and turn-management components are available.
 *
 * MVC Responsibility: Application startup and navigation
 * SRS Requirements: FR-1.1, FR-1.2, FR-1.4, FR-1.5
 * Iteration: 1
 */
public class LudoApplication extends Application {

    /**
     * Starts JavaFX and displays the main menu.
     *
     * @param primaryStage application window
     */
    @Override
    public void start(Stage primaryStage) {
        Scene scene = new Scene(new StackPane(), 1000, 700);

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
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        primaryStage.show();
    }

    /**
     * Displays the main menu and connects its navigation callback.
     *
     * @param scene shared game Scene
     */
    private void showMainMenu(Scene scene) {
        MainMenuView view = new MainMenuView();
        new MainMenuController(view, () -> showGameSetup(scene));
        scene.setRoot(view.getRoot());
    }

    /**
     * Displays player setup and connects validation and Back actions.
     *
     * @param scene shared game Scene
     */
    private void showGameSetup(Scene scene) {
        GameSetupView view = new GameSetupView();
        GameSetup model = new GameSetup();

        new GameSetupController(
                view,
                model,
                () -> showMainMenu(scene),
                players -> showSetupConfirmation(scene, players));

        scene.setRoot(view.getRoot());
    }

    /**
     * Confirms valid configuration while gameplay integration is pending.
     *
     * @param scene shared Scene used to locate the owner window
     * @param players validated player configurations
     */
    private void showSetupConfirmation(
            Scene scene, List<PlayerEntry> players) {
        StringBuilder details = new StringBuilder();
        for (PlayerEntry player : players) {
            details.append(player.name())
                   .append(" — ")
                   .append(player.color())
                   .append("\n");
        }

        GameDialog.showSuccess(
                scene.getWindow(),
                "Players Are Ready!",
                details.toString().stripTrailing()
                        + "\n\nThe playable Ludo board will be connected next.");
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
