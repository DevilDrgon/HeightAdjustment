package com.kitolus.heightadjustment.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.kitolus.heightadjustment.models.HistoryRecord;
import com.kitolus.heightadjustment.R;
import com.kitolus.heightadjustment.data.DataUtils;
import com.kitolus.heightadjustment.databinding.ActivityMainBinding;
import com.kitolus.heightadjustment.utils.CalculationResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    // 常量定义
    private static final double MIN_TOTAL_HEIGHT = 870;
    private static final double MAX_TOTAL_HEIGHT = 890;
    private static final double MIN_HEART_PLATE = 0;
    private static final double MAX_HEART_PLATE = 40;
    private static final double MIN_WEAR_PLATE = 8;
    private static final double MAX_WEAR_PLATE = 16;
    private static final double MIN_GAP = 30;

    // 视图绑定
    private ActivityMainBinding binding;
    private EditText etCarId, etWheelOriginal, etWheelAdjusted, etOriginalTotalHeight,
            etHeartPlateOriginal, etWearPlate, etGap;

    private List<HistoryRecord> history = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 初始化时加载历史记录
        history = new ArrayList<>(DataUtils.loadHistory(this)); // 加载已保存的记录

        initializeViews();
        setupButton();
    }

    private void initializeViews() {
        etCarId = binding.etCarId;
        etWheelOriginal = binding.etWheelOriginal;
        etWheelAdjusted = binding.etWheelAdjusted;
        etOriginalTotalHeight = binding.etOriginalTotalHeight;
        etHeartPlateOriginal = binding.etHeartPlateOriginal;
        etWearPlate = binding.etWearPlate;
        etGap = binding.etGap;
    }

    private void setupButton() {
        binding.btnCalculate.setOnClickListener(v -> validateAndCalculate());
        binding.btnViewHistory.setOnClickListener(v -> viewHistory());
    }

    // 检验并计算主方法
    private void validateAndCalculate() {
        try {
            // 验证车厢编号
            String carId = validateCarId();

            // 验证数据有效输入
            Map<String, Double> params = validateInputs();

            // 计算心盘高度
            CalculationResult result = calculateHeartPlate(params);

            // 将计算后的数据存储记录
            saveRecord(carId, params, result);

            // 展示结果页面
            showResultDialog(result);

        } catch (ValidationException e) {
            showError(e.getMessage());
        }
    }

    // 验证数据输入总和
    private Map<String, Double> validateInputs() throws ValidationException {
        Map<String, Double> params = new HashMap<>();

        params.put("WheelOriginal", parseInput(etWheelOriginal, R.string.error_wheel_original));
        params.put("WheelAdjusted", parseInput(etWheelAdjusted, R.string.error_wheel_adjusted));
        params.put("originalTotalHeight", parseInput(etOriginalTotalHeight, R.string.error_original_height));

        double heartPlateOriginal = parseInput(etHeartPlateOriginal, R.string.error_heart_plate_original);
        validateRange(heartPlateOriginal, MIN_HEART_PLATE, MAX_HEART_PLATE, R.string.error_heart_plate_range);
        params.put("heartPlateOriginal", heartPlateOriginal);

        double wearPlate = parseInput(etWearPlate, R.string.error_wear_plate);
        params.put("wearPlate", wearPlate);

        double gap = parseInput(etGap, R.string.error_gap);
        params.put("gap", gap);

        return params;
    }

    // 单独验证每个输入，并转化为有效值，同时返回每个数据对应的响应
    private double parseInput(EditText editText, int errorResId) throws ValidationException {
        String input = editText.getText().toString().trim();
        if (input.isEmpty()) {
            editText.requestFocus();
            throw new ValidationException(getString(R.string.field_required));
        }
        try {
            return Double.parseDouble(input);
        } catch (NumberFormatException e) {
            editText.requestFocus();
            throw new ValidationException(getString(errorResId));
        }
    }

    // 计算心盘高度，以及返回所有数据
    private CalculationResult calculateHeartPlate(Map<String, Double> params) {
        // 计算轮对调整量（保留方向）
        double wheelOriginal = params.get("WheelOriginal");
        double wheelAdjusted = params.get("WheelAdjusted");
        double heightDiff = (wheelOriginal - wheelAdjusted) / 2;

        // 获取其他参数
        double wearPlate = params.get("wearPlate");
        double gap = params.get("gap");
        double heartPlateOriginal = params.get("heartPlateOriginal");
        double totalHeightOriginal = params.get("originalTotalHeight");

        // 封装间隙并计算, 拆装封包 -> 间隙差数据（调整后的磨损板高度，磨损板高度差，磨损板以外修正高度）
        Double[] wearPlateResult = calculateWearPlate(new Double[]{wearPlate, gap});
        double adjustedWearPlate = wearPlateResult[0];
        double wearPlateDiff = wearPlateResult[1];
        double extraWearPlateAdjusted = wearPlateResult[2];

        // 计算目标总高度和高度差
        double totalHeightPlan = (MIN_TOTAL_HEIGHT + MAX_TOTAL_HEIGHT) / 2;
        double adjustedBaseHeight = totalHeightOriginal - heightDiff;
        double heightGap = totalHeightPlan - adjustedBaseHeight;

        // 计算调整后心盘高度
        double adjustedHeartPlateDiff = heightGap - wearPlateDiff * 2;
        double adjustedHeartPlate = heartPlateOriginal + adjustedHeartPlateDiff;

        // 验证心盘范围
        validateRange(adjustedHeartPlate, MIN_HEART_PLATE, MAX_HEART_PLATE, R.string.error_calculated_heart_plate);

        // 计算初步总高度
        double totalAdjusted = adjustedBaseHeight
                + adjustedHeartPlateDiff
                + extraWearPlateAdjusted;

        // 动态调整额外补偿值以确保总高度在范围内
        if (totalAdjusted < MIN_TOTAL_HEIGHT || totalAdjusted > MAX_TOTAL_HEIGHT) {
            double deltaAdjustment;
            if (totalAdjusted > MAX_TOTAL_HEIGHT) {
                // 总高度过高：通过减少额外补偿值（更负）降低总高度
                deltaAdjustment = totalAdjusted - MAX_TOTAL_HEIGHT;
                extraWearPlateAdjusted -= deltaAdjustment;
            } else {
                // 总高度过低：通过增加额外补偿值（更正）提高总高度
                deltaAdjustment = MIN_TOTAL_HEIGHT - totalAdjusted;
                extraWearPlateAdjusted += deltaAdjustment;
            }
            // 重新计算最终总高度
            totalAdjusted = adjustedBaseHeight
                    + adjustedHeartPlateDiff
                    + extraWearPlateAdjusted;
        }

        // 最终验证总高度范围
        validateTotalHeight(totalAdjusted, R.string.error_total_adjusted_range);

        return new CalculationResult(
                adjustedHeartPlate,
                adjustedWearPlate,
                totalHeightOriginal,
                totalAdjusted,
                extraWearPlateAdjusted,
                totalHeightPlan,
                heartPlateOriginal,
                wearPlate,
                wheelOriginal,
                wheelAdjusted
        );
    }

    // 检验车厢编号是否为空，并返回响应
    private String validateCarId() throws ValidationException {
        String carId = etCarId.getText().toString().trim();
        if (carId.isEmpty()) {
            etCarId.requestFocus();
            throw new ValidationException(getString(R.string.error_car_id_empty));
        }
        return carId;
    }

    // 验证间隙值，并返回修改后的磨损板高度、差值及额外补偿
    private Double[] calculateWearPlate(Double[] wearPlateAndGap) {
        double wearPlate = wearPlateAndGap[0];
        double gap = wearPlateAndGap[1];

        double wearPlateAdjusted;
        double wearPlateDiff;
        double extraAdjustedWearPlate;

        // 符合无需调整的条件：磨损板在8-16之间且间隙≥30
        if (wearPlate >= MIN_WEAR_PLATE && wearPlate <= MAX_WEAR_PLATE && gap >= MIN_GAP) {
            return new Double[]{
                    wearPlate,
                    0.0,
                    0.0
            };
        }

        // 调整磨损板到有效范围
        if (wearPlate < MIN_WEAR_PLATE) {
            // 过低：补到8
            wearPlateAdjusted = MIN_WEAR_PLATE;
            wearPlateDiff = MIN_WEAR_PLATE - wearPlate;
        } else if (wearPlate > MAX_WEAR_PLATE) {
            // 过高：降到16
            wearPlateAdjusted = MAX_WEAR_PLATE;
            wearPlateDiff = MAX_WEAR_PLATE - wearPlate;
        } else {
            // 范围正常（但间隙不足）
            wearPlateAdjusted = wearPlate;
            wearPlateDiff = 0.0;
        }

        // 计算调整后的间隙（原间隙 + 磨损板变化量）
        double adjustedGap = gap - wearPlateDiff;

        // 计算额外补偿（负值表示需要增加补偿量）
        extraAdjustedWearPlate = adjustedGap < MIN_GAP
                ? adjustedGap - MIN_GAP  // 产生负补偿
                : 0.0;

        return new Double[]{
                wearPlateAdjusted,
                wearPlateDiff,
                extraAdjustedWearPlate
        };
    }

    private void validateRange(double value, double min, double max, int errorResId) throws ValidationException {
        if (value < min || value > max) {
            throw new ValidationException(getString(errorResId, min, max));
        }
    }

    private void validateTotalHeight(double total, int errorResId) throws ValidationException {
        if (total < MIN_TOTAL_HEIGHT || total > MAX_TOTAL_HEIGHT) {
            throw new ValidationException(getString(errorResId, MIN_TOTAL_HEIGHT, MAX_TOTAL_HEIGHT));
        }
    }

    private void saveRecord(String carId, Map<String, Double> params, CalculationResult result) {
        HistoryRecord record = new HistoryRecord(
                System.currentTimeMillis(),
                carId,
                result.getAdjustedHeartPlate(),
                result.getAdjustedWearPlate(),
                new HashMap<>(params),
                result
        );
        history.add(0, record);
        DataUtils.saveHistory(this, history);
        Toast.makeText(this, "记录已保存", Toast.LENGTH_SHORT).show();
    }


    private void showResultDialog(CalculationResult result) {
        String message = String.format(Locale.getDefault(),
            "调整后心盘高度: %.1f mm\n" +
                    "调整后磨损板高度: %.1f mm\n" +
                    "调整后修正高度: %.1f mm\n" +
                    "原钩高高度: %.1f mm\n" +
                    "当前钩高高度: %.1f mm\n" +
                    "目标高度: %.1f mm",
                result.getAdjustedHeartPlate(),
                result.getAdjustedWearPlate(),
                result.getExtraAdjustedWearPlate(),
                result.getTotalHeightOriginal(),
                result.getTotalHeightAdjusted(),
                result.getTotalHeightPlan()
        );

        new AlertDialog.Builder(this)
                .setTitle("计算结果")
                .setMessage(message)
                .setPositiveButton("确定", null)
                .setNeutralButton("查看历史", (d, w) -> viewHistory())
                .show();
    }

    private void viewHistory() {
        startActivity(new Intent(this, HistoryActivity.class));
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    // 独立异常类
    static class ValidationException extends RuntimeException {
        public ValidationException(String message) {
            super(message);
        }
    }
}