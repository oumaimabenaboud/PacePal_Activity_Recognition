package com.example.pacepal_projet_benaboudoumaima_miaad;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ShowActivityOnMapFragment extends Fragment implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    private GoogleMap googleMap;
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;
    private MapView mapView;
    private LocationManager locationManager;
    private Marker marker;
    private Polyline polyline;
    private List<LatLng> polylinePoints;



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_show_activity_on_map, container, false);

        // Initialize the MapView
        mapView = view.findViewById(R.id.map_view);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        return view;
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;

        // Enable the necessary map settings
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        googleMap.setMyLocationEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);

        // Initialize the LocationManager
        locationManager = (LocationManager) requireActivity().getSystemService(Context.LOCATION_SERVICE);

        // Initialize the polyline and its list of points
        polylinePoints = new ArrayList<>();

        // Move the camera to the user's current location
        if (locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastKnownLocation != null) {
                LatLng latLng = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                marker = googleMap.addMarker(new MarkerOptions().position(latLng).title("My Location"));
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
            } else {
                // If the last known location is null, request location updates
                locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                        marker = googleMap.addMarker(new MarkerOptions().position(latLng).title("My Location"));
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                    }
                }, null);
            }
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        } else {
            requestLocationPermission();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();

        // Remove location updates
        if (locationManager != null) {
            locationManager.removeUpdates(locationListener);
        }
    }

    private void requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Show an explanation to the user
            Toast.makeText(requireContext(), "Location permission is required to show your location on the map",
                    Toast.LENGTH_LONG).show();
        }

        // Request the permission
        ActivityCompat.requestPermissions(requireActivity(),
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Location permission granted, start location updates
                startLocationUpdates();
            } else {
                // Location permission denied, handle accordingly (e.g., show a message or disable functionality)
                Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startLocationUpdates() {
        // Check if location services are enabled
        LocationManager locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            // Request location updates
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, locationListener);
        } else {
            // Location services are disabled, handle accordingly (e.g., show a message or prompt the user to enable)
            Toast.makeText(requireContext(), "Location services disabled", Toast.LENGTH_SHORT).show();
        }
    }

    private final LocationListener locationListener = new LocationListener() {

        private Location previousLocation;
        private long startTime;
        @Override
        public void onLocationChanged(Location location) {
            // Update the marker's position
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

            if (marker != null) {
                marker.setPosition(latLng);
            } else {
                marker = googleMap.addMarker(new MarkerOptions().position(latLng).title("My Location"));
            }

            // Add the updated position to the polyline
            polylinePoints.add(latLng);

            // Draw the polyline on the map
            if (polyline != null) {
                polyline.setPoints(polylinePoints);
            } else {
                polyline = googleMap.addPolyline(new PolylineOptions()
                        .addAll(polylinePoints)
                        .width(10)
                        .color(Color.parseColor("#105F52")));
            }
            // get activity name
            String activityName = displayLatestActivity();
            // Calculate speed
            double speed = calculateSpeed(location);
            // Calculate distance
            double distance = calculateDistance();

            // Update the information fields
            TextView activityText = requireView().findViewById(R.id.activity_text);
            TextView speedText = requireView().findViewById(R.id.speed_text);

            TextView distanceText = requireView().findViewById(R.id.distance_text);

            //activityText.setText("TEXT Activity:" + activityName);
            speedText.setText(String.format(Locale.getDefault(), "Speed: %.2f km/h", speed));
            distanceText.setText(String.format(Locale.getDefault(), "Distance Traveled: %.2f meters", distance));
        }


        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // Handle status changes of the location provider
        }

        @Override
        public void onProviderEnabled(String provider) {
            // Handle the case when the location provider is enabled
        }

        @Override
        public void onProviderDisabled(String provider) {
            // Handle the case when the location provider is disabled
        }

        private String displayLatestActivity() {
            String userId = mAuth.getCurrentUser().getUid();

            // Query the Firestore collection for the latest activity document of the current user
            final String[] activityName = new String[1]; // Declare the activityName variable outside the onSuccess method

            firestore.collection("activityData")
                    .whereEqualTo("userId", userId)
                    .orderBy("date", Query.Direction.DESCENDING)
                    .orderBy("startTime", Query.Direction.DESCENDING) // Order by start time in descending order
                    .limit(1)
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                        @Override
                        public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                            if (!queryDocumentSnapshots.isEmpty()) {
                                DocumentSnapshot documentSnapshot = queryDocumentSnapshots.getDocuments().get(0);

                                // Retrieve the activity name from the document
                                activityName[0] = documentSnapshot.getString("activityName"); // Assign the value to activityName
                                // Update the activityText TextView with the retrieved activity name
                                TextView activityText = requireView().findViewById(R.id.activity_text);
                                activityText.setText("Activity Detected: "+activityName[0]);
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(getActivity(), "Error occurred while retrieving activity data from Firestore", Toast.LENGTH_LONG).show();
                            // Error occurred while retrieving data from Firestore
                        }
                    });
            return activityName[0];
        }

        private double calculateSpeed(Location location) {
            if (previousLocation == null) {
                previousLocation = location;
                startTime = System.currentTimeMillis();
                return 0;
            }

            double distance = location.distanceTo(previousLocation);
            long deltaTime = System.currentTimeMillis() - startTime;
            double speed = (distance / deltaTime) * 1000 * 3.6; // Convert m/s to km/h

            previousLocation = location;
            startTime = System.currentTimeMillis();

            return speed;
        }

        private double calculateDistance() {
            double distance = 0;

            for (int i = 0; i < polylinePoints.size() - 1; i++) {
                LatLng startPoint = polylinePoints.get(i);
                LatLng endPoint = polylinePoints.get(i + 1);
                float[] results = new float[1];
                Location.distanceBetween(startPoint.latitude, startPoint.longitude, endPoint.latitude, endPoint.longitude, results);
                distance += results[0];
            }

            return distance;
        }
    };

}

