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

### Prerequisites
- Android SDK with API level 21+ (minimum)
- Android SDK with API level 35 (target/compile)
- Java 17 (for Gradle compatibility)
- Gradle 7.6+ (included in wrapper)

### Build Commands

```bash
# Clean any previous builds
./gradlew clean

# Build debug APK for relay flavor only
./gradlew assembleRelayDebug

# Build release APK for relay flavor only  
./gradlew assembleRelayRelease

# Build all flavors (full and relay) for comparison
./gradlew assembleDebug

# List all available build tasks
./gradlew tasks --group="build"
```

The built APKs will be located at:
- Relay Debug: `app/build/outputs/apk/relay/debug/app-relay-debug.apk`
- Relay Release: `app/build/outputs/apk/relay/release/app-relay-release.apk`

### Build Validation

A validation script is included to verify the relay configuration:

```bash
./validate_relay.sh
```

This checks that all required files are present and properly configured.

### Install

```bash
# Install via ADB (developer mode required)
adb install app/build/outputs/apk/relay/debug/app-relay-debug.apk

# Or copy APK to device and install manually through file manager
# Settings > Security > Unknown Sources must be enabled
```

### Testing the Build

The repository includes a smoke test to verify the relay APK builds correctly:

```bash
# Validate configuration
./validate_relay.sh

# Test build (if network connectivity available)
./gradlew assembleRelayDebug --dry-run
```

For CI/CD integration, the relay APK build can be tested with:
```bash
./gradlew assembleRelayDebug --offline
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
- Dependencies on nfcd module (contains native Xposed hooks)

### Simplified Implementation
- Uses standard Android NFC APIs only
- No system-level NFC service modifications
- Simplified network protocol using raw TCP sockets
- Basic HCE implementation for tag emulation
- Minimal UI focused on relay functionality
- Self-contained in relay product flavor

### Technical Changes Made

#### Build System
- Added `relay` product flavor in app/build.gradle
- Excluded nfcd module dependency for relay flavor only
- Removed Xposed repository from build configuration
- Set relay-specific application ID: `de.tu_darmstadt.seemoo.nfcgate.relay`

#### Application Structure
- New relay-specific package: `de.tu_darmstadt.seemoo.nfcgate.relay`
- Standalone activity replacing complex fragment-based navigation
- Self-contained service architecture
- No dependencies on Xposed or native components

#### NFC Implementation
- Uses standard Android `NfcAdapter.enableReaderMode()` for reading
- Uses `HostApduService` for tag emulation (HCE)
- No low-level NFC service hooks or modifications
- Limited to ISO-DEP protocol support initially

#### Network Protocol
- Simplified TCP socket communication
- Basic length-prefixed message format
- No complex protobuf integration (future enhancement)
- Direct socket management without connection pooling

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