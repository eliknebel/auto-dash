package com.bitbldr.eli.autodash;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.SensorEventListener;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.app.Activity;
import android.content.Intent;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.mikepenz.fontawesome_typeface_library.FontAwesome;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.iconics.context.IconicsContextWrapper;
import com.mikepenz.iconics.view.IconicsButton;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;

public class HomeActivity extends Activity implements SensorEventListener {

    private static final int REQUEST_LOCATION = 1;

    // (10 min * 60 s/min * 1000 ms/s) = 600000ms
    private static final int WEATHER_UPDATE_INTERVAL_MS = 600000;

    // Google Map
    private GoogleMap googleMap;

    private SensorManager sensorManager;

    private Sensor compassSensor;
    private Sensor accelerometerSensor;
    private Sensor magneticFieldSensor;

    private MediaChangeReciever mediaChangeReciever = new MediaChangeReciever();

    private final float[] mAccelerometerReading = new float[3];
    private final float[] mMagnetometerReading = new float[3];

    private final float[] mRotationMatrix = new float[9];
    private final float[] mOrientationAngles = new float[3];

    private int currentTrackPosition = 0;
    private int currentTrackLength = 1;
    private Timer mediaPositionUpdater;

    private boolean isMuting = false;

    WifiManager wifiManager;
    WifiConnectionReceiver wifiConnectionReceiver;
    WifiScanReceiver wifiScanReceiver;
    int selectedNetwork;

