# 🎓 Helply — Autonomous Student AI Operating System

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android_14+-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android 14+">
  <img src="https://img.shields.io/badge/Kotlin-2.0+-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin 2.0+">
  <img src="https://img.shields.io/badge/UI-Jetpack_Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose">
  <img src="https://img.shields.io/badge/AI_Engine-LiteRT_Gemma_4_E4B-4615B2?style=for-the-badge&logo=google&logoColor=white" alt="LiteRT Gemma">
  <img src="https://img.shields.io/badge/Privacy-100%25_Local--First-10B981?style=for-the-badge" alt="Privacy First">
</p>

---

## 📌 Overview

**Helply** is an autonomous, local-first, privacy-focused Student AI Operating System built natively for Android. Unlike standard conversational chatbots, Helply acts as an intelligent academic co-pilot that manages student deliverables, extracts calendar deadlines from circulars, calculates ATS compatibility scores, synthesizes dynamic web portfolios, and runs on-device generative AI without relying on cloud LLM APIs or compromising personal data.

---

## ✨ Key Features & Autonomous Modules

- 📚 **Module A: AI Academic Autopilot**
  - Instant OCR document parsing (ML Kit) for assignment sheets and lab guidelines.
  - Requirement extraction, multi-step research plan generation, and PDF/PPT export.

- 📩 **Module B & C: College Intelligence & Exam Focus**
  - Secure Gmail/Outlook circular parsing with OAuth 2.0 PKCE.
  - Smart email category classification (`EXAMINATION`, `ASSIGNMENT`, `PLACEMENT`).
  - Automated Android Focus Mode / DND scheduling during exam preparation windows.

- 🧠 **Module D: Personal Academic Memory**
  - Structured Room DB entity store (Projects, Skills, Certificates, Exams, Resumes).
  - SQLite-VSS vector index for semantic retrieval without model retraining.

- 🎯 **Module E & F: Placement Copilot & ATS Engine**
  - Job description requirement extraction & transparent ATS compatibility scoring (0–100%).
  - Skill gap analysis and targeted resume versioning (PDF/DOCX).

- 🌐 **Module H & I: Dynamic AI Portfolio Generator & Deployment**
  - Compiles verified academic memory records into dynamic, responsive web portfolios (8 theme presets).
  - Automated GitHub API repository creation and GitHub Actions deployment to **GitHub Pages**.

- 🎙️ **Module L & Hackathon Demo Mode**
  - On-device voice intent parsing & executable tool registry.
  - 18-step interactive presentation runner for live hackathon evaluations.

---

## 🏗️ Architecture

Helply is architected using **Clean Architecture** principles:

```
+-------------------------------------------------------------------+
|                        Presentation Layer                         |
|         (Jetpack Compose UI, ViewModels, UI State & Events)       |
+-------------------------------------------------------------------+
                                  |
                                  v
+-------------------------------------------------------------------+
|                           Domain Layer                            |
|             (Use Cases, Business Models, Validation)              |
+-------------------------------------------------------------------+
                                  |
                                  v
+-------------------------------------------------------------------+
|                            Data Layer                             |
|    (Repositories, Room Database, Network APIs, LiteRT Gemma)      |
+-------------------------------------------------------------------+
```

- **Local Storage**: Room Persistence Library encrypted with **SQLCipher** (AES-256).
- **AI Runtime**: **Google LiteRT-LM** executing quantized Gemma 4 E4B models on ARM64 NPU/GPU delegates.
- **Dependency Injection**: Dagger Hilt.

---

## 📱 Pre-built APK

The compiled debug APK is included directly in this repository:

- 📦 **Download APK**: [`Helply-v1.0-debug.apk`](./Helply-v1.0-debug.apk) (48 MB)

### 🔌 Installation via Wireless Debugging (ADB)

1. Enable **Developer Options** and **Wireless Debugging** on your Android device (Android 11+).
2. Connect your computer and device to the same Wi-Fi network.
3. Run ADB commands:

```bash
# Pair device (if pairing code required)
adb pair <DEVICE_IP>:<PORT>

# Connect via Wireless ADB
adb connect <DEVICE_IP>:<PORT>

# Install APK directly
adb -s <DEVICE_IP>:<PORT> install -r Helply-v1.0-debug.apk
```

---

## 🛠️ Building from Source

### Prerequisites
- JDK 17 LTS (`export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64`)
- Android SDK 34 (`export ANDROID_HOME=/usr/lib/android-sdk`)
- Gradle 8.7+ (Wrapper included `./gradlew`)

### Build Commands

```bash
# 1. Clone repository
git clone https://github.com/shivarajm8234/Praxis.git
cd Praxis

# 2. Build Debug APK
./gradlew assembleDebug

# 3. Build Production Release APK
./gradlew assembleRelease
```

Output location: `app/build/outputs/apk/debug/app-debug.apk`

---

## 📄 License & Privacy Guarantee

- **Privacy**: 100% Local-First on-device storage. Zero background telemetry or data monetization.
- **License**: MIT License.
