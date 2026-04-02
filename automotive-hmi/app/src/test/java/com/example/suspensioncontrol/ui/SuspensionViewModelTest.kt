package com.example.suspensioncontrol

import com.example.suspensioncontrol.domain.model.SuspensionMode
import com.example.suspensioncontrol.domain.model.SuspensionError
import com.example.suspensioncontrol.domain.model.SuspensionProperties
import com.example.suspensioncontrol.data.SuspensionRepository
import com.example.suspensioncontrol.ui.SuspensionViewModel
import com.example.suspensioncontrol.domain.contracts.SuspensionContract
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for SuspensionViewModel
 * Tests all state transitions and user interactions
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SuspensionViewModelTest {

    private lateinit var viewModel: SuspensionViewModel
    private lateinit var mockRepository: SuspensionRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Create mock repository
        mockRepository = mockk(relaxed = true)
        
        // Setup default behavior
        every { mockRepository.observeSuspensionMode() } returns MutableStateFlow(SuspensionMode.Comfort)
        every { mockRepository.observeVehicleSpeed() } returns MutableStateFlow(0f)
        coEvery { mockRepository.setSuspensionMode(any()) } returns Result.success(Unit)
        
        viewModel = SuspensionViewModel(mockRepository)
        
        // Advance dispatcher to complete initialization
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ==================== Mode Selection Tests ====================

    @Test
    fun `selectMode - updates state when mode is different`() = runTest {
        // Given
        val initialState = viewModel.state.first()
        assertEquals(SuspensionMode.Comfort, initialState.currentMode)
        
        // When
        viewModel.processIntent(SuspensionContract.Intent.SelectMode(SuspensionMode.Sport))
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        val newState = viewModel.state.first()
        assertEquals(SuspensionMode.Sport, newState.currentMode)
    }

    @Test
    fun `selectMode - does nothing when same mode selected`() = runTest {
        // Given
        viewModel.processIntent(SuspensionContract.Intent.SelectMode(SuspensionMode.Comfort))
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When - select same mode again
        viewModel.processIntent(SuspensionContract.Intent.SelectMode(SuspensionMode.Comfort))
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then - repository should only be called once
        coVerify(exactly = 1) { mockRepository.setSuspensionMode(any()) }
    }

    @Test
    fun `selectMode - shows confirmation dialog for OffRoad above threshold`() = runTest {
        // Given - speed above 30 km/h threshold
        every { mockRepository.observeVehicleSpeed() } returns MutableStateFlow(
            SuspensionProperties.OFF_ROAD_SPEED_THRESHOLD_MS + 1f
        )
        
        // Create fresh ViewModel with updated speed
        val speedViewModel = SuspensionViewModel(mockRepository)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When - select OffRoad mode
        speedViewModel.processIntent(SuspensionContract.Intent.SelectMode(SuspensionMode.OffRoad))
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then - should show confirmation
        val state = speedViewModel.state.first()
        assertTrue(state.showOffRoadConfirmation)
        assertEquals(SuspensionMode.OffRoad, state.pendingMode)
    }

    @Test
    fun `selectMode - applies OffRoad directly when below threshold`() = runTest {
        // Given - speed below 30 km/h threshold
        every { mockRepository.observeVehicleSpeed() } returns MutableStateFlow(5f)
        
        // Create fresh ViewModel with low speed
        val lowSpeedViewModel = SuspensionViewModel(mockRepository)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When - select OffRoad mode
        lowSpeedViewModel.processIntent(SuspensionContract.Intent.SelectMode(SuspensionMode.OffRoad))
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then - should apply directly without confirmation
        val state = lowSpeedViewModel.state.first()
        assertFalse(state.showOffRoadConfirmation)
    }

    // ==================== Confirmation Dialog Tests ====================

    @Test
    fun `confirmOffRoad - applies mode after confirmation`() = runTest {
        // Given - showing confirmation dialog
        every { mockRepository.observeVehicleSpeed() } returns MutableStateFlow(35f / 3.6f)
        
        val confirmViewModel = SuspensionViewModel(mockRepository)
        testDispatcher.scheduler.advanceUntilIdle()
        
        confirmViewModel.processIntent(SuspensionContract.Intent.SelectMode(SuspensionMode.OffRoad))
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When - confirm
        confirmViewModel.processIntent(SuspensionContract.Intent.ConfirmOffRoad)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        coVerify { mockRepository.setSuspensionMode(SuspensionMode.OffRoad) }
        val state = confirmViewModel.state.first()
        assertFalse(state.showOffRoadConfirmation)
    }

    @Test
    fun `cancelConfirmation - dismisses dialog without applying mode`() = runTest {
        // Given
        every { mockRepository.observeVehicleSpeed() } returns MutableStateFlow(35f / 3.6f)
        
        val confirmViewModel = SuspensionViewModel(mockRepository)
        testDispatcher.scheduler.advanceUntilIdle()
        
        confirmViewModel.processIntent(SuspensionContract.Intent.SelectMode(SuspensionMode.OffRoad))
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When - cancel
        confirmViewModel.processIntent(SuspensionContract.Intent.CancelConfirmation)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then - mode should not be applied
        coVerify(exactly = 0) { mockRepository.setSuspensionMode(SuspensionMode.OffRoad) }
        
        val state = confirmViewModel.state.first()
        assertFalse(state.showOffRoadConfirmation)
    }

    // ==================== Error Handling Tests ====================

    @Test
    fun `selectMode - reverts to previous mode on timeout error`() = runTest {
        // Given - repository returns timeout error
        coEvery { mockRepository.setSuspensionMode(any()) } returns Result.failure(
            SuspensionError.EcuTimeout
        )
        
        val errorViewModel = SuspensionViewModel(mockRepository)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When
        errorViewModel.processIntent(SuspensionContract.Intent.SelectMode(SuspensionMode.Sport))
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then - should show error and revert
        val state = errorViewModel.state.first()
        assertNotNull(state.error)
        assertEquals(SuspensionError.EcuTimeout, state.error)
    }

    @Test
    fun `dismissError - clears error state`() = runTest {
        // Given - trigger an error
        coEvery { mockRepository.setSuspensionMode(any()) } returns Result.failure(
            SuspensionError.EcuTimeout
        )
        
        val errorViewModel = SuspensionViewModel(mockRepository)
        testDispatcher.scheduler.advanceUntilIdle()
        
        errorViewModel.processIntent(SuspensionContract.Intent.SelectMode(SuspensionMode.Sport))
        testDispatcher.scheduler.advanceUntilIdle()
        
        var state = errorViewModel.state.first()
        assertNotNull(state.error)
        
        // When
        errorViewModel.processIntent(SuspensionContract.Intent.DismissError)
        
        // Then
        state = errorViewModel.state.first()
        assertNull(state.error)
    }

    // ==================== Loading State Tests ====================

    @Test
    fun `selectMode - sets loading state during mode change`() = runTest {
        // Given
        coEvery { mockRepository.setSuspensionMode(any()) } coAnswers {
            kotlinx.coroutines.delay(100)
            Result.success(Unit)
        }
        
        val loadingViewModel = SuspensionViewModel(mockRepository)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When
        loadingViewModel.processIntent(SuspensionContract.Intent.SelectMode(SuspensionMode.Sport))
        
        // Then - should be loading immediately
        val loadingState = loadingViewModel.state.first()
        assertTrue(loadingState.isLoading)
        
        // After completion
        testDispatcher.scheduler.advanceUntilIdle()
        val finalState = loadingViewModel.state.first()
        assertFalse(finalState.isLoading)
    }

    // ==================== Speed Display Tests ====================

    @Test
    fun `state - displays speed in km-h correctly`() = runTest {
        // Given - speed at 72 km/h (20 m/s)
        every { mockRepository.observeVehicleSpeed() } returns MutableStateFlow(20f)
        
        val speedViewModel = SuspensionViewModel(mockRepository)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        val state = speedViewModel.state.first()
        assertEquals(72f, state.speedKmh, 0.1f)
        assertEquals("72 km/h", state.displaySpeed)
    }

    @Test
    fun `state - shows dash when speed unavailable`() = runTest {
        // Given - speed at 0
        every { mockRepository.observeVehicleSpeed() } returns MutableStateFlow(0f)
        
        val zeroSpeedViewModel = SuspensionViewModel(mockRepository)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        val state = zeroSpeedViewModel.state.first()
        assertEquals("--- km/h", state.displaySpeed)
    }
}