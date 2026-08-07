package com.gpcompanion.model;

import com.gpcompanion.model.enums.TireCompound;
import java.util.Locale;

public class TelemetryData {
    private final double sessionTimeSeconds;
    private final int lapNumber;
    private final String driverId;
    private final String driverShortCode;
    private final double speedKmh;
    private final TireCompound tireCompound;
    private final double tireWearPercentage;
    private final double tireTemperatureC;
    private final double distanceMeters;
    private final double gapToLeaderSeconds;

    public TelemetryData(double sessionTimeSeconds, int lapNumber, String driverId, String driverShortCode,
                         double speedKmh, TireCompound tireCompound, double tireWearPercentage,
                         double tireTemperatureC, double distanceMeters, double gapToLeaderSeconds) {
        this.sessionTimeSeconds = sessionTimeSeconds;
        this.lapNumber = lapNumber;
        this.driverId = driverId;
        this.driverShortCode = driverShortCode;
        this.speedKmh = speedKmh;
        this.tireCompound = tireCompound;
        this.tireWearPercentage = tireWearPercentage;
        this.tireTemperatureC = tireTemperatureC;
        this.distanceMeters = distanceMeters;
        this.gapToLeaderSeconds = gapToLeaderSeconds;
    }

    public String toCsvRow() {
        String gapStr = String.format(Locale.US, "%+.3f", gapToLeaderSeconds);
        return String.format(Locale.US, "%.3f,%d,%s,%s,%.1f,%s,%.1f,%.1f,%.1f,%s",
                sessionTimeSeconds, lapNumber, driverId, driverShortCode, speedKmh,
                tireCompound.name(), tireWearPercentage, tireTemperatureC, distanceMeters, gapStr);
    }

    public double getSessionTimeSeconds() {
        return sessionTimeSeconds;
    }

    public int getLapNumber() {
        return lapNumber;
    }

    public String getDriverId() {
        return driverId;
    }

    public String getDriverShortCode() {
        return driverShortCode;
    }

    public double getSpeedKmh() {
        return speedKmh;
    }

    public TireCompound getTireCompound() {
        return tireCompound;
    }

    public double getTireWearPercentage() {
        return tireWearPercentage;
    }

    public double getTireTemperatureC() {
        return tireTemperatureC;
    }

    public double getDistanceMeters() {
        return distanceMeters;
    }

    public double getGapToLeaderSeconds() {
        return gapToLeaderSeconds;
    }
}
