package de.julianhofmann;

import de.julianhofmann.ui.UI;
import de.julianhofmann.util.Settings;
import de.julianhofmann.world.World;
import javafx.application.Application;
import javafx.stage.Stage;

import java.io.File;

public class App extends Application {

    public static Settings settings;
    public static UI ui;
    public static World world;
    public static Loop loop;

    @Override
    public void start(Stage primaryStage) {
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
        settings = new Settings();
        settings.load();

        world = new World();

        if (args.length > 0) {
            System.err.println("Opening '" + args[0] + "'...");
            if (!new File(args[0]).exists()) {
                System.err.println("Couldn't open '" + args[0] + "': No such file");
                System.exit(1);
            }
            if (!args[0].toLowerCase().endsWith(".gol")) {
                System.err.println("Couldn't open '" + args[0] + "': Invalid file type");
                System.exit(1);
            }
            world.load(args[0]);
        } else {
            System.err.println("No file provided. Opening new empty world...");
        }

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