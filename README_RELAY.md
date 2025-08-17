# NFCGate Relay - Standalone Application

This is a relay-only variant of NFCGate that works without root access or the Xposed framework. It provides a minimal interface for NFC traffic relaying using only standard Android SDK APIs.

## Features

- **No Root Required**: Works on standard, unrooted Android devices
- **No Xposed Framework**: Uses only official Android SDK APIs
- **NFC Relay**: Relay NFC traffic between devices over TCP network connection
- **Reader Mode**: Read NFC tags and forward data to remote server
- **Tag Mode (HCE)**: Emulate NFC tags using Host Card Emulation
- **Simple UI**: Minimal interface focused on relay functionality
- **Foreground Service**: Reliable background operation

## Requirements

### Device Requirements
- Android 4.4+ (API level 19+) for Host Card Emulation support
- NFC hardware capability
- Network connectivity (WiFi or mobile data)

### Server Requirements
- Any device running a compatible NFCGate server application
- TCP connectivity between relay device and server

## Building

### Build the Relay APK

```bash
# Clean build
./gradlew clean

# Build debug APK for relay flavor
./gradlew assembleRelayDebug

# Build release APK for relay flavor  
./gradlew assembleRelayRelease
```

The built APK will be located at:
- Debug: `app/build/outputs/apk/relay/debug/app-relay-debug.apk`
- Release: `app/build/outputs/apk/relay/release/app-relay-release.apk`

### Install

```bash
# Install via ADB
adb install app/build/outputs/apk/relay/debug/app-relay-debug.apk

# Or copy to device and install manually
```

## Usage

### Setup

1. Install the relay APK on your Android device
2. Ensure NFC is enabled in device settings
3. Have a compatible NFCGate server running and accessible over the network

### Running a Relay Session

1. **Launch the app**: Open "NFCGate Relay" from your app drawer

2. **Configure server connection**:
   - Enter the hostname/IP address of your NFCGate server
   - Enter the port number the server is listening on

3. **Select relay mode**:
   - **Reader Mode**: Device will read NFC tags and forward data to server
   - **Tag Mode (HCE)**: Device will emulate an NFC tag using data from server

4. **Start relay**: Tap "Start Relay" to begin the relay session

5. **Relay operation**:
   - In Reader Mode: Place the device near NFC tags to read and relay their data
   - In Tag Mode: Other NFC readers can interact with this device as if it were a tag

6. **Monitor activity**: Check the log section for real-time status and data transfer information

7. **Stop relay**: Tap "Stop Relay" to end the session

## Architecture

The relay application consists of several key components:

- **RelayMainActivity**: Main UI for configuration and control
- **RelayService**: Foreground service that handles NFC operations and network communication
- **RelayHceService**: Host Card Emulation service for tag mode
- **NetworkHandler**: TCP socket management for server communication

## Differences from Full NFCGate

This relay-only variant has the following limitations compared to the full NFCGate application:

### Removed Features
- All Xposed framework hooks and native code modifications
- Root-level NFC service manipulation
- Advanced NFC configuration and low-level protocol manipulation
- Clone mode and other advanced features
- Complex logging and analysis tools

### Simplified Implementation
- Uses standard Android NFC APIs only
- No system-level NFC service modifications
- Simplified network protocol
- Basic HCE implementation
- Minimal UI focused on relay functionality

## Troubleshooting

### Common Issues

**"NFC not supported"**
- Ensure your device has NFC hardware
- Check that NFC is enabled in Android settings

**"Host Card Emulation requires Android 4.4+"**
- Tag mode requires HCE support (API 19+)
- Use Reader mode on older devices

**"Failed to connect to server"**
- Verify server hostname and port
- Check network connectivity
- Ensure server is running and accessible
- Check firewall settings

**"Tag does not support ISO-DEP"**
- Some NFC tags don't support the ISO-DEP protocol
- Try with different tag types or use full NFCGate for broader compatibility

### Logs

The app provides real-time logging in the main interface. For more detailed logs, use:

```bash
adb logcat -s RelayMainActivity RelayService RelayHceService NetworkHandler
```

## Security Considerations

- Network traffic is not encrypted by default
- Use secure networks or implement additional encryption if needed
- The relay server should be trusted as it can access NFC data
- Be aware of local regulations regarding NFC usage and data interception

## Contributing

This relay-only variant is designed to be a minimal, standalone implementation. For full functionality, please refer to the main NFCGate project.

## License

Same license as the main NFCGate project.