package com.bitbldr.eli.autodash;

import android.Manifest;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.support.v4.app.FragmentActivity;
import android.content.Intent;
import android.view.WindowManager;
import android.widget.Toast;
import com.mikepenz.iconics.context.IconicsContextWrapper;

public class MainActivity extends FragmentActivity implements
        MediaBarFragment.OnFragmentInteractionListener,
        AutoMapFragment.OnFragmentInteractionListener,
        StatusBarFragment.OnFragmentInteractionListener,
        VehicleFragment.OnFragmentInteractionListener,
        NetworkManager.NetworkConsumer {

    // Application views
    View welcomeView;
    View mainView;

    // Boot logo duration = 2s
    private static final int BOOT_LOGO_DURATION_MS = 2000;

    private NetworkManager networkManager;

    private boolean isColdStart = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        inflateAllViews();
        showWelcomeView();

        this.checkPermissions();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        networkManager = new NetworkManager(this);
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
                    AutoMapFragment.REQUEST_LOCATION);
        }
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == AutoMapFragment.REQUEST_LOCATION) {
            if(grantResults.length == 1
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // We can now safely use the API we requested access to
                initMap();
            } else {
                // Permission was denied or request was cancelled
                Log.i("INFO", "Location services will not work without proper permissions");
                Toast.makeText(this, "Location services will not work without proper permissions",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onFragmentInteraction(Uri uri) {
        // handle fragment interactions
    }

    public void onToggleWifi() {
        networkManager.start();
    }

    @Override
    public void onConnectionStatusChanged(NetworkManager.NetworkStatus status) {
        VehicleFragment vehicleFragment = (VehicleFragment) getSupportFragmentManager().findFragmentById(R.id.vehicle);

        if (status == NetworkManager.NetworkStatus.CONNECTED) {
            // network is available, reinitialize network dependant services
            initNetworkDepServices();

            if (vehicleFragment != null) {
                vehicleFragment.setWifiIndicator(true);
            }
        }
        else {
            if (vehicleFragment != null) {
                vehicleFragment.setWifiIndicator(false);
            }
        }
    }

    private void initNetworkDepServices() {
        initMap();
    }

    private void initMap() {
        AutoMapFragment autoMapFragment = (AutoMapFragment) getSupportFragmentManager().findFragmentById(R.id.autoMap);

        if (autoMapFragment != null) {
            autoMapFragment.setupMap();
        }
    }

    public class PowerConnectedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // open this app on power connect
            startActivity(new Intent(context, MainActivity.class));
        }

    }

    public class PowerDisconnectedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            isColdStart = true;

            // pause music
            MediaBarFragment mediaBarFragment = (MediaBarFragment) getFragmentManager().findFragmentById(R.id.mediabar);
            mediaBarFragment.pauseMusic();

            networkManager.disconnect();
            turnScreenOff(context);
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
//
//    public void inflateAllViews() {
//        if (welcomeView == null) {
//            welcomeView = getLayoutInflater().inflate(R.layout.welcome, null);
//        }
//
//        if (mainView == null) {
//            mainView = getLayoutInflater().inflate(R.layout.activity_main, null);
//        }
//    }

    public void showWelcomeView() {
        if (welcomeView == null) {
            welcomeView = getLayoutInflater().inflate(R.layout.welcome, null);
        }

        setContentView(welcomeView);
    }

    public void showMainView() {
        if (mainView == null) {
            mainView = getLayoutInflater().inflate(R.layout.activity_main, null);
        }

        setContentView(mainView);

        setFullscreenMode();

        registerReceiver(new PowerConnectedReceiver(), new IntentFilter(Intent.ACTION_POWER_CONNECTED));
        registerReceiver(new PowerDisconnectedReceiver(), new IntentFilter(Intent.ACTION_POWER_DISCONNECTED));
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (isColdStart) {
            showWelcomeView();

            new Handler().postDelayed(
                    new Runnable() {
                        public void run() {
                            isColdStart = false;
                            showMainView();
                        }
                    },
                    BOOT_LOGO_DURATION_MS
            );
        }
        else {
            showMainView();
        }

        networkManager.start();

        setFullscreenMode();
    }

    @Override
    public void onPause() {
        super.onPause();

        // stop the listeners and services to save battery
        networkManager.stop();
    }

    /**
     * Turns the screen off and locks the device, provided that proper rights
     * are given.
     *
     * @param context
     *            - The application context
     */
    static void turnScreenOff(final Context context) {
        DevicePolicyManager policyManager = (DevicePolicyManager) context
                .getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName adminReceiver = new ComponentName(context,
                ScreenOffAdminReceiver.class);
        boolean admin = policyManager.isAdminActive(adminReceiver);
        if (admin) {
            Log.i("INFO", "Going to sleep now.");
            policyManager.lockNow();
        } else {
            Log.i("INFO", "Not an admin");
            Toast.makeText(context, "Device admin not enabled",
                    Toast.LENGTH_LONG).show();
        }
    }

}