    String primaryWifiSSID = "my wifi";
    String primaryWifiPassphrase = "passphrase";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        requestWindowFeature(Window.FEATURE_NO_TITLE);
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_home);

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
                    REQUEST_LOCATION);
        }
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_LOCATION) {
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

    private void startMediaPositionUpdater() {
        if (mediaPositionUpdater == null) {
            new android.os.Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        mediaPositionUpdater = new Timer();
                        mediaPositionUpdater.scheduleAtFixedRate(new TimerTask() {
                            @Override
                            public void run() {
                                // Get a handler that can be used to post to the main thread
                                Handler mainHandler = new Handler(Looper.getMainLooper());

                                Runnable myRunnable = new Runnable() {
                                    @Override
                                    public void run() {
                                        currentTrackPosition += 1000;

                                        ProgressBar mediaPositionProgressBar = findViewById(R.id.mediaPositionProgressBar);
                                        TextView mediaTrackPositionTextView = findViewById(R.id.mediaTrackPositionTextView);

                                        mediaPositionProgressBar.setProgress(currentTrackPosition);
                                        mediaTrackPositionTextView.setText(String.format("%d", (currentTrackPosition / 60000))
                                                + ":" + doubleDigitFormat(String.format("%d", ((currentTrackPosition / 1000) % 60))));
                                    }
                                };
                                mainHandler.post(myRunnable);
                            }
                        }, 0, 1000);
                    }
                }, getMSUntilNextMediaProgressTick()
            );
        }
    }

    private void stopMediaPositionUpdater() {
        if (mediaPositionUpdater != null) {
            mediaPositionUpdater.cancel();
            mediaPositionUpdater = null;
        }
    }

    private long getMSUntilNextMediaProgressTick() {
        Log.d("LOG", "getMSUntilNextMediaProgressTick " + (1000 - (currentTrackPosition % 1000)));
        return (1000 - (currentTrackPosition % 1000));
    }


    private HomeActivity getActivityInstance() {
        return this;
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

    public class RetrieveAlbumArtworkTask extends AsyncTask<String, Void, String> {

        protected String doInBackground(String... urls) {

            try {
                URL url = new URL(urls[0]);
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
                Log.e("ERROR", "Album info response is null");
                return;
            }

            try {
                JSONObject object = (JSONObject) new JSONTokener(response).nextValue();
                String albumArtUrl = object.getJSONObject("album")
                        .getJSONArray("image")
                        .getJSONObject(2)
                        .getString("#text");

                new DownloadAlbumArtworkTask().execute(albumArtUrl);

            } catch (JSONException e) {
                Log.e("ERROR", e.getMessage(), e);
            }
        }
    }

    private class DownloadAlbumArtworkTask extends AsyncTask<String, Void, Bitmap> {

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;

            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }

            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            ImageView mediaAlbumArtworkImageView = findViewById(R.id.mediaAlbumArtworkImageView);

            if (result == null) {
                clearAlbumArtwork();
                return;
            }

            mediaAlbumArtworkImageView.setImageBitmap(result);
        }
    }

    public void clearAlbumArtwork() {
        ImageView mediaAlbumArtworkImageView = findViewById(R.id.mediaAlbumArtworkImageView);

        mediaAlbumArtworkImageView.setImageDrawable(
            new IconicsDrawable(HomeActivity.this)
                .icon(FontAwesome.Icon.faw_music)
                .color(Color.WHITE)
                .sizeDp(64)
        );
    }

    public void fetchAlbumArtwork(String artistName, String albumName) {
        try {
            String LASTFM_API_KEY = "840e46089e774b43fd3ba374e1d9f5c4";

            StringBuilder stringBuilder = new StringBuilder("http://ws.audioscrobbler.com/2.0/");
            stringBuilder.append("?method=album.getinfo");
            stringBuilder.append("&api_key=");
            stringBuilder.append(LASTFM_API_KEY);
            stringBuilder.append("&artist=" + URLEncoder.encode(artistName, "UTF-8"));
            stringBuilder.append("&album=" + URLEncoder.encode(albumName, "UTF-8"));
            stringBuilder.append("&format=json");
            new RetrieveAlbumArtworkTask().execute(stringBuilder.toString());
        }
        catch(Exception e) {
            Toast.makeText(getApplicationContext(), "Unable to fetch album artwork url", Toast.LENGTH_SHORT).show();
            Log.d("HomeActivity", e.toString());
        }
    }

    public class MediaChangeReciever extends BroadcastReceiver {
        public final class BroadcastTypes {
            static final String SPOTIFY_PACKAGE = "com.spotify.music";
            static final String PLAYBACK_STATE_CHANGED = SPOTIFY_PACKAGE + ".playbackstatechanged";
            static final String QUEUE_CHANGED = SPOTIFY_PACKAGE + ".queuechanged";
            static final String METADATA_CHANGED = SPOTIFY_PACKAGE + ".metadatachanged";
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            // This is sent with all broadcasts, regardless of type. The value is taken from
            // System.currentTimeMillis(), which you can compare to in order to determine how
            // old the event is.
            long timeSentInMs = intent.getLongExtra("timeSent", 0L);

            String action = intent.getAction();

            if (action.equals(BroadcastTypes.METADATA_CHANGED)) {
                String trackId = intent.getStringExtra("id");
                String artistName = intent.getStringExtra("artist");
                String albumName = intent.getStringExtra("album");
                String trackName = intent.getStringExtra("track");
                int trackLengthInSec = intent.getIntExtra("length", 0);
                // Do something with extracted information...

                TextView mediaSongTextView = findViewById(R.id.mediaTrackTextView);
                TextView mediaArtistTextView = findViewById(R.id.mediaArtistTextView);
                TextView mediaAlbumTextView = findViewById(R.id.mediaAlbumTextView);

                mediaSongTextView.setText(trackName);
                mediaArtistTextView.setText(artistName);
                mediaAlbumTextView.setText(albumName);

                currentTrackLength = trackLengthInSec;

                clearAlbumArtwork();
                fetchAlbumArtwork(artistName, albumName);

            } else if (action.equals(BroadcastTypes.PLAYBACK_STATE_CHANGED)) {
                final boolean playing = intent.getBooleanExtra("playing", false);
                int positionInMs = intent.getIntExtra("playbackPosition", 0);
                // Do something with extracted information

                currentTrackPosition = positionInMs;

                Button mediaPlayPauseButton = findViewById(R.id.mediaPlayPauseButton);

                if (playing) {
                    startMediaPositionUpdater();
                    mediaPlayPauseButton.setText("{faw-pause}");
                }
                else {
                    stopMediaPositionUpdater();
                    mediaPlayPauseButton.setText("{faw-play}");
                }

                ProgressBar mediaPositionProgressBar = findViewById(R.id.mediaPositionProgressBar);
                TextView mediaTrackPositionTextView = findViewById(R.id.mediaTrackPositionTextView);
                TextView mediaTrackLengthTextView = findViewById(R.id.mediaTrackLengthTextView);

                mediaPositionProgressBar.setMax(currentTrackLength);
                mediaPositionProgressBar.setProgress(currentTrackPosition);
                mediaTrackPositionTextView.setText(String.format("%d", (currentTrackPosition / 60000))
                        + ":" + doubleDigitFormat(String.format("%d", ((currentTrackPosition / 1000) % 60))));
                mediaTrackLengthTextView.setText(String.format("%d", (currentTrackLength / 60000))
                        + ":" + doubleDigitFormat(String.format("%d", ((currentTrackLength / 1000) % 60))));

            } else if (action.equals(BroadcastTypes.QUEUE_CHANGED)) {
                // Sent only as a notification, your app may want to respond accordingly.
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


    public String doubleDigitFormat(String number) {
        if (number.length() == 1) {
            return "0" + number;
        }

        return number;
    }

    public class PowerConnectedReciever extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(HomeActivity.this, "Power Connected", Toast.LENGTH_SHORT) .show();

            wifiManager.setWifiEnabled(true);
            connectToWifiNetwork(primaryWifiSSID, primaryWifiPassphrase);
        }

    }

    public class PowerDisconnectedReciever extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(HomeActivity.this, "Power Disconnected", Toast.LENGTH_SHORT) .show();

            wifiManager.disableNetwork(selectedNetwork);
            wifiManager.removeNetwork(selectedNetwork);
            wifiManager.disconnect();
            wifiManager.setWifiEnabled(false);
        }

    }

    /**
     * function to load map. If map is not created it will create it for you
     * */
    private void initMap() {
        if (googleMap == null) {
            googleMap = ((MapFragment) getFragmentManager().findFragmentById(
                    R.id.map)).getMap();

            // check if map is created successfully or not
            if (googleMap == null) {
                Toast.makeText(getApplicationContext(),
                        "Unable to create maps", Toast.LENGTH_SHORT)
                        .show();
            }
        }

        Location locationCt;
        LocationManager locationManagerCt = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        try {
            locationCt = locationManagerCt
                    .getLastKnownLocation(LocationManager.GPS_PROVIDER);

        LatLng latLng = new LatLng(locationCt.getLatitude(),
                locationCt.getLongitude());
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
//        googleMap.addMarker(new MarkerOptions().position(latLng)
//                .title("My Spot").snippet("This is my spot!")
//                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_car)));

        googleMap.setMyLocationEnabled(true);

        googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));

        // Zoom in the Google Map
        googleMap.animateCamera(CameraUpdateFactory.zoomTo(18));

        }
        catch(SecurityException e) {
            Toast.makeText(getApplicationContext(),
                    "Unable to get location (No Permission)", Toast.LENGTH_SHORT)
                    .show();
        }
        catch(Exception e) {
            Toast.makeText(getApplicationContext(),
                    "Unable to get location", Toast.LENGTH_SHORT)
                    .show();
        }

        googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {

            @Override
            public void onMapClick(LatLng arg0) {
                HomeActivity.this.onMapClick();
            }
        });
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

    public void startNewActivity(Context context, String packageName) {
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        if (intent == null) {
            // Bring user to the market or let them choose an app?
            intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("market://details?id=" + packageName));
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public void onPlayPauseClick(View v) {
        Intent i = new Intent(Intent.ACTION_MEDIA_BUTTON);
        i.putExtra(Intent.EXTRA_KEY_EVENT,new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
        sendOrderedBroadcast(i, null);

        i = new Intent(Intent.ACTION_MEDIA_BUTTON);
        i.putExtra(Intent.EXTRA_KEY_EVENT,new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
        sendOrderedBroadcast(i, null);
    }

    public void onMediaPreviousClick(View v) {
        Intent i = new Intent(Intent.ACTION_MEDIA_BUTTON);
        i.putExtra(Intent.EXTRA_KEY_EVENT,new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS));
        sendOrderedBroadcast(i, null);

        i = new Intent(Intent.ACTION_MEDIA_BUTTON);
        i.putExtra(Intent.EXTRA_KEY_EVENT,new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PREVIOUS));
        sendOrderedBroadcast(i, null);
    }

    public void onMediaNextClick(View v) {
        Intent i = new Intent(Intent.ACTION_MEDIA_BUTTON);
        i.putExtra(Intent.EXTRA_KEY_EVENT,new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT));
        sendOrderedBroadcast(i, null);

        i = new Intent(Intent.ACTION_MEDIA_BUTTON);
        i.putExtra(Intent.EXTRA_KEY_EVENT,new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_NEXT));
        sendOrderedBroadcast(i, null);
    }

    public void onMediaAlbumArtworkClick(View v) {
        startNewActivity(this, "com.spotify.music");
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
        startNewActivity(this, "com.android.camera2");
    }

    public void onVoiceButtonClick(View v) {
        startNewActivity(this, "com.google.android.googlequicksearchbox");
    }

    public void onClockButtonClick(View v) {
        startNewActivity(this, "com.google.android.deskclock");
    }

    public void onWebButtonClick(View v) {
        startNewActivity(this, "com.android.chrome");
    }

    public void onAppsButtonClick(View v) {
        Intent i = new Intent(this, AppsListActivity.class);
        startActivity(i);
    }

    public void onSettingsButtonClick(View v) {
        startNewActivity(this, "com.android.settings");
    }

    public void onWeatherButtonClick(View v) {
        startNewActivity(this, "com.wunderground.android.weather");
    }

    public void onMapClick() {
        startNewActivity(this, "com.google.android.apps.maps");
    }

    public void MuteAudio(){
        AudioManager mAlramMAnager = (AudioManager) getSystemService(this.AUDIO_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            mAlramMAnager.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_MUTE, 0);
//            mAlramMAnager.adjustStreamVolume(AudioManager.STREAM_ALARM, AudioManager.ADJUST_MUTE, 0);
            mAlramMAnager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0);
