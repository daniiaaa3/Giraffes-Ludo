package View;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import Model.GameSetup.PlayerEntry;
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
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

/**
 * Main gameplay screen for Educational Ludo.
 *
 * Shows the board and pieces, whose turn it is (with a roster of
 * all players), the elapsed time, the animated die with its Roll
 * button, an "Extra roll" badge, and short game messages.
 *
 * The layout adapts to the window size and fits 1280 x 720.
 *
 * MVC Component: View
 * Iteration: 1
 */
public class GameplayView {

    private static final double BOARD_SIZE_PIXELS = 525;
    private static final double MAX_BOARD_SCALE = 1.6;

    private final BorderPane root;

    private final Label currentPlayerLabel;
    private final Circle currentPlayerDot;
    private final Label timerLabel;
    private final Label messageLabel;
    private final Label diceLabel;
    private final Label extraRollBadge;

    private final Button rollButton;
    private final Button mainMenuButton;

    private final VBox rosterBox;
    private final List<RosterRow> rosterRows = new ArrayList<>();

    private final EasyBoardView boardView;
    private final DiceView graphicalDice;

    private int elapsedSeconds = 0;
    private final Timeline timer;

    /** One player card in the roster. */
    private static final class RosterRow {
        final PlayerEntry player;
        final HBox card;
        final Label turnTag;
        final String hex;

        RosterRow(PlayerEntry player, HBox card, Label turnTag, String hex) {
            this.player = player;
            this.card = card;
            this.turnTag = turnTag;
            this.hex = hex;
        }
    }

    public GameplayView() {

        root = new BorderPane();
        root.getStyleClass().addAll("menu-root", "gameplay-root");
        root.setPadding(new Insets(14, 20, 14, 20));

        // ---------------------------------------------- top bar
        currentPlayerDot = new Circle(11, Color.web("#E5A0BE"));
        currentPlayerDot.setStroke(Color.WHITE);
        currentPlayerDot.setStrokeWidth(2.5);

        currentPlayerLabel = new Label("Getting ready");
        currentPlayerLabel.getStyleClass().add("turn-label");

        HBox turnChip = new HBox(10, currentPlayerDot, currentPlayerLabel);
        turnChip.setAlignment(Pos.CENTER_LEFT);
        turnChip.getStyleClass().add("turn-chip");

        timerLabel = new Label("Time 00:00");
        timerLabel.getStyleClass().add("timer-label");

        HBox timerChip = new HBox(timerLabel);
        timerChip.setAlignment(Pos.CENTER);
        timerChip.getStyleClass().add("timer-chip");

        // Navigation stays disabled until a Controller registers
        // the application-level return action.
        mainMenuButton = new Button("Main Menu");
        mainMenuButton.getStyleClass().add("gameplay-menu-button");
        mainMenuButton.setMinWidth(130);
        mainMenuButton.setDisable(true);
        UiEffects.interactive(mainMenuButton);

        Region navigationSpacer = new Region();
        HBox.setHgrow(navigationSpacer, Priority.ALWAYS);

        HBox topBar = new HBox(
                14, turnChip, timerChip, navigationSpacer, mainMenuButton);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.getStyleClass().add("gameplay-top-bar");
        root.setTop(topBar);
        BorderPane.setMargin(topBar, new Insets(0, 0, 12, 0));

        // ------------------------------------- left: player roster
        Label rosterTitle = new Label("Players");
        rosterTitle.getStyleClass().add("panel-title");

        rosterBox = new VBox(10);
        rosterBox.setAlignment(Pos.TOP_CENTER);

        VBox rosterPanel = new VBox(14, rosterTitle, rosterBox);
        rosterPanel.setAlignment(Pos.TOP_CENTER);
        rosterPanel.getStyleClass().add("side-panel");
        rosterPanel.setMinWidth(210);
        rosterPanel.setPrefWidth(210);
        rosterPanel.setMaxWidth(210);

        // ------------------------------------------- center: board
        boardView = new EasyBoardView();

        StackPane boardViewport = new StackPane();
        boardViewport.setMinSize(0, 0);
        boardViewport.setAlignment(Pos.CENTER);

        // The board is positioned and scaled by this view.
        boardView.setManaged(false);
        boardViewport.getChildren().add(boardView);

        boardViewport.widthProperty().addListener(
                (observable, oldValue, newValue) ->
                        fitBoardToViewport(boardViewport));
        boardViewport.heightProperty().addListener(
                (observable, oldValue, newValue) ->
                        fitBoardToViewport(boardViewport));

        // --------------------------------------- right: dice card
        Label diceTitle = new Label("Dice");
        diceTitle.getStyleClass().add("panel-title");

        graphicalDice = new DiceView();
        graphicalDice.setValue(1);

        diceLabel = new Label("Ready to roll");
        diceLabel.getStyleClass().add("dice-caption");

        extraRollBadge = new Label("★  Extra roll");
        extraRollBadge.getStyleClass().add("extra-roll-badge");
        extraRollBadge.setVisible(false);
        extraRollBadge.setManaged(false);

        rollButton = new Button("Roll Dice");
        rollButton.getStyleClass().add("roll-button");
        rollButton.setPrefSize(160, 56);
        rollButton.setMinSize(160, 56);
        rollButton.setMaxWidth(160);
        UiEffects.interactive(rollButton);

        VBox diceSection = new VBox(
                16, diceTitle, graphicalDice, diceLabel,
                extraRollBadge, rollButton);
        diceSection.setAlignment(Pos.CENTER);
        diceSection.getStyleClass().add("side-panel");
        diceSection.setMinWidth(210);
        diceSection.setPrefWidth(210);
        diceSection.setMaxWidth(210);

        HBox gameArea = new HBox(
                20, rosterPanel, boardViewport, diceSection);
        gameArea.setAlignment(Pos.CENTER);
        gameArea.setMinHeight(0);
        HBox.setHgrow(boardViewport, Priority.ALWAYS);
        root.setCenter(gameArea);

        // ------------------------------------- bottom: messages
        messageLabel = new Label("Welcome to Giraffes Ludo");
        messageLabel.getStyleClass().add("message-text");
        messageLabel.setWrapText(true);
        messageLabel.setAlignment(Pos.CENTER);
        messageLabel.setMaxWidth(Double.MAX_VALUE);

        HBox messageBox = new HBox(messageLabel);
        messageBox.setAlignment(Pos.CENTER);
        messageBox.getStyleClass().add("message-bar");
        root.setBottom(messageBox);
        BorderPane.setMargin(messageBox, new Insets(12, 0, 0, 0));

        // ------------------------------------------------- timer
        timer = new Timeline(new KeyFrame(
                Duration.seconds(1), event -> updateTimer()));
        timer.setCycleCount(Timeline.INDEFINITE);
    }

