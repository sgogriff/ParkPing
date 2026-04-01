# ParkPing

> **An app so you never forget to register your parking again!!!**

ParkPing is an Android app that reminds you to register parking when you arrive at work. It is local-first, Android-only, and built around low-power arrival signals: geofences first, optional workplace Wi-Fi second.

## Installing on a phone

For a non-technical install flow from GitHub Releases, see `INSTALL_ANDROID.md`.

## Current Features

- Simple onboarding
- English and Finnish support
- Multiple places with geofences and optional Wi-Fi SSIDs
- OpenStreetMap preview in Setup
- Home, Details, and Setup tabs
- Reminder notifications with Mark done, Snooze, and Test reminder
- Optional per-place retrigger rules
- Rearm for the current day
- Daily reset at 04:00
- Monitoring restored after reboot

## General project layout

- `app/`: Android app module, Compose UI, receivers, workers, and reminder logic.
- `INSTALL_ANDROID.md`: step-by-step APK install guide for non-technical Android users.
- `PRIVACY.md`: plain-language privacy statement.

And some other files and junk!

## Android permissions

ParkPing requests only these permissions:

- `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION`: used for geofence setup and arrival detection.
- `ACCESS_BACKGROUND_LOCATION`: required to keep geofence arrival detection working while the app is closed.
- `POST_NOTIFICATIONS`: required on Android 13+ to show reminders.
- `ACCESS_NETWORK_STATE` and `ACCESS_WIFI_STATE`: required for the Wi-Fi transport callback and SSID lookup.
- `INTERNET`: optional; used only to load OpenStreetMap tiles for the Setup preview map.
- `RECEIVE_BOOT_COMPLETED`: required to restore monitoring after a reboot.

The app does not use an account system and does not upload parking reminders or place configuration to a backend.

## Build from source

1. Install Android Studio with a recent Android SDK and JDK 17, or use a repo-local toolchain.
2. Open this repository in Android Studio.
3. Build and run the `app` module on a device running Android 10 or newer.

Validated locally in this repo with:

```bash
JAVA_HOME="$PWD/.tools/jdk/jdk-17.0.18+8/Contents/Home" \
ANDROID_HOME="$PWD/.android-sdk" \
ANDROID_SDK_ROOT="$PWD/.android-sdk" \
GRADLE_USER_HOME="$PWD/.gradle-home" \
"$PWD/.tools/gradle/gradle-9.3.1/bin/gradle" testDebugUnitTest assembleDebug --console=plain
```

The repo-local toolchain directories `.tools/`, `.android-sdk/`, and `.gradle-home/` are intended for local testing only and are ignored by git.

## This is open source

This repository uses the Apache 2.0 license. See `LICENSE`.
