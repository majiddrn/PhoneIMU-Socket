package com.example.phoneimu_socket;

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
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {

    private SensorManager sensorManager;
    private static final int MY_PERMISSIONS_REQUEST_INTERNET = 1;
    private Sensor gySensor;
    private Sensor acSensor;
    private Sensor orSensor;
    private SensorEventListener sensorListener;
    private Socket socket;
    private PrintWriter socketOutput;
    private OutputStreamWriter socketOutputstreamWriter;
    private DataOutputStream outputStream;
    private OutputStream socketOutputStream;
    private boolean serverConnected = false;
    final int dspWidowsSize = 20;
    final double movementsThreshold = 0.018;
    private final double[][] sensorSignalsGyro = new double[dspWidowsSize][3];
    private final double[][] sensorSignalsAc = new double[dspWidowsSize][3];
    private final double[][] sensorSignalsOrien = new double[dspWidowsSize][3];
    int iteratorGyro;
    int iteratorAc;
    int iteratorOrien;

    final Object lock = new Object();

    public MainActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, MY_PERMISSIONS_REQUEST_INTERNET);
        }

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        gySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        acSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        orSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);

        DSP dsp = new DSP();

        sensorListener = new SensorEventListener() {
            @SuppressLint({"SetTextI18n", "SuspiciousIndentation"})
            @Override
            public void onSensorChanged(SensorEvent event) {


                System.out.println(event.sensor.getType());

                if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                    if (iteratorGyro < sensorSignalsGyro.length) {
                        sensorSignalsGyro[iteratorGyro][0] = event.values[0];
                        sensorSignalsGyro[iteratorGyro][1] = event.values[1];
                        sensorSignalsGyro[iteratorGyro][2] = event.values[2];

                        iteratorGyro++;
                        if (iteratorGyro < sensorSignalsGyro.length)
                            iteratorGyro = 0;
                    } else {
                        iteratorGyro = 0; // Reset the iterator to avoid going out of bounds
                    }
                } else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    if (iteratorAc < sensorSignalsGyro.length) {
                        sensorSignalsAc[iteratorAc][0] = event.values[0];
                        sensorSignalsAc[iteratorAc][1] = event.values[1];
                        sensorSignalsAc[iteratorAc][2] = event.values[2];
//                        if (iteratorAc != dspWidowsSize)
                        iteratorAc++;
                        if (iteratorAc < sensorSignalsAc.length)
                            iteratorAc = 0;
                    } else
                        iteratorAc = 0;
                } else if (event.sensor.getType() == Sensor.TYPE_ORIENTATION) {
                    if (iteratorOrien < sensorSignalsGyro.length) {
                        sensorSignalsOrien[iteratorOrien][0] = event.values[0];
                        sensorSignalsOrien[iteratorOrien][1] = event.values[1];
                        sensorSignalsOrien[iteratorOrien][2] = event.values[2];
//                        if (iteratorOrien != dspWidowsSize - 1)
                            iteratorOrien++;
                        if (iteratorOrien < sensorSignalsOrien.length)
                            iteratorOrien = 0;
                    } else
                        iteratorOrien = 0;
                }



                if (serverConnected) {

                    System.out.println(iteratorAc);

                    double[][] normalized = new double[3][3];

                    if (iteratorGyro < dspWidowsSize){
                        normalized[0][0] = sensorSignalsGyro[iteratorGyro][0];
                        normalized[0][1] = sensorSignalsGyro[iteratorGyro][1];
                        normalized[0][2] = sensorSignalsGyro[iteratorGyro][2];
                    }

                    if (iteratorAc < dspWidowsSize){
                        normalized[1][0] = sensorSignalsAc[iteratorAc][0];
                        normalized[1][1] = sensorSignalsAc[iteratorAc][1];
                        normalized[1][2] = sensorSignalsAc[iteratorAc][2];
                    }

                    if (iteratorOrien < dspWidowsSize){
                        normalized[2][0] = sensorSignalsOrien[iteratorOrien][0];
                        normalized[2][1] = sensorSignalsOrien[iteratorOrien][1];
                        normalized[2][2] = sensorSignalsOrien[iteratorOrien][2];
                    }

                    JSONObject jsonObject = new JSONObject();

                    try {
                        JSONArray jsonArray = new JSONArray(normalized);

                        jsonObject.put("normalized", jsonArray);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    String jsonString = jsonObject.toString();

                    Thread thread = new Thread(new Networking(getActivity(), "signal", jsonString));
                    System.out.println("here");
                    thread.start();
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };

    }

    public void start(View view) {
        String serverIP = ((EditText) findViewById(R.id.ipAddress)).getText().toString();
        Thread thread = new Thread(new Networking(this, serverIP, 9999, "init"));
        thread.start();
        System.out.println("should be connected now");
    }

    private class Networking extends Thread {
        private MainActivity activity;
        private String ip;
        private int port;
        private String signals;
        private String task;

        public Networking(MainActivity activity, String ip, int port, String task) {
            this.activity = activity;
            this.ip = ip;
            this.port = port;
            this.task = task;
        }

        public Networking(MainActivity activity, String task, String signals) {
            this.activity = activity;
            this.task = task;
            this.signals = signals;
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void run() {
            if (task.compareTo("init") >= 0 && !this.activity.isServerConnected()) {
                try {
                    Socket socket = new Socket(ip, port);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((TextView) findViewById(R.id.status)).setText("Connected");
                        }
                    });

                    this.activity.setSocketOutput(new PrintWriter(socket.getOutputStream()));
                    this.activity.setSocketOutputstreamWriter(new OutputStreamWriter(socket.getOutputStream()));
                    this.activity.setSocketOutputStream(socket.getOutputStream());
                    this.activity.setServerConnected(true);
                    this.activity.setOutputStream(new DataOutputStream(this.activity.getSocketOutputStream()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {

                synchronized (this.activity.lock) {
                    try {
                        System.out.println(signals);
                        this.activity.getOutputStream().writeUTF(signals);
                        this.activity.getOutputStream().flush();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_REQUEST_INTERNET) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission was granted, you can perform your network operations
            } else {
                // permission was denied, show an appropriate message or take other action
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        iteratorGyro = 0;
        iteratorAc = 0;
        iteratorOrien = 0;
        sensorManager.registerListener(sensorListener, gySensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(sensorListener, orSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(sensorListener, acSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public DataOutputStream getOutputStream() {
        return outputStream;
    }

    public void setOutputStream(DataOutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public void setSocketOutput(PrintWriter socketOutput) {
        this.socketOutput = socketOutput;
    }

    public boolean isServerConnected() {
        return serverConnected;
    }

    public OutputStream getSocketOutputStream() {
        return socketOutputStream;
    }

    public void setSocketOutputStream(OutputStream socketOutputStream) {
        this.socketOutputStream = socketOutputStream;
    }

    public void setServerConnected(boolean serverConnected) {
        this.serverConnected = serverConnected;
    }

    public OutputStreamWriter getSocketOutputstreamWriter() {
        return socketOutputstreamWriter;
    }

    public void setSocketOutputstreamWriter(OutputStreamWriter socketOutputstream) {
        this.socketOutputstreamWriter = socketOutputstream;
    }


    @Override
    protected void onPause() {
        super.onPause();
        iteratorGyro = 0;
        iteratorAc = 0;
        iteratorOrien = 0;
        sensorManager.unregisterListener(sensorListener);
        sensorManager.unregisterListener(sensorListener);
        sensorManager.unregisterListener(sensorListener);
    }

    public MainActivity getActivity() {
        return this;
    }

}