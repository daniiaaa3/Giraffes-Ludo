
package Controller;

import Model.StepsDice;
import View.GameplayView;

/**
 * Connects the Steps die to the Gameplay UI.
 *
 * MVC Component: Controller
 * Iteration: 1
 */
public class DiceController {

    private final StepsDice dice;
    private final GameplayView view;

    public DiceController(StepsDice dice, GameplayView view) {

        if (dice == null || view == null) {
            throw new IllegalArgumentException(
                    "Dice and view cannot be null.");
        }

        this.dice = dice;
        this.view = view;

        view.getRollButton().setOnAction(event -> rollDice());
    }

    private void rollDice() {

        int result = dice.roll();

        view.setDiceResult(result);
        view.showGameMessage("You rolled a " + result + "!");
    }
}
