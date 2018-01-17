package com.bitbldr.eli.autodash;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;


import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link AutoMapFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 */
public class AutoMapFragment extends android.support.v4.app.Fragment implements OnMapReadyCallback {
    private OnFragmentInteractionListener mListener;

    // Google maps request location flag
    public static final int REQUEST_LOCATION = 1;

    // Google Map
    private GoogleMap googleMap;

    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            for (Location location : locationResult.getLocations()) {
                // Update UI with location data
                updateMapLocation(location);
            }
        };
    };

    private LocationRequest mLocationRequest = new LocationRequest();

    View view;

    public AutoMapFragment() {
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
        view = inflater.inflate(R.layout.fragment_map, container, false);

        initMap();

        startLocationUpdates();

        return view;
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
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;

        stopLocationUpdates();
    }

    public boolean hasCoarseLocationPermission() {
        return ContextCompat.checkSelfPermission(
                getActivity(),
                Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;
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
                mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                        mLocationCallback,
                        null /* Looper */);
            }
            catch (SecurityException e) {
                Log.e("AUTO_MAP", e.toString());
            }
        }
    }

    private void stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
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
    }

    public void initMap() {
        if (googleMap == null) {
            SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                    .findFragmentById(R.id.googlemap);
            mapFragment.getMapAsync(this);
        }
    }

    /**
     * function to load map. If map is not created it will create it for you
     * */
    public void setupMap() {
        if (hasCoarseLocationPermission()) {
            try {
                mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(getActivity(), new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            // Got last known location. In some rare situations this can be null.
                            if (location != null) {
                                // Logic to handle location object

                                LatLng latLng = new LatLng(location.getLatitude(),
                                        location.getLongitude());
                                googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

                                //                        googleMap.addMarker(new MarkerOptions().position(latLng)
                                //                                .title("My Spot").snippet("This is my spot!")
                                //                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_car)));

                                if (hasFineLocationPermission()) {
                                    try {
                                        googleMap.setMyLocationEnabled(true);
                                    } catch (SecurityException e) {
                                        Log.e("AUTO_MAP", e.toString());
                                    }
                                }

                                googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));

                                // Zoom in the Google Map
                                googleMap.animateCamera(CameraUpdateFactory.zoomTo(18));
                            }
                        }
                });
            }
            catch (SecurityException e) {
                Log.e("AUTO_MAP", e.toString());
            }
        }

        // disable scroll gestures
        googleMap.getUiSettings().setScrollGesturesEnabled(false);

        // setup click handler
        googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {

            @Override
            public void onMapClick(LatLng arg0) {
                AutoMapFragment.this.onMapClick();
            }
        });

        updateMap();
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;

        setupMap();
    }

    public void onMapClick() {
//        if (openMapsInDrivingMode) {
            Intent intent = getActivity().getPackageManager().getLaunchIntentForPackage("com.google.android.apps.maps");
            intent.setAction(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("google.navigation:/?free=1&mode=d&entry=fnls"));
            startActivity(intent);
//        }
//        else {
//            Utils.StartNewActivity(getActivity(), "com.google.android.apps.maps");
//        }
    }

    private void loadDayTheme() {
        if (getActivity() != null) {
            MapStyleOptions style = MapStyleOptions.loadRawResourceStyle(getActivity(), R.raw.mapstyle_day);
            googleMap.setMapStyle(style);
        }
    }

    private void loadNightTheme() {
        if (getActivity() != null) {
            MapStyleOptions style = MapStyleOptions.loadRawResourceStyle(getActivity(), R.raw.mapstyle_night);
            googleMap.setMapStyle(style);
        }
    }

    private void updateMap() {
        Date date = new Date();
        TimeZone tz = TimeZone.getTimeZone("America/New_York");
        Calendar now = GregorianCalendar.getInstance(tz);
        now.setTime(date);

        int hour = (now.get(Calendar.HOUR_OF_DAY));

        if (hour < 7 || hour > 18) {
            loadNightTheme();
        }
        else {
            loadDayTheme();
        }

        initNextMapUpdate();
    }

    private void updateMapLocation(Location location) {
        Log.d("AUTO_MAP", "updateMapLocation()");
        googleMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(location.getLatitude(),
                location.getLongitude())));
    }

    private long getMSUntilNextClockTick() {
        long currentMS = System.currentTimeMillis();

        return (60 - ((currentMS / 60000) % 60)) * 60000;
    }

    private void initNextMapUpdate() {
        new android.os.Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        updateMap();
                    }
                },
                getMSUntilNextClockTick());
    }


}
