package com.gpcompanion.engine;

import com.gpcompanion.model.Car;
import com.gpcompanion.model.TrackProfile;
import com.gpcompanion.model.WeatherState;
import com.gpcompanion.model.enums.TrackStatus;
import com.gpcompanion.model.tire.Tire;

/**
 * Stateless helper that encapsulates all physics formulae used by
 * {@link RaceEngine} during each simulation tick.
 *
 * <h2>Design Notes</h2>
 * <ul>
 *   <li>All methods are pure functions: they read their arguments and return a
 *       result without mutating any object.  Mutation is the caller's
 *       responsibility.</li>
 *   <li>No threading: instantiate once and call from the engine thread only.</li>
 *   <li>Constants are tuned to produce lap times in the 80–120 s range for
 *       a generic 5 km circuit at 1× simulation speed.</li>
 * </ul>
 */
public class PhysicsCalculator {

    // -----------------------------------------------------------------------
    // Tuning constants (blueprint §2.5)
    // -----------------------------------------------------------------------

    /** Fuel mass flow penalty: km/h speed loss per kg of fuel on board. */
    private static final double FUEL_DRAG_KMH_PER_KG      = 0.035;

    /** Fuel burn rate: kg consumed per metre travelled. */
    private static final double FUEL_BURN_KG_PER_METER     = 0.000_22;

    /** Safety-car speed cap (km/h). */
    private static final double SAFETY_CAR_SPEED_KMH       = 80.0;

    /** VSC (Virtual Safety Car) speed cap (km/h). */
    private static final double VSC_SPEED_KMH              = 60.0;

    /**
     * Pit-lane traversal speed (km/h).
     * Cars in the pit lane travel at this fixed speed; their lap timer is
     * not stopped until the engine decides the pit stop is complete.
     */
    private static final double PIT_LANE_SPEED_KMH         = 60.0;

    /** Time penalty added to pit-stop service time for tire change (seconds). */
    private static final double PIT_SERVICE_BASE_SECONDS   = 2.5;

    /** Random variation window added to base pit service time (seconds, ±). */
    private static final double PIT_SERVICE_JITTER_SECONDS = 0.6;

    // -----------------------------------------------------------------------
    // Speed calculation
    // -----------------------------------------------------------------------

    /**
     * Computes the effective speed (km/h) of a car for this tick.
     *
     * <p>The pipeline is:
     * <ol>
     *   <li>Start from {@code car.getBaseVelocityKmh()}.</li>
     *   <li>Scale by the tire grip coefficient (grip reduces from 1.0 at full
     *       wear to a non-linear lower bound).</li>
     *   <li>Apply a fuel-load drag penalty.</li>
     *   <li>Apply a weather slow-down if it is raining and the tire is a dry
     *       compound (handled implicitly via the grip value passed by the
     *       caller).</li>
     *   <li>Cap to the track-status speed limit.</li>
     *   <li>Cap to pit-lane speed if the car is in the pit lane.</li>
     * </ol>
     * </p>
     *
     * @param car         the car to calculate speed for.
     * @param weather     current weather state.
     * @param trackStatus current track status (CLEAR / SC / VSC).
     * @return effective speed in km/h, always ≥ 0.
     */
    public double calculateSpeed(Car car, WeatherState weather, TrackStatus trackStatus) {
        if (car.isInPitLane()) {
            return PIT_LANE_SPEED_KMH;
        }

        Tire tire = car.getCurrentTire();
        boolean raining = (weather != null) && weather.isRaining();
        double trackTempC = (weather != null) ? weather.getTrackTemperatureC() : 35.0;

        double grip = tire.calculateGrip(raining, trackTempC);          // [0, ~1]

        // Fuel drag
        double fuelPenalty = car.getFuelLoadKg() * FUEL_DRAG_KMH_PER_KG;
        double variance = (Math.random() - 0.5) * 2.0; // Random [-1.0, 1.0] km/h jitter
        double speed = (car.getBaseVelocityKmh() * grip) - fuelPenalty + variance;

        // Track-status cap
        switch (trackStatus) {
            case SAFETY_CAR:
                speed = Math.min(speed, SAFETY_CAR_SPEED_KMH);
                break;
            case VSC:
                speed = Math.min(speed, VSC_SPEED_KMH);
                break;
            case TRACK_CLEAR:
            default:
                break;
        }

        return Math.max(0.0, speed);
    }

    // -----------------------------------------------------------------------
    // Distance & fuel
    // -----------------------------------------------------------------------

    /**
     * Converts a speed and elapsed wall-clock delta into a distance increment.
     *
     * @param speedKmh  effective speed (km/h).
     * @param deltaTime elapsed time in seconds.
     * @return distance covered in metres.
     */
    public double calculateDistanceDelta(double speedKmh, double deltaTime) {
        // km/h → m/s : divide by 3.6
        return (speedKmh / 3.6) * deltaTime;
    }

