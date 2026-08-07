package com.gpcompanion.model.tire;

import com.gpcompanion.model.enums.TireCompound;

public class HardTire extends Tire {
    public HardTire() {
        super("Hard", TireCompound.HARD, 0.83, 0.00017, 80.0, 100.0, 0.55);
    }

    @Override
    public double calculateGrip(boolean isRaining, double trackTempC) {
        double conditionFactor = isRaining ? getOffConditionGripMultiplier() : 1.0;
        return getBaseGripCoefficient() * getWearFactor() * getTemperatureFactor(trackTempC) * conditionFactor;
    }
}
