
package View;

import javafx.animation.Animation;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * Draws an illustrated Ludo board for the main menu.
 *
 * This is a decorative preview, not the gameplay board.
 * The actual board logic belongs to the game Model
 * and will be integrated with the gameplay View later.
 *
 * MVC Component: View
 * Iteration: 1 - UI enhancement
 */
public class LudoBoardPreview extends StackPane {

    private static final int GRID_SIZE = 15;
    private static final double CELL_SIZE = 18;
    private static final double BOARD_SIZE =
            GRID_SIZE * CELL_SIZE;

    private static final Color RED =
            Color.web("#E85561");
    private static final Color BLUE =
            Color.web("#4A90E2");
    private static final Color GREEN =
            Color.web("#49B77C");
    private static final Color YELLOW =
            Color.web("#F0C541");

    /**
     * Creates the board illustration and starts
     * a subtle floating animation.
     */
    public LudoBoardPreview() {

        Canvas canvas = new Canvas(
                BOARD_SIZE, BOARD_SIZE);

        drawBoard(canvas.getGraphicsContext2D());

        canvas.setAccessibleText(
                "Decorative Ludo board illustration");

        StackPane frame = new StackPane(canvas);
        frame.getStyleClass().add("ludo-preview-board");
        frame.setPadding(new Insets(9));

        getChildren().add(frame);

        // Gentle motion to make the menu feel alive.
        TranslateTransition floating =
                new TranslateTransition(
                        Duration.seconds(3.2), frame);

        floating.setFromY(0);
        floating.setToY(-7);
        floating.setAutoReverse(true);
        floating.setCycleCount(Animation.INDEFINITE);
        floating.play();
    }

    /**
     * Draws the Ludo board's cells, home areas,
     * center and colored pieces.
     *
     * @param gc graphics context for drawing
     */
    private void drawBoard(GraphicsContext gc) {

        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {

                double x = col * CELL_SIZE;
                double y = row * CELL_SIZE;

                gc.setFill(getCellColor(row, col));
                gc.fillRect(x, y, CELL_SIZE, CELL_SIZE);

                gc.setStroke(Color.web("#E9D9E2"));
                gc.setLineWidth(0.7);
                gc.strokeRect(
                        x, y, CELL_SIZE, CELL_SIZE);
            }
        }

        drawHome(gc, 0, 0, RED);
        drawHome(gc, 9, 0, GREEN);
        drawHome(gc, 0, 9, BLUE);
        drawHome(gc, 9, 9, YELLOW);

        drawCenter(gc);

        gc.setStroke(Color.web("#DDBFCE"));
        gc.setLineWidth(2);
        gc.strokeRect(
                1, 1, BOARD_SIZE - 2, BOARD_SIZE - 2);
    }

    /**
     * Returns the background color for a board cell.
     *
     * @param row cell row
     * @param col cell column
     * @return display color
     */
    private Color getCellColor(int row, int col) {

        // Four Ludo home areas.
        if (row < 6 && col < 6) {
            return Color.web("#F8DDE5");
        }

        if (row < 6 && col >= 9) {
            return Color.web("#DFF1E6");
        }

        if (row >= 9 && col < 6) {
            return Color.web("#DDEBFB");
        }

        if (row >= 9 && col >= 9) {
            return Color.web("#FCF2C8");
        }

        // Colored home paths.
        if (row == 7 && col >= 1 && col <= 5) {
            return RED;
        }

        if (row == 7 && col >= 9 && col <= 13) {
            return BLUE;
        }

        if (col == 7 && row >= 1 && row <= 5) {
            return GREEN;
        }

        if (col == 7 && row >= 9 && row <= 13) {
            return YELLOW;
        }

        return Color.WHITE;
    }

    /**
     * Draws a player's home with four pieces.
     *
     * @param gc drawing context
     * @param startCol home area column
     * @param startRow home area row
     * @param color player's color
     */
    private void drawHome(
            GraphicsContext gc,
            int startCol,
            int startRow,
            Color color) {

        double x = startCol * CELL_SIZE
                + CELL_SIZE * 0.9;

        double y = startRow * CELL_SIZE
                + CELL_SIZE * 0.9;

        double innerSize = CELL_SIZE * 4.2;

        gc.setFill(Color.WHITE);
        gc.fillRoundRect(
                x, y, innerSize, innerSize, 12, 12);

        double first = CELL_SIZE * 1.2;
        double second = CELL_SIZE * 3.0;

        drawPiece(gc, x + first, y + first, color);
        drawPiece(gc, x + second, y + first, color);
        drawPiece(gc, x + first, y + second, color);
        drawPiece(gc, x + second, y + second, color);
    }

    /**
     * Draws one circular Ludo piece.
     *
     * @param gc drawing context
     * @param cx center X coordinate
     * @param cy center Y coordinate
     * @param color piece color
     */
    private void drawPiece(
            GraphicsContext gc,
            double cx,
            double cy,
            Color color) {

        double radius = CELL_SIZE * 0.52;

        gc.setFill(Color.WHITE);
        gc.fillOval(
                cx - radius - 2,
                cy - radius - 2,
                (radius + 2) * 2,
                (radius + 2) * 2);

        gc.setFill(color);
        gc.fillOval(
                cx - radius,
                cy - radius,
                radius * 2,
                radius * 2);
    }

    /**
     * Draws the four colored triangles
     * in the board's central finish area.
     *
     * @param gc drawing context
     */
    private void drawCenter(GraphicsContext gc) {

        double left = 6 * CELL_SIZE;
        double top = 6 * CELL_SIZE;
        double right = 9 * CELL_SIZE;
        double bottom = 9 * CELL_SIZE;
        double center = 7.5 * CELL_SIZE;

        gc.setFill(GREEN);
        gc.fillPolygon(
                new double[]{left, right, center},
                new double[]{top, top, center}, 3);

        gc.setFill(RED);
        gc.fillPolygon(
                new double[]{left, left, center},
                new double[]{top, bottom, center}, 3);

        gc.setFill(BLUE);
        gc.fillPolygon(
                new double[]{right, right, center},
                new double[]{top, bottom, center}, 3);

        gc.setFill(YELLOW);
        gc.fillPolygon(
                new double[]{left, right, center},
                new double[]{bottom, bottom, center}, 3);
    }
}
