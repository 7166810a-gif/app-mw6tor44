package com.generated.app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

public class SettingsActivity extends Activity {

    private SharedPreferences sharedPreferences;
    private Switch autoConnectSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        sharedPreferences = getSharedPreferences("WifiSettings", MODE_PRIVATE);

        autoConnectSwitch = findViewById(R.id.autoConnectSwitch);

        loadSettings();

        autoConnectSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                saveAutoConnectSetting(isChecked);
                if (isChecked) {
                    Toast.makeText(SettingsActivity.this, "התחברות אוטומטית מופעלת.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(SettingsActivity.this, "התחברות אוטומטית כבויה.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadSettings() {
        boolean autoConnectEnabled = sharedPreferences.getBoolean("auto_connect_enabled", false);
        autoConnectSwitch.setChecked(autoConnectEnabled);
    }

    private void saveAutoConnectSetting(boolean enabled) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("auto_connect_enabled", enabled);
        editor.apply();
    }
}
