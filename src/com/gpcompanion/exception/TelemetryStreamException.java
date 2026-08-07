package com.gpcompanion.exception;

/**
 * Thrown when the engine's telemetry streaming pipeline encounters an
 * unrecoverable error during a simulation tick — for example, if
 * {@link com.gpcompanion.io.TelemetryRecorder#record(com.gpcompanion.model.TelemetryData)}
 * is called on a recorder that has been closed, or if snapshot construction
 * fails due to a null driver state.
 *
 * <p>This is a checked exception so that the {@code RaceEngine.run()} method
 * is forced to handle or re-throw it explicitly inside its
 * {@code try/catch/finally} block.</p>
 */
public class TelemetryStreamException extends Exception {

    /**
     * Constructs the exception with a descriptive message only.
     *
     * @param message human-readable explanation of what failed.
     */
    public TelemetryStreamException(String message) {
        super(message);
    }

    /**
     * Constructs the exception with a message and the underlying cause.
     * Use this constructor when wrapping a lower-level failure
     * (e.g. an {@link IllegalArgumentException} from the recorder).
     *
     * @param message human-readable explanation of what failed.
     * @param cause   the root-cause exception.
     */
    public TelemetryStreamException(String message, Throwable cause) {
        super(message, cause);
    }
}
