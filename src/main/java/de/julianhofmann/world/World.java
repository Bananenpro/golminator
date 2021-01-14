package de.julianhofmann.world;

import de.julianhofmann.App;
import javafx.beans.property.*;
import javafx.scene.Cursor;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class World {
    public static final int STOPPED = -1;
    public static final int PAUSED = 0;
    public static final int RUNNING = 1;
    public static final int DEFAULT_CAMERA_X = 0;
    public static final int DEFAULT_CAMERA_Y = 0;
    public static final float DEFAULT_CELL_SIZE = 10, MIN_CELL_SIZE = 0.1f, MAX_CELL_SIZE = 300;

    private final FloatProperty cameraX;
    private final FloatProperty cameraY;
    private final FloatProperty cellSize;
    private final IntegerProperty state;
    private final UndoManager undoManager;
    private HashMap<Coordinates, Byte> cells;
    private HashMap<Coordinates, Byte> updatedCells;
    private HashMap<Coordinates, Byte> cache;
    private final ReentrantReadWriteLock cellsLock = new ReentrantReadWriteLock();
    private final Lock cellsReadLock = cellsLock.readLock();
    private final Lock cellsWriteLock = cellsLock.writeLock();
    private final StringProperty filePath = new SimpleStringProperty(null);
    private final BooleanProperty saved = new SimpleBooleanProperty(true);

    @SuppressWarnings("StatementWithEmptyBody")
    public World() {
        cells = new HashMap<>();
        updatedCells = new HashMap<>();
        cache = new HashMap<>();
        cameraX = new SimpleFloatProperty(DEFAULT_CAMERA_X);
        cameraY = new SimpleFloatProperty(DEFAULT_CAMERA_Y);
        cellSize = new SimpleFloatProperty(DEFAULT_CELL_SIZE);
        state = new SimpleIntegerProperty(STOPPED);
        undoManager = new UndoManager(this);

        stateProperty().addListener((p, o, n) -> {
            if (n.intValue() == STOPPED && o.intValue() != STOPPED) {
                updatedCells.clear();
                loadFromCache();
            } else if (n.intValue() == PAUSED && o.intValue() != PAUSED) {

            } else if (n.intValue() == RUNNING && o.intValue() != RUNNING) {

            }
        });
    }

    /* ********************** Update ************************* */

    public void update() {
        if (getState() == RUNNING) {
            updatedCells = new HashMap<>(cells.size());
            cellsReadLock.lock();
            try {
                cells.forEach((key, value) -> {
                    if (getState() == RUNNING) {
                        updateCell(key.getX(), key.getY());

                        if (value == (byte) 1) {
                            updateCell(key.getX() - 1, key.getY() - 1);
                            updateCell(key.getX(), key.getY() - 1);
                            updateCell(key.getX() + 1, key.getY() - 1);

                            updateCell(key.getX() - 1, key.getY());
                            updateCell(key.getX() + 1, key.getY());

                            updateCell(key.getX() - 1, key.getY() + 1);
                            updateCell(key.getX(), key.getY() + 1);
                            updateCell(key.getX() + 1, key.getY() + 1);
                        }
                    }
                });
            } finally {
                cellsReadLock.unlock();
            }
            if (getState() == RUNNING) {
                cells = updatedCells;
            } else {
                updatedCells.clear();
            }
        }
    }

    public void updateCell(float x, float y) {
        Coordinates key = new Coordinates(x, y);
        if (!updatedCells.containsKey(key)) {
            int neighborSum = getCell(x - 1, y - 1) + getCell(x, y - 1) + getCell(x + 1, y - 1)
                    + getCell(x - 1, y) + getCell(x + 1, y)
                    + getCell(x - 1, y + 1) + getCell(x, y + 1) + getCell(x + 1, y + 1);

            if (neighborSum == 3 || (neighborSum == 2 && cells.getOrDefault(key, (byte) 0) == 1)) {
                updatedCells.put(key, (byte) 1);
            } else {
                updatedCells.put(key, (byte) 0);
            }


            if (neighborSum == 0) {
                updatedCells.remove(key);
            }
        }
    }

    /* ********************** Cache ************************* */

    public void storeInCache() {
        App.ui.setCursor(Cursor.WAIT);
        cellsReadLock.lock();
        try {
            cells.forEach((key, value) -> {
                if (value == 1) {
                    cache.put(key, value);
                }
            });
        } finally {
            cellsReadLock.unlock();
        }
        App.ui.removeCursor(Cursor.WAIT);
    }

    public void loadFromCache() {
        if (cache != null && cache.size() > 0) {
            cellsWriteLock.lock();
            try {
                HashMap<Coordinates, Byte> temp;
                temp = cells;
                cells = cache;
                cache = temp;
                cache.clear();
            } finally {
                cellsWriteLock.unlock();
            }
        }
    }

    public void clearCache() {
        cache.clear();
    }

    /* ********************** Loading/Saving ********************* */


    public void newFile() {
        if (App.ui.checkChanges()) {
            cellsWriteLock.lock();
            try {
                setCellSize(DEFAULT_CELL_SIZE);
                setCameraX(DEFAULT_CAMERA_X);
                setCameraY(DEFAULT_CAMERA_Y);
                clearCache();
                undoManager.clear();
                filePath.set(null);
                cells.clear();
                updatedCells.clear();
                setSaved(true);
            } finally {
                cellsWriteLock.unlock();
            }
        }
    }

    public boolean load(String path) {
        if (path == null) return false;

        HashMap<Coordinates, Byte> cells = new HashMap<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));
            setCellSize(Float.parseFloat(reader.readLine()));
            setCameraX(Float.parseFloat(reader.readLine()));
            setCameraY(Float.parseFloat(reader.readLine()));
            String line = reader.readLine();
            while (line != null) {
                String[] temp = line.split("/");
                if (temp.length == 2) {
                    cells.put(new Coordinates(Float.parseFloat(temp[0]), Float.parseFloat(temp[1])), (byte) 1);
                } else {
                    System.err.println("Invalid line: " + line);
                }
                line = reader.readLine();
            }
            reader.close();
            setCells(cells);
            filePath.set(path);
            saved.set(true);
            return true;
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
        }
        return false;
    }


    @SuppressWarnings({"BooleanMethodIsAlwaysInverted", "ResultOfMethodCallIgnored"})
    public boolean save(String path) {
        if (path == null) return false;

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(path));
            writer.write(getCellSize()+"\n");
            writer.write(getCameraX()+"\n");
            writer.write(getCameraY()+"\n");
            cellsReadLock.lock();
            try {
                cells.forEach((key, value) -> {
                    if (value != 0) {
                        try {
                            writer.write(key.getX() + "/" + key.getY() + "\n");
                        } catch (IOException e) {
                            try {
                                writer.close();
                                File file = new File(path);
                                if (file.exists()) file.delete();
                            } catch (IOException ioException) {
                                ioException.printStackTrace();
                            }
                            e.printStackTrace();
                        }
                    }
                });
            } finally {
                cellsReadLock.unlock();
            }
            File file = new File(path);
            if (getFilePath() == null && file.exists()) filePath.set(file.getPath());
            writer.flush();
            writer.close();
            saved.set(file.exists());
            return file.exists();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void updatePosition() {
        if (getFilePath() != null) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(getFilePath()));
                ArrayList<String> lines = new ArrayList<>();
                String line = reader.readLine();
                while (line != null) {
                    lines.add(line);
                    line = reader.readLine();
                }
                reader.close();

                String cellSize = Float.toString(getCellSize());
                String cameraX = Float.toString(getCameraX());
                String cameraY = Float.toString(getCameraY());

                if (!cellSize.equals(lines.get(0)) || !cameraX.equals(lines.get(1)) || !cameraY.equals(lines.get(2))) {
                    lines.set(0, cellSize);
                    lines.set(1, cameraX);
                    lines.set(2, cameraY);

                    BufferedWriter writer = new BufferedWriter(new FileWriter(getFilePath()));
                    for (String l : lines) {
                        writer.write(l + "\n");
                    }
                    writer.flush();
                    writer.close();
                }
            } catch (IOException | NumberFormatException | ArrayIndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        }
    }

    /* ********************** Setters ************************* */

    /* ---------------------- Cells ------------------------ */

    public void setCells(HashMap<Coordinates, Byte> cells) {
        if (getState() != RUNNING) {
            this.cells = cells;
            clearCache();
            undoManager.clear();
        }
    }

    public void setCells(Coordinates coordinates, HashMap<Coordinates, Byte> cells) {
        if (getState() != RUNNING) {
            cells.forEach((key, value) -> setCell(coordinates.getX() + key.getX(),coordinates.getY() + key.getY(), value));
        }
    }

    public void setCell(Coordinates coordinates, byte value) {
        if (getState() != RUNNING) {
            cellsWriteLock.lock();
            try {
                undoManager.addChange(getCell(coordinates), value, coordinates);
                cells.put(coordinates, value);
                if (getState() == STOPPED) {
                    saved.set(false);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                cellsWriteLock.unlock();
            }
        }
    }

    public void setCell(float x, float y, byte value) {
        setCell(new Coordinates(x, y), value);
    }

    public void undoCell(Coordinates coordinates, byte value) {
        if (getState() != RUNNING) {
            cellsWriteLock.lock();
            try {
                cells.put(coordinates, value);
                if (getState() == STOPPED) {
                    saved.set(false);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                cellsWriteLock.unlock();
            }
        }
    }

    public boolean toggleCell(Coordinates coordinates) {
        if (getState() != RUNNING) {
            setCell(coordinates, getCell(coordinates) == 1 ? (byte) 0 : 1);
        }
        return getCell(coordinates) == 1;
    }

    public void fillCellsInRect(Coordinates start, Coordinates end, byte value) {
        if (end.getX() > start.getX() && end.getY() > start.getY()) {

            for (float x = start.getX(); x <= end.getX(); x++) {
                for (float y = start.getY(); y < end.getY(); y++) {
                    setCell(x, y, value);
                }
            }
        }
    }

    public void fillCellsOnLine(Coordinates start, Coordinates end, byte value) {
        // Bresenham Algorithm
        int x = (int)start.getX();
        int x2 = (int)end.getX();
        int y = (int)start.getY();
        int y2 = (int)end.getY();
        int w = x2 - x;
        int h = y2 - y;
        int dx1 = 0, dy1 = 0, dx2 = 0, dy2 = 0;
        if (w < 0) dx1 = -1;
        else if (w > 0) dx1 = 1;
        if (h < 0) dy1 = -1;
        else if (h > 0) dy1 = 1;
        if (w < 0) dx2 = -1;
        else if (w > 0) dx2 = 1;
        int longest = Math.abs(w);
        int shortest = Math.abs(h);
        if (!(longest > shortest)) {
            longest = Math.abs(h);
            shortest = Math.abs(w);
            if (h < 0) dy2 = -1;
            else if (h > 0) dy2 = 1;
            dx2 = 0;
        }
        int numerator = longest >> 1;
        for (int i = 0; i <= longest; i++) {
            setCell(x, y, value);

            numerator += shortest;
            if (!(numerator < longest)) {
                numerator -= longest;
                x += dx1;
                y += dy1;
            } else {
                x += dx2;
                y += dy2;
            }
        }
    }

    /* ---------------------- Other ------------------------ */

    public void setCameraX(float cameraX) {
        this.cameraX.set(cameraX);
        if (getCellSize() >= 1) {
            this.cameraX.set(Math.round(getCameraX()));
        }
    }

    public void addCameraX(float amount) {
        cameraX.set(cameraX.get() + amount);
    }

    public void setCameraY(float cameraY) {
        this.cameraY.set(cameraY);
        if (getCellSize() >= 1) {
            this.cameraY.set(Math.round(getCameraY()));
        }
    }

    public void addCameraY(float amount) {
        cameraY.set(cameraY.get() + amount);
    }

    public void setCellSize(float newCellSize) {
        if (newCellSize >= 1) newCellSize = Math.round(newCellSize);
        App.ui.getContentPane().getSelectionManager().beforeCellResize();

        float x;
        float y;
        if (App.ui.getContentPane().isCursorInArea()) {
            x = App.ui.getContentPane().getMouseX();
            y = App.ui.getContentPane().getMouseY();
        } else {
            x = (float) (App.ui.getContentPane().getCanvas().getWidth() / 2);
            y = (float) (App.ui.getContentPane().getCanvas().getHeight() / 2);
        }

        if (newCellSize < MIN_CELL_SIZE) newCellSize = MIN_CELL_SIZE;
        if (newCellSize > MAX_CELL_SIZE) newCellSize = MAX_CELL_SIZE;
        float centerX = (x - getCameraX()) / getCellSize();
        float centerY = (y - getCameraY()) / getCellSize();
        setCameraX(cameraX.divide(getCellSize()).floatValue());
        setCameraY(cameraY.divide(getCellSize()).floatValue());
        setCameraX(cameraX.multiply(newCellSize).floatValue());
        setCameraY(cameraY.multiply(newCellSize).floatValue());
        cellSize.set(newCellSize);
        double centerXAfter = (x - getCameraX()) / (double) newCellSize;
        double centerYAfter = (y - getCameraY()) / (double) newCellSize;
        double centerXDiff = centerX - centerXAfter;
        double centerYDiff = centerY - centerYAfter;
        setCameraX(cameraX.subtract(centerXDiff * newCellSize).floatValue());
        setCameraY(cameraY.subtract(centerYDiff * newCellSize).floatValue());
        App.ui.getContentPane().getSelectionManager().afterCellResize();
    }

    public void addCellSize(float amount) {
        if (cellSize.get() + amount >= 1 && amount < 0 && amount > -1) amount = -1;
        else if (cellSize.get() + amount >= 1 && amount > 0 && amount < 1) amount = 1;
        setCellSize(cellSize.get() + amount);
    }

    public void setState(int state) {
        if (state == World.RUNNING) {
            App.ui.getContentPane().getSelectionManager().finish();
            if (this.state.get() != PAUSED) {
                storeInCache();
            }
        }
        this.state.set(state);
    }

    public void setSaved(boolean saved) {
        this.saved.set(saved);
    }

    /* ********************** Getters ************************* */

    /* ---------------------- Cells ------------------------ */

    public HashMap<Coordinates, Byte> getCells() {
        return cells;
    }

    public byte getCell(Coordinates coordinates) {
        return cells.getOrDefault(coordinates, (byte) 0);
    }

    public byte getCell(float x, float y) {
        return cells.getOrDefault(new Coordinates(x, y), (byte) 0);
    }

    public HashMap<Coordinates, Byte> getCellsInRect(Coordinates start, Coordinates end) {
        HashMap<Coordinates, Byte> result = new HashMap<>();

        if (end.getX() > start.getX() && end.getY() > start.getY()) {

            for (float x = start.getX(); x <= end.getX(); x ++) {
                for (float y = start.getY(); y < end.getY(); y++) {
                    if (getCell(new Coordinates(x, y)) == 1) {
                        result.put(new Coordinates(x - start.getX(), y - start.getY()), (byte) 1);
                    }
                }
            }
        }

        return result;
    }

    /* ---------------------- Other ------------------------ */

    public float getCameraX() {
        return cameraX.get();
    }

    public FloatProperty cameraXProperty() {
        return cameraX;
    }

    public float getCameraY() {
        return cameraY.get();
    }

    public FloatProperty cameraYProperty() {
        return cameraY;
    }

    public float getCellSize() {
        return cellSize.get();
    }

    public FloatProperty cellSizeProperty() {
        return cellSize;
    }

    public int getState() {
        return state.get();
    }

    public IntegerProperty stateProperty() {
        return state;
    }

    public UndoManager getUndoManager() {
        return undoManager;
    }

    public Lock getCellsReadLock() {
        return cellsReadLock;
    }

    public String getFilePath() {
        return filePath.get();
    }

    public StringProperty filePathProperty() {
        return filePath;
    }


    public boolean isSaved() {
        return saved.get();
    }

    public BooleanProperty savedProperty() {
        return saved;
    }
}
