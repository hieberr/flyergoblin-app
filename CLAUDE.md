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

### Feature: Email

Not implemented yet

### Feature: Flyer

This feature allows users to maintain a list of Music events.
Users add events by providing a screenshot of a flyer.
The flyer is processed by an AI agent to extract event details.

#### Source Files

Source files for this feature should live in the `flyer` package.
The `flyer` package should have subpackages for standard architectural layers (`presentation` for UI etc.)

#### AI Agent

A remote LLM model that will process an image of a flyer and extract event details. In the future, there may be several llm calls to do additional research (e.g. look up additional info on the event artists).

- Initial implementation uses Google gemini. But, it should be easy to call another provider if we need to.

#### Persistence

- Event data is stored on the local device using kotlin multiplatform idiomatic standard.
-

#### Event Details

Event details to extract from flyers and save in th event list.

- Event Name (required)
- Event start date (required)
- Event start time (optional)
- Venue (optional)
    - The name of the venue that the event takes place
- Event url (optional)
    - The main event url provided on the flyer.
- Artists (List) (optional)
    - List of bands, djs, or artists performing.

#### UI

##### Flyer Screen

- Root screen is the top level 'Flyer' screen of the app.
- Displays a list of saved events. List can be sorted by either the date it was added, or by the date of the event itself.
- Provides a button for users to add a new event. Brings up the `AddEvent` screen
- Tapping an event in the list brings up the `EventDetail` screen.

##### EventDetail screen

- Displays all of the saved details for an event.
- User can edit any of the fields.
- Provides a button to delete the event.
- Event details

Package name: `com.hologrampacific.learnkmp`

##### AddEvent screen

- Asks the users to provide an image of an event flyer.
- Provides a button to browse for a file (this can be a standard file picker)
- Once the file is provided it is sent off to the remote ai agent to process. While waiting for processing a wait spinner is shown on the AddEvent screen until it completes.
- When the processing completes
    - If successful, create an event, save it, and switch to the EventDetail screen for that event.
    - If not successful, display the error and remain on the AddEve t screen.

## Project Structure

This is a Kotlin Multiplatform (KMP) project using Compose Multiplatform for shared UI across Android, iOS, and Desktop (JVM). The project uses:

- Kotlin 2.3.0
- Compose Multiplatform 1.10.0
- Material 3 for UI components
- Gradle with version catalogs (libs.versions.toml)
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