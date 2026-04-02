package com.example.suspensioncontrol.di

import android.content.Context
import com.example.suspensioncontrol.BuildConfig
import com.example.suspensioncontrol.data.CarPropertyService
import com.example.suspensioncontrol.data.MockCarPropertyService
import com.example.suspensioncontrol.data.RealCarPropertyService
import com.example.suspensioncontrol.data.SuspensionRepository
import com.example.suspensioncontrol.data.SuspensionRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Provides CarPropertyService based on build configuration.
     * In DEBUG mode (emulator), uses MockCarPropertyService for simulation.
     * In RELEASE mode, uses RealCarPropertyService to connect to actual VHAL.
     */
    @Provides
    @Singleton
    fun provideCarPropertyService(
        @ApplicationContext context: Context
    ): CarPropertyService {
        return if (BuildConfig.DEBUG) {
            // Use mock in emulator/debug builds
            MockCarPropertyService()
        } else {
            // Use real VHAL in production
            RealCarPropertyService(context)
        }
    }

    /**
     * Provides SuspensionRepository using the configured CarPropertyService
     */
    @Provides
    @Singleton
    fun provideSuspensionRepository(
        carPropertyService: CarPropertyService
    ): SuspensionRepository {
        return SuspensionRepositoryImpl(carPropertyService)
    }
}