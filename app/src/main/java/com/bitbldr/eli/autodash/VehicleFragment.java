package com.bitbldr.eli.autodash;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.hardware.SensorEvent;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.util.Formatter;
import java.util.Locale;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link VehicleFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 */
public class VehicleFragment extends Fragment implements SensorEventListener, SharedPreferences.OnSharedPreferenceChangeListener {
    private OnFragmentInteractionListener mListener;
    private View view;

    private SensorManager sensorManager;

    private Sensor compassSensor;
    private Sensor accelerometerSensor;
    private Sensor magneticFieldSensor;

    private final float[] mAccelerometerReading = new float[3];
    private final float[] mMagnetometerReading = new float[3];

    private final float[] mRotationMatrix = new float[9];
    private final float[] mOrientationAngles = new float[3];

    private SharedPreferences sharedPref;

    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest mLocationRequest = new LocationRequest();

    private LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            for (Location location : locationResult.getLocations()) {
                // Update UI with location data
                onLocationChanged(location);
            }
        };
    };

    public VehicleFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_vehicle, container, false);

        // initialize your android device sensor capabilities
        sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        compassSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magneticFieldSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        initSensors();

//        if (hasCoarseLocationPermission()) {
//            try {
//                // initialize GPS Speed listener
////                LocationManager locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
//                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
//                this.updateSpeed(null);
//            }
//            catch (SecurityException e) {
//                Log.e("AUTO_MAP", e.toString());
//            }
//        }

        // initialize GPS speed updates
        startLocationUpdates();

        bindEventHandlers(view);

        initInfo();

        return view;
    }

    public boolean hasFineLocationPermission() {
        return ContextCompat.checkSelfPermission(
                getActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;
    }

    private void startLocationUpdates() {
        if (hasFineLocationPermission()) {
            try {
                mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);
            }
            catch (SecurityException e) {
                Log.e("AUTO_MAP", e.toString());
            }
        }
    }

    private void stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }

        sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());

        // register prefs change listener
        sharedPref.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;

        sensorManager.unregisterListener(this);

        // unregister prefs change listener
        sharedPref.unregisterOnSharedPreferenceChangeListener(this);

        // unregister location updates
        stopLocationUpdates();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(AutoSettingsActivity.KEY_PREF_VEHICLE_DETAILS)) {
            setDescriptionFromPrefs();
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
        void onToggleWifi();
    }

    private void initInfo() {
        setDescriptionFromPrefs();
    }

    private void bindEventHandlers(View view) {
        view.findViewById(R.id.wifiButton).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) { onWifiButtonClick(v); }
        });
        view.findViewById(R.id.prefsButton).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) { onPrefsButtonClick(v); }
        });
    }

    private void onPrefsButtonClick(View v) {
        Intent i = new Intent(getActivity(), AutoSettingsActivity.class);
        startActivity(i);
    }

    private void onWifiButtonClick(View v) {
        ((OnFragmentInteractionListener) getActivity()).onToggleWifi();
    }

    private void setDescriptionFromPrefs() {
        String vehicleDetails = sharedPref.getString(AutoSettingsActivity.KEY_PREF_VEHICLE_DETAILS, "");

        TextView vehicleDetailsTextView = view.findViewById(R.id.vehicleDetailsTextView);
        vehicleDetailsTextView.setText(vehicleDetails);
    }

    public void setWifiIndicator(boolean enabled) {
        String newColor;

        if (enabled) {
            newColor = "#ffffff";
        }
        else {
            newColor = "#555555";
        }

        Button wifiButton = view.findViewById(R.id.wifiButton);
        wifiButton.setTextColor(Color.parseColor(newColor));
    }

    public void setBluetoothIndicator(boolean enabled) {
        String newColor;

        if (enabled) {
            newColor = "#3498db";
        }
        else {
            newColor = "#555555";
        }

        TextView bluetoothButton = view.findViewById(R.id.bluetoothButton);
        bluetoothButton.setTextColor(Color.parseColor(newColor));
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

            TextView compassTextView = view.findViewById(R.id.compassTextView);
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
        TextView pitchTextView = view.findViewById(R.id.pitchTextView);
        TextView rollTextView = view.findViewById(R.id.rollTextView);
        TextView yawTextView = view.findViewById(R.id.yawTextView);


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


    private void updateSpeed(CLocation location) {
        float nCurrentSpeed = 0;

        if(location != null)
        {
            nCurrentSpeed = location.getSpeed();
        }

        Formatter fmt = new Formatter(new StringBuilder());
        fmt.format(Locale.US, "%3.0f", nCurrentSpeed);
        String strCurrentSpeed = fmt.toString();

        if (strCurrentSpeed.equals("")) {
            strCurrentSpeed = "0";
        }

        TextView txtCurrentSpeed = view.findViewById(R.id.vehicleSpeed);
        txtCurrentSpeed.setText(strCurrentSpeed);
    }

    public void onLocationChanged(Location location) {
        if(location != null)
        {
            CLocation myLocation = new CLocation(location);
            this.updateSpeed(myLocation);
        }
    }

    public class CLocation extends Location {

        public CLocation(Location location) {
            super(location);
        }

        @Override
        public float distanceTo(Location dest) {
            float nDistance = super.distanceTo(dest);

            //Convert meters to feet
            nDistance = nDistance * 3.28083989501312f;

            return nDistance;
        }

        @Override
        public float getAccuracy() {
            float nAccuracy = super.getAccuracy();

            //Convert meters to feet
            nAccuracy = nAccuracy * 3.28083989501312f;

            return nAccuracy;
        }

        @Override
        public double getAltitude() {
            double nAltitude = super.getAltitude();

            //Convert meters to feet
            nAltitude = nAltitude * 3.28083989501312d;

            return nAltitude;
        }

        @Override
        public float getSpeed() {
            float nSpeed = super.getSpeed() * 3.6f;

            //Convert meters/second to miles/hour
            nSpeed = nSpeed * 2.2369362920544f/3.6f;

            return nSpeed;
        }



    }
}
