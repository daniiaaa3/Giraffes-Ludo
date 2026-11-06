
package View;

import java.util.List;
import java.util.function.Consumer;

import Model.GameState;
import Model.Piece;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Main gameplay screen for Educational Ludo.
 *
 * Displays:
 * - Easy board and player pieces
 * - Active player
 * - Elapsed game time
 * - Animated graphical die
 * - Roll button
 * - Game feedback messages
 *
 * The layout adapts to the available window size.
 *
 * MVC Component: View
 * Iteration: 1
 */
public class GameplayView {

    private static final double BOARD_SIZE_PIXELS = 525;

    private final BorderPane root;

    private final Label currentPlayerLabel;
    private final Label timerLabel;
    private final Label messageLabel;
    private final Label diceLabel;

    private final Button rollButton;

    private final EasyBoardView boardView;
    private final DiceView graphicalDice;

    private int elapsedSeconds = 0;
    private final Timeline timer;

    public GameplayView() {

        root = new BorderPane();
        root.getStyleClass().add("menu-root");

        root.setPadding(new Insets(12, 18, 12, 18));

        // -----------------------------------------
        // TOP - Player and timer
        // -----------------------------------------

        currentPlayerLabel =
                new Label("Current Player: --");

        currentPlayerLabel.setStyle(
                "-fx-font-size: 21px;"
                + "-fx-font-weight: bold;"
                + "-fx-text-fill: #542844;"
        );

        timerLabel = new Label("Time: 00:00");

        timerLabel.setStyle(
                "-fx-font-size: 18px;"
                + "-fx-font-weight: bold;"
                + "-fx-text-fill: #9D5279;"
        );

        HBox topBar = new HBox(
                35,
                currentPlayerLabel,
                timerLabel
        );

        topBar.setAlignment(Pos.CENTER);
        topBar.setPadding(new Insets(14, 20, 14, 20));

        topBar.setStyle(
                "-fx-background-color: rgba(255,255,255,0.85);"
                + "-fx-background-radius: 20;"
                + "-fx-border-color: #F1D6E2;"
                + "-fx-border-radius: 20;"
        );

        root.setTop(topBar);

        BorderPane.setMargin(
                topBar,
                new Insets(0, 0, 10, 0)
        );

        // -----------------------------------------
        // CENTER - Responsive board
        // -----------------------------------------

        boardView = new EasyBoardView();

        StackPane boardViewport = new StackPane();

        boardViewport.setMinSize(0, 0);
        boardViewport.setAlignment(Pos.CENTER);

        // We position and resize the board ourselves.
        boardView.setManaged(false);

        boardViewport.getChildren().add(boardView);

        boardViewport.widthProperty().addListener(
                (observable, oldValue, newValue) ->
                        fitBoardToViewport(boardViewport)
        );

        boardViewport.heightProperty().addListener(
                (observable, oldValue, newValue) ->
                        fitBoardToViewport(boardViewport)
        );

        // -----------------------------------------
        // RIGHT - Dice and Roll button
        // -----------------------------------------

        Label diceTitle = new Label("STEPS DICE");

        diceTitle.setStyle(
                "-fx-font-size: 17px;"
                + "-fx-font-weight: bold;"
                + "-fx-text-fill: #713852;"
        );

        graphicalDice = new DiceView();

        diceLabel = new Label("Ready to roll");

        diceLabel.setStyle(
                "-fx-font-size: 14px;"
                + "-fx-text-fill: #9D5279;"
                + "-fx-font-weight: bold;"
        );

        rollButton = new Button("Roll Dice");

        rollButton.setPrefSize(150, 54);
        rollButton.setMinSize(150, 54);
        rollButton.setMaxWidth(150);

        // Use a dedicated button design to avoid
        // the wide Main Menu button CSS settings.
        rollButton.setStyle(
                "-fx-background-color: #E5A0BE;"
                + "-fx-background-radius: 17;"
                + "-fx-border-color: #A14F7D;"
                + "-fx-border-width: 2;"
                + "-fx-border-radius: 17;"
                + "-fx-text-fill: #512942;"
                + "-fx-font-size: 17px;"
                + "-fx-font-weight: bold;"
                + "-fx-cursor: hand;"
        );

        VBox diceSection = new VBox(
                16,
                diceTitle,
                graphicalDice,
                diceLabel,
                rollButton
        );

        diceSection.setAlignment(Pos.CENTER);
        diceSection.setPadding(new Insets(20, 10, 20, 10));

        diceSection.setMinWidth(175);
        diceSection.setPrefWidth(175);
        diceSection.setMaxWidth(175);

        diceSection.setStyle(
                "-fx-background-color: rgba(255,255,255,0.83);"
                + "-fx-background-radius: 24;"
                + "-fx-border-color: #F1D6E2;"
                + "-fx-border-radius: 24;"
                + "-fx-border-width: 1;"
        );

        // -----------------------------------------
        // MAIN CONTENT - Board + Dice
        // -----------------------------------------

        HBox gameArea = new HBox(
                18,
                boardViewport,
                diceSection
        );

        gameArea.setAlignment(Pos.CENTER);
        gameArea.setMinHeight(0);

        HBox.setHgrow(
                boardViewport,
                Priority.ALWAYS
        );

        root.setCenter(gameArea);

        // -----------------------------------------
        // BOTTOM - Game feedback messages
        // -----------------------------------------

        messageLabel = new Label(
                "Welcome to Educational Ludo!"
        );

        messageLabel.setStyle(
                "-fx-font-size: 16px;"
                + "-fx-text-fill: #D63384;"
                + "-fx-font-weight: bold;"
        );

        messageLabel.setWrapText(true);
        messageLabel.setAlignment(Pos.CENTER);
        messageLabel.setMaxWidth(Double.MAX_VALUE);

        HBox messageBox = new HBox(messageLabel);

        messageBox.setAlignment(Pos.CENTER);
        messageBox.setPadding(
                new Insets(12, 20, 12, 20)
        );

        messageBox.setStyle(
                "-fx-background-color: rgba(255,255,255,0.85);"
                + "-fx-background-radius: 18;"
                + "-fx-border-color: #F1D6E2;"
                + "-fx-border-radius: 18;"
        );

        root.setBottom(messageBox);

        BorderPane.setMargin(
                messageBox,
                new Insets(10, 0, 0, 0)
        );

        // -----------------------------------------
        // TIMER
        // -----------------------------------------

        timer = new Timeline(
                new KeyFrame(
                        Duration.seconds(1),
                        event -> updateTimer()
                )
        );

        timer.setCycleCount(Timeline.INDEFINITE);
    }

