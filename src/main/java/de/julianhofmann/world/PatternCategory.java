package de.julianhofmann.world;

public class PatternCategory extends Pattern {

    public PatternCategory(String category) {
        this.category = category;
    }

    @Override
    public String toString() {
        return category;
    }
}
