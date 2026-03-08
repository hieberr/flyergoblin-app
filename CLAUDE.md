# CLAUDE.md

## Claude Instructions

- Do not use kotlinx.datetime.Clock. Use kotlin.time.Clock instead.
- Do not use kotlinx.datetime.Instant. Use kotlin.time.Instant instead.

- If adding a new icon use the google font icons. Instructions are at https://kotlinlang.org/docs/multiplatform/compose-multiplatform-resources-usage.html#icons

### Conventions

- For UI sizes given in DP, we have a standardized "unit" of ui space: `Ui.unit`. Sizes should generally be a multiple of `Ui.unit` if possible. Always use `Ui.unit` instead of `16.dp` and `Ui.halfUnit` instead of `8.dp`. If a size isn't an even multiple of `Ui.unit` round to the nearest. For example, instead of `20.dp` this rounds down to `Ui.unit` and `30.dp` rounds to `Ui.unit * 2`. For sizes smaller than `4.dp` it's fine to just use dp values. For standard icons use `Ui.standardIconSize`.

### Testing

Tests are located in `composeApp/src/commonTest/kotlin/`. Use `kotlin-test` library for writing platform-agnostic tests.

Tests should use the mockK library for mocking.

Test names should be descriptive using "```" marks and spaces. When words from code are used use the actual name. For example: testing the function `findSoundCloudProfile()` `fun `test findSoundCloudProfile trims whitespace`()"

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

### Code Intelligence

Prefer LSP over Grep/Read for code navigation — it's faster, precise, and avoids reading entire files:

- `workspaceSymbol` to find where something is defined
- `findReferences` to see all usages across the codebase
- `goToDefinition` / `goToImplementation` to jump to source
- `hover` for type info without reading the file

Use Grep only when LSP isn't available or for text/pattern searches (comments, strings, config).

After writing or editing code, check LSP diagnostics and fix errors before proceeding.

## Project Overview

This app allows users to maintain a list of Music events.
Users add events by providing a screenshot of a flyer.
The flyer is processed by an AI agent to extract event details.

Event details include the musicians/artists playing at the event. Users can select these artists to find out more about them and preview tracks by them on SoundCloud.

## Project Structure

This is a Kotlin Multiplatform (KMP) project using Compose Multiplatform for shared UI across Android, iOS, and Desktop (JVM). The project uses:

- Compose Multiplatform
- Material 3 for UI components
- Gradle with version catalogs
  The project follows KMP conventions with platform-specific and shared code:

- **composeApp/** - Main module containing all shared and platform-specific code
    - **src/commonMain/kotlin/** - Shared code for all platforms (UI, business logic)
    - **src/androidMain/kotlin/** - Android-specific implementations
    - **src/iosMain/kotlin/** - iOS-specific implementations (exports framework for Swift)
    - **src/jvmMain/kotlin/** - Desktop (JVM) specific implementations
    - **src/commonTest/kotlin/** - Shared tests

### Dependency Management

Dependencies are declared using Gradle version catalogs in `gradle/libs.versions.toml`. Use the `libs.*` accessor in build scripts to reference dependencies.

Common dependencies in commonMain are automatically available to platform-specific source sets.

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
