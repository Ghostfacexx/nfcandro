package com.ghostfacexx.relay;

import android.content.Context;

/**
 * Small coordinator to start/stop background managers when the app/service lifecycle changes.
 */
public class DaemonManager {
    private final Context ctx;
    private final NetworkManager nm = NetworkManager.INSTANCE;

    public DaemonManager(Context ctx) {
        this.ctx = ctx.getApplicationContext();
    }

    public void start(String host, int port) {
        nm.configure(host, port);
        nm.start();
    }

    public void stop() {
        nm.stop();
    }
}