
package View;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Displays the main menu of the Educational Ludo Game.
 *
 * Creates the visual elements and assigns CSS style
 * classes. Button actions are managed separately
 * by MainMenuController.
 *
 * MVC Component: View
 * SRS Requirement: FR-1.1
 * Iteration: 1
 */
public class MainMenuView {

    private final VBox root;

    private final Button newGameButton;
    private final Button historyButton;
    private final Button questionsButton;

    /**
     * Creates the main menu interface.
     */
    public MainMenuView() {

        Label eyebrow = new Label(
                "GIRAFFES GROUP · EDUCATIONAL GAME");
        eyebrow.getStyleClass().add("menu-eyebrow");

        Label title = new Label("GIRAFFES LUDO");
        title.getStyleClass().add("menu-title");

        Label subtitle = new Label("Learn. Play. Win.");
        subtitle.getStyleClass().add("menu-subtitle");

        // Create buttons using shared CSS styles.
        newGameButton = createButton(
                "New Game", "primary-button");

        historyButton = createButton(
                "Game History", "secondary-button");

        questionsButton = createButton(
                "Question Management", "secondary-button");

        VBox buttons = new VBox(
                12,
                newGameButton,
                historyButton,
                questionsButton
        );
        buttons.setAlignment(Pos.CENTER);
        buttons.setMaxWidth(310);

        Label footer = new Label(
                "Educational Ludo · Iteration 1");
        footer.getStyleClass().add("menu-footer");

        // Central card containing the menu.
        VBox card = new VBox(
                18,
                eyebrow,
                title,
                subtitle,
                buttons,
                footer
        );
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(480);
        card.getStyleClass().add("menu-card");

        // Background layout.
        root = new VBox(card);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(24));
        root.getStyleClass().add("menu-root");
    }

    /**
     * Creates a navigation button with shared styles.
     *
     * @param text label displayed on the button
     * @param styleClass CSS class for button appearance
     * @return configured navigation button
     */
    private Button createButton(
            String text, String styleClass) {

        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setPrefHeight(50);

        button.getStyleClass().addAll(
                "menu-button", styleClass);

        return button;
    }

    /**
     * @return root layout of the main menu
     */
    public Parent getRoot() {
        return root;
    }

    /**
     * @return New Game button
     */
    public Button getNewGameButton() {
        return newGameButton;
    }

    /**
     * @return Game History button
     */
    public Button getHistoryButton() {
        return historyButton;
    }

    /**
     * @return Question Management button
     */
    public Button getQuestionsButton() {
        return questionsButton;
    }
}
