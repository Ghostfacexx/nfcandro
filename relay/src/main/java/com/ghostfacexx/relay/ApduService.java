package com.ghostfacexx.relay;

import android.content.SharedPreferences;
import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

public class ApduService extends HostApduService {
    private static final String TAG = "ApduService";
    private static final String PREFS = "relay_prefs";

    @Override
    public byte[] processCommandApdu(byte[] apdu, Bundle extras) {
        try {
            SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
            String host = prefs.getString("host", "");
            int port = prefs.getInt("port", 0);
            if (host == null || host.isEmpty() || port == 0) {
                Log.w(TAG, "Host/port not configured");
                return statusWord(0x6F00);
            }
            NetworkManager nm = NetworkManager.INSTANCE;
            nm.configure(host, port);
            nm.start();

            SendBase sb = new SendBase(apdu, 5000);
            nm.enqueue(sb);
            byte[] resp = sb.waitForResponse();
            if (resp == null) return statusWord(0x6F00);
            return resp;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return statusWord(0x6F00);
        } catch (Exception e) {
            return statusWord(0x6F00);
        }
    }

    @Override
    public void onDeactivated(int reason) {
        Log.i(TAG, "Deactivated: " + reason);
    }

    private byte[] statusWord(int sw) {
        byte hi = (byte)((sw >> 8) & 0xFF);
        byte lo = (byte)(sw & 0xFF);
        return new byte[] { hi, lo };
    }
}