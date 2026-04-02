package com.example.suspensioncontrol.domain.model

/**
 * Represents the current suspension mode.
 * Corresponds to VHAL property CUSTOM_SUSPENSION_MODE (0x11400101)
 */
sealed class SuspensionMode(
    val vhalValue: Int,
    val displayName: String
) {
    object Comfort : SuspensionMode(0, "COMFORT")
    object Sport : SuspensionMode(1, "SPORT")
    object OffRoad : SuspensionMode(2, "OFF-ROAD")
    object Auto : SuspensionMode(3, "AUTO")
    object Unknown : SuspensionMode(-1, "UNKNOWN")

    fun toVhalValue(): Int = vhalValue

    companion object {
        fun fromVhalValue(value: Int): SuspensionMode = when (value) {
            0 -> Comfort
            1 -> Sport
            2 -> OffRoad
            3 -> Auto
            else -> Unknown
        }

        val allModes: List<SuspensionMode> = listOf(Comfort, Sport, OffRoad, Auto)
    }
}

/**
 * Error types for suspension control
 */
sealed class SuspensionError(val code: Int, val message: String) {
    object ModeUnavailable : SuspensionError(0x01, "Suspension control not available")
    object EcuTimeout : SuspensionError(0x02, "System timeout. Please try again")
    object SpeedRestriction : SuspensionError(0x03, "Off-road mode restricted above 30 km/h")
    object VhalError : SuspensionError(0x04, "System error. Please restart vehicle")
}

/**
 * VHAL property constants
 */
object SuspensionProperties {
    // Custom vendor property for suspension mode
    const val CUSTOM_SUSPENSION_MODE = 0x11400101
    
    // Standard vehicle speed property
    const val VEHICLE_SPEED = 0x1101 // VehicleProperty.VEHICLE_SPEED
    
    // Timeout configuration (3 seconds)
    const val ECU_TIMEOUT_MS = 3000L
    
    // Speed threshold for Off-Road restriction (30 km/h)
    const val OFF_ROAD_SPEED_THRESHOLD_KMH = 30f
    const val OFF_ROAD_SPEED_THRESHOLD_MS = 30f / 3.6f
}