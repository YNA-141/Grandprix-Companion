package com.gpcompanion.model;

import com.gpcompanion.model.enums.WeatherCondition;

public class WeatherState {
    private WeatherCondition condition; 
    private double airTemperatureC; 
    private double trackTemperatureC; 
    private double windSpeedKmh; 
    private double windDirectionDegrees; 

    public WeatherState(WeatherCondition condition, double airTemperatureC, double trackTemperatureC,
                        double windSpeedKmh, double windDirectionDegrees) {
        this.condition = condition;
        this.airTemperatureC = airTemperatureC;
        this.trackTemperatureC = trackTemperatureC;
        this.windSpeedKmh = windSpeedKmh;
        this.windDirectionDegrees = windDirectionDegrees;
    }

    public boolean isRaining() {
        return condition != WeatherCondition.DRY;
    }

    public void toggleRain() {
        switch (condition) {
            case DRY:
                condition = WeatherCondition.LIGHT_RAIN;
                trackTemperatureC -= 2.0; 
                break;
            case LIGHT_RAIN:
                condition = WeatherCondition.HEAVY_RAIN;
                trackTemperatureC -= 2.0;
                break;
            case HEAVY_RAIN:
                condition = WeatherCondition.DRY;
                trackTemperatureC += 4.0; 
                break;
        }
    }

    public WeatherCondition getCondition() {
        return condition;
    }

    public void setCondition(WeatherCondition condition) {
        this.condition = condition;
    }

    public double getAirTemperatureC() {
        return airTemperatureC;
    }

    public void setAirTemperatureC(double airTemperatureC) {
        this.airTemperatureC = airTemperatureC;
    }

    public double getTrackTemperatureC() {
        return trackTemperatureC;
    }

    public void setTrackTemperatureC(double trackTemperatureC) {
        this.trackTemperatureC = trackTemperatureC;
    }

    public double getWindSpeedKmh() {
        return windSpeedKmh;
    }

    public void setWindSpeedKmh(double windSpeedKmh) {
        this.windSpeedKmh = windSpeedKmh;
    }

    public double getWindDirectionDegrees() {
        return windDirectionDegrees;
    }

    public void setWindDirectionDegrees(double windDirectionDegrees) {
        this.windDirectionDegrees = windDirectionDegrees;
    }
}
