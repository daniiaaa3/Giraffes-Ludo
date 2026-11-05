
package Controller;

import View.MainMenuView;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

/**
 * Controls navigation actions from the main menu.
 *
 * The View displays the buttons, while this Controller
 * connects them to the appropriate actions.
 *
 * MVC Component: Controller
 * SRS Requirement: FR-1.1
 * Iteration: 1
 */
public class MainMenuController {

    /**
     * Connects the main menu buttons to their actions.
     *
     * @param view the main menu interface
     * @param onNewGame action that opens Game Setup
     */
    public MainMenuController(
            MainMenuView view,
            Runnable onNewGame) {

        if (view == null || onNewGame == null) {
            throw new IllegalArgumentException(
                    "Main menu dependencies cannot be null.");
        }

        // Open the real Game Setup screen.
        view.getNewGameButton().setOnAction(
                event -> onNewGame.run());

        // History will be implemented in Iteration 3.
        view.getHistoryButton().setOnAction(event -> {
            showInformation(
                    "Game History",
                    "Game History is planned for Iteration 3.");
        });

        // Question Management is planned for Iteration 3.
        view.getQuestionsButton().setOnAction(event -> {
            showInformation(
                    "Question Management",
                    "Question Management is planned for Iteration 3.");
        });
    }

    /**
     * Displays an informational dialog.
     *
     * @param title dialog title
     * @param message text displayed in the dialog
     */
    private void showInformation(
            String title, String message) {

        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
