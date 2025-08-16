#include <nfcd/helper/Config.h>

#include <string>
#include <unordered_map>
#include <cstring>
#include <stdexcept>
#include <sstream>

std::unordered_map<uint8_t, std::string> knownConfigTypes = {
        // COMMON
        {0x00, "TOTAL_DURATION"},
        {0x01, "CON_DEVICES_LIMIT"},
        {0x02, "CON_DISCOVERY_PARAM"},
        {0x03, "POWER_STATE"},
        // 0x04 - 0x07 RFU

        // POLL A
        {0x08, "PA_BAIL_OUT"},
        {0x09, "PA_DEVICES_LIMIT"},
        // 0x0A - 0x0F RFU

        // POLL B
        {0x10, "PB_AFI"},
        {0x11, "PB_BAIL_OUT"},
        {0x12, "PB_ATTRIB_PARAM1"},
        {0x13, "PB_SENSB_REQ_PARAM"},
        {0x14, "PB_DEVICES_LIMIT"},
        // 0x15 - 0x17 RFU

        // POLL F
        {0x18, "PF_BIT_RATE"},
        {0x19, "PF_RC_CODE"},
        {0x1A, "PF_DEVICES_LIMIT"},
        // 0x1B - 0x1F RFU

        // POLL ISO-DEP
        {0x20, "PB_H_INFO"},
        {0x21, "PI_BIT_RATE"},
        {0x22, "PA_ADV_FEAT"},
        // 0x23 - 0x27 RFU

        // POLL NFC-DEP
        {0x28, "PN_NFC_DEP_SPEED"},
        {0x29, "PN_ATR_REQ_GEN_BYTES"},
        {0x2A, "PN_ATR_REQ_CONFIG"},
        // 0x2B - 0x2E RFU

        // POLL NFC-V
        {0x2F, "PV_DEVICES_LIMIT"},

        // LISTEN A
        {0x30, "LA_BIT_FRAME_SDD"},
        {0x31, "LA_PLATFORM_CONFIG"},
        {0x32, "LA_SEL_INFO"},
        {0x33, "LA_NFCID1"},
        // 0x34 - 0x37 RFU

        // LISTEN B
        {0x38, "LB_SENSB_INFO"},
        {0x39, "LB_NFCID0"},
        {0x3A, "LB_APPLICATION_DATA"},
        {0x3B, "LB_SFGI"},
        {0x3C, "LB_FWI_ADC_FO"},
        // 0x3D RFU
        {0x3E, "LB_BIT_RATE"},
        // 0x3F RFU

        // LISTEN F
        {0x40, "LF_T3T_IDENTIFIERS_1"},
        {0x41, "LF_T3T_IDENTIFIERS_2"},
        {0x42, "LF_T3T_IDENTIFIERS_3"},
        {0x43, "LF_T3T_IDENTIFIERS_4"},
        {0x44, "LF_T3T_IDENTIFIERS_5"},
        {0x45, "LF_T3T_IDENTIFIERS_6"},
        {0x46, "LF_T3T_IDENTIFIERS_7"},
        {0x47, "LF_T3T_IDENTIFIERS_8"},
        {0x48, "LF_T3T_IDENTIFIERS_9"},
        {0x49, "LF_T3T_IDENTIFIERS_10"},
        {0x4A, "LF_T3T_IDENTIFIERS_11"},
        {0x4B, "LF_T3T_IDENTIFIERS_12"},
        {0x4C, "LF_T3T_IDENTIFIERS_13"},
        {0x4D, "LF_T3T_IDENTIFIERS_14"},
        {0x4E, "LF_T3T_IDENTIFIERS_15"},
        {0x4F, "LF_T3T_IDENTIFIERS_16"},
        {0x50, "LF_PROTOCOL_TYPE"},
        // 0x51 - 0x57 RFU

        // LISTEN ISO-DEP
        {0x58, "LI_A_RATS_TB1"},
        {0x59, "LA_HIST_BY"},
        {0x5A, "LB_H_INFO_RESP"},
        {0x5B, "LI_BIT_RATE"},
        {0x5C, "LI_A_RATS_TC1"},
        // 0x5D - 0x5F RFU

        // LISTEN NFC-DEP
        {0x60, "LN_WT"},
        {0x61, "LN_ATR_RES_GEN_BYTES"},
        {0x62, "LN_ATR_RES_CONFIG"},
        // 0x63 - 0x67 RFU

        // ACTIVE / WLC / OTHER
        {0x68, "PACM_BIT_RATE"},
        {0x69, "WLC_CAP_POWER_CLASS"},
        {0x6A, "TOT_POWER_STEPS"},
        {0x6B, "WLC_AUTO_CAPABILITIES"},
        // 0x6C - 0x7F RFU

        // OTHER
        {0x80, "RF_FIELD_INFO"},
        {0x81, "RF_NFCEE_ACTION"},
        {0x82, "NFCDEP_OP"},
        {0x83, "LLCP_VERSION"},
        {0x85, "NFCC_CONFIG_CONTROL"},
        {0x86, "RF_WLC_STATUS_CONFIG"},
        // 0x87 - 0x9F RFU
};

std::string Option::name() const {
    auto it = knownConfigTypes.find(mType);
    if (it != knownConfigTypes.end()) return it->second;
    std::stringstream ss;
    ss << "Unknown(0x" << std::hex << (int)mType << ")";
    return ss.str();
}

void Option::push(config_ref &config, size_t &offset) const {
    /*
     * Each config option has:
     * - 1 byte type
     * - 1 byte length
     * - length byte data
     *
     * offset is a size_t to avoid overflow when totals > 255.
     */
    if (offset + 2 > mTotal && mTotal != 0) // sanity check if caller set mTotal
        throw std::runtime_error("Config::push: offset out of range");

    config.get()[offset + 0] = type();
    // clamp length to 255 for the single-byte length field
    uint8_t length_byte = static_cast<uint8_t>(len() & 0xFF);
    config.get()[offset + 1] = length_byte;

    if (len() > 0)
        memcpy(&config.get()[offset + 2], value(), len());
    offset += len() + 2;
}

void Config::build(config_ref &config) {
    mTotal = 0;

    // calculate total size of needed buffer
    for (const auto &opt : mOptions) {
        // each option contributes len + 2
        mTotal += opt.len() + 2;
    }

    if (mTotal == 0) {
        config.reset(nullptr);
        return;
    }

    // allocate buffer
    config.reset(new uint8_t[mTotal]);
    memset(config.get(), 0, mTotal);

    // push each option to buffer
    size_t offset = 0;
    for (const auto &opt : mOptions) {
        // safety: prevent extremely large options (Java side limits to 255)
        if (opt.len() > 65535)
            throw std::runtime_error("Config::build: option too large");

        opt.push(config, offset);
    }
}

void Config::parse(size_t size, const uint8_t *stream) {
    mOptions.clear();
    mTotal = 0;

    if (stream == nullptr || size == 0) return;

    size_t offset = 0;
    // iterate while there's at least type+len available
    while (offset + 2 <= size) {
        uint8_t type = stream[offset + 0];
        uint8_t len = stream[offset + 1];
        size_t length = static_cast<size_t>(len);

        // bounds check: if the payload doesn't fit, stop parsing (malformed)
        if (offset + 2 + length > size) {
            // truncated stream: break to avoid reading out-of-bounds
            break;
        }

        // add an Option with a copy of the payload
        add(type, &stream[offset + 2], length);
        offset += length + 2;
    }
}
