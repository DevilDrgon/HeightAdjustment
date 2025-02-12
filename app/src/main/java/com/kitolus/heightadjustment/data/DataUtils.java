package com.kitolus.heightadjustment.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.kitolus.heightadjustment.utils.CalculationResult;
import com.kitolus.heightadjustment.models.HistoryRecord;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DataUtils {

    // 常量定义
    private static final String PREFS_NAME = "WheelAdjustmentPrefs";
    private static final String HISTORY_KEY = "history";
    private static final String KEY_TIMESTAMP = "timestamp";
    private static final String KEY_CAR_ID = "carId";
    private static final String KEY_ADJUSTED_HEART_PLATE = "adjustedHeartPlate";
    private static final String KEY_PARAMETERS = "parameters";
    private static final String KEY_RESULT = "result";

    public static void saveHistory(Context context, List<HistoryRecord> history) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        JSONArray recordResult = new JSONArray();

        try {
            for (HistoryRecord record : history) {
                JSONObject recordJson = new JSONObject();
                recordJson.put(KEY_TIMESTAMP, record.getTimestamp());
                recordJson.put(KEY_CAR_ID, record.getCarId());
                recordJson.put(KEY_ADJUSTED_HEART_PLATE, record.getAdjustedHeartPlate());

                // 序列化参数
                JSONObject paramsJson = new JSONObject();
                for (Map.Entry<String, Double> entry : record.getParameters().entrySet()) {
                    paramsJson.put(entry.getKey(), entry.getValue());
                }
                recordJson.put(KEY_PARAMETERS, paramsJson);

                // 序列化计算结果（关键修复）
                JSONObject resultJson = getCompeleteResultJsonObject(record);
                recordJson.put(KEY_RESULT, resultJson);

                recordResult.put(recordJson);
            }
        } catch (JSONException e) {
            Log.e("DataUtils", "Error saving history", e);
        }

        prefs.edit().putString(HISTORY_KEY, recordResult.toString()).apply();
    }

    @NonNull
    private static JSONObject getCompeleteResultJsonObject(HistoryRecord record) throws JSONException {
        CalculationResult result = record.getResult();
        JSONObject resultJson = new JSONObject();
        resultJson.put("adjustedHeartPlate", result.getAdjustedHeartPlate());
        resultJson.put("adjustedWearPlate", result.getAdjustedWearPlate());
        resultJson.put("totalOriginal", result.getTotalHeightOriginal());
        resultJson.put("totalAdjusted", result.getTotalHeightAdjusted());
        resultJson.put("extraAdjustedWearPlate", result.getExtraAdjustedWearPlate());
        resultJson.put("totalHeightPlan", result.getTotalHeightPlan());
        resultJson.put("originalHeartPlate", result.getOriginalHeartPlate());
        resultJson.put("originalWearPlate", result.getOriginalWearPlate());
        resultJson.put("wheelOriginal", result.getWheelOriginal());
        resultJson.put("wheelAdjusted", result.getWheelAdjusted());
        return resultJson;
    }

    public static List<HistoryRecord> loadHistory(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String jsonString = prefs.getString(HISTORY_KEY, "");
        List<HistoryRecord> history = new ArrayList<>();

        if (TextUtils.isEmpty(jsonString)) {
            return history;
        }

        try {
            JSONArray jsonArray = new JSONArray(jsonString);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject recordJson = jsonArray.getJSONObject(i);

                // 反序列化参数
                Map<String, Double> parameters = new HashMap<>();
                JSONObject paramsJson = recordJson.optJSONObject(KEY_PARAMETERS);
                if (paramsJson != null) {
                    Iterator<String> keys = paramsJson.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        parameters.put(key, paramsJson.getDouble(key));
                    }
                }

                // 反序列化计算结果（关键修复）
                JSONObject resultJson = recordJson.getJSONObject(KEY_RESULT);
                CalculationResult result = new CalculationResult(
                        resultJson.getDouble("adjustedHeartPlate"),
                        resultJson.getDouble("adjustedWearPlate"),
                        resultJson.getDouble("totalOriginal"),
                        resultJson.getDouble("totalAdjusted"),
                        resultJson.getDouble("extraAdjustedWearPlate"),
                        resultJson.getDouble("totalHeightPlan"),
                        resultJson.getDouble("originalHeartPlate"),
                        resultJson.getDouble("originalWearPlate"),
                        resultJson.getDouble("wheelOriginal"),
                        resultJson.getDouble("wheelAdjusted")
                );

                HistoryRecord record = new HistoryRecord(
                        recordJson.getLong(KEY_TIMESTAMP),
                        recordJson.getString(KEY_CAR_ID),
                        recordJson.getDouble(KEY_ADJUSTED_HEART_PLATE),
                        result.getAdjustedWearPlate(), // 从 result 中获取
                        parameters,
                        result
                );

                history.add(record);
            }
        } catch (JSONException e) {
            Log.e("DataUtils", "Error loading history", e);
        }

        return history;

    }
}
