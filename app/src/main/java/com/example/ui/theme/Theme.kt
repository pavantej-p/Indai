package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

val StudioDarkColorScheme = darkColorScheme(
    primary = SaffronAmber,
    onPrimary = OnSaffronAmber,
    primaryContainer = SaffronAmberDark,
    onPrimaryContainer = SaffronAmberLight,
    secondary = ElectricTeal,
    onSecondary = OnElectricTeal,
    secondaryContainer = ElectricTealDark,
    onSecondaryContainer = ElectricTealLight,
    tertiary = WarmCoral,
    onTertiary = OnWarmCoral,
    tertiaryContainer = WarmCoralDark,
    onTertiaryContainer = WarmCoral,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = StudioSurface,
    onSurface = TextPrimary,
    surfaceVariant = StudioSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = ObsidianBg,
    surfaceContainerLow = StudioSurface,
    surfaceContainer = StudioSurfaceElevated,
    surfaceContainerHigh = StudioSurfaceHighest,
    surfaceContainerHighest = StudioSurfaceHighest,
    outline = StudioBorder,
    outlineVariant = StudioBorder.copy(alpha = 0.6f),
    error = ErrorRed,
    onError = OnErrorRed
)

val StudioShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun IndiAiGuildTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = StudioDarkColorScheme,
        typography = AppTypography,
        shapes = StudioShapes,
        content = content
    )
}
