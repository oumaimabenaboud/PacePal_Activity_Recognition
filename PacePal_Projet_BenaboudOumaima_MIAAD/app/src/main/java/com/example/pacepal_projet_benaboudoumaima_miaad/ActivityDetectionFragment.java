package com.example.pacepal_projet_benaboudoumaima_miaad;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.Manifest;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ActivityDetectionFragment extends Fragment {

    private BroadcastReceiver broadcastReceiver;
    private FirebaseFirestore firestore;

    private FirebaseAuth mAuth;
    private Button startButton;
    private Button stopButton;
    private int totalConfidence = 100;
    private boolean isActivityOngoing = false;
    private int previousActivityType = -1;


    public ActivityDetectionFragment() {
        // Required empty public constructor
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_activity_detection, container, false);

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        startButton = view.findViewById(R.id.start_button);
        stopButton = view.findViewById(R.id.stop_button);
        requestActivityRecognitionPermission();

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Constants.BROADCAST_DETECTED_ACTIVITY)) {
                    ArrayList<Integer> activityTypes = intent.getIntegerArrayListExtra("Activités");
                    ArrayList<Integer> confidences = intent.getIntegerArrayListExtra("Confidences");
                    userActivityOutput(activityTypes, confidences);
                }
            }
        };

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startTracking();
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopTracking();
            }
        });
        return view;
    }
    private void startTracking() {
        Intent intent = new Intent(requireContext(), BackgroundDetectedActivitiesService.class);
        requireContext().startService(intent);
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
    }

    private void stopTracking() {
        Intent intent = new Intent(requireContext(), BackgroundDetectedActivitiesService.class);
        requireContext().stopService(intent);
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(broadcastReceiver,
                new IntentFilter(Constants.BROADCAST_DETECTED_ACTIVITY));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(broadcastReceiver);
    }

    private void userActivityOutput(ArrayList<Integer> activityTypes, ArrayList<Integer> confidences) {
        int sumConfidences = 0;
        int walkingConfidence = 0;
        int onFootConfidence = 0;
        int stillConfidence = 0;
        int runningConfidence = 0;
        int maxConfidence = 0;
        int maxConfidenceType = -1;
        int maxConfidenceIndex = -1;
        for (int i = 0; i < activityTypes.size(); i++) {
            int type = activityTypes.get(i);
            int confidence = confidences.get(i);
            switch (type) {
                case DetectedActivity.WALKING:
                    walkingConfidence = confidence;
                    break;
                case DetectedActivity.ON_FOOT:
                    onFootConfidence = confidence;
                    break;
                case DetectedActivity.STILL:
                    stillConfidence = confidence;
                    break;
                case DetectedActivity.RUNNING:
                    runningConfidence = confidence;
                    break;
            }
        }
        sumConfidences = walkingConfidence + onFootConfidence + stillConfidence + runningConfidence;

        if (sumConfidences != totalConfidence) {
            float factor = (float) totalConfidence / (sumConfidences + 1);
            walkingConfidence = Math.round(walkingConfidence * factor);
            onFootConfidence = Math.round(onFootConfidence * factor);
            stillConfidence = Math.round(stillConfidence * factor);
            runningConfidence = totalConfidence - walkingConfidence - onFootConfidence - stillConfidence;
        }
        TableLayout tableLayout = getView().findViewById(R.id.activity_table);
        TextView standingTextView = getView().findViewById(R.id.standing_percentage_text_view);
        standingTextView.setText(String.valueOf(onFootConfidence) + "%");
        TextView sittingTextView = getView().findViewById(R.id.sitting_percentage_text_view);
        sittingTextView.setText(String.valueOf(stillConfidence) + "%");
        TextView walkingTextView = getView().findViewById(R.id.walking_percentage_text_view);
        walkingTextView.setText(String.valueOf(walkingConfidence) + "%");
        TextView jumpingTextView = getView().findViewById(R.id.jumping_percentage_text_view);
        jumpingTextView.setText(String.valueOf(runningConfidence) + "%");


        // Find the activity type with the highest confidence
        if (walkingConfidence > maxConfidence) {
            maxConfidence = walkingConfidence;
            maxConfidenceType = DetectedActivity.WALKING;
        }
        if (onFootConfidence > maxConfidence) {
            maxConfidence = onFootConfidence;
            maxConfidenceType = DetectedActivity.ON_FOOT;
        }
        if (stillConfidence > maxConfidence) {
            maxConfidence = stillConfidence;
            maxConfidenceType = DetectedActivity.STILL;
        }
        if (runningConfidence > maxConfidence) {
            maxConfidence = runningConfidence;
            maxConfidenceType = DetectedActivity.RUNNING;
        }

        if (maxConfidenceType != -1) {

            if (maxConfidenceType != previousActivityType) {
                // Detected activity has changed, end the previous activity
                if (isActivityOngoing) {
                    // Get the current date and time
                    Date currentDate = new Date();
                    SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                    String endTimeString = timeFormat.format(currentDate);

                    // Update the Firestore document with the end time
                    //updateActivityEndTime(endTimeString);


                    isActivityOngoing = false;
                }
            }

            String activityName = getActivityName(maxConfidenceType);

            if (!isActivityOngoing) {
                // Get the current date and time
                Date currentDate = new Date();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                String dateString = dateFormat.format(currentDate);
                String startTimeString = timeFormat.format(currentDate);

                // Save the activity data to Firestore
                saveActivityData(activityName, dateString, startTimeString);
                isActivityOngoing = true;
            }
            previousActivityType = maxConfidenceType;
        } else {
            // No activity with confidence above threshold detected
            if (isActivityOngoing) {
                // Get the current date and time

                // Update the Firestore document with the end time
                //updateActivityEndTime(endTimeString);

                isActivityOngoing = false;
            }
        }
    }


    private void saveActivityData(String activityName, String dateString, String startTimeString) {
        String userId = mAuth.getCurrentUser().getUid();
        // Create a data object to be stored in Firestore
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("activityName", activityName);
        data.put("date", dateString);
        data.put("startTime", startTimeString);
        data.put("endTime", ""); // Initially set end time to empty

        // Query the Firestore collection for the ongoing activity document of the current user
        firestore.collection("activityData")
                .whereEqualTo("userId", userId)
                .whereEqualTo("endTime", "")
                .orderBy("date", Query.Direction.DESCENDING)
                .orderBy("startTime", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            DocumentSnapshot documentSnapshot = queryDocumentSnapshots.getDocuments().get(0);
                            String documentId = documentSnapshot.getId();

                            // Get the current date and time
                            Date currentDate = new Date();
                            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                            String endTimeString = timeFormat.format(currentDate);

                            // Update the Firestore document with the end time
                            firestore.collection("activityData").document(documentId)
                                    .update("endTime", endTimeString)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            // End time successfully updated in Firestore
                                            // Save the new activity data
                                            saveNewActivityData(data);
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Toast.makeText(getActivity(), "Error occurred while updating end time in Firestore", Toast.LENGTH_LONG).show();
                                            // Error occurred while updating end time in Firestore
                                        }
                                    });
                        } else {
                            // No ongoing activity found, save the new activity data directly
                            saveNewActivityData(data);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getActivity(), "Error occurred while querying ongoing activity in Firestore", Toast.LENGTH_LONG).show();
                        // Error occurred while querying ongoing activity in Firestore
                    }
                });
    }

    private void saveNewActivityData(Map<String, Object> data) {
        firestore.collection("activityData").add(data)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        // Data successfully stored in Firestore
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getActivity(), "Error occurred while storing Activity data in Firestore", Toast.LENGTH_LONG).show();
                        // Error occurred while storing data in Firestore
                    }
                });
    }




    private String getActivityName(int activityType) {
        switch (activityType) {
            case DetectedActivity.WALKING:
                return "Walking";
            case DetectedActivity.ON_FOOT:
                return "On Foot";
            case DetectedActivity.STILL:
                return "Still";
            case DetectedActivity.RUNNING:
                return "Running";
            default:
                return "Unknown";
        }
    }
    private void requestActivityRecognitionPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACTIVITY_RECOGNITION)
                == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, 101);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (grantResults.length > 0) {
                for (int i = 0; i < grantResults.length; i++) {
                    String permission = permissions[i];
                    if (Manifest.permission.ACTIVITY_RECOGNITION.equalsIgnoreCase(permission)) {
                        if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                            // you now have permission
                        }
                    }
                }
            }
        }
    }
}
