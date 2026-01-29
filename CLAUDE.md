# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Claude Instructions

There is a `claude` folder for claude related files.

`claude/context/ios.md`

- Contains additional useful context for working with iOS. Only read this when needed.

## Project Overview

This project is a sandbox for quickly prototyping independent different feature ideas.
Each feature has a package in commonMain where all code related to the feature lives.
The UI for the app has a different section for each feature. At the top level there is a navigation bar with an item for each feature.

This is a Kotlin Multiplatform (KMP) project using Compose Multiplatform for shared UI across Android, iOS, and Desktop (JVM). The project uses:

- Kotlin 2.3.0
- Compose Multiplatform 1.10.0
- Material 3 for UI components
- Gradle with version catalogs (libs.versions.toml)

Package name: `com.hologrampacific.learnkmp`

## Project Structure

The project follows KMP conventions with platform-specific and shared code:

- **composeApp/** - Main module containing all shared and platform-specific code
    - **src/commonMain/kotlin/** - Shared code for all platforms (UI, business logic)
    - **src/androidMain/kotlin/** - Android-specific implementations
    - **src/iosMain/kotlin/** - iOS-specific implementations (exports framework for Swift)
    - **src/jvmMain/kotlin/** - Desktop (JVM) specific implementations
    - **src/commonTest/kotlin/** - Shared tests

## Build Commands

### Android

Build debug APK:

```shell
./gradlew :composeApp:assembleDebug
```

### Desktop (JVM)

Run desktop application:

```shell
./gradlew :composeApp:run
```

Build desktop distribution packages (DMG, MSI, DEB):

```shell
./gradlew :composeApp:packageDistributionForCurrentOS
```

The desktop app's main class is defined as `com.hologrampacific.learnkmp.MainKt` in composeApp/build.gradle.kts:89.

## Architecture Notes

### Dependency Management

Dependencies are declared using Gradle version catalogs in `gradle/libs.versions.toml`. Use the `libs.*` accessor in build scripts to reference dependencies.

Common dependencies in commonMain are automatically available to platform-specific source sets.

## Testing

Tests are located in `composeApp/src/commonTest/kotlin/`. Use `kotlin-test` library for writing platform-agnostic tests.

## SDK Requirements

- Android: minSdk 24, targetSdk 36, compileSdk 36
- JVM: Java 21 (source and target compatibility)
- Kotlin compiler JVM target: 21