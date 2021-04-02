package de.julianhofmann.world;

import de.julianhofmann.App;
import org.json.simple.JSONObject;

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

    @SuppressWarnings("unchecked")
    public String toJson() {
        JSONObject object = new JSONObject();
        object.put("name", name);
        object.put("category", category);
        object.put("width", width);
        object.put("height", height);

        cells.forEach((key, value) -> object.put("cell:" + key.toString(), value));
        return object.toJSONString();
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

    public void setName(String name) {
        this.name = name;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
