# <p align="center"><img src="logo.png" alt="Vellum Logo" width="300"></p>

Vellum is a modern Android reader designed for comic book archives (CBZ), EPUBs, and PDFs. It focuses on a clean aesthetic, fluid navigation, and seamless system integration.

## Features

- **Format Support**:
  - **CBZ / ZIP**: Sequential image rendering for comics and manga.
  - **PDF**: High-quality hardware-accelerated rendering with white-point correction and 2x supersampling for crisp text.
  - **EPUB**: Responsive chapter-based reading with customizable themes.
- **Deep System Integration**:
  - **Universal Intent Support**: Open documents directly from WhatsApp, Telegram, or your favorite File Manager.
  - **Auto-Persistence**: Files opened from external apps are automatically imported into Vellum's internal storage for persistent library access.
  - **Smart Scanning**: Scan entire directories to build your collection instantly.
- **Reading Experience**:
  - **Adaptive Chroma**: UI elements dynamically adapt their color to match the book's cover art.
  - **Immersive Mode**: Full-screen reading with transient system bars.
  - **Manga Mode**: Right-to-Left (RTL) reading support.
  - **Hardware Controls**: Use volume keys to turn pages.
  - **Page Caching**: Intelligent pre-fetching for zero-latency page turns.

## Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Concurrency**: Kotlin Coroutines & Flow
- **Dependency Injection**: Hilt
- **Database**: Room (with Paging 3)
- **Image Loading**: Coil 3
- **Build System**: Gradle Kotlin DSL

## Getting Started

1. **Installation**: Install the latest release on your Android device (Android 8.0+).
2. **First Scan**: Tap the **Add Folder** icon to scan your existing library.
3. **Quick Open**: Use the **Open File** icon to read a single document without importing it into a collection.
4. **External Open**: Simply click on a PDF or CBZ in any other app and select **Vellum** to start reading.

## Development Setup

1. Clone the repository.
2. Open in **Android Studio (Ladybug 2024.2.1+)**.
3. Ensure **JDK 17** is configured.
4. Build the `app` module.

---
*Vellum - Crafted for a premium reading experience.*
