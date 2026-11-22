package View;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import Model.EasyBoard.BoardCell;
import Model.GameBoardTopology;
import Model.GameDifficulty;
import Model.GameSetup.PlayerColor;
import Model.GameSetup.PlayerEntry;
import Model.GameState;
import Model.Piece;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

/**
 * Medium/Hard board: the polished Easy visual style around the additional
 * tracks defined by GameBoardTopology. This class never changes game rules.
 *
 * All interactive pieces remain on a transparent grid above the artwork.
 * Iteration: 2 | MVC: View
 */
public final class AdvancedBoardView extends StackPane {

    public static final double BOARD_PIXELS = 525;
    private static final String PIECES_TAG = "advanced-pieces";

    private final GameBoardTopology topology;
    private final int size;
    private final int inset;
    private final double cellSize;
    private final StackPane[][] cells;
    private final Pane movingLayer = new Pane();

    private final Set<BoardCell> sharedTrack = new HashSet<>();
    private final Map<BoardCell, PlayerColor> starts = new HashMap<>();
    private final Map<BoardCell, PlayerColor> home = new HashMap<>();
    private final Map<BoardCell, PlayerColor> arrows = new HashMap<>();

    private Consumer<Piece> onPieceSelected;
    private List<Piece> selectablePieces = List.of();
    private Piece movingPiece;
    private boolean animating;
    private GameState lastState;

