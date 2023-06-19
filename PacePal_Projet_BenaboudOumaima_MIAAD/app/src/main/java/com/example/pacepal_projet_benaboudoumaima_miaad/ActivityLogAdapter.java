package com.example.pacepal_projet_benaboudoumaima_miaad;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ActivityLogAdapter extends RecyclerView.Adapter<ActivityLogAdapter.ViewHolder> {
    private List<ActivityLog> activityLogs;

    public ActivityLogAdapter(List<ActivityLog> activityLogs) {
        this.activityLogs = activityLogs;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_activity_log, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ActivityLog activityLog = activityLogs.get(position);

        holder.activityNameTextView.setText(activityLog.getActivityName());
        holder.dateTextView.setText(activityLog.getDate());
        holder.startTimeTextView.setText(activityLog.getStartTime());
        holder.endTimeTextView.setText(activityLog.getEndTime());
    }

    @Override
    public int getItemCount() {
        return activityLogs.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView activityNameTextView;
        TextView dateTextView;
        TextView startTimeTextView;
        TextView endTimeTextView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            activityNameTextView = itemView.findViewById(R.id.activity_name_text_view);
            dateTextView = itemView.findViewById(R.id.date_text_view);
            startTimeTextView = itemView.findViewById(R.id.start_time_text_view);
            endTimeTextView = itemView.findViewById(R.id.end_time_text_view);
        }
    }
}

