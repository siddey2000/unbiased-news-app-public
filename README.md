# Unbiased News App

An Android application that aggregates news from multiple sources with coverage analysis to provide balanced, unbiased news coverage.

---

## ⚠️ Configuration Required

### You Need an OpenRouter API Key

**The app requires an OpenRouter API key to function properly.**

The app's core features (news aggregation from RSS sources) work without a key, but **coverage analysis and AI-powered features require an API key**.

### How to Get Your API Key

1. Visit [OpenRouter](https://openrouter.ai/)
2. Sign up for a free account
3. Navigate to the API Keys section
4. Create a new API key
5. Copy the key (it starts with `sk-or-v1-`)

### How to Configure the App

After installing the APK:

1. Open the Unbiased News App
2. Go to **Settings**
3. Enter your OpenRouter API key
4. The app will save the key securely

> **Your API key is stored locally on your device and is never shared with anyone other than OpenRouter for API calls.**

---

## 📥 Download & Install

### Quick Install (No Building Required)

1. Download the APK from the releases section below
2. Transfer to your Android device
3. Tap to install (you may need to enable "Unknown Sources" in Settings → Security)
4. Open the app and configure your API key in Settings

---

## 📦 Releases

| Version | Type | Size | Download |
|---------|------|------|----------|
| v1.0.0 | Debug | 19MB | [Download APK](https://github.com/siddey2000/unbiased-news-app-public/releases/download/v1.0.0/app-v1.0.0-debug.apk) |

**[View All Releases →](https://github.com/siddey2000/unbiased-news-app-public/releases)**

---

## ✨ Features

- **Multi-Source Aggregation**: Fetches news from 16+ diverse sources
- **Coverage Analysis**: Analyzes article sentiment using AI (requires API key)
- **Bias Detection**: Identifies political and ideological bias in coverage (requires API key)
- **Topic Filtering**: Filter news by topics (Technology, Politics, World, etc.)
- **Offline Reading**: Save articles for offline access

---

## 🛠️ Build from Source

For developers who want to build the app from source code:

### Prerequisites

- Android Studio Hedgehog or later
- JDK 17
- Android SDK 34+
- OpenRouter API key (for full features)

### API Key Setup for Development

1. Get an API key from [OpenRouter](https://openrouter.ai/)
2. Create a file at `context/api_key.txt` in the project root
3. Add your API key to that file

### Build Steps

```bash
# Clone the repository
git clone https://github.com/siddey2000/unbiased-news-app-public.git
cd unbiased-news-app-public

# Build debug APK
./gradlew assembleDebug

# The APK will be at:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 📋 Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose
- **Networking**: OkHttp, Retrofit
- **DI**: Koin
- **Async**: Coroutines, Flow
- **AI Analysis**: OpenRouter API (Claude)

---

## 📁 Project Structure

```
app/src/main/
├── java/com/unbiased/news/
│   ├── data/          # Data layer (repositories, models)
│   ├── di/            # Dependency injection
│   ├── domain/        # Business logic
│   ├── ui/            # UI layer (Compose screens)
│   └── util/          # Utilities
├── res/               # Resources (layouts, strings, themes)
└── assets/            # Configuration files (sources.json)
```

---

## 🤝 Contributing

This is a reference repository showcasing clean Android architecture. The active development happens in a private repository.

Feel free to fork this repository, study the code, and use it as a reference for your own projects.

---

## 📄 License

Copyright © 2025. All rights reserved.

---

## 📧 Support

For issues, questions, or feedback, please open an issue on GitHub.

---

**Current Version: v1.0.0**
