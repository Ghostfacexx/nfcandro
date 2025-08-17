package de.tu_darmstadt.seemoo.nfcgate.relay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.tu_darmstadt.seemoo.nfcgate.R;

/**
 * Foreground service that handles NFC relay operations.
 * Manages NFC reader/HCE modes and network communication without requiring root access.
 */
public class RelayService extends Service implements NfcAdapter.ReaderCallback {
    private static final String TAG = "RelayService";
    private static final String CHANNEL_ID = "RelayServiceChannel";
    private static final int NOTIFICATION_ID = 1;
    
    private String mHostname;
    private int mPort;
    private boolean mReaderMode;
    private boolean mIsRunning = false;
    
    private NfcAdapter mNfcAdapter;
    private ExecutorService mExecutor;
    private Socket mSocket;
    private NetworkHandler mNetworkHandler;
    
    @Override
    public void onCreate() {
        super.onCreate();
        mExecutor = Executors.newCachedThreadPool();
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        createNotificationChannel();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            mHostname = intent.getStringExtra("hostname");
            mPort = intent.getIntExtra("port", 0);
            mReaderMode = intent.getBooleanExtra("reader_mode", true);
            
            startForeground(NOTIFICATION_ID, createNotification());
            startRelay();
        }
        
        return START_STICKY;
    }
    
    @Override
    public void onDestroy() {
        stopRelay();
        if (mExecutor != null) {
            mExecutor.shutdown();
        }
        super.onDestroy();
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    private void startRelay() {
        if (mIsRunning) return;
        
        Log.i(TAG, "Starting relay service - hostname: " + mHostname + ", port: " + mPort + ", reader mode: " + mReaderMode);
        mIsRunning = true;
        
        // Start network connection
        mExecutor.execute(this::connectToServer);
        
        // Setup NFC
        if (mReaderMode) {
            enableReaderMode();
        } else {
            // Tag mode (HCE) is handled by RelayHceService
            Log.i(TAG, "Tag mode - HCE service should be enabled");
        }
        
        logToActivity("Relay service started");
    }
    
    private void stopRelay() {
        if (!mIsRunning) return;
        
        Log.i(TAG, "Stopping relay service");
        mIsRunning = false;
        
        // Disable NFC reader mode
        if (mNfcAdapter != null) {
            mNfcAdapter.disableReaderMode(this);
        }
        
        // Close network connection
        if (mNetworkHandler != null) {
            mNetworkHandler.disconnect();
        }
        
        logToActivity("Relay service stopped");
    }
    
    private void enableReaderMode() {
        if (mNfcAdapter == null) {
            Log.e(TAG, "NFC adapter not available");
            return;
        }
        
        // Enable reader mode for all NFC technologies
        Bundle options = new Bundle();
        options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250);
        
        mNfcAdapter.enableReaderMode(this, this,
            NfcAdapter.FLAG_READER_NFC_A |
            NfcAdapter.FLAG_READER_NFC_B |
            NfcAdapter.FLAG_READER_NFC_F |
            NfcAdapter.FLAG_READER_NFC_V |
            NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
            options);
        
        Log.i(TAG, "NFC reader mode enabled");
        logToActivity("NFC reader mode enabled");
    }
    
    private void connectToServer() {
        try {
            Log.i(TAG, "Connecting to server: " + mHostname + ":" + mPort);
            mSocket = new Socket(mHostname, mPort);
            mNetworkHandler = new NetworkHandler(mSocket, this::onNetworkData, this::onNetworkDisconnected);
            mNetworkHandler.start();
            
            logToActivity("Connected to server: " + mHostname + ":" + mPort);
            
        } catch (IOException e) {
            Log.e(TAG, "Failed to connect to server", e);
            logToActivity("Failed to connect to server: " + e.getMessage());
            stopSelf();
        }
    }
    
    // NfcAdapter.ReaderCallback implementation
    @Override
    public void onTagDiscovered(Tag tag) {
        Log.i(TAG, "Tag discovered: " + tag);
        logToActivity("Tag discovered: " + tag.toString());
        
        // Process tag with IsoDep if available
        IsoDep isoDep = IsoDep.get(tag);
        if (isoDep != null) {
            mExecutor.execute(() -> processTag(isoDep));
        } else {
            Log.w(TAG, "Tag does not support ISO-DEP");
            logToActivity("Tag does not support ISO-DEP");
        }
    }
    
    private void processTag(IsoDep tag) {
        try {
            tag.connect();
            logToActivity("Connected to ISO-DEP tag");
            
            // For now, just send tag ID to server
            byte[] tagId = tag.getTag().getId();
            if (mNetworkHandler != null) {
                mNetworkHandler.sendData(tagId);
                logToActivity("Sent tag ID to server: " + bytesToHex(tagId));
            }
            
            // Keep tag connection open for further communication
            // In a full implementation, this would relay APDU commands
            
        } catch (IOException e) {
            Log.e(TAG, "Error processing tag", e);
            logToActivity("Error processing tag: " + e.getMessage());
        } finally {
            try {
                tag.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing tag", e);
            }
        }
    }
    
    private void onNetworkData(byte[] data) {
        Log.i(TAG, "Received data from server: " + bytesToHex(data));
        logToActivity("Received from server: " + bytesToHex(data));
        
        // In a full implementation, this would send the data to the NFC tag
        // For now, just log it
    }
    
    private void onNetworkDisconnected() {
        Log.i(TAG, "Network disconnected");
        logToActivity("Network disconnected");
        stopSelf();
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Relay Service",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("NFC Relay Service");
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }
    
    private Notification createNotification() {
        Intent intent = new Intent(this, RelayMainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("NFC Relay Active")
            .setContentText("Relaying NFC traffic to " + mHostname + ":" + mPort)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build();
    }
    
    private void logToActivity(String message) {
        // Send broadcast to activity for logging
        Intent intent = new Intent("relay.log.message");
        intent.putExtra("message", message);
        sendBroadcast(intent);
    }
    
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02X ", b));
        }
        return result.toString().trim();
    }
}