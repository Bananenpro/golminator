package de.julianhofmann;

import de.julianhofmann.ui.Renderer;
import javafx.animation.AnimationTimer;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

public class Loop {
    public static final int DEFAULT_UPDATE_DELAY = 100;
    private final AnimationTimer drawTimer;
    private final Thread updateThread;
    private boolean running;
    private final IntegerProperty updateDelay = new SimpleIntegerProperty(DEFAULT_UPDATE_DELAY);
    private final IntegerProperty actualUpdateDelay = new SimpleIntegerProperty(updateDelay.get());
    private boolean finished;

    @SuppressWarnings("BusyWait")
    public Loop() {
        drawTimer = new AnimationTimer() {
            @Override
            public void handle(long timestamp) {
                Renderer renderer = App.ui.getContentPane().getRenderer();

                if (!finished) {
                    boolean firstRound = renderer.getDrawCells().isEmpty();
                    if (firstRound) {
                        renderer.collectCells();
                    }

                    finished = renderer.render(firstRound);
                }

                renderer.snapshot();

                if (finished) {
                    App.ui.getContentPane().swapBuffers();
                    App.ui.getContentPane().draw();
                    finished = false;
                }
            }
        };

        updateThread = new Thread(() -> {
            while (running) {
                long startTime = System.nanoTime();
                App.world.update();
                long delay = updateDelay.get() - Math.round((System.nanoTime() - startTime) / 1000000d);
                try {
                    if (delay == 0) delay = 1;
                    if (delay > 0) {
                        Thread.sleep(delay);
                    }
                } catch (InterruptedException e) {
                    App.exit();
                }
                setActualUpdateDelay((int) Math.round((System.nanoTime() - startTime) / 1000000d));
            }
        });
    }

    public void start() {
        running = true;
        drawTimer.start();
        updateThread.start();
    }

    public void stop() {
        drawTimer.stop();
        running = false;
    }

    public int getUpdateDelay() {
        return updateDelay.get();
    }

    public IntegerProperty updateDelayProperty() {
        return updateDelay;
    }

    public void setUpdateDelay(int updateDelay) {
        this.updateDelay.set(updateDelay);
    }

    public int getActualUpdateDelay() {
        return actualUpdateDelay.get();
    }

    public IntegerProperty actualUpdateDelayProperty() {
        return actualUpdateDelay;
    }

    private void setActualUpdateDelay(int actualUpdateDelay) {
        this.actualUpdateDelay.set(actualUpdateDelay);
    }

    public void addUpdateDelay(int amount) {
        updateDelay.set(updateDelay.add(amount).get());
        if (updateDelay.get() < 0) updateDelay.set(0);
    }
}