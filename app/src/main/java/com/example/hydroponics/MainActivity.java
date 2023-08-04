package com.example.hydroponics;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;

import com.example.hydroponics.databinding.ActivityMainBinding;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements Firestore.onRealtimeUpdateListener {
    private static final String TAG = MainActivity.class.getSimpleName();

    private ActivityMainBinding mBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        Firestore db = new Firestore(this);
        db.setup();
        db.addRealtimeUpdateListener();
    }

    public void onRealtimeUpdate(DocumentSnapshot snapshot) {
        Timestamp timestamp = snapshot.getTimestamp("timestamp");
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        Double tds = snapshot.getDouble("tdsValue");
        Double ph = snapshot.getDouble("phValue");
        Double temperature = snapshot.getDouble("temperature");
        Double humidity = snapshot.getDouble("humidity");
        Double tank = snapshot.getDouble("tankLevel");

        mBinding.timestampValue.setText(sdf.format(timestamp.toDate()));
        mBinding.tdsValue.setText(String.valueOf(tds));
        mBinding.phValue.setText(String.valueOf(ph));
        mBinding.temperatureValue.setText(String.valueOf(temperature));
        mBinding.humidityValue.setText(String.valueOf(humidity));
        mBinding.tankValue.setText(String.valueOf(tank));
    }
}