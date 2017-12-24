package com.bitbldr.eli.autodash;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;


import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;

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

    View view;

    public AutoMapFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_map, container, false);

        initMap();

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

    private void initMap() {
        if (googleMap == null) {
            SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                    .findFragmentById(R.id.googlemap);
            mapFragment.getMapAsync(this);
        }
    }

    /**
     * function to load map. If map is not created it will create it for you
     * */
    private void setupMap() {
        Location locationCt;
        LocationManager locationManagerCt = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);

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
            Toast.makeText(getActivity().getApplicationContext(),
                    "Unable to get location (No Permission)", Toast.LENGTH_SHORT)
                    .show();
        }
        catch(Exception e) {
            Toast.makeText(getActivity().getApplicationContext(),
                    "Unable to get location", Toast.LENGTH_SHORT)
                    .show();
        }

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
        Utils.StartNewActivity(getActivity(), "com.google.android.apps.maps");
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
