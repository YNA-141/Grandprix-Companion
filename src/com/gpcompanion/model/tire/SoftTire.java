package com.gpcompanion.model.tire;

import com.gpcompanion.model.enums.TireCompound;

public class SoftTire extends Tire {
    public SoftTire() {
        super("Soft", TireCompound.SOFT, 0.97, 0.00042, 90.0, 110.0, 0.30);
    }

    @Override
    public double calculateGrip(boolean isRaining, double trackTempC) {
        double conditionFactor = isRaining ? getOffConditionGripMultiplier() : 1.0;
        return getBaseGripCoefficient() * getWearFactor() * getTemperatureFactor(trackTempC) * conditionFactor;
    }
}
