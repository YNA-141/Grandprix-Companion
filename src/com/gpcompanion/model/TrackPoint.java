package com.gpcompanion.model;

public class TrackPoint {
    private double x;
    private double y;
    private double cumulativeDistanceMeters;

    public TrackPoint(double x, double y, double cumulativeDistanceMeters) {
        this.x = x;
        this.y = y;
        this.cumulativeDistanceMeters = cumulativeDistanceMeters;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getCumulativeDistanceMeters() {
        return cumulativeDistanceMeters;
    }
}
