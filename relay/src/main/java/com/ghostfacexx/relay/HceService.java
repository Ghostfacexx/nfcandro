package com.ghostfacexx.relay;

import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.util.Log;

public class HceService extends HostApduService {
    private static final String TAG = "HceService";
    private static final String PREFS_NAME = "RelayPrefs";
    private static final int TIMEOUT_MS = 5000;

    @Override
    public byte[] processCommandApdu(byte[] commandApdu, Bundle extras) {
        Log.d(TAG, "Received APDU: " + bytesToHex(commandApdu));
        sendLogBroadcast("HCE: Received APDU (" + commandApdu.length + " bytes)");
        
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String host = prefs.getString("host", "");
        String portStr = prefs.getString("port", "0");
        
        if (host.isEmpty() || portStr.equals("0")) {
            sendLogBroadcast("HCE: No host/port configured");
            return new byte[]{(byte) 0x6F, 0x00}; // SW_UNKNOWN error
        }
        
        int port = Integer.parseInt(portStr);
        
        try {
            byte[] response = NetRelay.forwardToRemote(host, port, commandApdu, TIMEOUT_MS);
            if (response != null) {
                sendLogBroadcast("HCE: Forwarded to " + host + ":" + port + ", got " + response.length + " bytes");
                Log.d(TAG, "Response APDU: " + bytesToHex(response));
                return response;
            } else {
                sendLogBroadcast("HCE: No response from remote");
                return new byte[]{(byte) 0x6F, 0x00}; // SW_UNKNOWN error
            }
        } catch (Exception e) {
            Log.e(TAG, "Error forwarding APDU", e);
            sendLogBroadcast("HCE: Error - " + e.getMessage());
            return new byte[]{(byte) 0x6F, 0x00}; // SW_UNKNOWN error
        }
    }

    @Override
    public void onDeactivated(int reason) {
        Log.d(TAG, "HCE deactivated, reason: " + reason);
        String reasonStr;
        switch (reason) {
            case DEACTIVATION_LINK_LOSS:
                reasonStr = "Link loss";
                break;
            case DEACTIVATION_DESELECTED:
                reasonStr = "Deselected";
                break;
            default:
                reasonStr = "Unknown (" + reason + ")";
                break;
        }
        sendLogBroadcast("HCE: Deactivated - " + reasonStr);
    }
    
    private void sendLogBroadcast(String message) {
        Intent intent = new Intent("com.ghostfacexx.relay.LOG");
        intent.putExtra("message", message);
        sendBroadcast(intent);
    }
    
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02X", b));
        }
        return result.toString();
    }
}