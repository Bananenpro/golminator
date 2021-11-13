package de.julianhofmann.util;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.util.Locale;

public class Settings {
    private final String FILE_PATH = getConfigDir() + "/settings.json";
    private final BooleanProperty darkTheme = new SimpleBooleanProperty(false);
    private final IntegerProperty maxUndoStages = new SimpleIntegerProperty(100);
    private final IntegerProperty windowWidth = new SimpleIntegerProperty(1200), windowHeight = new SimpleIntegerProperty(800);

    public Settings() {
    }

    public void load() {
        JSONParser parser = new JSONParser();
        try {
            JSONObject object = (JSONObject) parser.parse(new FileReader(FILE_PATH));
            try {
                setDarkTheme((boolean) object.get("dark_theme"));
            } catch (NullPointerException ignored) { }
            try {
                setWindowWidth((int) (long) object.get("window_width"));
            } catch (NullPointerException ignored) { }
            try {
                setWindowHeight((int) (long) object.get("window_height"));
            } catch (NullPointerException ignored) { }
            try {
                setMaxUndoStages((int) (long) object.get("max_undo_stages"));
            } catch (NullPointerException ignored) { }
        } catch (ParseException | IOException ignored) {
            File file = new File(FILE_PATH);
            //noinspection ResultOfMethodCallIgnored
            file.delete();
            try {
                //noinspection ResultOfMethodCallIgnored
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void save() {
        JSONObject object = new JSONObject();
        object.put("dark_theme", isDarkTheme());
        object.put("window_width", getWindowWidth());
        object.put("window_height", getWindowHeight());
        object.put("max_undo_stages", getMaxUndoStages());
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_PATH));
            writer.write(object.toJSONString());
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getWindowWidth() {
        return windowWidth.get();
    }

    public void setWindowWidth(int windowWidth) {
        this.windowWidth.set(windowWidth);
    }

    public int getWindowHeight() {
        return windowHeight.get();
    }

    public void setWindowHeight(int windowHeight) {
        this.windowHeight.set(windowHeight);
    }

    public boolean isDarkTheme() {
        return darkTheme.get();
    }

    public BooleanProperty darkThemeProperty() {
        return darkTheme;
    }

    public int getMaxUndoStages() {
        return maxUndoStages.get();
    }

    public void setMaxUndoStages(int maxUndoStages) {
        this.maxUndoStages.set(maxUndoStages);
    }

    public void setDarkTheme(boolean darkTheme) {
        this.darkTheme.set(darkTheme);
    }

    private String getConfigDir() {
        String rootPath;
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("windows")) {
            rootPath = System.getenv("APPDATA");
        } else {
            rootPath = System.getenv("XDG_CONFIG_HOME");

            if(rootPath == null) {
                rootPath = System.getProperty("user.home")+"/.config";
            }
        }

        String path = rootPath + "/golminator";
        //noinspection ResultOfMethodCallIgnored
        new File(path).mkdirs();

        return path;
    }
}
