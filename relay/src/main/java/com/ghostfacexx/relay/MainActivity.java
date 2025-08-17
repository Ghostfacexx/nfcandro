package com.ghostfacexx.relay;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "RelayPrefs";
    private static final String PREF_HOST = "host";
    private static final String PREF_PORT = "port";
    
    private EditText hostEditText;
    private EditText portEditText;
    private Button toggleButton;
    private TextView logTextView;
    private boolean isRelayRunning = false;
    
    private BroadcastReceiver logReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");
            if (message != null) {
                appendLog(message);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        hostEditText = findViewById(R.id.hostEditText);
        portEditText = findViewById(R.id.portEditText);
        toggleButton = findViewById(R.id.toggleButton);
        logTextView = findViewById(R.id.logTextView);
        
        logTextView.setMovementMethod(new ScrollingMovementMethod());
        
        loadPreferences();
        
        toggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleRelay();
            }
        });
        
        appendLog("NFC Relay initialized");
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter("com.ghostfacexx.relay.LOG");
        registerReceiver(logReceiver, filter);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(logReceiver);
    }
    
    private void loadPreferences() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        hostEditText.setText(prefs.getString(PREF_HOST, ""));
        portEditText.setText(prefs.getString(PREF_PORT, ""));
    }
    
    private void savePreferences() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREF_HOST, hostEditText.getText().toString());
        editor.putString(PREF_PORT, portEditText.getText().toString());
        editor.apply();
    }
    
    private void toggleRelay() {
        if (!isRelayRunning) {
            String host = hostEditText.getText().toString().trim();
            String port = portEditText.getText().toString().trim();
            
            if (host.isEmpty() || port.isEmpty()) {
                Toast.makeText(this, R.string.please_enter_host_port, Toast.LENGTH_SHORT).show();
                return;
            }
            
            savePreferences();
            
            Intent serviceIntent = new Intent(this, RelayService.class);
            serviceIntent.putExtra("host", host);
            serviceIntent.putExtra("port", Integer.parseInt(port));
            startForegroundService(serviceIntent);
            
            isRelayRunning = true;
            toggleButton.setText(R.string.stop_relay);
            appendLog("Starting relay to " + host + ":" + port);
        } else {
            Intent serviceIntent = new Intent(this, RelayService.class);
            stopService(serviceIntent);
            
            isRelayRunning = false;
            toggleButton.setText(R.string.start_relay);
            appendLog("Stopping relay");
        }
    }
    
    private void appendLog(String message) {
        String timestamp = java.text.DateFormat.getTimeInstance().format(new java.util.Date());
        String logEntry = timestamp + ": " + message + "\n";
        logTextView.append(logEntry);
        
        // Auto-scroll to bottom
        final int scrollAmount = logTextView.getLayout() != null ? 
            logTextView.getLayout().getLineTop(logTextView.getLineCount()) - logTextView.getHeight() : 0;
        if (scrollAmount > 0) {
            logTextView.scrollTo(0, scrollAmount);
        }
    }
}