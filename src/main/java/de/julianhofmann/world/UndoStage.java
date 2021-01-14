package de.julianhofmann.world;

import java.util.HashMap;

public class UndoStage {
    private final HashMap<Coordinates, Byte> before;
    private final HashMap<Coordinates, Byte> after;

    public UndoStage() {
        this.before = new HashMap<>();
        this.after = new HashMap<>();
    }

    public HashMap<Coordinates, Byte> getBefore() {
        return before;
    }

    public HashMap<Coordinates, Byte> getAfter() {
        return after;

    }
}
