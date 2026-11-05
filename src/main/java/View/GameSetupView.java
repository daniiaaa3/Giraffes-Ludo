package View;

import java.util.ArrayList;
import java.util.List;

import Model.GameSetup.PlayerColor;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

/**
 * Displays the player setup lobby for Giraffes Ludo.
 *
 * <p>The lobby shows two compact player cards per row. Each player can enter
 * a name and choose one of four Ludo colors using accessible, clickable tiles.
 * Selecting a color updates that player's visual accent and plays a brief
 * animation. The View does not validate names or unique colors: those rules
 * remain in the GameSetup Model, called by GameSetupController.</p>
 *
 * <p>MVC Component: View<br>
 * SRS Requirements: FR-1.2, FR-1.4, FR-1.5<br>
 * Iteration: 1</p>
 */
public class GameSetupView {

    private static final int MAX_PLAYERS = 4;

    private static final String[] CARD_ACCENT_CLASSES = {
            "player-panel-red", "player-panel-blue",
            "player-panel-green", "player-panel-yellow"
    };

    private static final String[] STRIPE_ACCENT_CLASSES = {
            "player-stripe-red", "player-stripe-blue",
            "player-stripe-green", "player-stripe-yellow"
    };

    private static final String[] STATUS_ACCENT_CLASSES = {
            "player-status-red", "player-status-blue",
            "player-status-green", "player-status-yellow"
    };

    private final StackPane root;
    private final ComboBox<Integer> playerCountBox;
    private final List<TextField> nameFields;
    private final List<ToggleGroup> colorGroups;
    private final List<VBox> playerRows;
    private final Button startButton;
    private final Button backButton;

    /**
     * Creates the polished Game Setup screen and registers visual interactions.
     */
    public GameSetupView() {
        nameFields = new ArrayList<>();
        colorGroups = new ArrayList<>();
        playerRows = new ArrayList<>();

        // The heading and player-count selector share one compact row.
        Label eyebrow = new Label("LOCAL MULTIPLAYER  |  MATCH SETUP");
        eyebrow.getStyleClass().add("setup-eyebrow");

        Label title = new Label("SET UP PLAYERS");
        title.getStyleClass().addAll("menu-title", "setup-title");

        Label subtitle = new Label("Choose names and colors for your next match.");
        subtitle.getStyleClass().addAll("menu-subtitle", "setup-subtitle");

        VBox heading = new VBox(5, eyebrow, title, subtitle);
        heading.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(heading, Priority.ALWAYS);

        Label countLabel = new Label("Players");
        countLabel.getStyleClass().add("setup-label");

        playerCountBox = new ComboBox<>();
        playerCountBox.getItems().addAll(2, 3, 4);
        playerCountBox.setValue(2);
        playerCountBox.setPrefWidth(115);
        playerCountBox.getStyleClass().add("setup-count");
        playerCountBox.setAccessibleText("Number of players");

        VBox countSection = new VBox(6, countLabel, playerCountBox);
        countSection.setAlignment(Pos.CENTER_RIGHT);

        HBox header = new HBox(20, heading, countSection);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setMaxWidth(Double.MAX_VALUE);

        // A fixed two-column grid keeps the layout predictable for 2-4 players.
        GridPane playersSection = new GridPane();
        playersSection.setHgap(14);
        playersSection.setVgap(14);
        playersSection.setMaxWidth(Double.MAX_VALUE);

        ColumnConstraints leftColumn = new ColumnConstraints();
        leftColumn.setPercentWidth(50);
        leftColumn.setHgrow(Priority.ALWAYS);
        leftColumn.setFillWidth(true);

        ColumnConstraints rightColumn = new ColumnConstraints();
        rightColumn.setPercentWidth(50);
        rightColumn.setHgrow(Priority.ALWAYS);
        rightColumn.setFillWidth(true);

        playersSection.getColumnConstraints().addAll(leftColumn, rightColumn);

        for (int i = 0; i < MAX_PLAYERS; i++) {
            VBox playerCard = createPlayerCard(i);
            playerRows.add(playerCard);
            playersSection.add(playerCard, i % 2, i / 2);
            GridPane.setHgrow(playerCard, Priority.ALWAYS);
            GridPane.setFillWidth(playerCard, true);
        }

        Label hint = new Label("Each player needs a unique name and color.");
        hint.getStyleClass().add("setup-hint");

        backButton = new Button("Back to Menu");
        backButton.getStyleClass().addAll("menu-button", "secondary-button");
        backButton.setMinWidth(140);
        installButtonHover(backButton);

        startButton = new Button("Start Game");
        startButton.getStyleClass().addAll("menu-button", "primary-button");
        startButton.setMinWidth(145);
        startButton.setDefaultButton(true);
        installButtonHover(startButton);

        HBox actions = new HBox(12, backButton, startButton);
        actions.setAlignment(Pos.CENTER_RIGHT);

        HBox footer = new HBox(14, hint, actions);
        footer.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(hint, Priority.ALWAYS);

        VBox card = new VBox(19, header, playersSection, footer);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPrefWidth(740);
        card.setMaxWidth(780);
        card.getStyleClass().addAll("menu-card", "setup-card");

        // Center the lobby when it fits, and scroll if four players need space.
        VBox page = new VBox(card);
        page.setAlignment(Pos.CENTER);
        page.setPadding(new Insets(22));

        ScrollPane scrollPane = new ScrollPane(page);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.getStyleClass().add("setup-scroll");
        page.minHeightProperty().bind(scrollPane.heightProperty().subtract(2));

        root = new StackPane(scrollPane);
        root.getStyleClass().add("menu-root");

        playerCountBox.setOnAction(event -> updateVisibleRows());
        updateVisibleRows();
        playEntranceAnimation(card);
    }

