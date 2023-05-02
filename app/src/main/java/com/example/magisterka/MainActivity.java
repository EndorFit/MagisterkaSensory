package com.example.magisterka;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
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

    private SensorManager sensorManager;
    private Sensor gyroSensor;
    private Sensor accelSensor;

    TextView textViewActivity;

    Button buttonStairs;
    Button buttonLift;
    Button buttonFlat;

    private boolean collectingData;

    private Map<String, Object> gyroscopeData;
    private Map<String, Object> accelerometerData;

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
                if(collectingData){
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
                if(collectingData){
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
                if(collectingData){
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

    private void showSimpleToast(String text) {
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }

    private void startCollectingData() {
        gyroscopeData = new HashMap<>();
        accelerometerData = new HashMap<>();
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Date currentTime = Calendar.getInstance().getTime();

        if(collectingData){
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

