package com.example.hydroponics;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.hydroponics.databinding.ActivityMainBinding;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements Firestore.onRealtimeUpdateListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String COMMAND_REQUEST_URL = "https://asia-southeast2-hydroponics-378311.cloudfunctions.net/httpSendCommand";
    private static final int HARVEST_DAY = 25;
    private HandlerThread streamThread;
    private Handler streamHandler;
    private boolean initialized = false;
    private boolean isCommandButtonEnabled = false;
    private ActivityMainBinding mBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        Firestore db = new Firestore(this);
        db.setup();
        db.addRealtimeUpdateListener();

        streamThread = new HandlerThread("espcam");
        streamThread.start();
        streamHandler = new EspCamHandler(streamThread.getLooper());

        mBinding.commandButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isCommandButtonEnabled) {
                    JSONObject cmdMsg = new JSONObject();
                    try {
                        cmdMsg.put("deviceId", "HP01");
                        cmdMsg.put("cmdType", initialized ? 1 : 0);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    Log.d(TAG, "cmdMsg: " + cmdMsg);
                    sendCommand(cmdMsg);
                } else {
                    Toast.makeText(MainActivity.this, "Cannot harvest before day " + HARVEST_DAY, Toast.LENGTH_SHORT).show();
                }
            }
        });

//        mBinding.espCamIpText.setText("http://192.168.18.72:81");
        mBinding.espCamIpText.setText("https://167a-118-99-106-18.ngrok-free.app/");
        mBinding.connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                streamHandler.sendEmptyMessage(0);
            }
        });
    }

    private void sendCommand(JSONObject cmdMsg) {
        RequestQueue queue = Volley.newRequestQueue(this);
        mBinding.volleyProgressBar.setVisibility(View.VISIBLE);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, COMMAND_REQUEST_URL, cmdMsg, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                mBinding.volleyProgressBar.setVisibility(View.GONE);
//                Toast.makeText(MainActivity.this, "response: " + response, Toast.LENGTH_SHORT).show();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                mBinding.volleyProgressBar.setVisibility(View.GONE);
//                Toast.makeText(MainActivity.this, "error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
        queue.add(request);
    }

    public void onRealtimeUpdate(DocumentSnapshot snapshot) {
        Timestamp timestamp = snapshot.getTimestamp("timestamp");
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());

        Double tdsValue = snapshot.getDouble("tdsValue");
        Double phValue = snapshot.getDouble("phValue");
        Double temperature = snapshot.getDouble("temperature");
        Double humidity = snapshot.getDouble("humidity");
        Double tankLevel = snapshot.getDouble("tankLevel");

        initialized = Boolean.TRUE.equals(snapshot.getBoolean("initialized"));
        int elapsedDays = snapshot.getLong("elapsedDays").intValue();
        if (initialized) {
            isCommandButtonEnabled = elapsedDays >= HARVEST_DAY;
            mBinding.commandButton.setText(R.string.cmd_btn_harvest);
        } else {
            isCommandButtonEnabled = true;
            mBinding.commandButton.setText(R.string.cmd_btn_start);
        }
        mBinding.commandButton.setAlpha(isCommandButtonEnabled ? 1.0f : 0.5f);
        mBinding.elapsedDaysText.setText(getString(R.string.elapsed_days_text, elapsedDays));
        mBinding.timestampValue.setText(sdf.format(timestamp.toDate()));
        mBinding.tdsValue.setText(String.valueOf(tdsValue));
        mBinding.phValue.setText(String.valueOf(phValue));
        mBinding.temperatureValue.setText(String.valueOf(temperature));
        mBinding.humidityValue.setText(String.valueOf(humidity));
        mBinding.tankValue.setText(String.valueOf(tankLevel));
    }

    private void espCamGetStream() {
        String streamUrl = "http://192.168.18.72:81/stream";
        BufferedInputStream bis = null;
        FileOutputStream fos = null;
        try {
            URL url = new URL(streamUrl);
            try {
                HttpURLConnection huc = (HttpURLConnection) url.openConnection();
                huc.setRequestMethod("GET");
                huc.setConnectTimeout(1000 * 5);
                huc.setReadTimeout(1000 * 5);
                huc.setDoInput(true);
                huc.connect();
                if (huc.getResponseCode() == 200) {
                    InputStream in = huc.getInputStream();
                    InputStreamReader isr = new InputStreamReader(in);
                    BufferedReader br = new BufferedReader(isr);

                    String data;
                    int len;
                    byte[] buffer;
                    while ((data = br.readLine()) != null) {
                        if (data.contains("Content-Type:")) {
                            data = br.readLine();
                            len = Integer.parseInt(data.split(":")[1].trim());
                            bis = new BufferedInputStream(in);
                            buffer = new byte[len];

                            int t = 0;
                            while (t < len) {
                                t += bis.read(buffer, t, len - t);
                            }

                            Bytes2ImageFile(buffer, getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "/0A.jpg");
                            final Bitmap bitmap = BitmapFactory.decodeFile(getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "/0A.jpg");
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mBinding.espCamStreamView.setImageBitmap(bitmap);
                                }
                            });
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bis != null) {
                    bis.close();
                }
                if (fos != null) {
                    fos.close();
                }
                streamHandler.sendEmptyMessageDelayed(0, 3000);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void Bytes2ImageFile(byte[] bytes, String fileName) {
        try {
            File file = new File(fileName);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(bytes, 0, bytes.length);
            fos.flush();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class EspCamHandler extends Handler {
        public EspCamHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            espCamGetStream();
        }
    }
}