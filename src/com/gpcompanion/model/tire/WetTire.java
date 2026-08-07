package com.gpcompanion.model.tire;

import com.gpcompanion.model.enums.TireCompound;

public class WetTire extends Tire {
    public WetTire() {
        super("Wet", TireCompound.WET, 0.70, 0.00015, 35.0, 55.0, 0.35);
    }

    @Override
    public double calculateGrip(boolean isRaining, double trackTempC) {
        double conditionFactor = isRaining ? 1.0 : getOffConditionGripMultiplier();
        return getBaseGripCoefficient() * getWearFactor() * getTemperatureFactor(trackTempC) * conditionFactor;
    }
}