    /**
     * Builds one player card with name input, colorful choices, and a header
     * that reflects the currently selected color.
     *
     * @param index zero-based player index
     * @return a player configuration card
     */
    private VBox createPlayerCard(int index) {
        Region stripe = new Region();
        stripe.getStyleClass().add("player-stripe");
        stripe.setMinSize(4, 29);
        stripe.setPrefSize(4, 29);
        stripe.setMaxSize(4, 29);

        Label number = new Label(Integer.toString(index + 1));
        number.getStyleClass().add("player-number");

        StackPane numberBadge = new StackPane(number);
        numberBadge.getStyleClass().add("player-number-badge");
        numberBadge.setMinSize(28, 28);
        numberBadge.setPrefSize(28, 28);
        numberBadge.setMaxSize(28, 28);

        Label playerTitle = new Label("Player " + (index + 1));
        playerTitle.getStyleClass().add("player-title");

        Label colorStatus = new Label("Choose a color");
        colorStatus.getStyleClass().add("player-color-status");

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        HBox playerHeader = new HBox(
                9, stripe, numberBadge, playerTitle, headerSpacer, colorStatus);
        playerHeader.setAlignment(Pos.CENTER_LEFT);

        TextField nameField = new TextField();
        nameField.setPromptText("Enter player name");
        nameField.setMaxWidth(Double.MAX_VALUE);
        nameField.getStyleClass().add("setup-name");
        nameField.setAccessibleText("Player " + (index + 1) + " name");
        nameFields.add(nameField);

        Label chooseLabel = new Label("YOUR COLOR");
        chooseLabel.getStyleClass().add("setup-color-caption");

        ToggleGroup colorGroup = new ToggleGroup();
        colorGroups.add(colorGroup);

        GridPane colorGrid = new GridPane();
        colorGrid.setHgap(8);
        colorGrid.setVgap(8);
        colorGrid.setMaxWidth(Double.MAX_VALUE);

        ColumnConstraints leftColor = new ColumnConstraints();
        leftColor.setPercentWidth(50);
        leftColor.setHgrow(Priority.ALWAYS);
        leftColor.setFillWidth(true);

        ColumnConstraints rightColor = new ColumnConstraints();
        rightColor.setPercentWidth(50);
        rightColor.setHgrow(Priority.ALWAYS);
        rightColor.setFillWidth(true);
        colorGrid.getColumnConstraints().addAll(leftColor, rightColor);

        PlayerColor[] colors = PlayerColor.values();
        for (int j = 0; j < colors.length; j++) {
            ToggleButton colorTile = createColorTile(colors[j], colorGroup);
            colorGrid.add(colorTile, j % 2, j / 2);
            GridPane.setHgrow(colorTile, Priority.ALWAYS);
            GridPane.setFillWidth(colorTile, true);
        }

        VBox playerCard = new VBox(
                10, playerHeader, nameField, chooseLabel, colorGrid);
        playerCard.getStyleClass().add("player-panel");
        playerCard.setMinWidth(290);
        playerCard.setMaxWidth(Double.MAX_VALUE);

        // This listener changes presentation only; validation stays in Model.
        colorGroup.selectedToggleProperty().addListener(
                (observable, previous, selected) -> {
                    PlayerColor chosen = selected == null
                            ? null : (PlayerColor) selected.getUserData();
                    updatePlayerAccent(playerCard, stripe, colorStatus, chosen);

                    if (selected instanceof ToggleButton button) {
                        playColorSelectionAnimation(button);
                    }
                });

        return playerCard;
    }

