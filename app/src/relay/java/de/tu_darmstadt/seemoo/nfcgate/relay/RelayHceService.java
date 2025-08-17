package de.tu_darmstadt.seemoo.nfcgate.relay;

import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.util.Log;

/**
 * Host Card Emulation service for tag mode relay operations.
 * Handles APDU commands when the device acts as an NFC tag.
 */
public class RelayHceService extends HostApduService {
    private static final String TAG = "RelayHceService";
    
    // AID for our HCE service (must match relay_hce.xml)
    private static final String RELAY_AID = "F0010203040506";
    
    @Override
    public byte[] processCommandApdu(byte[] commandApdu, Bundle extras) {
        Log.i(TAG, "Received APDU: " + bytesToHex(commandApdu));
        
        // For now, just return a simple response
        // In a full implementation, this would relay the APDU to the server
        // and return the response from the server
        
        if (commandApdu.length >= 4) {
            // SELECT command
            if (commandApdu[1] == (byte) 0xA4) {
                Log.i(TAG, "SELECT command received");
                return new byte[]{(byte) 0x90, 0x00}; // Status OK
            }
        }
        
        // Default response for unknown commands
        Log.i(TAG, "Unknown command, returning 6D00");
        return new byte[]{0x6D, 0x00}; // Instruction not supported
    }
    
    @Override
    public void onDeactivated(int reason) {
        Log.i(TAG, "HCE deactivated, reason: " + reason);
    }
    
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02X ", b));
        }
        return result.toString().trim();
    }
}