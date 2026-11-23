package View;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import Controller.QuestionController;
import Model.Question;
import Model.QuestionDifficulty;
import Model.QuestionResultEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Window;

/**
 * An interactive, multiple-choice question window for Educational Ludo.
 *
 * The existing QuestionController and QuestionEngine own the question
 * selection, answer evaluation, and Observer notifications. This class
 * presents four answer buttons and returns their QuestionResultEvent.
 * It never changes a piece's position or advances a player's turn.
 *
 * MVC component: View
 * Iteration: 2 - Interactive Question-and-Answer Dialog
 */
public final class QuestionDialog {

    private static final String TEXT = "#57394F";
    private static final String ACCENT = "#AF578B";
    private static final String BACKGROUND = "#FFFAFD";
    private static final String OUTLINE = "#E9D8E3";

    private final QuestionController questionController;

    public QuestionDialog(QuestionController questionController) {
        this.questionController = Objects.requireNonNull(
                questionController, "Question controller is required");
    }

    /** Opens a question window without an explicit owner. */
    public Optional<QuestionResultEvent> showQuestion(
            QuestionDifficulty difficulty) {
        return showQuestion(difficulty, null);
    }

    /**
     * Opens a question window and returns the outcome after an answer.
     *
     * @param difficulty difficulty of the QUESTION SQUARE (not necessarily
     *                   the game's Medium/Hard difficulty)
     * @param owner parent window, or null when called from a standalone view
     * @return the answer event; empty only if showing the dialog fails to
     *         complete normally (e.g. its owner is being torn down)
     */
    public Optional<QuestionResultEvent> showQuestion(
            QuestionDifficulty difficulty, Window owner) {

        Objects.requireNonNull(difficulty, "Question difficulty is required");
        Question question = questionController.startQuestion(difficulty);

        try {
            return createDialog(question, owner).showAndWait();
        } finally {
            // Cancel only when there was no submitted answer. The controller
            // already clears its active question on successful submission.
            if (questionController.hasActiveQuestion()) {
                questionController.cancelQuestion();
            }
        }
    }

    private Dialog<QuestionResultEvent> createDialog(
            Question question, Window owner) {

        Dialog<QuestionResultEvent> dialog = new Dialog<>();
        dialog.setTitle("Giraffes Ludo - Question");
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) {
            dialog.initOwner(owner);
        }

        DialogPane pane = dialog.getDialogPane();
        pane.setPrefWidth(520);
        pane.setStyle("-fx-background-color: " + BACKGROUND + ";");

        ButtonType continueType = new ButtonType(
                "Continue", ButtonBar.ButtonData.OK_DONE);
        pane.getButtonTypes().add(continueType);

        Button continueButton = (Button) pane.lookupButton(continueType);
        continueButton.setDisable(true);
        continueButton.setStyle(
                "-fx-background-color: " + ACCENT + ";"
                + "-fx-text-fill: white; -fx-font-weight: bold;"
                + "-fx-background-radius: 9; -fx-padding: 10 22;");

        Label heading = new Label("QUESTION TIME!");
        heading.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;"
                + "-fx-text-fill: " + TEXT + ";");

        Label difficultyLabel = new Label(
                question.getDifficulty().name() + " QUESTION");
        String difficultyColor = switch (question.getDifficulty()) {
            case EASY -> "#E0F1E7";
            case MEDIUM -> "#FFF2CA";
            case HARD -> "#F9DFE4";
        };
        difficultyLabel.setStyle("-fx-background-color: " + difficultyColor
                + "; -fx-background-radius: 10; -fx-padding: 6 12;"
                + "-fx-font-size: 12px; -fx-font-weight: bold;"
                + "-fx-text-fill: " + TEXT + ";");

        Label questionText = new Label(question.getText());
        questionText.setWrapText(true);
        questionText.setMaxWidth(475);
        questionText.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;"
                + "-fx-text-fill: " + TEXT + ";");

        Label feedback = new Label("");
        feedback.setWrapText(true);
        feedback.setMaxWidth(475);
        feedback.setMinHeight(42);
        feedback.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;"
                + "-fx-text-fill: " + TEXT + ";");

        Label sourceLabel = new Label("");
        sourceLabel.setWrapText(true);
        sourceLabel.setMaxWidth(475);
        sourceLabel.setStyle("-fx-font-size: 11px;"
                + "-fx-text-fill: #907C8B;");

        VBox answerBox = new VBox(9);
        List<Button> answerButtons = new ArrayList<>();
        AtomicReference<QuestionResultEvent> submitted =
                new AtomicReference<>();

        for (int index = 0; index < question.getAnswers().size(); index++) {
            final int selectedIndex = index;
            Button option = new Button(
                    (char) ('A' + index) + ".  " + question.getAnswers().get(index));
            option.setWrapText(true);
            option.setMaxWidth(Double.MAX_VALUE);
            option.setMinHeight(50);
            option.setAlignment(Pos.CENTER_LEFT);
            option.setStyle(answerStyle("#FFFFFF", OUTLINE));

            option.setOnAction(event -> {
                if (submitted.get() != null) {
                    return;
                }
                // Evaluating via Controller preserves its duplicate-answer
                // protection and the QuestionEngine Observer notifications.
                QuestionResultEvent result =
                        questionController.submitAnswer(selectedIndex);
                submitted.set(result);

                for (int i = 0; i < answerButtons.size(); i++) {
                    Button button = answerButtons.get(i);
                    button.setDisable(true);
                    if (i == question.getCorrectAnswerIndex()) {
                        button.setStyle(answerStyle("#DDF2E5", "#47956E"));
                    } else if (i == selectedIndex) {
                        button.setStyle(answerStyle("#FBE1E8", "#D76A84"));
                    }
                }

                int movement = result.getMovementSteps();
                String direction = movement > 0 ? "forward" : "backward";
                if (result.isCorrect()) {
                    feedback.setText("Correct answer! Move " + direction
                            + " " + Math.abs(movement) + " square(s).");
                    feedback.setStyle("-fx-font-size: 14px;"
                            + "-fx-font-weight: bold; -fx-text-fill: #28784E;");
                } else {
                    feedback.setText("Not quite. Move " + direction
                            + " " + Math.abs(movement) + " square(s)."
                            + " Correct answer: "
                            + question.getAnswers().get(
                                    question.getCorrectAnswerIndex()));
                    feedback.setStyle("-fx-font-size: 14px;"
                            + "-fx-font-weight: bold; -fx-text-fill: #B43C59;");
                }
                sourceLabel.setText("Source: " + question.getSource());
                continueButton.setDisable(false);
                continueButton.requestFocus();
            });

            answerButtons.add(option);
            answerBox.getChildren().add(option);
        }

        VBox content = new VBox(15,
                heading, difficultyLabel, questionText,
                answerBox, feedback, sourceLabel);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.TOP_LEFT);
        pane.setContent(content);

        // Answering is mandatory: do not allow the window close button or
        // Escape to bypass the educational question.
        dialog.setOnCloseRequest(event -> {
            if (submitted.get() == null) {
                event.consume();
            }
        });
        dialog.setResultConverter(button -> submitted.get());
        return dialog;
    }

    private static String answerStyle(String fill, String border) {
        return "-fx-background-color: " + fill + ";"
                + "-fx-background-radius: 10;"
                + "-fx-border-radius: 10;"
                + "-fx-border-color: " + border + ";"
                + "-fx-border-width: 1.4;"
                + "-fx-padding: 10 14;"
                + "-fx-text-fill: " + TEXT + ";"
                + "-fx-font-size: 13px;";
    }
}
