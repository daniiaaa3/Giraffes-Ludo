
package View;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Displays the animated Giraffes Ludo main menu.
 *
 * Provides navigation buttons, a Ludo board preview,
 * and subtle animations that improve the experience.
 *
 * Button actions are assigned by MainMenuController.
 *
 * MVC Component: View
 * SRS Requirement: FR-1.1
 * Iteration: 1 - UI enhancement
 */
public class MainMenuView {

    private final StackPane root;

    private final Button newGameButton;
    private final Button historyButton;
    private final Button questionsButton;

    /**
     * Constructs the main menu.
     */
    public MainMenuView() {

        Label eyebrow = new Label(
                "GIRAFFES GROUP PRESENTS");
        eyebrow.getStyleClass().add("hero-eyebrow");

        Label title = new Label("GIRAFFES LUDO");
        title.getStyleClass().add("hero-title");

        Label subtitle = new Label(
                "Roll the dice and race home");
        subtitle.getStyleClass().add("hero-subtitle");

        Label description = new Label(
                "A playful Ludo game for 2 to 4 "
                + "players on one computer");
        description.setWrapText(true);
        description.getStyleClass().add(
                "hero-description");

        // Keep the same button getters used
        // by MainMenuController.
        newGameButton = createButton(
                "New Game", "primary-button");

        historyButton = createButton(
                "Game History", "secondary-button");

        questionsButton = createButton(
                "Question Management",
                "secondary-button");

        VBox buttons = new VBox(
                12,
                newGameButton,
                historyButton,
                questionsButton
        );

        buttons.setMaxWidth(300);

        Label footer = new Label(
                "EDUCATIONAL LUDO");
        footer.getStyleClass().add("hero-footer");

        VBox leftSide = new VBox(
                17,
                eyebrow,
                title,
                subtitle,
                description,
                buttons,
                footer
        );

        leftSide.setAlignment(Pos.CENTER_LEFT);
        leftSide.setPrefWidth(320);

        // Animated visual preview.
        LudoBoardPreview board =
                new LudoBoardPreview();

        Label previewTitle = new Label(
                "READY FOR YOUR NEXT MATCH?");
        previewTitle.getStyleClass().add(
                "hero-preview-title");

        Label previewCaption = new Label(
                "Choose your colors and start playing");
        previewCaption.getStyleClass().add(
                "hero-preview-caption");

        VBox rightSide = new VBox(
                18,
                previewTitle,
                board,
                previewCaption
        );

        rightSide.setAlignment(Pos.CENTER);
        rightSide.setPrefWidth(300);
        rightSide.getStyleClass().add(
                "hero-preview-panel");

        HBox heroCard = new HBox(
                26, leftSide, rightSide);

        heroCard.setAlignment(Pos.CENTER);
        heroCard.setMaxWidth(850);
        heroCard.setMaxHeight(660);
        heroCard.getStyleClass().add("hero-card");

        root = new StackPane(heroCard);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("menu-root");

        playEntranceAnimation(heroCard);
    }

    /**
     * Creates a styled button with hover animation.
     *
     * @param text button text
     * @param styleClass CSS class for its appearance
     * @return configured button
     */
    private Button createButton(
            String text,
            String styleClass) {

        Button button = new Button(text);

        button.getStyleClass().addAll(
                "menu-button", styleClass);

        button.setMaxWidth(Double.MAX_VALUE);
        button.setPrefHeight(51);

        // Smooth enlargement on mouse hover.
        button.setOnMouseEntered(
                event -> animateButton(button, 1.035));

        button.setOnMouseExited(
                event -> animateButton(button, 1.0));

        return button;
    }

    /**
     * Smoothly scales a button to the requested size.
     *
     * @param button the button to animate
     * @param scale target scale factor
     */
    private void animateButton(
            Button button,
            double scale) {

        ScaleTransition transition =
                new ScaleTransition(
                        Duration.millis(140), button);

        transition.setToX(scale);
        transition.setToY(scale);
        transition.play();
    }

    /**
     * Fades and slides the menu into view.
     *
     * @param content the menu card
     */
    private void playEntranceAnimation(
            HBox content) {

        FadeTransition fade =
                new FadeTransition(
                        Duration.millis(480), content);

        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition slide =
                new TranslateTransition(
                        Duration.millis(480), content);

        slide.setFromY(18);
        slide.setToY(0);

        new ParallelTransition(fade, slide).play();
    }

    /**
     * @return root menu layout
     */
    public Parent getRoot() {
        return root;
    }

    /**
     * @return New Game navigation button
     */
    public Button getNewGameButton() {
        return newGameButton;
    }

    /**
     * @return Game History navigation button
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
