package de.tu_darmstadt.seemoo.nfcgate.relay;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import de.tu_darmstadt.seemoo.nfcgate.R;

/**
 * Main activity for the relay-only application variant.
 * Provides a minimal UI for configuring and starting NFC relay operations
 * without requiring root or Xposed framework.
 */
public class RelayMainActivity extends AppCompatActivity {
    private static final String TAG = "RelayMainActivity";
    
    private EditText mHostnameEdit;
    private EditText mPortEdit;
    private Switch mModeSwitch; // true = reader, false = tag
    private Button mStartStopButton;
    private TextView mStatusText;
    private TextView mLogText;
    
    private NfcAdapter mNfcAdapter;
    private RelayService mRelayService;
    private boolean mRelayActive = false;
    
    private BroadcastReceiver mLogReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");
            if (message != null) {
                logMessage(message);
            }
        }
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_relay_main);
        
        initViews();
        initNfc();
        loadSettings();
        updateUI();
        
        // Register for log messages from service
        IntentFilter filter = new IntentFilter("relay.log.message");
        registerReceiver(mLogReceiver, filter);
        
        logMessage("NFCGate Relay - No root required");
        logMessage("Minimum Android version: " + Build.VERSION_CODES.KITKAT + " (API 19) for HCE");
    }
    
    private void initViews() {
        mHostnameEdit = findViewById(R.id.edit_hostname);
        mPortEdit = findViewById(R.id.edit_port);
        mModeSwitch = findViewById(R.id.switch_mode);
        mStartStopButton = findViewById(R.id.btn_start_stop);
        mStatusText = findViewById(R.id.text_status);
        mLogText = findViewById(R.id.text_log);
        
        mStartStopButton.setOnClickListener(this::onStartStopClicked);
        
        // Mode switch listener
        mModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mModeSwitch.setText(isChecked ? "Reader Mode" : "Tag Mode (HCE)");
            saveSettings();
        });
    }
    
    private void initNfc() {
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        
        if (mNfcAdapter == null) {
            showError("NFC not supported on this device");
            mStartStopButton.setEnabled(false);
            return;
        }
        
        if (!mNfcAdapter.isEnabled()) {
            showError("NFC is disabled. Please enable NFC in settings.");
        }
        
        // Check HCE support for tag mode
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            showError("Host Card Emulation requires Android 4.4 (API 19) or higher");
            mStartStopButton.setEnabled(false);
        }
    }
    
    private void onStartStopClicked(View view) {
        if (mRelayActive) {
            stopRelay();
        } else {
            startRelay();
        }
    }
    
    private void startRelay() {
        if (!validateSettings()) {
            return;
        }
        
        saveSettings();
        
        String hostname = mHostnameEdit.getText().toString().trim();
        String portStr = mPortEdit.getText().toString().trim();
        int port = Integer.parseInt(portStr);
        boolean readerMode = mModeSwitch.isChecked();
        
        // Start relay service
        Intent serviceIntent = new Intent(this, RelayService.class);
        serviceIntent.putExtra("hostname", hostname);
        serviceIntent.putExtra("port", port);
        serviceIntent.putExtra("reader_mode", readerMode);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        
        mRelayActive = true;
        updateUI();
        
        logMessage("Starting relay in " + (readerMode ? "reader" : "tag") + " mode");
        logMessage("Connecting to " + hostname + ":" + port);
    }
    
    private void stopRelay() {
        Intent serviceIntent = new Intent(this, RelayService.class);
        stopService(serviceIntent);
        
        mRelayActive = false;
        updateUI();
        
        logMessage("Relay stopped");
    }
    
    private boolean validateSettings() {
        String hostname = mHostnameEdit.getText().toString().trim();
        String portStr = mPortEdit.getText().toString().trim();
        
        if (hostname.isEmpty()) {
            showError("Please enter a hostname");
            return false;
        }
        
        if (portStr.isEmpty()) {
            showError("Please enter a port number");
            return false;
        }
        
        try {
            int port = Integer.parseInt(portStr);
            if (port < 1 || port > 65535) {
                showError("Port must be between 1 and 65535");
                return false;
            }
        } catch (NumberFormatException e) {
            showError("Invalid port number");
            return false;
        }
        
        if (mNfcAdapter == null || !mNfcAdapter.isEnabled()) {
            showError("NFC must be enabled");
            return false;
        }
        
        return true;
    }
    
    private void updateUI() {
        mStartStopButton.setText(mRelayActive ? "Stop Relay" : "Start Relay");
        mStartStopButton.setBackgroundColor(mRelayActive ? 
            getResources().getColor(android.R.color.holo_red_dark) :
            getResources().getColor(android.R.color.holo_green_dark));
        
        mStatusText.setText(mRelayActive ? "Relay Active" : "Relay Stopped");
        mStatusText.setTextColor(mRelayActive ? 
            getResources().getColor(android.R.color.holo_green_dark) :
            getResources().getColor(android.R.color.primary_text_light));
        
        // Disable editing while relay is active
        mHostnameEdit.setEnabled(!mRelayActive);
        mPortEdit.setEnabled(!mRelayActive);
        mModeSwitch.setEnabled(!mRelayActive);
    }
    
    private void loadSettings() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mHostnameEdit.setText(prefs.getString("relay_hostname", ""));
        mPortEdit.setText(prefs.getString("relay_port", ""));
        boolean readerMode = prefs.getBoolean("relay_reader_mode", true);
        mModeSwitch.setChecked(readerMode);
        mModeSwitch.setText(readerMode ? "Reader Mode" : "Tag Mode (HCE)");
    }
    
    private void saveSettings() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("relay_hostname", mHostnameEdit.getText().toString().trim());
        editor.putString("relay_port", mPortEdit.getText().toString().trim());
        editor.putBoolean("relay_reader_mode", mModeSwitch.isChecked());
        editor.apply();
    }
    
    public void logMessage(String message) {
        Log.i(TAG, message);
        runOnUiThread(() -> {
            String timestamp = new java.text.SimpleDateFormat("HH:mm:ss", 
                java.util.Locale.getDefault()).format(new java.util.Date());
            String logEntry = timestamp + ": " + message + "\n";
            mLogText.append(logEntry);
            
            // Keep log size reasonable
            String currentLog = mLogText.getText().toString();
            String[] lines = currentLog.split("\n");
            if (lines.length > 100) {
                StringBuilder newLog = new StringBuilder();
                for (int i = lines.length - 50; i < lines.length; i++) {
                    if (i >= 0) {
                        newLog.append(lines[i]).append("\n");
                    }
                }
                mLogText.setText(newLog.toString());
            }
        });
    }
    
    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        logMessage("ERROR: " + message);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mRelayActive) {
            stopRelay();
        }
        try {
            unregisterReceiver(mLogReceiver);
        } catch (Exception e) {
            // Receiver might not be registered
        }
    }
}