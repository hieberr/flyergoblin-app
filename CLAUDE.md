# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Claude Instructions

- Do not use kotlinx.datetime.Clock. Use kotlin.time.Clock instead.
- Do not use kotlinx.datetime.Instant. Use kotlin.time.Instant instead.

### Conventions

- For UI sizes given in DP, we have a standardized "unit" of ui space: `Ui.unit`. Sizes should generally be a multiple of `Ui.unit` if possible. Always use `Ui.unit` instead of `16.dp` and `Ui.halfUnit` instead of `8.dp`. If a size isn't an even multiple of `Ui.unit` round to the nearest. For example, instead of `20.dp` this rounds down to `Ui.unit` and `30.dp` rounds to `Ui.unit * 2`. For sizes smaller than `4.dp` it's fine to just use dp values.

### Agents Folder

There is an `agents` folder for claude related files.

`agents/tmp/`
Folder for temporary reports. Not saved in version control

`agents/tmp/codereview/`
Folder for coderviews plans to be stored.

`agents/context/`
Folder for .md files containing additional context for specific tasks. Loaded only when needed.

`agents/context/ios.md`
Contains additional useful context for working with iOS. Only read this when needed.

## Project Overview

This feature allows users to maintain a list of Music events.
Users add events by providing a screenshot of a flyer.
The flyer is processed by an AI agent to extract event details.

Event details include the musicians/artists playing at the event. Users can select these artists to find out more about them and preview tracks by them on SoundCloud.

#### Persistence

- Event data is stored on the local device using kotlin multiplatform idiomatic standard.

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
- When an artist is clicked on opens the ArtistDetail screen

##### AddEvent screen

- Asks the users to provide an image of an event flyer.
- Provides a button to browse for a file (this can be a standard file picker)
- Once the file is provided it is sent off to the remote ai agent to process. While waiting for processing a wait spinner is shown on the AddEvent screen until it completes.
- When the processing completes
    - If successful, create an event, save it, and switch to the EventDetail screen for that event.
    - If not successful, display the error and remain on the AddEve t screen.

##### ArtistDetail screen

- Shows the artist's SoundCloud profile that we found by searching
- Tapping on the SoundCloud profile button brings up the SoundCloudProfileSelection screen which allows the user to select a different SoundCloud profile from our list of profiles saved from the search request.
- Shows a button which opens the SoundCloud profile url externally.
- Shows a webview which contains SoundCloud track player widgets.

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

The desktop app's main class is defined as `com.hologrampacific.flyergoblin.MainKt` in composeApp/build.gradle.kts:127.

## Architecture Notes

### Dependency Management

Dependencies are declared using Gradle version catalogs in `gradle/libs.versions.toml`. Use the `libs.*` accessor in build scripts to reference dependencies.

Common dependencies in commonMain are automatically available to platform-specific source sets.

## Testing

Tests are located in `composeApp/src/commonTest/kotlin/`. Use `kotlin-test` library for writing platform-agnostic tests.

Tests should use the mockK library for mocking.

Test names should be descriptive using "```" marks and spaces. When words from code are used use the actual name. For example: testing the function `findSoundCloudProfile()` `fun `test findSoundCloudProfile trims whitespace`()"

## SDK Requirements

- Android: minSdk 24, targetSdk 36, compileSdk 36
- JVM: Java 21 (source and target compatibility)
- Kotlin compiler JVM target: 21