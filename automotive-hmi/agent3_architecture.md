# Agent 3 — Software Architect
# Software Architecture: Suspension Mode Control HMI

## 1. Project Structure

```
SuspensionControlApp/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/suspensioncontrol/
│   │   │   ├── di/
│   │   │   │   └── AppModule.kt
│   │   │   ├── data/
│   │   │   │   ├── CarPropertyService.kt
│   │   │   │   └── SuspensionRepository.kt
│   │   │   ├── domain/
│   │   │   │   ├── model/
│   │   │   │   │   ├── SuspensionMode.kt
│   │   │   │   │   └── SuspensionUiState.kt
│   │   │   │   └── contracts/
│   │   │   │       └── SuspensionContract.kt
│   │   │   ├── ui/
│   │   │   │   ├── SuspensionScreen.kt
│   │   │   │   └── SuspensionViewModel.kt
│   │   │   └── MainActivity.kt
│   │   ├── res/
│   │   │   ├── values/
│   │   │   │   ├── colors.xml
│   │   │   │   ├── strings.xml
│   │   │   │   └── themes.xml
│   │   │   ├── drawable/
│   │   │   │   ├── ic_comfort.xml
│   │   │   │   ├── ic_sport.xml
│   │   │   │   ├── ic_off_road.xml
│   │   │   │   └── ic_auto.xml
│   │   │   └── xml/
│   │   │       └── automotive_app_desc.xml
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts
└── settings.gradle.kts
```

## 2. Package Structure

| Package | Responsibility |
|---------|----------------|
| `di` | Hilt dependency injection modules |
| `data` | VHAL integration, repository implementation |
| `domain.model` | Domain models (SuspensionMode, UiState) |
| `domain.contracts` | ViewModel contracts ( intents, states) |
| `ui` | Compose UI and ViewModel |

## 3. MVVM Architecture

### 3.1 Data Flow
```
User Action → ViewModel → Repository → CarPropertyService → VHAL
                ↓
           UiState Update → Compose UI
```

### 3.2 Key Components
- **SuspensionViewModel**: Handles UI logic, state management, user interactions
- **SuspensionRepository**: Abstracts VHAL data source, provides clean API
- **CarPropertyService**: Low-level VHAL integration using CarPropertyManager
- **SuspensionMode**: Sealed class defining all mode states

## 4. State Machine (Kotlin Sealed Class)

```kotlin
sealed class SuspensionMode {
    object Comfort : SuspensionMode()
    object Sport : SuspensionMode()
    object OffRoad : SuspensionMode()
    object Auto : SuspensionMode()
    object Unknown : SuspensionMode()
    
    // VHAL value mapping
    fun toVhalValue(): Int = when (this) {
        is Comfort -> 0
        is Sport -> 1
        is OffRoad -> 2
        is Auto -> 3
        is Unknown -> -1
    }
    
    companion object {
        fun fromVhalValue(value: Int): SuspensionMode = when (value) {
            0 -> Comfort
            1 -> Sport
            2 -> OffRoad
            3 -> Auto
            else -> Unknown
        }
    }
}
```

### 4.1 State Transitions
```kotlin
sealed class SuspensionUiState {
    data class Idle(
        val currentMode: SuspensionMode,
        val vehicleSpeed: Float,
        val isOffRoadRestricted: Boolean
    ) : SuspensionUiState()
    
    data class Loading(
        val targetMode: SuspensionMode
    ) : SuspensionUiState()
    
    data class Error(
        val errorCode: SuspensionError,
        val message: String,
        val previousMode: SuspensionMode
    ) : SuspensionUiState()
    
    data class ConfirmationRequired(
        val targetMode: SuspensionMode,
        val currentSpeed: Float
    ) : SuspensionUiState()
}
```

### 4.2 Error States
```kotlin
sealed class SuspensionError(val code: Int, val message: String) {
    object ModeUnavailable : SuspensionError(0x01, "Suspension control not available")
    object EcuTimeout : SuspensionError(0x02, "System timeout. Please try again")
    object SpeedRestriction : SuspensionError(0x03, "Off-road mode restricted above 30 km/h")
    object VhalError : SuspensionError(0x04, "System error. Please restart vehicle")
}
```

## 5. VHAL Integration

