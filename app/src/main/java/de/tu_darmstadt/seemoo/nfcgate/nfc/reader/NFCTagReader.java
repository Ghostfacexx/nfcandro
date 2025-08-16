package de.tu_darmstadt.seemoo.nfcgate.nfc.reader;

import android.nfc.tech.*;
import androidx.annotation.NonNull;
import android.util.Log;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import de.tu_darmstadt.seemoo.nfcgate.nfc.config.ConfigBuilder;
import de.tu_darmstadt.seemoo.nfcgate.nfc.config.Technologies;

/**
 * Interface to all NFCTagReader-Classes.
 */
public abstract class NFCTagReader {
    final TagTechnology mReader;

    NFCTagReader(TagTechnology reader) {
        mReader = reader;
    }

    /**
     * Indicates whether the connection is open
     */
    boolean isConnected() {
        return mReader.isConnected();
    }

    /**
     * Opens the connection
     */
    public void connect() {
        try{
            mReader.connect();
        } catch(IOException e) {
            Log.e("NFCGATE", "Failed to connect to tag technology", e);
        }
    }

    /**
     * Closes the connection, no further communication will be possible
     */
    public void close() {
        try{
            mReader.close();
        } catch(IOException e) {
            Log.e("NFCGATE", "Failed to close tag technology", e);
        }
    }

    /**
     * Send a raw command to the NFC chip, receiving the answer as a byte[]
     *
     * @param command: byte[]-representation of the command to be sent
     * @return byte[]-representation of the answer of the NFC chip
     */
    public byte[] transceive(byte[] command) {
        try {
            // Prefer type-safe calls instead of reflection.
            if (mReader instanceof IsoDep) {
                return ((IsoDep) mReader).transceive(command);
            } else if (mReader instanceof NfcA) {
                return ((NfcA) mReader).transceive(command);
            } else if (mReader instanceof NfcB) {
                return ((NfcB) mReader).transceive(command);
            } else if (mReader instanceof NfcF) {
                return ((NfcF) mReader).transceive(command);
            } else if (mReader instanceof NfcV) {
                return ((NfcV) mReader).transceive(command);
            } else {
                // Fallback to reflection if an unexpected TagTechnology is present, but log it.
                Method transceive = mReader.getClass().getMethod("transceive", byte[].class);
                return (byte[]) transceive.invoke(mReader, command);
            }
        } catch (Exception e) {
            // Do not swallow exceptions silently. Log full context so captures aren't corrupted unknowingly.
            Log.e("NFCGATE", "transceive failed for " + mReader.getClass().getName() + " command="
                    + (command == null ? "null" : bytesToHex(command)), e);
            return null;
        }
    }

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    static String bytesToHex(byte[] bytes) {
        if (bytes == null) return "null";
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    /**
     * Returns a config object with options set to emulate this tag
     */
    @NonNull
    public abstract ConfigBuilder getConfig();

    /**
     * Picks the highest available technology for a given Tag
     */
    @NonNull
    public static NFCTagReader create(android.nfc.Tag tag) {
        List<String> technologies = Arrays.asList(tag.getTechList());

        // look for higher layer technology
        if (technologies.contains(Technologies.IsoDep)) {
            // an IsoDep tag can be backed by either NfcA or NfcB technology
            if (technologies.contains(Technologies.A))
                return new IsoDepReader(tag, Technologies.A);
            else if (technologies.contains(Technologies.B))
                return new IsoDepReader(tag, Technologies.B);
            else
                Log.e("NFCGATE", "Unknown tag technology backing IsoDep" +
                        android.text.TextUtils.join(", ", technologies));
        }

        for (String tech : technologies) {
            switch (tech) {
                case Technologies.A:
                    return new NfcAReader(tag);
                case Technologies.B:
                    return new NfcBReader(tag);
                case Technologies.F:
                    return new NfcFReader(tag);
                case Technologies.V:
                    return new NfcVReader(tag);
            }
        }

        throw new UnsupportedOperationException("Unknown Tag type");
    }
}
