# Implementation Plan: Enhanced Suspension Control

**Project:** Android HMI Suspension Control  
**Created:** 2026-04-03  
**Planner:** AI Planner (ECC Skills)

---

## Overview

Add new features to the suspension control app including:
1. Manual mode override (driver preference)
2. Suspension health monitoring
3. Driving mode recommendations

---

## Requirements

### Functional Requirements
- **FR-1:** Allow drivers to manually override AUTO mode
- **FR-2:** Display suspension system health status
- **FR-3:** Show recommended mode based on road conditions
- **FR-4:** Log mode changes for diagnostics

### Non-Functional Requirements
- **NFR-1:** Maintain ASIL-B safety compliance
- **NFR-2:** Response time < 500ms for mode changes
- **NFR-3:** 80%+ test coverage
- **NFR-4:** Support for multi-display (cluster + center)

---

## Architecture Changes

### New Files

| File | Purpose |
|------|---------|
| `domain/model/SuspensionHealth.kt` | Health status domain model |
| `domain/usecase/GetRecommendedModeUseCase.kt` | Recommendation logic |
| `data/SuspensionHealthRepository.kt` | Health monitoring data |
| `ui/components/HealthIndicator.kt` | Health status UI component |
| `ui/components/RecommendationCard.kt` | Mode recommendation UI |

### Modified Files

| File | Changes |
|------|---------|
| `SuspensionContract.kt` | Add health state, recommendation |
| `SuspensionViewModel.kt` | Add health observation, recommendation |
| `SuspensionScreen.kt` | Add health indicator, recommendation card |

---

## Implementation Steps

### Phase 1: Domain Layer (Pure Kotlin)

**Step 1.1:** Create `SuspensionHealth.kt` (NEW)
- Action: Define health status sealed class
- Dependencies: None
- Risk: Low
- Test: Unit test for health states

```kotlin
sealed class SuspensionHealth {
    object Healthy : SuspensionHealth()
    data class Degraded(val issue: String) : SuspensionHealth()
    data class Error(val code: Int) : SuspensionHealth()
    object Unknown : SuspensionHealth()
}
```

**Step 1.2:** Create `GetRecommendedModeUseCase.kt` (NEW)
- Action: Implement recommendation logic
- Dependencies: Step 1.1
- Risk: Medium (algorithm complexity)

```kotlin
class GetRecommendedModeUseCase {
    operator fun invoke(
        speed: Float,
        roadCondition: RoadCondition,
        health: SuspensionHealth
    ): SuspensionMode {
        // Implementation
    }
}
```

---

### Phase 2: Data Layer

**Step 2.1:** Create `SuspensionHealthRepository.kt` (NEW)
- Action: Add health monitoring interface
- Dependencies: Step 1.1
- Risk: Medium (VHAL integration)

```kotlin
interface SuspensionHealthRepository {
    fun observeHealth(): Flow<SuspensionHealth>
    suspend fun getDiagnostics(): Result<DiagnosticsReport>
}
```

**Step 2.2:** Add health property to VHAL
- Action: Extend `CarPropertyService` with health monitoring
- Dependencies: None
- Risk: High (requires VHAL property ID)

---

### Phase 3: Presentation Layer

**Step 3.1:** Update `SuspensionContract.kt`
- Action: Add health and recommendation to State
- Dependencies: Step 1.1, 1.2
- Risk: Low

```kotlin
data class State(
    // ... existing fields
    val health: SuspensionHealth = SuspensionHealth.Unknown,
    val recommendation: SuspensionMode? = null
)
```

**Step 3.2:** Create `HealthIndicator.kt` (NEW)
- Action: Build Compose health status component
- Dependencies: Step 1.1
- Risk: Low

**Step 3.3:** Update `SuspensionScreen.kt`
- Action: Add health indicator to status bar
- Dependencies: Step 3.1, 3.2
- Risk: Low

---

### Phase 4: Testing

**Step 4.1:** Unit tests for new domain classes
- Test `SuspensionHealth`
- Test `GetRecommendedModeUseCase`

**Step 4.2:** Integration tests for health repository
- Test VHAL health property reading

**Step 4.3:** UI tests for health indicator
- Compose UI test for `HealthIndicator`

---

## Testing Strategy

### Unit Tests
- `SuspensionHealthTest.kt` - Health state transitions
- `GetRecommendedModeUseCaseTest.kt` - Recommendation logic
- `SuspensionHealthRepositoryTest.kt` - Repository behavior

### Integration Tests
- VHAL health property integration
- ViewModel with health observation

### E2E Tests (Manual)
- Health indicator displays correctly
- Recommendation updates with speed changes

---

## Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| VHAL property not available | HIGH | Use mock in emulator, feature flag in production |
| Recommendation algorithm inaccurate | MEDIUM | Conservative defaults, user override always available |
| Health monitoring affects performance | MEDIUM | Poll at 1Hz max, debounce updates |

---

## Success Criteria

- [ ] Health indicator shows current suspension status
- [ ] Recommendations update based on speed/conditions
- [ ] All new code has 80%+ test coverage
- [ ] ASIL-B compliance maintained
- [ ] No regression in existing mode selection

---

## Estimated Effort

| Phase | Duration |
|-------|----------|
| Phase 1 (Domain) | 1 day |
| Phase 2 (Data) | 2 days |
| Phase 3 (Presentation) | 1 day |
| Phase 4 (Testing) | 1 day |
| **Total** | **5 days** |

---

## Next Actions

1. **NOW:** Review and approve this plan
2. **TDD START:** Write failing test for `SuspensionHealth`
3. **IMPLEMENT:** Follow TDD Red-Green-Refactor cycle
4. **VERIFY:** Run full test suite after each phase
