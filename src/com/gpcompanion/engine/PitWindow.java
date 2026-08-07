package com.gpcompanion.engine;

import com.gpcompanion.model.enums.TireCompound;
import com.gpcompanion.model.enums.WeatherCondition;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Immutable value object that describes a recommended pit-stop opportunity
 * for a single driver.
 *
 * <p>A {@code PitWindow} is produced by {@link PitStrategyPredictor} and
 * consumed by the UI layer (leaderboard tooltip, strategy panel) and by the
 * AI pit-stop trigger inside {@link RaceEngine}.</p>
 *
 * <h2>Fields (all read-only after construction)</h2>
 * <ul>
 *   <li>{@code earliestLap} — the first lap on which a pit stop is optimal.</li>
 *   <li>{@code latestLap}   — the last lap before tire performance collapses.</li>
 *   <li>{@code recommendedCompounds} — ordered set of compounds the predictor
 *       considers viable for the next stint, best first.</li>
 *   <li>{@code urgency}     — qualitative label ({@code LOW / MEDIUM / HIGH})
 *       driven by remaining tire wear and race laps left.</li>
 *   <li>{@code weatherConstraint} — if non-null, the weather condition that
 *       drove the compound recommendation (e.g. LIGHT_RAIN → INTERMEDIATE).</li>
 * </ul>
 */
public final class PitWindow {

    // -----------------------------------------------------------------------
    // Urgency level
    // -----------------------------------------------------------------------

    /** Qualitative urgency of the pit window. */
    public enum Urgency {
        /** Plenty of life left; pit only if strategically advantageous. */
        LOW,
        /** Tire wear is progressing; a pit stop should be planned. */
        MEDIUM,
        /** Tire is critically worn; a pit stop is required within a few laps. */
        HIGH
    }

    // -----------------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------------

    private final String        driverId;
    private final int           earliestLap;
    private final int           latestLap;
    private final Set<TireCompound> recommendedCompounds;
    private final Urgency       urgency;
    private final WeatherCondition weatherConstraint;   // nullable

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    /**
     * Constructs a new {@code PitWindow}.
     *
     * @param driverId             driver this window belongs to.
     * @param earliestLap          earliest viable pit lap.
     * @param latestLap            latest viable pit lap (must be ≥ earliestLap).
     * @param recommendedCompounds ordered set of recommended next compounds;
     *                             must not be null or empty.
     * @param urgency              urgency classification; must not be null.
     * @param weatherConstraint    weather condition that influenced the
     *                             recommendation, or {@code null} for dry.
     * @throws IllegalArgumentException if invariants are violated.
     */
    public PitWindow(String driverId, int earliestLap, int latestLap,
                     Set<TireCompound> recommendedCompounds,
                     Urgency urgency,
                     WeatherCondition weatherConstraint) {

        if (driverId == null || driverId.isBlank()) {
            throw new IllegalArgumentException("driverId must not be null or blank.");
        }
        if (earliestLap < 1) {
            throw new IllegalArgumentException("earliestLap must be >= 1, got: " + earliestLap);
        }
        if (latestLap < earliestLap) {
            throw new IllegalArgumentException(
                    "latestLap (" + latestLap + ") must be >= earliestLap (" + earliestLap + ").");
        }
        if (recommendedCompounds == null || recommendedCompounds.isEmpty()) {
            throw new IllegalArgumentException("recommendedCompounds must not be null or empty.");
        }
        if (urgency == null) {
            throw new IllegalArgumentException("urgency must not be null.");
        }

        this.driverId              = driverId;
        this.earliestLap           = earliestLap;
        this.latestLap             = latestLap;
        this.recommendedCompounds  = Collections.unmodifiableSet(
                                         EnumSet.copyOf(recommendedCompounds));
        this.urgency               = urgency;
        this.weatherConstraint     = weatherConstraint;
    }

    // -----------------------------------------------------------------------
    // Queries
    // -----------------------------------------------------------------------

    /**
     * Returns {@code true} if the supplied lap number falls within this window.
     *
     * @param lap the current race lap.
     * @return {@code true} when {@code earliestLap <= lap <= latestLap}.
     */
    public boolean containsLap(int lap) {
        return lap >= earliestLap && lap <= latestLap;
    }

    /**
     * Returns {@code true} if this window is weather-driven rather than
     * wear-driven.
     */
    public boolean isWeatherDriven() {
        return weatherConstraint != null;
    }

    // -----------------------------------------------------------------------
    // Getters
    // -----------------------------------------------------------------------

    public String getDriverId()                         { return driverId; }
    public int    getEarliestLap()                      { return earliestLap; }
    public int    getLatestLap()                        { return latestLap; }
    public Set<TireCompound> getRecommendedCompounds()  { return recommendedCompounds; }
    public Urgency getUrgency()                         { return urgency; }
    public WeatherCondition getWeatherConstraint()      { return weatherConstraint; }

    // -----------------------------------------------------------------------
    // Object overrides
    // -----------------------------------------------------------------------

    @Override
    public String toString() {
        return String.format("PitWindow{driver=%s, laps=[%d,%d], urgency=%s, compounds=%s%s}",
                driverId, earliestLap, latestLap, urgency, recommendedCompounds,
                weatherConstraint != null ? ", weather=" + weatherConstraint : "");
    }
}
