package com.example.hestia.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val HestiaScheme = lightColorScheme(
    primary = HestiaInk,
    onPrimary = HestiaCream,
    primaryContainer = HestiaGoldLight,
    onPrimaryContainer = HestiaInk,
    secondary = HestiaTerracotta,
    onSecondary = Color.White,
    secondaryContainer = HestiaCream2,
    onSecondaryContainer = HestiaInk,
    background = HestiaCream,
    onBackground = HestiaInk,
    surface = HestiaSurface,
    onSurface = HestiaInk,
    surfaceVariant = HestiaCream2,
    onSurfaceVariant = HestiaMuted,
    outline = HestiaBorder,
    error = HestiaRed,
    onError = Color.White
)

@Composable
fun HestiaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = HestiaScheme, typography = HestiaTypography, content = content)
}
