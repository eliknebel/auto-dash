package com.bitbldr.eli.autodash;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.hardware.SensorEvent;
import android.widget.TextView;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link VehicleFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 */
public class VehicleFragment extends Fragment implements SensorEventListener {
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

    public VehicleFragment() {
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
        view = inflater.inflate(R.layout.fragment_vehicle, container, false);

        // initialize your android device sensor capabilities
        sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        compassSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magneticFieldSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        initSensors();

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

        sensorManager.unregisterListener(this);
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

}
