package de.julianhofmann.world;

import java.util.Objects;

public class Coordinates {
    private float x, y;

    public Coordinates(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public Coordinates toWorldCoordinates(World world, boolean round) {
        float worldX, worldY;
        if (round) {
            worldX = Math.round((x - world.getCameraX()) / world.getCellSize());
            worldY = Math.round((y - world.getCameraY()) / world.getCellSize());
        } else {
            worldX = (int) Math.floor((x - world.getCameraX()) / world.getCellSize());
            worldY = (int) Math.floor((y - world.getCameraY()) / world.getCellSize());
        }
        return new Coordinates(worldX, worldY);
    }

    public Coordinates toCanvasCoordinates(World world) {
        float windowX = x * world.getCellSize() + world.getCameraX();
        float windowY = y * world.getCellSize() + world.getCameraY();

        return new Coordinates(windowX, windowY);
    }

    @Override
    public String toString() {
        return x+":"+y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Coordinates that = (Coordinates) o;
        return Float.compare(x, that.x) == 0 && Float.compare(y, that.y) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    public Coordinates copy() {
        return new Coordinates(x, y);
    }
}
