# Code Review: Android HMI Suspension Control

**Generated:** 2026-04-03  
**Reviewer:** AI Code Reviewer (ECC Skills)  
**Files Reviewed:** 12 Kotlin files

---

## Review Summary

| Severity | Count | Status |
|----------|-------|--------|
| CRITICAL | 0 | ✅ pass |
| HIGH | 3 | ⚠️ warn |
| MEDIUM | 4 | ℹ️ info |
| LOW | 2 | 📝 note |

**Verdict:** WARNING — 3 HIGH issues should be resolved before production

---

## HIGH Severity Issues

### 1. Missing Error Handling for Coroutines

**File:** `SuspensionRepository.kt:35-45`

**Issue:** `scope.launch` in init block uses `SupervisorJob` but errors are only logged, not surfaced to UI.

```kotlin
// CURRENT: Errors swallowed
scope.launch {
    carPropertyService.observeSuspensionMode()
        .catch { e -> Log.e(TAG, "Error observing mode", e) }
        .collect { mode -> cachedMode = mode }
}
```

**Fix:** Expose error state to ViewModel:
```kotlin
// BETTER: Surface errors to UI
private val _errors = MutableSharedFlow<SuspensionError>()
val errors: SharedFlow<SuspensionError> = _errors

scope.launch {
    carPropertyService.observeSuspensionMode()
        .catch { e -> 
            Log.e(TAG, "Error observing mode", e)
            _errors.tryEmit(SuspensionError.VhalError)
        }
        .collect { mode -> cachedMode = mode }
}
```

---

### 2. Hardcoded Timeout in Mock Service

**File:** `CarPropertyService.kt:186`

**Issue:** Mock simulates 1.5s delay hardcoded, should use `SuspensionProperties.ECU_TIMEOUT_MS` for consistency.

```kotlin
// CURRENT: Hardcoded delay
kotlinx.coroutines.delay(1500)

// FIX: Use constant
kotlinx.coroutines.delay(SuspensionProperties.ECU_TIMEOUT_MS / 2)
```

---

### 3. No Input Validation on Mode Selection

**File:** `SuspensionViewModel.kt:58`

**Issue:** `handleModeSelection` doesn't validate that mode is a valid `SuspensionMode` from `allModes`.

```kotlin
// CURRENT: No validation
private fun handleModeSelection(mode: SuspensionMode) {
    if (mode == _state.value.currentMode) return
    // ...
}

// FIX: Add validation
private fun handleModeSelection(mode: SuspensionMode) {
    if (mode == _state.value.currentMode) return
    require(mode in SuspensionMode.allModes) { "Invalid mode: $mode" }
    // ...
}
```

---

## MEDIUM Severity Issues

### 4. Large Composable Function

**File:** `SuspensionScreen.kt:35-150`

**Issue:** `SuspensionScreen` is 115 lines. Consider