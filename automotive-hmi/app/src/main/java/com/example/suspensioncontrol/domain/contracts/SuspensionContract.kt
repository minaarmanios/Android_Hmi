package com.example.suspensioncontrol.domain.contracts

import com.example.suspensioncontrol.domain.model.SuspensionError
import com.example.suspensioncontrol.domain.model.SuspensionMode

/**
 * Contract defining the interface between ViewModel and UI
 */
object SuspensionContract {

    /**
     * User intents/actions
     */
    sealed class Intent {
        data class SelectMode(val mode: SuspensionMode) : Intent()
        object ConfirmOffRoad : Intent()
        object CancelConfirmation : Intent()
        object DismissError : Intent()
        object Retry : Intent()
    }

    /**
     * UI State
     */
    data class State(
        val currentMode: SuspensionMode = SuspensionMode.Comfort,
        val vehicleSpeed: Float = 0f,
        val isLoading: Boolean = false,
        val error: SuspensionError? = null,
        val showOffRoadConfirmation: Boolean = false,
        val isOffRoadRestricted: Boolean = false,
        val pendingMode: SuspensionMode? = null
    ) {
        val speedKmh: Float
            get() = vehicleSpeed * 3.6f
        
        val displaySpeed: String
            get() = if (vehicleSpeed > 0) "${speedKmh.toInt()} km/h" else "--- km/h"
    }

    /**
     * Side effects
     */
    sealed class Effect {
        data class ShowToast(val message: String) : Effect()
        data class ShowConfirmation(val mode: SuspensionMode, val speed: Float) : Effect()
    }
}