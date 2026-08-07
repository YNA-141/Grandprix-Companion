package com.gpcompanion.io;

import com.gpcompanion.exception.CorruptedTrackFileException;
import com.gpcompanion.model.TrackPoint;
import com.gpcompanion.model.TrackProfile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses a plain-text track configuration file into a {@link TrackProfile}.
 *
 * <h2>File Format</h2>
 * <p>
 * The file uses a simple {@code key=value} format.  Lines beginning with
 * {@code #} are comments and are ignored.  Blank lines are also ignored.
 * All keys are case-insensitive and leading/trailing whitespace around both
 * key and value is stripped.
 * </p>
 *
 * <h3>Required scalar keys</h3>
 * <pre>
 *   name              = Monaco Grand Prix
 *   length_meters     = 3337.0
 *   total_laps        = 78
 *   sector1_end_m     = 1100.0
 *   sector2_end_m     = 2300.0
 * </pre>
 *
 * <h3>Curve-point block</h3>
 * <p>
 * After all scalar keys, include a {@code [curve_points]} section header
 * followed by one point per line in the format:
 * </p>
 * <pre>
 *   [curve_points]
 *   x,y,cumulative_distance
 *   0.0,0.0,0.0
 *   50.3,12.7,250.0
 *   ...
 * </pre>
 *
 * <h2>Validation Rules (blueprint §2.6)</h2>
 * <ol>
 *   <li>{@code length_meters} must be &gt; 0.</li>
 *   <li>{@code total_laps} must be &ge; 1.</li>
 *   <li>{@code sector1_end_m} must be &gt; 0 and &lt; {@code length_meters}.</li>
 *   <li>{@code sector2_end_m} must be &gt; {@code sector1_end_m} and
 *       &lt; {@code length_meters}.</li>
 *   <li>The curve-point list must contain at least two points.</li>
 *   <li>Every curve point's cumulative distance must be in
 *       [0, {@code length_meters}] and the list must be strictly
 *       monotonically increasing in cumulative distance.</li>
 *   <li>The first curve-point's cumulative distance must be 0.0.</li>
 *   <li>The last curve-point's cumulative distance must equal
 *       {@code length_meters} (within 1.0 m tolerance).</li>
 * </ol>
 */
public class TrackFileReader {

    // -----------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------

    private static final String SECTION_CURVE_POINTS  = "[curve_points]";
    private static final double DISTANCE_TOLERANCE_M   = 1.0;

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Reads and parses the supplied track configuration file.
     *
     * @param sourceFile the track configuration file to parse.  Must not be
     *             {@code null}.
     * @return a fully validated {@link TrackProfile} ready for use by the
     *         simulation engine.
     * @throws CorruptedTrackFileException if the file cannot be read, if any
     *                                   mandatory key is missing, or if any
     *                                   validation rule is violated.
     * @throws IOException if any I/O error prevents reading.
     */
    public TrackProfile readTrackProfile(File sourceFile) throws CorruptedTrackFileException, IOException {
        if (sourceFile == null) {
            throw new CorruptedTrackFileException("Track file must not be null.");
        }
        if (!sourceFile.exists()) {
            throw new CorruptedTrackFileException(
                    "Track file not found: " + sourceFile.getAbsolutePath());
        }
        if (!sourceFile.isFile()) {
            throw new CorruptedTrackFileException(
                    "Path does not point to a regular file: " + sourceFile.getAbsolutePath());
        }
        if (!sourceFile.canRead()) {
            throw new CorruptedTrackFileException(
                    "Track file is not readable (check permissions): " + sourceFile.getAbsolutePath());
        }

        Map<String, String> scalars     = new HashMap<>();
        List<TrackPoint>    curvePoints = new ArrayList<>();

        // ── Parse ──────────────────────────────────────────────────────────
        try (BufferedReader br = new BufferedReader(new FileReader(sourceFile))) {
            boolean inCurveSection = false;
            int     lineNumber     = 0;
            String  line;

            while ((line = br.readLine()) != null) {
                lineNumber++;
                line = line.strip();

                // Skip blank lines and comments
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                // Section header detection
                if (line.equalsIgnoreCase(SECTION_CURVE_POINTS)) {
                    inCurveSection = true;
                    continue;
                }

                if (inCurveSection) {
                    curvePoints.add(parseCurvePoint(line, lineNumber));
                } else {
                    parseScalarEntry(line, lineNumber, scalars);
                }
            }
        } catch (IOException e) {
            throw new CorruptedTrackFileException(
                    "I/O error while reading track file: " + e.getMessage(), e);
        }

        // ── Extract scalar values ──────────────────────────────────────────
        String name          = requireString(scalars, "name");
        double lengthMeters  = requireDouble(scalars, "length_meters");
        int    totalLaps     = requireInt   (scalars, "total_laps");
        double sector1EndM   = requireDouble(scalars, "sector1_end_m");
        double sector2EndM   = requireDouble(scalars, "sector2_end_m");

        // ── Validation (blueprint §2.6) ────────────────────────────────────
        validateScalars(name, lengthMeters, totalLaps, sector1EndM, sector2EndM);
        validateCurvePoints(curvePoints, lengthMeters);

        return new TrackProfile(name, lengthMeters, totalLaps,
                                sector1EndM, sector2EndM, curvePoints);
    }

    // -----------------------------------------------------------------------
    // Private helpers – parsing
    // -----------------------------------------------------------------------

    /**
     * Parses a {@code key=value} line and stores the result in {@code scalars}.
     */
    private void parseScalarEntry(String line, int lineNumber,
                                  Map<String, String> scalars)
            throws CorruptedTrackFileException {

        int eqIdx = line.indexOf('=');
        if (eqIdx <= 0) {
            throw new CorruptedTrackFileException(
                    "Line " + lineNumber + ": expected 'key=value', got: " + line);
        }
        String key   = line.substring(0, eqIdx).strip().toLowerCase();
        String value = line.substring(eqIdx + 1).strip();

        if (key.isEmpty()) {
            throw new CorruptedTrackFileException(
                    "Line " + lineNumber + ": key must not be empty.");
        }
        if (value.isEmpty()) {
            throw new CorruptedTrackFileException(
                    "Line " + lineNumber + ": value for key '" + key + "' must not be empty.");
        }
        scalars.put(key, value);
    }

    /**
     * Parses a {@code x,y,cumulativeDistance} line into a {@link TrackPoint}.
     */
    private TrackPoint parseCurvePoint(String line, int lineNumber)
            throws CorruptedTrackFileException {

        String[] parts = line.split(",");
        if (parts.length != 3) {
            throw new CorruptedTrackFileException(
                    "Line " + lineNumber + ": curve point must have exactly 3 "
                    + "comma-separated values (x,y,cumulative_distance), got: " + line);
        }

        try {
            double x    = Double.parseDouble(parts[0].strip());
            double y    = Double.parseDouble(parts[1].strip());
            double dist = Double.parseDouble(parts[2].strip());
            return new TrackPoint(x, y, dist);
        } catch (NumberFormatException e) {
            throw new CorruptedTrackFileException(
                    "Line " + lineNumber + ": could not parse curve-point numbers in: " + line, e);
        }
    }

    // -----------------------------------------------------------------------
    // Private helpers – value extraction
    // -----------------------------------------------------------------------

    private String requireString(Map<String, String> scalars, String key)
            throws CorruptedTrackFileException {
        String v = scalars.get(key);
        if (v == null || v.isEmpty()) {
            throw new CorruptedTrackFileException(
                    "Missing required track property: '" + key + "'.");
        }
        return v;
    }

    private double requireDouble(Map<String, String> scalars, String key)
            throws CorruptedTrackFileException {
        String raw = requireString(scalars, key);
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            throw new CorruptedTrackFileException(
                    "Property '" + key + "' must be a decimal number, got: " + raw, e);
        }
    }

    private int requireInt(Map<String, String> scalars, String key)
            throws CorruptedTrackFileException {
        String raw = requireString(scalars, key);
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw new CorruptedTrackFileException(
                    "Property '" + key + "' must be an integer, got: " + raw, e);
        }
    }

    // -----------------------------------------------------------------------
    // Private helpers – validation
    // -----------------------------------------------------------------------

    /**
     * Validates all scalar fields according to blueprint §2.6 rules 1–4.
     */
    private void validateScalars(String name, double lengthMeters, int totalLaps,
                                  double sector1EndM, double sector2EndM)
            throws CorruptedTrackFileException {

        // Rule 1
        if (lengthMeters <= 0.0) {
            throw new CorruptedTrackFileException(
                    "length_meters must be > 0, got: " + lengthMeters);
        }

        // Rule 2
        if (totalLaps < 1) {
            throw new CorruptedTrackFileException(
                    "total_laps must be >= 1, got: " + totalLaps);
        }

        // Rule 3
        if (sector1EndM <= 0.0 || sector1EndM >= lengthMeters) {
            throw new CorruptedTrackFileException(
                    "sector1_end_m must be in (0, " + lengthMeters + "), got: " + sector1EndM);
        }

        // Rule 4
        if (sector2EndM <= sector1EndM || sector2EndM >= lengthMeters) {
            throw new CorruptedTrackFileException(
                    "sector2_end_m must be in (" + sector1EndM + ", " + lengthMeters
                    + "), got: " + sector2EndM);
        }
    }

    /**
     * Validates the curve-point list according to blueprint §2.6 rules 5–8.
     */
    private void validateCurvePoints(List<TrackPoint> points, double lengthMeters)
            throws CorruptedTrackFileException {

        // Rule 5
        if (points.size() < 2) {
            throw new CorruptedTrackFileException(
                    "Track must have at least 2 curve points, found: " + points.size());
        }

        // Rule 7: first point must start at distance 0
        if (points.get(0).getCumulativeDistanceMeters() != 0.0) {
            throw new CorruptedTrackFileException(
                    "The first curve point's cumulative distance must be 0.0, got: "
                    + points.get(0).getCumulativeDistanceMeters());
        }

        // Rule 8: last point must end at track length (within tolerance)
        double lastDist = points.get(points.size() - 1).getCumulativeDistanceMeters();
        if (Math.abs(lastDist - lengthMeters) > DISTANCE_TOLERANCE_M) {
            throw new CorruptedTrackFileException(
                    "The last curve point's cumulative distance (" + lastDist
                    + " m) must be within " + DISTANCE_TOLERANCE_M
                    + " m of length_meters (" + lengthMeters + " m).");
        }

        // Rules 6 + strict monotonicity
        double prev = points.get(0).getCumulativeDistanceMeters();
        for (int i = 1; i < points.size(); i++) {
            double curr = points.get(i).getCumulativeDistanceMeters();

            // Rule 6a: must be within [0, lengthMeters]
            if (curr < 0.0 || curr > lengthMeters + DISTANCE_TOLERANCE_M) {
                throw new CorruptedTrackFileException(
                        "Curve point " + i + " cumulative distance (" + curr
                        + " m) is out of range [0, " + lengthMeters + " m].");
            }

            // Rule 6b: strictly monotonically increasing
            if (curr <= prev) {
                throw new CorruptedTrackFileException(
                        "Curve-point cumulative distances must be strictly increasing, "
                        + "but point " + i + " (" + curr + " m) is not greater than "
                        + "point " + (i - 1) + " (" + prev + " m).");
            }
            prev = curr;
        }
    }
}
