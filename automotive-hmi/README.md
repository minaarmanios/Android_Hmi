# Suspension Control App - Android Automotive OS

A complete, runnable Android Automotive OS application for vehicle suspension mode control.

## Features

- **4 Suspension Modes**: COMFORT, SPORT, OFF-ROAD, AUTO
- **VHAL Integration**: Uses Vehicle Property API (0x11400101 for custom suspension mode)
- **Safety Features**: Speed restriction warnings, confirmation dialog for OFF-ROAD above 30 km/h
- **Error Handling**: ECU timeout (3s), mode unavailable, VHAL errors
- **Material Design 3**: Dark automotive theme optimized for driving

## Project Structure

```
SuspensionControlApp/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/suspensioncontrol/
│   │   │   ├── di/AppModule.kt              # Hilt DI
│   │   │   ├── data/
│   │   │   │   ├── CarPropertyService.kt    # VHAL integration
│   │   │   │   └── SuspensionRepository.kt  # Data repository
│   │   │   ├── domain/
│   │   │   │   ├── model/
│   │   │   │   │   ├── SuspensionMode.kt    # Mode sealed class
│   │   │   │   │   └── SuspensionUiState.kt # UI state
│   │   │   │   └── contracts/SuspensionContract.kt
│   │   │   ├── ui/
│   │   │   │   ├── SuspensionScreen.kt      # Compose UI
│   │   │   │   ├── SuspensionViewModel.kt   # ViewModel
│   │   │   │   └── theme/Theme.kt           # Material 3 theme
│   │   │   ├── MainActivity.kt
│   │   │   └── SuspensionControlApp.kt      # Application class
│   │   └── res/
│   │       ├── values/colors.xml, strings.xml, themes.xml
│   │       └── xml/automotive_app_desc.xml
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## Requirements

- **Android Studio**: Arctic Fox or newer
- **JDK**: 17 or higher
- **Android SDK**: API level 34
- **Emulator**: Android Automotive OS 14 (API 34) - Polestar 2 system image

## Setup Steps

### 1. Open in Android Studio

1. Open Android Studio
2. File → Open → Select `automotive-hmi` folder
3. Wait for Gradle sync to complete

### 2. Create AVD (Android Virtual Device)

1. Tools → AVD Manager
2. Click "Create Virtual Device"
3. Select "Automotive" category
4. Choose "Polestar 2" (or any Android Automotive OS image)
5. Select API Level 34 (or recommended)
6. Click "Finish"

### 3. Run on Emulator

1. Select the Automotive emulator from run configuration
2. Click Run → Run 'app'
3. The app will launch in the emulator

### 4. Testing with Mock Mode

When running in DEBUG mode (default for emulator), the app uses `MockCarPropertyService` which:
- Simulates mode changes with 1.5s delay
- Simulates vehicle speed at 45 km/h
- Blocks OFF-ROAD mode above 80 km/h

To test different speeds, modify `MockCarPropertyService.kt`:

```kotlin
// Line ~180 - Set different speeds
init {
    currentSpeed = 45f / 3.6f  // Change this value
}
```

## Build Commands

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Run tests
./gradlew test
```

## Emulator Setup

### Recommended AVD Configuration

| Setting | Value |
|---------|-------|
| Device | Polestar 2 |
| API Level | 34 |
| System Image | Android 14 (API 34) with Google Play |
| Display | 1280x720 landscape |
| RAM | 2048 MB |
| Heap | 512 MB |

### Starting the Emulator

```bash
# Via command line
emulator -avd <avd_name> -no-snapshot

# Or from Android Studio
# Run → Run 'app' (select Automotive emulator)
```

## Design Decisions & Traceability

### Requirements Matrix

| Req ID | Requirement | Implementation | Test |
|--------|-------------|----------------|------|
| SUS-001 | Support COMFORT, SPORT, OFF-ROAD, AUTO | `SuspensionMode.kt` sealed class | Unit tests |
| SUS-002 | Single tap for COMFORT/SPORT/AUTO | `SuspensionViewModel.handleModeSelection()` | UI tests |
| SUS-003 | Confirmation for OFF-ROAD >30 km/h | `OffRoadConfirmationDialog` | UI tests |
| SUS-004 | VHAL property 0x11400101 | `CarPropertyService.kt` | Integration tests |
| SUS-005 | Read VEHICLE_SPEED | `observeVehicleSpeed()` | Integration tests |
| SUS-006 | 3s ECU timeout | `SuspensionRepositoryImpl` with `withTimeout()` | Unit tests |
| SUS-007 | Error states | `SuspensionError` sealed class | Unit tests |
| SUS-008 | Revert on error | `SuspensionViewModel.applyMode()` | Unit tests |
| SUS-009 | ASIL-B classification | Design document | Review |

### Architecture

- **Pattern**: MVVM with Clean Architecture
- **DI**: Hilt for dependency injection
- **UI**: Jetpack Compose with Material 3
- **State Management**: StateFlow + SharedFlow for effects
- **VHAL**: CarPropertyManager with property change callbacks

## Troubleshooting

### App doesn't launch on emulator

1. Ensure you're using an Automotive system image (not regular Android)
2. Check that `android.hardware.type.automotive` feature is available
3. Verify the emulator has Play Store/Play Services if needed

### VHAL errors in logcat

- In DEBUG mode, MockCarPropertyService is used automatically
- If see "Car connection failed", this is normal in emulator
- The mock service will handle all VHAL requests

### Build errors

1. Ensure JDK 17 is set: `java -version`
2. In Android Studio: File → Project Structure → JDK Location → 17
3. Clear cache: File → Invalidate Caches → Invalidate and Restart

## License

This is a demonstration project for Android Automotive OS development.