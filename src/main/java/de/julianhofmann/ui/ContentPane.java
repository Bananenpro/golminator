package de.julianhofmann.ui;

import de.julianhofmann.App;
import de.julianhofmann.world.Coordinates;
import de.julianhofmann.world.World;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

public class ContentPane {
    private final Canvas canvas;

    private final BooleanProperty editing = new SimpleBooleanProperty(false);
    private final BooleanProperty draggingMiddle = new SimpleBooleanProperty(false);
    private final BooleanProperty cursorInArea = new SimpleBooleanProperty(false);

    private boolean showGrid;

    private Color background, backgroundTransparent;
    private Color grid;
    private Color foreground, foregroundTransparent;
    private Color highlight;
    private Color selection;
    private WritableImage image;
    private WritableImage backBuffer;

    private final float NO_MOUSE_POS = Float.MIN_VALUE;
    private float mouseX = NO_MOUSE_POS, mouseY = NO_MOUSE_POS;
    private float lastPrimaryX = NO_MOUSE_POS, lastPrimaryY = NO_MOUSE_POS;
    private float lastMiddleMouseX = NO_MOUSE_POS, lastMiddleMouseY = NO_MOUSE_POS;

    private final Renderer renderer;
    private final SelectionManager selectionManager;

    private boolean drawing;
    private boolean activating;

    public ContentPane(Pane pane, Canvas canvas) {
        this.canvas = canvas;

        canvas.widthProperty().bind(pane.widthProperty());
        canvas.heightProperty().bind(pane.heightProperty());

        swapBuffers();
        renderer = new Renderer(this);

        showGrid = true;
        App.world.cellSizeProperty().addListener((p, oldValue, newValue) -> showGrid = newValue.floatValue() >= 5);

        updateColors(App.settings.isDarkTheme());
        App.settings.darkThemeProperty().addListener((p, oldValue, newValue) -> updateColors(newValue));

        selectionManager = new SelectionManager();

        canvas.addEventFilter(MouseEvent.MOUSE_PRESSED, this::mousePressed);
        canvas.addEventFilter(MouseEvent.MOUSE_RELEASED, this::mouseReleased);
        canvas.addEventFilter(MouseEvent.MOUSE_MOVED, this::mouseMoved);
        canvas.addEventFilter(MouseEvent.MOUSE_DRAGGED, this::mouseDragged);
        canvas.addEventFilter(MouseEvent.MOUSE_ENTERED, this::mouseEntered);
        canvas.addEventFilter(MouseEvent.MOUSE_EXITED, this::mouseExited);
        canvas.addEventFilter(ScrollEvent.SCROLL, this::mouseScrolled);
        canvas.addEventFilter(KeyEvent.KEY_PRESSED, this::keyPressed);

        editing.addListener((p, o, n) -> updateCursor());
        draggingMiddle.addListener((p, o, n) -> updateCursor());
        selectionManager.draggingProperty().addListener((p, o, n) -> updateCursor());
        cursorInArea.addListener((p, o, n) -> updateCursor());

        editing.set(App.world.getState() != World.RUNNING);
        App.world.stateProperty().addListener((p, o, n) -> {
            setEditing(n.intValue() != World.RUNNING);
            updateCursor();
        });
    }

    public void swapBuffers() {
        image = backBuffer;
        int width = (int)Math.ceil(canvas.getWidth());
        int height = (int)Math.ceil(canvas.getHeight());
        if (width <= 0) width = 1;
        if (height <= 0) height = 1;
        backBuffer = new WritableImage(width, height);
    }

    public void draw() {
        if (image != null) {
            GraphicsContext gc = canvas.getGraphicsContext2D();
            gc.setFill(background);
            gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
            gc.drawImage(image, 0, 0, canvas.getWidth(), canvas.getHeight());
        }
    }

    /* ********************* Input Events *********************** */

