package de.julianhofmann.ui;

import de.julianhofmann.App;
import de.julianhofmann.Loop;
import de.julianhofmann.world.World;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class PrimaryController implements Initializable {

    @FXML private CheckMenuItem themeMenuItem;
    @FXML private Button playButton, pauseButton, stopButton;
    @FXML private HBox toolBar;
    @FXML private HBox leftPaneToolBar;
    @FXML private Pane canvasPane;
    @FXML private Canvas canvas;
    @FXML private MenuItem saveMenuItem, saveSelectionMenuItem;
    @FXML private MenuItem undoMenuItem, redoMenuItem, copyMenuItem, cutMenuItem, pasteMenuItem, deleteMenuItem;
    @FXML private MenuItem playMenuItem, pauseMenuItem, stopMenuItem, fasterMenuItem, resetSpeedMenuItem;
    @FXML private MenuItem zoomInMenuItem, zoomOutMenuItem, resetZoomMenuItem, resetCameraMenuItem;
    @FXML private Text fileNameLabel;
    @FXML private BorderPane leftPane;
    @FXML private Text delayIndicator, zoomIndicator;
    @FXML private Button deleteTreeItemButton;

    private ImageView playIcon, playIconDisabled, pauseIcon, pauseIconDisabled, stopIcon, stopIconDisabled, deleteIcon, deleteIconDisabled;

    /* ******************** Initialization *************************** */

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        App.ui.setContentPane(new ContentPane(canvasPane, canvas));
        App.ui.setPatternList(new PatternList(leftPane));

        initToolBar();
        initFileMenu();
        initEditMenu();
        initSimulationMenu();
        initViewMenu();
        initStatusBar();

        updateColors(App.settings.isDarkTheme());
        App.ui.setPrimaryController(this);
    }

    private void initToolBar() {
        App.world.filePathProperty().addListener((p, o, n) -> updateFileNameLabel());
        App.world.savedProperty().addListener((p, o, n) -> updateFileNameLabel());

        playIcon = new ImageView(new Image(new File("rsc/icons/play_btn.png").toURI().toString()));
        playIconDisabled = new ImageView(new Image(new File("rsc/icons/play_btn_disabled.png").toURI().toString()));
        playIcon.setFitHeight(15);
        playIcon.setPreserveRatio(true);
        playIconDisabled.setFitHeight(15);
        playIconDisabled.setPreserveRatio(true);

        playButton.setGraphic(playIcon);

        pauseIcon = new ImageView(new Image(new File("rsc/icons/pause_btn.png").toURI().toString()));
        pauseIconDisabled = new ImageView(new Image(new File("rsc/icons/pause_btn_disabled.png").toURI().toString()));
        pauseIcon.setFitHeight(15);
        pauseIcon.setPreserveRatio(true);
        pauseIconDisabled.setFitHeight(15);
        pauseIconDisabled.setPreserveRatio(true);

        pauseButton.setGraphic(pauseIcon);

        stopIcon = new ImageView(new Image(new File("rsc/icons/stop_btn.png").toURI().toString()));
        stopIconDisabled = new ImageView(new Image(new File("rsc/icons/stop_btn_disabled.png").toURI().toString()));
        stopIcon.setFitHeight(15);
        stopIcon.setPreserveRatio(true);
        stopIconDisabled.setFitHeight(15);
        stopIconDisabled.setPreserveRatio(true);

        stopButton.setGraphic(stopIcon);


        deleteIcon = new ImageView(new Image(new File("rsc/icons/delete_btn.png").toURI().toString()));
        deleteIconDisabled = new ImageView(new Image(new File("rsc/icons/delete_btn_disabled.png").toURI().toString()));
        deleteIcon.setFitHeight(18);
        deleteIcon.setPreserveRatio(true);
        deleteIconDisabled.setFitHeight(18);
        deleteIconDisabled.setPreserveRatio(true);

        stopButton.setGraphic(deleteIcon);

        App.world.stateProperty().addListener((p, o, n) -> updateToolBarButtons());

        App.settings.darkThemeProperty().addListener((p, o, n) -> updateColors(n));

        updateFileNameLabel();
        updateToolBarButtons();
    }

    private void initFileMenu() {
        App.world.savedProperty().addListener((p, o, n) -> updateFileMenuItems());
        App.ui.getContentPane().getSelectionManager().selectingProperty().addListener((p, o, n) -> updateFileMenuItems());
        updateFileMenuItems();
    }

    private void initEditMenu() {
        App.world.getUndoManager().undoAvailableProperty().addListener((p, o, n) -> updateEditMenuItems());
        App.world.getUndoManager().redoAvailableProperty().addListener((p, o, n) -> updateEditMenuItems());
        App.ui.getContentPane().getSelectionManager().selectingProperty().addListener((p, o, n) -> updateEditMenuItems());
        App.ui.getContentPane().getSelectionManager().clipboardEmptyProperty().addListener((p, o, n) -> updateEditMenuItems());
        App.world.stateProperty().addListener((p, o, n) -> updateEditMenuItems());
        updateEditMenuItems();
    }

    private void initSimulationMenu() {
        App.world.stateProperty().addListener((p, o, n) -> updateSimulationMenuItems());
        App.loop.updateDelayProperty().addListener((p, o, n) -> updateSimulationMenuItems());
        App.loop.actualUpdateDelayProperty().addListener((p, o, n) -> updateSimulationMenuItems());
        updateSimulationMenuItems();
    }

    private void initViewMenu() {
        themeMenuItem.selectedProperty().bindBidirectional(App.settings.darkThemeProperty());
        App.world.cameraXProperty().addListener((p, o, n) -> updateViewMenuItems());
        App.world.cameraYProperty().addListener((p, o, n) -> updateViewMenuItems());
        App.world.cellSizeProperty().addListener((p, o, n) -> updateViewMenuItems());
        updateViewMenuItems();
    }

    private void initStatusBar() {
        App.loop.updateDelayProperty().addListener((p, o, n) -> updateStatusBar());
        App.loop.actualUpdateDelayProperty().addListener((p, o, n) -> updateStatusBar());
        App.world.cellSizeProperty().addListener((p, o, n) -> updateStatusBar());
        updateStatusBar();
    }

    /* ************************ ToolBar Action Methods ************************** */

    @FXML
    private void play() {
        App.world.setState(World.RUNNING);
    }

    @FXML
    private void pause() {
        App.world.setState(World.PAUSED);
    }

    @FXML
    private void stop() {
        App.world.setState(World.STOPPED);
    }

    @FXML
    private void deleteTreeItem() {
        App.ui.getPatternList().deletePattern();
    }

    /* ************************ Menu Action Methods ************************** */

    /* -------------------- File Menu -------------------------- */

    @FXML
    private void newFile() {
        App.world.newFile();
    }

    @FXML
    private void openFile() {
        App.ui.open();
    }

    @FXML
    private void saveFile() {
        App.ui.save();
    }

    @FXML
    private void saveFileAs() {
        App.ui.saveAs();
    }

    @FXML
    private void saveSelection() {
        if (App.ui.getContentPane().getSelectionManager().isSelecting()) {
            String name = App.ui.inputDialog("Auswahl speichern", "Name:", "");
            if (name.isBlank()) {
                return;
            }
            if (!App.ui.getPatternList().getPatternManager().nameExists(name)) {
                String category = App.ui.inputDialog("Auswahl speichern", "Kategorie:", "");
                if (!category.isBlank()) {
                    if (App.ui.getPatternList().getPatternManager().savePattern(name, category, App.ui.getContentPane().getSelectionManager().toJson(name, category))) {
                        App.ui.getPatternList().getPatternManager().refreshPatterns();
                    } else {
                        App.ui.alert(Alert.AlertType.ERROR, "Fehler", "Das Muster konnte nicht gespeichert werden!");
                    }
                }
            } else {
                App.ui.alert(Alert.AlertType.ERROR, "Fehler", "Der Name existiert bereits!");
            }
        }
    }

    @FXML
    private void exit() {
        App.exit();
    }

    /* -------------------- Edit Menu -------------------------- */

    @FXML
    private void undo() {
        App.world.getUndoManager().undo();
    }

    @FXML
    private void redo() {
        App.world.getUndoManager().redo();
    }

    @FXML
    private void copy() {
        App.ui.getContentPane().getSelectionManager().copy();
    }

    @FXML
    private void cut() {
        App.ui.getContentPane().getSelectionManager().cut();
    }

    @FXML
    private void paste() {
        App.ui.getContentPane().getSelectionManager().paste();
    }

    @FXML
    private void delete() {
        App.ui.getContentPane().getSelectionManager().delete();
    }

    /* -------------------- Simulation Menu -------------------------- */

    @FXML
    private void speedUpSimulation() {
        App.loop.addUpdateDelay(-10);
    }

    @FXML
    private void slowDownSimulation() {
        App.loop.addUpdateDelay(10);
    }

    @FXML
    private void changeSimulationDelay() {
        String input = App.ui.inputDialog("Verzögerung ändern", "Verzögerung:", Integer.toString(App.loop.getUpdateDelay()));
        if (!input.isBlank()) {
            try {
                int delay = Integer.parseInt(input);
                if (delay >= 0) {
                    App.loop.setUpdateDelay(delay);
                    return;
                }
            } catch (NumberFormatException ignore) { }
        }
        App.ui.alert(Alert.AlertType.ERROR, "Fehler", "Ungültige Verzögerung", ButtonType.OK);
    }

    @FXML
    private void resetSimulationDelay() {
        App.loop.setUpdateDelay(Loop.DEFAULT_UPDATE_DELAY);
    }

    /* -------------------- View Menu -------------------------- */

    @FXML
    private void zoomIn() {
        App.world.addCellSize(App.world.getCellSize() / 10);
    }

    @FXML
    private void zoomOut() {
        App.world.addCellSize(-(App.world.getCellSize() / 10));
    }

    @FXML
    private void resetZoom() {
        App.world.setCellSize(World.DEFAULT_CELL_SIZE);
    }

    @FXML
    private void resetCamera() {
        App.world.setCellSize(World.DEFAULT_CELL_SIZE);
        App.world.setCameraX(World.DEFAULT_CAMERA_X);
        App.world.setCameraY(World.DEFAULT_CAMERA_Y);
    }

    /* ************************ Update Methods ************************** */

    private void updateColors(boolean darkTheme) {
        if (darkTheme) {
            toolBar.setStyle("-fx-background-color: #353535");
            leftPaneToolBar.setStyle("-fx-background-color: #404040");
            playButton.setStyle("-fx-border-width: 0; -fx-border-color: none; -fx-background-color: #353535");
            pauseButton.setStyle("-fx-border-width: 0; -fx-border-color: none; -fx-background-color: #353535");
            stopButton.setStyle("-fx-border-width: 0; -fx-border-color: none; -fx-background-color: #353535");
            deleteTreeItemButton.setStyle("-fx-border-width: 0; -fx-border-color: none; -fx-background-color: #404040");
        } else {
            toolBar.setStyle("-fx-border-width: 0; -fx-border-color: none; -fx-background-color: #E0E0E0");
            leftPaneToolBar.setStyle("-fx-border-width: 0; -fx-border-color: none; -fx-background-color: #EAEAEA");
            playButton.setStyle("-fx-border-width: 0; -fx-border-color: none; -fx-background-color: #E0E0E0");
            pauseButton.setStyle("-fx-border-width: 0; -fx-border-color: none; -fx-background-color: #E0E0E0");
            stopButton.setStyle("-fx-border-width: 0; -fx-border-color: none; -fx-background-color: #E0E0E0");
            deleteTreeItemButton.setStyle("-fx-border-width: 0; -fx-border-color: none; -fx-background-color: #EAEAEA");
        }
    }

    public void updateToolBarButtons() {
        playButton.setDisable(App.world.getState() == World.RUNNING);
        playButton.setGraphic(playButton.isDisable() ? playIconDisabled : playIcon);

        pauseButton.setDisable(App.world.getState() != World.RUNNING);
        pauseButton.setGraphic(pauseButton.isDisable() ? pauseIconDisabled : pauseIcon);

        stopButton.setDisable(App.world.getState() == World.STOPPED);
        stopButton.setGraphic(stopButton.isDisable() ? stopIconDisabled : stopIcon);

        deleteTreeItemButton.setDisable(App.ui.getPatternList().getTreeView().getSelectionModel().getSelectedItem() == null);
        deleteTreeItemButton.setGraphic(deleteTreeItemButton.isDisable() ? deleteIconDisabled : deleteIcon);
    }

    private void updateFileNameLabel() {
        if (App.world.getFilePath() != null) {
            String[] temp = App.world.getFilePath().split(System.getProperty("file.separator"));
            fileNameLabel.setText(">> " + temp[temp.length-1] + (App.world.isSaved() ? "" : "*"));
        } else {
            fileNameLabel.setText(">> Unbenannt.gol"+(App.world.isSaved() ? "" : "*"));
        }
    }

    private void updateFileMenuItems() {
        saveMenuItem.setDisable(App.world.isSaved());
        saveSelectionMenuItem.setDisable(!App.ui.getContentPane().getSelectionManager().isSelecting());
    }

    private void updateEditMenuItems() {
        undoMenuItem.setDisable(App.world.getState() != World.STOPPED || !App.world.getUndoManager().isUndoAvailable());
        redoMenuItem.setDisable(App.world.getState() != World.STOPPED || !App.world.getUndoManager().isRedoAvailable());
        copyMenuItem.setDisable(App.world.getState() == World.RUNNING || !App.ui.getContentPane().getSelectionManager().isSelecting());
        cutMenuItem.setDisable(App.world.getState() == World.RUNNING || !App.ui.getContentPane().getSelectionManager().isSelecting());
        pasteMenuItem.setDisable(App.world.getState() == World.RUNNING || App.ui.getContentPane().getSelectionManager().isClipboardEmpty());
        deleteMenuItem.setDisable(App.world.getState() == World.RUNNING || !App.ui.getContentPane().getSelectionManager().isSelecting());
    }

    private void updateSimulationMenuItems() {
        playMenuItem.setDisable(App.world.getState() == World.RUNNING);
        pauseMenuItem.setDisable(App.world.getState() != World.RUNNING);
        stopMenuItem.setDisable(App.world.getState() == World.STOPPED);

        fasterMenuItem.setDisable(App.loop.getUpdateDelay() == 0);
        resetSpeedMenuItem.setDisable(App.loop.getUpdateDelay() == Loop.DEFAULT_UPDATE_DELAY);
    }

    private void updateViewMenuItems() {
        resetCameraMenuItem.setDisable(App.world.getCameraX() == World.DEFAULT_CAMERA_X && App.world.getCameraY() == World.DEFAULT_CAMERA_Y && App.world.getCellSize() == World.DEFAULT_CELL_SIZE);
        resetZoomMenuItem.setDisable(App.world.getCellSize() == World.DEFAULT_CELL_SIZE);
        zoomOutMenuItem.setDisable(App.world.getCellSize() == World.MIN_CELL_SIZE);
        zoomInMenuItem.setDisable(App.world.getCellSize() == World.MAX_CELL_SIZE);
    }

    private void updateStatusBar() {
        delayIndicator.setText(App.loop.getActualUpdateDelay() + "ms/" + App.loop.getUpdateDelay() + "ms");
        zoomIndicator.setText(Math.round((App.world.getCellSize() * 10)) + "%");
    }
}