### 5.1 Property Definitions
```kotlin
object SuspensionProperties {
    // Custom vendor property: 0x11400101
    const val CUSTOM_SUSPENSION_MODE = 0x11400101
    
    // Standard property
    const val VEHICLE_SPEED = VehicleProperty.VEHICLE_SPEED
    
    // Timeout configuration
    const val ECU_TIMEOUT_MS = 3000L
    
    // Speed threshold for Off-Road restriction
    const val OFF_ROAD_SPEED_THRESHOLD_KMH = 30f
    const val OFF_ROAD_SPEED_THRESHOLD_MS = 30f / 3.6f
}
```

### 5.2 CarPropertyService Interface
```kotlin
interface CarPropertyService {
    fun getSuspensionMode(): Flow<SuspensionMode>
    fun getVehicleSpeed(): Flow<Float>
    suspend fun setSuspensionMode(mode: SuspensionMode): Result<Unit>
    fun close()
}
```

### 5.3 Property Change Listeners
- Subscribe to `CUSTOM_SUSPENSION_MODE` for mode changes
- Subscribe to `VEHICLE_SPEED` for speed updates
- Use `CarPropertyManager.addOnPropertyChangedCallback()`

## 6. Error Handling Strategy

### 6.1 Timeout Management
```kotlin
suspend fun <T> withTimeout(
    timeoutMs: Long = SuspensionProperties.ECU_TIMEOUT_MS,
    block: suspend () -> T
): Result<T> = runCatching {
    withTimeoutOrNull(timeoutMs) { block() } 
        ?: throw EcuTimeoutException()
}
```

### 6.2 Error Recovery
1. On timeout: Revert to previous valid mode, show error
2. On VHAL error: Retry once, then show error if failed
3. On invalid mode: Default to COMFORT, log error

## 7. Data Classes & Interfaces

### 7.1 SuspensionContract
```kotlin
object SuspensionContract {
    sealed class Intent {
        data class SelectMode(val mode: SuspensionMode) : Intent()
        object ConfirmOffRoad : Intent()
        object CancelConfirmation : Intent()
        object DismissError : Intent()
        object Retry : Intent()
    }
    
    data class State(
        val currentMode: SuspensionMode = SuspensionMode.Comfort,
        val vehicleSpeed: Float = 0f,
        val isLoading: Boolean = false,
        val error: SuspensionError? = null,
        val showOffRoadConfirmation: Boolean = false,
        val isOffRoadRestricted: Boolean = false
    )
    
    sealed class Effect {
        data class ShowToast(val message: String) : Effect()
        object NavigateBack : Effect()
    }
}
```

### 7.2 Repository Interface
```kotlin
interface SuspensionRepository {
    fun observeSuspensionMode(): Flow<SuspensionMode>
    fun observeVehicleSpeed(): Flow<Float>
    suspend fun setSuspensionMode(mode: SuspensionMode): Result<Unit>
    suspend fun getCurrentMode(): SuspensionMode
    suspend fun getCurrentSpeed(): Float
}
```

## 8. Gradle Dependencies

### 8.1 Project-Level build.gradle.kts
```kotlin
plugins {
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.20" apply false
    id("com.google.dagger.hilt.android") version "2.48.1" apply false
}
```

### 8.2 App-Level build.gradle.kts
```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    kotlin("kapt")
}

android {
    namespace = "com.example.suspensioncontrol"
    compileSdk = 34
    
    defaultConfig {
        applicationId = "com.example.suspensioncontrol"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
    
    buildFeatures {
        compose = true
        buildConfig = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.5"
    }
}

dependencies {
    // AndroidX Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.1")
    
    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    
    // Lifecycle & ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.2")
    
    // Car App Library (Android Automotive)
    implementation("androidx.car.app:app:1.4.0")
    implementation("androidx.car.app:app-leanback:1.4.0")
    
    // Hilt Dependency Injection
    implementation("com.google.dagger:hilt-android:2.48.1")
    kapt("com.google.dagger:hilt-android-compiler:2.48.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

kapt {
    correctErrorTypes = true
}
```

## 9. Gradle Settings

### settings.gradle.kts
```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "SuspensionControlApp"
include(":app")
```

---
End of Agent 3 Output