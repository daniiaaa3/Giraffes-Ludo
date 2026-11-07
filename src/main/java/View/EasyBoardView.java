
package View;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import Model.EasyBoard;
import Model.EasyBoard.BoardCell;
import Model.GameSetup.PlayerColor;
import Model.GameSetup.PlayerEntry;
import Model.GameState;
import Model.Piece;

import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.LinearGradient;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

/**
 * Visual representation of the Easy Ludo board.
 *
 * Board colors and castle positions are aligned
 * with the EasyBoard model.
 *
 * MVC Component: View
 * Iteration: 1
 */
public class EasyBoardView extends StackPane {

    private static final int BOARD_SIZE = 15;
    private static final int CELL_SIZE = 35;
    private static final int BOARD_PIXELS = BOARD_SIZE * CELL_SIZE;

    private final GridPane boardGrid;
    private final EasyBoard boardModel;

    private final StackPane[][] cells =
            new StackPane[BOARD_SIZE][BOARD_SIZE];

    private Consumer<Piece> onPieceSelected;
    private List<Piece> selectablePieces = List.of();

    /** Transparent layer above the cells where a moving piece flies. */
    private final Pane movingLayer = new Pane();

    /** The piece currently being animated (drawn by the animation). */
    private Piece hiddenPiece;
    private boolean animating;
    private GameState lastState;

    /**
     * Builds a polished board background while retaining an
     * independent, interactive cell layer for game pieces.
     */
    public EasyBoardView() {
        boardModel = new EasyBoard();

        boardGrid = new GridPane();
        boardGrid.setAlignment(Pos.CENTER);

        Canvas boardArtwork = new Canvas(BOARD_PIXELS, BOARD_PIXELS);
        boardArtwork.setMouseTransparent(true);
        paintBoard(boardArtwork.getGraphicsContext2D());

        createBoard();

        // The artwork never intercepts clicks. Pieces remain
        // inside their original 15x15 interactive cell grid.
        movingLayer.setMouseTransparent(true);
        movingLayer.setPickOnBounds(false);
        movingLayer.setMinSize(BOARD_PIXELS, BOARD_PIXELS);
        movingLayer.setPrefSize(BOARD_PIXELS, BOARD_PIXELS);
        movingLayer.setMaxSize(BOARD_PIXELS, BOARD_PIXELS);

        getChildren().addAll(boardArtwork, boardGrid, movingLayer);
        setAlignment(Pos.CENTER);
        setMinSize(BOARD_PIXELS, BOARD_PIXELS);
        setPrefSize(BOARD_PIXELS, BOARD_PIXELS);

        DropShadow boardShadow = new DropShadow();
        boardShadow.setRadius(15);
        boardShadow.setOffsetY(5);
        boardShadow.setColor(Color.rgb(105, 54, 85, 0.18));
        setEffect(boardShadow);
    }

