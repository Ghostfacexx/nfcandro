#!/bin/bash

# Simple build validation script for the relay flavor

echo "=== NFCGate Relay Build Validation ==="

# Check if required files exist
echo "Checking required files..."

required_files=(
    "app/build.gradle"
    "app/src/relay/AndroidManifest.xml"
    "app/src/relay/java/de/tu_darmstadt/seemoo/nfcgate/relay/RelayMainActivity.java"
    "app/src/relay/java/de/tu_darmstadt/seemoo/nfcgate/relay/RelayService.java"
    "app/src/relay/java/de/tu_darmstadt/seemoo/nfcgate/relay/RelayHceService.java"
    "app/src/relay/java/de/tu_darmstadt/seemoo/nfcgate/relay/NetworkHandler.java"
    "app/src/relay/res/layout/activity_relay_main.xml"
    "app/src/relay/res/values/strings.xml"
    "app/src/relay/res/xml/relay_hce.xml"
    "app/src/relay/res/xml/tech.xml"
)

missing_files=()
for file in "${required_files[@]}"; do
    if [ ! -f "$file" ]; then
        missing_files+=("$file")
    fi
done

if [ ${#missing_files[@]} -eq 0 ]; then
    echo "✓ All required files present"
else
    echo "✗ Missing files:"
    for file in "${missing_files[@]}"; do
        echo "  - $file"
    done
    exit 1
fi

# Check gradle configuration
echo "Checking gradle configuration..."

if grep -q "relay" app/build.gradle; then
    echo "✓ Relay product flavor configured"
else
    echo "✗ Relay product flavor not found in build.gradle"
    exit 1
fi

if grep -q "fullImplementation project(':nfcd')" app/build.gradle; then
    echo "✓ nfcd dependency correctly scoped to full flavor"
else
    echo "✗ nfcd dependency not properly scoped"
    exit 1
fi

# Check manifest differences
echo "Checking manifest configuration..."

if grep -q "xposed" app/src/relay/AndroidManifest.xml; then
    echo "✗ Relay manifest still contains Xposed references"
    exit 1
else
    echo "✓ Relay manifest clean of Xposed references"
fi

if grep -q "RelayMainActivity" app/src/relay/AndroidManifest.xml; then
    echo "✓ RelayMainActivity configured in manifest"
else
    echo "✗ RelayMainActivity not found in manifest"
    exit 1
fi

echo ""
echo "=== Build Validation Complete ==="
echo "✓ All checks passed - relay flavor should build successfully"
echo ""
echo "To build the relay APK:"
echo "  ./gradlew assembleRelayDebug"
echo ""
echo "Install command:"
echo "  adb install app/build/outputs/apk/relay/debug/app-relay-debug.apk"