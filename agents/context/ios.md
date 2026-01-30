# iOS useful context for Claude

## Project Structure

- **iosApp/** - iOS application wrapper
    - Contains Xcode project and SwiftUI entry point
    - SwiftUI `ContentView` wraps the Compose UI via `UIViewControllerRepresentable`
    - The Compose framework is imported as `ComposeApp` and accessed via `MainViewControllerKt.MainViewController()`

## iOS Build configuration

The iOS app must be built and run through Xcode. Open the `iosApp` directory in Xcode or use the IDE's run configuration.

The iOS targets are configured for:

- `iosArm64()` - Physical devices
- `iosSimulatorArm64()` - Simulator on Apple Silicon

The Kotlin code is compiled into a static framework named `ComposeApp`

## iOS Integration

The iOS app integrates Compose UI through a SwiftUI bridge:

1. Kotlin code in `iosMain` exports `MainViewController()`
2. Swift code in `iosApp/iosApp/ContentView.swift` wraps this as a `UIViewControllerRepresentable`
3. The main SwiftUI app embeds this view

