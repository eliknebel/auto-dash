package com.bitbldr.eli.autodash;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.mikepenz.fontawesome_typeface_library.FontAwesome;
import com.mikepenz.iconics.IconicsDrawable;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link MediaBarFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 */
public class MediaBarFragment extends Fragment {
    private Context context;

    private View view;

    private MediaChangeReciever mediaChangeReciever = new MediaChangeReciever();

    private int currentTrackPosition = 0;
    private int currentTrackLength = 1;
    private Timer mediaPositionUpdater;

    private OnFragmentInteractionListener mListener;

    public MediaBarFragment() {
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
        view =  inflater.inflate(R.layout.fragment_media_bar, container, false);

        // initialize fragment event listeners view
        bindEventHandlers(view);

        return view;
    }

    @Override
    public void onAttach(Context context)
    {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }

        this.context = context;

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

        context.registerReceiver(mediaChangeReciever, iF);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;

        context.unregisterReceiver(mediaChangeReciever);
    }

    @Override
    public void onDestroyView()
    {
        super.onDestroyView();
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

    /**
     * Registers fragment views with their event handlers
     */
    private void bindEventHandlers(View view) {
        view.findViewById(R.id.mediaPlayPauseButton).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) { onMediaPlayPauseClick(v); }
        });

        view.findViewById(R.id.mediaNextButton).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) { onMediaNextClick(v); }
        });

        view.findViewById(R.id.mediaPreviousButton).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) { onMediaPreviousClick(v); }
        });

        view.findViewById(R.id.mediaInfoView).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) { onMediaInfoViewClick(v); }
        });
    }

    /**
     * Handle media play/pause button click
     * @param v
     */
    public void onMediaPlayPauseClick(View v) {
        Intent i = new Intent(Intent.ACTION_MEDIA_BUTTON);
        i.putExtra(Intent.EXTRA_KEY_EVENT,new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
        getActivity().sendOrderedBroadcast(i, null);

        i = new Intent(Intent.ACTION_MEDIA_BUTTON);
        i.putExtra(Intent.EXTRA_KEY_EVENT,new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
        getActivity().sendOrderedBroadcast(i, null);
    }

    /**
     * Handle media previous button click
     * @param v
     */
    public void onMediaPreviousClick(View v) {
        Intent i = new Intent(Intent.ACTION_MEDIA_BUTTON);
        i.putExtra(Intent.EXTRA_KEY_EVENT,new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS));
        getActivity().sendOrderedBroadcast(i, null);

        i = new Intent(Intent.ACTION_MEDIA_BUTTON);
        i.putExtra(Intent.EXTRA_KEY_EVENT,new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PREVIOUS));
        getActivity().sendOrderedBroadcast(i, null);
    }

    /**
     * Handle media next button click
     * @param v
     */
    public void onMediaNextClick(View v) {
        Intent i = new Intent(Intent.ACTION_MEDIA_BUTTON);
        i.putExtra(Intent.EXTRA_KEY_EVENT,new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT));
        getActivity().sendOrderedBroadcast(i, null);

        i = new Intent(Intent.ACTION_MEDIA_BUTTON);
        i.putExtra(Intent.EXTRA_KEY_EVENT,new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_NEXT));
        getActivity().sendOrderedBroadcast(i, null);
    }

    /**
     * Handle media info view click
     * @param v
     */
    public void onMediaInfoViewClick(View v) {
        Utils.StartNewActivity(getActivity(), "com.spotify.music");
    }

    public void updateMediaPosition() {
        ProgressBar mediaPositionProgressBar = getView().findViewById(R.id.mediaPositionProgressBar);
        TextView mediaTrackPositionTextView = getView().findViewById(R.id.mediaTrackPositionTextView);

        mediaPositionProgressBar.setProgress(currentTrackPosition);
        mediaTrackPositionTextView.setText(String.format("%d", (currentTrackPosition / 60000))
                + ":" + Utils.DoubleDigitFormat(String.format("%d", ((currentTrackPosition / 1000) % 60))));
    }

    /**
     * Starts a new handler to update the media scrubber position every second
     */
    private void startMediaPositionUpdater() {
        updateMediaPosition();

        // ensure there is only ever one mediaPositionUpdater timer
        synchronized(this) {
            if (mediaPositionUpdater != null) {
                return;
            }

            mediaPositionUpdater = new Timer();
        }

        // schedule first update on the next media progress round second tick
        new android.os.Handler().postDelayed(
            new Runnable() {
                public void run() {
                    // schedule reoccurring updates every round second tick
                    mediaPositionUpdater.scheduleAtFixedRate(new TimerTask() {
                        @Override
                        public void run() {
                            // Get a handler that can be used to post to the main thread
                            Handler mainHandler = new Handler(Looper.getMainLooper());

                            Runnable myRunnable = new Runnable() {
                                @Override
                                public void run() {
                                    currentTrackPosition += 1000;
                                    updateMediaPosition();
                                }
                            };
                            mainHandler.post(myRunnable);
                        }
                    }, 0, 1000);
                }
            }, getMSUntilNextMediaProgressTick()
        );
    }

    /**
     * Stops the handler for the media scrubber position updater
     */
    private void stopMediaPositionUpdater() {
        if (mediaPositionUpdater != null) {
            mediaPositionUpdater.cancel();
        }

        mediaPositionUpdater = null;
    }

    /**
     * Calculates the time delta until the next rounded second tick for the media position scrubber
     * @return
     */
    private long getMSUntilNextMediaProgressTick() {
        return (1000 - (currentTrackPosition % 1000));
    }


    /**
     * Task to fetch media album artwork information
     */
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

    /**
     * Task to download media album artwork
     */
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
            ImageView mediaAlbumArtworkImageView = getView().findViewById(R.id.mediaAlbumArtworkImageView);

            if (result == null) {
                clearAlbumArtwork();
                return;
            }

            mediaAlbumArtworkImageView.setImageBitmap(result);
        }
    }

    /**
     * Clears the current media album artwork
     */
    public void clearAlbumArtwork() {
        ImageView mediaAlbumArtworkImageView = getView().findViewById(R.id.mediaAlbumArtworkImageView);

        mediaAlbumArtworkImageView.setImageDrawable(
            new IconicsDrawable(getActivity())
                .icon(FontAwesome.Icon.faw_music)
                .color(Color.WHITE)
                .sizeDp(64)
        );
    }

    /**
     * Fetches media album artwork for the given artist name and album name
     * @param artistName
     * @param albumName
     */
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
            Toast.makeText(getActivity().getApplicationContext(), "Unable to fetch album artwork url", Toast.LENGTH_SHORT).show();
            Log.w("WARNING", e.toString());
        }
    }

    /**
     * Handle media change broadcast events
     */
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

                TextView mediaSongTextView = getView().findViewById(R.id.mediaTrackTextView);
                TextView mediaArtistTextView = getView().findViewById(R.id.mediaArtistTextView);
                TextView mediaAlbumTextView = getView().findViewById(R.id.mediaAlbumTextView);

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

                Button mediaPlayPauseButton = getView().findViewById(R.id.mediaPlayPauseButton);

                if (playing) {
                    startMediaPositionUpdater();
                    mediaPlayPauseButton.setText("{faw-pause}");
                }
                else {
                    stopMediaPositionUpdater();
                    mediaPlayPauseButton.setText("{faw-play}");
                }

                ProgressBar mediaPositionProgressBar = getView().findViewById(R.id.mediaPositionProgressBar);
                TextView mediaTrackPositionTextView = getView().findViewById(R.id.mediaTrackPositionTextView);
                TextView mediaTrackLengthTextView = getView().findViewById(R.id.mediaTrackLengthTextView);

                mediaPositionProgressBar.setMax(currentTrackLength);
                mediaPositionProgressBar.setProgress(currentTrackPosition);
                mediaTrackPositionTextView.setText(String.format("%d", (currentTrackPosition / 60000))
                        + ":" + Utils.DoubleDigitFormat(String.format("%d", ((currentTrackPosition / 1000) % 60))));
                mediaTrackLengthTextView.setText(String.format("%d", (currentTrackLength / 60000))
                        + ":" + Utils.DoubleDigitFormat(String.format("%d", ((currentTrackLength / 1000) % 60))));

            } else if (action.equals(BroadcastTypes.QUEUE_CHANGED)) {
                // Sent only as a notification, your app may want to respond accordingly.
            }
        }

    }


}
