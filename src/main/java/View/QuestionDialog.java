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
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * Compact, accessible question dialog for Educational Ludo.
 *
 * The QuestionController handles selection/answer submission.
 * GameplayController handles movement after this modal dialog returns.
 * This view never changes the game state or advances a turn.
 */
public final class QuestionDialog {

    private static final String INK = "#55354B";
    private static final String MUTED = "#8D7385";
    private static final String PINK = "#B3558E";
    private static final String BORDER = "#EBD8E3";
    private static final String PANEL = "#FFFAFD";

    // Explicit dimensions prevent the answer controls from expanding
    // inside ScrollPane (the issue seen in the previous design).
    private static final double OPTION_WIDTH = 510;
    private static final double CONTENT_WIDTH = 532;
    private static final double VIEWPORT_HEIGHT = 465;

    private final QuestionController questionController;

    public QuestionDialog(QuestionController questionController) {
        this.questionController = Objects.requireNonNull(
                questionController, "Question controller is required");
    }

    public Optional<QuestionResultEvent> showQuestion(
            QuestionDifficulty difficulty) {
        return showQuestion(difficulty, null);
    }

    /** Opens a non-skippable modal question, optionally owned by the game. */
    public Optional<QuestionResultEvent> showQuestion(
            QuestionDifficulty difficulty, Window owner) {
        Objects.requireNonNull(difficulty, "Question difficulty is required");
        Question question = questionController.startQuestion(difficulty);
        Rectangle backdrop = installBackdrop(owner);

        try {
            return createDialog(question, owner).showAndWait();
        } finally {
            if (questionController.hasActiveQuestion()) {
                questionController.cancelQuestion();
            }
            removeBackdrop(backdrop);
        }
    }

    private Dialog<QuestionResultEvent> createDialog(
            Question question, Window owner) {
        Dialog<QuestionResultEvent> dialog = new Dialog<>();
        dialog.setTitle("Giraffes Ludo - Question");
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) {
            dialog.initOwner(owner);
        }

        DialogPane pane = dialog.getDialogPane();
        pane.setPrefWidth(620);
        pane.setMinWidth(620);
        pane.setStyle(
                "-fx-background-color: " + PANEL + ";"
                + "-fx-background-radius: 22;"
                + "-fx-border-color: " + BORDER + ";"
                + "-fx-border-radius: 22; -fx-border-width: 1.3;"
                + "-fx-padding: 14 24 16 24;"
                + "-fx-effect: dropshadow(gaussian, rgba(75,37,65,0.22), 22, 0.18, 0, 8);");

        ButtonType continueType = new ButtonType(
                "Continue", ButtonBar.ButtonData.OK_DONE);
        pane.getButtonTypes().add(continueType);
        Button continueButton = (Button) pane.lookupButton(continueType);
        continueButton.setDisable(true);
        continueButton.setMinSize(144, 42);
        continueButton.setStyle(primaryButtonStyle());

