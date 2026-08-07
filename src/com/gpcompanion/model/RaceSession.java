package com.gpcompanion.model;

import com.gpcompanion.core.Simulatable;
import com.gpcompanion.model.enums.TrackStatus;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RaceSession {
    private String sessionId;
    private TrackProfile trackProfile;
    private List<Driver> drivers;
    private List<Simulatable> simulatableEntities;
    private int currentLap; 
    private double sessionTimeElapsedSeconds; 
    private WeatherState weather; 
    private TrackStatus trackStatus; 
    private double simulationSpeedMultiplier; 
    private boolean isPaused; 

    public RaceSession(String sessionId, TrackProfile trackProfile, List<Driver> drivers) {
        this.sessionId = sessionId;
        this.trackProfile = trackProfile;
        this.drivers = drivers;
        
        this.simulatableEntities = new ArrayList<>();
        for (Driver driver : drivers) {
            if (driver.getCar() != null) {
                this.simulatableEntities.add(driver.getCar());
            }
        }
        
        this.currentLap = 1;
        this.trackStatus = TrackStatus.TRACK_CLEAR;
        this.simulationSpeedMultiplier = 1.0;
        this.sessionTimeElapsedSeconds = 0.0;
        this.isPaused = false;
        this.weather = new WeatherState(com.gpcompanion.model.enums.WeatherCondition.DRY, 28.0, 42.0, 15.0, 315.0);
    }

    public void update(double deltaTime) {
        for (Simulatable entity : simulatableEntities) {
            entity.update(deltaTime);
        }
        
        List<Driver> sortedDrivers = getLeaderboardOrder();
        if (!sortedDrivers.isEmpty()) {
            this.currentLap = sortedDrivers.get(0).getCurrentLapNumber();
        }
        
        this.sessionTimeElapsedSeconds += deltaTime;
    }

    public List<Driver> getLeaderboardOrder() {
        List<Driver> sortedDrivers = new ArrayList<>(drivers);
        sortedDrivers.sort(new Comparator<Driver>() {
            @Override
            public int compare(Driver d1, Driver d2) {
                return Double.compare(d2.getCar().getDistanceTraveledMeters(), d1.getCar().getDistanceTraveledMeters());
            }
        });
        
        for (int i = 0; i < sortedDrivers.size(); i++) {
            sortedDrivers.get(i).setPosition(i + 1);
        }
        
        return sortedDrivers;
    }

    /**
     * Recalculates each driver's gap-to-leader and interval-to-car-ahead
     * using a simple distance-over-speed approximation.
     *
     * <p>The engine layer will replace this with a proper physics-model
     * calculation once the {@code engine} package is implemented.</p>
     */
    public void recalculateGapsAndIntervals() {
        List<Driver> sorted = getLeaderboardOrder();
        if (sorted.isEmpty()) return;

        double leaderDistance = sorted.get(0).getCar().getDistanceTraveledMeters();

        for (int i = 0; i < sorted.size(); i++) {
            Driver driver = sorted.get(i);
            double distanceDelta = leaderDistance - driver.getCar().getDistanceTraveledMeters();
            double speedMs = Math.max(1.0, driver.getCar().getCurrentSpeedKmh() / 3.6);
            double gap = distanceDelta / speedMs;          // seconds
            driver.setGapToLeaderSeconds(gap);

            if (i == 0) {
                driver.setIntervalToAheadSeconds(0.0);
            } else {
                Driver ahead        = sorted.get(i - 1);
                double intervalDist = ahead.getCar().getDistanceTraveledMeters()
                                      - driver.getCar().getDistanceTraveledMeters();
                driver.setIntervalToAheadSeconds(intervalDist / speedMs);
            }
        }
    }

    public void triggerSafetyCar() {
        this.trackStatus = TrackStatus.SAFETY_CAR;
    }

    public void clearSafetyCar() {
        this.trackStatus = TrackStatus.TRACK_CLEAR;
    }

    public boolean isRaceComplete() {
        return currentLap > trackProfile.getTotalLaps();
    }

    public String getSessionId() { return sessionId; }
    public TrackProfile getTrackProfile() { return trackProfile; }
    public List<Driver> getDrivers() { return drivers; }
    public List<Simulatable> getSimulatableEntities() { return simulatableEntities; }

    public int getCurrentLap() { return currentLap; }
    public void setCurrentLap(int currentLap) { this.currentLap = currentLap; }

    public double getSessionTimeElapsedSeconds() { return sessionTimeElapsedSeconds; }
    public void setSessionTimeElapsedSeconds(double sessionTimeElapsedSeconds) { this.sessionTimeElapsedSeconds = sessionTimeElapsedSeconds; }

    public WeatherState getWeather() { return weather; }
    public void setWeather(WeatherState weather) { this.weather = weather; }

    public TrackStatus getTrackStatus() { return trackStatus; }
    public void setTrackStatus(TrackStatus trackStatus) { this.trackStatus = trackStatus; }

    public double getSimulationSpeedMultiplier() { return simulationSpeedMultiplier; }
    public void setSimulationSpeedMultiplier(double simulationSpeedMultiplier) { this.simulationSpeedMultiplier = simulationSpeedMultiplier; }

    public boolean isPaused() { return isPaused; }
    public void setPaused(boolean paused) { this.isPaused = paused; }
}
