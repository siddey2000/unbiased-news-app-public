# Unbiased News App

An Android application that aggregates news from multiple sources with coverage analysis to provide balanced, unbiased news coverage.

## Current Version: v1.0.0

## Features

- **Multi-Source Aggregation**: Fetches news from 16+ diverse sources
- **Coverage Analysis**: Analyzes article sentiment using AI
- **Bias Detection**: Identifies political and ideological bias in coverage
- **Topic Filtering**: Filter news by topics (Technology, Politics, World, etc.)
- **Offline Reading**: Save articles for offline access

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose
- **Networking**: OkHttp, Retrofit
- **DI**: Koin
- **Async**: Coroutines, Flow
- **AI Analysis**: OpenRouter API (Claude)

## Project Structure

```
app/src/main/
├── java/com/unbiased/news/
│   ├── data/          # Data layer (repositories, models)
│   ├── di/            # Dependency injection
│   ├── domain/        # Business logic
│   ├── presentation/  # UI layer
│   └── util/          # Utilities
├── res/               # Resources
└── assets/            # Configuration files
```

## Building

Prerequisites:
- Android Studio Hedgehog or later
- JDK 17
- Android SDK 34+

```bash
./gradlew build
```

## Contributing

This is a reference repository showcasing clean Android architecture. The active development happens in a private repository.

## License

Copyright © 2025. All rights reserved.
