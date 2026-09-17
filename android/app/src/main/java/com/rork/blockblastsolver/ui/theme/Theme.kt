package com.rork.blockblastsolver.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val NeonDarkScheme = darkColorScheme(
    primary = NeonTeal,
    onPrimary = Canvas,
    primaryContainer = NeonTealDim,
    onPrimaryContainer = TextPrimary,
    secondary = NeonPink,
    onSecondary = Canvas,
    secondaryContainer = NeonPinkDim,
    onSecondaryContainer = TextPrimary,
    tertiary = NeonGold,
    onTertiary = Canvas,
    background = Canvas,
    onBackground = TextPrimary,
    surface = Canvas,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = SurfaceElevated,
    surfaceContainerHigh = SurfaceElevatedHigh,
    surfaceContainerHighest = SurfaceElevatedHigh,
    surfaceContainerLow = SurfaceElevated,
    surfaceContainerLowest = Canvas,
    outline = GridLine,
    outlineVariant = Divider,
    error = NeonPink,
    onError = Canvas
)

/** The app is dark-neon only by design; system dark/light and dynamic color are ignored. */
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NeonDarkScheme,
        typography = AppTypography,
        content = content
    )
}
