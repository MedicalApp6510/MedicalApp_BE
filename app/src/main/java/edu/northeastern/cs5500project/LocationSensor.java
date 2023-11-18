package edu.northeastern.cs5500project;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

public class LocationSensor  extends AppCompatActivity {
    private FusedLocationProviderClient mFusedLocationClient;
    private TextView txtLocation;
    private TextView txtLatitude;
    private TextView txtLongtitude;
    private TextView txtReminder;
    private int locationRequestCode = 1000;
    private double wayLatitude = 0.0, wayLongitude = 0.0;
    public LocationRequest locationRequest;
    public LocationCallback locationCallback;

    private Location lastMovedLocation;
    private Location lastLocation;

    private SensorManager sensorManager;

    private SensorEventListener sensorListener;
    private HandlerThread locationThread;

    private double totalD=0;
    private TextView txtDistance;

    private Button mStartButton;
    private Button mPauseButton;
    private Button mResetButton;
    private Chronometer mChronometer;
    private long lastPause;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);
        this.txtLocation = (TextView) findViewById(R.id.txtLocation);
        this.txtDistance = (TextView) findViewById(R.id.txtDistance);
        this.txtReminder = (TextView) findViewById(R.id.txtReminder);
        this.mStartButton=(Button)findViewById(R.id.start);
        this.mPauseButton=(Button)findViewById(R.id.pause);
        this.mResetButton=(Button)findViewById(R.id.reset);
        this.mChronometer=(Chronometer) findViewById(R.id.chronometer);

        mStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (lastPause != 0){
                    mChronometer.setBase(mChronometer.getBase() + SystemClock.elapsedRealtime() - lastPause);
                }
                else{
                    mChronometer.setBase(SystemClock.elapsedRealtime());
                }

                mChronometer.start();
                mStartButton.setEnabled(false);
                mPauseButton.setEnabled(true);
            }
        });

        mPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                lastPause = SystemClock.elapsedRealtime();
                mChronometer.stop();
                mPauseButton.setEnabled(false);
                mStartButton.setEnabled(true);
            }
        });

        mResetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mChronometer.stop();
                mChronometer.setBase(SystemClock.elapsedRealtime());
                lastPause = 0;
                mStartButton.setEnabled(true);
                mPauseButton.setEnabled(false);
            }
        });

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY,1000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(1000)
                .build();
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                Location location =locationResult.getLastLocation();
                if (location != null) {
                    lastLocation = location;
                    wayLatitude = location.getLatitude();
                    wayLongitude = location.getLongitude();
                }
            }
        };

        startWithPermissions();
    }

    private void startWithPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, locationRequestCode);
        } else {
            startLocationThread();
            setupSensor();
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1000: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startLocationThread();
                    setupSensor();
                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }


    private void startLocationThread() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, locationRequestCode);
        } else {
            locationThread = new HandlerThread("LocationThread");
            locationThread.start();

            mFusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    locationThread.getLooper());
        }
    }

    private void setupSensor() {
        sensorListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                Sensor sensor = sensorEvent.sensor;
                if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    // https://developer.android.com/reference/android/hardware/SensorEvent

                    float x = sensorEvent.values[0];  // in cm
                    float y = sensorEvent.values[1];
                    float z = sensorEvent.values[2];

                    // check the movement, no need to do anything if movement is too small
                    // try with different values
                    if (x > 1 || y > 1 || z > 1) {
                        if (lastMovedLocation != null) {
                            double distance = lastLocation.distanceTo(lastMovedLocation); // in meter
                            // add to your total distance
                            totalD+=distance;
                        }
                        lastMovedLocation = lastLocation;
                    }

//                    txtLocation.setText("Latitude and longtitude are "+wayLatitude+"and " + wayLongitude);
//                    txtDistance.setText("Total distance is "+totalD);
                    if(lastMovedLocation!=null) {
                        txtDistance.setText("You have walked around for " + Math.round(totalD * 100.0) / 100.0 + "meters while doing fitness");
                    }
                    if (totalD<10){
                        txtReminder.setText("Good Focus Time!");
                    }
                    else{
                        txtReminder.setText("Don't Walk Away! Stay Focused!");
                    }
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {
            }
        };

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorManager.registerListener(
                sensorListener,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onBackPressed() {
        // remember to ask user
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (locationThread != null) {
            mFusedLocationClient.removeLocationUpdates(locationCallback);
            locationThread.getLooper().quit();
            locationThread.interrupt();
        }
        if (sensorManager != null) {
            sensorManager.unregisterListener(sensorListener);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startWithPermissions();
    }
//    private void startWithPermissions() {
//        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
//                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION}, locationRequestCode);
//        } else {
//            startLocationThread();
//            setupSensor();
//        }
//    }
//
//    @SuppressLint("MissingPermission")
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        switch (requestCode) {
//            case 1000: {
//                // If request is cancelled, the result arrays are empty.
//                if (grantResults.length > 0
//                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    startLocationThread();
//                    setupSensor();
//                } else {
//                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
//                }
//                break;
//            }
//        }
//    }
//
//
//    private void startLocationThread() {
//        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
//                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, locationRequestCode);
//        } else {
//            locationThread = new HandlerThread("LocationThread");
//            locationThread.start();
//
//            mFusedLocationClient.requestLocationUpdates(locationRequest,
//                    locationCallback,
//                    locationThread.getLooper());
//        }
//    }
//
//    private void setupSensor() {
//        sensorListener = new SensorEventListener() {
//            @Override
//            public void onSensorChanged(SensorEvent sensorEvent) {
//                Sensor sensor = sensorEvent.sensor;
//                if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
//                    // https://developer.android.com/reference/android/hardware/SensorEvent
//
//                    float x = sensorEvent.values[0];  // in cm
//                    float y = sensorEvent.values[1];
//                    float z = sensorEvent.values[2];
//
//                    // check the movement, no need to do anything if movement is too small
//                    // try with different values
//                    if (x > 1 || y > 1 || z > 1) {
//                        if (lastMovedLocation != null) {
//                            double distance = lastLocation.distanceTo(lastMovedLocation); // in meter
//                            // add to your total distance
//                            totalD+=distance;
//                        }
//                        lastMovedLocation = lastLocation;
//                    }
//
////                    txtLocation.setText("Latitude and longtitude are "+wayLatitude+"and " + wayLongitude);
////                    txtDistance.setText("Total distance is "+totalD);
//
////                    txtLocation.setText("");
//                    txtDistance.setText("You have walked around for "+Math.round(totalD * 100.0) / 100.0 + "meters while doing fitness");
//                    if (totalD<10){
//                        txtReminder.setText("Good Focus Time!");
//                    }
//                    else{
//                        txtReminder.setText("Don't Walk Away! Stay Focused!");
//                    }
//                }
//            }
//
//            @Override
//            public void onAccuracyChanged(Sensor sensor, int i) {
//            }
//        };
//
//        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
//        sensorManager.registerListener(
//                sensorListener,
//                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
//                SensorManager.SENSOR_DELAY_NORMAL);
//    }
//
////    @Override
////    public void onBackPressed() {
////        // remember to ask user
////    }
//
//    @Override
//    protected void onPause() {
//        super.onPause();
//        if (locationThread != null) {
//            mFusedLocationClient.removeLocationUpdates(locationCallback);
//            locationThread.getLooper().quit();
//            locationThread.interrupt();
//        }
//        if (sensorManager != null) {
//            sensorManager.unregisterListener(sensorListener);
//        }
//    }
//
//    @Override
//    protected void onResume() {
//        super.onResume();
//        startWithPermissions();
//    }
}
