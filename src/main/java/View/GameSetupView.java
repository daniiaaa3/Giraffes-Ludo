
package View;

import java.util.ArrayList;
import java.util.List;

import Model.GameSetup.PlayerColor;
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

/**
 * Displays the player configuration interface.
 *
 * Allows users to select 2-4 players, enter names,
 * and select Ludo colors through clickable color cards.
 *
 * Input validation is performed by the GameSetup Model.
 *
 * MVC Component: View
 * SRS Requirements: FR-1.2, FR-1.4, FR-1.5
 * Iteration: 1
 */
public class GameSetupView {

    private static final int MAX_PLAYERS = 4;

    private final StackPane root;
    private final ComboBox<Integer> playerCountBox;

    private final List<TextField> nameFields;
    private final List<ToggleGroup> colorGroups;
    private final List<VBox> playerRows;

    private final Button startButton;
    private final Button backButton;

    /**
     * Creates the Game Setup screen.
     */
    public GameSetupView() {

        nameFields = new ArrayList<>();
        colorGroups = new ArrayList<>();
        playerRows = new ArrayList<>();

        // Page heading.
        Label title = new Label("NEW GAME");
        title.getStyleClass().add("menu-title");

        Label subtitle = new Label(
                "Configure your players to begin");
        subtitle.getStyleClass().add("menu-subtitle");

        // Player count selection.
        Label countLabel = new Label("Number of Players");
        countLabel.getStyleClass().add("setup-label");

        playerCountBox = new ComboBox<>();
        playerCountBox.getItems().addAll(2, 3, 4);
        playerCountBox.setValue(2);
        playerCountBox.setPrefWidth(150);
        playerCountBox.getStyleClass().add("setup-count");

        VBox countSection = new VBox(
                8, countLabel, playerCountBox);
        countSection.setMaxWidth(Double.MAX_VALUE);

        // Player configuration cards.
        VBox playersSection = new VBox(16);
        playersSection.setMaxWidth(Double.MAX_VALUE);

        for (int i = 0; i < MAX_PLAYERS; i++) {

            Label playerTitle =
                    new Label("Player " + (i + 1));
            playerTitle.getStyleClass().add("player-title");

            TextField nameField = new TextField();
            nameField.setPromptText("Enter player name");
            nameField.getStyleClass().add("setup-name");
            nameField.setMaxWidth(Double.MAX_VALUE);

            Label chooseLabel =
                    new Label("Choose your color");
            chooseLabel.getStyleClass().add("setup-label");

            // A ToggleGroup allows one color per player.
            ToggleGroup colorGroup = new ToggleGroup();

            // Arrange the four color cards in a 2x2 grid.
            GridPane colorGrid = new GridPane();
            colorGrid.setHgap(10);
            colorGrid.setVgap(10);

            ColumnConstraints left = new ColumnConstraints();
            left.setPercentWidth(50);

            ColumnConstraints right = new ColumnConstraints();
            right.setPercentWidth(50);

            colorGrid.getColumnConstraints().addAll(
                    left, right);

            PlayerColor[] colors = PlayerColor.values();

            for (int j = 0; j < colors.length; j++) {

                ToggleButton tile =
                        createColorTile(colors[j], colorGroup);

                colorGrid.add(tile, j % 2, j / 2);
                GridPane.setHgrow(tile, Priority.ALWAYS);
            }

            VBox playerRow = new VBox(
                    9,
                    playerTitle,
                    nameField,
                    chooseLabel,
                    colorGrid
            );

            playerRow.getStyleClass().add("player-panel");
            playerRow.setMaxWidth(Double.MAX_VALUE);

            nameFields.add(nameField);
            colorGroups.add(colorGroup);
            playerRows.add(playerRow);

            playersSection.getChildren().add(playerRow);
        }

        // Navigation buttons.
        backButton = new Button("Back to Menu");
        backButton.getStyleClass().addAll(
                "menu-button", "secondary-button");

        startButton = new Button("Start Game");
        startButton.getStyleClass().addAll(
                "menu-button", "primary-button");

        backButton.setMinWidth(155);
        startButton.setMinWidth(155);

        HBox actions = new HBox(
                16, backButton, startButton);
        actions.setAlignment(Pos.CENTER);

        // Main setup card.
        VBox card = new VBox(
                18,
                title,
                subtitle,
                countSection,
                playersSection,
                actions
        );

        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(650);
        card.setMaxWidth(700);
        card.getStyleClass().add("menu-card");

        // Scrolling supports smaller screens and 4 players.
        VBox page = new VBox(card);
        page.setAlignment(Pos.TOP_CENTER);
        page.setPadding(new Insets(28));

        ScrollPane scrollPane = new ScrollPane(page);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(
                ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add("setup-scroll");

        root = new StackPane(scrollPane);
        root.getStyleClass().add("menu-root");

        // Update the visible players when count changes.
        playerCountBox.setOnAction(
                event -> updateVisibleRows());

        updateVisibleRows();
    }

    /**
     * Creates a clickable color card.
     *
     * Each card displays a colored circle, its name,
     * and a checkmark when selected.
     *
     * @param color the Ludo color represented
     * @param group the player's color selection group
     * @return the configured color card
     */
    private ToggleButton createColorTile(
            PlayerColor color, ToggleGroup group) {

        ToggleButton tile = new ToggleButton();
        tile.setToggleGroup(group);
        tile.setUserData(color);
        tile.getStyleClass().add("color-choice");
        tile.setMaxWidth(Double.MAX_VALUE);
        tile.setMinHeight(46);

        Circle circle = new Circle(
                9, getColorPaint(color));

        circle.setStroke(Color.web("#BDA4AF"));
        circle.setStrokeWidth(0.8);

        String colorName = switch (color) {
            case RED -> "Red";
            case BLUE -> "Blue";
            case GREEN -> "Green";
            case YELLOW -> "Yellow";
        };

        Label name = new Label(colorName);
        name.getStyleClass().add("color-choice-label");

        // Push the checkmark to the right side.
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label checkmark = new Label("✓");
        checkmark.getStyleClass().add("color-choice-check");

        checkmark.visibleProperty().bind(
                tile.selectedProperty());

        checkmark.managedProperty().bind(
                tile.selectedProperty());

        HBox content = new HBox(
                10, circle, name, spacer, checkmark);

        content.setAlignment(Pos.CENTER_LEFT);
        content.prefWidthProperty().bind(
                tile.widthProperty().subtract(28));

        tile.setGraphic(content);

        return tile;
    }

    /**
     * Returns the display color for a Ludo color.
     *
     * @param color the player's selected color
     * @return JavaFX paint color for the circle
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
     * Shows only the selected number of player panels.
     */
    private void updateVisibleRows() {

        int count = getPlayerCount();

        for (int i = 0; i < MAX_PLAYERS; i++) {
            boolean active = i < count;

            playerRows.get(i).setVisible(active);
            playerRows.get(i).setManaged(active);
        }
    }

    /**
     * @return root layout for the setup screen
     */
    public Parent getRoot() {
        return root;
    }

    /**
     * @return selected number of players
     */
    public int getPlayerCount() {
        Integer count = playerCountBox.getValue();
        return count == null ? 2 : count;
    }

    /**
     * Returns the name input for a particular player.
     *
     * @param index zero-based player index
     * @return player's name field
     */
    public TextField getNameField(int index) {
        return nameFields.get(index);
    }

    /**
     * Returns the color selected by a particular player.
     *
     * @param index zero-based player index
     * @return selected color, or null if none is selected
     */
    public PlayerColor getSelectedColor(int index) {

        Toggle selected =
                colorGroups.get(index).getSelectedToggle();

        if (selected == null) {
            return null;
        }

        return (PlayerColor) selected.getUserData();
    }

    /**
     * @return Start Game button
     */
    public Button getStartButton() {
        return startButton;
    }

    /**
     * @return Back to Menu button
     */
    public Button getBackButton() {
        return backButton;
    }
}
