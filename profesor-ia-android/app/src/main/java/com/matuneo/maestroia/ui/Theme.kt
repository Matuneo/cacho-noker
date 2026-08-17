package com.matuneo.maestroia.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Night = Color(0xFF07111F)
val NightSoft = Color(0xFF0E1B2E)
val Panel = Color(0xFF13243A)
val Cyan = Color(0xFF20D5E8)
val Violet = Color(0xFF8B5CF6)
val Green = Color(0xFF39D98A)
val Amber = Color(0xFFFFC857)
val TextMain = Color(0xFFF4F8FF)
val TextMuted = Color(0xFFA7B7CC)

private val AppColors = darkColorScheme(
    primary = Cyan,
    onPrimary = Night,
    secondary = Violet,
    tertiary = Green,
    background = Night,
    onBackground = TextMain,
    surface = NightSoft,
    onSurface = TextMain,
    surfaceVariant = Panel,
    onSurfaceVariant = TextMuted,
    error = Color(0xFFFF6B7A)
)

@Composable
fun ProfesorIATheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AppColors, content = content)
}

