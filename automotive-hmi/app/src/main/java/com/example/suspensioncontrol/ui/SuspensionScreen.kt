package com.example.suspensioncontrol.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.Sports
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.suspensioncontrol.domain.model.SuspensionMode
import com.example.suspensioncontrol.domain.model.SuspensionError
import com.example.suspensioncontrol.domain.contracts.SuspensionContract

/**
 * Main Suspension Control Screen
 * Displays suspension mode selection buttons and current vehicle state
 */
@Composable
fun SuspensionScreen(
    viewModel: SuspensionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Status Bar
            StatusBar(
                speedKmh = state.displaySpeed,
                currentMode = state.currentMode
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Warning Bar (if Off-Road is restricted)
            if (state.isOffRoadRestricted) {
                WarningBar(message = "Off-road mode restricted above 30 km/h")
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Error Bar (if there's an error)
            state.error?.let { error ->
                ErrorBar(
                    error = error,
                    onDismiss = { viewModel.processIntent(SuspensionContract.Intent.DismissError) }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Main Content - Mode Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SuspensionMode.allModes.forEach { mode ->
                    ModeButton(
                        mode = mode,
                        isSelected = state.currentMode == mode,
                        isLoading = state.isLoading && state.currentMode != mode,
                        onClick = { 
                            viewModel.processIntent(SuspensionContract.Intent.SelectMode(mode)) 
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        }
        
        // Confirmation Dialog for Off-Road at speed
        if (state.showOffRoadConfirmation) {
            OffRoadConfirmationDialog(
                currentSpeed = state.speedKmh,
                onConfirm = { viewModel.processIntent(SuspensionContract.Intent.ConfirmOffRoad) },
                onCancel = { viewModel.processIntent(SuspensionContract.Intent.CancelConfirmation) }
            )
        }
    }
}

/**
 * Status bar showing current speed and mode
 */
@Composable
private fun StatusBar(
    speedKmh: String,
    currentMode: SuspensionMode
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(12.dp)
            )
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Speed Display
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.DirectionsCar,
                contentDescription = "Speed",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Speed: $speedKmh",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        // Current Mode Display
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Current Mode: ",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = currentMode.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Warning bar for speed restrictions
 */
@Composable
private fun WarningBar(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color(0xFF3D2A10),
                RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Warning",
            tint = Color(0xFFFFB74D),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = message,
            color = Color(0xFFFFB74D),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * Error bar for system errors
 */
@Composable
private fun ErrorBar(
    error: SuspensionError,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color(0xFF4A2020),
                RoundedCornerShape(8.dp)
            )
            .clickable { onDismiss() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Error",
            tint = Color(0xFFFF4444),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = error.message,
            color = Color(0xFFFF4444),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onDismiss) {
            Text("Dismiss", color = Color(0xFFFF4444))
        }
    }
}

/**
 * Mode selection button
 */
@Composable
private fun ModeButton(
    mode: SuspensionMode,
    isSelected: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(200),
        label = "backgroundColor"
    )
    
    val contentColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }
    
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = tween(100),
        label = "scale"
    )
    
    Box(
        modifier = modifier
            .size(160.dp)
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(enabled = !isLoading) { onClick() }
            .semantics { contentDescription = "${mode.displayName} mode" },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = contentColor,
                    strokeWidth = 4.dp
                )
            } else {
                Icon(
                    imageVector = getModeIcon(mode),
                    contentDescription = mode.displayName,
                    tint = contentColor,
                    modifier = Modifier.size(64.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = mode.displayName,
                color = contentColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Get the appropriate icon for each suspension mode
 */
private fun getModeIcon(mode: SuspensionMode): ImageVector = when (mode) {
    is SuspensionMode.Comfort -> Icons.Default.DirectionsCar // Soft ride icon
    is SuspensionMode.Sport -> Icons.Default.Sports // Dynamic icon
    is SuspensionMode.OffRoad -> Icons.Default.Terrain // Terrain icon
    is SuspensionMode.Auto -> Icons.Default.AutoMode // Auto/adaptive icon
    is SuspensionMode.Unknown -> Icons.Default.DirectionsCar
}

/**
 * Confirmation dialog for Off-Road mode at high speed
 */
@Composable
private fun OffRoadConfirmationDialog(
    currentSpeed: Float,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                text = "Enable Off-Road Mode?",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = "Off-road mode is not recommended above 30 km/h.\n\nCurrent speed: ${currentSpeed.toInt()} km/h",
                style = MaterialTheme.typography.bodyLarge
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(24.dp)
    )
}