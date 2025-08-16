package de.tu_darmstadt.seemoo.nfcgate.nfc.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

/**
 * Represents a NCI config stream.
 * Parses an existing stream or builds a new one from options.
 */
public class ConfigBuilder {
    private final List<ConfigOption> mOptions = new ArrayList<>();

    public ConfigBuilder() { }

    public ConfigBuilder(byte[] config) {
        parse(config);
    }

    public void add(OptionType ID, byte[] data) {
        if (data == null) return;
        if (data.length > 255)
            throw new IllegalArgumentException("Option data too large (>255)");
        mOptions.add(new ConfigOption(ID, data));
    }

    public void add(OptionType ID, byte data) {
        mOptions.add(new ConfigOption(ID, data));
    }

    public void add(ConfigOption option) {
        mOptions.add(option);
    }

    public List<ConfigOption> getOptions() {
        return mOptions;
    }

    private void parse(byte[] config) {
        mOptions.clear();
        if (config == null) return;

        int index = 0;
        final int total = config.length;

        // treat type/length as unsigned and bounds-check
        while (index + 2 <= total) {
            int type = config[index] & 0xFF;
            int length = config[index + 1] & 0xFF;

            if (length == 0) {
                // zero-length is allowed but still consumes 2 bytes
                add(OptionType.fromType((byte) type), new byte[0]);
                index += 2;
                continue;
            }

            if (index + 2 + length > total) {
                // truncated/malformed stream: stop parsing to avoid exceptions
                break;
            }

            byte[] data = Arrays.copyOfRange(config, index + 2, index + 2 + length);
            add(OptionType.fromType((byte) type), data);
            index += length + 2;
        }
    }

    public byte[] build() {
        int length = 0;

        for (ConfigOption option : mOptions) {
            length += option.len() + 2;
        }

        byte[] data = new byte[length];
        int offset = 0;

        for (ConfigOption option : mOptions) {
            option.push(data, offset);
            offset += option.len() + 2;
        }

        return data;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();

        for (ConfigOption option : mOptions) {
            if (result.length() > 0)
                result.append("\n");

            result.append(option.toString());
        }

        return result.toString();
    }
}
