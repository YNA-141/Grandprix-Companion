package com.gpcompanion.model.tire;

import com.gpcompanion.model.enums.TireCompound;

public class MediumTire extends Tire {
    public MediumTire() {
        super("Medium", TireCompound.MEDIUM, 0.90, 0.00028, 85.0, 105.0, 0.45);
    }

    @Override
    public double calculateGrip(boolean isRaining, double trackTempC) {
        double conditionFactor = isRaining ? getOffConditionGripMultiplier() : 1.0;
        return getBaseGripCoefficient() * getWearFactor() * getTemperatureFactor(trackTempC) * conditionFactor;
    }
}
