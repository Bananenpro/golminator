package de.julianhofmann.world;

import de.julianhofmann.App;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.util.ArrayList;

public class UndoManager {
    private final ArrayList<UndoStage> undoStages;
    private final World world;
    private int undoIndex;
    private final BooleanProperty undoAvailable = new SimpleBooleanProperty(false), redoAvailable = new SimpleBooleanProperty(false);

    public UndoManager(World world) {
        undoStages = new ArrayList<>();
        this.world = world;
        clear();
    }

    public void newUndoStage() {
        if (world.getState() == World.STOPPED) {
            if (undoIndex < 0 || undoStages.get(undoIndex).getBefore().size() > 0) {
                while (undoStages.size() - 1 > undoIndex) {
                    undoStages.remove(undoStages.size() - 1);
                }
                undoStages.add(new UndoStage());
                undoIndex++;
                if (undoStages.size() > App.settings.getMaxUndoStages()) {
                    undoStages.remove(0);
                    undoIndex--;
                }
                updateAvailability();
            }
        }
    }

    public void addChange(byte valueBefore, byte valueAfter, Coordinates coordinates) {
        if (world.getState() == World.STOPPED) {
            if (valueBefore != valueAfter) {
                UndoStage undoStage = undoStages.get(undoStages.size() - 1);
                byte undoBefore = undoStage.getBefore().getOrDefault(coordinates, (byte)-1);
                if (undoBefore == valueAfter) {
                    undoStage.getBefore().remove(coordinates);
                    undoStage.getAfter().remove(coordinates);
                } else {
                    undoStage.getBefore().put(coordinates, valueBefore);
                    undoStage.getAfter().put(coordinates, valueAfter);
                }
                updateAvailability();
            }
        }
    }

    public void undo() {
        if (world.getState() == World.STOPPED) {
            App.ui.getContentPane().getSelectionManager().finish();
            if (isUndoAvailable()) {
                undoStages.get(undoIndex).getBefore().forEach(world::undoCell);

                undoIndex--;

                updateAvailability();
                if (undoStages.get(undoIndex+1).getBefore().isEmpty()) undo();
            }
        }
    }

    public void redo() {
        if (world.getState() == World.STOPPED) {
            App.ui.getContentPane().getSelectionManager().cancel();
            if (isRedoAvailable()) {
                undoIndex++;
                undoStages.get(undoIndex).getAfter().forEach(world::undoCell);
                updateAvailability();
            }
        }
    }

    public void clear() {
        undoStages.clear();
        undoIndex = -1;
        newUndoStage();
    }

    private void updateAvailability() {
        undoAvailable.set(undoIndex >= 0 && !(undoIndex == 0 && undoStages.get(undoIndex).getBefore().isEmpty()));
        redoAvailable.set(undoStages.size() - 1 > undoIndex);
    }

    public boolean isUndoAvailable() {
        return undoAvailable.get();
    }

    public BooleanProperty undoAvailableProperty() {
        return undoAvailable;
    }

    public boolean isRedoAvailable() {
        return redoAvailable.get();
    }

    public BooleanProperty redoAvailableProperty() {
        return redoAvailable;
    }
}
