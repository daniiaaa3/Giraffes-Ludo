
package View;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.util.Duration;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

public class GameplayView {

    private final BorderPane root;
    private final Label currentPlayerLabel;
    private final Label timerLabel;
    private final Label messageLabel;
    private final Label diceLabel;
    private final Button rollButton;

    private int elapsedSeconds = 0;
    private final Timeline timer;

    public GameplayView() {

        root = new BorderPane();
        root.getStyleClass().add("menu-root");

        // Current player display
        currentPlayerLabel = new Label("Current Player: --");
        currentPlayerLabel.setStyle(
                "-fx-font-size: 20px; -fx-font-weight: bold;");

        // Game timer display
        timerLabel = new Label("Time: 00:00");
        timerLabel.setStyle("-fx-font-size: 18px;");

        HBox topBar = new HBox(30, currentPlayerLabel, timerLabel);
        topBar.setAlignment(Pos.CENTER);
        topBar.setPadding(new Insets(20));

        root.setTop(topBar);
     // Game messages display
        messageLabel = new Label("Welcome to Educational Ludo!");
        messageLabel.setStyle(
                "-fx-font-size: 16px;" +
                "-fx-text-fill: #D63384;" +
                "-fx-font-weight: bold;"
        );

        messageLabel.setWrapText(true);
        messageLabel.setAlignment(Pos.CENTER);
        messageLabel.setMaxWidth(Double.MAX_VALUE);

        HBox messageBox = new HBox(messageLabel);
        messageBox.setAlignment(Pos.CENTER);
        messageBox.setPadding(new Insets(20));

        root.setBottom(messageBox);
        
     // Dice display
        diceLabel = new Label("Dice: -");
        diceLabel.setStyle(
                "-fx-font-size: 26px; -fx-font-weight: bold;");

        // Roll button
        rollButton = new Button("Roll Dice");
        rollButton.getStyleClass().addAll(
                "menu-button", "primary-button");

        // Dice section
        VBox diceSection = new VBox(15, diceLabel, rollButton);
        diceSection.setAlignment(Pos.CENTER);
        diceSection.setPadding(new Insets(20));

        root.setRight(diceSection);

        // Update the timer every second
        timer = new Timeline(new KeyFrame(
                Duration.seconds(1),
                event -> updateTimer()
        ));
        timer.setCycleCount(Timeline.INDEFINITE);
    }

    private void updateTimer() {
        elapsedSeconds++;

        int minutes = elapsedSeconds / 60;
        int seconds = elapsedSeconds % 60;

        timerLabel.setText(
                String.format("Time: %02d:%02d", minutes, seconds));
    }

    public void startTimer() {
        timer.play();
    }

    public void stopTimer() {
        timer.stop();
    }

    public void setCurrentPlayer(String playerName) {
        currentPlayerLabel.setText("Current Player: " + playerName);
    }
    
    /**
     * Updates the message shown to the player.
     *
     * @param message message to display
     */
    public void showGameMessage(String message) {
        messageLabel.setText(message);
    }
    
    /**
     * Returns the Roll Dice button for the controller.
     */
    public Button getRollButton() {
        return rollButton;
    }

    /**
     * Displays the dice result.
     */
    public void setDiceResult(int result) {
        diceLabel.setText("Dice: " + result);
    }

    public Parent getRoot() {
        return root;
    }
}
