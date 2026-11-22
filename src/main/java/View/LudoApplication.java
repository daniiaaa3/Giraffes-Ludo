package View;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Random;

import Controller.GameSetupController;
import Controller.GameplayController;
import Controller.MainMenuController;
import Model.GameDifficulty;
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
 * Entry point and screen navigation for Giraffes Ludo.
 * Iteration 2: starts the selected board difficulty.
 */
public class LudoApplication extends Application {

    private GameplayView activeGameplayView;

    @Override
    public void start(Stage primaryStage) {
        loadOptionalFonts();
        Scene scene = new Scene(new StackPane(), 1280, 720);
        URL stylesheet = LudoApplication.class.getResource("/styles/game.css");
        if (stylesheet == null) {
            throw new IllegalStateException("Missing stylesheet: /styles/game.css");
        }
        scene.getStylesheets().add(stylesheet.toExternalForm());
        showMainMenu(scene);
        primaryStage.setTitle("Giraffes Ludo");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(850);
        primaryStage.setMinHeight(600);
        primaryStage.show();
    }

    private static void loadOptionalFonts() {
        String[] files = {
                "Nunito-Regular", "Nunito-SemiBold", "Nunito-Bold",
                "Nunito-ExtraBold", "Fredoka-Regular", "Fredoka-SemiBold"
        };
        for (String file : files) {
            try (InputStream in = LudoApplication.class.getResourceAsStream(
                    "/fonts/" + file + ".ttf")) {
                if (in != null) {
                    Font.loadFont(in, 14);
                }
            } catch (IOException ignored) {
                // Optional fonts are allowed to be missing.
            }
        }
    }

    private void showMainMenu(Scene scene) {
        MainMenuView view = new MainMenuView();
        new MainMenuController(view, () -> showGameSetup(scene));
        scene.setRoot(view.getRoot());
    }

    private void showGameSetup(Scene scene) {
        GameSetupView view = new GameSetupView();
        GameSetup model = new GameSetup();
        new GameSetupController(
                view,
                model,
                () -> showMainMenu(scene),
                players -> showGameplay(scene, players, view.getSelectedDifficulty()));
        scene.setRoot(view.getRoot());
    }

    private void showGameplay(
            Scene scene, List<PlayerEntry> players, String selectedDifficulty) {

        // Fail clearly if the selector contains an unsupported value.
        GameDifficulty difficulty = GameDifficulty.fromSelection(selectedDifficulty);
        GameState state = GameState.forDifficulty(players, new Random(), difficulty);

        // Both gameplay board and controller refer to this SAME state.
        GameplayView view = new GameplayView(state);
        new GameplayController(
                view,
                state,
                new StepsDice(),
                () -> confirmReturnToMenu(scene, view));

        activeGameplayView = view;
        scene.setRoot(view.getRoot());
        view.startTimer();
    }

    private void confirmReturnToMenu(Scene scene, GameplayView view) {
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

    @Override
    public void stop() {
        if (activeGameplayView != null) {
            activeGameplayView.stopTimer();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
