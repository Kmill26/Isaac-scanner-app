package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val IsaacColorScheme = darkColorScheme(
    primary = IsaacPrimaryCrimson,
    onPrimary = Color.White,
    primaryContainer = IsaacPrimaryContainer,
    onPrimaryContainer = IsaacOnPrimaryContainer,
    secondary = IsaacSecondaryAmber,
    onSecondary = Color(0xFF1F1500),
    secondaryContainer = IsaacSecondaryContainer,
    onSecondaryContainer = Color(0xFFFFECB3),
    tertiary = IsaacTertiaryGlow,
    onTertiary = Color(0xFF002233),
    background = IsaacBackground,
    onBackground = IsaacOnBackground,
    surface = IsaacSurface,
    onSurface = IsaacOnSurface,
    surfaceVariant = IsaacSurfaceVariant,
    onSurfaceVariant = IsaacOnSurfaceVariant,
    outline = IsaacBorder,
    outlineVariant = IsaacBorderSubtle
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false, // Keep cohesive Isaac gothic arcade styling
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = IsaacColorScheme,
        typography = Typography,
        content = content
    )
}