    public AdvancedBoardView(GameState state) {
        if (state == null || state.getDifficulty() == GameDifficulty.EASY) {
            throw new IllegalArgumentException(
                    "AdvancedBoardView requires Medium or Hard GameState");
        }

        topology = state.getBoardTopology();
        size = topology.getBoardSize();
        inset = (size - 15) / 2;
        cellSize = BOARD_PIXELS / size;
        cells = new StackPane[size][size];
        indexBoardCells();

        Canvas artwork = new Canvas(BOARD_PIXELS, BOARD_PIXELS);
        artwork.setMouseTransparent(true);
        paintBoard(artwork.getGraphicsContext2D());

        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setSnapToPixel(false);
        grid.setMinSize(BOARD_PIXELS, BOARD_PIXELS);
        grid.setPrefSize(BOARD_PIXELS, BOARD_PIXELS);
        grid.setMaxSize(BOARD_PIXELS, BOARD_PIXELS);

        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                StackPane cell = createInteractiveCell(new BoardCell(row, col));
                cells[row][col] = cell;
                grid.add(cell, col, row);
            }
        }

        movingLayer.setMouseTransparent(true);
        movingLayer.setPickOnBounds(false);
        movingLayer.setMinSize(BOARD_PIXELS, BOARD_PIXELS);
        movingLayer.setPrefSize(BOARD_PIXELS, BOARD_PIXELS);
        movingLayer.setMaxSize(BOARD_PIXELS, BOARD_PIXELS);

        getChildren().addAll(artwork, grid, movingLayer);
        setAlignment(Pos.CENTER);
        setMinSize(BOARD_PIXELS, BOARD_PIXELS);
        setPrefSize(BOARD_PIXELS, BOARD_PIXELS);
        setMaxSize(BOARD_PIXELS, BOARD_PIXELS);

        DropShadow boardShadow = new DropShadow();
        boardShadow.setRadius(15);
        boardShadow.setOffsetY(5);
        boardShadow.setColor(Color.rgb(105, 54, 85, 0.18));
        setEffect(boardShadow);
        renderPieces(state);
    }

    /** Uses exactly the same track and special-square locations as GameState. */
    private void indexBoardCells() {
        for (PlayerColor color : PlayerColor.values()) {
            List<BoardCell> path = topology.getPlayerPath(color);
            int entry = topology.getEntrySteps(color);
            starts.put(path.get(0), color);

            for (int i = 0; i <= entry; i++) {
                sharedTrack.add(path.get(i));
            }
            for (int i = entry + 1; i < path.size(); i++) {
                home.put(path.get(i), color);
            }

            if (topology.getDifficulty() == GameDifficulty.MEDIUM) {
                arrows.put(path.get(57), color);
                arrows.put(path.get(109), color);
            } else {
                arrows.put(path.get(65), color);
                arrows.put(path.get(129), color);
                arrows.put(path.get(181), color);
            }
        }
    }

    // ---------------------------------------------------------- Artwork

    private void paintBoard(GraphicsContext gc) {
        gc.save();
        clipRoundedBoard(gc);
        gc.setFill(Color.web("#FFFCFE"));
        gc.fillRect(0, 0, BOARD_PIXELS, BOARD_PIXELS);

        // Draw the fixed geometry and special squares underneath interaction.
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                BoardCell cell = new BoardCell(row, col);
                if (!sharedTrack.contains(cell) && !home.containsKey(cell)) {
                    continue;
                }
                double x = col * cellSize;
                double y = row * cellSize;
                gc.setFill(Color.web(backgroundFor(cell)));
                gc.fillRect(x, y, cellSize, cellSize);
                gc.setStroke(Color.web("#D5CCD6"));
                gc.setLineWidth(0.85);
                gc.strokeRect(x + 0.4, y + 0.4,
                        cellSize - 0.8, cellSize - 0.8);
            }
        }

        // These panels deliberately hide the old grid inside each castle.
        drawCastle(gc, PlayerColor.GREEN, inset, inset);
        drawCastle(gc, PlayerColor.YELLOW, inset, inset + 9);
        drawCastle(gc, PlayerColor.RED, inset + 9, inset);
        drawCastle(gc, PlayerColor.BLUE, inset + 9, inset + 9);
        drawClassicFinish(gc);
        gc.restore();

        gc.setStroke(Color.web("#B9819E"));
        gc.setLineWidth(2);
        gc.strokeRoundRect(1, 1, BOARD_PIXELS - 2,
                BOARD_PIXELS - 2, 18, 18);
    }

    private String backgroundFor(BoardCell cell) {
        PlayerColor startColor = starts.get(cell);
        if (startColor != null) return colorHex(startColor);

        PlayerColor homeColor = home.get(cell);
        if (homeColor != null) return colorHex(homeColor);

        GameDifficulty question = topology.getQuestionSquares().get(cell);
        if (question != null) {
            return switch (question) {
                case EASY -> "#D8F3DF";
                case MEDIUM -> "#FFE9A6";
                case HARD -> "#F8C6CB";
            };
        }
        if (topology.getSurpriseSquares().contains(cell)) return "#F3D5F1";
        if (topology.getSafeSquares().contains(cell)) return "#C6EBD8";
        return "#F2EDF5";
    }

    private void clipRoundedBoard(GraphicsContext gc) {
        double s = BOARD_PIXELS;
        double r = 15;
        gc.beginPath();
        gc.moveTo(r, 0);
        gc.lineTo(s - r, 0);
        gc.quadraticCurveTo(s, 0, s, r);
        gc.lineTo(s, s - r);
        gc.quadraticCurveTo(s, s, s - r, s);
        gc.lineTo(r, s);
        gc.quadraticCurveTo(0, s, 0, s - r);
        gc.lineTo(0, r);
        gc.quadraticCurveTo(0, 0, r, 0);
        gc.closePath();
        gc.clip();
    }

    /** Reproduces EasyBoardView's white inset panels and four piece docks. */
    private void drawCastle(GraphicsContext gc, PlayerColor color,
            int baseRow, int baseCol) {
        double x = baseCol * cellSize;
        double y = baseRow * cellSize;
        double zone = 6.0 * cellSize;
        Color base = Color.web(colorHex(color));

        gc.setFill(new LinearGradient(0, 0, 1, 1, true,
                CycleMethod.NO_CYCLE,
                new Stop(0, base.interpolate(Color.WHITE, 0.80)),
                new Stop(1, base.interpolate(Color.WHITE, 0.88))));
        gc.fillRect(x, y, zone, zone);

        double margin = cellSize * 0.67;
        double innerX = x + margin;
        double innerY = y + margin;
        double inner = zone - 2 * margin;
        double radius = Math.min(24, cellSize * 0.68);

        gc.setFill(Color.rgb(100, 55, 82, 0.09));
        gc.fillRoundRect(innerX, innerY + 3, inner, inner, radius, radius);
        gc.setFill(Color.WHITE);
        gc.fillRoundRect(innerX, innerY, inner, inner, radius, radius);
        gc.setStroke(base.interpolate(Color.WHITE, 0.47));
        gc.setLineWidth(2);
        gc.strokeRoundRect(innerX + 1, innerY + 1,
                inner - 2, inner - 2, radius, radius);

        for (int rowOffset : new int[] {1, 4}) {
            for (int colOffset : new int[] {1, 4}) {
                double cx = (baseCol + colOffset + 0.5) * cellSize;
                double cy = (baseRow + rowOffset + 0.5) * cellSize;
                double dockRadius = cellSize * 0.45;
                gc.setFill(base.interpolate(Color.WHITE, 0.86));
                gc.fillOval(cx - dockRadius, cy - dockRadius,
                        dockRadius * 2, dockRadius * 2);
                gc.setStroke(base.interpolate(Color.WHITE, 0.30));
                gc.setLineWidth(1.7);
                gc.strokeOval(cx - dockRadius, cy - dockRadius,
                        dockRadius * 2, dockRadius * 2);
            }
        }
    }

    /** Draws the same four triangular finish wedges as EasyBoardView. */
    private void drawClassicFinish(GraphicsContext gc) {
        double left = (inset + 6) * cellSize;
        double top = (inset + 6) * cellSize;
        double side = 3 * cellSize;
        double right = left + side;
        double bottom = top + side;
        double centerX = left + side / 2;
        double centerY = top + side / 2;

        triangle(gc, PlayerColor.YELLOW,
                new double[] {left, right, centerX},
                new double[] {top, top, centerY});
        triangle(gc, PlayerColor.BLUE,
                new double[] {right, right, centerX},
                new double[] {top, bottom, centerY});
        triangle(gc, PlayerColor.RED,
                new double[] {left, right, centerX},
                new double[] {bottom, bottom, centerY});
        triangle(gc, PlayerColor.GREEN,
                new double[] {left, left, centerX},
                new double[] {top, bottom, centerY});

        gc.setStroke(Color.web("#705467"));
        gc.setLineWidth(1.2);
        gc.strokeLine(left, top, centerX, centerY);
        gc.strokeLine(right, top, centerX, centerY);
        gc.strokeLine(left, bottom, centerX, centerY);
        gc.strokeLine(right, bottom, centerX, centerY);
        gc.setStroke(Color.web("#C7AEBE"));
        gc.setLineWidth(1.4);
        gc.strokeRect(left, top, side, side);
    }

    private static void triangle(GraphicsContext gc, PlayerColor color,
            double[] xs, double[] ys) {
        gc.setFill(Color.web(colorHex(color)));
        gc.fillPolygon(xs, ys, 3);
    }

    // ---------------------------------------------------- Interactive grid

    private StackPane createInteractiveCell(BoardCell position) {
        StackPane cell = new StackPane();
        cell.setSnapToPixel(false);
        cell.setMinSize(cellSize, cellSize);
        cell.setPrefSize(cellSize, cellSize);
        cell.setMaxSize(cellSize, cellSize);
        cell.setStyle("-fx-background-color: transparent;");

        Label marker = markerFor(position);
        if (marker != null) cell.getChildren().add(marker);
        return cell;
    }

    private Label markerFor(BoardCell position) {
        String symbol;
        Color textColor = Color.web("#554457");
        PlayerColor arrowColor = arrows.get(position);

        if (starts.containsKey(position)) {
            symbol = "#";
            textColor = Color.WHITE;
        } else if (topology.getQuestionSquares().containsKey(position)) {
            symbol = "?";
            textColor = switch (topology.getQuestionSquares().get(position)) {
                case EASY -> Color.web("#257E50");
                case MEDIUM -> Color.web("#876010");
                case HARD -> Color.web("#A23F4B");
            };
        } else if (topology.getSurpriseSquares().contains(position)) {
            symbol = "★";
            textColor = Color.web("#9D498A");
        } else if (topology.getSafeSquares().contains(position)) {
            symbol = "S";
            textColor = Color.web("#267651");
        } else if (arrowColor != null) {
            symbol = "➜";
            textColor = Color.web(colorHex(arrowColor));
        } else {
            return null;
        }

        Label marker = new Label(symbol);
        marker.setMouseTransparent(true);
        marker.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD,
                Math.max(12, cellSize * 0.55)));
        marker.setTextFill(textColor);
        if (starts.containsKey(position)) {
            marker.setEffect(new DropShadow(2.5, 0, 1,
                    Color.rgb(60, 30, 50, 0.55)));
        }
        if (arrowColor != null && "➜".equals(symbol)) {
            marker.setRotate(switch (arrowColor) {
                case RED -> -90;
                case GREEN -> 0;
                case YELLOW -> 90;
                case BLUE -> 180;
            });
        }
        return marker;
    }

    private static String colorHex(PlayerColor color) {
        return switch (color) {
            case RED -> "#E85561";
            case BLUE -> "#4A90E2";
            case GREEN -> "#49B77C";
            case YELLOW -> "#F0C541";
        };
    }

    private BoardCell castleCell(Piece piece) {
        int baseRow = switch (piece.getColor()) {
            case GREEN, YELLOW -> inset;
            case RED, BLUE -> inset + 9;
        };
        int baseCol = switch (piece.getColor()) {
            case GREEN, RED -> inset;
            case YELLOW, BLUE -> inset + 9;
        };
        int rowOffset = piece.getId() / 2 == 0 ? 1 : 4;
        int colOffset = piece.getId() % 2 == 0 ? 1 : 4;
        return new BoardCell(baseRow + rowOffset, baseCol + colOffset);
    }

    // -------------------------------------------------------- Piece graphics

    private Circle pieceGraphic(Piece piece, boolean crowded) {
        double radius = cellSize * (crowded ? 0.18 : 0.31);
        Circle circle = new Circle(radius);
        Color base = Color.web(colorHex(piece.getColor()));
        circle.setFill(new RadialGradient(
                0, 0, 0.30, 0.25, 0.85, true,
                CycleMethod.NO_CYCLE,
                new Stop(0, base.interpolate(Color.WHITE, 0.45)),
                new Stop(0.45, base),
                new Stop(1, base.darker())));
        circle.setStroke(Color.WHITE);
        circle.setStrokeWidth(Math.max(1.2, cellSize * 0.05));

        DropShadow shadow = new DropShadow();
        shadow.setRadius(5);
        shadow.setOffsetY(2);
        shadow.setColor(Color.rgb(70, 35, 55, 0.30));
        circle.setEffect(shadow);
        return circle;
    }

    private void animateSelectablePiece(Circle piece, double scale) {
        ScaleTransition transition = new ScaleTransition(
                Duration.millis(125), piece);
        transition.setToX(scale);
        transition.setToY(scale);
        transition.play();
    }

    public void setOnPieceSelected(Consumer<Piece> handler) {
        onPieceSelected = handler;
    }

    public void setSelectablePieces(List<Piece> pieces) {
        if (pieces == null) {
            throw new IllegalArgumentException("Selectable pieces cannot be null");
        }
        selectablePieces = List.copyOf(pieces);
    }

    public boolean isAnimating() {
        return animating;
    }

    public void renderPieces(GameState state) {
        if (state == null) {
            throw new IllegalArgumentException("GameState cannot be null");
        }
        lastState = state;
        for (StackPane[] row : cells) {
            for (StackPane cell : row) {
                cell.getChildren().removeIf(
                        node -> PIECES_TAG.equals(node.getUserData()));
            }
        }

        Map<BoardCell, List<Piece>> locations = new HashMap<>();
        for (PlayerEntry player : state.getPlayers()) {
            for (Piece piece : state.getPieces(player)) {
                if (piece == movingPiece) continue;
                BoardCell location = piece.isInCastle()
                        ? castleCell(piece)
                        : state.getBoardCell(piece).orElse(null);
                if (location != null) {
                    locations.computeIfAbsent(location,
                            k -> new ArrayList<>()).add(piece);
                }
            }
        }

        for (Map.Entry<BoardCell, List<Piece>> entry : locations.entrySet()) {
            BoardCell pos = entry.getKey();
            if (pos.row() < 0 || pos.row() >= size
                    || pos.col() < 0 || pos.col() >= size) {
                throw new IllegalStateException(
                        "Piece outside " + size + "x" + size + " board: " + pos);
            }
            List<Piece> group = entry.getValue();
            GridPane pieceGroup = new GridPane();
            pieceGroup.setAlignment(Pos.CENTER);
            pieceGroup.setHgap(1);
            pieceGroup.setVgap(1);
            pieceGroup.setUserData(PIECES_TAG);

            for (int i = 0; i < group.size(); i++) {
                Piece piece = group.get(i);
                Circle graphic = pieceGraphic(piece, group.size() > 1);
                if (selectablePieces.contains(piece)) {
                    graphic.setStroke(Color.GOLD);
                    graphic.setStrokeWidth(3);
                    graphic.setCursor(Cursor.HAND);
                    graphic.setOnMouseEntered(event ->
                            animateSelectablePiece(graphic, 1.14));
                    graphic.setOnMouseExited(event ->
                            animateSelectablePiece(graphic, 1.0));
                    graphic.setOnMouseClicked(event -> {
                        if (!animating && onPieceSelected != null) {
                            onPieceSelected.accept(piece);
                        }
                        event.consume();
                    });
                }
                pieceGroup.add(graphic, i % 2, i / 2);
            }
            cells[pos.row()][pos.col()].getChildren().add(pieceGroup);
        }
    }

    /** Same hop animation style and speed setting used by EasyBoardView. */
    public void animateMove(GameState state, Piece piece,
            int fromProgress, Runnable onFinished) {
        if (animating) return;

        List<BoardCell> path = new ArrayList<>();
        if (fromProgress < 0) {
            path.add(castleCell(piece));
        }
        for (int progress = Math.max(0, fromProgress);
                progress <= piece.getProgress(); progress++) {
            path.add(state.getBoardCellAt(
                    piece.getColor(), progress).orElseThrow());
        }
        if (path.size() < 2) {
            renderPieces(state);
            if (onFinished != null) onFinished.run();
            return;
        }

        animating = true;
        movingPiece = piece;
        renderPieces(state);

        Circle flyer = pieceGraphic(piece, false);
        flyer.setMouseTransparent(true);
        flyer.setCenterX(centerX(path.get(0)));
        flyer.setCenterY(centerY(path.get(0)));
        movingLayer.getChildren().add(flyer);

        Timeline timeline = new Timeline();
        for (int i = 1; i < path.size(); i++) {
            double halfway = (i - 0.5) * Motion.STEP_MILLIS;
            double finished = i * Motion.STEP_MILLIS;
            BoardCell target = path.get(i);
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(halfway),
                            new KeyValue(flyer.scaleXProperty(), 1.32),
                            new KeyValue(flyer.scaleYProperty(), 1.32)),
                    new KeyFrame(Duration.millis(finished),
                            new KeyValue(flyer.centerXProperty(),
                                    centerX(target), Interpolator.EASE_BOTH),
                            new KeyValue(flyer.centerYProperty(),
                                    centerY(target), Interpolator.EASE_BOTH),
                            new KeyValue(flyer.scaleXProperty(), 1.0),
                            new KeyValue(flyer.scaleYProperty(), 1.0)));
        }
        timeline.setRate(Motion.speed());
        timeline.setOnFinished(event -> {
            movingLayer.getChildren().remove(flyer);
            movingPiece = null;
            animating = false;
            renderPieces(lastState);
            if (onFinished != null) onFinished.run();
        });
        timeline.play();
    }

    private double centerX(BoardCell cell) {
        return (cell.col() + 0.5) * cellSize;
    }

    private double centerY(BoardCell cell) {
        return (cell.row() + 0.5) * cellSize;
    }
}
