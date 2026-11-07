package com.generated.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private WifiManager wifiManager;
    private ListView wifiList;
    private TextView statusTextView;
    private Button scanButton;
    private List<ScanResult> wifiScanResults;
    private WifiReceiver wifiReceiver;
    private Handler scanHandler = new Handler();
    private Runnable scanRunnable;

    private boolean autoConnectEnabled = false;
    private List<String> savedNetworksSSID = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiList = findViewById(R.id.wifiList);
        statusTextView = findViewById(R.id.statusTextView);
        scanButton = findViewById(R.id.scanButton);

        if (wifiManager == null) {
            Toast.makeText(this, "שירות Wi-Fi אינו זמין.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (!wifiManager.isWifiEnabled()) {
            statusTextView.setText("Wi-Fi כבוי. יש להפעיל.");
            scanButton.setText(R.string.turn_wifi_on);
            scanButton.setVisibility(View.VISIBLE);
            scanButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                }
            });
        } else {
            startWifiScan();
        }

        setupWifiListListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (wifiReceiver == null) {
            wifiReceiver = new WifiReceiver();
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        registerReceiver(wifiReceiver, filter);

        // Check current Wi-Fi connection status
        updateConnectionStatus();

        // Reload settings
        loadSettings();

        // Update button visibility based on Wi-Fi state
        if (wifiManager != null && !wifiManager.isWifiEnabled()) {
            scanButton.setText(R.string.turn_wifi_on);
            scanButton.setVisibility(View.VISIBLE);
            scanButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                }
            });
            statusTextView.setText("Wi-Fi כבוי. יש להפעיל.");
            wifiList.setAdapter(null); // Clear the list if Wi-Fi is off
        } else {
            scanButton.setVisibility(View.GONE);
            if (wifiManager != null && wifiManager.isWifiEnabled()) {
                startWifiScan(); // Start scanning if Wi-Fi is on
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (wifiReceiver != null) {
            unregisterReceiver(wifiReceiver);
        }
        stopScanTimer();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void startWifiScan() {
        if (wifiManager.isWifiEnabled()) {
            if (checkLocationPermission()) {
                statusTextView.setText(R.string.scanning);
                if (wifiManager.startScan()) {
                    // Scan initiated successfully
                } else {
                    statusTextView.setText(R.string.error_wifi_scan);
                }
                setupScanTimer();
            } else {
                requestLocationPermission();
            }
        } else {
            statusTextView.setText("Wi-Fi כבוי. יש להפעיל.");
            scanButton.setText(R.string.turn_wifi_on);
            scanButton.setVisibility(View.VISIBLE);
            scanButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                }
            });
        }
    }

    private void setupScanTimer() {
        stopScanTimer(); // Ensure no duplicate timers are running
        scanRunnable = new Runnable() {
            @Override
            public void run() {
                if (wifiManager.isWifiEnabled()) {
                    if (wifiManager.startScan()) {
                        // Scan initiated successfully
                    } else {
                        statusTextView.setText(R.string.error_wifi_scan);
                    }
                }
                scanHandler.postDelayed(this, TimeUnit.SECONDS.toMillis(10)); // Rescan every 10 seconds
            }
        };
        scanHandler.postDelayed(scanRunnable, TimeUnit.SECONDS.toMillis(10)); // Start the timer
    }

    private void stopScanTimer() {
        if (scanRunnable != null) {
            scanHandler.removeCallbacks(scanRunnable);
            scanRunnable = null;
        }
    }

    private boolean checkLocationPermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
               (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED);
    }

    private void requestLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && (grantResults[0] == PackageManager.PERMISSION_GRANTED || grantResults[1] == PackageManager.PERMISSION_GRANTED)) {
                startWifiScan();
            } else {
                statusTextView.setText(R.string.error_location_permission);
                Toast.makeText(this, R.string.error_location_permission, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void updateWifiList(List<ScanResult> results) {
        wifiScanResults = results;
        if (wifiScanResults == null || wifiScanResults.isEmpty()) {
            if (wifiManager.isWifiEnabled()) {
                statusTextView.setText(R.string.no_wifi_found);
            }
            wifiList.setAdapter(null);
            return;
        }

        ArrayAdapter<ScanResult> adapter = new ArrayAdapter<ScanResult>(this, R.layout.list_item_wifi, R.id.wifi_ssid, wifiScanResults) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = convertView;
                if (view == null) {
                    view = getLayoutInflater().inflate(R.layout.list_item_wifi, parent, false);
                }

                TextView wifiSsidText = view.findViewById(R.id.wifi_ssid);
                TextView wifiDetailsText = view.findViewById(R.id.wifi_details);

                ScanResult scanResult = getItem(position);
                if (scanResult != null) {
                    wifiSsidText.setText(scanResult.SSID.isEmpty() ? "(Hidden Network)" : scanResult.SSID);

                    String capabilities = scanResult.capabilities;
                    String security = "פתוחה";
                    if (capabilities.contains("WEP") || capabilities.contains("WPA") || capabilities.contains("WPA2") || capabilities.contains("WPA3")) {
                        security = "מאובטחת";
                    }

                    int level = WifiManager.calculateSignalLevel(scanResult.level, 5);
                    String signalStrength = "";
                    for (int i = 0; i < 5; i++) {
                        signalStrength += (i < level) ? "★" : "☆";
                    }

                    String details = String.format("%s | %s | %d GHz", security, signalStrength, scanResult.frequency);
                    wifiDetailsText.setText(details);
                }
                return view;
            }
        };
        wifiList.setAdapter(adapter);
    }

    private void setupWifiListListener() {
        wifiList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ScanResult selectedNetwork = wifiScanResults.get(position);
                String networkSSID = selectedNetwork.SSID;

                if (networkSSID == null || networkSSID.isEmpty()) {
                    if (selectedNetwork.capabilities.contains("ESS") && !selectedNetwork.SSID.isEmpty()) {
                       networkSSID = selectedNetwork.SSID;
                    } else {
                        Toast.makeText(MainActivity.this, "שם רשת לא חוקי.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                // Check if already connected to this network
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                if (wifiInfo != null && networkSSID.equals(removeQuotes(wifiInfo.getSSID()))) {
                    Toast.makeText(MainActivity.this, getString(R.string.connected_to) + " " + networkSSID, Toast.LENGTH_SHORT).show();
                    return;
                }

                String capabilities = selectedNetwork.capabilities;
                if (capabilities.contains("WEP") || capabilities.contains("WPA") || capabilities.contains("WPA2") || capabilities.contains("WPA3")) {
                    promptForPassword(selectedNetwork);
                } else {
                    promptForOpenNetworkConnection(selectedNetwork);
                }
            }
        });
    }

    private void promptForPassword(ScanResult scanResult) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.password_prompt_title);
        builder.setMessage(getString(R.string.password_prompt_message) + " '" + (scanResult.SSID.isEmpty() ? "(Hidden Network)" : scanResult.SSID) + "'");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setHint("סיסמה");
        builder.setView(input);

        builder.setPositiveButton(R.string.connect, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String password = input.getText().toString();
                if (!password.isEmpty()) {
                    connectToWifi(scanResult, password);
                } else {
                    Toast.makeText(MainActivity.this, "הסיסמה אינה יכולה להיות ריקה.", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void promptForOpenNetworkConnection(ScanResult scanResult) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.open_network_connect_prompt_title);
        builder.setMessage(getString(R.string.open_network_connect_prompt_message) + " '" + (scanResult.SSID.isEmpty() ? "(Hidden Network)" : scanResult.SSID) + "'?");

        builder.setPositiveButton(R.string.connect, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                connectToWifi(scanResult, null); // null password for open networks
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void connectToWifi(ScanResult scanResult, String password) {
        statusTextView.setText(R.string.connecting);
        WifiConfiguration wifiConfiguration = new WifiConfiguration();
        wifiConfiguration.SSID = "\"" + (scanResult.SSID.isEmpty() ? "" : scanResult.SSID) + "\""; // Handle hidden network SSID

        String capabilities = scanResult.capabilities;

        if (capabilities.contains("WEP")) {
            wifiConfiguration.wepKeys = new String[]{"\"" + password + "\""};
            wifiConfiguration.wepTxKeyIndex = 0;
            wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        } else if (capabilities.contains("WPA") || capabilities.contains("WPA2") || capabilities.contains("WPA3")) {
            wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            wifiConfiguration.preSharedKey = "\"" + password + "\"";
        } else {
            // Open network
            wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        }

        // If the network is already configured, remove it to avoid issues
        List<WifiConfiguration> existingConfigs = wifiManager.getConfiguredNetworks();
        if (existingConfigs != null) {
            for (WifiConfiguration config : existingConfigs) {
                if (config.SSID.equals(wifiConfiguration.SSID)) {
                    wifiManager.removeNetwork(config.networkId);
                    break;
                }
            }
        }

        int netId = wifiManager.addNetwork(wifiConfiguration);
        if (netId != -1) {
            wifiManager.enableNetwork(netId, true);
            // wifiManager.saveConfiguration(); // Saving is implicitly handled by addNetwork/enableNetwork
            // Connection attempt will be handled by NETWORK_STATE_CHANGED_ACTION
        } else {
            Toast.makeText(this, R.string.error_connection, Toast.LENGTH_SHORT).show();
            statusTextView.setText("חיפוש Wi-Fi..."); // Reset status
        }
    }

    private void updateConnectionStatus() {
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo != null && wifiInfo.getSSID() != null && wifiInfo.getSupplicantState() == android.net.wifi.SupplicantState.COMPLETED) {
            String connectedSSID = removeQuotes(wifiInfo.getSSID());
            statusTextView.setText(getString(R.string.connected_to) + " " + connectedSSID);
            if (autoConnectEnabled && !savedNetworksSSID.contains(connectedSSID)) {
                savedNetworksSSID.add(connectedSSID);
            }
        } else {
            statusTextView.setText("Wi-Fi מנותק");
        }
    }

    private String removeQuotes(String ssid) {
        if (ssid == null) return null;
        if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
            return ssid.substring(1, ssid.length() - 1);
        }
        return ssid;
    }

    private void loadSettings() {
        autoConnectEnabled = getSharedPreferences("WifiSettings", MODE_PRIVATE).getBoolean("auto_connect_enabled", false);
        // Load saved networks if needed. For now, savedNetworksSSID is managed dynamically on connection.
    }

    private void handleAutoConnect(ScanResult scanResult) {
        if (autoConnectEnabled && savedNetworksSSID.contains(scanResult.SSID)) {
            connectToWifi(scanResult, null); // Assuming saved networks are open or password is known and not prompted
        }
    }

    private class WifiReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case WifiManager.SCAN_RESULTS_AVAILABLE_ACTION:
                        List<ScanResult> results = wifiManager.getScanResults();
                        updateWifiList(results);
                        // Attempt auto-connect for discovered networks
                        if (autoConnectEnabled) {
                            for (ScanResult result : results) {
                                handleAutoConnect(result);
                            }
                        }
                        break;
                    case WifiManager.NETWORK_STATE_CHANGED_ACTION:
                        updateConnectionStatus();
                        break;
                    case WifiManager.WIFI_STATE_CHANGED_ACTION:
                        int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
                        if (state == WifiManager.WIFI_STATE_ENABLED) {
                            statusTextView.setText("Wi-Fi הופעל. סורק...");
                            scanButton.setVisibility(View.GONE);
                            startWifiScan();
                        } else if (state == WifiManager.WIFI_STATE_DISABLED) {
                            statusTextView.setText("Wi-Fi כבוי.");
                            stopScanTimer();
                            scanButton.setText(R.string.turn_wifi_on);
                            scanButton.setVisibility(View.VISIBLE);
                            scanButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                                }
                            });
                            wifiList.setAdapter(null); // Clear the list
                        }
                        break;
                }
            }
        }
    }
}
