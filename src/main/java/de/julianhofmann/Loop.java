package de.julianhofmann;

import de.julianhofmann.ui.Renderer;
import javafx.animation.AnimationTimer;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

public class Loop {
    public static final int DEFAULT_UPDATE_DELAY = 100;
    private static final int PATTERN_LIST_REFRESH_DELAY = 5000;
    private final AnimationTimer drawTimer;
    private final Thread updateThread, updatePatternListThread;
    private boolean running;
    private final IntegerProperty updateDelay = new SimpleIntegerProperty(DEFAULT_UPDATE_DELAY);
    private final IntegerProperty actualUpdateDelay = new SimpleIntegerProperty(updateDelay.get());
    private boolean finished;
    private int drawTimerIterations;
    private long deltaMillisCombined;

    @SuppressWarnings("BusyWait")
    public Loop() {
        drawTimer = new AnimationTimer() {
            @Override
            public void handle(long timestamp) {
                long startNanos = System.nanoTime();
                if (App.ui.getPatternList().getPatternManager().isChanged()) {
                    App.ui.getPatternList().getPatternManager().setChanged(false);
                    App.ui.getPatternList().refresh();
                }

                Renderer renderer = App.ui.getContentPane().getRenderer();

                if (!finished) {
                    boolean firstRound = renderer.getDrawCells().isEmpty();
                    if (firstRound) {
                        renderer.collectCells();
                    }

                    finished = renderer.render(firstRound);
                }

                renderer.snapshot();

                long deltaMillis = Math.round((System.nanoTime() - startNanos) / 1000000d);
                deltaMillisCombined += deltaMillis;
                drawTimerIterations ++;

                if (finished) {
                    long averageDeltaMillis = deltaMillisCombined / drawTimerIterations;
                    int newMaxDrawCalls = (int) Math.round(((double) renderer.getMaxDrawCalls() / averageDeltaMillis * 15));
                    if (newMaxDrawCalls > 10000 && newMaxDrawCalls < 1000000) {
                        if (!(averageDeltaMillis < 15 && newMaxDrawCalls < renderer.getMaxDrawCalls())) {
                            renderer.setMaxDrawCalls(newMaxDrawCalls);
                        }
                    }

                    drawTimerIterations = 0;
                    deltaMillisCombined = 0;

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

        updatePatternListThread = new Thread(() -> {
            while (running) {
                App.ui.getPatternList().getPatternManager().refreshPatterns();
                try {
                    Thread.sleep(PATTERN_LIST_REFRESH_DELAY);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void start() {
        running = true;
        drawTimer.start();
        updateThread.start();
        updatePatternListThread.start();
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