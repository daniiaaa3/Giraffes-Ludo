package View;

import java.net.URL;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.Effect;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * Reusable, game-themed modal dialogs for Giraffes Ludo.
 *
 * Displays success, error, and information messages using the
 * shared pink visual style. The dialog blocks interaction with
 * its owner until dismissed, supports Enter and Escape, and
 * provides a gentle entrance animation.
 *
 * MVC Component: View
 * Iteration: 1 - UI enhancement
 */
public final class GameDialog {

    /** Supported message styles. */
    private enum Type {
        INFO, SUCCESS, ERROR
    }

    /** Utility class; do not instantiate. */
    private GameDialog() {
    }

    /**
     * Shows a friendly information dialog.
     *
     * @param owner window displaying the dialog, or null
     * @param title main message heading
     * @param message explanation shown below the heading
     */
    public static void showInfo(Window owner, String title, String message) {
        show(owner, Type.INFO, title, message);
    }

    /**
     * Shows a confirmation dialog after successful validation.
     *
     * @param owner window displaying the dialog, or null
     * @param title main message heading
     * @param message explanation shown below the heading
     */
    public static void showSuccess(Window owner, String title, String message) {
        show(owner, Type.SUCCESS, title, message);
    }

    /**
     * Shows an error dialog for invalid input.
     *
     * @param owner window displaying the dialog, or null
     * @param title main message heading
     * @param message explanation shown below the heading
     */
    public static void showError(Window owner, String title, String message) {
        show(owner, Type.ERROR, title, message);
    }

    /**
     * Builds and displays a modal window with no operating-system title bar.
     *
     * @param owner application window that owns the dialog
     * @param type visual style of the dialog
     * @param title dialog heading
     * @param message detailed message
     */
    private static void show(Window owner, Type type,
                             String title, String message) {

        Stage popup = new Stage(StageStyle.TRANSPARENT);
        if (owner != null) {
            popup.initOwner(owner);
            popup.initModality(Modality.WINDOW_MODAL);
        } else {
            popup.initModality(Modality.APPLICATION_MODAL);
        }
        popup.setTitle("Giraffes Ludo - " + title);
        popup.setResizable(false);

        // Header: icon, small brand label, clear heading.
        Label symbol = new Label(switch (type) {
            case SUCCESS -> "✓";
            case ERROR -> "!";
            case INFO -> "i";
        });
        symbol.getStyleClass().add("dialog-symbol");

        StackPane symbolBadge = new StackPane(symbol);
        symbolBadge.getStyleClass().add("dialog-symbol-badge");
        symbolBadge.setMinSize(54, 54);
        symbolBadge.setMaxSize(54, 54);

        Label eyebrow = new Label("GIRAFFES LUDO");
        eyebrow.getStyleClass().add("dialog-eyebrow");

        Label heading = new Label(title);
        heading.setWrapText(true);
        heading.getStyleClass().add("dialog-heading");

        Label description = new Label(message);
        description.setWrapText(true);
        description.setMaxWidth(365);
        description.getStyleClass().add("dialog-description");

        Button closeButton = new Button(switch (type) {
            case SUCCESS -> "Continue";
            case ERROR -> "Try Again";
            case INFO -> "Got It";
        });
        closeButton.getStyleClass().add("dialog-action");
        closeButton.setMinWidth(136);
        closeButton.setDefaultButton(true);
        closeButton.setOnAction(event -> popup.close());

        HBox actions = new HBox(closeButton);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setPadding(new Insets(10, 0, 0, 0));

        VBox card = new VBox(13, symbolBadge, eyebrow,
                heading, description, actions);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(30, 32, 26, 32));
        card.setPrefWidth(435);
        card.setMaxWidth(435);
        card.getStyleClass().addAll("game-dialog-card", switch (type) {
            case SUCCESS -> "dialog-success";
            case ERROR -> "dialog-error";
            case INFO -> "dialog-info";
        });

        // Transparent padding gives the rounded card space for its shadow.
        StackPane root = new StackPane(card);
        root.setPadding(new Insets(22));
        root.setStyle("-fx-background-color: transparent;");

        Scene popupScene = new Scene(root);
        popupScene.setFill(Color.TRANSPARENT);
        URL css = GameDialog.class.getResource("/styles/game-dialog.css");
        if (css == null) {
            throw new IllegalStateException(
                    "Missing stylesheet: /styles/game-dialog.css");
        }
        popupScene.getStylesheets().add(css.toExternalForm());
        popupScene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                popup.close();
                event.consume();
            }
        });
        popup.setScene(popupScene);

        // Animate gently rather than abruptly appearing.
        card.setOpacity(0);
        card.setScaleX(0.96);
        card.setScaleY(0.96);
        card.setTranslateY(10);

        popup.setOnShown(event -> {
            if (owner != null) {
                popup.setX(owner.getX()
                        + (owner.getWidth() - popup.getWidth()) / 2);
                popup.setY(owner.getY()
                        + (owner.getHeight() - popup.getHeight()) / 2);
            }

            FadeTransition fade = new FadeTransition(
                    Duration.millis(230), card);
            fade.setToValue(1);

            ScaleTransition scale = new ScaleTransition(
                    Duration.millis(230), card);
            scale.setToX(1);
            scale.setToY(1);

            TranslateTransition slide = new TranslateTransition(
                    Duration.millis(230), card);
            slide.setToY(0);

            new ParallelTransition(fade, scale, slide).play();
            Platform.runLater(closeButton::requestFocus);
        });

        // Soften the underlying game window while the modal is open.
        Parent ownerRoot = owner != null && owner.getScene() != null
                ? owner.getScene().getRoot() : null;
        Effect previousEffect = ownerRoot == null
                ? null : ownerRoot.getEffect();

        try {
            if (ownerRoot != null) {
                ownerRoot.setEffect(new GaussianBlur(3.5));
            }
            popup.showAndWait();
        } finally {
            if (ownerRoot != null) {
                ownerRoot.setEffect(previousEffect);
            }
        }
    }
}
