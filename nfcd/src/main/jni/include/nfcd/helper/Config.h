#ifndef NFCD_CONFIG_H
#define NFCD_CONFIG_H

#include <vector>
#include <memory>
#include <cstddef>
#include <cstdint>

using config_ref = std::unique_ptr<uint8_t[]>;

/*
 * Helper types: Option stores type and a vector<uint8_t> payload.
 * Build/parse use size_t for offsets/totals to avoid overflow issues.
 */

class Option {
public:
    Option(uint8_t type, const uint8_t *value, size_t len)
            : mType(type), mValue(value, value + len) { }

    std::string name() const;

    uint8_t type() const { return mType; }
    size_t len() const { return mValue.size(); }
    const uint8_t *value() const { return mValue.empty() ? nullptr : mValue.data(); }

    void value(const uint8_t *newValue, size_t newLen) {
        mValue.assign(newValue, newValue + newLen);
    }

    void push(config_ref &config, size_t &offset) const {
        // write type and length as single bytes
        config[offset + 0] = mType;
        config[offset + 1] = static_cast<uint8_t>(mValue.size() & 0xFF);
        if (!mValue.empty())
            memcpy(&config[offset + 2], mValue.data(), mValue.size());
        offset += mValue.size() + 2;
    }

protected:
    uint8_t mType;
    std::vector<uint8_t> mValue;
};

class Config {
public:
    Config() = default;

    // total bytes required
    size_t total() const { return mTotal; }

    void add(uint8_t type, const uint8_t *value, size_t len = 1) {
        mOptions.emplace_back(type, value, len);
    }

    void add(const Option &opt) {
        mOptions.push_back(opt);
    }

    void build(config_ref &config);
    void parse(size_t size, const uint8_t *stream);

    const std::vector<Option> &options() const { return mOptions; }
    std::vector<Option> &options() { return mOptions; }

protected:
    size_t mTotal = 0;
    std::vector<Option> mOptions;
};

#endif //NFCD_CONFIG_H
