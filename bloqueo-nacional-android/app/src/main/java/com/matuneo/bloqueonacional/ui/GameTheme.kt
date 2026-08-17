package com.matuneo.bloqueonacional.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GameColors = darkColorScheme(
    primary = Color(0xFFFFD54F),
    onPrimary = Color(0xFF1B2430),
    secondary = Color(0xFF49C76D),
    onSecondary = Color(0xFF071522),
    tertiary = Color(0xFFE95D56),
    background = Color(0xFF071522),
    surface = Color(0xFF10243A),
    surfaceVariant = Color(0xFF1A3853),
    onSurface = Color(0xFFFFF4D6),
    onBackground = Color(0xFFFFF4D6)
)

@Composable
fun BloqueoNacionalTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = GameColors, content = content)
}

