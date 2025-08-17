package com.ghostfacexx.relay;

import android.content.Context;
import android.nfc.NfcAdapter;
import android.util.Log;

public class NfcManager {
    private static final String TAG = "NfcManager";
    private final Context ctx;

    public NfcManager(Context ctx) {
        this.ctx = ctx.getApplicationContext();
    }

    public boolean isNfcAvailable() {
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(ctx);
        return adapter != null && adapter.isEnabled();
    }
}