    /**
     * Creates a large, keyboard-accessible color tile with a circular swatch.
     * The selected tile displays a visible checkmark beside its name.
     *
     * @param color the represented Ludo color
     * @param group selection group for this player
     * @return a clickable color choice
     */
    private ToggleButton createColorTile(PlayerColor color, ToggleGroup group) {
        ToggleButton tile = new ToggleButton();
        tile.setToggleGroup(group);
        tile.setUserData(color);
        tile.getStyleClass().addAll(
                "color-choice", "color-choice-" + color.name().toLowerCase());
        tile.setMaxWidth(Double.MAX_VALUE);
        tile.setMinHeight(41);
        tile.setAlignment(Pos.CENTER_LEFT);

        Circle swatch = new Circle(8, getColorPaint(color));
        swatch.setStroke(Color.web("#BDA4AF"));
        swatch.setStrokeWidth(0.8);

        String colorName = displayColorName(color);
        tile.setAccessibleText("Choose " + colorName + " for this player");

        Label name = new Label(colorName);
        name.getStyleClass().add("color-choice-label");

        Label check = new Label("\u2713");
        check.getStyleClass().add("color-choice-check");
        check.visibleProperty().bind(tile.selectedProperty());
        check.managedProperty().bind(tile.selectedProperty());

        // Keep the check beside the text so it cannot be clipped at the edge.
        HBox tileContent = new HBox(7, swatch, name, check);
        tileContent.setAlignment(Pos.CENTER_LEFT);
        tile.setGraphic(tileContent);

        return tile;
    }

    /**
     * Adds a matching accent to the card once its player picks a color.
     *
     * @param card the player card
     * @param stripe colored decorative stripe by the player number
     * @param status human-readable selection label
     * @param selectedColor newly selected color, or null when cleared
     */
    private void updatePlayerAccent(
            VBox card, Region stripe, Label status, PlayerColor selectedColor) {

        card.getStyleClass().removeAll(CARD_ACCENT_CLASSES);
        stripe.getStyleClass().removeAll(STRIPE_ACCENT_CLASSES);
        status.getStyleClass().removeAll(STATUS_ACCENT_CLASSES);

        if (selectedColor == null) {
            status.setText("Choose a color");
            return;
        }

        String suffix = selectedColor.name().toLowerCase();
        card.getStyleClass().add("player-panel-" + suffix);
        stripe.getStyleClass().add("player-stripe-" + suffix);
        status.getStyleClass().add("player-status-" + suffix);
        status.setText(displayColorName(selectedColor) + " selected");
    }

