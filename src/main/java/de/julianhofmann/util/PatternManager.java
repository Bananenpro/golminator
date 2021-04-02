package de.julianhofmann.util;

import de.julianhofmann.world.Coordinates;
import de.julianhofmann.world.Pattern;
import de.julianhofmann.world.PatternCategory;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class PatternManager {
    public static final String PATTERN_DIRECTORY = "rsc/patterns/";

    private ArrayList<Pattern> patterns;
    private List<File> lastFiles;
    private final Lock refreshLock = new ReentrantLock();
    private boolean changed;

    public PatternManager() {
        File patternDir = new File(PATTERN_DIRECTORY);
        if (!patternDir.exists()) patternDir.mkdirs();
        refreshPatterns();
    }

    public boolean savePattern(String name, String category, String json) {
        if (name.isBlank() || category.isBlank() || json == null || json.isBlank()) return false;
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(PatternManager.PATTERN_DIRECTORY + name + ".json"));
            writer.write(json);
            writer.flush();
            writer.close();
            refreshPatterns();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public void refreshPatterns() {
        refreshLock.lock();
        try {
            JSONParser parser = new JSONParser();

            File patternDir = new File(PATTERN_DIRECTORY);
            File[] filesArray = patternDir.listFiles(file -> file.getName().endsWith(".json"));
            if (filesArray != null) {
                List<File> files = Arrays.asList(filesArray);
                if (lastFiles == null || !(lastFiles.size() == files.size() && lastFiles.containsAll(files))) {
                    patterns = new ArrayList<>();
                    for (File file : files) {
                        try {
                            BufferedReader reader = new BufferedReader(new FileReader(file));
                            StringBuilder sb = new StringBuilder();
                            String line = reader.readLine();
                            while (line != null) {
                                sb.append(line);
                                line = reader.readLine();
                            }
                            reader.close();

                            JSONObject object = (JSONObject) parser.parse(sb.toString());

                            String name = (String) object.get("name");
                            String category = (String) object.get("category");
                            float width = (float) (double) object.get("width");
                            float height = (float) (double) object.get("height");

                            HashMap<Coordinates, Byte> cells = new HashMap<>();

                            object.forEach((key, value) -> {
                                if (key.toString().startsWith("cell:")) {
                                    String[] temp = key.toString().split(":");
                                    if (temp.length == 3) {
                                        try {
                                            cells.put(new Coordinates(Float.parseFloat(temp[1]), Float.parseFloat(temp[2])), Byte.parseByte(value.toString()));
                                        } catch (NumberFormatException ignored) {
                                        }
                                    }
                                }
                            });

                            patterns.add(new Pattern(name, category, width, height, cells));
                        } catch (IOException | ParseException e) {
                            e.printStackTrace();
                        }
                    }
                    patterns.sort(Comparator.comparing(Pattern::getCategory));
                    patterns.sort((pattern, pattern2) -> {
                        if (pattern.getCategory().equals(pattern2.getCategory())) {
                            return pattern.getName().compareTo(pattern2.getName());
                        }
                        return 0;
                    });
                    changed = true;
                }
                lastFiles = files;
            }
        } finally {
            refreshLock.unlock();
        }
    }

    public ArrayList<Pattern> getPatterns() {
        return patterns;
    }

    public Pattern getPattern(String name) {
        return patterns.stream().filter(p -> p.getName().equals(name)).findFirst().orElse(null);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean nameExists(String name) {
        return getPattern(name) != null;
    }

    public void deletePattern(Pattern pattern) {
        if (pattern instanceof PatternCategory) {
            List<Pattern> patternsInCategory = patterns.stream().filter(p -> p.getCategory().equals(pattern.getCategory())).collect(Collectors.toList());
            for (Pattern p : patternsInCategory) {
                File file = new File(PATTERN_DIRECTORY + p.getName() + ".json");
                if (file.exists()) {
                    file.delete();
                }
            }
        } else {
            File file = new File(PATTERN_DIRECTORY + pattern.getName() + ".json");
            if (file.exists()) {
                file.delete();
            }
        }
        refreshPatterns();
    }

    public boolean isChanged() {
        return changed;
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
    }
}
