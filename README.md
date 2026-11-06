# Bakaláři Morphe Patches — Ad Blocking for cz.bakalari.mobile

## ⚠️ Important: Morphe patches do NOT work for this app

**cz.bakalari.mobile** is a **Flutter application**. All ad-serving logic is in Dart code compiled to `libapp.so` (native library), NOT in DEX bytecode. Morphe patches can only modify DEX bytecode, so they cannot block ads in this app.

Additionally, direct binary patching of `libapp.so` **crashes the app** because it corrupts the Flutter snapshot (Dart's AOT-compiled code). The app crashes during `FlutterJNI.runBundleAndSnapshotFromLibrary` at startup.

## Solution: Frida Runtime Instrumentation

The only viable approach is to use **Frida** to hook the HTTP client at runtime and block ad requests before they are sent.

### Prerequisites

- Rooted Android device or emulator with Frida server running
- `frida-tools` installed on your computer (`pip install frida-tools`)
- The original (unpatched) `cz.bakalari.mobile` APK installed

### Files

| File | Purpose |
|------|---------|
| `cancel_ads.js` | Frida script that blocks ad-serving CDN URLs |
| `README.md` | This documentation |

### Usage

#### Step 1: Install the original APK

```bash
adb install cz.bakalari.mobile_4.3.2.apk
```

#### Step 2: Start Frida server on the device

```bash
# On the device (as root):
frida-server &
```

Or use `frida-ps` to verify Frida server is running:
```bash
frida-ps -U
```

#### Step 3: Run the patching script

```bash
frida -U -f cz.bakalari.mobile -l cancel_ads.js --no-pause
```

This will:
1. Launch the app with Frida attached
2. Hook all HTTP request methods
3. Block any request to `campaign.bakalari.cz`, `campaign-dev.bakalari.cz`, or `netglade.cz`
4. Log all blocked requests to the console

#### Alternative: Attach to a running app

If the app is already running:

```bash
frida -U -f cz.bakalari.mobile -l cancel_ads.js
```

### What Gets Blocked

| Domain | Purpose |
|--------|---------|
| `campaign.bakalari.cz` | Ad banner API |
| `campaign-dev.bakalari.cz` | Dev ad API |
| `netglade.cz` | Ad network |

### Hook Locations

The Frida script hooks multiple levels of the HTTP stack to ensure all ad requests are caught:

1. `okhttp3.OkHttpClient.newCall()` - Main OkHttp client
2. `com.google.firebase.perf.network.FirebasePerfOkHttpClient.execute()` - Firebase Performance OkHttp wrapper
3. `c8/f.j(String)` - Firebase Performance URL tracking (called for every outgoing request)
4. `java.net.HttpURLConnection.connect()` - Direct Java HTTP connections
5. `com.google.firebase.perf.network.FirebasePerfHttpClient.execute()` - Firebase Performance Apache HttpClient wrapper
6. `okhttp3.HttpUrl.Companion.d(String)` - URL parsing
7. `okhttp3.Call.execute()` - Direct method call execution

### Why This Works

- **Flutter apps** use Dart code compiled to `libapp.so`
- Dart's `http` package uses `dart:io.HttpClient` on Android
- This maps to either `okhttp3.OkHttpClient` or `java.net.HttpURLConnection`
- Frida hooks at the Java level, intercepting requests before they leave the app
- No binary modification is needed, so the Flutter snapshot remains intact

### Troubleshooting

**App crashes on startup:**
- Make sure you installed the **original** APK, not a binary-patched one
- Binary patching `libapp.so` corrupts the Flutter snapshot and causes crashes

**Ads still appear:**
- Check the Frida console output for "BLOCKED" messages
- If no blocked messages appear, the app may use a different HTTP library not covered by the hooks
- Add additional URL patterns to `BLOCKED_DOMAINS` if new ad domains appear

**Frida can't attach:**
- Ensure the device is rooted and Frida server is running
- Try `frida-ps -U` to verify Frida server is working
- Some apps have anti-Frida detection; you may need frida-server with anti-detection patches
