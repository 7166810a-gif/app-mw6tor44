package com.generated.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.util.Log;
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

public class MainActivity extends Activity {

    private static final int MY_PERMISSIONS_REQUEST_CODE = 100;
    private static final int WIFI_SETTINGS_REQUEST_CODE = 101;
    private static final String TAG = "MainActivity";

    private WifiManager wifiManager;
    private ListView wifiList;
    private TextView statusTextView;
    private Button scanButton;
    private List<ScanResult> wifiScanResults;
    private WifiScanReceiver wifiScanReceiver;
    private ConnectivityManager connectivityManager;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        connectivityManager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        sharedPreferences = getSharedPreferences("WifiSettings", MODE_PRIVATE);

        wifiList = findViewById(R.id.wifiList);
        statusTextView = findViewById(R.id.statusTextView);
        scanButton = findViewById(R.id.scanButton);

        if (wifiManager == null) {
            statusTextView.setText("שגיאה: שירות ה-Wi-Fi אינו זמין.");
            return;
        }

        // Check and request permissions
        checkAndRequestPermissions();

        // Setup scan button
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!wifiManager.isWifiEnabled()) {
                    // Prompt to enable Wi-Fi if it's off
                    Intent intent = new Intent(WifiManager.ACTION_WIFI_SETTINGS);
                    startActivityForResult(intent, WIFI_SETTINGS_REQUEST_CODE);
                } else {
                    startWifiScan();
                }
            }
        });

        // Handle item clicks in the ListView
        wifiList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ScanResult scanResult = wifiScanResults.get(position);
                connectToWifi(scanResult);
            }
        });

        // Register broadcast receiver for Wi-Fi scans
        wifiScanReceiver = new WifiScanReceiver();
        IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(wifiScanReceiver, filter);

        updateWifiStatus();
        startWifiScan(); // Initial scan on startup
    }

    private void checkAndRequestPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();
        if (checkSelfPermission(Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_WIFI_STATE);
        }
        if (checkSelfPermission(Manifest.permission.CHANGE_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.CHANGE_WIFI_STATE);
        }
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (!permissionsNeeded.isEmpty()) {
            requestPermissions(permissionsNeeded.toArray(new String[0]), MY_PERMISSIONS_REQUEST_CODE);
        } else {
            // Permissions are already granted
            initializeWifiOperations();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                initializeWifiOperations();
            } else {
                statusTextView.setText(R.string.error_location_permission);
                Toast.makeText(this, R.string.error_location_permission, Toast.LENGTH_LONG).show();
                scanButton.setVisibility(View.VISIBLE);
                scanButton.setText(R.string.turn_wifi_on); // Might need to prompt user to enable Wi-Fi if permissions denied.
            }
        }
    }

    private void initializeWifiOperations() {
        if (wifiManager.isWifiEnabled()) {
            startWifiScan();
        } else {
            statusTextView.setText("Wi-Fi כבוי. אנא הפעל אותו.");
            scanButton.setVisibility(View.VISIBLE);
            scanButton.setText(R.string.turn_wifi_on);
        }
    }

    private void startWifiScan() {
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (wifiManager.isWifiEnabled()) {
                statusTextView.setText(R.string.scanning);
                scanButton.setVisibility(View.GONE);
                wifiManager.startScan();
            } else {
                statusTextView.setText("Wi-Fi כבוי. אנא הפעל אותו.");
                scanButton.setVisibility(View.VISIBLE);
                scanButton.setText(R.string.turn_wifi_on);
            }
        } else {
            // Permissions not granted, handled in onRequestPermissionsResult
            statusTextView.setText(R.string.error_location_permission);
        }
    }

    private void updateWifiStatus() {
        if (wifiManager.isWifiEnabled()) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo != null && wifiInfo.getSSID() != null) {
                String ssid = wifiInfo.getSSID();
                if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
                    ssid = ssid.substring(1, ssid.length() - 1);
                }
                statusTextView.setText(getString(R.string.connected_to) + " " + ssid);
                scanButton.setVisibility(View.GONE);
            } else {
                statusTextView.setText("Wi-Fi מופעל, לא מחובר.");
                scanButton.setVisibility(View.VISIBLE);
                scanButton.setText("סרוק רשתות");
            }
        } else {
            statusTextView.setText("Wi-Fi מנותק");
            scanButton.setVisibility(View.VISIBLE);
            scanButton.setText(R.string.turn_wifi_on);
        }
    }

    private void connectToWifi(final ScanResult scanResult) {
        if (!wifiManager.isWifiEnabled()) {
            Toast.makeText(this, "אנא הפעל Wi-Fi תחילה.", Toast.LENGTH_SHORT).show();
            return;
        }

        final String networkSSID = "\"" + scanResult.SSID + "\"";
        final String capabilities = scanResult.capabilities;

        // Check if the network is open or requires a password
        if (capabilities.contains("ESS") && !capabilities.contains("WEP") && !capabilities.contains("WPA") && !capabilities.contains("WPA2") && !capabilities.contains("WPA3")) {
            // Open network
            new AlertDialog.Builder(this)
                    .setTitle(R.string.open_network_connect_prompt_title)
                    .setMessage(getString(R.string.open_network_connect_prompt_message) + " " + scanResult.SSID + "?")
                    .setPositiveButton(R.string.connect, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            connectToOpenNetwork(scanResult.SSID);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        } else {
            // Secured network - prompt for password
            final EditText passwordEditText = new EditText(this);
            passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            passwordEditText.setHint(R.string.password_prompt_message);
            passwordEditText.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            passwordEditText.setPadding(32, 16, 32, 16); // Example padding

            new AlertDialog.Builder(this)
                    .setTitle(R.string.password_prompt_title)
                    .setMessage(getString(R.string.password_prompt_message) + " " + scanResult.SSID)
                    .setView(passwordEditText)
                    .setPositiveButton(R.string.connect, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String password = passwordEditText.getText().toString();
                            if (password.isEmpty()) {
                                Toast.makeText(MainActivity.this, "הסיסמה אינה יכולה להיות ריקה.", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            connectToSecuredNetwork(scanResult.SSID, password);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .setIcon(android.R.drawable.ic_dialog_lock_holo_dark)
                    .show();
        }
    }

    private void connectToOpenNetwork(String networkSSID) {
        WifiConfiguration wifiConfiguration = new WifiConfiguration();
        wifiConfiguration.SSID = "\"" + networkSSID + "\"";
        wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);

        int netId = wifiManager.addNetwork(wifiConfiguration);
        if (netId != -1) {
            wifiManager.enableNetwork(netId, true);
            wifiManager.saveConfiguration();
            Toast.makeText(this, "מתחבר לרשת פתוחה...", Toast.LENGTH_SHORT).show();
            // We'll rely on the ConnectivityManager to confirm connection
        } else {
            Toast.makeText(this, R.string.error_connection, Toast.LENGTH_SHORT).show();
        }
    }

    private void connectToSecuredNetwork(String networkSSID, String password) {
        WifiConfiguration wifiConfiguration = new WifiConfiguration();
        wifiConfiguration.SSID = "\"" + networkSSID + "\"";
        wifiConfiguration.preSharedKey = "\"" + password + "\"";

        // Configure security type based on capabilities (simplified)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // For Android M and above, determine capabilities more precisely
            // Simplified for demonstration: WPA2 PSK is most common
            wifiConfiguration.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        } else {
            // Older versions might use different configurations, but this is a common setup
            wifiConfiguration.hiddenSSID = false; // Assume not hidden unless specified
            wifiConfiguration.status = WifiConfiguration.Status.ENABLED;
            wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        }

        int netId = wifiManager.addNetwork(wifiConfiguration);
        if (netId != -1) {
            wifiManager.enableNetwork(netId, true);
            wifiManager.saveConfiguration();
            Toast.makeText(this, "מתחבר לרשת מוצפנת...", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.error_connection, Toast.LENGTH_SHORT).show();
        }
    }


    // Broadcast receiver to listen for Wi-Fi scan results
    private class WifiScanReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(intent.getAction())) {
                wifiScanResults = wifiManager.getScanResults();
                if (wifiScanResults != null && !wifiScanResults.isEmpty()) {
                    // Sort results by signal strength (descending)
                    wifiScanResults.sort((result1, result2) -> Integer.compare(result2.level, result1.level));

                    WifiListAdapter adapter = new WifiListAdapter(MainActivity.this, wifiScanResults);
                    wifiList.setAdapter(adapter);
                    statusTextView.setText("רשתות Wi-Fi זמינות:");
                    scanButton.setVisibility(View.VISIBLE);
                    scanButton.setText("סרוק שוב");
                } else {
                    statusTextView.setText(R.string.no_wifi_found);
                    wifiList.setAdapter(null); // Clear the list
                    scanButton.setVisibility(View.VISIBLE);
                    scanButton.setText("סרוק שוב");
                }
            }
        }
    }

    // Custom adapter for the ListView
    private class WifiListAdapter extends ArrayAdapter<ScanResult> {
        private Context mContext;
        private List<ScanResult> mWifiList;

        public WifiListAdapter(Context context, List<ScanResult> wifiList) {
            super(context, 0, wifiList);
            mContext = context;
            mWifiList = wifiList;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View listItem = convertView;
            if (listItem == null) {
                listItem = View.inflate(mContext, R.layout.list_item_wifi, null);
            }

            ScanResult currentWifi = mWifiList.get(position);

            TextView ssidTextView = listItem.findViewById(R.id.wifi_ssid);
            TextView detailsTextView = listItem.findViewById(R.id.wifi_details);

            ssidTextView.setText(currentWifi.SSID);

            // Determine signal strength and security type
            String security = "";
            if (currentWifi.capabilities.contains("WPA") || currentWifi.capabilities.contains("WPA2") || currentWifi.capabilities.contains("WPA3")) {
                security = "מוצפן";
            } else {
                security = "פתוח";
            }

            int level = WifiManager.calculateSignalLevel(currentWifi.level, 5); // 5 levels
            String signalStrength = "";
            for (int i = 0; i < 5; i++) {
                if (i < level) {
                    signalStrength += "★";
                } else {
                    signalStrength += "☆";
                }
            }

            String frequency = "";
            if (currentWifi.frequency >= 2400 && currentWifi.frequency <= 2500) {
                frequency = "2.4 GHz";
            } else if (currentWifi.frequency >= 4900 && currentWifi.frequency <= 6000) {
                frequency = "5 GHz";
            }

            detailsTextView.setText(security + " | " + signalStrength + " | " + frequency);

            return listItem;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-register receiver if it was unregistered
        if (wifiScanReceiver == null) {
            wifiScanReceiver = new WifiScanReceiver();
        }
        IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(wifiScanReceiver, filter);
        updateWifiStatus();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister receiver to avoid leaks
        if (wifiScanReceiver != null) {
            unregisterReceiver(wifiScanReceiver);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (wifiScanReceiver != null) {
            unregisterReceiver(wifiScanReceiver);
        }
    }

    // Override onOptionsItemSelected to handle menu item clicks
    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Override onCreateOptionsMenu to inflate the menu
    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }
}
