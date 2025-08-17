# NFC Relay Module

This is a standalone Android application module that provides NFC relay functionality without requiring root access or Xposed framework. The app uses only unprivileged Android APIs (API level 19+).

## Features

- **Standalone APK**: Builds as a separate application (`com.ghostfacexx.relay`)
- **No Root Required**: Uses only standard Android APIs
- **HCE Support**: Host Card Emulation for API 19+ devices
- **TCP Relay**: Forwards NFC APDU commands to a remote server over TCP
- **Minimal UI**: Simple configuration interface for host/port settings
- **Foreground Service**: Maintains persistent relay connection

## Build Instructions

To build the relay module:

```bash
./gradlew :relay:assembleDebug
```

The APK will be generated at: `relay/build/outputs/apk/debug/relay-debug.apk`

## Installation

1. Enable "Unknown sources" in Android settings
2. Install the APK: `adb install relay/build/outputs/apk/debug/relay-debug.apk`
3. Grant NFC permissions when prompted

## Usage

1. **Configure Remote Host**:
   - Open the NFC Relay app
   - Enter the remote host IP address (e.g., `192.168.1.100`)
   - Enter the remote port number (e.g., `8080`)

2. **Start Relay**:
   - Tap "Start Relay" button
   - The app will start a foreground service
   - A notification will appear showing the relay is active

3. **NFC Interaction**:
   - When an NFC reader communicates with the device
   - HCE service automatically forwards APDU commands to the remote host
   - Responses from the remote are sent back to the NFC reader

4. **Monitor Activity**:
   - View real-time log messages in the app
   - Messages show APDU forwarding activity and connection status

5. **Stop Relay**:
   - Tap "Stop Relay" button to stop the service

## HCE (Host Card Emulation) Notes

- **API Requirements**: Requires Android 4.4+ (API level 19)
- **NFC Hardware**: Device must support NFC and HCE
- **AID Configuration**: Configured to accept all APDU commands (wildcard AID "*")
- **Payment Category**: Uses "other" category to avoid conflicts with payment apps
- **Auto-Response**: Automatically forwards all received APDUs to the configured remote host

## Network Protocol

The relay uses a simple TCP protocol:

1. **Connection**: Opens a new TCP connection for each APDU exchange
2. **Send Format**: 4-byte length prefix + APDU data
3. **Response Format**: 4-byte length prefix + response data
4. **Timeout**: 5-second timeout for network operations
5. **Error Handling**: Returns SW_UNKNOWN (0x6F00) on network errors

## Architecture

```
NFC Reader → Android HCE → HceService → NetRelay → TCP → Remote Server
           ←              ←           ←         ←     ←
```

## Permissions

- `android.permission.NFC`: Required for NFC functionality
- `android.permission.INTERNET`: Required for TCP connections
- `android.permission.FOREGROUND_SERVICE`: Required for persistent service

## Troubleshooting

1. **"No host/port configured"**: Ensure both host and port are entered before starting relay
2. **"No response from remote"**: Check network connectivity and remote server status
3. **HCE not working**: Verify device supports HCE and NFC is enabled
4. **Build errors**: Ensure Android SDK and build tools are properly installed

## Technical Limitations

- **Single APDU**: Each APDU command opens a new TCP connection
- **Timeout**: 5-second timeout may not be suitable for all use cases
- **No Session State**: Does not maintain session state between APDU exchanges
- **Basic Protocol**: Simple length-prefixed protocol without encryption

## Compatibility

- **Minimum API**: Android 4.4 (API level 19)
- **Target API**: Android 13 (API level 33)
- **Architecture**: All Android architectures supported
- **Dependencies**: Only androidx.appcompat required