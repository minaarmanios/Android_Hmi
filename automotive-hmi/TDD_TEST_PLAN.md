# TDD Test Plan: Android HMI Suspension Control

**Generated:** 2026-04-03  
**Framework:** JUnit + MockK + Kotlin Coroutines Test  
**Coverage Target:** 80%+

---

## Current Test Status

| Category | Files | Tests | Coverage |
|----------|-------|-------|----------|
| Unit Tests | 1 | 12 | ~70% |
| Integration Tests | 0 | 0 | 0% |
| E2E Tests | 0 | 0 | 0% |

**Gap:** Missing integration tests for VHAL layer and E2E UI tests.

---

## TDD Workflow Plan

### Phase 1: Fix Existing Tests (RED → GREEN)

Current tests in `SuspensionViewModelTest.kt` are well-structured. Verify all pass:

```bash
./gradlew test --tests "com.example.suspensioncontrol.SuspensionViewModelTest"
```

---

### Phase 2: Add Missing Unit Tests

#### 2.1 SuspensionMode Tests

**File:** `app/src/test/java/com/example/suspensioncontrol/domain/model/SuspensionModeTest.kt`

```kotlin
class SuspensionModeTest {
    
    @Test
    fun `fromVhalValue returns correct mode for valid values`() {
        assertEquals(SuspensionMode.Comfort, SuspensionMode.fromVhalValue(0))
        assertEquals(SuspensionMode.Sport, SuspensionMode.fromVhalValue(1))
        assertEquals(SuspensionMode.OffRoad, SuspensionMode.fromVhalValue(2))
        assertEquals(SuspensionMode.Auto, SuspensionMode.fromVhalValue(3))
    }
    
    @Test
    fun `fromVhalValue returns Unknown for invalid values`() {
        assertEquals(SuspensionMode.Unknown, SuspensionMode.fromVhalValue(-1))
        assertEquals(SuspensionMode.Unknown, SuspensionMode.fromVhalValue(99))
    }
    
    @Test
    fun `toVhalValue returns correct integer`() {
        assertEquals(0, SuspensionMode.Comfort.toVhalValue())
        assertEquals(1, SuspensionMode.Sport.toVhalValue())
        assertEquals(2, SuspensionMode.OffRoad.toVhalValue())
        assertEquals(3, SuspensionMode.Auto.toVhalValue())
    }
    
    @Test
    fun `allModes contains exactly four modes`() {
        assertEquals(4, SuspensionMode.allModes.size)
        assertTrue(SuspensionMode.Comfort in SuspensionMode.allModes)
        assertTrue(SuspensionMode.Sport in SuspensionMode.allModes)
        assertTrue(SuspensionMode.OffRoad in SuspensionMode.allModes)
        assertTrue(SuspensionMode.Auto in SuspensionMode.allModes)
    }
}
```

#### 2.2 SuspensionError Tests

**File:** `app/src/test/java/com/example/suspensioncontrol/domain/model/SuspensionErrorTest.kt`

```kotlin
class SuspensionErrorTest {
    
    @Test
    fun `error codes are unique`() {
        val codes = listOf(
            SuspensionError.ModeUnavailable.code,
            SuspensionError.EcuTimeout.code,
            SuspensionError.SpeedRestriction.code,
            SuspensionError.VhalError.code
        )
        assertEquals(codes.size, codes.distinct().size)
    }
    
    @Test
    fun `error messages are user-friendly`() {
        assertFalse(SuspensionError.EcuTimeout.message.contains("Exception"))
        assertFalse(SuspensionError.VhalError.message.contains("Error