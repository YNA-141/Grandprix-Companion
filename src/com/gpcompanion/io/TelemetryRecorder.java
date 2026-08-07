package com.gpcompanion.io;

import com.gpcompanion.core.Exportable;
import com.gpcompanion.model.TelemetryData;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Thread-safe recorder that accumulates {@link TelemetryData} snapshots
 * during a race session and can export them to a CSV file on demand.
 *
 * <h2>CSV Layout</h2>
 * <pre>
 *   session_time_s,lap,driver_id,driver_code,speed_kmh,compound,wear_%,temp_c,distance_m,gap_s
 *   0.016,1,VER,VER,0.0,SOFT,100.0,100.0,0.0,+0.000
 *   ...
 * </pre>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * {@link #record(TelemetryData)} and {@link #exportToFile(File)} synchronize
 * on the internal snapshot list so that the simulation thread can call
 * {@code record} at high frequency while the UI thread triggers an export
 * without data corruption.
 * </p>
 */
public class TelemetryRecorder implements Exportable {

    // -----------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------

    private static final String CSV_HEADER =
            "session_time_s,lap,driver_id,driver_code,speed_kmh,"
            + "compound,wear_%,temp_c,distance_m,gap_s";



    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------

    /** Guarded by {@code this}. */
    private final List<TelemetryData> snapshots = new ArrayList<>();

    // -----------------------------------------------------------------------
    // Recording
    // -----------------------------------------------------------------------

    /**
     * Appends a single telemetry snapshot to the in-memory list.
     *
     * @param data the snapshot to record; must not be {@code null}.
     * @throws IllegalArgumentException if {@code data} is {@code null}.
     */
    public synchronized void record(TelemetryData data) {
        if (data == null) {
            throw new IllegalArgumentException("TelemetryData snapshot must not be null.");
        }
        snapshots.add(data);
    }

    /**
     * Returns the number of snapshots currently held.
     *
     * @return snapshot count (&ge; 0).
     */
    public synchronized int getSnapshotCount() {
        return snapshots.size();
    }

    /**
     * Returns an unmodifiable view of all snapshots recorded so far.
     * The returned list reflects the state at the time of calling.
     *
     * @return immutable ordered list of snapshots; never {@code null}.
     */
    public synchronized List<TelemetryData> getSnapshots() {
        return Collections.unmodifiableList(new ArrayList<>(snapshots));
    }

    /**
     * Clears all recorded snapshots, freeing memory.
     * Intended for use between race sessions.
     */
    public synchronized void clear() {
        snapshots.clear();
    }

    // -----------------------------------------------------------------------
    // Exportable implementation
    // -----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Writes a UTF-8 CSV file with one header row followed by one data
     * row per recorded {@link TelemetryData} snapshot.  The CSV columns
     * exactly match the fields produced by {@link TelemetryData#toCsvRow()}.
     *
     * <p>The export is taken over a point-in-time copy of the snapshot list
     * so recording can continue concurrently without blocking.
     *
     * @throws IOException if there are no snapshots to export, or if any
     *                         I/O error occurs while writing.
     */
    @Override
    public void exportToCSV(File destination) throws IOException {
        if (destination == null) {
            throw new IOException("Export destination file must not be null.");
        }

        // Take a consistent snapshot under the lock, then release immediately
        // so the simulation thread is not stalled during the (potentially slow)
        // file I/O that follows.
        List<TelemetryData> copy;
        synchronized (this) {
            if (snapshots.isEmpty()) {
                throw new IOException(
                        "Cannot export: no telemetry snapshots have been recorded.");
            }
            copy = new ArrayList<>(snapshots);
        }

        // Ensure parent directory exists
        File parent = destination.getParentFile();
        if (parent != null && !parent.exists()) {
            if (!parent.mkdirs()) {
                throw new IOException(
                        "Could not create directory: " + parent.getAbsolutePath());
            }
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(destination, false))) {
            writer.write(CSV_HEADER);
            writer.newLine();

            for (TelemetryData td : copy) {
                writer.write(td.toCsvRow());
                writer.newLine();
            }
        }
    }
}