    /**
     * Fits the board inside the available area.
     *
     * The board is scaled visually without
     * changing its logical cell coordinates.
     */
    private void fitBoardToViewport(
            StackPane viewport) {

        double availableWidth = viewport.getWidth();
        double availableHeight = viewport.getHeight();

        if (availableWidth <= 0 || availableHeight <= 0) {
            return;
        }

        double widthScale =
                availableWidth / BOARD_SIZE_PIXELS;

        double heightScale =
                availableHeight / BOARD_SIZE_PIXELS;

        double scale = Math.min(
                1.0,
                Math.min(widthScale, heightScale)
        );

        boardView.resize(
                BOARD_SIZE_PIXELS,
                BOARD_SIZE_PIXELS
        );

        boardView.setScaleX(scale);
        boardView.setScaleY(scale);

        // Center the scaled board in its viewport.
        boardView.relocate(
                (availableWidth - BOARD_SIZE_PIXELS) / 2.0,
                (availableHeight - BOARD_SIZE_PIXELS) / 2.0
        );
    }

    /**
     * Updates elapsed game time.
     */
    private void updateTimer() {

        elapsedSeconds++;

        int minutes = elapsedSeconds / 60;
        int seconds = elapsedSeconds % 60;

        timerLabel.setText(
                String.format(
                        "Time: %02d:%02d",
                        minutes,
                        seconds
                )
        );
    }

    /**
     * Starts the game timer.
     */
    public void startTimer() {
        timer.play();
    }

    /**
     * Stops the game timer.
     */
    public void stopTimer() {
        timer.stop();
    }

    /**
     * Updates the active player display.
     */
    public void setCurrentPlayer(String playerName) {

        currentPlayerLabel.setText(
                "Current Player: " + playerName
        );
    }

    /**
     * Displays game feedback.
     */
    public void showGameMessage(String message) {

        messageLabel.setText(message);
    }

    /**
     * Returns the Roll Dice button.
     */
    public Button getRollButton() {

        return rollButton;
    }

    /**
     * Updates the displayed dice result.
     */
    public void setDiceResult(int result) {

        diceLabel.setText("Rolled: " + result);
    }

    /**
     * Refreshes the pieces displayed on the board.
     */
    public void updateBoard(GameState gameState) {

        boardView.renderPieces(gameState);
    }

    /**
     * Registers piece selection from the board.
     */
    public void setOnPieceSelected(
            Consumer<Piece> handler) {

        boardView.setOnPieceSelected(handler);
    }

    /**
     * Sets pieces that can be selected.
     */
    public void setSelectablePieces(List<Piece> pieces) {

        boardView.setSelectablePieces(pieces);
    }

    /**
     * Enables or disables the Roll Dice button.
     */
    public void setRollEnabled(boolean enabled) {

        rollButton.setDisable(!enabled);
    }

    /**
     * Animates the die before applying the result.
     */
    public void animateDiceRoll(
            int value,
            Runnable onFinished) {

        rollButton.setDisable(true);

        graphicalDice.animateRoll(
                value,
                onFinished
        );
    }

    /**
     * Returns the main gameplay root.
     */
    public Parent getRoot() {

        return root;
    }
}