    private void mousePressed(MouseEvent mouseEvent) {
        if (mouseEvent.getButton() == MouseButton.PRIMARY) {
            if (!drawing) {
                if (!selectionManager.isInSelection(new Coordinates((float)mouseEvent.getX(), (float)mouseEvent.getY()))) {
                    if (selectionManager.isSelecting(true)) {
                        selectionManager.finish();
                    } else {
                        drawing = true;
                        App.world.getUndoManager().newUndoStage();
                        activating = App.world.toggleCell(new Coordinates((float) mouseEvent.getX(), (float) mouseEvent.getY()).toWorldCoordinates(App.world, false));
                        lastPrimaryX = (float) mouseEvent.getX();
                        lastPrimaryY = (float) mouseEvent.getY();
                    }
                }
            }
        } else if (mouseEvent.getButton() == MouseButton.SECONDARY) {
            selectionManager.finish();
            selectionManager.setStart(new Coordinates((float)mouseEvent.getX(), (float)mouseEvent.getY()));
        }
    }

    private void mouseReleased(MouseEvent mouseEvent) {
        if (mouseEvent.getButton() == MouseButton.PRIMARY) {
            drawing = false;
            lastPrimaryX = NO_MOUSE_POS;
            lastPrimaryY = NO_MOUSE_POS;
            if (selectionManager.isDragging()) {
                selectionManager.setDragging(false);
                selectionManager.snap();
            }
        } else if (mouseEvent.getButton() == MouseButton.MIDDLE) {
            draggingMiddle.set(false);
            lastMiddleMouseX = NO_MOUSE_POS;
            lastMiddleMouseY = NO_MOUSE_POS;
        } else if (mouseEvent.getButton() == MouseButton.SECONDARY) {
            if (selectionManager.isSelecting(true)) {
                selectionManager.snap();
                selectionManager.select();
            } else {
                selectionManager.cancel();
            }
        }
    }

    private void mouseMoved(MouseEvent mouseEvent) {
        mouseX = (float) mouseEvent.getX();
        mouseY = (float) mouseEvent.getY();
    }

    private void mouseDragged(MouseEvent mouseEvent) {
        mouseX = (float) mouseEvent.getX();
        mouseY = (float) mouseEvent.getY();

        if (mouseEvent.getButton() == MouseButton.PRIMARY) {
            if (lastPrimaryX != NO_MOUSE_POS && lastPrimaryY != NO_MOUSE_POS) {
                if (drawing) {
                    App.world.fillCellsOnLine(new Coordinates(lastPrimaryX, lastPrimaryY).toWorldCoordinates(App.world, false), new Coordinates(mouseX, mouseY).toWorldCoordinates(App.world, false), activating ? (byte) 1 : 0);
                } else if (selectionManager.isDragging() || selectionManager.isInSelection(new Coordinates(mouseX, mouseY))) {
                    selectionManager.move(new Coordinates(mouseX - lastPrimaryX, mouseY - lastPrimaryY), true);
                }
            }
            lastPrimaryX = mouseX;
            lastPrimaryY = mouseY;
        } else if (mouseEvent.getButton() == MouseButton.MIDDLE) {
            if (lastMiddleMouseX != NO_MOUSE_POS && lastMiddleMouseY != NO_MOUSE_POS) {
                draggingMiddle.set(true);
                float deltaX = (float) (mouseEvent.getX() - lastMiddleMouseX);
                float deltaY = (float) (mouseEvent.getY() - lastMiddleMouseY);

                selectionManager.move(new Coordinates(deltaX, deltaY), false);

                App.world.addCameraX(deltaX);
                App.world.addCameraY(deltaY);
            }

            lastMiddleMouseX = (float) mouseEvent.getX();
            lastMiddleMouseY = (float) mouseEvent.getY();
        } else if (mouseEvent.getButton() == MouseButton.SECONDARY) {
            selectionManager.setEnd(new Coordinates((float) mouseEvent.getX(), (float)mouseEvent.getY()));
        }
    }

