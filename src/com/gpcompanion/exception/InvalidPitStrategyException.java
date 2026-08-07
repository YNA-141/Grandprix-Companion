package com.gpcompanion.exception;

/**
 * Thrown by RaceEngine.forcePitStop(...) when: the given driverId doesn't exist
 * in the session, the driver is already isInPitLane, or the predicted pit
 * window is structurally invalid.
 */
public class InvalidPitStrategyException extends Exception {

    public InvalidPitStrategyException(String message) {
        super(message);
    }

    public InvalidPitStrategyException(String message, Throwable cause) {
        super(message, cause);
    }
}
