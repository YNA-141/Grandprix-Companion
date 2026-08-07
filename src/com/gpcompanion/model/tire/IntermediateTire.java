package com.gpcompanion.model.tire;

import com.gpcompanion.model.enums.TireCompound;

public class IntermediateTire extends Tire {
    public IntermediateTire() {
        super("Intermediate", TireCompound.INTERMEDIATE, 0.78, 0.00022, 45.0, 65.0, 0.50);
    }

    @Override
    public double calculateGrip(boolean isRaining, double trackTempC) {
        double conditionFactor = isRaining ? 1.0 : getOffConditionGripMultiplier();
        return getBaseGripCoefficient() * getWearFactor() * getTemperatureFactor(trackTempC) * conditionFactor;
    }
}
