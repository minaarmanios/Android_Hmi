# Android HMI - Suspension Control - Onboarding Guide

## Overview
An Android Automotive OS app that lets drivers control their vehicle's suspension modes (COMFORT, SPORT, OFF-ROAD, AUTO) directly from the car's infotainment screen. Features ASIL-B safety compliance with speed restrictions and ECU timeout protection.

## Tech Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Language | Kotlin | 1.9.20 |
| UI | Jetpack Compose + Material 3 | BOM 2023.10.01 |
| Architecture | MVVM + Clean Architecture | - |
| DI | Hilt | 2.48.1 |
| Vehicle Integration | Android Car App Library | 1.4.0 |
| Testing | JUnit + MockK | 4.13.2 / 1.13.8 |
| Min SDK | Android 10 (API 29) | - |
| Target SDK | Android 14 (API 34) | - |

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                        │
│  SuspensionScreen.kt (Compose UI)                           │
│  SuspensionViewModel.kt (MVVM)                              │
│  SuspensionContract.kt (Intent/State/Effect)                │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      Domain Layer                            │
│  SuspensionMode.kt (domain models - pure Kotlin)            │
│  SuspensionError.kt (error types)                           │
│  SuspensionProperties.kt (constants)                        │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                       Data Layer                             │
│  SuspensionRepository.kt (repository interface + impl)      │
│  CarPropertyService.kt (VHAL integration)                   │
│    - RealCarPropertyService (production)                    │
│    - MockCarPropertyService (emulator/debug)                │
└─────────────────────────────────────────────────────────────┘
```

## Key Entry Points

| Component | File | Purpose |
|-----------|------|---------|
| **App Entry** | `SuspensionControlApp.kt` | Hilt application class |
| **Main Activity** | `MainActivity.kt` | Single activity, sets content |
| **Main Screen** | `SuspensionScreen.kt` | Compose UI for mode selection |
| **DI Module** | `AppModule.kt` | Hilt dependency injection |
| **VHAL Service** | `CarPropertyService.kt` | Vehicle hardware abstraction |

## Directory Structure

```
app/src/main/java/com/example/suspensioncontrol/
├── MainActivity.kt                 # Single activity entry
├── SuspensionControlApp.kt         # Application class with Hilt
├── ui/
│   ├── SuspensionScreen.kt         # Compose UI
│   ├── SuspensionViewModel.kt      # ViewModel (MVVM)
│   └── theme/
│       └── Theme.kt                # Material 3 dark theme
├── domain/
│   ├── model/
│   │   ├── SuspensionMode.kt       # Domain models
│   │   └── SuspensionUiState.kt    # UI state model
│   └── contracts/
│       └── SuspensionContract.kt   # MVI contract
├── data/
│   ├── SuspensionRepository.kt     # Repository pattern
│   └── CarPropertyService.kt       # VHAL integration
└── di/
    └── AppModule.kt                # Hilt DI module
```

## Request Lifecycle

1. **User taps mode button** → `SuspensionScreen` sends `Intent.SelectMode`
2. **ViewModel processes** → Checks speed restriction, shows confirmation if needed
3. **Repository call** → `setSuspensionMode()` with 3-second timeout
4. **VHAL write** → `CarPropertyManager.setProperty()` to vehicle ECU
5. **State update** → Flow emits new mode, UI recomposes

## Safety Features

| Feature | Implementation |
|---------|---------------|
| **Speed Restriction** | Off-Road blocked above 30 km/h (ASIL-B) |
| **Confirmation Dialog** | Required for Off-Road at speed |
| **ECU Timeout** | 3-second timeout with fallback |
| **Error Recovery** | Reverts to previous mode on failure |
| **Mock Mode** | Safe testing in emulator |

## Common Tasks

| Task | Command |
|------|---------|
| Build debug APK | `./gradlew assembleDebug` |
| Run unit tests | `./gradlew test` |
| Run on emulator | `./gradlew installDebug` |
| Lint check | `./gradlew lint` |
| Clean build | `./gradlew clean` |

## Where to Look

| I want to... | Look at... |
|--------------