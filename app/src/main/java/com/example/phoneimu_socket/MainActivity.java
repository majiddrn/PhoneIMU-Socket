package com.example.phoneimu_socket;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity {

    private SensorManager sensorManager;
    private static final int MY_PERMISSIONS_REQUEST_INTERNET = 1;
    private Sensor gySensor;
    private SensorEventListener gySensorListener;
    private Socket socket;
    private PrintWriter socketOutput;
    private OutputStreamWriter socketOutputstreamWriter;
    private OutputStream socketOutputStream;
    private boolean serverConnected = false;
    private int rightLeftSignal;
    private char topDownSignal;
    final int dspWidowsSize = 20;
    final double movementsThreshold = 0.018;
    private final double[] rotationSignal0 = new double[dspWidowsSize];
    private final double[] rotationSignal1 = new double[dspWidowsSize];
    int iRotationSig;

    public MainActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void start(View view) {

    }

    private class Networking extends Thread {
        private MainActivity activity;
        private String ip;
        private int port;
        private int signal;
        private String task;

        public Networking(MainActivity activity, String ip, int port, String task) {
            this.activity = activity;
            this.ip = ip;
            this.port = port;
            this.task = task;
        }

        public Networking(MainActivity activity, String task, int signal) {
            this.activity = activity;
            this.task = task;
            this.signal = signal;
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void run() {
            if (task.compareTo("init") >= 0 && !this.activity.isServerConnected()) {
                try {
                    Socket socket = new Socket(ip, port);
                    ((TextView) findViewById(R.id.status)).setText("Connected");
                    this.activity.setSocketOutput(new PrintWriter(socket.getOutputStream()));
                    this.activity.setSocketOutputstreamWriter(new OutputStreamWriter(socket.getOutputStream()));
                    this.activity.setSocketOutputStream(socket.getOutputStream());
                    this.activity.setServerConnected(true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                byte[] bytes = ByteBuffer.allocate(4).putInt(signal).array();
                System.out.println(signal);
                try {
                    this.activity.getSocketOutputStream().write(bytes);
                    this.activity.getSocketOutputStream().flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
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
        iRotationSig = 0;
        sensorManager.unregisterListener(gySensorListener);
    }

}