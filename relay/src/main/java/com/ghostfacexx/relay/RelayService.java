package com.ghostfacexx.relay;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class RelayService extends Service {
    public static final String EXTRA_HOST = "EXTRA_HOST";
    public static final String EXTRA_PORT = "EXTRA_PORT";
    public static final String ACTION_LOG = "com.ghostfacexx.relay.LOG";
    public static final String EXTRA_LOG = "msg";

    private static final String CHANNEL_ID = "relay_channel";
    private static final String PREFS = "relay_prefs";

    private DaemonManager daemonManager;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        NotificationCompat.Builder b = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("NFC Relay")
                .setContentText("Relay running")
                .setSmallIcon(android.R.drawable.ic_menu_send)
                .setPriority(NotificationCompat.PRIORITY_LOW);
        startForeground(1, b.build());
        daemonManager = new DaemonManager(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final String host;
        final int port;
        if (intent != null && intent.hasExtra(EXTRA_HOST)) {
            host = intent.getStringExtra(EXTRA_HOST);
            port = intent.getIntExtra(EXTRA_PORT, 0);
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .putString("host", host).putInt("port", port).apply();
        } else {
            host = getSharedPreferences(PREFS, MODE_PRIVATE).getString("host", "");
            port = getSharedPreferences(PREFS, MODE_PRIVATE).getInt("port", 0);
        }
        if (host == null || host.isEmpty() || port == 0) {
            stopSelf();
            return START_NOT_STICKY;
        }
        daemonManager.start(host, port);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        daemonManager.stop();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "Relay", NotificationManager.IMPORTANCE_LOW);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }
}