# Bakaláři Morphe Patches - Disable Sponsored Ad Pop-ups

## Repository Status

⚠️ **Important**: This is a Flutter application. Morphe patches (bytecode patches) do NOT work because all ad logic is in Dart code compiled to `libapp.so`, not in DEX bytecode.

Binary patching `libapp.so` also crashes the app because it corrupts the Flutter snapshot.

**The only working solution is Frida runtime instrumentation.**

---

## Prerequisites

- Rooted Android device with Frida server running
- `frida-tools` installed (`pip install frida-tools`)
- Original `cz.bakalari.mobile_4.3.2.apk` installed

## Files

| File | Purpose |
|------|---------|
| `cancel_ads.js` | Frida script - blocks ad-serving CDN URLs at runtime |
| `README.md` | This documentation |

## How to Use

### Step 1: Install the Original APK

```bash
adb install cz.bakalari.mobile_4.3.2.apk
```

### Step 2: Start Frida Server on Device

```bash
# Push frida-server to device
adb push frida-server /data/local/tmp/frida-server
adb shell "chmod +x /data/local/tmp/frida-server"

# Start frida-server
adb shell "/data/local/tmp/frida-server &"
```

### Step 3: Run the Ad-Blocking Script

```bash
frida -U -l cancel_ads.js -f cz.bakalari.mobile
```

This will:
1. Launch the app with Frida attached
2. Hook all HTTP client methods
3. Block any request to `campaign.bakalari.cz`, `campaign-dev.bakalari.cz`, or `netglade.cz`
4. Log all blocked requests to the console

### What Gets Blocked

| Domain | Purpose |
|--------|---------|
| `campaign.bakalari.cz` | Ad banner API |
| `campaign-dev.bakalari.cz` | Dev ad API |
| `netglade.cz` | Ad network |

### Hook Locations

The Frida script hooks multiple levels of the HTTP stack:

| Hook | Target |
|------|--------|
| `okhttp3.OkHttpClient.newCall()` | Main OkHttp client |
| `okhttp3.Call.execute()` | Direct OkHttp call execution |
| `com.google.firebase.perf.network.FirebasePerfOkHttpClient.execute()` | Firebase Performance OkHttp wrapper |
| `c8/f.j(String)` | Firebase Performance URL tracking (called for every request) |
| `java.net.HttpURLConnection.connect()` | Direct Java HTTP connections |
| `okhttp3.HttpUrl.Companion.d()` | URL parsing |

### How It Works

1. Flutter app uses `dart:io.HttpClient` → `okhttp3.OkHttpClient` → `java.net.HttpURLConnection`
2. Frida hooks each layer before the request is sent
3. If the URL matches a blocked domain, the request is thrown with an error
4. The ad never loads, no pop-up appears

### Troubleshooting

**Frida server not running:**
```bash
adb push frida-server /data/local/tmp/frida-server
adb shell "chmod +x /data/local/tmp/frida-server"
adb shell "/data/local/tmp/frida-server &"
```

**App crashes on startup:**
- Make sure you installed the **original** APK, not a binary-patched one
- Binary patching `libapp.so` corrupts the Flutter snapshot

**Frida can't attach:**
- Ensure device is rooted and frida-server is running
- Try `frida-ps -U` to verify frida-server is working

**Ads still appear:**
- Check Frida console output for "BLOCKED" messages
- Add additional ad domains to `BLOCKED_DOMAINS` array if new domains appear

## Repository Contents

| File | Purpose |
|------|---------|
| `cancel_ads.js` | Frida script - blocks ad CDN URLs at runtime |
| `README.md` | This documentation |

## Why Binary Patching Fails

This is a Flutter app. All ad logic is in Dart code compiled to `libapp.so`. Morphe patches target DEX bytecode which doesn't contain ad logic. Binary patching `libapp.so` corrupts the Flutter snapshot and causes startup crashes.

**Frida runtime instrumentation is the only viable approach for this app.**
