package View;


import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import javafx.stage.Stage;

public class LudoApplication extends Application {

    @Override
    public void start(Stage primaryStage) {
        Label title = new Label("Giraffes Ludo");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold;");

        Button newGameButton = new Button("New Game");

        VBox layout = new VBox(20);
        layout.setAlignment(Pos.CENTER);
        layout.getChildren().addAll(title, newGameButton);

        Scene scene = new Scene(layout, 800, 600);

        primaryStage.setTitle("Giraffes Ludo - Iteration 1");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
