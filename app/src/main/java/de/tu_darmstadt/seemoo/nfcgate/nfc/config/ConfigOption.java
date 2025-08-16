package de.tu_darmstadt.seemoo.nfcgate.nfc.config;

import de.tu_darmstadt.seemoo.nfcgate.util.Utils;

/**
 * Represents a single NCI configuration option with an option code, its length and data
 */
public class ConfigOption {
    private final OptionType mID;
    private final byte[] mData;

    ConfigOption(OptionType ID, byte[] data) {
        mID = ID;
        mData = data == null ? new byte[0] : data;
    }

    ConfigOption(OptionType ID, byte data) {
        this(ID, new byte[] { data });
    }

    public int len() {
        return mData.length;
    }

    public void push(byte[] data, int offset) {
        if (mData.length > 255)
            throw new IllegalStateException("ConfigOption length > 255");

        data[offset] = mID.getID();
        data[offset + 1] = (byte) (mData.length & 0xFF);

        if (mData.length > 0)
            System.arraycopy(mData, 0, data, offset + 2, mData.length);
    }

    // from https://stackoverflow.com/a/9855338/207861
    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();

        result.append("Type: ");
        result.append(mID.toString());

        if (mData.length > 1) {
            result.append(" (");
            result.append(mData.length);
            result.append(")");
        }

        result.append(", Value: 0x");
        result.append(bytesToHex(mData));

        return result.toString();
    }
}
