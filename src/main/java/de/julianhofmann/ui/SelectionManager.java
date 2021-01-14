package de.julianhofmann.ui;

import de.julianhofmann.App;
import de.julianhofmann.world.Coordinates;
import de.julianhofmann.world.Pattern;
import de.julianhofmann.world.World;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.canvas.GraphicsContext;
import org.json.simple.JSONObject;

import java.util.HashMap;

public class SelectionManager {
    private final BooleanProperty dragging = new SimpleBooleanProperty(false);
    private Coordinates originalPos;
    private Coordinates start, end;
    private HashMap<Coordinates, Byte> selectedCells;
    private HashMap<Coordinates, Byte> clipboard;
    private Coordinates clipboardSize;
    private boolean deleted, pasted;
    private final BooleanProperty selecting = new SimpleBooleanProperty(false);
    private final BooleanProperty clipboardEmpty = new SimpleBooleanProperty(true);

    public void draw(GraphicsContext gc) {

        if (isSelecting(true)) {

            Coordinates start = getDrawStart();

            if (isSelecting(false)) {
                selectedCells.forEach((key, value) -> {
                    if (isDragging()) {
                        gc.setFill(value == (byte) 1 ? App.ui.getContentPane().getForegroundTransparent() : App.ui.getContentPane().getBackgroundTransparent());
                    } else {
                        gc.setFill(value == (byte) 1 ? App.ui.getContentPane().getForeground() : App.ui.getContentPane().getBackground());
                    }
                    gc.fillRect(start.getX() + key.getX() * App.world.getCellSize(), start.getY() + key.getY() * App.world.getCellSize(), App.world.getCellSize(), App.world.getCellSize());
                });
            }

            gc.setStroke(App.ui.getContentPane().getSelection());
            gc.strokeRect(start.getX(), start.getY(), getSize().getX(), getSize().getY());
        }
    }

    public void snap() {
        if (start != null)
            start = start.toWorldCoordinates(App.world, true).toCanvasCoordinates(App.world);
        if (end != null)
            end = end.toWorldCoordinates(App.world, true).toCanvasCoordinates(App.world);
    }

    public void select() {
        App.world.getUndoManager().newUndoStage();
        Coordinates start = new Coordinates(Math.min(this.start.getX(), this.end.getX()), Math.min(this.start.getY(), this.end.getY()));
        Coordinates end = new Coordinates(Math.max(this.start.getX(), this.end.getX()), Math.max(this.start.getY(), this.end.getY()));
        if (originalPos == null) {
            originalPos = start;
        }
        selectedCells = App.world.getCellsInRect(start.toWorldCoordinates(App.world, true), end.toWorldCoordinates(App.world, true));
        App.world.fillCellsInRect(start.toWorldCoordinates(App.world, true), end.toWorldCoordinates(App.world, true), (byte)0);
        selecting.set(true);
    }

    public void finish() {
        snap();
        if (isSelecting(false)) {
            Coordinates start = new Coordinates(Math.min(this.start.getX(), this.end.getX()), Math.min(this.start.getY(), this.end.getY()));
            if (deleted || pasted) {
                App.world.getUndoManager().newUndoStage();
            }
            App.world.setCells(start.toWorldCoordinates(App.world, true), selectedCells);
        }
        originalPos = null;
        start = null;
        end = null;
        selectedCells = null;
        deleted = false;
        pasted = false;
        selecting.set(false);
    }

    public void cancel() {
        snap();
        if (isSelecting(false)) {
            App.world.setCells(originalPos.toWorldCoordinates(App.world, true), selectedCells);
        }
        originalPos = null;
        start = null;
        end = null;
        selectedCells = null;
        deleted = false;
        pasted = false;
        selecting.set(false);
    }

    public boolean isInSelection(Coordinates point) {
        if (isSelecting(true)) {
            float startX = Math.min(start.getX(), end.getX());
            float startY = Math.min(start.getY(), end.getY());
            float endX = Math.max(start.getX(), end.getX());
            float endY = Math.max(start.getY(), end.getY());
            return point.getX() > startX && point.getX() < endX && point.getY() > startY && point.getY() < endY;
        }
        return false;
    }

    public void delete() {
        if (isSelecting(false)) {
            selectedCells.clear();
            deleted = true;
            finish();
        }
    }

    @SuppressWarnings("unchecked")
    public void copy() {
        if (isSelecting(false)) {
            clipboard = (HashMap<Coordinates, Byte>) selectedCells.clone();
            clipboardSize = new Coordinates(getSize().getX() / App.world.getCellSize(), getSize().getY() / App.world.getCellSize());
            clipboardEmpty.set(false);
        }
    }

    public void cut() {
        copy();
        delete();
    }

