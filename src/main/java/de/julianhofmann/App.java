package de.julianhofmann;

import de.julianhofmann.ui.UI;
import de.julianhofmann.util.Settings;
import de.julianhofmann.world.World;
import javafx.application.Application;
import javafx.stage.Stage;

public class App extends Application {

    public static Settings settings;
    public static UI ui;
    public static World world;
    public static Loop loop;

    @Override
    public void start(Stage primaryStage) {
        settings = new Settings();
        settings.load();

        world = new World();

        loop = new Loop();

        ui = new UI(primaryStage);
        ui.init();

        loop.start();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        exit();
    }

    public static void main(String[] args) {
        launch(args);
    }

    public static void exit() {
        world.setState(World.STOPPED);
        ui.getPrimaryStage().close();
        loop.stop();
        settings.save();
        System.exit(0);
    }
}