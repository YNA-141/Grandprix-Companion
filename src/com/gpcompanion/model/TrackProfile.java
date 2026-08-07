package com.gpcompanion.model;

import java.util.List;
import javafx.geometry.Point2D;

public class TrackProfile {
    private String trackName;
    private double trackLengthMeters;
    private int totalLaps;
    private double sector1EndDistanceMeters;
    private double sector2EndDistanceMeters;
    private List<TrackPoint> curvePoints;

    public TrackProfile(String trackName, double trackLengthMeters, int totalLaps,
                        double sector1EndDistanceMeters, double sector2EndDistanceMeters,
                        List<TrackPoint> curvePoints) {
        this.trackName = trackName;
        this.trackLengthMeters = trackLengthMeters;
        this.totalLaps = totalLaps;
        this.sector1EndDistanceMeters = sector1EndDistanceMeters;
        this.sector2EndDistanceMeters = sector2EndDistanceMeters;
        this.curvePoints = curvePoints;
    }

    public int getSectorForDistance(double lapDistanceMeters) {
        if (lapDistanceMeters <= sector1EndDistanceMeters) {
            return 1;
        } else if (lapDistanceMeters <= sector2EndDistanceMeters) {
            return 2;
        } else {
            return 3;
        }
    }

    /**
     * Linearly interpolates the (x, y) canvas coordinate for a given
     * cumulative lap distance.
     *
     * @param lapDistanceMeters distance from the start line in metres,
     *                          clamped to [0, trackLengthMeters].
     * @return {@code Point2D} representing the canvas coordinates.
     */
    public Point2D getXYForDistance(double lapDistanceMeters) {
        if (curvePoints == null || curvePoints.isEmpty()) {
            return new Point2D(0.0, 0.0);
        }

        TrackPoint prev = curvePoints.get(0);
        TrackPoint next = curvePoints.get(curvePoints.size() - 1);

        for (int i = 0; i < curvePoints.size() - 1; i++) {
            if (lapDistanceMeters >= curvePoints.get(i).getCumulativeDistanceMeters()
                    && lapDistanceMeters <= curvePoints.get(i + 1).getCumulativeDistanceMeters()) {
                prev = curvePoints.get(i);
                next = curvePoints.get(i + 1);
                break;
            }
        }

        double segmentLength = next.getCumulativeDistanceMeters() - prev.getCumulativeDistanceMeters();
        if (segmentLength == 0.0) {
            return new Point2D(prev.getX(), prev.getY());
        }

        double fraction = (lapDistanceMeters - prev.getCumulativeDistanceMeters()) / segmentLength;
        double x = prev.getX() + fraction * (next.getX() - prev.getX());
        double y = prev.getY() + fraction * (next.getY() - prev.getY());
        return new Point2D(x, y);
    }

    public String getTrackName() {
        return trackName;
    }

    public double getTrackLengthMeters() {
        return trackLengthMeters;
    }

    public int getTotalLaps() {
        return totalLaps;
    }

    public double getSector1EndDistanceMeters() {
        return sector1EndDistanceMeters;
    }

    public double getSector2EndDistanceMeters() {
        return sector2EndDistanceMeters;
    }

    public List<TrackPoint> getCurvePoints() {
        return curvePoints;
    }
}
