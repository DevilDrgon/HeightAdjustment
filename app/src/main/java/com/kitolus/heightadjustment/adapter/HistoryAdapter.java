package com.kitolus.heightadjustment.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kitolus.heightadjustment.models.HistoryRecord;
import com.kitolus.heightadjustment.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
    private final List<HistoryRecord> history;
    private final OnItemClickListener itemClickListener;
    private final OnDeleteClickListener deleteClickListener;

    public interface OnItemClickListener {
        void onItemClick(HistoryRecord record);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(HistoryRecord record);
    }

    public HistoryAdapter(List<HistoryRecord> history,
                          OnItemClickListener itemClickListener,
                          OnDeleteClickListener deleteClickListener) {
        this.history = new ArrayList<>(history);
        this.itemClickListener = itemClickListener;
        this.deleteClickListener = deleteClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoryRecord record = history.get(position);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

        holder.tvCarId.setText(record.getCarId());

        // 固定显示格式
        holder.tvHeartPlate.setText(String.format(Locale.getDefault(),
                "调整量: %.1f mm",  // 直接使用固定单位
                record.getAdjustedHeartPlate()
        ));

        holder.tvTimestamp.setText(sdf.format(record.getTimestamp()));

        holder.itemView.setOnClickListener(v -> itemClickListener.onItemClick(record));
        holder.btnDelete.setOnClickListener(v -> deleteClickListener.onDeleteClick(record));
    }

    public void removeItem(HistoryRecord record) {
        int position = history.indexOf(record);
        if (position != -1) {
            history.remove(position);
            notifyItemRemoved(position);
        }
    }

    @Override
    public int getItemCount() {
        return history.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCarId, tvHeartPlate, tvTimestamp;
        ImageButton btnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            tvCarId = itemView.findViewById(R.id.tvCarId);
            tvHeartPlate = itemView.findViewById(R.id.tvHeartPlate);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
