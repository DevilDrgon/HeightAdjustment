package com.kitolus.heightadjustment.models;

import com.kitolus.heightadjustment.utils.CalculationResult;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;

@Getter
public class HistoryRecord {
    private final long timestamp;
    private final String carId;
    private final double adjustedHeartPlate;
    private final double adjustedWearPlate;
    private final Map<String, Double> parameters;
    private final CalculationResult result;

    public HistoryRecord(long timestamp, String carId,
                         double adjustedHeartPlate,
                         double adjustedWearPlate,
                         Map<String, Double> parameters,
                         CalculationResult result) {
        this.timestamp = timestamp;
        this.carId = carId;
        this.adjustedHeartPlate = adjustedHeartPlate;
        this.adjustedWearPlate = adjustedWearPlate;
        this.parameters = new HashMap<>(parameters);
        this.result = result;
    }

    // 动态返回单位
    public String getUnit() {
        return "mm";
    }
}