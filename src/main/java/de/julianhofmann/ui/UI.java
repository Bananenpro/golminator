package de.julianhofmann.ui;

import de.julianhofmann.App;
import de.julianhofmann.world.World;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import jfxtras.styles.jmetro.JMetro;
import jfxtras.styles.jmetro.Style;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.Stack;

public class UI {
    private final Stage primaryStage;
    private Scene primaryScene;
    private JMetro jMetro;
    private final Stack<Cursor> cursors;
    private PrimaryController primaryController;
    private ContentPane contentPane;
    private PatternList patternList;
    private boolean askedToSave = false;

    public UI(Stage primaryStage) {
        this.primaryStage = primaryStage;
        cursors = new Stack<>();
    }

    public void init() {
        Parent fxml = loadFXML("primary");

        jMetro = new JMetro(App.settings.isDarkTheme() ? Style.DARK : Style.LIGHT);

        App.settings.darkThemeProperty().addListener((v, oldValue, newValue) -> jMetro.setStyle(newValue ? Style.DARK : Style.LIGHT));

        if (fxml == null) {
            App.exit();
            primaryScene = null;
            return;
        }

        primaryScene = new Scene(fxml, App.settings.getWindowWidth(), App.settings.getWindowHeight());
        primaryScene.widthProperty().addListener((p, o, n) -> App.settings.setWindowWidth((int)n.doubleValue()));
        primaryScene.heightProperty().addListener((p, o, n) -> App.settings.setWindowHeight((int)n.doubleValue()));

        jMetro.setScene(primaryScene);
        primaryStage.setScene(primaryScene);
        primaryStage.setOnCloseRequest(windowEvent -> {
            if (!App.ui.checkChanges()) {
                windowEvent.consume();
            }
        });
        primaryStage.show();
    }

    /* ******************* Dialogs ********************* */

    public Optional<ButtonType> alert(Alert.AlertType type, String title, String text, ButtonType... buttonTypes) {
        Alert alert = new Alert(type, text, buttonTypes);
        alert.initOwner(primaryStage);
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.setTitle(title);
        alert.setHeaderText(title);
        JMetro jMetro = new JMetro(App.settings.isDarkTheme() ? Style.DARK : Style.LIGHT);
        jMetro.setScene(alert.getDialogPane().getScene());

        alert.setResizable(true);
        alert.onShownProperty().addListener(e -> Platform.runLater(() -> {
            alert.getDialogPane().getScene().getWindow().sizeToScene();
            alert.setResizable(false);
        }));
        return alert.showAndWait();
    }

    public String inputDialog(String title, String content, String value) {
        TextInputDialog input = new TextInputDialog("Verzögerung...");
        input.setHeaderText(title);
        input.setContentText(content);
        input.getEditor().setText(value);
        input.initOwner(getPrimaryStage());
        input.initModality(Modality.APPLICATION_MODAL);
        input.setTitle(title);
        JMetro jMetro = new JMetro(App.settings.isDarkTheme() ? Style.DARK : Style.LIGHT);
        jMetro.setScene(input.getDialogPane().getScene());

        input.setResizable(true);
        input.onShownProperty().addListener(e -> Platform.runLater(() -> {
            input.getDialogPane().getScene().getWindow().sizeToScene();
            input.setResizable(false);
        }));

        input.showAndWait();
        return input.getEditor().getText();
    }

    public void open() {
        if (checkChanges()) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Öffnen...");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Game of Life Datei", "*.gol"));
            File selectedFile = fileChooser.showOpenDialog(primaryStage);
            if (selectedFile != null) {
                App.world.setState(World.STOPPED);
                if (!App.world.load(selectedFile.getPath())) {
                    alert(Alert.AlertType.ERROR, "Fehler", "Die Datei konnte nicht geöffnet werden!", ButtonType.OK);
                }
            }
        }
    }

    @SuppressWarnings("ConstantConditions")
    public boolean checkChanges() {
        App.world.setState(World.STOPPED);
        while (!App.world.isSaved() && !askedToSave) {
            askedToSave = false;
            Optional<ButtonType> button = App.ui.alert(Alert.AlertType.CONFIRMATION, "Ungesicherte Änderungen", "Möchtest du die Änderungen speichern?", ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
            if (button.isPresent() && button.get() != ButtonType.CANCEL) {
                if (button.get() == ButtonType.YES) {
                    App.ui.save();
                } else {
                    askedToSave = true;
                }
            } else {
                return false;
            }
        }
        if (App.world.isSaved())
            App.world.updatePosition();
        return true;
    }

    public void save() {
        if (!App.world.save(App.world.getFilePath())) {
            saveAs();
        }
    }

    public void saveAs() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Speichern Als...");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Game of Life Datei", "*.gol"));
        File selectedFile = fileChooser.showSaveDialog(primaryStage);
        if (selectedFile != null) {
            App.world.setState(World.STOPPED);
            String path = selectedFile.getPath();
            if (!path.endsWith(".gol")) path += ".gol";
            if (!App.world.save(path)) {
                alert(Alert.AlertType.ERROR, "Fehler", "Speichern fehlgeschlagen", ButtonType.OK);
            }
        }
    }

    /* ******************* Cursor ********************* */

    public void setCursor(Cursor cursor) {
        if (primaryScene != null) {
            if (primaryScene.getCursor() == cursor) return;
            if (cursors.contains(cursor)) removeCursor(cursor);
            cursors.push(primaryScene.getCursor());
            primaryScene.setCursor(cursor);
        }
    }

    public void removeCursor(Cursor cursor) {
        if (primaryScene != null) {
            if (cursors.isEmpty()) {
                primaryScene.setCursor(Cursor.DEFAULT);
            } else {
                if (primaryScene.getCursor() == cursor) {
                    primaryScene.setCursor(cursors.pop());
                }
                cursors.remove(cursor);
            }
        }
    }

    /* ******************* FXML ********************* */

    @SuppressWarnings("SameParameterValue")
    private Parent loadFXML(String fxml) {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        try {
            return fxmlLoader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /* ******************* Getters/Setters ********************* */

    public ContentPane getContentPane() {
        return contentPane;
    }

    public void setContentPane(ContentPane contentPane) {
        this.contentPane = contentPane;
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public PatternList getPatternList() {
        return patternList;
    }

    public void setPatternList(PatternList patternList) {
        this.patternList = patternList;
    }

    public void setPrimaryController(PrimaryController primaryController) {
        this.primaryController = primaryController;
    }

    public PrimaryController getPrimaryController() {
        return primaryController;
    }
}
