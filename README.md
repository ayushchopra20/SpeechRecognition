# 🎤 Android Speech Recognition App (Google Cloud REST API)

This Android app records audio using the device's microphone and converts it into text using **Google Cloud Speech-to-Text REST API**. It supports multi-language transcription and processes audio in short (e.g. 5-second) real-time-like chunks.

---

## 🚀 Features

- 🎙️ Real-time voice recording via `AudioRecord`
- 🔁 Uploads `.wav` files to Google Cloud Speech-to-Text
- 🌍 Supports multiple languages (`en-US`, `hi-IN`, etc.)
- 📤 Uses REST API (not gRPC) for wide Android compatibility
- 🔐 Authenticated with Google Cloud service account

---

## 📦 Prerequisites

1. **Google Cloud Project** with:
   - Speech-to-Text API **enabled**
   - A **Service Account** created with `"Cloud Speech Admin"` role
   - A **service account key file** (`credentials.json`)

2. **Android Studio** Arctic Fox or later

3. **Internet permission** and **Microphone permission**

---
Note add credentials.json at following folder app/src/main/res/raw/credentials.json

