package com.bitbldr.eli.autodash;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class NetworkManager {

    private WifiManager wifiManager;
    private WifiConnectionReceiver wifiConnectionReceiver;
    private WifiScanReceiver wifiScanReceiver;
    private Context context;
    private NetworkConsumer consumer;
    private SharedPreferences sharedPref;
    private int selectedNetwork;
    private String currentNetworkSSID = "";
    private boolean waitingForNextRefresh = false;
    private boolean refreshActive = false;
    private long initTimestamp = 0;
    private boolean networkSetupMode = false;

    private static String LOG_TAG = "NETWORK_MANAGER";

    private static int SETUP_MODE_PERIOD_MS = 60000;             // 1 min
    private static int CONNECT_RETRY_SETUP_INTERVAL_MS = 3000;      // 3 sec
    private static int CONNECT_RETRY_INTERVAL_MS = 30000;           // 30 sec

    public enum NetworkStatus { CONNECTED, DISCONNECTED }

    public interface NetworkConsumer {
        void onConnectionStatusChanged(NetworkManager.NetworkStatus status);
    }

    public NetworkManager(Context context) {
        this.context = context;
        this.consumer = (NetworkConsumer) context;

        sharedPref = PreferenceManager.getDefaultSharedPreferences(context);

        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        enterNetworkSetupMode();
    }

    public void start() {
        wifiManager.setWifiEnabled(true);

        // setup broadcast receivers
        if (wifiScanReceiver == null) {
            wifiScanReceiver = new WifiScanReceiver();
            context.registerReceiver(wifiScanReceiver, new IntentFilter(
                    WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        }

        if (wifiConnectionReceiver == null) {
            wifiConnectionReceiver = new WifiConnectionReceiver();
            context.registerReceiver(wifiConnectionReceiver, new IntentFilter(
                    WifiManager.NETWORK_STATE_CHANGED_ACTION));
        }

        handleWifiConnectionStateChange();

        scanForWifiNetworks();

        refreshActive = true;
        scheduleNetworkRefresh();
    }

    public void stop() {
        // unregister listeners
        if (wifiScanReceiver != null) {
            context.unregisterReceiver(wifiScanReceiver);
            wifiScanReceiver = null;
        }
        if (wifiConnectionReceiver != null) {
            context.unregisterReceiver(wifiConnectionReceiver);
            wifiConnectionReceiver = null;
        }

        refreshActive = false;
    }

    public void disconnect() {
        wifiManager.disconnect();
        wifiManager.setWifiEnabled(false);
    }

    /** Scans for wifi networks. Results in WifiScanReceiver.onReceive invocation */
    private void scanForWifiNetworks() {
        wifiManager.startScan();
    }

    /** Connects to the specified network. Results in WifiConnectionReceiver.onReceive invocation */
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

    private void enterNetworkSetupMode() {
        networkSetupMode = true;
    }

    private void exitNetworkSetupMode() {
        networkSetupMode = false;
    }

    private void scheduleNetworkRefresh() {
        // schedule next retry for CONNECT_RETRY_INTERVAL_MS
        if (!waitingForNextRefresh) {
            waitingForNextRefresh = true;

            if ((System.currentTimeMillis() - SETUP_MODE_PERIOD_MS) < initTimestamp) {
                networkSetupMode = false;
            }

            int retryInterval = networkSetupMode ?
                    CONNECT_RETRY_SETUP_INTERVAL_MS : CONNECT_RETRY_INTERVAL_MS;

            Log.v(LOG_TAG, "Network will refresh in " + retryInterval / 1000 + " seconds");

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    waitingForNextRefresh = false;

                    if (refreshActive) {
                        start();
                    }
                }
            }, retryInterval);
        }
    };

    private void handleWifiConnectionStateChange() {
        if (wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED
                && wifiManager.getConnectionInfo().getNetworkId() != -1) {
            exitNetworkSetupMode();

            currentNetworkSSID = wifiManager.getConnectionInfo().getSSID();
            currentNetworkSSID = currentNetworkSSID.substring(1, (currentNetworkSSID.length() - 1));

            // notify consumer of state change
            consumer.onConnectionStatusChanged(NetworkStatus.CONNECTED);

            Log.v(LOG_TAG, "Now connected to " + currentNetworkSSID);
        }
        else {
            // connection disrupted, enter network setup mode
            enterNetworkSetupMode();

            currentNetworkSSID = "";

            // notify consumer of state change
            consumer.onConnectionStatusChanged(NetworkStatus.DISCONNECTED);

            Log.v(LOG_TAG, "Network disconnected");
        }
    }

    private void handleWifiScanComplete() {
        ArrayList<String> connections = new ArrayList<>();
        ArrayList<Float> signalStrength = new ArrayList<>();

        List<ScanResult> wifiList;
        wifiList = wifiManager.getScanResults();
        for (int i = 0; i < wifiList.size(); i++) {
            connections.add(wifiList.get(i).SSID);
        }

        Log.v(LOG_TAG, "Available wifi networks: " + connections.toString());

        // check if preferred (home) network is available
        String preferredNetworkSSID = sharedPref.getString(AutoSettingsActivity.KEY_PREF_PRIMARY_NETWORK_SSID, "");
        String alternateNetworkSSID = sharedPref.getString(AutoSettingsActivity.KEY_PREF_SECONDARY_NETWORK_SSID, "");

        if (!preferredNetworkSSID.equals("") && !currentNetworkSSID.equals(preferredNetworkSSID)
                && connections.contains(preferredNetworkSSID)) {
            // connect to preferred network
            Log.v(LOG_TAG, "Preferred network is available. Attempting to connect...");

            connectToWifiNetwork(preferredNetworkSSID,
                    sharedPref.getString(AutoSettingsActivity.KEY_PREF_PRIMARY_NETWORK_PASS, ""));
        }
        else if (!alternateNetworkSSID.equals("") && !currentNetworkSSID.equals(alternateNetworkSSID)
                && !connections.contains(preferredNetworkSSID)) {
            // connect to alternate network
            Log.v(LOG_TAG, "Preferred network is unavailable. Attempting to connect to alternate network...");

            connectToWifiNetwork(alternateNetworkSSID,
                    sharedPref.getString(AutoSettingsActivity.KEY_PREF_SECONDARY_NETWORK_PASS, ""));
        }
        else if (preferredNetworkSSID == "" && alternateNetworkSSID == "") {
            // display message to configure networks and turn off wifi
            Toast.makeText(context, "No Wifi networks configured. Configure networks under dashboard preferences", Toast.LENGTH_LONG).show();
            stop();
        }
        else {
            // we are already properly connected, do nothing
            Log.v(LOG_TAG, "Network is already connected to " + wifiManager.getConnectionInfo().getSSID());
        }
    }

    class WifiConnectionReceiver extends BroadcastReceiver
    {
        public void onReceive(Context c, Intent intent)
        {
            handleWifiConnectionStateChange();
        }
    }

    class WifiScanReceiver extends BroadcastReceiver
    {
        public void onReceive(Context c, Intent intent) {
            if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                handleWifiScanComplete();
            }
        }
    }

}
