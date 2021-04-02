package de.julianhofmann.ui;

import de.julianhofmann.App;
import de.julianhofmann.world.Coordinates;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.*;

public class Renderer {
    private int maxDrawCalls = 10000;
    private final ContentPane contentPane;
    private final Canvas canvas;
    private final LinkedList<Coordinates> drawCells;

    public Renderer(ContentPane contentPane) {
        this.contentPane = contentPane;
        canvas = new Canvas(contentPane.getBackBuffer().getWidth(), contentPane.getBackBuffer().getHeight());
        drawCells = new LinkedList<>();
    }

    public void collectCells() {
        final int width = (int) Math.ceil(canvas.getWidth());
        final int height = (int) Math.ceil(canvas.getHeight());
        App.world.getCellsReadLock().lock();
        HashMap<Coordinates, Byte> cells = App.world.getCells();
        try {
            for (Map.Entry<Coordinates, Byte> entry : cells.entrySet()) {
                Coordinates coordinates = entry.getKey();
                Byte value = entry.getValue();
                if (value == 1) {
                    Coordinates windowCoords = coordinates.toCanvasCoordinates(App.world);
                    if (windowCoords.getX() >= -App.world.getCellSize() && windowCoords.getX() <= width + App.world.getCellSize() && windowCoords.getY() >= -App.world.getCellSize() && windowCoords.getY() <= height + App.world.getCellSize()) {
                        drawCells.push(windowCoords);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            App.world.getCellsReadLock().unlock();
        }
    }

    public boolean render(boolean firstRound) {
        canvas.setWidth(contentPane.getBackBuffer().getWidth());
        canvas.setHeight(contentPane.getBackBuffer().getHeight());

        GraphicsContext gc = canvas.getGraphicsContext2D();
        final int width = (int) Math.ceil(canvas.getWidth());
        final int height = (int) Math.ceil(canvas.getHeight());

        if (firstRound) {
            gc.setFill(contentPane.getBackground());
            gc.fillRect(0, 0, width, height);
        }

        gc.setFill(contentPane.getForeground());

        int frameDrawCalls = 0;

        // Cells
        while (frameDrawCalls < maxDrawCalls && !drawCells.isEmpty()) {
            Coordinates coordinates = drawCells.pop();
            gc.fillRect(coordinates.getX(), coordinates.getY(), App.world.getCellSize(), App.world.getCellSize());
            frameDrawCalls++;
        }

        boolean finished = drawCells.isEmpty();

        if (finished) {
            // Highlight
            if (contentPane.isEditing() && contentPane.isCursorInArea() && !contentPane.getSelectionManager().isSelecting(true)) {
                Coordinates coords = new Coordinates(contentPane.getMouseX(), contentPane.getMouseY()).toWorldCoordinates(App.world, false).toCanvasCoordinates(App.world);
                gc.setFill(contentPane.getHighlight());
                gc.fillRect(coords.getX(), coords.getY(), App.world.getCellSize(), App.world.getCellSize());
            }

            // Grid
            Color gridColor = contentPane.getGrid();
            float gridOpacity = contentPane.getGridOpacity();
            if (gridOpacity > 1) gridOpacity = 1;
            else if (gridOpacity < 0) gridOpacity = 0;
            gc.setStroke(new Color(gridColor.getRed(), gridColor.getGreen(), gridColor.getBlue(), gridOpacity));

            // Horizontal
            for (float i = (Math.round(App.world.getCameraY()) % App.world.getCellSize()) - App.world.getCellSize(); i < height + App.world.getCellSize(); i += App.world.getCellSize()) {
                gc.strokeLine(-App.world.getCellSize(), i, width + App.world.getCellSize(), i);
            }

            // Vertical
            for (float i = (Math.round(App.world.getCameraX()) % App.world.getCellSize()) - App.world.getCellSize(); i < width + App.world.getCellSize(); i += App.world.getCellSize()) {
                gc.strokeLine(i, -App.world.getCellSize(), i, height + App.world.getCellSize());
            }

            // Selection
            contentPane.getSelectionManager().draw(gc);
        }

        return finished;
    }

    public void snapshot() {
        canvas.snapshot(null, contentPane.getBackBuffer());
    }

    public LinkedList<Coordinates> getDrawCells() {
        return drawCells;
    }

    public int getMaxDrawCalls() {
        return maxDrawCalls;
    }

    public void setMaxDrawCalls(int maxDrawCalls) {
        this.maxDrawCalls = maxDrawCalls;
    }
}
