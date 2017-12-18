package com.bitbldr.eli.autodash;

import android.Manifest;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
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
import com.mikepenz.iconics.view.IconicsButton;
import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends FragmentActivity implements
        MediaBarFragment.OnFragmentInteractionListener,
        AutoMapFragment.OnFragmentInteractionListener,
        StatusBarFragment.OnFragmentInteractionListener,
        VehicleFragment.OnFragmentInteractionListener {

    // Application views
    View welcomeView;
    View mainView;

    // Boot logo duration = 2s
    private static final int BOOT_LOGO_DURATION_MS = 2000;

    WifiManager wifiManager;
    WifiConnectionReceiver wifiConnectionReceiver;
    WifiScanReceiver wifiScanReceiver;
    int selectedNetwork;

    String primaryWifiSSID = "my wifi";
    String primaryWifiPassphrase = "passphrase";

    String mobileWifiSSID = "my wifi";
    String mobileWifiPassphrase = "passphrase";

    private boolean isColdStart = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        inflateAllViews();
        showWelcomeView();

        this.checkPermissions();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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

            } else {
                // Permission was denied or request was cancelled
            }
        }
    }

    @Override
    public void onFragmentInteraction(Uri uri) {
        // handle fragment interactions
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
//            wifiManager.setWifiEnabled(true);
//            connectToWifiNetwork(primaryWifiSSID, primaryWifiPassphrase);

            // open this app on power connect
            startActivity(new Intent(context, HomeActivity.class));
        }

    }

    public class PowerDisconnectedReciever extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            wifiManager.disableNetwork(selectedNetwork);
            wifiManager.removeNetwork(selectedNetwork);
            wifiManager.disconnect();
            wifiManager.setWifiEnabled(false);

            isColdStart = true;

            turnScreenOff(context);
        }

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

        setFullscreenMode();

        initWifi();

        IconicsButton muteButton = findViewById(R.id.muteButton);
        muteButton.setText("{faw-volume-off}\nMUTE");

        registerReceiver(new PowerConnectedReciever(), new IntentFilter(Intent.ACTION_POWER_CONNECTED));
        registerReceiver(new PowerDisconnectedReciever(), new IntentFilter(Intent.ACTION_POWER_DISCONNECTED));
    }

    @Override
    protected void onResume() {
        super.onResume();

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
            inflateAllViews();
            showMainView();
        }
//        inflateAllViews();        // for debugging
//        showMainView();

        setFullscreenMode();
    }

    @Override
    public void onPause() {
        super.onPause();
        // to stop the listener and save battery

//        unregisterReceiver(wifiScanReceiver);
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
