package com.gpcompanion.model;

import com.gpcompanion.model.enums.TireCompound;

public class TireStint {
    private TireCompound compound;
    private int startLap;
    private int endLap; 

    public TireStint(TireCompound compound, int startLap) {
        this.compound = compound;
        this.startLap = startLap;
        this.endLap = -1;
    }

    public void closeStint(int endLap) {
        this.endLap = endLap;
    }

    public int getLapsUsed(int currentLapIfOngoing) {
        if (endLap != -1) {
            return endLap - startLap;
        } else {
            return currentLapIfOngoing - startLap;
        }
    }

    public TireCompound getCompound() {
        return compound;
    }

    public int getStartLap() {
        return startLap;
    }

    public int getEndLap() {
        return endLap;
    }

    public void setEndLap(int endLap) {
        this.endLap = endLap;
    }
}
