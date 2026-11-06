
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
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Polygon;
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

    private final GridPane boardGrid;
    private final EasyBoard boardModel;

    private final StackPane[][] cells =
            new StackPane[BOARD_SIZE][BOARD_SIZE];

    private Consumer<Piece> onPieceSelected;
    private List<Piece> selectablePieces = List.of();

    public EasyBoardView() {

        boardModel = new EasyBoard();

        boardGrid = new GridPane();
        boardGrid.setAlignment(Pos.CENTER);

        createBoard();

        getChildren().add(boardGrid);
        setAlignment(Pos.CENTER);
    }

    /**
     * Creates the 15x15 board.
     */
    private void createBoard() {

        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {

                StackPane cell = new StackPane();

                cell.setMinSize(CELL_SIZE, CELL_SIZE);
                cell.setPrefSize(CELL_SIZE, CELL_SIZE);
                cell.setMaxSize(CELL_SIZE, CELL_SIZE);

                cell.setStyle(
                        "-fx-background-color: "
                        + getCellColor(row, col) + ";"
                        + "-fx-border-color: #D6C8D0;"
                        + "-fx-border-width: 0.5;"
                );

                addArrow(cell, row, col);
                
                // Draw the four-color finish center.
                if (row == 7 && col == 7) {
                    addCenterDesign(cell);
                }

                cells[row][col] = cell;
                boardGrid.add(cell, col, row);
            }
        }
    }

    /**
     * Adds each player's home-entry arrow.
     */
    private void addArrow(
            StackPane cell, int row, int col) {

        for (PlayerColor color : PlayerColor.values()) {

            BoardCell arrow = boardModel.getArrowSquare(color);

            if (arrow.row() == row && arrow.col() == col) {

                Label label = new Label("\u279C");

                label.setStyle(
                        "-fx-font-size: 23px;"
                        + "-fx-font-weight: bold;"
                        + "-fx-text-fill: "
                        + getPlayerColor(color) + ";"
                );

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
     * Draws a decorative four-color center.
     * This does not change board logic or piece movement.
     */
    private void addCenterDesign(StackPane cell) {

        double size = CELL_SIZE;
        double middle = size / 2.0;

        Pane center = new Pane();
        center.setMinSize(size, size);
        center.setPrefSize(size, size);
        center.setMaxSize(size, size);

        // Top triangle - Yellow.
        Polygon top = new Polygon(
                0.0, 0.0,
                size, 0.0,
                middle, middle
        );
        top.setFill(Color.web("#F0C541"));

        // Right triangle - Blue.
        Polygon right = new Polygon(
                size, 0.0,
                size, size,
                middle, middle
        );
        right.setFill(Color.web("#4A90E2"));

        // Bottom triangle - Red.
        Polygon bottom = new Polygon(
                0.0, size,
                size, size,
                middle, middle
        );
        bottom.setFill(Color.web("#E85561"));

        // Left triangle - Green.
        Polygon left = new Polygon(
                0.0, 0.0,
                0.0, size,
                middle, middle
        );
        left.setFill(Color.web("#49B77C"));

        center.getChildren().addAll(
                top, right, bottom, left
        );

        center.setMouseTransparent(true);
        cell.getChildren().add(center);
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
