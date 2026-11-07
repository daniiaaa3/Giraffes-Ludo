package Controller;

import View.GameDialog;
import View.MainMenuView;

/**
 * Handles actions from the main menu.
 *
 * Navigation is requested through a callback. Features planned
 * for later iterations show consistent game-themed dialogs.
 *
 * MVC Component: Controller
 * SRS Requirement: FR-1.1
 * Iteration: 1
 */
public class MainMenuController {

    /**
     * Connects main menu buttons to their actions.
     *
     * @param view main menu interface
     * @param onNewGame callback opening the Game Setup screen
     */
    public MainMenuController(MainMenuView view, Runnable onNewGame) {
        if (view == null || onNewGame == null) {
            throw new IllegalArgumentException(
                    "Main menu dependencies cannot be null.");
        }

        view.getNewGameButton().setOnAction(event -> onNewGame.run());

        view.getHistoryButton().setOnAction(event -> GameDialog.showInfo(
                view.getHistoryButton().getScene().getWindow(),
                "Game History",
                "Game history is coming soon"));

        view.getQuestionsButton().setOnAction(event -> GameDialog.showInfo(
                view.getQuestionsButton().getScene().getWindow(),
                "Question Management",
                "Question management is coming soon"));
    }
}
