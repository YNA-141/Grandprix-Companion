package com.gpcompanion.exception;

/**
 * Thrown when a track configuration file cannot be parsed into a valid
 * {@link com.gpcompanion.model.TrackProfile}.
 *
 * <p>Causes include: missing mandatory keys, values that fail range
 * validation, a curve-point list that is empty or has too few entries, or
 * sector boundary distances that are inconsistent with the total track
 * length.</p>
 */
public class CorruptedTrackFileException extends Exception {

    /**
     * Constructs the exception with a descriptive message only.
     *
     * @param message human-readable explanation of what failed.
     */
    public CorruptedTrackFileException(String message) {
        super(message);
    }

    /**
     * Constructs the exception with a message and the underlying cause.
     * Use this constructor when wrapping a lower-level exception
     * (e.g. {@link java.io.IOException}, {@link NumberFormatException}).
     *
     * @param message human-readable explanation of what failed.
     * @param cause   the root-cause exception.
     */
    public CorruptedTrackFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
