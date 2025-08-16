package de.tu_darmstadt.seemoo.nfcgate.nfc.reader;

import android.nfc.Tag;
import android.nfc.tech.IsoDep;

import androidx.annotation.NonNull;

import de.tu_darmstadt.seemoo.nfcgate.nfc.config.ConfigBuilder;
import de.tu_darmstadt.seemoo.nfcgate.nfc.config.OptionType;
import de.tu_darmstadt.seemoo.nfcgate.nfc.config.Technologies;

/**
 * Implements an NFCTagReader using the IsoDep technology
 *
 */
public class IsoDepReader extends NFCTagReader {
    private final NFCTagReader mUnderlying;

    /**
     * Provides a NFC reader interface
     *
     * @param tag: A tag using the IsoDep technology.
     */
    IsoDepReader(Tag tag, String underlying) {
        super(IsoDep.get(tag));

        // set extended timeout
        ((IsoDep) mReader).setTimeout(5000);

        // determine underlying technology
        if (underlying.equals(Technologies.A))
            mUnderlying = new NfcAReader(tag);
        else
            mUnderlying = new NfcBReader(tag);
    }

    @NonNull
    @Override
    public ConfigBuilder getConfig() {
        ConfigBuilder builder = mUnderlying.getConfig();
        IsoDep readerIsoDep = (IsoDep) mReader;

        // guard against null responses and enforce reasonable size limits
        if (mUnderlying instanceof NfcAReader) {
            byte[] hist = readerIsoDep.getHistoricalBytes();
            if (hist != null && hist.length > 0) {
                // limit to 255 bytes (option length is a single byte in the builder)
                if (hist.length > 255) {
                    byte[] trimmed = new byte[255];
                    System.arraycopy(hist, 0, trimmed, 0, 255);
                    builder.add(OptionType.LA_HIST_BY, trimmed);
                } else {
                    builder.add(OptionType.LA_HIST_BY, hist);
                }
            }
        } else {
            byte[] hi = readerIsoDep.getHiLayerResponse();
            if (hi != null && hi.length > 0) {
                if (hi.length > 255) {
                    byte[] trimmed = new byte[255];
                    System.arraycopy(hi, 0, trimmed, 0, 255);
                    builder.add(OptionType.LB_H_INFO_RSP, trimmed);
                } else {
                    builder.add(OptionType.LB_H_INFO_RSP, hi);
                }
            }
        }

        return builder;
    }
}
