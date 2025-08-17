package com.ghostfacexx.relay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

public class RelayService extends Service {
    private static final String CHANNEL_ID = "RelayServiceChannel";
    private static final int NOTIFICATION_ID = 1;
    private static final String PREFS_NAME = "RelayPrefs";
    
    private String host;
    private int port;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            host = intent.getStringExtra("host");
            port = intent.getIntExtra("port", 0);
            
            if (host == null || port == 0) {
                // Load from preferences if not provided in intent
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                host = prefs.getString("host", "");
                port = Integer.parseInt(prefs.getString("port", "0"));
            } else {
                // Save to preferences
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("host", host);
                editor.putString("port", String.valueOf(port));
                editor.apply();
            }
        }
        
        startForeground(NOTIFICATION_ID, createNotification());
        sendLogBroadcast("Relay service started - Target: " + host + ":" + port);
        
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sendLogBroadcast("Relay service stopped");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                "Relay Service Channel",
                NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.relay_service_title))
                .setContentText(getString(R.string.relay_service_content) + " (" + host + ":" + port + ")")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build();
    }
    
    private void sendLogBroadcast(String message) {
        Intent intent = new Intent("com.ghostfacexx.relay.LOG");
        intent.putExtra("message", message);
        sendBroadcast(intent);
    }
    
    public String getHost() {
        return host;
    }
    
    public int getPort() {
        return port;
    }
}