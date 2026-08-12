# FlyerGoblin

[![CI](https://github.com/hieberr/flyergoblin-app/actions/workflows/ci.yml/badge.svg)](https://github.com/hieberr/flyergoblin-app/actions/workflows/ci.yml)

FlyerGoblin turns a photo of a paper event flyer into a structured, searchable list of music events. Users snap or upload a flyer image, an AI agent extracts the event details (date, venue, lineup), and users can tap any artist on the lineup to look them up and preview their tracks on SoundCloud and Mixcloud.

## Highlights

- **Kotlin Multiplatform** — a single Compose Multiplatform codebase shared across Android, iOS, and Desktop (JVM), with thin platform-specific layers only where the platform requires it (e.g. iOS/Darwin networking).
- **AI-powered extraction pipeline** — flyer images are sent to a self-built AWS Lambda backend (separate repo), where an AI agent returns structured event data (venue, date, lineup) from an unstructured image. See [Flyer Processing](#flyer-processing) below.
- **Clean architecture** — feature code (see [`flyer/`](./composeApp/src/commonMain/kotlin/com/hologrampacific/flyergoblin/flyer)) is organized into `data` / `domain` / `presentation` layers with repository and use-case patterns, dependency-injected via Koin.
- **Local persistence** — SQLDelight for type-safe, multiplatform local storage of events and artists.
- **Third-party integrations** — Ktor-based clients for SoundCloud and Mixcloud, letting users preview an artist's tracks without leaving the app.
- **Tooling** — Spotless formatting enforced via a pre-commit git hook, and a `commonTest` suite (using `kotlin-test` and Mokkery for mocking) covering shared business logic across all platforms.

## Tech Stack

Kotlin Multiplatform · Compose Multiplatform · Material 3 · Koin (DI) · Ktor (networking) · SQLDelight (persistence) · Coil (image loading) · Mokkery (test mocking)

## Project Layout

* [/composeApp](./composeApp/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
    - [commonMain](./composeApp/src/commonMain/kotlin) is for code that's common for all targets.
    - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
      For example, if you want to use Apple's CoreCrypto for the iOS part of your Kotlin app,
      the [iosMain](./composeApp/src/iosMain/kotlin) folder would be the right place for such calls.
      Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./composeApp/src/jvmMain/kotlin)
      folder is the appropriate location.

* [/iosApp](./iosApp/iosApp) contains iOS applications. Even if you're sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

## Architecture

Each feature is organized into `data` / `domain` / `presentation` layers, using the [`flyer`](./composeApp/src/commonMain/kotlin/com/hologrampacific/flyergoblin/flyer) package as the primary example:

- **`domain`** — platform-agnostic business logic with no framework dependencies: models (`Artist`, `Event`), repository/data-source interfaces (`ArtistRepository`, `EventRepository`), and use cases (`ProcessFlyerUseCase`, `SearchSoundCloudProfilesUseCase`, etc.) that each encapsulate one unit of business logic.
- **`data`** — implementations of the domain interfaces: SQLDelight-backed repositories (`SqlDelightEventRepository`, `SqlDelightArtistRepository`) for local persistence, and Ktor-backed data sources/API clients (`ApiFlyerDataSource`, `SoundCloudApiClientImpl`, `MixcloudApiClientImpl`) for remote calls.
- **`presentation`** — Compose screens paired with one `ViewModel` per screen (`EventsViewModel`, `ArtistDetailViewModel`, `EditEventViewModel`, …), which depend only on use cases and repositories, never directly on data sources.

The dependency rule flows inward — `presentation` depends on `domain`, `data` depends on `domain`, and `domain` depends on nothing platform- or framework-specific — so business logic stays testable and swapping an implementation (e.g. the local database, or a remote API client) doesn't ripple into the UI layer.

**Dependency injection** is handled by Koin, wired in a single module ([`di/FlyerModule.kt`](./composeApp/src/commonMain/kotlin/com/hologrampacific/flyergoblin/di/FlyerModule.kt)): the SQLDelight driver, HTTP client, repositories, and data sources are registered as singletons; use cases are registered as factories (a fresh instance per injection site); and ViewModels are registered via `koin-compose-viewmodel`, some parameterized at injection time (e.g. `ArtistDetailViewModel` takes an `artistName`, `EventDetailViewModel` takes an `eventId`).

**Navigation** uses Jetpack [Navigation 3](https://developer.android.com/guide/navigation/navigation-3) (`androidx.navigation3`), backed by a `NavBackStack` of type-safe `NavKey` routes. Each feature contributes its own `entryProvider` builder (`flyerEntryBuilder`, `emailEntryBuilder`) rather than one global nav graph, so features stay self-contained.

**Platform-specific code** is kept to a minimum and isolated behind Kotlin's `expect`/`actual` mechanism — for example `DriverFactory` (SQLDelight driver creation), `Platform` (platform detection), and `PlatformImageUtils` (image decoding) each have one `expect` declaration in `commonMain` and an `actual` implementation per target (`androidMain`, `iosMain`, `jvmMain`). Everything else — UI, view models, business logic, networking, persistence queries — is written once in `commonMain`.

**Data flow**, end to end: Compose UI → ViewModel → UseCase → Repository/DataSource → SQLDelight (local) or Ktor (remote: the flyer-processing API, SoundCloud, Mixcloud).

## Flyer Processing

Flyer images are processed by a backend service I also built and maintain: `uedo-lambdas`, a set of AWS Lambda functions that expose the flyer-processing API this app calls. The Lambda backend owns the LLM prompt and model configuration, so the extraction logic can evolve server-side without an app release. That repo is currently private while it's still evolving, but this app is the client half of a full-stack project, not a wrapper around a third-party API.

## Getting Started

### Prerequisites

- **JDK 21** (all platforms compile against Java 21)
- **Android Studio** (latest stable) for Android development, or IntelliJ IDEA with the Kotlin Multiplatform plugin
- **Xcode 16+** for iOS development (macOS only) — the iOS target deploys to iOS 18.2+
- macOS is required to build/run the iOS target and to produce a `.dmg` desktop distribution; Android and JVM targets build on any OS

### Local Configuration

Some features require local secrets that are not checked into the repo. Copy the template and fill in your own values:

```shell
cp local.properties.template local.properties
```

`local.properties` is gitignored. It currently holds:

- `sdk.dir` — path to your Android SDK (auto-filled by Android Studio on first sync)
- `soundcloud.client.id` / `soundcloud.client.secret` — SoundCloud API credentials, used to look up artist profiles and preview tracks. Register an app at [soundcloud.com/you/apps](https://soundcloud.com/you/apps) to get these. The app builds and runs without them, but SoundCloud lookups will fail.

### Build and Run Android Application

Open the project in Android Studio, select the `composeApp` run configuration, and run on an emulator or connected device — or use the terminal:

- on macOS/Linux
  ```shell
  ./gradlew :composeApp:assembleDebug     # build a debug APK
  ./gradlew :composeApp:installDebug      # build and install on a connected device/emulator
  ```
- on Windows
  ```shell
  .\gradlew.bat :composeApp:assembleDebug
  .\gradlew.bat :composeApp:installDebug
  ```

### Build and Run Desktop (JVM) Application

Use the `composeApp` run configuration in your IDE's toolbar, or run it directly from the terminal:

- on macOS/Linux
  ```shell
  ./gradlew :composeApp:run
  ```
- on Windows
  ```shell
  .\gradlew.bat :composeApp:run
  ```

To build a distributable installer for your current OS (`.dmg` on macOS, `.msi` on Windows, `.deb` on Linux):

```shell
./gradlew :composeApp:packageDistributionForCurrentOS
```

Compose Hot Reload is enabled for this target — while running via `./gradlew :composeApp:run`, most UI/logic edits apply without a full restart.

### Build and Run iOS Application

Requires a Mac with Xcode installed.

1. Open [`iosApp/iosApp.xcodeproj`](./iosApp) in Xcode.
2. Select the `iosApp` scheme and a simulator or connected device as the run destination.
3. Run (⌘R).

Xcode builds the shared Kotlin code into a framework as part of the build phase, so no separate Gradle step is needed first. There's no CocoaPods dependency — the KMP framework is consumed directly. A `ShareExtension` target is also included, for handling flyer images shared in from other apps (e.g. Photos).

### Running Tests

Shared business logic lives in [`commonTest`](./composeApp/src/commonTest/kotlin) and runs on every platform:

```shell
./gradlew allTests           # run the shared test suite across all targets
./gradlew jvmTest            # run just the JVM target's tests (fastest for local iteration)
```

### Resetting Local Data

To clear the local event/artist database during development (desktop, a connected Android device, and a booted iOS simulator, wherever applicable):

```shell
./gradlew resetDatabase
```

## Development Setup

### Code Formatting (Spotless)

Kotlin source and Gradle build scripts are formatted with [Spotless](https://github.com/diffplug/spotless) using `ktfmt` (Google style).

```shell
./gradlew spotlessCheck   # verify formatting, fails if anything is out of style
./gradlew spotlessApply   # auto-format all Kotlin and .gradle.kts files
```

### Git Hooks

A pre-commit hook is included that runs `spotlessCheck` before each commit, so badly formatted code can't be committed by accident. Install it once after cloning:

```shell
cp hooks/pre-commit .git/hooks/pre-commit
chmod +x .git/hooks/pre-commit
```

If the hook blocks a commit, run `./gradlew spotlessApply` and re-commit.

### Continuous Integration

[`.github/workflows/ci.yml`](./.github/workflows/ci.yml) runs `spotlessCheck`, `jvmTest`, `testDebugUnitTest`, and `verifySqlDelightMigration` on every push and pull request to `main`. iOS test/build verification isn't included yet, since it needs a macOS runner.

## License

All rights reserved — see [LICENSE](./LICENSE). This repository is shared publicly for portfolio and evaluation purposes; no license is granted for reuse, modification, or redistribution.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…
