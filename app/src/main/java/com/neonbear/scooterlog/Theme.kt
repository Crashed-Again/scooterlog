package com.neonbear.scooterlog

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Scheme = darkColorScheme(
    primary = Color(0xFF3B82F6),
    onPrimary = Color.White,
    secondary = Color(0xFF22D3EE),
    background = Color(0xFF0A0F1E),
    onBackground = Color(0xFFE6EBFA),
    surface = Color(0xFF121A2E),
    onSurface = Color(0xFFE6EBFA),
    surfaceVariant = Color(0xFF1A2440),
    onSurfaceVariant = Color(0xFF9AA7C7),
)

@Composable
fun ScooterTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}
