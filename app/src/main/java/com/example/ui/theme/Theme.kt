package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val StudioDarkColorScheme =
  darkColorScheme(
    primary = CinemaAmber,
    onPrimary = OnCinemaAmber,
    primaryContainer = CinemaAmberContainer,
    onPrimaryContainer = CinemaAmber,
    secondary = ElectricCyan,
    onSecondary = OnElectricCyan,
    secondaryContainer = ElectricCyanContainer,
    onSecondaryContainer = ElectricCyan,
    tertiary = VioletMotion,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = StudioSurface,
    onSurface = TextPrimary,
    surfaceVariant = StudioSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLow = StudioSurface,
    surfaceContainer = StudioSurfaceElevated,
    surfaceContainerHigh = StudioSurfaceHighest,
    outline = HairlineBorder,
    outlineVariant = HairlineBorder,
    error = CrimsonRecord,
    onError = TextPrimary,
  )

private val StudioShapes =
  Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
  )

@Composable
fun VideoAgentTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = StudioDarkColorScheme,
    typography = StudioTypography,
    shapes = StudioShapes,
    content = content,
  )
}
