package com.gpcompanion.model.tire;

import com.gpcompanion.model.enums.TireCompound;

public abstract class Tire {
    private String compoundName;
    private TireCompound compound;
    private double baseGripCoefficient;
    private double wearRatePerMeter;
    private double optimalTempMinC;
    private double optimalTempMaxC;
    private double offConditionGripMultiplier;

    private double currentWearPercentage;
    private double currentTemperatureC;
    private int stintLapCount;

    protected Tire(String compoundName, TireCompound compound, double baseGripCoefficient,
                   double wearRatePerMeter, double optimalTempMinC, double optimalTempMaxC,
                   double offConditionGripMultiplier) {
        this.compoundName = compoundName;
        this.compound = compound;
        this.baseGripCoefficient = baseGripCoefficient;
        this.wearRatePerMeter = wearRatePerMeter;
        this.optimalTempMinC = optimalTempMinC;
        this.optimalTempMaxC = optimalTempMaxC;
        this.offConditionGripMultiplier = offConditionGripMultiplier;
        
        this.currentWearPercentage = 100.0;
        this.currentTemperatureC = (optimalTempMinC + optimalTempMaxC) / 2.0; 
        this.stintLapCount = 0;
    }

    public abstract double calculateGrip(boolean isRaining, double trackTempC);

    public void degrade(double distanceMeters) {
        currentWearPercentage -= wearRatePerMeter * distanceMeters;
        if (currentWearPercentage < 0) {
            currentWearPercentage = 0;
        }
    }

    public void updateTemperature(double trackTempC, double speedKmh) {
        double target = trackTempC + (speedKmh * 0.05);
        currentTemperatureC += (target - currentTemperatureC) * 0.02;
    }

    public boolean isCriticallyWorn() {
        return currentWearPercentage < 30.0;
    }

    public void reset() {
        currentWearPercentage = 100.0;
        stintLapCount = 0;
    }

    public void incrementStintLap() {
        stintLapCount++;
    }

    protected double getWearFactor() {
        if (currentWearPercentage >= 30.0) {
            return currentWearPercentage / 100.0;
        } else {
            return (currentWearPercentage / 100.0) * (currentWearPercentage / 30.0);
        }
    }

    protected double getTemperatureFactor(double trackTempC) {
        if (trackTempC >= optimalTempMinC && trackTempC <= optimalTempMaxC) {
            return 1.0;
        } else if (trackTempC < optimalTempMinC) {
            return Math.max(0.5, 1.0 - (optimalTempMinC - trackTempC) * 0.02);
        } else {
            return Math.max(0.5, 1.0 - (trackTempC - optimalTempMaxC) * 0.02);
        }
    }

    public String getCompoundName() { return compoundName; }
    public TireCompound getCompound() { return compound; }
    public double getBaseGripCoefficient() { return baseGripCoefficient; }
    public double getWearRatePerMeter() { return wearRatePerMeter; }
    public double getOptimalTempMinC() { return optimalTempMinC; }
    public double getOptimalTempMaxC() { return optimalTempMaxC; }
    public double getOffConditionGripMultiplier() { return offConditionGripMultiplier; }

    public double getCurrentWearPercentage() { return currentWearPercentage; }
    public void setCurrentWearPercentage(double currentWearPercentage) { this.currentWearPercentage = currentWearPercentage; }

    public double getCurrentTemperatureC() { return currentTemperatureC; }
    public void setCurrentTemperatureC(double currentTemperatureC) { this.currentTemperatureC = currentTemperatureC; }

    public int getStintLapCount() { return stintLapCount; }
    public void setStintLapCount(int stintLapCount) { this.stintLapCount = stintLapCount; }
}
