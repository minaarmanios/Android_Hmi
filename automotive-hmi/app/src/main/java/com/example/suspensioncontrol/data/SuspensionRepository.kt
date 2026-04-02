package com.example.suspensioncontrol.data

import android.util.Log
import com.example.suspensioncontrol.domain.model.SuspensionError
import com.example.suspensioncontrol.domain.model.SuspensionMode
import com.example.suspensioncontrol.domain.model.SuspensionProperties
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for suspension mode data.
 * Provides a clean API for the ViewModel to interact with VHAL properties.
 * Handles error cases and timeout management.
 */
interface SuspensionRepository {
    fun observeSuspensionMode(): Flow<SuspensionMode>
    fun observeVehicleSpeed(): Flow<Float>
    fun observeCombinedState(): Flow<Pair<SuspensionMode, Float>>
    suspend fun setSuspensionMode(mode: SuspensionMode): Result<Unit>
    suspend fun getCurrentMode(): SuspensionMode
    suspend fun getCurrentSpeed(): Float
}

/**
 * Implementation of SuspensionRepository.
 * Uses CarPropertyService to interact with VHAL.
 */
@Singleton
class SuspensionRepositoryImpl @Inject constructor(
    private val carPropertyService: CarPropertyService
) : SuspensionRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var cachedMode: SuspensionMode = SuspensionMode.Comfort
    private var cachedSpeed: Float = 0f

    init {
        // Cache values when they change
        scope.launch {
            carPropertyService.observeSuspensionMode()
                .catch { e -> Log.e(TAG, "Error observing mode", e) }
                .collect { mode -> cachedMode = mode }
        }
        scope.launch {
            carPropertyService.observeVehicleSpeed()
                .catch { e -> Log.e(TAG, "Error observing speed", e) }
                .collect { speed -> cachedSpeed = speed }
        }
    }

    override fun observeSuspensionMode(): Flow<SuspensionMode> =
        carPropertyService.observeSuspensionMode()
            .catch { e ->
                Log.e(TAG, "Error observing suspension mode", e)
                emit(SuspensionMode.Unknown)
            }

    override fun observeVehicleSpeed(): Flow<Float> =
        carPropertyService.observeVehicleSpeed()
            .catch { e ->
                Log.e(TAG, "Error observing vehicle speed", e)
                emit(0f)
            }

    override fun observeCombinedState(): Flow<Pair<SuspensionMode, Float>> =
        combine(
            observeSuspensionMode(),
            observeVehicleSpeed()
        ) { mode, speed -> Pair(mode, speed) }

    override suspend fun setSuspensionMode(mode: SuspensionMode): Result<Unit> {
        return try {
            // Set with timeout
            val result = withTimeout(SuspensionProperties.ECU_TIMEOUT_MS) {
                carPropertyService.setSuspensionMode(mode)
            }
            
            if (result.isSuccess) {
                cachedMode = mode
                Log.d(TAG, "Successfully set mode to: ${mode.displayName}")
            }
            
            result
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            Log.e(TAG, "Timeout setting suspension mode", e)
            Result.failure(SuspensionError.EcuTimeout)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting suspension mode", e)
            Result.failure(e)
        }
    }

    override suspend fun getCurrentMode(): SuspensionMode = cachedMode
    override suspend fun getCurrentSpeed(): Float = cachedSpeed

    companion object {
        private const val TAG = "SuspensionRepository"
    }
}