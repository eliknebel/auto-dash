package com.bitbldr.eli.autodash;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.SensorEventListener;
import android.media.AudioManager;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.app.Activity;
import android.content.Intent;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.mikepenz.iconics.context.IconicsContextWrapper;
import com.mikepenz.iconics.view.IconicsButton;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;

public class HomeActivity extends Activity implements SensorEventListener,
        MediaBarFragment.OnFragmentInteractionListener,
        com.bitbldr.eli.autodash.MapFragment.OnFragmentInteractionListener,
        StatusBarFragment.OnFragmentInteractionListener {

    // Application views
    View welcomeView;
    View mainView;

    // Boot logo duration = 2s
    private static final int BOOT_LOGO_DURATION_MS = 2000;

    private SensorManager sensorManager;

    private Sensor compassSensor;
    private Sensor accelerometerSensor;
    private Sensor magneticFieldSensor;

    private final float[] mAccelerometerReading = new float[3];
    private final float[] mMagnetometerReading = new float[3];

    private final float[] mRotationMatrix = new float[9];
    private final float[] mOrientationAngles = new float[3];

    WifiManager wifiManager;
    WifiConnectionReceiver wifiConnectionReceiver;
    WifiScanReceiver wifiScanReceiver;
    int selectedNetwork;

    String primaryWifiSSID = "my wifi";
    String primaryWifiPassphrase = "passphrase";

    String mobileWifiSSID = "my wifi";
    String mobileWifiPassphrase = "passphrase";

    private boolean isColdStart = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        inflateAllViews();
        showWelcomeView();
//        showMainView();       // for debugging

        this.checkPermissions();
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(IconicsContextWrapper.wrap(newBase));
    }

    private void checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Check Permissions Now
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    com.bitbldr.eli.autodash.MapFragment.REQUEST_LOCATION);
        }
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == com.bitbldr.eli.autodash.MapFragment.REQUEST_LOCATION) {
            if(grantResults.length == 1
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // We can now safely use the API we requested access to

            } else {
                // Permission was denied or request was cancelled
            }
        }
    }

    @Override
    public void onFragmentInteraction(Uri uri) {
        // handle fragment interactions
    }

    class WifiScanReceiver extends BroadcastReceiver
    {
        public void onReceive(Context c, Intent intent)
        {

            ArrayList<String> connections = new ArrayList<>();
            ArrayList<Float> signalStrength = new ArrayList<>();

            List<ScanResult> wifiList;
            wifiList = wifiManager.getScanResults();
            for(int i = 0; i < wifiList.size(); i++)
            {
                connections.add(wifiList.get(i).SSID);
            }
        }
    }

    class WifiConnectionReceiver extends BroadcastReceiver
    {
        public void onReceive(Context c, Intent intent)
        {
            Toast.makeText(getApplicationContext(), wifiManager.getWifiState(), Toast.LENGTH_SHORT).show();
        }
    }

    public class PowerConnectedReciever extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
//            Toast.makeText(HomeActivity.this, "Power Connected", Toast.LENGTH_SHORT).show();

            wifiManager.setWifiEnabled(true);
            connectToWifiNetwork(primaryWifiSSID, primaryWifiPassphrase);

            // open this app on power connect
            startActivity(new Intent(context, HomeActivity.class));
        }

    }

    public class PowerDisconnectedReciever extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