    @SuppressWarnings("unchecked")
    public void paste() {
        if (clipboard != null && clipboardSize != null) {
            finish();
            float mouseX = App.ui.getContentPane().getMouseX();
            float mouseY = App.ui.getContentPane().getMouseY();
            if (App.ui.getContentPane().isCursorInArea()) {
                start = new Coordinates(mouseX - clipboardSize.getX() * App.world.getCellSize() / 2, mouseY - clipboardSize.getY() * App.world.getCellSize() / 2);
            } else {
                start = new Coordinates((float)App.ui.getContentPane().getCanvas().getWidth() / 2 - clipboardSize.getX() * App.world.getCellSize() / 2,
                        (float) App.ui.getContentPane().getCanvas().getHeight() / 2 - clipboardSize.getY() * App.world.getCellSize() / 2);
            }
            end = new Coordinates(start.getX() + clipboardSize.getX() * App.world.getCellSize(), start.getY() + clipboardSize.getY() * App.world.getCellSize());

            originalPos = start.copy();
            selectedCells = (HashMap<Coordinates, Byte>) clipboard.clone();
            pasted = true;
        }
    }

    public void setStart(Coordinates start) {
        this.start = start;
    }

    public void setEnd(Coordinates end) {
        this.end = end;
    }

    public void move(Coordinates delta, boolean dragging) {
        if (isSelecting(true)) {
            start.setX(start.getX() + delta.getX());
            start.setY(start.getY() + delta.getY());
            end.setX(end.getX() + delta.getX());
            end.setY(end.getY() + delta.getY());
            if (dragging) {
                this.dragging.set(true);
            } else {
                originalPos.setX(originalPos.getX() + delta.getX());
                originalPos.setY(originalPos.getY() + delta.getY());
            }
            if (selectedCells != null && !selectedCells.isEmpty()) {
                App.world.setSaved(false);
            }
        }
    }

    public boolean isSelecting(boolean allowEmptySelectedCells) {
        return (allowEmptySelectedCells || selectedCells != null) && start != null && end != null;
    }

    public boolean isDragging() {
        return dragging.get();
    }

    public BooleanProperty draggingProperty() {
        return dragging;
    }

    public void setDragging(boolean dragging) {
        this.dragging.set(dragging);
        if (!dragging)
            snap();
    }

    public void beforeCellResize() {
        if (isSelecting(true)) {
            start = start.toWorldCoordinates(App.world, true);
            end = end.toWorldCoordinates(App.world, true);
            originalPos = originalPos.toWorldCoordinates(App.world, true);
        }
    }

    public void afterCellResize() {
        if (isSelecting(true)) {
            start = start.toCanvasCoordinates(App.world);
            end = end.toCanvasCoordinates(App.world);
            originalPos = originalPos.toCanvasCoordinates(App.world);
        }
    }

    @SuppressWarnings("unchecked")
    public String toJson(String name, String category) {
        if (isSelecting(false)) {
            JSONObject object = new JSONObject();
            object.put("name", name);
            object.put("category", category);
            object.put("width", getSize().getX() / App.world.getCellSize());
            object.put("height", getSize().getY() / App.world.getCellSize());

            selectedCells.forEach((key, value) -> object.put("cell:" + key.toString(), value));
            return object.toJSONString();
        }
        return null;
    }

    public void spawnPattern(Pattern pattern) {
        if (App.world.getState() != World.RUNNING) {
            finish();

            float mouseX = App.ui.getContentPane().getMouseX();
            float mouseY = App.ui.getContentPane().getMouseY();
            if (App.ui.getContentPane().isCursorInArea()) {
                start = new Coordinates(mouseX - pattern.getWidth() * App.world.getCellSize() / 2, mouseY - pattern.getHeight() * App.world.getCellSize() / 2);
            } else {
                start = new Coordinates((float) App.ui.getContentPane().getCanvas().getWidth() / 2 - pattern.getWidth() * App.world.getCellSize() / 2,
                        (float) App.ui.getContentPane().getCanvas().getHeight() / 2 - pattern.getHeight() * App.world.getCellSize() / 2);
            }
            end = new Coordinates(start.getX() + pattern.getWidth() * App.world.getCellSize(), start.getY() + pattern.getHeight() * App.world.getCellSize());

            originalPos = start.copy();
            selectedCells = pattern.getCells();
            pasted = true;
        }
    }

    private Coordinates getDrawStart() {
        return new Coordinates(Math.min(this.start.getX(), this.end.getX()), Math.min(this.start.getY(), this.end.getY()));
    }

    private Coordinates getDrawEnd() {
        return new Coordinates(Math.max(this.start.getX(), this.end.getX()), Math.max(this.start.getY(), this.end.getY()));
    }

    private Coordinates getSize() {
        return new Coordinates(getDrawEnd().getX() - getDrawStart().getX(), getDrawEnd().getY() - getDrawStart().getY());
    }

    public BooleanProperty selectingProperty() {
        return selecting;
    }

    public boolean isSelecting() {
        return selecting.get();
    }

    public boolean isClipboardEmpty() {
        return clipboardEmpty.get();
    }

    public BooleanProperty clipboardEmptyProperty() {
        return clipboardEmpty;
    }
}