    /**
     * Creates transparent cells over the artwork. Arrows, pieces,
     * and movement callbacks continue to use the original
     * model-aligned cell coordinates.
     */
    private void createBoard() {
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                StackPane cell = new StackPane();
                cell.setMinSize(CELL_SIZE, CELL_SIZE);
                cell.setPrefSize(CELL_SIZE, CELL_SIZE);
                cell.setMaxSize(CELL_SIZE, CELL_SIZE);
                cell.setStyle("-fx-background-color: transparent;");

                addArrow(cell, row, col);
                addStartMarker(cell, row, col);
                cells[row][col] = cell;
                boardGrid.add(cell, col, row);
            }
        }
    }

    /**
     * Draws the complete board in a single pass, preventing
     * seams in the center and removing unnecessary grid lines
     * from the four colored castles.
     *
     * @param gc drawing surface behind the interactive pieces
     */
    private void paintBoard(GraphicsContext gc) {
        gc.save();
        clipRoundedBoard(gc);

        gc.setFill(Color.web("#FFFCFE"));
        gc.fillRect(0, 0, BOARD_PIXELS, BOARD_PIXELS);

        drawCastle(gc, PlayerColor.GREEN, 0, 0);
        drawCastle(gc, PlayerColor.YELLOW, 0, 9);
        drawCastle(gc, PlayerColor.RED, 9, 0);
        drawCastle(gc, PlayerColor.BLUE, 9, 9);

        drawCrossTrack(gc);
        drawClassicFinish(gc);

        gc.restore();

        // Fine frame visible on both light and colored corners.
        gc.setStroke(Color.web("#B9819E"));
        gc.setLineWidth(2.0);
        gc.strokeRoundRect(
                1, 1, BOARD_PIXELS - 2, BOARD_PIXELS - 2,
                18, 18);
    }

    /**
     * Clips the board artwork to gently rounded corners.
     *
     * @param gc board drawing surface
     */
    private void clipRoundedBoard(GraphicsContext gc) {
        double size = BOARD_PIXELS;
        double r = 15;

        gc.beginPath();
        gc.moveTo(r, 0);
        gc.lineTo(size - r, 0);
        gc.quadraticCurveTo(size, 0, size, r);
        gc.lineTo(size, size - r);
        gc.quadraticCurveTo(size, size, size - r, size);
        gc.lineTo(r, size);
        gc.quadraticCurveTo(0, size, 0, size - r);
        gc.lineTo(0, r);
        gc.quadraticCurveTo(0, 0, r, 0);
        gc.closePath();
        gc.clip();
    }

    /**
     * Creates one elegant, uncluttered home zone with a white
     * inset panel and four subtle circular piece docks.
     * The dock centers exactly match getCastleCell().
     *
     * @param gc board drawing surface
     * @param color player color
     * @param baseRow top row of this 6x6 home zone
     * @param baseCol left column of this 6x6 home zone
     */
    private void drawCastle(
            GraphicsContext gc,
            PlayerColor color,
            int baseRow,
            int baseCol) {

        double x = baseCol * CELL_SIZE;
        double y = baseRow * CELL_SIZE;
        double zone = 6.0 * CELL_SIZE;
        Color base = Color.web(getPlayerColor(color));

        // A gentle gradient gives the home areas some depth.
        gc.setFill(new LinearGradient(
                0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, base.interpolate(Color.WHITE, 0.80)),
                new Stop(1, base.interpolate(Color.WHITE, 0.88))));
        gc.fillRect(x, y, zone, zone);

        double inset = CELL_SIZE * 0.67;
        double innerX = x + inset;
        double innerY = y + inset;
        double innerSize = zone - 2 * inset;

        gc.setFill(Color.rgb(100, 55, 82, 0.09));
        gc.fillRoundRect(
                innerX, innerY + 3, innerSize, innerSize,
                24, 24);

        gc.setFill(Color.web("#FFFFFF"));
        gc.fillRoundRect(
                innerX, innerY, innerSize, innerSize,
                24, 24);
        gc.setStroke(base.interpolate(Color.WHITE, 0.47));
        gc.setLineWidth(2.0);
        gc.strokeRoundRect(
                innerX + 1, innerY + 1,
                innerSize - 2, innerSize - 2,
                24, 24);

        // These positions match IDs 0, 1, 2, 3 in the Model.
        for (int rowOffset : new int[] {1, 4}) {
            for (int colOffset : new int[] {1, 4}) {
                double cx = (baseCol + colOffset + 0.5) * CELL_SIZE;
                double cy = (baseRow + rowOffset + 0.5) * CELL_SIZE;

                gc.setFill(base.interpolate(Color.WHITE, 0.86));
                gc.fillOval(cx - 16, cy - 16, 32, 32);
                gc.setStroke(base.interpolate(Color.WHITE, 0.30));
                gc.setLineWidth(1.7);
                gc.strokeOval(cx - 16, cy - 16, 32, 32);
            }
        }
    }

    /**
     * Draws the cross-shaped track and colored home paths.
     * All colored path positions come from EasyBoard, so this
     * method cannot change movement rules or track order.
     *
     * @param gc board drawing surface
     */
    private void drawCrossTrack(GraphicsContext gc) {
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                if (!isCrossCell(row, col) || isFinishCell(row, col)) {
                    continue;
                }

                double x = col * CELL_SIZE;
                double y = row * CELL_SIZE;

                gc.setFill(Color.web(getCellColor(row, col)));
                gc.fillRect(x, y, CELL_SIZE, CELL_SIZE);

                gc.setStroke(Color.web("#D5CCD6"));
                gc.setLineWidth(0.85);
                gc.strokeRect(
                        x + 0.4, y + 0.4,
                        CELL_SIZE - 0.8, CELL_SIZE - 0.8);
            }
        }
    }

    /**
     * Adds the # marker to each player's protected starting square
     * (FR-2.5, FR-3.2). The four squares come from the EasyBoard
     * model, the same squares where a released piece lands.
     *
     * @param cell the interactive cell being created
     * @param row board row
     * @param col board column
     */
    private void addStartMarker(StackPane cell, int row, int col) {
        for (PlayerColor color : PlayerColor.values()) {
            BoardCell start = boardModel.getSharedTrackCell(
                    boardModel.getStartSquare(color));

            if (start.row() == row && start.col() == col) {
                Label marker = new Label("#");
                marker.setMouseTransparent(true);
                marker.setFont(Font.font(
                        "Segoe UI", FontWeight.EXTRA_BOLD, 24));
                marker.setTextFill(Color.WHITE);
                marker.setEffect(new DropShadow(
                        2.5, 0, 1, Color.rgb(60, 30, 50, 0.55)));
                marker.getStyleClass().add("start-marker");
                marker.setUserData("start-marker:" + color.name());
                cell.getChildren().add(marker);
            }
        }
    }

    /**
     * Draws the classic seamless finish: yellow above, blue
     * right, red below, green left, all meeting at one point.
     * This covers the entire 3x3 central area.
     *
     * @param gc board drawing surface
     */
    private void drawClassicFinish(GraphicsContext gc) {
        double left = 6.0 * CELL_SIZE;
        double top = 6.0 * CELL_SIZE;
        double side = 3.0 * CELL_SIZE;
        double right = left + side;
        double bottom = top + side;
        double middleX = left + side / 2.0;
        double middleY = top + side / 2.0;

        paintTriangle(gc, PlayerColor.YELLOW,
                new double[] {left, right, middleX},
                new double[] {top, top, middleY});
        paintTriangle(gc, PlayerColor.BLUE,
                new double[] {right, right, middleX},
                new double[] {top, bottom, middleY});
        paintTriangle(gc, PlayerColor.RED,
                new double[] {left, right, middleX},
                new double[] {bottom, bottom, middleY});
        paintTriangle(gc, PlayerColor.GREEN,
                new double[] {left, left, middleX},
                new double[] {top, bottom, middleY});

        gc.setStroke(Color.web("#705467"));
        gc.setLineWidth(1.2);
        gc.strokeLine(left, top, middleX, middleY);
        gc.strokeLine(right, top, middleX, middleY);
        gc.strokeLine(left, bottom, middleX, middleY);
        gc.strokeLine(right, bottom, middleX, middleY);

        gc.setStroke(Color.web("#C7AEBE"));
        gc.setLineWidth(1.4);
        gc.strokeRect(left, top, side, side);
    }

    /**
     * Paints one wedge of the central finish.
     *
     * @param gc board drawing surface
     * @param color wedge player color
     * @param xs three X coordinates
     * @param ys three Y coordinates
     */
    private void paintTriangle(
            GraphicsContext gc,
            PlayerColor color,
            double[] xs,
            double[] ys) {
        gc.setFill(Color.web(getPlayerColor(color)));
        gc.fillPolygon(xs, ys, 3);
    }

    /**
     * Checks whether a cell lies in the middle cross.
     *
     * @param row board row
     * @param col board column
     * @return true for vertical or horizontal track lanes
     */
    private boolean isCrossCell(int row, int col) {
        return (row >= 6 && row <= 8)
                || (col >= 6 && col <= 8);
    }

    /**
     * Checks whether a cell is within the nine-cell finish.
     *
     * @param row board row
     * @param col board column
     * @return true for the 3x3 central finish
     */
    private boolean isFinishCell(int row, int col) {
        return row >= 6 && row <= 8 && col >= 6 && col <= 8;
    }

    /**
     * Adds a direction arrow at each player's home entry.
     *
     * @param cell transparent interactive cell
     * @param row board row
     * @param col board column
     */
    private void addArrow(StackPane cell, int row, int col) {
        for (PlayerColor color : PlayerColor.values()) {
            BoardCell arrow = boardModel.getArrowSquare(color);
            if (arrow.row() == row && arrow.col() == col) {
                Label label = new Label("\u279C");
                label.setMouseTransparent(true);
                label.setStyle(
                        "-fx-font-size: 23px;"
                        + "-fx-font-weight: bold;"
                        + "-fx-text-fill: "
                        + getPlayerColor(color) + ";");
                switch (color) {
                    case RED -> label.setRotate(-90);
                    case GREEN -> label.setRotate(0);
                    case YELLOW -> label.setRotate(90);
                    case BLUE -> label.setRotate(180);
                }
                cell.getChildren().add(label);
            }
        }
    }

    /**
     * Reports whether a piece-movement animation is running.
     *
     * @return true while a piece is hopping across the board
     */
    public boolean isAnimating() {
        return animating;
    }

    /**
     * Animates a piece square by square after the Model has already
     * moved it. The piece hops through every square of its path, so
     * the number of hops always equals the die value (a release from
     * the castle is one hop onto the # start square). The Model is
     * never touched here; the animation only draws.
     *
     * @param gameState the game, already updated by the Model
     * @param piece the piece that just moved
     * @param fromProgress the piece's progress before the move
     *        (-1 when it was in the castle)
     * @param onFinished called once the piece has arrived
     */
    public void animateMove(
            GameState gameState,
            Piece piece,
            int fromProgress,
            Runnable onFinished) {

        List<BoardCell> path = new ArrayList<>();

        if (fromProgress < 0) {
            path.add(getCastleCell(piece.getColor(), piece.getId()));
        }

        for (int p = Math.max(fromProgress, 0);
                p <= piece.getProgress(); p++) {
            path.add(gameState.getBoardCellAt(
                    piece.getColor(), p).orElseThrow());
        }

        if (path.size() < 2 || animating) {
            renderPieces(gameState);
            if (onFinished != null) {
                onFinished.run();
            }
            return;
        }

        animating = true;
        hiddenPiece = piece;
        renderPieces(gameState);

        Circle flyer = createPieceGraphic(piece.getColor(), false);
        flyer.setMouseTransparent(true);
        flyer.setCenterX(centerX(path.get(0)));
        flyer.setCenterY(centerY(path.get(0)));
        flyer.setEffect(new DropShadow(
                9, 0, 4, Color.rgb(70, 35, 55, 0.45)));
        movingLayer.getChildren().add(flyer);

        Timeline hop = new Timeline();
        for (int k = 1; k < path.size(); k++) {
            double t0 = (k - 1) * Motion.STEP_MILLIS;
            double tm = t0 + Motion.STEP_MILLIS * 0.5;
            double t1 = k * Motion.STEP_MILLIS;
            BoardCell cell = path.get(k);

            // lift in the middle of each hop, land on the next square
            hop.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(tm),
                            new KeyValue(flyer.scaleXProperty(), 1.32),
                            new KeyValue(flyer.scaleYProperty(), 1.32)),
                    new KeyFrame(Duration.millis(t1),
                            new KeyValue(flyer.centerXProperty(),
                                    centerX(cell), Interpolator.EASE_BOTH),
                            new KeyValue(flyer.centerYProperty(),
                                    centerY(cell), Interpolator.EASE_BOTH),
                            new KeyValue(flyer.scaleXProperty(), 1.0),
                            new KeyValue(flyer.scaleYProperty(), 1.0)));
        }

        hop.setRate(Motion.speed());
        hop.setOnFinished(event -> {
            movingLayer.getChildren().remove(flyer);
            hiddenPiece = null;
            animating = false;
            renderPieces(lastState);
            if (onFinished != null) {
                onFinished.run();
            }
        });
        hop.play();
    }

    private static double centerX(BoardCell cell) {
        return (cell.col() + 0.5) * CELL_SIZE;
    }

    private static double centerY(BoardCell cell) {
        return (cell.row() + 0.5) * CELL_SIZE;
    }

    /**
     * Registers the piece selection callback.
     */
    public void setOnPieceSelected(Consumer<Piece> handler) {
        this.onPieceSelected = handler;
    }

    /**
     * Defines the pieces that may legally move.
     */
    public void setSelectablePieces(List<Piece> pieces) {

        if (pieces == null) {
            throw new IllegalArgumentException(
                    "Selectable pieces cannot be null.");
        }

        selectablePieces = List.copyOf(pieces);
    }

    /**
     * Renders all pieces from GameState.
     */
    public void renderPieces(GameState gameState) {

        if (gameState == null) {
            throw new IllegalArgumentException(
                    "Game state cannot be null.");
        }

        lastState = gameState;

        // Remove previous pieces, preserving arrows.
        for (StackPane[] row : cells) {
            for (StackPane cell : row) {

                cell.getChildren().removeIf(
                        node -> "pieces".equals(node.getUserData())
                );
            }
        }

        Map<BoardCell, List<Piece>> positions =
                new HashMap<>();

        for (PlayerEntry player : gameState.getPlayers()) {

            for (Piece piece : gameState.getPieces(player)) {

                // The animation draws this piece while it moves.
                if (piece == hiddenPiece) {
                    continue;
                }

                BoardCell position;

                if (piece.isInCastle()) {

                    position = getCastleCell(
                            piece.getColor(),
                            piece.getId()
                    );

                } else {

                    // The model determines the real
                    // location of a piece on the board.
                    position = gameState.getBoardCell(piece)
                            .orElse(null);

                    // Finished pieces may not have
                    // a regular board coordinate.
                    if (position == null) {
                        continue;
                    }
                }

                positions.computeIfAbsent(
                        position,
                        key -> new ArrayList<>()
                ).add(piece);
            }
        }

        // Draw pieces at their current positions.
        for (Map.Entry<BoardCell, List<Piece>> entry
                : positions.entrySet()) {

            BoardCell position = entry.getKey();

            GridPane pieceGroup = new GridPane();
            pieceGroup.setAlignment(Pos.CENTER);
            pieceGroup.setHgap(1);
            pieceGroup.setVgap(1);
            pieceGroup.setUserData("pieces");

            List<Piece> piecesHere = entry.getValue();

            for (int i = 0; i < piecesHere.size(); i++) {

                Piece piece = piecesHere.get(i);

                Circle graphic = createPieceGraphic(
                        piece.getColor(),
                        piecesHere.size() > 1
                );

                // Allow selection only for legal pieces.
                if (selectablePieces.contains(piece)) {

                    graphic.setStroke(Color.GOLD);
                    graphic.setStrokeWidth(3);
                    graphic.setCursor(Cursor.HAND);

                    graphic.setOnMouseEntered(event ->
                            animateSelectablePiece(graphic, 1.14));
                    graphic.setOnMouseExited(event ->
                            animateSelectablePiece(graphic, 1.0));

                    graphic.setOnMouseClicked(event -> {

                        if (onPieceSelected != null) {
                            onPieceSelected.accept(piece);
                        }

                        event.consume();
                    });
                }

                pieceGroup.add(
                        graphic,
                        i % 2,
                        i / 2
                );
            }

            cells[position.row()][position.col()]
                    .getChildren().add(pieceGroup);
        }
    }

    /**
     * Gently enlarges selectable pieces on hover without changing
     * the hit target, board position, or game rules.
     *
     * @param piece interactive piece graphic
     * @param size target scale multiplier
     */
    private void animateSelectablePiece(Circle piece, double size) {
        ScaleTransition animation = new ScaleTransition(
                Duration.millis(125), piece);
        animation.setToX(size);
        animation.setToY(size);
        animation.play();
    }

    /**
     * Returns the castle cell for piece IDs 0-3.
     *
     * Castle positions match EasyBoard:
     *
     * GREEN  | YELLOW
     * -------+-------
     * RED    | BLUE
     */
    private BoardCell getCastleCell(
            PlayerColor color, int pieceId) {

        int baseRow = switch (color) {
            case GREEN, YELLOW -> 0;
            case RED, BLUE -> 9;
        };

        int baseCol = switch (color) {
            case GREEN, RED -> 0;
            case YELLOW, BLUE -> 9;
        };

        int rowOffset = pieceId / 2 == 0 ? 1 : 4;
        int colOffset = pieceId % 2 == 0 ? 1 : 4;

        return new BoardCell(
                baseRow + rowOffset,
                baseCol + colOffset
        );
    }


    /**
     * Creates a 3D-style Ludo piece with
     * a soft highlight and subtle shadow.
     */
    private Circle createPieceGraphic(
            PlayerColor color, boolean crowded) {

        double radius = crowded ? 6 : 11;

        Circle piece = new Circle(radius);

        Color baseColor =
                Color.web(getPlayerColor(color));

        // Create a soft 3D color gradient.
        RadialGradient gradient = new RadialGradient(
                0,
                0,
                0.30,
                0.25,
                0.85,
                true,
                CycleMethod.NO_CYCLE,
                new Stop(0.0,
                        baseColor.interpolate(Color.WHITE, 0.45)),
                new Stop(0.45, baseColor),
                new Stop(1.0, baseColor.darker())
        );

        piece.setFill(gradient);

        // White border keeps pieces visible
        // on matching colored squares.
        piece.setStroke(Color.WHITE);
        piece.setStrokeWidth(2);

        // Subtle shadow adds depth.
        DropShadow shadow = new DropShadow();
        shadow.setRadius(5);
        shadow.setOffsetY(2);
        shadow.setColor(
                Color.rgb(70, 35, 55, 0.30)
        );

        piece.setEffect(shadow);

        return piece;
    }


    /**
     * Determines the background color of each cell.
     *
     * Castle colors match the paths defined
     * by the EasyBoard model.
     */
    private String getCellColor(int row, int col) {

        // GREEN castle: top-left.
        if (row < 6 && col < 6) {
            return "#DFF1E6";
        }

        // YELLOW castle: top-right.
        if (row < 6 && col >= 9) {
            return "#FCF2C8";
        }

        // RED castle: bottom-left.
        if (row >= 9 && col < 6) {
            return "#F8DDE5";
        }

        // BLUE castle: bottom-right.
        if (row >= 9 && col >= 9) {
            return "#DDEBFB";
        }

        // Colored home paths.
        for (PlayerColor color : PlayerColor.values()) {

            for (BoardCell cell :
                    boardModel.getHomePathCells(color)) {

                if (cell.row() == row && cell.col() == col) {
                    return getPlayerColor(color);
                }
            }
        }

        // Colored starting squares.
        for (PlayerColor color : PlayerColor.values()) {

            BoardCell start = boardModel.getSharedTrackCell(
                    boardModel.getStartSquare(color)
            );

            if (start.row() == row && start.col() == col) {
                return getPlayerColor(color);
            }
        }

        // Shared track.
        if (isTrackCell(row, col)) {
            return "#F2EDF5";
        }

        return "#FFFFFF";
    }

    /**
     * Checks whether the cell is part of
     * the 52-square shared track.
     */
    private boolean isTrackCell(int row, int col) {

        for (BoardCell cell :
                boardModel.getSharedTrackCells()) {

            if (cell.row() == row && cell.col() == col) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns the visual color of a player.
     */
    private String getPlayerColor(PlayerColor color) {

        return switch (color) {
            case RED -> "#E85561";
            case BLUE -> "#4A90E2";
            case GREEN -> "#49B77C";
            case YELLOW -> "#F0C541";
        };
    }
}
