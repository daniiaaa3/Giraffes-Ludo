package View;

import javafx.animation.ScaleTransition;
import javafx.scene.Node;
import javafx.scene.control.ButtonBase;

/**
 * Small, consistent hover and press animations for buttons.
 *
 * MVC Component: View
 * Iteration: 1
 */
final class UiEffects {

    private UiEffects() {
    }

    /**
     * Makes a button grow slightly on hover and press in
     * slightly, like the other buttons in the game.
     *
     * @param button the button to animate
     */
    static void interactive(ButtonBase button) {
        button.setOnMouseEntered(event -> scale(button, 1.045));
        button.setOnMouseExited(event -> scale(button, 1.0));
        button.setOnMousePressed(event -> scale(button, 0.965));
        button.setOnMouseReleased(event ->
                scale(button, button.isHover() ? 1.045 : 1.0));
    }

    private static void scale(Node node, double target) {
        if (node.isDisabled()) {
            target = 1.0;
        }
        ScaleTransition animation =
                new ScaleTransition(Motion.ms(120), node);
        animation.setToX(target);
        animation.setToY(target);
        animation.play();
    }
}
