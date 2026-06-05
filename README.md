<p align="center">
  <img src="metadata/logo.png" alt="Vellum Logo" width="240">
</p>

> [!WARNING]
> I'm not actively working on Vellum. Most of the design work is done using AI and the README itself is AI-generated, but hey! The app looks decent imo.
>
> I’m still around for bug fixes though — if something breaks or feels off, open an issue and I’ll take a look.

# Vellum

Vellum is a reader application for Android that supports Comic Archives (CBZ), EPUBs, and PDFs.

## Features

### Reading
- Supports CBZ, PDF, and EPUB formats.
- Page navigation via volume keys or customizable tap zones on the screen edges.
- Right-to-Left reading direction option (manga mode).
- Vertical progress indicator on the right edge of the screen to track reading completion.

### Content & File Management
- Open archives and documents directly from external file managers and applications.
- Ingestion of external files into local storage.
- Recursive folder scanner to import files from a selected directory.
- Backup and restore reading progress database via JSON export/import.
- Local network server (LAN Transfer) with PIN authentication to upload archives from a browser.

## Tech Stack
- **Languages & Frameworks**: Kotlin, Jetpack Compose.
- **Concurrency**: Kotlin Coroutines and Flows.
- **Dependency Injection**: Hilt.
- **Database & Persistence**: Room Database, Jetpack DataStore.
- **Image Loading**: Coil 3.

## Getting Started

### Installation
1. Install the application on an Android device running Android 8.0 (API level 26) or higher.
2. Tap the import/add button in the navigation bar to import archives or scan a directory.
3. Tap the center of the screen during reading to toggle overlays and chapter settings.

### Development Environment
1. Clone the repository.
2. Open the project in Android Studio.
3. Ensure JDK 17 is configured for the build environment.
4. Build using Gradle Kotlin DSL.

## Might add
The following settings are currently exposed in the preferences UI but are unimplemented in the reading engine:
- **Keep Screen On**: Prevent the device from entering sleep mode while reading.
- **High-Quality Scaling**: Toggle advanced image interpolation for sharper page rendering.
- **Long Press Menu**: Open the reader controls menu via a long press instead of a single tap.
- **Adaptive Chromaticity**: Dynamically tint UI components based on the colors of the current page or cover art.
