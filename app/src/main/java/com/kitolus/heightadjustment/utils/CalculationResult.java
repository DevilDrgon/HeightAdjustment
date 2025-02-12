package com.kitolus.heightadjustment.utils;

import lombok.Getter;

@Getter
public class CalculationResult {

    private final double originalHeartPlate;
    private final double originalWearPlate;
    private final double adjustedHeartPlate;
    private final double adjustedWearPlate;
    private final double extraAdjustedWearPlate;
    private final double totalHeightOriginal;
    private final double totalHeightAdjusted;
    private final double totalHeightPlan;
    private final double wheelOriginal;
    private final double wheelAdjusted;

    public CalculationResult(double adjustedHeartPlate,
                             double adjustedWearPlate,
                             double totalHeightOriginal,
                             double totalHeightAdjusted,
                             double extraAdjustedWearPlate,
                             double totalHeightPlan,
                             double originalHeartPlate,
                             double originalWearPlate,
                             double wheelOriginal,
                             double wheelAdjusted) {
        this.adjustedHeartPlate = adjustedHeartPlate;
        this.adjustedWearPlate = adjustedWearPlate;
        this.extraAdjustedWearPlate = extraAdjustedWearPlate;
        this.totalHeightOriginal = totalHeightOriginal;
        this.totalHeightAdjusted = totalHeightAdjusted;
        this.totalHeightPlan = totalHeightPlan;
        this.originalHeartPlate = originalHeartPlate;
        this.originalWearPlate = originalWearPlate;
        this.wheelOriginal = wheelOriginal;
        this.wheelAdjusted = wheelAdjusted;
    }

}