    /**
     * Briefly enlarges and restores the newly selected color tile.
     * This animation never changes the selected value.
     *
     * @param tile the color tile selected by the user
     */
    private void playColorSelectionAnimation(ToggleButton tile) {
        ScaleTransition pop = new ScaleTransition(Duration.millis(95), tile);
        pop.setToX(1.045);
        pop.setToY(1.045);

        ScaleTransition settle = new ScaleTransition(Duration.millis(150), tile);
        settle.setToX(1.0);
        settle.setToY(1.0);

        new SequentialTransition(pop, settle).play();
    }

    /**
     * Adds a small scaling effect while hovering over a navigation button.
     *
     * @param button the navigation button to animate
     */
    private void installButtonHover(Button button) {
        button.setOnMouseEntered(event -> animateButton(button, 1.025));
        button.setOnMouseExited(event -> animateButton(button, 1.0));
    }

    /**
     * Animates a navigation button to a specific scale.
     *
     * @param button button to animate
     * @param scale target scale factor
     */
    private void animateButton(Button button, double scale) {
        ScaleTransition transition = new ScaleTransition(
                Duration.millis(135), button);
        transition.setToX(scale);
        transition.setToY(scale);
        transition.play();
    }

    /**
     * Introduces the lobby with a subtle fade and upward slide.
     *
     * @param card the primary Game Setup card
     */
    private void playEntranceAnimation(VBox card) {
        FadeTransition fade = new FadeTransition(
                Duration.millis(330), card);
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition slide = new TranslateTransition(
                Duration.millis(330), card);
        slide.setFromY(10);
        slide.setToY(0);

        new ParallelTransition(fade, slide).play();
    }

    /**
     * Shows only the selected number of player cards, and fades in new cards.
     */
    private void updateVisibleRows() {
        int count = getPlayerCount();
        for (int i = 0; i < MAX_PLAYERS; i++) {
            VBox playerCard = playerRows.get(i);
            boolean wasManaged = playerCard.isManaged();
            boolean active = i < count;

            playerCard.setVisible(active);
            playerCard.setManaged(active);

            if (active && !wasManaged) {
                FadeTransition reveal = new FadeTransition(
                        Duration.millis(230), playerCard);
                reveal.setFromValue(0);
                reveal.setToValue(1);
                reveal.play();
            }
        }
    }

    /**
     * Converts a logical player color into a readable label.
     *
     * @param color logical Ludo color
     * @return human-readable color name
     */
    private String displayColorName(PlayerColor color) {
        return switch (color) {
            case RED -> "Red";
            case BLUE -> "Blue";
            case GREEN -> "Green";
            case YELLOW -> "Yellow";
        };
    }

    /**
     * Converts a Ludo color to the paint used for its circular swatch.
     *
     * @param color logical Ludo color
     * @return JavaFX paint for that color
     */
    private Color getColorPaint(PlayerColor color) {
        return switch (color) {
            case RED -> Color.web("#E85561");
            case BLUE -> Color.web("#4A90E2");
            case GREEN -> Color.web("#49B77C");
            case YELLOW -> Color.web("#F0C541");
        };
    }

    /**
     * @return root of the Game Setup screen
     */
    public Parent getRoot() {
        return root;
    }

    /**
     * @return chosen number of players, or 2 when no value is selected
     */
    public int getPlayerCount() {
        Integer count = playerCountBox.getValue();
        return count == null ? 2 : count;
    }

    /**
     * @param index zero-based player index
     * @return corresponding player name input field
     */
    public TextField getNameField(int index) {
        return nameFields.get(index);
    }

    /**
     * @param index zero-based player index
     * @return selected color or null if none is selected
     */
    public PlayerColor getSelectedColor(int index) {
        Toggle selected = colorGroups.get(index).getSelectedToggle();
        return selected == null
                ? null
                : (PlayerColor) selected.getUserData();
    }

    /**
     * @return Start Game button wired by GameSetupController
     */
    public Button getStartButton() {
        return startButton;
    }

    /**
     * @return Back to Menu button wired by GameSetupController
     */
    public Button getBackButton() {
        return backButton;
    }
}
