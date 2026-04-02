package com.example.suspensioncontrol.data

import android.car.Car
import android.car.VehiclePropertyIds
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.example.suspensioncontrol.domain.model.SuspensionMode
import com.example.suspensioncontrol.domain.model.SuspensionProperties
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for interacting with VHAL properties related to suspension control.
 * Uses CarPropertyManager to read/write suspension mode and vehicle speed.
 * 
 * When running in DEBUG mode (emulator), uses MockCarPropertyManager for simulation.
 */
interface CarPropertyService {
    fun observeSuspensionMode(): Flow<SuspensionMode>
    fun observeVehicleSpeed(): Flow<Float>
    suspend fun setSuspensionMode(mode: SuspensionMode): Result<Unit>
    fun close()
}

/**
 * Production implementation using real CarPropertyManager
 */
@Singleton
class RealCarPropertyService @Inject constructor(
    @ApplicationContext private val context: Context
) : CarPropertyService {

    private val car: Car? = try {
        Car.createCar(context)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to create Car connection", e)
        null
    }

    private val carPropertyManager: CarPropertyManager? = car?.getCarPropertyManager(
        CarPropertyManager.VEHICLE_PROPERTY_CHANGE_Callback
    )

    override fun observeSuspensionMode(): Flow<SuspensionMode> = callbackFlow {
        val callback = object : CarPropertyManager.CarPropertyChangeCallback {
            override fun onChangeEvent(property: android.car.hardware.property.CarPropertyValue<*>) {
                val value = property.value as? Int ?: return
                trySend(SuspensionMode.fromVhalValue(value))
            }

            override fun onChangeError(
                propertyId: Int,
                zoneId: Int,
                errorCode: Int,
                errorMessage: String
            ) {
                Log.e(TAG, "Property change error: $errorCode - $errorMessage")
            }
        }

        try {
            carPropertyManager?.registerCallback(
                callback,
                SuspensionProperties.CUSTOM_SUSPENSION_MODE,
                CarPropertyManager.SAMPLING_RATE_ON_CHANGE
            )

            // Emit current value
            val currentValue = carPropertyManager?.getProperty(
                Int::class.java,
                SuspensionProperties.CUSTOM_SUSPENSION_MODE,
                0
            )?.value

            currentValue?.let {
                trySend(SuspensionMode.fromVhalValue(it))
            } ?: trySend(SuspensionMode.Unknown)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register for suspension mode", e)
            trySend(SuspensionMode.Unknown)
        }

        awaitClose {
            try {
                carPropertyManager?.unregisterCallback(callback)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unregister callback", e)
            }
        }
    }.flowOn(Dispatchers.IO)

    override fun observeVehicleSpeed(): Flow<Float> = callbackFlow {
        val callback = object : CarPropertyManager.CarPropertyChangeCallback {
            override fun onChangeEvent(property: android.car.hardware.property.CarPropertyValue<*>) {
                val value = property.value as? Float ?: return
                trySend(value)
            }

            override fun onChangeError(
                propertyId: Int,
                zoneId: Int,
                errorCode: Int,
                errorMessage: String
            ) {
                Log.e(TAG, "Property change error: $errorCode - $errorMessage")
            }
        }

        try {
            carPropertyManager?.registerCallback(
                callback,
                SuspensionProperties.VEHICLE_SPEED,
                CarPropertyManager.SAMPLING_RATE_ON_CHANGE
            )

            // Emit current value
            val currentValue = carPropertyManager?.getProperty(
                Float::class.java,
                SuspensionProperties.VEHICLE_SPEED,
                0
            )?.value

            trySend(currentValue ?: 0f)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register for vehicle speed", e)
            trySend(0f)
        }

        awaitClose {
            try {
                carPropertyManager?.unregisterCallback(callback)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unregister callback", e)
            }
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun setSuspensionMode(mode: SuspensionMode): Result<Unit> = 
        withContext(Dispatchers.IO) {
            try {
                val propertyId = SuspensionProperties.CUSTOM_SUSPENSION_MODE
                
                // Use car API to set property
                carPropertyManager?.setProperty(
                    Int::class.java,
                    propertyId,
                    0, // zoneId
                    mode.toVhalValue()
                )
                
                Log.d(TAG, "Set suspension mode to: ${mode.displayName}")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set suspension mode", e)
                Result.failure(e)
            }
        }

    override fun close() {
        try {
            car?.disconnect()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to close Car connection", e)
        }
    }

    companion object {
        private const val TAG = "RealCarPropertyService"
    }
}

/**
 * Mock implementation for emulator testing
 * Simulates VHAL responses when running in DEBUG mode
 */
@Singleton
class MockCarPropertyService @Inject constructor() : CarPropertyService {

    private var currentMode: SuspensionMode = SuspensionMode.Comfort
    private var currentSpeed: Float = 0f
    private val modeObservers = mutableListOf<(SuspensionMode) -> Unit>()
    private val speedObservers = mutableListOf<(Float) -> Unit>()

    // Simulate speed changes for demo
    init {
        // Start with some speed
        currentSpeed = 45f / 3.6f // 45 km/h in m/s
    }

    override fun observeSuspensionMode(): Flow<SuspensionMode> = callbackFlow {
        val observer: (SuspensionMode) -> Unit = { mode ->
            trySend(mode)
        }
        modeObservers.add(observer)
        
        // Emit current mode immediately
        trySend(currentMode)
        
        awaitClose {
            modeObservers.remove(observer)
        }
    }.flowOn(Dispatchers.IO)

    override fun observeVehicleSpeed(): Flow<Float> = callbackFlow {
        val observer: (Float) -> Unit = { speed ->
            trySend(speed)
        }
        speedObservers.add(observer)
        
        // Emit current speed immediately
        trySend(currentSpeed)
        
        awaitClose {
            speedObservers.remove(observer)
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun setSuspensionMode(mode: SuspensionMode): Result<Unit> = 
        withContext(Dispatchers.IO) {
            try {
                // Simulate mode change delay (1.5s)
                kotlinx.coroutines.delay(1500)
                
                // Simulate speed restriction for Off-Road above 80 km/h
                val speedKmh = currentSpeed * 3.6f
                if (mode == SuspensionMode.OffRoad && speedKmh > 80) {
                    Log.w(TAG, "Off-Road mode blocked: speed is ${speedKmh.toInt()} km/h")
                    return@withContext Result.failure(
                        IllegalStateException("Off-road mode blocked above 80 km/h")
                    )
                }
                
                currentMode = mode
                modeObservers.forEach { it(mode) }
                
                Log.d(TAG, "Mock: Set suspension mode to: ${mode.displayName}")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Mock: Failed to set suspension mode", e)
                Result.failure(e)
            }
        }

    fun setSpeed(speedKmh: Float) {
        currentSpeed = speedKmh / 3.6f
        speedObservers.forEach { it(currentSpeed) }
    }

    override fun close() {
        modeObservers.clear()
        speedObservers.clear()
    }

    companion object {
        private const val TAG = "MockCarPropertyService"
    }
}