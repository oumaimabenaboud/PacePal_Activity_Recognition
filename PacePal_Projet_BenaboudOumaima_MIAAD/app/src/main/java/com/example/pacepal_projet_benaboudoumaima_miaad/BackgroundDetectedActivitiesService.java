package com.example.pacepal_projet_benaboudoumaima_miaad;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
public class BackgroundDetectedActivitiesService extends Service {
    Intent intentService;
    ActivityRecognitionClient activityRecognitionClient;
    PendingIntent mPendingIntent;
    private final IBinder mBinder = new MyBinder();

    public class MyBinder extends Binder {
        BackgroundDetectedActivitiesService getService() {
            return BackgroundDetectedActivitiesService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        activityRecognitionClient = new ActivityRecognitionClient(this);
        intentService = new Intent(this, ActivityDetectionIntentService.class);
        mPendingIntent = PendingIntent.getService(
                this,
                1,
                intentService,
                PendingIntent.FLAG_MUTABLE
        );

        requestActivityUpdatesButtonHandler();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    private void requestActivityUpdatesButtonHandler() {
        Task<Void> task = activityRecognitionClient.requestActivityUpdates(
                Constants.DETECTION_INTERVAL_IN_MILLISECONDS,
                mPendingIntent
        );
        task.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Toast.makeText(
                        getApplicationContext(),
                        "Successfully requested activity updates",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(
                        getApplicationContext(),
                        "Requesting activity updates failed to start",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    private void removeActivityUpdatesButtonHandler() {
        Task<Void> task = activityRecognitionClient.removeActivityUpdates(mPendingIntent);
        task.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Toast.makeText(
                        getApplicationContext(),
                        "Removed activity updates successfully!",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(
                        getApplicationContext(),
                        "Failed to remove activity updates!",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        removeActivityUpdatesButtonHandler();
    }
}