//            Toast.makeText(HomeActivity.this, "Power Disconnected", Toast.LENGTH_SHORT) .show();

            wifiManager.disableNetwork(selectedNetwork);
            wifiManager.removeNetwork(selectedNetwork);
            wifiManager.disconnect();
            wifiManager.setWifiEnabled(false);

            isColdStart = true;
        }

    }

    public void initSensors() {
        // for the system's orientation sensor registered listeners
        sensorManager.registerListener(this, compassSensor, SensorManager.SENSOR_DELAY_GAME);

//        sensorManager.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_GAME);

        sensorManager.registerListener(this, accelerometerSensor,
                SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, magneticFieldSensor,
                SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
    }

    public void initWifi() {
        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

//        wifiScanReceiver = new WifiScanReceiver();
//        registerReceiver(wifiScanReceiver, new IntentFilter(
//                WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        wifiConnectionReceiver = new WifiConnectionReceiver();
        registerReceiver(wifiScanReceiver, new IntentFilter(
                WifiManager.NETWORK_STATE_CHANGED_ACTION));

        if(wifiManager.isWifiEnabled()==false)
        {
            wifiManager.setWifiEnabled(true);
        }

//        scanForWifiNetworks();
        connectToWifiNetwork(primaryWifiSSID, primaryWifiPassphrase);
    }

    public void setFullscreenMode() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
    }

    public void inflateAllViews() {
        if (welcomeView == null) {
            welcomeView = getLayoutInflater().inflate(R.layout.welcome, null);
        }

        if (mainView == null) {
            mainView = getLayoutInflater().inflate(R.layout.activity_home, null);
        }
    }

    public void showWelcomeView() {
        setContentView(welcomeView);
    }

    public void showMainView() {
        setContentView(mainView);

        // initialize your android device sensor capabilities
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        compassSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magneticFieldSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        setFullscreenMode();

        initSensors();
        initWifi();

        IconicsButton muteButton = findViewById(R.id.muteButton);
        muteButton.setText("{faw-volume-off}\nMUTE");

        registerReceiver(new PowerConnectedReciever(), new IntentFilter(Intent.ACTION_POWER_CONNECTED));
        registerReceiver(new PowerDisconnectedReciever(), new IntentFilter(Intent.ACTION_POWER_DISCONNECTED));
    }

    @Override
    protected void onResume() {
        if (isColdStart) {
            inflateAllViews();
            showWelcomeView();

            new Handler().postDelayed(
                    new Runnable() {
                        public void run() {
                            showMainView();
                            isColdStart = false;
                        }
                    },
                    BOOT_LOGO_DURATION_MS
            );
        }
        else {
            showMainView();
        }
//        inflateAllViews();        // for debugging
//        showMainView();

        setFullscreenMode();

        super.onResume();
    }

    @Override
    public void onPause() {
        // to stop the listener and save battery
        sensorManager.unregisterListener(this);

//        unregisterReceiver(wifiScanReceiver);

        super.onPause();
    }

    private String degreesToHeading(double deg) {
        final String[] HEADINGS = new String[]{"N", "NE", "E", "SE", "S", "SW", "W", "NW"};

        int sector = (int) Math.floor(((deg + 22) % 360) / 45);

        return HEADINGS[sector];
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == compassSensor) {
            // get the angle around the z-axis rotated
            float degree = Math.round(event.values[0]);

            TextView compassTextView = findViewById(R.id.compassTextView);
            compassTextView.setText("{faw-compass} " + degreesToHeading(degree));
        }
        if (event.sensor == accelerometerSensor) {
            System.arraycopy(event.values, 0, mAccelerometerReading,
                    0, mAccelerometerReading.length);
        }
        else if (event.sensor == magneticFieldSensor) {
            System.arraycopy(event.values, 0, mMagnetometerReading,
                    0, mMagnetometerReading.length);
        }

        updateOrientationAngles();
        TextView pitchTextView = findViewById(R.id.pitchTextView);
        TextView rollTextView = findViewById(R.id.rollTextView);
        TextView yawTextView = findViewById(R.id.yawTextView);


        pitchTextView.setText(String.format("%.1f", mOrientationAngles[0]));
        rollTextView.setText(String.format("%.1f", mOrientationAngles[1]));
        yawTextView.setText(String.format("%.1f", mOrientationAngles[2]));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // not in use
    }

    // Compute the three orientation angles based on the most recent readings from
    // the device's accelerometer and magnetometer.
    public void updateOrientationAngles() {
        // Update rotation matrix, which is needed to update orientation angles.
        sensorManager.getRotationMatrix(mRotationMatrix, null,
                mAccelerometerReading, mMagnetometerReading);

        // "mRotationMatrix" now has up-to-date information.

        sensorManager.getOrientation(mRotationMatrix, mOrientationAngles);

        // "mOrientationAngles" now has up-to-date information.
    }

    public void scanForWifiNetworks() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run()
            {
                wifiManager.startScan();
                scanForWifiNetworks();
            }
        }, 1000);
    }

    public void connectToWifiNetwork(String networkSSID, String networkPass) {
        WifiConfiguration conf = new WifiConfiguration();
        conf.SSID = "\"" + networkSSID + "\"";

//        // WEP Network
//        conf.wepKeys[0] = "\"" + networkPass + "\"";
//        conf.wepTxKeyIndex = 0;
//        conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
//        conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);

        // WPA Network
        conf.preSharedKey = "\""+ networkPass +"\"";

//      // Open Network
//        conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);

        wifiManager.addNetwork(conf);

        List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
        for( WifiConfiguration i : list ) {
            if(i.SSID != null && i.SSID.equals("\"" + networkSSID + "\"")) {
                selectedNetwork = i.networkId;
                wifiManager.disconnect();
                wifiManager.enableNetwork(selectedNetwork, true);
                wifiManager.reconnect();

                break;
            }
        }
    }

}
