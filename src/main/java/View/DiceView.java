
package View;

import java.util.Random;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.RotateTransition;
import javafx.geometry.Pos;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

/**
 * Graphical representation of the Steps die.
 *
 * Displays values 1-6 using dice pips and
 * supports a short visual rolling animation.
 *
 * MVC Component: View
 * Iteration: 1
 */
public class DiceView extends StackPane {

    private static final int DICE_SIZE = 90;
    private static final double PIP_RADIUS = 6;

    private final GridPane pipGrid;
    private final Random animationRandom = new Random();

    private boolean rolling = false;

    public DiceView() {

        setMinSize(DICE_SIZE, DICE_SIZE);
        setPrefSize(DICE_SIZE, DICE_SIZE);
        setMaxSize(DICE_SIZE, DICE_SIZE);

        setAlignment(Pos.CENTER);

        setStyle(
                "-fx-background-color: white;"
                + "-fx-background-radius: 18;"
                + "-fx-border-color: #D88CAD;"
                + "-fx-border-width: 2;"
                + "-fx-border-radius: 18;"
                + "-fx-effect: dropshadow(gaussian, "
                + "rgba(110, 45, 80, 0.22), "
                + "12, 0.2, 0, 5);"
        );

        pipGrid = new GridPane();
        pipGrid.setAlignment(Pos.CENTER);
        pipGrid.setHgap(10);
        pipGrid.setVgap(10);

        getChildren().add(pipGrid);

        
    }

    /**
     * Displays a specific die value.
     */
    public void setValue(int value) {

        if (value < 1 || value > 6) {
            throw new IllegalArgumentException(
                    "Dice value must be between 1 and 6.");
        }

        pipGrid.getChildren().clear();

        switch (value) {

            case 1 -> {
                addPip(0, 0);
            }

            case 2 -> {
                addPip(0, 0);
                addPip(2, 2);
            }

            case 3 -> {
                addPip(0, 0);
                addPip(1, 1);
                addPip(2, 2);
            }

            case 4 -> {
                addPip(0, 0);
                addPip(2, 0);
                addPip(0, 2);
                addPip(2, 2);
            }

            case 5 -> {
                addPip(0, 0);
                addPip(2, 0);
                addPip(1, 1);
                addPip(0, 2);
                addPip(2, 2);
            }

            case 6 -> {
                addPip(0, 0);
                addPip(0, 1);
                addPip(0, 2);
                addPip(2, 0);
                addPip(2, 1);
                addPip(2, 2);
            }
        }
    }

    /**
     * Adds a single dot to the die.
     */
    private void addPip(int column, int row) {

        Circle pip = new Circle(PIP_RADIUS);

        pip.setFill(Color.web("#63324F"));

        pipGrid.add(pip, column, row);
    }

    /**
     * Plays a visual rolling animation.
     *
     * The final value must come from StepsDice,
     * not from this animation.
     */
    public void animateRoll(
            int finalValue,
            Runnable onFinished) {

        if (rolling) {
            return;
        }

        if (finalValue < 1 || finalValue > 6) {
            throw new IllegalArgumentException(
                    "Dice value must be between 1 and 6.");
        }

        rolling = true;

        Timeline rollingTimeline = new Timeline(
                new KeyFrame(
                        Duration.millis(70),
                        event -> {
                            int temporaryValue =
                                    animationRandom.nextInt(6) + 1;

                            setValue(temporaryValue);
                        }
                )
        );

        rollingTimeline.setCycleCount(8);

        RotateTransition rotation =
                new RotateTransition(
                        Duration.millis(560),
                        this
                );

        rotation.setByAngle(360);
        rotation.setCycleCount(1);

        rollingTimeline.setOnFinished(event -> {

            setValue(finalValue);
            rolling = false;

            if (onFinished != null) {
                onFinished.run();
            }
        });

        rotation.play();
        rollingTimeline.play();
    }

    /**
     * Indicates whether the animation is active.
     */
    public boolean isRolling() {
        return rolling;
    }
}
