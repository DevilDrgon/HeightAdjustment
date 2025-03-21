package com.kitolus.heightadjustment.activity;

import android.app.AlertDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.kitolus.heightadjustment.adapter.HistoryAdapter;
import com.kitolus.heightadjustment.models.HistoryRecord;
import com.kitolus.heightadjustment.R;
import com.kitolus.heightadjustment.data.DataUtils;
import com.kitolus.heightadjustment.utils.CalculationResult;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity implements
        HistoryAdapter.OnItemClickListener,
        HistoryAdapter.OnDeleteClickListener {

    private static final SimpleDateFormat SDF =
            new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        new LoadHistoryTask().execute();
    }

    private class LoadHistoryTask extends AsyncTask<Void, Void, List<HistoryRecord>> {
        @Override
        protected List<HistoryRecord> doInBackground(Void... voids) {
            return DataUtils.loadHistory(HistoryActivity.this);
        }

        @Override
        protected void onPostExecute(List<HistoryRecord> history) {
            if (history.isEmpty()) {
                Toast.makeText(HistoryActivity.this, R.string.no_history, Toast.LENGTH_SHORT).show();
                finish();
            } else {
                recyclerView.setAdapter(new HistoryAdapter(history, HistoryActivity.this, HistoryActivity.this));
            }
        }
    }

    @Override
    public void onItemClick(HistoryRecord record) {
        showDetailDialog(record);
    }

    @Override
    public void onDeleteClick(HistoryRecord record) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_delete)
                .setMessage(R.string.delete_confirmation)
                .setPositiveButton(R.string.delete, (dialog, which) -> deleteRecord(record))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void deleteRecord(HistoryRecord record) {
        new DeleteHistoryTask().execute(record);
    }

    private class DeleteHistoryTask extends AsyncTask<HistoryRecord, Void, Boolean> {
        private HistoryRecord deletedRecord;

        @Override
        protected Boolean doInBackground(HistoryRecord... records) {
            deletedRecord = records[0];
            try {
                List<HistoryRecord> history = DataUtils.loadHistory(HistoryActivity.this);

                // 使用更精确的删除条件
                history.removeIf(r ->
                        r.getTimestamp() == deletedRecord.getTimestamp() &&
                                r.getCarId().equals(deletedRecord.getCarId())
                );

                DataUtils.saveHistory(HistoryActivity.this, history);
                return true;
            } catch (Exception e) {
                Log.e("HistoryActivity", "删除失败", e);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                HistoryAdapter adapter = (HistoryAdapter) recyclerView.getAdapter();
                if (adapter != null) {
                    adapter.removeItem(deletedRecord);
                    if (adapter.getItemCount() == 0) {
                        Toast.makeText(HistoryActivity.this, R.string.no_history, Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }
                Toast.makeText(HistoryActivity.this, R.string.delete_success, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(HistoryActivity.this, R.string.delete_failed, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showDetailDialog(HistoryRecord record) {
        CalculationResult result = record.getResult();
        String details = getString(R.string.detail_template,
                SDF.format(new Date(record.getTimestamp())),  // %1$s -> 时间
                record.getCarId(),                           // %2$s -> 车厢编号
                record.getCarNum(),                          // %3$s -> 车厢位数（应当保持为String）
                result.getWheelOriginal(),              // %5$.1f 原轮径
                result.getWheelAdjusted(),              // %6$.1f 调整后轮径
                result.getOriginalHeartPlate(),         // %7$.1f 原心盘
                record.getAdjustedHeartPlate(),         // %8$.1f 调整后心盘
                result.getOriginalWearPlate(),          // %9$.1f 原磨损板
                record.getAdjustedWearPlate(),          // %10$.1f 调整后磨损板
                result.getTotalHeightOriginal(),        // %11$.1f 调整前总高度
                result.getTotalHeightAdjusted(),        // %12$.1f 调整后总高度
                result.getExtraAdjustedWearPlate(),     // %13$.1f 修正高度
                result.getTotalHeightPlan()             // %14$.1f 目标高度
        );


        TextView content = new TextView(this);
        content.setText(details);
        content.setPadding(64, 32, 64, 32);

        new AlertDialog.Builder(this)
                .setTitle(R.string.history_detail)
                .setView(new ScrollView(this) {{
                    addView(content);
                }})
                .setPositiveButton(R.string.close, null)
                .show();
    }
}
