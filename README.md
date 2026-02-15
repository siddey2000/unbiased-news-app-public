# Unbiased News App

An Android application that aggregates news from multiple sources with coverage analysis to provide balanced, unbiased news coverage.

---

## 📥 Download & Install

### Quick Install (No Building Required)

1. Download the APK from the releases section below
2. Transfer to your Android device
3. Tap to install (you may need to enable "Unknown Sources" in Settings → Security)

---

## 📦 Releases

| Version | Type | Size | Download |
|---------|------|------|----------|
| v1.0.0 | Debug | 19MB | [Download APK](https://github.com/siddey2000/unbiased-news-app-public/releases/download/v1.0.0/app-v1.0.0-debug.apk) |

**[View All Releases →](https://github.com/siddey2000/unbiased-news-app-public/releases)**

---

## ✨ Features

- **Multi-Source Aggregation**: Fetches news from 16+ diverse sources
- **Coverage Analysis**: Analyzes article sentiment using AI
- **Bias Detection**: Identifies political and ideological bias in coverage
- **Topic Filtering**: Filter news by topics (Technology, Politics, World, etc.)
- **Offline Reading**: Save articles for offline access

---

## 🛠️ Build from Source

For developers who want to build the app from source code:

### Prerequisites

- Android Studio Hedgehog or later
- JDK 17
- Android SDK 34+

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

## 🔐 API Configuration

The app uses the OpenRouter API for AI analysis. To build and run with full features:

1. Get an API key from [OpenRouter](https://openrouter.ai/)
2. Create a file at `context/api_key.txt` in the project root
3. Add your API key to that file

> **Note**: The app will still function without an API key, but coverage analysis features will be disabled.

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
