package com.wavebalance.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    onPrimary = OnPrimaryBlue,
    primaryContainer = PrimaryContainerBlue,
    onPrimaryContainer = DarkSurface,
    secondary = SecondaryEmerald,
    onSecondary = OnSecondaryEmerald,
    secondaryContainer = SecondaryContainerEmerald,
    onSecondaryContainer = DarkSurface,
    tertiary = TertiaryAmber,
    onTertiary = OnTertiaryAmber,
    tertiaryContainer = TertiaryContainerAmber,
    onTertiaryContainer = DarkSurface,
    background = DarkBackground,
    onBackground = OnSurfaceText,
    surface = DarkSurface,
    onSurface = OnSurfaceText,
    surfaceVariant = DarkSurfaceContainerHigh,
    onSurfaceVariant = OnSurfaceVariantText,
    surfaceContainerLowest = DarkSurfaceContainerLowest,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
    outline = OutlineBorder,
    outlineVariant = OutlineBorderVariant,
    error = ErrorRed,
    errorContainer = ErrorContainerRed,
    onError = OnErrorRed
)

// WaveBalance uses Deep Slate Dark Palette by default for Prosumer / Analyzer aesthetics
private val LightColorScheme = DarkColorScheme

private val StitchShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun WaveBalanceTheme(
    darkTheme: Boolean = true, // Network telemetry looks best in tech dark mode
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = StitchShapes,
        content = content
    )
}
