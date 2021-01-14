package de.julianhofmann.world;

import java.util.HashMap;

public class Pattern {
    protected String name, category;
    protected float width, height;
    protected HashMap<Coordinates, Byte> cells;

    public Pattern() { }

    public Pattern(String name, String category, float width, float height, HashMap<Coordinates, Byte> cells) {
        this.name = name;
        this.category = category;
        this.width = width;
        this.height = height;
        this.cells = cells;
    }

    @Override
    public String toString() {
        return name;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public HashMap<Coordinates, Byte> getCells() {
        return cells;
    }
}
