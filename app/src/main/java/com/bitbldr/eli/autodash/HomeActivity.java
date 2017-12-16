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
        com.bitbldr.eli.autodash.MapFragment.OnFragmentInteractionListener {

    // (10 min * 60 s/min * 1000 ms/s) = 600000ms
    private static final int WEATHER_UPDATE_INTERVAL_MS = 600000;
    // Boot logo duration = 2s
    private static final int BOOT_LOGO_DURATION_MS = 2000;
//    // Google maps request location flag
//    private static final int REQUEST_LOCATION = 1;
//
//    // Google Map
//    private GoogleMap googleMap;

    private SensorManager sensorManager;

    private Sensor compassSensor;
    private Sensor accelerometerSensor;
    private Sensor magneticFieldSensor;

    private final float[] mAccelerometerReading = new float[3];
    private final float[] mMagnetometerReading = new float[3];

    private final float[] mRotationMatrix = new float[9];
    private final float[] mOrientationAngles = new float[3];

    private boolean isMuting = false;

    WifiManager wifiManager;
    WifiConnectionReceiver wifiConnectionReceiver;
    WifiScanReceiver wifiScanReceiver;
    int selectedNetwork;

    String primaryWifiSSID = "my wifi";
    String primaryWifiPassphrase = "passphrase";

    String mobileWifiSSID = "my wifi";
    String mobileWifiPassphrase = "passphrase";

    View welcomeView;
    View mainView;
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

    private String getWeatherConditionsIcon(String condition, boolean isNight) {
        HashMap<String, String> weatherIconMap = new HashMap<>();

        if (isNight) {
            weatherIconMap.put("chanceflurries", "{wic-day-snow-wind}");
            weatherIconMap.put("chancerain", "{wic-night-alt-rain}");
            weatherIconMap.put("chancesleet", "{wic-night-alt-rain-mix}");
            weatherIconMap.put("chancesnow", "{wic-night-alt-snow-wind}");
            weatherIconMap.put("chancetstorms", "{wic-storm-showers}");
            weatherIconMap.put("mostlycloudy", "{wic-cloudy}");
            weatherIconMap.put("mostlysunny", "{wic-night-alt-cloudy}");
            weatherIconMap.put("partlycloudy", "{wic-cloud}");
            weatherIconMap.put("partlysunny", "{wic-night-alt-cloudy}");
            weatherIconMap.put("clear", "{wic-night-clear}");
            weatherIconMap.put("cloudy", "{wic-cloudy}");
            weatherIconMap.put("flurries", "{wic-snow-wind}");
            weatherIconMap.put("fog", "{wic-fog}");
            weatherIconMap.put("hazy", "{wic-cloudy-windy}");
            weatherIconMap.put("sleet", "{wic-rain-mix}");
            weatherIconMap.put("rain", "{wic-raindrops}");
            weatherIconMap.put("snow", "{wic-snowflake-cold}");
            weatherIconMap.put("sunny", "{wic-night-clear}");
            weatherIconMap.put("tstorms", "{wic-thunderstorm}");
        }
        else {
            weatherIconMap.put("chanceflurries", "{wic-day-snow-wind}");
            weatherIconMap.put("chancerain", "{wic-day-rain}");
            weatherIconMap.put("chancesleet", "{wic-day-sleet}");
            weatherIconMap.put("chancesnow", "{wic-day-snow}");
            weatherIconMap.put("chancetstorms", "{wic-day-storm-showers}");
            weatherIconMap.put("mostlycloudy", "{wic-cloudy}");
            weatherIconMap.put("mostlysunny", "{wic-day-sunny-overcast}");
            weatherIconMap.put("partlycloudy", "{wic-cloud}");
            weatherIconMap.put("partlysunny", "{wic-day-cloudy}");
            weatherIconMap.put("clear", "{wic-day-sunny}");
            weatherIconMap.put("cloudy", "{wic-cloudy}");
            weatherIconMap.put("flurries", "{wic-snow-wind}");
            weatherIconMap.put("fog", "{wic-fog}");
            weatherIconMap.put("hazy", "{wic-cloudy-windy}");
            weatherIconMap.put("sleet", "{wic-rain-mix}");
            weatherIconMap.put("rain", "{wic-raindrops}");
            weatherIconMap.put("snow", "{wic-snowflake-cold}");
            weatherIconMap.put("sunny", "{wic-day-sunny}");
            weatherIconMap.put("tstorms", "{wic-thunderstorm}");
        }

        if (weatherIconMap.get(condition) == null) {
            return "{wic-na}";
        }

        return weatherIconMap.get(condition);
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

    private void updateClock() {
        Date date = new Date();
        TimeZone tz = TimeZone.getTimeZone("America/New_York");
        Calendar now = GregorianCalendar.getInstance(tz);
        now.setTime(date);
        int daylightSavingsOffset = tz.inDaylightTime(date) ? 0 : 1;

        String hour = "" + ((now.get(Calendar.HOUR_OF_DAY) % 12) + 1 - daylightSavingsOffset);
        hour = hour.equals("0") ? "12" : hour;

        String minute = now.get(Calendar.MINUTE) < 10
                ? "0" + now.get(Calendar.MINUTE)
                : "" + now.get(Calendar.MINUTE);

        String ampm = (now.get(Calendar.AM_PM) == 0 ? " AM" : " PM");

        String currentTime = hour + ":" + minute + ampm;

        Button clockButton = findViewById(R.id.clockButton);
        clockButton.setText(currentTime);

        initNextClockUpdate();
    }

    private long getMSUntilNextClockTick() {
        long currentMS = System.currentTimeMillis();

        return (60 - ((currentMS / 1000) % 60)) * 1000;
    }

    private void initNextClockUpdate() {
        new android.os.Handler().postDelayed(
            new Runnable() {
                public void run() {
                    updateClock();
                }
            },
        getMSUntilNextClockTick());
    }

    private void updateWeather() {
        new RetrieveWeatherTask().execute();

        initNextWeatherUpdate();
    }

    private void initNextWeatherUpdate() {
        new android.os.Handler().postDelayed(
            new Runnable() {
                public void run() {
                    updateWeather();
                }
            },
            WEATHER_UPDATE_INTERVAL_MS
        );
    }

    @Override
    public void onFragmentInteraction(Uri uri) {
        // handle fragment interactions
    }

    public class RetrieveWeatherTask extends AsyncTask<Void, Void, String> {
        protected void onPreExecute() {
            // do nothing, for now
        }

        protected String doInBackground(Void... urls) {
            try {
                String API_KEY = "175638bf5ad25874";
                URL url = new URL("http://api.wunderground.com/api/" + API_KEY + "/conditions/q/PA/Pittsburgh.json");
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                try {
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuilder.append(line).append("\n");
                    }
                    bufferedReader.close();
                    return stringBuilder.toString();
                }
                finally{
                    urlConnection.disconnect();
                }
            }
            catch(Exception e) {
                Log.e("ERROR", e.getMessage(), e);
                return null;
            }
        }

        protected void onPostExecute(String response) {
            if(response == null) {
                Log.e("ERROR", "Weather response is null");
                return;
            }

            Button currentWeatherButton = findViewById(R.id.weatherButton);

            try {
                JSONObject object = (JSONObject) new JSONTokener(response).nextValue();
                int currentTemp_f = object.getJSONObject("current_observation").getInt("temp_f");
                String currentConditions = object.getJSONObject("current_observation").getString("icon");

                currentWeatherButton.setText(currentTemp_f + "° " + getWeatherConditionsIcon(currentConditions, false));
            } catch (JSONException e) {
                // Appropriate error handling code
            }
        }
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

    public void onMuteButtonClick(View v) {
        IconicsButton muteButton = findViewById(R.id.muteButton);

        if (isMuting) {
            UnMuteAudio();
            muteButton.setText("{faw-volume-off}\nMUTE");
            muteButton.setTextColor(Color.WHITE);
        }
        else {
            MuteAudio();
            muteButton.setText("{faw-volume-off}\nMUTING");
            muteButton.setTextColor(Color.RED);
        }

        isMuting = !isMuting;
    }

    public void onBackupButtonClick(View v) {
        Utils.StartNewActivity(this, "com.android.camera2");
    }

    public void onVoiceButtonClick(View v) {
        Utils.StartNewActivity(this, "com.google.android.googlequicksearchbox");
    }

    public void onClockButtonClick(View v) {
        Utils.StartNewActivity(this, "com.google.android.deskclock");
    }

    public void onWebButtonClick(View v) {
        Utils.StartNewActivity(this, "com.android.chrome");
    }

    public void onAppsButtonClick(View v) {
        Intent i = new Intent(this, AppsListActivity.class);
        startActivity(i);
    }

    public void onSettingsButtonClick(View v) {
        Utils.StartNewActivity(this, "com.android.settings");
    }

    public void onWeatherButtonClick(View v) {
        Utils.StartNewActivity(this, "com.wunderground.android.weather");
    }

    public void MuteAudio(){
        AudioManager mAlramMAnager = (AudioManager) getSystemService(this.AUDIO_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mAlramMAnager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0);
        } else {
            mAlramMAnager.setStreamMute(AudioManager.STREAM_MUSIC, true);
        }
    }

    public void UnMuteAudio(){
        AudioManager audioManager = (AudioManager) getSystemService(this.AUDIO_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE,0);
        } else {
            audioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
        }
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

        updateClock();
        updateWeather();
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
