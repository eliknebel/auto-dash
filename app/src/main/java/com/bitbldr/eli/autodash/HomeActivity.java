package com.bitbldr.eli.autodash;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.LayoutInflaterCompat;
import android.util.Log;
import android.view.View;
import android.app.Activity;
import android.content.Intent;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.mikepenz.iconics.context.IconicsContextWrapper;
import com.mikepenz.iconics.context.IconicsLayoutInflater;
import com.mikepenz.iconics.context.IconicsLayoutInflater2;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class HomeActivity extends Activity {

    private static final int REQUEST_LOCATION = 1;

    // (10 mins * 60 sec/min * 1000 ms/sec) = 600000ms
    private static final int WEATHER_UPDATE_INTERVAL_MS = 600000;

    // Google Map
    private GoogleMap googleMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        this.checkPermissions();

        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        this.updateClock();
        this.updateWeather();

        try {
            // initialize map
            initMap();

        } catch (Exception e) {
            e.printStackTrace();
        }

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


    public void showApps(View v){
        Intent i = new Intent(this, AppsListActivity.class);
        startActivity(i);
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

    public class RetrieveWeatherTask extends AsyncTask<Void, Void, String> {
        protected void onPreExecute() {
            // do nothing, for now
        }

        protected String doInBackground(Void... urls) {
            try {
                URL url = new URL("http://api.wunderground.com/api/175638bf5ad25874/conditions/q/PA/Pittsburgh.json");
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
                response = "THERE WAS AN ERROR";
            }

            Button currentWeatherButton = findViewById(R.id.weatherButton);

            try {
                JSONObject object = (JSONObject) new JSONTokener(response).nextValue();
                int currentTemp_f = object.getJSONObject("current_observation").getInt("temp_f");

                currentWeatherButton.setText(currentTemp_f + "°");
            } catch (JSONException e) {
                // Appropriate error handling code
            }
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
        catch( SecurityException e) {
            Toast.makeText(getApplicationContext(),
                    "Unable to get location (No Permission)", Toast.LENGTH_SHORT)
                    .show();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        initMap();
    }

}
