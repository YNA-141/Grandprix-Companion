package com.gpcompanion.engine;

import com.gpcompanion.engine.PitWindow.Urgency;
import com.gpcompanion.model.Driver;
import com.gpcompanion.model.RaceSession;
import com.gpcompanion.model.WeatherState;
import com.gpcompanion.model.enums.TireCompound;
import com.gpcompanion.model.enums.WeatherCondition;
import com.gpcompanion.model.tire.Tire;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stateless strategy engine that derives a {@link PitWindow} for every driver
 * on every call to {@link #predict(RaceSession, PhysicsCalculator)}.
 *
 * <h2>Algorithm (blueprint §2.5)</h2>
 * <ol>
 *   <li><b>Weather override</b> — if the session is currently raining and a
 *       driver is on a dry compound (Soft/Medium/Hard), issue an immediate
 *       {@code HIGH} urgency window recommending Intermediate or Wet based on
 *       rain intensity.</li>
 *   <li><b>Dry override</b> — if the session is dry and a driver is on a wet
 *       compound (Intermediate/Wet), issue an immediate {@code HIGH} urgency
 *       window recommending the best dry compound available.</li>
 *   <li><b>Wear model</b> — otherwise, compute the remaining stint life from
 *       tire wear percentage and estimate how many laps are left before the
 *       tire falls below the critical-wear threshold (30 %).  The window is
 *       set around that lap, and urgency is graded LOW / MEDIUM / HIGH based
 *       on how close it is.</li>
 * </ol>
 *
 * <h2>Compound Selection Heuristic</h2>
 * <p>For dry conditions the predictor recommends a compound one step harder
 * than the current one (Soft → Medium → Hard → Hard).  For rain it always
 * recommends Intermediate first, Wet if rain is HEAVY.</p>
 */
public class PitStrategyPredictor {

    // -----------------------------------------------------------------------
    // Tuning constants
    // -----------------------------------------------------------------------

    /** Wear % below which urgency escalates to HIGH immediately. */
    private static final double CRITICAL_WEAR_THRESHOLD  = 30.0;

    /** Wear % below which urgency is MEDIUM. */
    private static final double MEDIUM_URGENCY_THRESHOLD = 55.0;

    /**
     * The predictor estimates laps-remaining by assuming a fixed wear-per-lap
     * coefficient derived from stintLapCount vs current wear.
     * If stintLapCount is 0, we fall back to this default laps-per-life value.
     */
    private static final double DEFAULT_LAPS_PER_TIRE_LIFE = 25.0;

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Produces a {@link PitWindow} for every driver in the session.
     *
     * @param session    current race session; must not be null.
     * @param calculator physics helper; used for any future lap-time projections.
     * @return a map keyed by driver ID; never null, never contains null values.
     */
    public Map<String, PitWindow> predict(RaceSession session,
                                          PhysicsCalculator calculator) {

        Map<String, PitWindow> windows = new HashMap<>();
        WeatherState weather   = session.getWeather();
        int totalLaps          = session.getTrackProfile().getTotalLaps();
        int currentLap         = session.getCurrentLap();

        for (Driver driver : session.getDrivers()) {
            PitWindow window = buildWindow(driver, currentLap, totalLaps, weather);
            windows.put(driver.getDriverId(), window);
        }
        return windows;
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private PitWindow buildWindow(Driver driver, int currentLap,
                                   int totalLaps, WeatherState weather) {

        Tire    tire         = driver.getCar().getCurrentTire();
        boolean isRaining    = (weather != null) && weather.isRaining();
        WeatherCondition cond = (weather != null)
                ? weather.getCondition() : WeatherCondition.DRY;

        boolean onDryCompound = isDryCompound(tire.getCompound());
        boolean onWetCompound = !onDryCompound;

        // ── Rule 1: dry compound in rain → immediate pit ───────────────────
        if (isRaining && onDryCompound) {
            Set<TireCompound> rec = (cond == WeatherCondition.HEAVY_RAIN)
                    ? EnumSet.of(TireCompound.WET, TireCompound.INTERMEDIATE)
                    : EnumSet.of(TireCompound.INTERMEDIATE, TireCompound.WET);

            return new PitWindow(driver.getDriverId(),
                    currentLap, Math.min(currentLap + 2, totalLaps),
                    rec, Urgency.HIGH, cond);
        }

        // ── Rule 2: wet compound in dry → immediate pit ────────────────────
        if (!isRaining && onWetCompound) {
            Set<TireCompound> rec = recommendDryCompound(tire.getCompound(),
                                                         driver.getPitStopCount());
            return new PitWindow(driver.getDriverId(),
                    currentLap, Math.min(currentLap + 2, totalLaps),
                    rec, Urgency.HIGH, null);
        }

        // ── Rule 3: wear-based model ───────────────────────────────────────
        double wear        = tire.getCurrentWearPercentage();
        int    stintLaps   = tire.getStintLapCount();

        // Estimate laps-per-full-life from actual stintLap count vs wear loss
        double lapsRemaining;
        if (stintLaps > 0) {
            double wearPerLap = (100.0 - wear) / stintLaps;
            if (wearPerLap > 0) {
                lapsRemaining = (wear - CRITICAL_WEAR_THRESHOLD) / wearPerLap;
            } else {
                lapsRemaining = DEFAULT_LAPS_PER_TIRE_LIFE;
            }
        } else {
            // No laps done yet on this stint; use default life minus current wear loss
            lapsRemaining = DEFAULT_LAPS_PER_TIRE_LIFE * (wear / 100.0);
        }

        int predictedPitLap = currentLap + Math.max(0, (int) Math.floor(lapsRemaining));
        predictedPitLap = Math.min(predictedPitLap, totalLaps);

        // Window: [predicted - 2, predicted + 2], clamped to race bounds
        int earliest = Math.max(currentLap, predictedPitLap - 2);
        int latest   = Math.min(totalLaps, predictedPitLap + 2);

        // Urgency
        Urgency urgency;
        if (wear <= CRITICAL_WEAR_THRESHOLD || lapsRemaining <= 2) {
            urgency = Urgency.HIGH;
        } else if (wear <= MEDIUM_URGENCY_THRESHOLD || lapsRemaining <= 6) {
            urgency = Urgency.MEDIUM;
        } else {
            urgency = Urgency.LOW;
        }

        Set<TireCompound> rec = isRaining
                ? EnumSet.of(TireCompound.INTERMEDIATE, TireCompound.WET)
                : recommendDryCompound(tire.getCompound(), driver.getPitStopCount());

        return new PitWindow(driver.getDriverId(), earliest, latest, rec, urgency, null);
    }

    /**
     * Returns the recommended set of dry compounds for the next stint,
     * stepping one rung harder on the Pirelli ladder.
     */
    private Set<TireCompound> recommendDryCompound(TireCompound current, int pitsDone) {
        switch (current) {
            case SOFT:
                return EnumSet.of(TireCompound.MEDIUM, TireCompound.HARD);
            case MEDIUM:
                return (pitsDone == 0)
                        ? EnumSet.of(TireCompound.SOFT, TireCompound.HARD)
                        : EnumSet.of(TireCompound.HARD, TireCompound.MEDIUM);
            case HARD:
                return EnumSet.of(TireCompound.HARD, TireCompound.MEDIUM);
            case INTERMEDIATE:
            case WET:
            default:
                return EnumSet.of(TireCompound.MEDIUM, TireCompound.SOFT);
        }
    }

    private boolean isDryCompound(TireCompound compound) {
        return compound == TireCompound.SOFT
                || compound == TireCompound.MEDIUM
                || compound == TireCompound.HARD;
    }
}
