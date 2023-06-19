package com.example.pacepal_projet_benaboudoumaima_miaad;

import android.app.IntentService;
import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.util.ArrayList;
public class ActivityDetectionIntentService extends IntentService {
    public ActivityDetectionIntentService() {
        super(ActivityDetectionIntentService.class.getSimpleName());
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
        ArrayList<DetectedActivity> detectedActivities = (ArrayList<DetectedActivity>) result.getProbableActivities();


        broadcastActivity(detectedActivities);
    }

    private void broadcastActivity(ArrayList<DetectedActivity> detectedActivities) {
        ArrayList<Integer> activityTypes = new ArrayList<Integer>();
        ArrayList<Integer> confidences = new ArrayList<Integer>();
        for (DetectedActivity activity : detectedActivities) {
            activityTypes.add(activity.getType());
            confidences.add(activity.getConfidence());
        }
        Intent intent = new Intent(Constants.BROADCAST_DETECTED_ACTIVITY);
        intent.putIntegerArrayListExtra("Activités", activityTypes);
        intent.putIntegerArrayListExtra("Confidences", confidences);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}