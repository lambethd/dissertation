package com.dl561.simulation;

import com.dl561.rest.service.ISimulationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Timer;
import java.util.TimerTask;

@Service
public class Tick {

    private final ISimulationService simulationService;
    public static final int TICKS_PER_SECOND = 100;
    private static final int ONE_SECOND = 1000;
    public static final int TICK_TIME = ONE_SECOND / TICKS_PER_SECOND;
    public static final double SECONDS_PER_TICK = (double) TICKS_PER_SECOND / (double) ONE_SECOND;
    private static long previousTickTime;
    private static int tickCount = 0;
    private static long tickWaypoint = -1;
    private static int tickCounter = 0;

    @Value("${tick.message.debug}")
    private boolean showDebug;

    @Value("${tick.message.summary}")
    private boolean showSummary;

    @Autowired
    public Tick(ISimulationService simulationService) {
        this.simulationService = simulationService;
    }

    @PostConstruct
    public void beginTick() {
        Timer timer = new Timer();
        TimerTask tickTask = new TickTask();
        timer.scheduleAtFixedRate(tickTask, 1000, TICK_TIME);
    }

    private void tick() {
        tickCount++;
        tickCounter++;
        long beforeTime = System.currentTimeMillis();
        simulationService.doTick();
        long afterTime = System.currentTimeMillis();
        long betweenTime = afterTime - beforeTime;
        if (showDebug) {
            System.out.println("Tick took " + betweenTime + "ms with a gap of " + (afterTime - previousTickTime) + "ms");
        }
        previousTickTime = System.currentTimeMillis();
        if (previousTickTime > tickWaypoint + 1000) {
            System.out.println(tickCounter + " ticks in the last " + (previousTickTime - tickWaypoint) + "ms.");
            tickWaypoint = previousTickTime;
            tickCounter = 0;
        }
    }

    private class TickTask extends TimerTask {

        public TickTask() {
            tickWaypoint = System.currentTimeMillis();
        }

        @Override
        public void run() {
            tick();
        }
    }

    private class TickCountTask extends TimerTask {
        @Override
        public void run() {
            if (showSummary) {
                System.out.println(tickCount + " ticks per second");
            }
            tickCount = 0;
        }
    }

    public static int getTickCount() {
        return tickCount;
    }
}
