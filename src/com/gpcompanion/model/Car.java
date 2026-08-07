package com.gpcompanion.model;

import com.gpcompanion.core.Simulatable;
import com.gpcompanion.model.tire.Tire;

public class Car implements Simulatable {
    private int carNumber;
    private Tire currentTire; 
    private double fuelLoadKg; 
    private double startingFuelKg;
    private double baseVelocityKmh;
    private double currentSpeedKmh; 
    private double distanceTraveledMeters; 
    private boolean isInPitLane; 

    public Car(int carNumber, Tire startingTire, double baseVelocityKmh, double startingFuelKg) {
        this.carNumber = carNumber;
        this.currentTire = startingTire;
        this.baseVelocityKmh = baseVelocityKmh;
        this.startingFuelKg = startingFuelKg;
        
        this.fuelLoadKg = startingFuelKg;
        this.distanceTraveledMeters = 0.0;
        this.currentSpeedKmh = 0.0;
        this.isInPitLane = false;
    }

    @Override
    public void update(double deltaTime) {
        // Implementation left intentionally blank, as it relies on PhysicsCalculator (part of engine)
        // according to the spec, the actual engine loop drives this.
    }

    public void fitTire(Tire newTire) {
        this.currentTire = newTire;
        this.currentTire.reset();
    }

    public double getLapDistanceMeters(double trackLengthMeters) {
        return distanceTraveledMeters % trackLengthMeters;
    }

    public int getCarNumber() {
        return carNumber;
    }

    public Tire getCurrentTire() {
        return currentTire;
    }

    public void setCurrentTire(Tire currentTire) {
        this.currentTire = currentTire;
    }

    public double getFuelLoadKg() {
        return fuelLoadKg;
    }

    public void setFuelLoadKg(double fuelLoadKg) {
        this.fuelLoadKg = fuelLoadKg;
    }

    public double getStartingFuelKg() {
        return startingFuelKg;
    }

    public double getBaseVelocityKmh() {
        return baseVelocityKmh;
    }

    public double getCurrentSpeedKmh() {
        return currentSpeedKmh;
    }

    public void setCurrentSpeedKmh(double currentSpeedKmh) {
        this.currentSpeedKmh = currentSpeedKmh;
    }

    public double getDistanceTraveledMeters() {
        return distanceTraveledMeters;
    }

    public void setDistanceTraveledMeters(double distanceTraveledMeters) {
        this.distanceTraveledMeters = distanceTraveledMeters;
    }

    public boolean isInPitLane() {
        return isInPitLane;
    }

    public void setInPitLane(boolean inPitLane) {
        this.isInPitLane = inPitLane;
    }
}
