
package View;

import java.net.URL;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import Controller.DiceController;
import Model.StepsDice;

public class GameplayViewTestLauncher extends Application {

    @Override
    public void start(Stage stage) {

        GameplayView view = new GameplayView();

        // Temporary test data
        view.setCurrentPlayer("Yara");
        view.showGameMessage("Welcome to Educational Ludo!");
     // Connect the real Steps die to the UI
        StepsDice dice = new StepsDice();
        new DiceController(dice, view);

        Scene scene = new Scene(view.getRoot(), 1000, 700);

        URL stylesheet = getClass().getResource("/styles/game.css");

        if (stylesheet != null) {
            scene.getStylesheets().add(
                    stylesheet.toExternalForm());
        }

        stage.setTitle("Gameplay - UI Test");
        stage.setScene(scene);
        stage.show();

        view.startTimer();
        
        stage.setOnCloseRequest(event -> view.stopTimer());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
