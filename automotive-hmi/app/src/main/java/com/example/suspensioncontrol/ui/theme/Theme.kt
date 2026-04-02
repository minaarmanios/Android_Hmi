package com.example.suspensioncontrol.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Dark Automotive Color Scheme
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4A90D9),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF1A3A5C),
    onPrimaryContainer = Color(0xFFD6E4FF),
    
    secondary = Color(0xFF7EB8DA),
    onSecondary = Color(0xFF00344F),
    secondaryContainer = Color(0xFF004C75),
    onSecondaryContainer = Color(0xFFC5E7FF),
    
    tertiary = Color(0xFFC9A9E3),
    onTertiary = Color(0xFF3D2A55),
    tertiaryContainer = Color(0xFF55406D),
    onTertiaryContainer = Color(0xFFF3DAFF),
    
    background = Color(0xFF1A1A1A),
    onBackground = Color(0xFFFFFFFF),
    
    surface = Color(0xFF1A1A1A),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF2D2D2D),
    onSurfaceVariant = Color(0xFFB0B0B0),
    
    error = Color(0xFFFF4444),
    errorContainer = Color(0xFF4A2020),
    onError = Color(0xFF000000),
    
    outline = Color(0xFF444444),
    outlineVariant = Color(0xFF333333),
    
    inverseSurface = Color(0xFFFFFFFF),
    inverseOnSurface = Color(0xFF1A1A1A),
    inversePrimary = Color(0xFF1A3A5C),
    
    scrim = Color(0xFF000000)
)

// Custom colors for specific use cases
object SuspensionColors {
    val Warning = Color(0xFFFFB74D)
    val WarningContainer = Color(0xFF3D2A10)
    val Success = Color(0xFF4CAF50)
    val Disabled = Color(0xFF666666)
}

@Composable
fun SuspensionControlTheme(
    darkTheme: Boolean = true, // Always dark for automotive
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}