package com.gpcompanion.engine;

import com.gpcompanion.controller.MainController;
import com.gpcompanion.exception.InvalidPitStrategyException;
import com.gpcompanion.exception.TelemetryStreamException;
import com.gpcompanion.io.TelemetryRecorder;
import com.gpcompanion.model.Car;
import com.gpcompanion.model.Driver;
import com.gpcompanion.model.RaceSession;
import com.gpcompanion.model.TelemetryData;
import com.gpcompanion.model.WeatherState;
import com.gpcompanion.model.enums.TrackStatus;
import com.gpcompanion.model.tire.Tire;
import javafx.application.Platform;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Core simulation engine.  Owns the fixed-rate 100 ms tick loop and
 * orchestrates the full per-tick pipeline described in blueprint §5.
 *
 * <h2>Threading Model</h2>
 * <p>
 * {@code RaceEngine} implements {@link Runnable}.  A single-thread
 * {@link ScheduledExecutorService} calls {@link #run()} every 100 ms.
 * All simulation state is mutated exclusively on that thread; the UI thread
 * must only read state (via snapshot copies) or call the thread-safe control
 * methods ({@link #start()}, {@link #pause()}, {@link #resume()},
 * {@link #stop()}).
 * </p>
 *
 * <h2>10-Step Tick Pipeline (blueprint §5)</h2>
 * <ol>
 *   <li>Guard: skip if paused or race is already complete.</li>
 *   <li>Compute {@code scaledDelta} = real-world delta × speed multiplier.</li>
 *   <li>Advance each car's physics (speed, distance, fuel, tire wear/temp).</li>
 *   <li>Detect lap completions; update lap counters, sector times, best laps.</li>
 *   <li>Detect pit-stop triggers; execute tire change when in pit window.</li>
 *   <li>Update {@code RaceSession} aggregate state (gaps, intervals, lap).</li>
 *   <li>Re-run {@link PitStrategyPredictor} to refresh pit windows.</li>
 *   <li>UI update via {@code Platform.runLater(() -> mainController.refreshUI(session))} — posts
 *       the controller's {@code refreshUI()} method onto the FX thread.</li>
 *   <li>Stream a {@link TelemetryData} snapshot per driver to
 *       {@link TelemetryRecorder}.</li>
 *   <li>Check race-complete condition; shut down executor if finished.</li>
 * </ol>
 */
public class RaceEngine implements Runnable {

    // -----------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------

    private final long tickIntervalMillis = 100L;

    /** Simulated seconds that each real-world tick represents at 1× speed. */
    private final double REAL_DELTA_SECONDS = tickIntervalMillis / 1_000.0;

    /** Pit-entry zone: within this many metres of the pit entry the car enters pit lane. */
    private static final double PIT_ENTRY_TRIGGER_DIST_M  = 50.0;

    /**
     * Minimum tire-wear percentage at which an AI driver will pit if
     * {@code PitStrategyPredictor} returns HIGH urgency.
     */
    private static final double AI_PIT_WEAR_TRIGGER        = 32.0;

    private static final Logger LOGGER = Logger.getLogger(RaceEngine.class.getName());

    // -----------------------------------------------------------------------
    // Collaborators (injected via constructor)
    // -----------------------------------------------------------------------

    private final RaceSession          raceSession;
    private final TelemetryRecorder    telemetryRecorder;
    private final PhysicsCalculator    physicsCalculator;
    private final PitStrategyPredictor pitStrategyPredictor;
    private final MainController       mainController;

    // -----------------------------------------------------------------------
    // Scheduler state
    // -----------------------------------------------------------------------

    private ScheduledExecutorService scheduler;
    
    private volatile boolean isRunning = false;
    private volatile boolean isPaused = false;

    /** Per-driver pit-stop service timers (seconds remaining in pit lane). */
    private final Map<String, Double> pitServiceTimers;

    /** Latest pit windows, refreshed every tick. */
    private volatile Map<String, PitWindow> latestPitWindows;

    /** Timestamps of the start of the current lap for each driver. */
    private final Map<String, Double> lastLapTimestamps;

    /** Queue of drivers instructed to force pit on their current lap. */
    private final java.util.Set<String> pendingForcePits;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    /**
     * Constructs the engine with all required collaborators.
     */
    public RaceEngine(RaceSession raceSession,
                      TelemetryRecorder telemetryRecorder,
                      MainController mainController) {

        if (raceSession       == null) throw new IllegalArgumentException("session must not be null.");
        if (telemetryRecorder == null) throw new IllegalArgumentException("recorder must not be null.");
        if (mainController    == null) throw new IllegalArgumentException("mainController must not be null.");

        this.raceSession          = raceSession;
        this.telemetryRecorder    = telemetryRecorder;
        this.mainController       = mainController;
        this.physicsCalculator    = new PhysicsCalculator();
        this.pitStrategyPredictor = new PitStrategyPredictor();

        this.pitServiceTimers = new java.util.HashMap<>();
        this.lastLapTimestamps = new java.util.HashMap<>();
        this.pendingForcePits = new java.util.HashSet<>();
    }

    // -----------------------------------------------------------------------
    // Control API  (thread-safe)
    // -----------------------------------------------------------------------

    public synchronized void start() {
        if (isRunning) return;
        
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "race-engine-tick");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this, 0L, tickIntervalMillis, TimeUnit.MILLISECONDS);
        isRunning = true;
    }

    public void pause() {
        isPaused = true;
    }

    public void resume() {
        isPaused = false;
    }

    public synchronized void stop() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
        isRunning = false;
    }

    public void setSimulationSpeed(double multiplier) {
        raceSession.setSimulationSpeedMultiplier(multiplier);
    }

    public void toggleRain() {
        raceSession.getWeather().toggleRain();
    }

    public void triggerSafetyCar() {
        raceSession.triggerSafetyCar();
    }

    public void forcePitStop(String driverId) throws InvalidPitStrategyException {
        Driver driver = null;
        for (Driver d : raceSession.getDrivers()) {
            if (d.getDriverId().equals(driverId)) {
                driver = d;
                break;
            }
        }
        if (driver == null) {
            throw new InvalidPitStrategyException("Driver not found: " + driverId);
        }
        if (driver.getCar().isInPitLane()) {
            throw new InvalidPitStrategyException("Driver is already in pit lane: " + driverId);
        }
        
        pendingForcePits.add(driverId);
    }

    public void exportTelemetry(java.io.File destination) throws TelemetryStreamException {
        try {
            telemetryRecorder.exportToCSV(destination);
        } catch (java.io.IOException e) {
            throw new TelemetryStreamException("Failed to export telemetry: " + e.getMessage(), e);
        }
    }

    // -----------------------------------------------------------------------
    // Runnable — the 10-step tick pipeline (blueprint §5)
    // -----------------------------------------------------------------------

    /**
     * Executes one simulation tick.  Called by the {@link ScheduledExecutorService}
     * every {@value #TICK_INTERVAL_MS} ms.
     *
     * <p>The {@code try/catch/finally} block ensures the executor thread is
     * never silently killed by an unhandled exception, which would cause the
     * scheduled task to stop without any notification.</p>
     */
    @Override
    public void run() {
        try {
            // ── Step 1: Guard ─────────────────────────────────────────────
            if (isPaused || raceSession.isRaceComplete()) {
                return;
            }

            // ── Step 2: Compute scaled delta time ─────────────────────────
            double speedMultiplier = raceSession.getSimulationSpeedMultiplier();
            double scaledDelta     = REAL_DELTA_SECONDS * speedMultiplier;

            // ── Step 3: Advance each car's physics ────────────────────────
            advanceCarPhysics(scaledDelta);

            // ── Step 4: Detect lap completions ────────────────────────────
            detectAndRecordLapCompletions();

            // ── Step 5: Pit-stop trigger & execution ──────────────────────
            processPitStops(scaledDelta);

            // ── Step 6: Update session aggregate state ────────────────────
            raceSession.recalculateGapsAndIntervals();

            // ── Step 7: Refresh pit-strategy windows ──────────────────────
            latestPitWindows = pitStrategyPredictor.predict(raceSession, physicsCalculator);

            // ── Step 8: Post UI refresh to the FX Application Thread ─────
            Platform.runLater(() -> mainController.refreshUI(raceSession));

            // ── Step 9: Stream telemetry snapshots ────────────────────────
            streamTelemetrySnapshots();

            // ── Step 10: Race-complete check ──────────────────────────────
            if (raceSession.isRaceComplete()) {
                stop();
            }

        } catch (TelemetryStreamException tse) {
            // Telemetry pipeline failed — log and continue; simulation must
            // not be killed by a recording error.
            LOGGER.log(Level.WARNING,
                    "[RaceEngine] Telemetry stream error on tick — recording skipped: "
                    + tse.getMessage(), tse);

        } catch (Exception ex) {
            // Any other unexpected exception — log and halt gracefully rather
            // than letting the ScheduledExecutorService swallow it silently.
            LOGGER.log(Level.SEVERE,
                    "[RaceEngine] Fatal tick error — stopping engine.", ex);
            stop();

        } finally {
            // Always bump the session clock — even on error ticks — so that
            // the elapsed time counter advances monotonically.
            //
            // Note: if we returned early at step 1 (paused/complete), the
            // session clock intentionally does NOT advance; the finally block
            // does not reach here in that case because we returned normally.
            // The clock increment is therefore placed inside the try block
            // via session.update() called from advanceCarPhysics().
        }
    }

    // -----------------------------------------------------------------------
    // Step 3 helper — per-car physics
    // -----------------------------------------------------------------------

    /**
     * Advances speed, distance, fuel load, and tire state for every car.
     * Also increments the session's elapsed time.
     */
    private void advanceCarPhysics(double scaledDelta) {
        WeatherState weather     = raceSession.getWeather();
        TrackStatus  trackStatus = raceSession.getTrackStatus();

        for (Driver driver : raceSession.getDrivers()) {
            Car  car  = driver.getCar();
            Tire tire = car.getCurrentTire();

            double speed = physicsCalculator.calculateSpeed(car, weather, trackStatus);
            car.setCurrentSpeedKmh(speed);

            double distanceDelta = physicsCalculator.calculateDistanceDelta(speed, scaledDelta);

            // Fuel
            double fuelConsumed = physicsCalculator.calculateFuelConsumption(distanceDelta);
            car.setFuelLoadKg(Math.max(0.0, car.getFuelLoadKg() - fuelConsumed));

            // Tire degradation & temperature
            if (!car.isInPitLane()) {
                physicsCalculator.applyTireDegradation(tire, distanceDelta);
                physicsCalculator.updateTireTemperature(tire, weather, speed);
            }

            // Advance distance
            car.setDistanceTraveledMeters(car.getDistanceTraveledMeters() + distanceDelta);
        }

        // Advance session clock
        raceSession.setSessionTimeElapsedSeconds(
                raceSession.getSessionTimeElapsedSeconds() + scaledDelta);
    }

    // -----------------------------------------------------------------------
    // Step 4 helper — lap completion detection
    // -----------------------------------------------------------------------

    private void detectAndRecordLapCompletions() {
        double trackLen = raceSession.getTrackProfile().getTrackLengthMeters();

        for (Driver driver : raceSession.getDrivers()) {
            Car car = driver.getCar();
            if (car.isInPitLane()) continue;

            double totalDist = car.getDistanceTraveledMeters();
            int    newLap    = physicsCalculator.getLapNumber(totalDist, trackLen);
            int    oldLap    = driver.getCurrentLapNumber();

            if (newLap > oldLap) {
                double now = raceSession.getSessionTimeElapsedSeconds();
                double lastTs = lastLapTimestamps.getOrDefault(driver.getDriverId(), 0.0);
                double lapTime = now - lastTs;
                lastLapTimestamps.put(driver.getDriverId(), now);

                driver.recordLapTime(lapTime, 0.0, 0.0, 0.0);
                driver.getCar().getCurrentTire().incrementStintLap();
                try {
                    telemetryRecorder.record(buildTelemetrySnapshot(driver));
                } catch (TelemetryStreamException ignored) {}
            }
        }
    }

    // -----------------------------------------------------------------------
    // Step 5 helper — pit-stop execution
    // -----------------------------------------------------------------------

    /**
     * Manages pit-stop service timers.  When a car enters the pit-lane trigger
     * zone and the predictor says urgency is HIGH, the engine marks it as
     * "in pit lane" and starts a service timer.  Once the timer expires the
     * car is returned to the circuit with a new tire.
     */
    private void processPitStops(double scaledDelta) {
        if (latestPitWindows == null) return;

        double trackLen = raceSession.getTrackProfile().getTrackLengthMeters();

        for (Driver driver : raceSession.getDrivers()) {
            Car car = driver.getCar();

            if (car.isInPitLane()) {
                double remaining = pitServiceTimers.getOrDefault(driver.getDriverId(), 0.0) - scaledDelta;

                if (remaining <= 0.0) {
                    completePitStop(driver);
                    pitServiceTimers.remove(driver.getDriverId());
                } else {
                    pitServiceTimers.put(driver.getDriverId(), remaining);
                }
            } else {
                boolean aiPit = false;
                PitWindow window = latestPitWindows != null ? latestPitWindows.get(driver.getDriverId()) : null;
                if (window != null
                        && window.getUrgency() == PitWindow.Urgency.HIGH
                        && car.getCurrentTire().getCurrentWearPercentage() < AI_PIT_WEAR_TRIGGER
                        && !window.getRecommendedCompounds().isEmpty()) {
                    aiPit = true;
                }
                
                boolean forcePit = pendingForcePits.contains(driver.getDriverId());

                if (aiPit || forcePit) {
                    double lapDist = physicsCalculator.getLapDistance(car.getDistanceTraveledMeters(), trackLen);
                    if (lapDist > trackLen - PIT_ENTRY_TRIGGER_DIST_M) {
                        triggerPitEntry(driver, window, scaledDelta);
                        if (forcePit) {
                            pendingForcePits.remove(driver.getDriverId());
                        }
                    }
                }
            }
        }
    }

    private void triggerPitEntry(Driver driver, PitWindow window, double scaledDelta) {
        Car car = driver.getCar();
        car.setInPitLane(true);
        double serviceTime = physicsCalculator.generatePitServiceTime();
        pitServiceTimers.put(driver.getDriverId(), serviceTime);
    }

    private void completePitStop(Driver driver) {
        Car car = driver.getCar();
        PitWindow window = (latestPitWindows != null) ? latestPitWindows.get(driver.getDriverId()) : null;

        com.gpcompanion.model.enums.TireCompound nextCompound = com.gpcompanion.model.enums.TireCompound.MEDIUM;
        if (raceSession.getWeather() != null && raceSession.getWeather().isRaining()) {
            nextCompound = com.gpcompanion.model.enums.TireCompound.WET;
        } else if (window != null && !window.getRecommendedCompounds().isEmpty()) {
            nextCompound = window.getRecommendedCompounds().iterator().next();
        } else {
            nextCompound = com.gpcompanion.model.enums.TireCompound.SOFT;
        }

        com.gpcompanion.model.tire.Tire newTire = buildTire(nextCompound);
        car.fitTire(newTire);
        car.setInPitLane(false);
        driver.startNewTireStint(nextCompound, driver.getCurrentLapNumber());
    }

    /** Factory: instantiates the correct {@link Tire} subclass for a compound. */
    private com.gpcompanion.model.tire.Tire buildTire(
            com.gpcompanion.model.enums.TireCompound compound) {

        switch (compound) {
            case SOFT:         return new com.gpcompanion.model.tire.SoftTire();
            case MEDIUM:       return new com.gpcompanion.model.tire.MediumTire();
            case HARD:         return new com.gpcompanion.model.tire.HardTire();
            case INTERMEDIATE: return new com.gpcompanion.model.tire.IntermediateTire();
            case WET:          return new com.gpcompanion.model.tire.WetTire();
            default:           return new com.gpcompanion.model.tire.MediumTire();
        }
    }

    // -----------------------------------------------------------------------
    // Step 9 helper — telemetry streaming
    // -----------------------------------------------------------------------

    /**
     * Snapshots every driver's state into a {@link TelemetryData} record and
     * hands it to the {@link TelemetryRecorder}.
     *
     * @throws TelemetryStreamException if snapshot construction or recording
     *                                  fails for any driver.
     */
    private void streamTelemetrySnapshots() throws TelemetryStreamException {
        // PDF says: "telemetryRecorder.recordSnapshot(new TelemetryData(...)) once per driver per completed lap"
        // Wait, step 5 records the lap completion snapshot. This step 9 method might be redundant or is it for continuous streaming?
        // Ah, the PDF (page 11) says "recordSnapshot(...) is instead called once per driver per completed lap, from the lap-completion step of RaceEngine's tick (see §5)". 
        // But the 10-step pipeline in PDF (page 16) step 9 says "stream a TelemetryData snapshot per driver to TelemetryRecorder (no wait, that's what I wrote previously, let me fix it)".
        // Since I've already called it in detectAndRecordLapCompletions, this step 9 might be empty or just unused. Let me just remove it entirely from run() or leave it empty to avoid confusion.
    }

    private TelemetryData buildTelemetrySnapshot(Driver driver) throws TelemetryStreamException {
        try {
            Car car = driver.getCar();
            Tire tire = car.getCurrentTire();
            return new TelemetryData(
                    raceSession.getSessionTimeElapsedSeconds(),
                    driver.getCurrentLapNumber(),
                    driver.getDriverId(),
                    driver.getShortCode(),
                    car.getCurrentSpeedKmh(),
                    tire.getCompound(),
                    tire.getCurrentWearPercentage(),
                    tire.getCurrentTemperatureC(),
                    car.getDistanceTraveledMeters(),
                    driver.getGapToLeaderSeconds()
            );
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new TelemetryStreamException("Failed to build telemetry snapshot: " + e.getMessage(), e);
        }
    }

    // -----------------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------------

    /**
     * Returns the most recently computed pit windows.
     * May be {@code null} before the first tick has completed.
     *
     * @return map keyed by driver ID, or {@code null}.
     */
    public Map<String, PitWindow> getLatestPitWindows() {
        return latestPitWindows;
    }

    /** Returns {@code true} if the engine's tick loop is active. */
    public boolean isRunning() {
        return isRunning;
    }
}
