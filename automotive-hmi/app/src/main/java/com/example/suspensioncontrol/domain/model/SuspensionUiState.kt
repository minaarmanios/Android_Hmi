package com.example.suspensioncontrol.domain.model

/**
 * Represents the UI state for the suspension control screen
 */
data class SuspensionUiState(
    val currentMode: SuspensionMode = SuspensionMode.Comfort,
    val vehicleSpeed: Float = 0f,
    val isLoading: Boolean = false,
    val error: SuspensionError? = null,
    val showOffRoadConfirmation: Boolean = false,
    val isOffRoadRestricted: Boolean = false,
    val pendingMode: SuspensionMode? = null
) {
    val speedKmh: Float
        get() = vehicleSpeed * 3.6f // Convert m/s to km/h
    
    val displaySpeed: String
        get() = if (vehicleSpeed > 0) "${speedKmh.toInt()} km/h" else "--- km/h"
}