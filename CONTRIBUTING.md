# Contributing to DevScope

Thanks for your interest! DevScope is a small, focused app — contributions are
welcome and easy to review.

## Ground rules

- Keep it **zero analytics, zero tracking, zero ads**. Never add network calls
  or third-party telemetry.
- Additions should read **local device information only**.
- Prefer plain Android APIs over new dependencies. The app currently depends
  only on AndroidX AppCompat and Material Components.

## Development setup

1. Install Android Studio or the Android SDK (compileSdk 34) and JDK 17.
2. Clone and build:

```bash
git clone https://github.com/BaiX-sudo/devscope.git
cd devscope
./gradlew assembleDebug
```

## Adding a new info section

1. Add a `private static Section xxx(Context)` method to `HardwareInfo.java`.
2. Add it to the list in `gather(...)`.
3. Keep every call wrapped defensively — the code must not crash on devices
   where a given file or API is unavailable.

## Style

- Java, 4-space indent, no wildcard imports.
- `minSdk 26`, `targetSdk 34`.

## Reporting issues

Include your device model, Android version, and which value looks wrong.
