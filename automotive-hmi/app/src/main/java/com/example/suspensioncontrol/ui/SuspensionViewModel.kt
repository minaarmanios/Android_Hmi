package com.example.suspensioncontrol.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.suspensioncontrol.data.SuspensionRepository
import com.example.suspensioncontrol.domain.model.SuspensionError
import com.example.suspensioncontrol.domain.model.SuspensionMode
import com.example.suspensioncontrol.domain.model.SuspensionProperties
import com.example.suspensioncontrol.domain.contracts.SuspensionContract
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Suspension Control screen.
 * Handles user interactions and manages UI state.
 */
@HiltViewModel
class SuspensionViewModel @Inject constructor(
    private val repository: SuspensionRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SuspensionContract.State())
    val state: StateFlow<SuspensionContract.State> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<SuspensionContract.Effect>()
    val effects = _effects.asSharedFlow()

    private var previousMode: SuspensionMode = SuspensionMode.Comfort

    init {
        observeVehicleState()
    }

    /**
     * Observe changes from VHAL
     */
    private fun observeVehicleState() {
        viewModelScope.launch {
            repository.observeSuspensionMode()
                .collect { mode ->
                    previousMode = mode
                    _state.update { it.copy(currentMode = mode) }
                }
        }

        viewModelScope.launch {
            repository.observeVehicleSpeed()
                .collect { speed ->
                    _state.update { 
                        it.copy(
                            vehicleSpeed = speed,
                            isOffRoadRestricted = speed > SuspensionProperties.OFF_ROAD_SPEED_THRESHOLD_MS
                        )
                    }
                }
        }
    }

    /**
     * Process user intents
     */
    fun processIntent(intent: SuspensionContract.Intent) {
        when (intent) {
            is SuspensionContract.Intent.SelectMode -> handleModeSelection(intent.mode)
            is SuspensionContract.Intent.ConfirmOffRoad -> confirmOffRoadMode()
            is SuspensionContract.Intent.CancelConfirmation -> cancelConfirmation()
            is SuspensionContract.Intent.DismissError -> dismissError()
            is SuspensionContract.Intent.Retry -> retryLastAction()
        }
    }

    private fun handleModeSelection(mode: SuspensionMode) {
        // If already in this mode, do nothing
        if (mode == _state.value.currentMode) return

        // Check if Off-Road confirmation is needed
        if (mode == SuspensionMode.OffRoad && _state.value.isOffRoadRestricted) {
            _state.update { 
                it.copy(
                    showOffRoadConfirmation = true,
                    pendingMode = mode
                )
            }
            return
        }

        // Apply mode directly
        applyMode(mode)
    }

    private fun confirmOffRoadMode() {
        _state.value.pendingMode?.let { mode ->
            _state.update { it.copy(showOffRoadConfirmation = false, pendingMode = null) }
            applyMode(mode)
        }
    }

    private fun cancelConfirmation() {
        _state.update { it.copy(showOffRoadConfirmation = false, pendingMode = null) }
    }

    private fun applyMode(mode: SuspensionMode) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            val result = repository.setSuspensionMode(mode)
            
            result.fold(
                onSuccess = {
                    _state.update { 
                        it.copy(
                            currentMode = mode,
                            isLoading = false
                        )
                    }
                },
                onFailure = { error ->
                    val suspensionError = when {
                        error is SuspensionError -> error
                        error.message?.contains("timeout", ignoreCase = true) == true -> 
                            SuspensionError.EcuTimeout
                        else -> SuspensionError.VhalError
                    }
                    
                    _state.update { 
                        it.copy(
                            currentMode = previousMode, // Revert to previous
                            isLoading = false,
                            error = suspensionError
                        )
                    }
                }
            )
        }
    }

    private fun dismissError() {
        _state.update { it.copy(error = null) }
    }

    private fun retryLastAction() {
        _state.value.error?.let {
            dismissError()
            // Retry setting the last attempted mode
            _state.value.pendingMode?.let { mode -> applyMode(mode) }
                ?: applyMode(_state.value.currentMode)
        }
    }
}