//            mAlramMAnager.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_MUTE, 0);
//            mAlramMAnager.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_MUTE, 0);
        } else {
//            mAlramMAnager.setStreamMute(AudioManager.STREAM_NOTIFICATION, true);
//            mAlramMAnager.setStreamMute(AudioManager.STREAM_ALARM, true);
            mAlramMAnager.setStreamMute(AudioManager.STREAM_MUSIC, true);
//            mAlramMAnager.setStreamMute(AudioManager.STREAM_RING, true);
//            mAlramMAnager.setStreamMute(AudioManager.STREAM_SYSTEM, true);
        }
    }

    public void UnMuteAudio(){
        AudioManager mAlramMAnager = (AudioManager) getSystemService(this.AUDIO_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            mAlramMAnager.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_UNMUTE, 0);
//            mAlramMAnager.adjustStreamVolume(AudioManager.STREAM_ALARM, AudioManager.ADJUST_UNMUTE, 0);
            mAlramMAnager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE,0);
//            mAlramMAnager.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_UNMUTE, 0);
//            mAlramMAnager.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_UNMUTE, 0);
        } else {
//            mAlramMAnager.setStreamMute(AudioManager.STREAM_NOTIFICATION, false);
//            mAlramMAnager.setStreamMute(AudioManager.STREAM_ALARM, false);
            mAlramMAnager.setStreamMute(AudioManager.STREAM_MUSIC, false);
//            mAlramMAnager.setStreamMute(AudioManager.STREAM_RING, false);
//            mAlramMAnager.setStreamMute(AudioManager.STREAM_SYSTEM, false);
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

    public void loadHomeContent() {

        // AVOID APP CRASH, DO THIS IN ONCREATE FOR NOW
//        setContentView(R.layout.activity_home);

        // initialize your android device sensor capabilities
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        compassSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magneticFieldSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        setFullscreenMode();

        updateClock();
        updateWeather();
        initSensors();
        initMap();
        initWifi();

        IconicsButton muteButton = findViewById(R.id.muteButton);
        muteButton.setText("{faw-volume-off}\nMUTE");


        registerReceiver(new PowerConnectedReciever(), new IntentFilter(Intent.ACTION_POWER_CONNECTED));
        registerReceiver(new PowerDisconnectedReciever(), new IntentFilter(Intent.ACTION_POWER_DISCONNECTED));

//        registerReceiver(wifiScanReceiver, new IntentFilter(
//                WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        IntentFilter iF = new IntentFilter();
        iF.addAction("com.android.music.metachanged");
        iF.addAction("com.android.music.playstatechanged");
        iF.addAction("com.android.music.playbackcomplete");
        iF.addAction("com.android.music.queuechanged");
        iF.addAction("com.android.music.musicservicecommand");
        iF.addAction("com.android.music.metachanged");
        iF.addAction("com.android.music.playstatechanged");
        iF.addAction("com.android.music.updateprogress");
        iF.addAction("com.htc.music.metachanged");
        iF.addAction("fm.last.android.metachanged");
        iF.addAction("com.sec.android.app.music.metachanged");
        iF.addAction("com.nullsoft.winamp.metachanged");
        iF.addAction("com.amazon.mp3.metachanged");
        iF.addAction("com.miui.player.metachanged");
        iF.addAction("com.real.IMP.metachanged");
        iF.addAction("com.sonyericsson.music.metachanged");
        iF.addAction("com.rdio.android.metachanged");
        iF.addAction("com.samsung.sec.android.MusicPlayer.metachanged");
        iF.addAction("com.spotify.music.metadatachanged");
        iF.addAction("com.spotify.music.playbackstatechanged");
        iF.addAction("com.spotify.music.queuechanged");

        registerReceiver(mediaChangeReciever, iF);
    }

    @Override
    protected void onResume() {
//        setContentView(R.layout.welcome);
//        setFullscreenMode();
//
//        new Handler().postDelayed(
//            new Runnable() {
//                public void run() {
//                    loadHomeContent();
//                }
//            },
//            3000
//        );

        loadHomeContent();
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
