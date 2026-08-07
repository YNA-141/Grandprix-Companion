package com.gpcompanion.core;

/**
 * Contract for any entity that participates in the simulation tick loop.
 * <p>
 * Every object that must advance its internal state by one discrete time step
 * (e.g. {@code Car}, an AI driver brain, or a weather interpolator) should
 * implement this interface.  The engine calls {@link #update(double)} on all
 * registered {@code Simulatable}s once per simulation tick.
 * </p>
 *
 * <p><b>Threading note:</b> implementations must NOT spawn threads or perform
 * any JavaFX Platform.runLater() calls; threading is the responsibility of
 * the engine layer.</p>
 */
public interface Simulatable {

    /**
     * Advances the object's state by the given wall-clock delta.
     *
     * @param deltaTime elapsed real-world seconds since the last tick,
     *                  already scaled by the current simulation speed multiplier.
     *                  Always {@code > 0}.
     */
    void update(double deltaTime);
}