    private void mouseEntered(MouseEvent mouseEvent) {
        setCursorInArea(true);
        mouseX = (float) mouseEvent.getX();
        mouseY = (float) mouseEvent.getY();
        canvas.requestFocus();
    }

    private void mouseExited(MouseEvent mouseEvent) {
        setCursorInArea(false);
        mouseX = NO_MOUSE_POS;
        mouseY = NO_MOUSE_POS;
    }

    private void mouseScrolled(ScrollEvent scrollEvent) {
        App.world.addCellSize((float)scrollEvent.getDeltaY() / 40);
    }

    private void keyPressed(KeyEvent keyEvent) {
        if (keyEvent.getCode() == KeyCode.ESCAPE) {
            selectionManager.cancel();
        } else if (keyEvent.getCode() == KeyCode.ENTER) {
            selectionManager.finish();
        }
    }

    /* ********************* Update Methods *********************** */

    @SuppressWarnings("IfStatementWithIdenticalBranches")
    private void updateColors(boolean darkTheme) {
        if (darkTheme) {
            background = new Color(0.05, 0.05, 0.05, 1);
            backgroundTransparent = new Color(0.05, 0.05, 0.05, 0.3);
            grid = new Color(0.4, 0.4, 0.4, 0.5);
            foreground = new Color(0.95, 0.95, 0.95,1);
            foregroundTransparent = new Color(0.95, 0.95, 0.95, 0.3);
            highlight = new Color(0.6, 0.6, 0.6, 0.5);
            selection = new Color(0, 0.5, 0.8, 0.8);
        } else {
            background = new Color(0.9, 0.9, 0.9, 1);
            backgroundTransparent = new Color(0.9, 0.9, 0.9, 0.3);
            grid = new Color(0.6, 0.6, 0.6, 0.5);
            foreground = new Color(0.03, 0.03, 0.03, 1);
            foregroundTransparent = new Color(0.03, 0.03, 0.03, 0.43);
            highlight = new Color(0.5, 0.5, 0.5, 0.5);
            selection = new Color(0, 0.5, 0.8, 0.8);
        }
    }

    private void updateCursor() {
        if (isCursorInArea() && isEditing())
            App.ui.setCursor(Cursor.CROSSHAIR);
        else
            App.ui.removeCursor(Cursor.CROSSHAIR);

        if (isDraggingMiddle() || selectionManager.isDragging())
            App.ui.setCursor(Cursor.CLOSED_HAND);
        else
            App.ui.removeCursor(Cursor.CLOSED_HAND);
    }

    /* ********************* Getters/Setters *********************** */

    public boolean isEditing() {
        return editing.get();
    }

    private void setEditing(boolean editing) {
        this.editing.set(editing);
    }

    private boolean isDraggingMiddle() {
        return draggingMiddle.get();
    }

    public boolean isCursorInArea() {
        return cursorInArea.get();
    }

    private void setCursorInArea(boolean cursorInArea) {
        this.cursorInArea.set(cursorInArea);
    }

    public Color getBackground() {
        return background;
    }

    public Color getBackgroundTransparent() {
        return backgroundTransparent;
    }

    public Color getGrid() {
        return grid;
    }

    public Color getForeground() {
        return foreground;
    }

    public Color getForegroundTransparent() {
        return foregroundTransparent;
    }

    public Color getHighlight() {
        return highlight;
    }

    public Color getSelection() {
        return selection;
    }

    public Renderer getRenderer() {
        return renderer;
    }

    public float getMouseX() {
        return mouseX;
    }

    public float getMouseY() {
        return mouseY;
    }

    public Canvas getCanvas() {
        return canvas;
    }

    public WritableImage getBackBuffer() {
        return backBuffer;
    }

    public boolean isShowGrid() {
        return showGrid;
    }

    public SelectionManager getSelectionManager() {
        return selectionManager;
    }
}
