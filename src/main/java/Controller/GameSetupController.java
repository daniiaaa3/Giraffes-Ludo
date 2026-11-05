
package Controller;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import Model.GameSetup;
import Model.GameSetup.PlayerEntry;
import View.GameSetupView;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

/**
 * Handles user interactions on the Game Setup screen.
 *
 * Collects player information from the View,
 * asks the Model to validate it, and handles
 * navigation between application screens.
 *
 * MVC Component: Controller
 * SRS Requirements: FR-1.1, FR-1.2, FR-1.4, FR-1.5
 * Iteration: 1
 */
public class GameSetupController {

    private final GameSetupView view;
    private final GameSetup model;

    /**
     * Creates the controller and registers button actions.
     *
     * @param view the Game Setup screen
     * @param model the setup validation model
     * @param onBack action for returning to the main menu
     * @param onValidSetup action after successful validation
     */
    public GameSetupController(
            GameSetupView view,
            GameSetup model,
            Runnable onBack,
            Consumer<List<PlayerEntry>> onValidSetup) {

        if (view == null || model == null ||
                onBack == null || onValidSetup == null) {
            throw new IllegalArgumentException(
                    "Controller dependencies cannot be null.");
        }

        this.view = view;
        this.model = model;

        // Return to the main menu.
        view.getBackButton().setOnAction(
                event -> onBack.run());

        // Validate player inputs before continuing.
        view.getStartButton().setOnAction(
                event -> handleStartGame(onValidSetup));
    }

    /**
     * Collects player information and validates it
     * before notifying the application.
     *
     * @param onValidSetup callback receiving valid players
     */
    private void handleStartGame(
            Consumer<List<PlayerEntry>> onValidSetup) {

        List<PlayerEntry> players = new ArrayList<>();

        // Read only the number of players selected.
        int count = view.getPlayerCount();

        for (int i = 0; i < count; i++) {

            String name = view.getNameField(i).getText();

            GameSetup.PlayerColor color =
                    view.getSelectedColor(i);

            players.add(new PlayerEntry(name, color));
        }

        try {

            // The Model checks all setup requirements.
            List<PlayerEntry> validPlayers =
                    model.validate(players);

            // Continue only when validation succeeds.
            onValidSetup.accept(validPlayers);

        } catch (IllegalArgumentException exception) {

            // Inform the player about invalid input.
            showError(exception.getMessage());
        }
    }

    /**
     * Displays an error when player setup is invalid.
     *
     * @param message explanation of the validation error
     */
    private void showError(String message) {

        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Invalid Game Setup");
        alert.setHeaderText("Please check your players");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