    // ------------------------------------------------------ layout
    /**
     * Fits the board inside the available area. The board is only
     * scaled visually; its logical cell coordinates never change.
     */
    private void fitBoardToViewport(StackPane viewport) {

        double availableWidth = viewport.getWidth();
        double availableHeight = viewport.getHeight();

        if (availableWidth <= 0 || availableHeight <= 0) {
            return;
        }

        double scale = Math.min(MAX_BOARD_SCALE, Math.min(
                availableWidth / BOARD_SIZE_PIXELS,
                availableHeight / BOARD_SIZE_PIXELS));

        boardView.resize(BOARD_SIZE_PIXELS, BOARD_SIZE_PIXELS);
        boardView.setScaleX(scale);
        boardView.setScaleY(scale);

        boardView.relocate(
                (availableWidth - BOARD_SIZE_PIXELS) / 2.0,
                (availableHeight - BOARD_SIZE_PIXELS) / 2.0);
    }

    // ------------------------------------------------------ timer
    private void updateTimer() {
        elapsedSeconds++;
        timerLabel.setText(String.format("Time %02d:%02d",
                elapsedSeconds / 60, elapsedSeconds % 60));
    }

    /** Starts the game timer. */
    public void startTimer() {
        timer.play();
    }

    /** Stops the game timer. */
    public void stopTimer() {
        timer.stop();
    }

    /** Pauses elapsed time during a leave-game confirmation. */
    public void pauseTimer() {
        timer.pause();
    }

    /** Resumes elapsed time if the player stays in the game. */
    public void resumeTimer() {
        timer.play();
    }

    // -------------------------------------------- status and input
    /**
     * Reports whether a piece-movement animation is running.
     *
     * @return true while the board animates a piece
     */
    public boolean isAnimating() {
        return boardView.isAnimating();
    }

    /**
     * Returns the gameplay navigation button. The Controller
     * attaches its action so this View stays presentation-only.
     *
     * @return button for requesting a return to the main menu
     */
    public Button getMainMenuButton() {
        return mainMenuButton;
    }

    /**
     * Shows whose turn it is.
     *
     * @param playerName the active player's name
     */
    public void setCurrentPlayer(String playerName) {
        currentPlayerLabel.setText(playerName + "'s turn");
    }

    /**
     * Displays a short game message.
     *
     * @param message the text to show
     */
    public void showGameMessage(String message) {
        messageLabel.setText(message);
    }

    /** @return the Roll Dice button */
    public Button getRollButton() {
        return rollButton;
    }

    /**
     * Shows the die result under the die.
     *
     * @param result the rolled value
     */
    public void setDiceResult(int result) {
        diceLabel.setText("Rolled " + result);
    }

