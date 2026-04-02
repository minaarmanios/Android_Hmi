# Agent 1 — Systems Engineer
# Functional Requirements Document: Suspension Mode Control HMI

## 1. Overview
This document defines the formal functional requirements for a vehicle suspension mode control Human-Machine Interface (HMI) feature for Android Automotive OS.

## 2. Supported Suspension Modes

| Mode | Description | Icon Reference |
|------|-------------|----------------|
| **COMFORT** | Optimized for city driving and passenger comfort | Soft suspension icon |
| **SPORT** | Firm suspension for Dynamic handling | Sport/dynamic icon |
| **OFF-ROAD** | Maximum wheel articulation for rough terrain | Terrain/off-road icon |
| **AUTO** | Adaptive suspension based on driving conditions | Auto/adaptive icon |

## 3. Driver Interaction Model

### 3.1 Mode Selection
- **Single tap**: Immediately selects the mode (COMFORT, SPORT, AUTO)
- **Confirmation dialog**: Required for OFF-ROAD mode when vehicle speed > 30 km/h

### 3.2 Interaction Flow
```
User taps mode button → 
  IF mode == OFF-AND && speed > 30 km/h →
    Show confirmation dialog →
      IF confirmed → Apply mode
      ELSE → Cancel
  ELSE →
    Apply mode immediately
```

## 4. State Transitions

### 4.1 Valid State Transitions
| From | To | Condition |
|------|-----|-----------|
| ANY | COMFORT | Always allowed |
| ANY | SPORT | Always allowed |
| ANY | AUTO | Always allowed |
| ANY | OFF-ROAD | Speed ≤ 30 km/h OR user confirms |
| OFF-ROAD | COMFORT/SPORT/AUTO | Always allowed (auto-cancel speed restriction) |

### 4.2 Invalid State Handling
- If VHAL returns invalid mode → Display error state, revert to previous valid mode
- If vehicle speed changes to > 30 km/h while in OFF-ROAD → Show warning, allow continued operation

## 5. VHAL Property Mapping

### 5.1 Standard Properties
| Property | ID | Type | Access | Description |
|----------|-----|------|--------|-------------|
| VEHICLE_SPEED | 0x1101 | float | R | Current vehicle speed in m/s |

### 5.2 Custom Vendor Property
| Property | ID | Type | Access | Description |
|----------|-----|------|--------|-------------|
| CUSTOM_SUSPENSION_MODE | 0x11400101 | int32 | R/W | Current suspension mode |

### 5.3 Mode Value Mapping
```kotlin
enum class SuspensionMode(val vhalValue: Int) {
    COMFORT = 0,
    SPORT = 1,
    OFF_ROAD = 2,
    AUTO = 3,
    UNKNOWN = -1
}
```

## 6. Safety Constraints & ASIL-B Classification

### 6.1 ASIL-B Rationale
- Suspension mode affects vehicle dynamics
- Incorrect mode selection could lead to loss of vehicle control
- Confirmation required for OFF-ROAD at speed addresses systematic faults
- Timeout handling addresses random hardware faults

### 6.2 Safety Constraints
1. **Speed Restriction**: OFF-ROAD mode requires confirmation above 30 km/h
2. **Timeout Protection**: ECU communication timeout set to 3 seconds
3. **Invalid State Fallback**: System reverts to previous valid mode on error
4. **Mode Persistence**: Selected mode persists across ignition cycles

## 7. Error States

### 7.1 Error Types
| Error | Code | Description | User Message |
|-------|------|-------------|--------------|
| MODE_UNAVAILABLE | 0x01 | Feature not available on this vehicle | "Suspension control not available" |
| ECU_TIMEOUT | 0x02 | No response from suspension ECU (3s) | "System timeout. Please try again" |
| SPEED_RESTRICTION | 0x03 | Off-road blocked due to speed | "Off-road mode restricted above 30 km/h" |
| VHAL_ERROR | 0x04 | VHAL property read/write error | "System error. Please restart vehicle" |

### 7.2 Error Handling Flow
```
Apply mode → 
  Wait max 3s for ECU response →
    IF timeout → Show MODE_UNAVAILABLE error, revert to previous mode
    IF VHAL error → Show VHAL_ERROR, retry once
    IF success → Update UI, persist mode
```

## 8. Requirements Traceability

| Req ID | Requirement | Priority | Test Reference |
|--------|-------------|----------|----------------|
| SUS-001 | Support COMFORT, SPORT, OFF-ROAD, AUTO modes | Must | Unit tests |
| SUS-002 | Single tap selection for COMFORT/SPORT/AUTO | Must | UI tests |
| SUS-003 | Confirmation dialog for OFF-ROAD at >30 km/h | Must | UI tests |
| SUS-004 | VHAL integration via CUSTOM_SUSPENSION_MODE (0x11400101) | Must | Integration tests |
| SUS-005 | Read VEHICLE_SPEED for restriction logic | Must | Integration tests |
| SUS-006 | 3-second ECU timeout with error display | Must | Unit tests |
| SUS-007 | Error states: MODE_UNAVAILABLE, ECU_TIMEOUT, SPEED_RESTRICTION | Must | Unit tests |
| SUS-008 | Revert to previous valid mode on error | Must | Unit tests |
| SUS-009 | ASIL-B classification for safety-critical feature | Must | Design review |

---
End of Agent 1 Output