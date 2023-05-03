package com.example.magisterka;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements SensorEventListener, View.OnClickListener {

    private static final int REQUEST_CODE_LOCATION_PERMISSION = 100;
    private SensorManager sensorManager;
    private LocationManager locationManager;

    private Sensor gyroSensor;
    private Sensor accelSensor;

    TextView textViewActivity;

    Button buttonStairs;
    Button buttonLift;
    Button buttonFlat;

    private boolean collectingData;

    private Map<String, Object> gyroscopeData;
    private Map<String, Object> accelerometerData;
    private Map<String, Object> gpsData;

    FirebaseFirestore db;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textViewActivity = findViewById(R.id.textViewActivity);

        buttonStairs = findViewById(R.id.buttonStairs);
        buttonLift = findViewById(R.id.buttonLift);
        buttonFlat = findViewById(R.id.buttonFlat);

        buttonStairs.setOnClickListener(this);
        buttonLift.setOnClickListener(this);
        buttonFlat.setOnClickListener(this);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        db = FirebaseFirestore.getInstance();

        collectingData = false;
    }

    @Override
    public void onClick(View v) {
        collectingData = !collectingData;

        switch (v.getId()) {
            case R.id.buttonStairs:
                buttonFlat.setEnabled(!buttonFlat.isEnabled());
                buttonLift.setEnabled(!buttonLift.isEnabled());
                if (collectingData) {
                    buttonStairs.setText("Stop");
                    textViewActivity.setText("Schody");
                    startCollectingData();
                } else {
                    buttonStairs.setText("Schody");
                    textViewActivity.setText("");
                    stopCollectingData("schody");
                }
                break;
            case R.id.buttonLift:
                buttonFlat.setEnabled(!buttonFlat.isEnabled());
                buttonStairs.setEnabled(!buttonStairs.isEnabled());
                if (collectingData) {
                    buttonLift.setText("Stop");
                    textViewActivity.setText("Winda");
                    startCollectingData();
                } else {
                    buttonLift.setText("Winda");
                    textViewActivity.setText("");
                    stopCollectingData("winda");
                }
                break;
            case R.id.buttonFlat:
                buttonStairs.setEnabled(!buttonStairs.isEnabled());
                buttonLift.setEnabled(!buttonLift.isEnabled());
                if (collectingData) {
                    buttonFlat.setText("Stop");
                    textViewActivity.setText("Płasko");
                    startCollectingData();
                } else {
                    buttonFlat.setText("Płasko");
                    textViewActivity.setText("");
                    stopCollectingData("płasko");
                }
                break;
        }
    }

    private void stopCollectingData(String activity) {

        Map<String, Object> doc = new HashMap<>();
        doc.put("Aktywnosc", activity);

        // Add a new document with a generated ID
        db.collection("pomiary")
                .add(doc)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        String docId = documentReference.getId();
                        Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                        db.collection("pomiary").document(docId).collection("Żyroskop")
                                .add(gyroscopeData)
                                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                    @Override
                                    public void onSuccess(DocumentReference documentReference) {
                                        db.collection("pomiary").document(docId).collection("Akcelerometr")
                                                .add(accelerometerData)
                                                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                                    @Override
                                                    public void onSuccess(DocumentReference documentReference) {
                                                        db.collection("pomiary").document(docId).collection("GPS")
                                                                .add(gpsData)
                                                                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                                                    @Override
                                                                    public void onSuccess(DocumentReference documentReference) {
                                                                        showSimpleToast("Pomiar dodany do bazy danych.");
                                                                    }
                                                                })
                                                                .addOnFailureListener(new OnFailureListener() {
                                                                    @Override
                                                                    public void onFailure(@NonNull Exception e) {
                                                                        showSimpleToast("Pomiar NIE dodany do bazy danych.");
                                                                    }
                                                                });
                                                    }
                                                })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        showSimpleToast("Pomiar NIE dodany do bazy danych.");
                                                    }
                                                });
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        showSimpleToast("Pomiar NIE dodany do bazy danych.");
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        showSimpleToast("Pomiar NIE dodany do bazy danych.");
                    }
                });
    }

    private void showSimpleToast(String text) {
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }

    private void startCollectingData() {
        gyroscopeData = new HashMap<>();
        accelerometerData = new HashMap<>();
        gpsData = new HashMap<>();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_LOCATION_PERMISSION);
        }

        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if(collectingData && location != null) {
            Map<String, Double> values = new HashMap<>();
            Date currentTime = Calendar.getInstance().getTime();

            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            double altitude = location.getAltitude();

            values.put("latitude", latitude);
            values.put("longitude", longitude);
            values.put("altitude", altitude);

            gpsData.put(currentTime.toString(), values);
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListener);// Do something with location
    }

    LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            if(collectingData) {
                Map<String, Double> values = new HashMap<>();
                Date currentTime = Calendar.getInstance().getTime();

                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                double altitude = location.getAltitude();

                values.put("latitude", latitude);
                values.put("longitude", longitude);
                values.put("altitude", altitude);

                gpsData.put(currentTime.toString(), values);
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
        sensorManager.registerListener(this, gyroSensor, 1000000);

        sensorManager.registerListener(this, accelSensor, 1000000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {


        if(collectingData){
            Date currentTime = Calendar.getInstance().getTime();
            if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                Map<String, Object> values = new HashMap<>();
                values.put("X", event.values[0]);
                values.put("Y", event.values[1]);
                values.put("Z", event.values[2]);
                gyroscopeData.put(currentTime.toString(), values);
            }
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                Map<String, Object> values = new HashMap<>();
                values.put("X", event.values[0]);
                values.put("Y", event.values[1]);
                values.put("Z", event.values[2]);
                accelerometerData.put(currentTime.toString(), values);
            }
        }


    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}

