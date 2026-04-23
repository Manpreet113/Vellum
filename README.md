# Vellum

Android application for reading comic book archives (CBZ), EPUBs, and PDFs.

## Supported Formats
- **CBZ / ZIP**: Sequential image rendering from compressed archives.
- **EPUB**: Manifest parsing and chapter-based reading.
- **PDF**: Hardware-accelerated document rendering via `PdfRenderer`.

## Key Features
- **File Discovery**: Recursive scanning of directories using Android Storage Access Framework (SAF).
- **Library Persistence**: Local database storage for book metadata and library state.
- **Progress Tracking**: Persistent storage of reading position and completion status.
- **Cover Generation**: Automated thumbnail extraction from supported file types.
- **Collection Mapping**: Grouping logic based on source directory structure.
- **Paging**: Optimized list loading for large datasets via Paging 3.

## Technical Specifications
- **Language**: Kotlin 2.1.0
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 35 (Android 15)
- **UI Architecture**: Jetpack Compose with Material 3
- **Dependency Injection**: Dagger Hilt
- **Local Database**: Room (SQLite)
- **Image Pipeline**: Coil 3
- **Data Persistence**: Preferences DataStore
- **Build System**: Gradle Kotlin DSL

## Development Setup
1. Clone the repository.
2. Open the project in Android Studio (Ladybug 2024.2.1 or newer).
3. Ensure JDK 11 is configured for the project.
4. Synchronize Gradle files and build the `app` module.
