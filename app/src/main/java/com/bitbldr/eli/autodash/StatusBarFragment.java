package com.bitbldr.eli.autodash;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.mikepenz.iconics.view.IconicsButton;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link StatusBarFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 */
public class StatusBarFragment extends Fragment {
    private OnFragmentInteractionListener mListener;
    View view;

    // (10 min * 60 s/min * 1000 ms/s) = 600000ms
    private static final int WEATHER_UPDATE_INTERVAL_MS = 600000;

    private boolean isMuting = false;

    public StatusBarFragment() {
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
        view = inflater.inflate(R.layout.fragment_status_bar, container, false);

        bindEventHandlers(view);
        updateClock();
        updateWeather((TextView) view.findViewById(R.id.weatherButton));

        // initialize icons for iconics buttons
        IconicsButton muteButton = view.findViewById(R.id.muteButton);
        muteButton.setText("{faw-volume-off}\nMUTE");

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

    private void bindEventHandlers(View view) {
        view.findViewById(R.id.internalTempButton).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) { Log.d("INFO", "NOT IMPLEMENTED"); }
        });

        view.findViewById(R.id.muteButton).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) { onMuteButtonClick(v); }
        });

        view.findViewById(R.id.backupButton).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) { onBackupButtonClick(v); }
        });

        view.findViewById(R.id.voiceButton).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) { onVoiceButtonClick(v); }
        });

        view.findViewById(R.id.clockButton).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) { onClockButtonClick(v); }
        });

        view.findViewById(R.id.webButton).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) { onWebButtonClick(v); }
        });

        view.findViewById(R.id.appsButton).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) { onAppsButtonClick(v); }
        });

        view.findViewById(R.id.settingsButton).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) { onSettingsButtonClick(v); }
        });

        view.findViewById(R.id.weatherButton).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) { onWeatherButtonClick(v); }
        });
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

        Button clockButton = view.findViewById(R.id.clockButton);
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

    private void updateWeather(TextView weatherButton) {
        new WeatherTextViewUpdaterTask(weatherButton).execute();

        initNextWeatherUpdate();
    }

    private void initNextWeatherUpdate() {
        new android.os.Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        updateWeather((TextView) view.findViewById(R.id.weatherButton));
                    }
                },
                WEATHER_UPDATE_INTERVAL_MS
        );
    }

    public void onMuteButtonClick(View v) {
        IconicsButton muteButton = view.findViewById(R.id.muteButton);

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
        Utils.StartNewActivity(getActivity(), "com.android.camera2");
    }

    public void onVoiceButtonClick(View v) {
//        Utils.StartNewActivity(getActivity(), "com.google.android.googlequicksearchbox");
        startActivity(new Intent(Intent.ACTION_VOICE_COMMAND).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    public void onClockButtonClick(View v) {
        Utils.StartNewActivity(getActivity(), "com.google.android.deskclock");
    }

    public void onWebButtonClick(View v) {
        Utils.StartNewActivity(getActivity(), "com.android.chrome");
    }

    public void onAppsButtonClick(View v) {
        Intent i = new Intent(getActivity(), AppsListActivity.class);
        startActivity(i);
    }

    public void onSettingsButtonClick(View v) {
        Utils.StartNewActivity(getActivity(), "com.android.settings");
    }

    public void onWeatherButtonClick(View v) {
        Utils.StartNewActivity(getActivity(), "com.wunderground.android.weather");
    }

    public void MuteAudio(){
        AudioManager mAudioManager = (AudioManager) getActivity().getSystemService(getActivity().AUDIO_SERVICE);
        mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0);
    }

    public void UnMuteAudio(){
        AudioManager audioManager = (AudioManager) getActivity().getSystemService(getActivity().AUDIO_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE,0);
        } else {
            audioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
        }
    }

}