        Label eyebrow = new Label("✦  EDUCATIONAL CHALLENGE");
        eyebrow.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;"
                + "-fx-text-fill: " + PINK + ";");

        Label title = new Label("Question time!");
        title.setStyle("-fx-font-size: 23px; -fx-font-weight: bold;"
                + "-fx-text-fill: " + INK + ";");

        Label hint = new Label("Choose one answer to continue the game");
        hint.setStyle("-fx-font-size: 12px; -fx-text-fill: " + MUTED + ";");

        VBox titles = new VBox(4, eyebrow, title, hint);
        titles.setAlignment(Pos.CENTER_LEFT);

        Label difficultyBadge = new Label(question.getDifficulty().name()
                + "  ·  +/-" + stepsFor(question.getDifficulty()) + " STEPS");
        difficultyBadge.setStyle(badgeStyle(question.getDifficulty()));

        // Pin the difficulty badge to the RIGHT edge of the heading.
        // A BorderPane is more reliable than a content-sized HBox here.
        BorderPane heading = new BorderPane();
        heading.setLeft(titles);
        heading.setRight(difficultyBadge);
        heading.setMinWidth(OPTION_WIDTH);
        heading.setPrefWidth(OPTION_WIDTH);
        heading.setMaxWidth(OPTION_WIDTH);
        BorderPane.setAlignment(titles, Pos.CENTER_LEFT);
        BorderPane.setAlignment(difficultyBadge, Pos.CENTER_RIGHT);

        Region divider = new Region();
        divider.setMinHeight(1);
        divider.setPrefHeight(1);
        divider.setMaxHeight(1);
        divider.setStyle("-fx-background-color: " + BORDER + ";");

        Label questionText = new Label(question.getText());
        questionText.setWrapText(true);
        questionText.setPrefWidth(OPTION_WIDTH);
        questionText.setMaxWidth(OPTION_WIDTH);
        questionText.setStyle("-fx-font-size: 17px; -fx-font-weight: bold;"
                + "-fx-text-fill: " + INK + ";");

        Label chooseLabel = new Label("SELECT YOUR ANSWER");
        chooseLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;"
                + "-fx-text-fill: " + MUTED + ";");

        VBox answers = new VBox(7);
        answers.setAlignment(Pos.TOP_LEFT);
        List<Button> answerButtons = new ArrayList<>();
        AtomicReference<QuestionResultEvent> submitted = new AtomicReference<>();

        Label feedbackTitle = new Label();
        feedbackTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");
        Label feedbackDetails = new Label();
        feedbackDetails.setWrapText(true);
        feedbackDetails.setPrefWidth(OPTION_WIDTH - 36);
        feedbackDetails.setStyle("-fx-font-size: 13px; -fx-text-fill: " + INK + ";");
        Label sourceLabel = new Label();
        sourceLabel.setWrapText(true);
        sourceLabel.setPrefWidth(OPTION_WIDTH - 36);
        sourceLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + MUTED + ";");

        VBox feedbackCard = new VBox(7, feedbackTitle, feedbackDetails, sourceLabel);
        feedbackCard.setPadding(new Insets(14));
        feedbackCard.setPrefWidth(OPTION_WIDTH);
        feedbackCard.setMaxWidth(OPTION_WIDTH);
        feedbackCard.setVisible(false);
        feedbackCard.setManaged(false);

        // Compact vertical rhythm: the prompt, instruction and four
        // answers remain together instead of leaving large blank spaces.
        VBox content = new VBox(8,
                heading, divider, questionText, chooseLabel, answers, feedbackCard);
        content.setPadding(new Insets(6, 8, 6, 8));
        content.setPrefWidth(CONTENT_WIDTH);
        content.setMinWidth(CONTENT_WIDTH);
        content.setFillWidth(false);
        content.setAlignment(Pos.TOP_LEFT);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(false);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setPrefViewportHeight(VIEWPORT_HEIGHT);
        scroll.setPrefHeight(VIEWPORT_HEIGHT + 2);
        scroll.setMinHeight(VIEWPORT_HEIGHT + 2);
        scroll.setMaxHeight(VIEWPORT_HEIGHT + 2);
        scroll.setStyle("-fx-background-color: transparent;"
                + "-fx-background: transparent; -fx-border-color: transparent;");
        pane.setContent(scroll);

        for (int i = 0; i < question.getAnswers().size(); i++) {
            final int selectedIndex = i;
            String answer = question.getAnswers().get(i);
            String letter = String.valueOf((char) ('A' + i));
            Button option = new Button(letter + ".   " + answer);
            option.setAlignment(Pos.CENTER_LEFT);
            option.setWrapText(true);
            option.setPrefWidth(OPTION_WIDTH);
            option.setMinWidth(OPTION_WIDTH);
            option.setMaxWidth(OPTION_WIDTH);

            // A fixed, sensible height for each answer (including long ones).
            double optionHeight = answer.length() > 95 ? 76
                    : answer.length() > 62 ? 62 : 50;
            option.setPrefHeight(optionHeight);
            option.setMinHeight(optionHeight);
            option.setMaxHeight(optionHeight);
            option.setStyle(answerStyle("#FFFFFF", BORDER));

            option.setOnMouseEntered(event -> {
                if (submitted.get() == null) {
                    option.setStyle(answerStyle("#FFF1F8", PINK));
                }
            });
            option.setOnMouseExited(event -> {
                if (submitted.get() == null) {
                    option.setStyle(answerStyle("#FFFFFF", BORDER));
                }
            });
            option.setOnAction(event -> {
                if (submitted.get() != null) {
                    return;
                }

                // Submission, correctness and Observer notifications belong
                // to the existing subsystem (Dania's implementation).
                QuestionResultEvent result =
                        questionController.submitAnswer(selectedIndex);
                submitted.set(result);

                for (int j = 0; j < answerButtons.size(); j++) {
                    Button button = answerButtons.get(j);
                    // Prevent another click without dimming the answer colors.
                    button.setOnAction(null);
                    button.setMouseTransparent(true);
                    button.setFocusTraversable(false);
                    if (j == question.getCorrectAnswerIndex()) {
                        button.setStyle(answerStyle("#E6F5EC", "#4CA47A"));
                    } else if (j == selectedIndex) {
                        button.setStyle(answerStyle("#FFF0F3", "#D8748C"));
                    } else {
                        button.setStyle(answerStyle("#FFFFFF", BORDER));
                    }
                }

                int steps = Math.abs(result.getMovementSteps());
                if (result.isCorrect()) {
                    feedbackTitle.setText("✓  Correct answer!");
                    feedbackTitle.setStyle("-fx-font-size: 15px;"
                            + "-fx-font-weight: bold; -fx-text-fill: #277A50;");
                    feedbackDetails.setText("Your piece moves forward "
                            + steps + " square(s).");
                    feedbackCard.setStyle("-fx-background-color: #E9F7EF;"
                            + "-fx-background-radius: 12;"
                            + "-fx-border-color: #C7E9D5; -fx-border-radius: 12;");
                } else {
                    feedbackTitle.setText("Not quite — keep learning!");
                    feedbackTitle.setStyle("-fx-font-size: 15px;"
                            + "-fx-font-weight: bold; -fx-text-fill: #B34164;");
                    feedbackDetails.setText("Your piece moves backward " + steps
                            + " square(s). Correct answer: "
                            + question.getAnswers().get(question.getCorrectAnswerIndex()));
                    feedbackCard.setStyle("-fx-background-color: #FFF0F3;"
                            + "-fx-background-radius: 12;"
                            + "-fx-border-color: #F2CCD6; -fx-border-radius: 12;");
                }
                sourceLabel.setText("Source: " + question.getSource());
                feedbackCard.setManaged(true);
                feedbackCard.setVisible(true);
                feedbackCard.setOpacity(0);
                FadeTransition reveal = new FadeTransition(
                        Duration.millis(200), feedbackCard);
                reveal.setFromValue(0);
                reveal.setToValue(1);
                reveal.play();

                continueButton.setDisable(false);
                // If feedback extends below the viewport, show it immediately.
                Platform.runLater(() -> scroll.setVvalue(1.0));
            });
            answerButtons.add(option);
            answers.getChildren().add(option);
        }

        dialog.setOnCloseRequest(event -> {
            if (submitted.get() == null) {
                event.consume();
            }
        });
        dialog.setResultConverter(button -> submitted.get());
        dialog.setOnShowing(event -> {
            Scene scene = pane.getScene();
            if (scene != null) {
                scene.setFill(Color.TRANSPARENT);
            }
        });
        dialog.setOnShown(event -> {
            pane.setOpacity(0);
            pane.setScaleX(0.97);
            pane.setScaleY(0.97);
            FadeTransition fade = new FadeTransition(Duration.millis(190), pane);
            fade.setFromValue(0);
            fade.setToValue(1);
            ScaleTransition scale = new ScaleTransition(Duration.millis(190), pane);
            scale.setToX(1);
            scale.setToY(1);
            new ParallelTransition(fade, scale).play();
        });
        return dialog;
    }

    /** Subtle backdrop over gameplay while the modal question is open. */
    private static Rectangle installBackdrop(Window owner) {
        if (owner == null || owner.getScene() == null) {
            return null;
        }
        Parent root = owner.getScene().getRoot();
        if (!(root instanceof Pane pane)) {
            return null;
        }
        Rectangle shade = new Rectangle();
        shade.setManaged(false);
        shade.setFill(Color.rgb(51, 26, 48, 0.30));
        shade.widthProperty().bind(pane.widthProperty());
        shade.heightProperty().bind(pane.heightProperty());
        pane.getChildren().add(shade);
        shade.toFront();
        return shade;
    }

    private static void removeBackdrop(Rectangle backdrop) {
        if (backdrop == null) {
            return;
        }
        if (backdrop.getParent() instanceof Pane pane) {
            pane.getChildren().remove(backdrop);
        }
        backdrop.widthProperty().unbind();
        backdrop.heightProperty().unbind();
    }

    private static int stepsFor(QuestionDifficulty difficulty) {
        return switch (difficulty) {
            case EASY -> 1;
            case MEDIUM -> 2;
            case HARD -> 3;
        };
    }

    private static String badgeStyle(QuestionDifficulty difficulty) {
        String bg = switch (difficulty) {
            case EASY -> "#E2F4E9";
            case MEDIUM -> "#FFF1CE";
            case HARD -> "#FBE0E8";
        };
        String fg = switch (difficulty) {
            case EASY -> "#277A50";
            case MEDIUM -> "#8B651E";
            case HARD -> "#B34164";
        };
        return "-fx-background-color: " + bg + ";"
                + "-fx-background-radius: 12; -fx-padding: 9 12;"
                + "-fx-font-size: 11px; -fx-font-weight: bold;"
                + "-fx-text-fill: " + fg + ";";
    }

    private static String answerStyle(String bg, String border) {
        return "-fx-background-color: " + bg + ";"
                + "-fx-background-radius: 12; -fx-background-insets: 0;"
                + "-fx-border-color: " + border + ";"
                + "-fx-border-width: 1.3; -fx-border-radius: 12;"
                + "-fx-padding: 8 14; -fx-cursor: hand;"
                + "-fx-font-size: 13px; -fx-text-fill: " + INK + ";";
    }

    private static String primaryButtonStyle() {
        return "-fx-background-color: " + PINK + ";"
                + "-fx-background-radius: 12; -fx-background-insets: 0;"
                + "-fx-text-fill: white; -fx-font-size: 13px;"
                + "-fx-font-weight: bold; -fx-padding: 10 26;";
    }
}
