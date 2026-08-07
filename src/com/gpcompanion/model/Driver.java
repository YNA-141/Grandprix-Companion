package com.gpcompanion.model;

import com.gpcompanion.model.enums.TireCompound;
import com.gpcompanion.model.TireStint;
import java.util.ArrayList;
import java.util.List;

public class Driver {
    private String driverId;
    private String fullName;
    private String shortCode;
    private String teamName;
    private Car car;

    private int position; 
    private int currentLapNumber; 
    private double lastLapTimeSeconds; 
    private double bestLapTimeSeconds; 
    private double sector1TimeSeconds; 
    private double sector2TimeSeconds; 
    private double sector3TimeSeconds; 
    private double gapToLeaderSeconds; 
    private double intervalToAheadSeconds; 
    private List<TireStint> tireHistory; 
    private int pitStopCount; 

    public Driver(String driverId, String fullName, String shortCode, String teamName, Car car) {
        this.driverId = driverId;
        this.fullName = fullName;
        this.shortCode = shortCode;
        this.teamName = teamName;
        this.car = car;

        this.position = 0;
        this.currentLapNumber = 0;
        this.lastLapTimeSeconds = 0.0;
        this.bestLapTimeSeconds = Double.MAX_VALUE;
        this.sector1TimeSeconds = 0.0;
        this.sector2TimeSeconds = 0.0;
        this.sector3TimeSeconds = 0.0;
        this.gapToLeaderSeconds = 0.0;
        this.intervalToAheadSeconds = 0.0;
        this.tireHistory = new ArrayList<>();
        this.pitStopCount = 0;

        if (car != null && car.getCurrentTire() != null) {
            this.tireHistory.add(new TireStint(car.getCurrentTire().getCompound(), 0));
        }
    }

    public void recordLapTime(double lapTime, double s1, double s2, double s3) {
        this.lastLapTimeSeconds = lapTime;
        this.sector1TimeSeconds = s1;
        this.sector2TimeSeconds = s2;
        this.sector3TimeSeconds = s3;
        if (lapTime < this.bestLapTimeSeconds) {
            this.bestLapTimeSeconds = lapTime;
        }
        this.currentLapNumber++;
    }

    public void startNewTireStint(TireCompound compound, int startLap) {
        TireStint currentStint = getCurrentTireStint();
        if (currentStint != null) {
            currentStint.closeStint(startLap);
        }
        this.tireHistory.add(new TireStint(compound, startLap));
        this.pitStopCount++;
    }

    public TireStint getCurrentTireStint() {
        if (tireHistory.isEmpty()) {
            return null;
        }
        return tireHistory.get(tireHistory.size() - 1);
    }

    public String getDriverId() { return driverId; }
    public String getFullName() { return fullName; }
    public String getShortCode() { return shortCode; }
    public String getTeamName() { return teamName; }
    public Car getCar() { return car; }

    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }

    public int getCurrentLapNumber() { return currentLapNumber; }
    public void setCurrentLapNumber(int currentLapNumber) { this.currentLapNumber = currentLapNumber; }

    public double getLastLapTimeSeconds() { return lastLapTimeSeconds; }
    public void setLastLapTimeSeconds(double lastLapTimeSeconds) { this.lastLapTimeSeconds = lastLapTimeSeconds; }

    public double getBestLapTimeSeconds() { return bestLapTimeSeconds; }
    public void setBestLapTimeSeconds(double bestLapTimeSeconds) { this.bestLapTimeSeconds = bestLapTimeSeconds; }

    public double getSector1TimeSeconds() { return sector1TimeSeconds; }
    public void setSector1TimeSeconds(double sector1TimeSeconds) { this.sector1TimeSeconds = sector1TimeSeconds; }

    public double getSector2TimeSeconds() { return sector2TimeSeconds; }
    public void setSector2TimeSeconds(double sector2TimeSeconds) { this.sector2TimeSeconds = sector2TimeSeconds; }

    public double getSector3TimeSeconds() { return sector3TimeSeconds; }
    public void setSector3TimeSeconds(double sector3TimeSeconds) { this.sector3TimeSeconds = sector3TimeSeconds; }

    public double getGapToLeaderSeconds() { return gapToLeaderSeconds; }
    public void setGapToLeaderSeconds(double gapToLeaderSeconds) { this.gapToLeaderSeconds = gapToLeaderSeconds; }

    public double getIntervalToAheadSeconds() { return intervalToAheadSeconds; }
    public void setIntervalToAheadSeconds(double intervalToAheadSeconds) { this.intervalToAheadSeconds = intervalToAheadSeconds; }

    public List<TireStint> getTireHistory() { return tireHistory; }
    public void setTireHistory(List<TireStint> tireHistory) { this.tireHistory = tireHistory; }

    public int getPitStopCount() { return pitStopCount; }
    public void setPitStopCount(int pitStopCount) { this.pitStopCount = pitStopCount; }
}