    /**
     * Computes the fuel mass consumed for a given distance increment.
     *
     * @param distanceMeters metres travelled this tick.
     * @return fuel consumed in kg.
     */
    public double calculateFuelConsumption(double distanceMeters) {
        return FUEL_BURN_KG_PER_METER * distanceMeters;
    }

    // -----------------------------------------------------------------------
    // Tire physics
    // -----------------------------------------------------------------------

    /**
     * Computes tire degradation for a tick.  Delegates to
     * {@link Tire#degrade(double)} which already contains the compound-
     * specific wear-rate constant.
     *
     * @param tire            the tire to query.
     * @param distanceMeters  metres covered this tick.
     */
    public void applyTireDegradation(Tire tire, double distanceMeters) {
        tire.degrade(distanceMeters);
    }

    /**
     * Updates tire temperature using the ambient track temperature and the
     * car's current speed.
     *
     * @param tire       the tire to update.
     * @param weather    current weather state.
     * @param speedKmh   current car speed (km/h).
     */
    public void updateTireTemperature(Tire tire, WeatherState weather, double speedKmh) {
        double trackTempC = (weather != null) ? weather.getTrackTemperatureC() : 35.0;
        tire.updateTemperature(trackTempC, speedKmh);
    }

    // -----------------------------------------------------------------------
    // Lap & sector detection
    // -----------------------------------------------------------------------

    /**
     * Determines whether a car has completed a new lap given its distance
     * before and after the tick.
     *
     * @param distanceBefore cumulative distance at the start of the tick.
     * @param distanceAfter  cumulative distance at the end of the tick.
     * @param trackLength    full lap length in metres.
     * @return {@code true} if the car crossed the start/finish line this tick.
     */
    public boolean hasCompletedLap(double distanceBefore, double distanceAfter,
                                   double trackLength) {
        long lapsBefore = (long) (distanceBefore / trackLength);
        long lapsAfter  = (long) (distanceAfter  / trackLength);
        return lapsAfter > lapsBefore;
    }

    /**
     * Returns the current lap number (1-indexed) for a cumulative distance.
     *
     * @param totalDistanceMeters cumulative distance from race start.
     * @param trackLengthMeters   full lap length in metres.
     * @return lap number (&ge; 1).
     */
    public int getLapNumber(double totalDistanceMeters, double trackLengthMeters) {
        return (int) (totalDistanceMeters / trackLengthMeters) + 1;
    }

    /**
     * Returns the lap-relative distance (metres from the start line on the
     * current lap).
     *
     * @param totalDistanceMeters cumulative race distance.
     * @param trackLengthMeters   full lap length in metres.
     * @return distance in [0, trackLengthMeters).
     */
    public double getLapDistance(double totalDistanceMeters, double trackLengthMeters) {
        return totalDistanceMeters % trackLengthMeters;
    }

    // -----------------------------------------------------------------------
    // Gap calculation
    // -----------------------------------------------------------------------

    /**
     * Estimates the time gap between two cars (seconds) given their cumulative
     * distances and the chasing car's current speed.
     *
     * <pre>
     *   gap = (leaderDistance − chaserDistance) / chaserSpeedMs
     * </pre>
     *
     * <p>If the chaser is ahead (negative delta) the result is negative, which
     * is meaningful for internal leaderboard ordering.</p>
     *
     * @param leaderDistanceMeters  cumulative distance of the car ahead.
     * @param chaserDistanceMeters  cumulative distance of the car behind.
     * @param chaserSpeedKmh        chasing car's current speed (km/h).
     * @return time gap in seconds; 0.0 when speeds are effectively zero.
     */
    public double calculateGap(double leaderDistanceMeters,
                               double chaserDistanceMeters,
                               double chaserSpeedKmh) {
        double distanceDelta = leaderDistanceMeters - chaserDistanceMeters;
        double speedMs       = Math.max(1.0, chaserSpeedKmh / 3.6);   // guard ÷0
        return distanceDelta / speedMs;
    }

    // -----------------------------------------------------------------------
    // Pit-stop service time
    // -----------------------------------------------------------------------

    /**
     * Generates a randomised pit-stop service duration in seconds.
     *
     * <p>The value is drawn from a uniform distribution centred on
     * {@code PIT_SERVICE_BASE_SECONDS} with ± {@code PIT_SERVICE_JITTER_SECONDS}.</p>
     *
     * @return pit service time in seconds.
     */
    public double generatePitServiceTime() {
        double jitter = (Math.random() - 0.5) * 2.0 * PIT_SERVICE_JITTER_SECONDS;
        return PIT_SERVICE_BASE_SECONDS + jitter;
    }

    // -----------------------------------------------------------------------
    // Accessors for constants (used by PitStrategyPredictor)
    // -----------------------------------------------------------------------

    public double getFuelBurnKgPerMeter()    { return FUEL_BURN_KG_PER_METER; }
    public double getPitServiceBaseSeconds() { return PIT_SERVICE_BASE_SECONDS; }
}