    /**
     * Shows or hides the "Extra roll" badge next to the die.
     *
     * @param visible true while the same player rolls again
     */
    public void showExtraRoll(boolean visible) {
        extraRollBadge.setVisible(visible);
        extraRollBadge.setManaged(visible);
    }

    /**
     * Refreshes the pieces on the board and the player roster.
     *
     * @param gameState the current game
     */
    public void updateBoard(GameState gameState) {
        boardView.renderPieces(gameState);
        updateRoster(gameState);
    }

    /**
     * Hops a piece square by square after the Model moved it.
     *
     * @param gameState the game, already updated by the Model
     * @param piece the piece that moved
     * @param fromProgress progress before the move (-1 = castle)
     * @param onFinished called when the piece has arrived
     */
    public void animatePieceMove(
            GameState gameState,
            Piece piece,
            int fromProgress,
            Runnable onFinished) {
        boardView.animateMove(gameState, piece, fromProgress, onFinished);
    }

    /**
     * Registers piece selection from the board.
     *
     * @param handler called with the clicked piece
     */
    public void setOnPieceSelected(Consumer<Piece> handler) {
        boardView.setOnPieceSelected(handler);
    }

    /**
     * Sets the pieces that can be selected.
     *
     * @param pieces the legal pieces
     */
    public void setSelectablePieces(List<Piece> pieces) {
        boardView.setSelectablePieces(pieces);
    }

    /**
     * Enables or disables the Roll Dice button.
     *
     * @param enabled true to allow rolling
     */
    public void setRollEnabled(boolean enabled) {
        rollButton.setDisable(!enabled);
    }

    /**
     * Animates the die before the result is applied.
     *
     * @param value the final die value
     * @param onFinished called when the animation ends
     */
    public void animateDiceRoll(int value, Runnable onFinished) {
        rollButton.setDisable(true);
        graphicalDice.animateRoll(value, onFinished);
    }

    /** @return the root of the gameplay screen */
    public Parent getRoot() {
        return root;
    }

    // ------------------------------------------------------ roster
    private void updateRoster(GameState gameState) {

        if (rosterRows.size() != gameState.getPlayers().size()) {
            buildRoster(gameState);
        }

        PlayerEntry active = gameState.getActivePlayer();

        for (RosterRow row : rosterRows) {
            boolean isActive = row.player.equals(active);

            row.card.getStyleClass().remove("roster-card-active");
            row.turnTag.setVisible(isActive);
            row.turnTag.setManaged(isActive);

            if (isActive) {
                // A soft tint of the player's own color, a clear
                // border and a gentle glow.
                row.card.getStyleClass().add("roster-card-active");
                row.card.setStyle(
                        "-fx-background-color: " + row.hex + "26;"
                        + "-fx-border-color: " + row.hex + ";"
                        + "-fx-border-width: 2.5;"
                        + "-fx-effect: dropshadow(gaussian, "
                        + row.hex + "73, 18, 0.25, 0, 4);");
            } else {
                row.card.setStyle(
                        "-fx-border-color: " + row.hex + "40;");
            }
        }

        currentPlayerDot.setFill(Color.web(
                PlayerPalette.hex(active.color())));
    }

    private void buildRoster(GameState gameState) {
        rosterBox.getChildren().clear();
        rosterRows.clear();

        for (PlayerEntry player : gameState.getPlayers()) {
            String hex = PlayerPalette.hex(player.color());

            Circle colorDot = new Circle(11, Color.web(hex));
            colorDot.setStroke(Color.WHITE);
            colorDot.setStrokeWidth(2.5);
            colorDot.setEffect(new javafx.scene.effect.DropShadow(
                    4, 0, 1, Color.rgb(70, 35, 55, 0.25)));

            Label name = new Label(player.name());
            name.getStyleClass().add("roster-name");
            name.setMaxWidth(Double.MAX_VALUE);

            // "Your Turn" appears only on the active player's card.
            Label turnTag = new Label("▶  Your Turn");
            turnTag.getStyleClass().add("roster-turn-tag");
            turnTag.setMinWidth(Region.USE_PREF_SIZE);
            turnTag.setVisible(false);
            turnTag.setManaged(false);
            turnTag.setStyle(
                    "-fx-background-color: " + hex + ";"
                    + "-fx-text-fill: "
                    + (player.color()
                            == Model.GameSetup.PlayerColor.YELLOW
                            ? "#5A4200" : "#FFFFFF") + ";");

            VBox text = new VBox(5, name, turnTag);
            text.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(text, Priority.ALWAYS);

            HBox card = new HBox(12, colorDot, text);
            card.setAlignment(Pos.CENTER_LEFT);
            card.getStyleClass().add("roster-card");
            card.setStyle("-fx-border-color: " + hex + "40;");

            rosterRows.add(new RosterRow(player, card, turnTag, hex));
            rosterBox.getChildren().add(card);
        }
    }
}
