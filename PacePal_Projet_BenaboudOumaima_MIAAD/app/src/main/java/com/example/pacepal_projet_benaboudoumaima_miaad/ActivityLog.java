package com.example.pacepal_projet_benaboudoumaima_miaad;

public class ActivityLog {
    private String activityName;
    private String date;
    private String startTime;
    private String endTime;

    public ActivityLog() {
    }

    public ActivityLog(String activityName, String date, String startTime, String endTime) {
        this.activityName = activityName;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getActivityName() {
        return activityName;
    }

    public String getDate() {
        return date;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }
}
