# Bakaláři Morphe Patches — APK Patching Scripts

Morphe patches for **cz.bakalari.mobile** (Bakaláři OnLine) cannot block sponsored ad pop-ups because this is a **Flutter application** where all ad logic lives in Dart AOT-compiled native code (`libapp.so`), not in DEX bytecode that Morphe can patch.

This repository contains scripts to patch the APK directly by replacing ad-serving CDN URLs with `localhost` in the native library.

## What Gets Blocked

| Domain | Purpose |
|--------|---------|
| `campaign.bakalari.cz` | Ad banner API |
| `campaign-dev.bakalari.cz` | Dev ad API |
| `netglade.cz` | Ad network |

## Prerequisites

- Python 3
- `zip` (for APK repacking)
- `jarsigner` / `keytool` (from Android SDK or JDK) — for re-signing
- `adb` — for installation (optional)
- `radare2` (`r2`) — for analysis (optional, already done)

## Files

| File | Purpose |
|------|---------|
| `patch_apk.py` | Python script to patch `libapp.so` domains |
| `repack_apk.py` | Script to repack the patched `.so` back into the APK |
| `README.md` | This documentation |

## Usage

### Step 1: Extract the APK

If you have an XAPK, extract the base APK first:

```bash
unzip -o -j cz.bakalari.mobile_4.3.2.xapk cz.bakalari.mobile.apk -d ./apk/
```

### Step 2: Extract libapp.so

```bash
mkdir -p extracted
cd extracted
unzip -o -j ../apk/cz.bakalari.mobile.apk lib/arm64-v8a/libapp.so
cd ..
```

### Step 3: Patch libapp.so

```bash
python3 patch_apk.py libapp.so libapp.so.patched
```

This replaces:
- `campaign.bakalari.cz` → `localhost`
- `campaign-dev.bakalari.cz` → `localhost`
- `netglade.cz` → `localhost`

### Step 4: Repack the APK

```bash
python3 repack_apk.py cz.bakalari.mobile_4.3.2.apk libapp.so.patched cz.bakalari.mobile_4.3.2_patched.apk
```

### Step 5: Re-sign the APK

```bash
# Generate keystore (one-time)
keytool -genkeypair -keystore mykeys.keystore -alias mykey \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass android -keypass android \
  -dname "CN=MorPatches, O=MorPatches, C=US"

# Sign the APK
jarsigner -keystore mykeys.keystore -storepass android \
  -keypass android cz.bakalari.mobile_4.3.2_patched.apk mykey
```

### Step 6: Install

```bash
# Uninstall original first (signature mismatch)
adb uninstall cz.bakalari.mobile

# Install patched APK
adb install cz.bakalari.mobile_4.3.2_patched.apk
```

## How It Works

The Flutter app constructs ad request URLs at runtime from string constants embedded in `libapp.so`. This patch replaces the ad-serving domain names with `localhost` followed by null bytes, making the URLs malformed:

```
Before: https://campaign.bakalari.cz/bannerinfo/...
After:  https://localhost\x00...\x00
```

The app attempts to connect to `localhost` which doesn't serve ads, so the ad request fails silently and no pop-up appears.

## Limitations

- Patches **known** ad URLs. If the app updates and changes CDN endpoints, re-patch.
- Signed with a test key — may show signature warnings on installation.
- Only blocks the specific ad domains listed above; other analytics/telemetry endpoints are not affected.

## Why Morphe Patches Don't Work Here

Morphe patches modify DEX bytecode, but this app's ad logic is in Dart code compiled to `libapp.so`. There is no DEX code that constructs or loads ad URLs — all networking goes through Dart's `dart:io` → `java.net.HttpURLConnection` (a system class not present in DEX). Binary patching of the native library is the only viable approach.
