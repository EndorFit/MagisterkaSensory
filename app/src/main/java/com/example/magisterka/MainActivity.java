package com.example.magisterka;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements SensorEventListener, View.OnClickListener, View.OnTouchListener {

    private static final int REQUEST_CODE_LOCATION_PERMISSION = 100;
    private SensorManager sensorManager;
    private LocationManager locationManager;

    private Sensor gyroSensor;
    private Sensor accelSensor;

    TextView textViewActivity;

    Button buttonStairs;
    Button buttonLift;
    Button buttonFlat;
    Button buttonRamp;

    private boolean collectingData;

    private Map<String, Object> gyroscopeData;
    private Map<String, Object> accelerometerData;
    private Map<String, Object> gpsData;

    FirebaseFirestore db;

    private long touched = 0;

    private long eventGyro = 0;
    private long eventAccel = 0;

    @SuppressLint({"MissingInflatedId", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        textViewActivity = findViewById(R.id.textViewActivity);

        buttonStairs = findViewById(R.id.buttonStairs);
        buttonLift = findViewById(R.id.buttonLift);
        buttonFlat = findViewById(R.id.buttonFlat);
        buttonRamp = findViewById(R.id.buttonRamp);

        buttonStairs.setOnClickListener(this);
        buttonLift.setOnClickListener(this);
        buttonFlat.setOnClickListener(this);
        buttonRamp.setOnClickListener(this);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        db = FirebaseFirestore.getInstance();

        collectingData = false;

        buttonStairs.setOnTouchListener(this);
        buttonLift.setOnTouchListener(this);
        buttonFlat.setOnTouchListener(this);
        buttonRamp.setOnTouchListener(this);
    }

    @Override
    public void onClick(View v) {
        if(!collectingData){
            performButtonActions(v);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if(collectingData){
            if(motionEvent.getAction() == MotionEvent.ACTION_DOWN){
                touched = (Long) System.currentTimeMillis();
            }
            else if(motionEvent.getAction() == MotionEvent.ACTION_UP){
                if(((Long) System.currentTimeMillis() - touched) > 1500){
                    touched = 0;
                    performButtonActions(view);
                    return true;
                }
                touched = 0;
            }
        }
        return false;
    }

    @SuppressLint({"NonConstantResourceId", "SetTextI18n"})
    public void performButtonActions(View v){
        collectingData = !collectingData;

        switch (v.getId()) {
            case R.id.buttonStairs:
                buttonFlat.setEnabled(!buttonFlat.isEnabled());
                buttonLift.setEnabled(!buttonLift.isEnabled());
                buttonRamp.setEnabled(!buttonRamp.isEnabled());
                if (collectingData) {
                    buttonStairs.setText("Stop");
                    textViewActivity.setText("Schody");
                    startCollectingData();
                } else {
                    buttonStairs.setText("Schody");
                    textViewActivity.setText("");
                    showConfirmationDialog("schody");
                }
                break;
            case R.id.buttonLift:
                buttonFlat.setEnabled(!buttonFlat.isEnabled());
                buttonStairs.setEnabled(!buttonStairs.isEnabled());
                buttonRamp.setEnabled(!buttonRamp.isEnabled());
                if (collectingData) {
                    buttonLift.setText("Stop");
                    textViewActivity.setText("Winda");
                    startCollectingData();
                } else {
                    buttonLift.setText("Winda");
                    textViewActivity.setText("");
                    showConfirmationDialog("winda");
                }
                break;
            case R.id.buttonFlat:
                buttonStairs.setEnabled(!buttonStairs.isEnabled());
                buttonLift.setEnabled(!buttonLift.isEnabled());
                buttonRamp.setEnabled(!buttonRamp.isEnabled());
                if (collectingData) {
                    buttonFlat.setText("Stop");
                    textViewActivity.setText("Płasko");
                    startCollectingData();
                } else {
                    buttonFlat.setText("Płasko");
                    textViewActivity.setText("");
                    showConfirmationDialog("płasko");
                }
                break;
            case R.id.buttonRamp:
                buttonStairs.setEnabled(!buttonStairs.isEnabled());
                buttonLift.setEnabled(!buttonLift.isEnabled());
                buttonFlat.setEnabled(!buttonFlat.isEnabled());
                if (collectingData) {
                    buttonRamp.setText("Stop");
                    textViewActivity.setText("Podjazd");
                    startCollectingData();
                } else {
                    buttonRamp.setText("Podjazd");
                    textViewActivity.setText("");
                    showConfirmationDialog("podjazd");
                }
                break;
        }
    }

    private void startCollectingData() {

        gyroscopeData = new LinkedHashMap<>();
        accelerometerData = new LinkedHashMap<>();
        gpsData = new LinkedHashMap<>();


        while(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_LOCATION_PERMISSION);
        }

        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if(collectingData && location != null) {
            Map<String, Double> values = new HashMap<>();

            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            double altitude = location.getAltitude();

            values.put("latitude", latitude);
            values.put("longitude", longitude);
            values.put("altitude", altitude);

            gpsData.put(getCurrentTimeStamp(), values);
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 250, 0, locationListener);// Do something with location
    }

    private void stopCollectingData(String activity) {
        Map<String, Object> doc = new HashMap<>();
        doc.put("Aktywnosc", activity);

        // Add a new document with a generated ID
        db.collection("pomiaryWstepne")
                .add(doc)
                .addOnSuccessListener(documentReference -> {
                    String docId = documentReference.getId();
                    db.collection("pomiaryWstepne").document(docId).collection("Żyroskop")
                            .add(gyroscopeData)
                            .addOnSuccessListener(documentReference12 -> db.collection("pomiaryWstepne")
                                    .document(docId).collection("Akcelerometr")
                                    .add(accelerometerData)
                                    .addOnSuccessListener(documentReference1 -> db.collection("pomiaryWstepne")
                                            .document(docId).collection("GPS")
                                            .add(gpsData)
                                            .addOnSuccessListener(documentReference11 -> showSimpleToast("Pomiar dodany do bazy danych."))
                                            .addOnFailureListener(e -> showSimpleToast("Pomiar NIE dodany do bazy danych.")))
                                    .addOnFailureListener(e -> showSimpleToast("Pomiar NIE dodany do bazy danych.")))
                            .addOnFailureListener(e -> showSimpleToast("Pomiar NIE dodany do bazy danych."));
                })
                .addOnFailureListener(e -> showSimpleToast("Pomiar NIE dodany do bazy danych."));
    }

    private void showConfirmationDialog(String activity) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("Potwierdzenie");
        alertDialogBuilder.setMessage("Czy na pewno chcesz zapisać?");
        alertDialogBuilder.setPositiveButton("Tak", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Obsługa kliknięcia przycisku "Tak"
                stopCollectingData(activity);
                dialog.dismiss();
            }
        });
        alertDialogBuilder.setNegativeButton("Nie", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Obsługa kliknięcia przycisku "Nie"
                dialog.dismiss();
            }
        });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private void showSimpleToast(String text) {
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }

    @SuppressLint("SimpleDateFormat")
    public String getCurrentTimeStamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
    }

    LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            if(collectingData) {
                Map<String, Double> values = new HashMap<>();

                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                double altitude = location.getAltitude();

                values.put("latitude", latitude);
                values.put("longitude", longitude);
                values.put("altitude", altitude);

                gpsData.put(getCurrentTimeStamp(),values);
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}

        @Override
        public void onProviderEnabled(String provider) {}

        @Override
        public void onProviderDisabled(String provider) {}
    };


    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, gyroSensor, 500000);

        sensorManager.registerListener(this, accelSensor,500000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {


        if(collectingData){
            if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {

                if(((Long) System.currentTimeMillis() - eventGyro) > 500){
                    eventGyro = (Long) System.currentTimeMillis();
                    Map<String, Object> values = new HashMap<>();
                    values.put("X", event.values[0]);
                    values.put("Y", event.values[1]);
                    values.put("Z", event.values[2]);

                    gyroscopeData.put(getCurrentTimeStamp(),values);
                }
            }
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

                if(((Long) System.currentTimeMillis() - eventAccel) > 500) {
                    eventAccel = (Long) System.currentTimeMillis();
                    Map<String, Object> values = new HashMap<>();
                    values.put("X", event.values[0]);
                    values.put("Y", event.values[1]);
                    values.put("Z", event.values[2]);

                    accelerometerData.put(getCurrentTimeStamp(), values);
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}

