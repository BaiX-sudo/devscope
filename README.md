# DevScope 🔍

> **Open-source system & hardware information for Android — the CPU-Z alternative with no ads, no trackers, and full transparency.**

DevScope is a lightweight Android app that shows you everything about your device in one clean, dark-themed interface: SoC, memory, storage, battery, display, sensors, GPU, network and more. Fully offline, no data collection, no special permissions beyond what it needs to read.

[![license](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![platform](https://img.shields.io/badge/platform-Android%208.0%2B-brightgreen)](#requirements)
[![release](https://img.shields.io/github/v/release/BaiX-sudo/devscope)](https://github.com/BaiX-sudo/devscope/releases)

---

## ✨ What it shows

| Section    | Details |
| ---------- | ------- |
| 📱 Device   | manufacturer, brand, model, device, product, board, hardware |
| 🧭 System   | Android version, API level, build ID, security patch, kernel, architecture, uptime, root status |
| 🧠 CPU      | SoC (Android 12+), supported ABIs, core count, min/max frequency, governor |
| 💾 Memory   | total / available RAM, low-memory state, Java heap usage |
| 🗄️ Storage  | internal & external total / free space |
| 🔋 Battery  | level, status, health, technology, temperature, voltage, power source |
| 🖥️ Display  | resolution, density (dpi), scaled density, refresh rate, screen size |
| 🎮 GPU      | OpenGL ES version, GPU name |
| 🌐 Network  | connectivity, transport type, IP addresses |
| 📡 Sensors  | every sensor with vendor & type |

Every value can be exported as a shareable text report with one tap.

## 📥 Download

Get the latest signed APK from [Releases](https://github.com/BaiX-sudo/devscope/releases/latest), or build it yourself:

```bash
git clone https://github.com/BaiX-sudo/devscope.git
cd devscope
./gradlew assembleDebug
# APK at app/build/outputs/apk/debug/app-debug.apk
```

## 🛠️ Build a signed release

```bash
# 1. Generate a signing key (once)
keytool -genkeypair -v -keystore devscope.keystore -alias devscope \
  -keyalg RSA -keysize 2048 -validity 10000

# 2. Build the unsigned release
./gradlew assembleRelease

# 3. Align & sign (build-tools 34.0.0)
zipalign -f 4 app/build/outputs/apk/release/app-release-unsigned.apk app-aligned.apk
apksigner sign --ks devscope.keystore --out devscope.apk app-aligned.apk
apksigner verify devscope.apk
```

## 📋 Requirements

- Android **8.0 (API 26)** or later.
- Permissions used (all normal, none dangerous): `ACCESS_NETWORK_STATE` (connection type), `ACCESS_WIFI_STATE` (network details), `BATTERY_STATS` (battery readings).

## 🔒 Privacy

DevScope is **100% offline** — it reads device information locally and never sends anything anywhere. There are no ads, no analytics SDKs, and no network calls. The source is small enough to audit in minutes.

## 🧭 Project structure

```
devscope/
├── app/src/main/
│   ├── java/io/github/baixsudo/devscope/
│   │   ├── MainActivity.java     # UI rendering & share
│   │   └── HardwareInfo.java     # data collection
│   └── res/                      # layouts, themes, icons
├── build.gradle
├── settings.gradle
└── gradle/wrapper/               # reproducible builds
```

## 🤝 Contributing

Pull requests are welcome — see [CONTRIBUTING.md](CONTRIBUTING.md). Ideas: dark/light toggle, per-section export, benchmark tool, live sensor graphs.

## 📄 License

[MIT](LICENSE) © 2026 DevScope contributors.

---

### 中文简介

**DevScope** 是一款开源、无广告、零追踪的 Android 系统与硬件信息查看应用，可视为开源版的 CPU-Z。它以清爽的深色界面展示设备的 SoC、内存、存储、电池、屏幕、传感器、GPU、网络等信息，完全离线运行，不收集任何数据。支持 Android 8.0 及以上，可从 Releases 下载已签名 APK，或按源码构建。
