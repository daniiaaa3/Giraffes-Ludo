package Controller;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import Model.GameSetup;
import Model.GameSetup.PlayerEntry;
import View.GameDialog;
import View.GameSetupView;

/**
 * Handles user interaction and validation for the Game Setup screen.
 *
 * Reads player data from the View, asks the Model to validate it,
 * and reports the result to the application through a callback.
 *
 * MVC Component: Controller
 * SRS Requirements: FR-1.1, FR-1.2, FR-1.4, FR-1.5
 * Iteration: 1
 */
public class GameSetupController {

    private final GameSetupView view;
    private final GameSetup model;

    /**
     * Connects the setup View and Model with navigation actions.
     *
     * @param view Game Setup screen
     * @param model validation model
     * @param onBack callback returning to the main menu
     * @param onValidSetup callback receiving validated player details
     */
    public GameSetupController(
            GameSetupView view,
            GameSetup model,
            Runnable onBack,
            Consumer<List<PlayerEntry>> onValidSetup) {

        if (view == null || model == null
                || onBack == null || onValidSetup == null) {
            throw new IllegalArgumentException(
                    "Controller dependencies cannot be null.");
        }

        this.view = view;
        this.model = model;

        view.getBackButton().setOnAction(event -> onBack.run());
        view.getStartButton().setOnAction(
                event -> handleStartGame(onValidSetup));
    }

    /**
     * Validates the active players before continuing to gameplay.
     *
     * @param onValidSetup callback called only for valid setups
     */
    private void handleStartGame(
            Consumer<List<PlayerEntry>> onValidSetup) {

        List<PlayerEntry> players = new ArrayList<>();

        for (int i = 0; i < view.getPlayerCount(); i++) {
            String name = view.getNameField(i).getText();
            GameSetup.PlayerColor color = view.getSelectedColor(i);
            players.add(new PlayerEntry(name, color));
        }

        try {
            List<PlayerEntry> validPlayers = model.validate(players);
            onValidSetup.accept(validPlayers);
        } catch (IllegalArgumentException exception) {
            GameDialog.showError(
                    view.getStartButton().getScene().getWindow(),
                    "Check Your Players",
                    exception.getMessage());
        }
    }
}
