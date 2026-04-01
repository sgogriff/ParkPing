package com.gowain.parkping.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ParkPingDarkColors = darkColorScheme(
    primary = BlueAccent,
    secondary = TealAccent,
    tertiary = AmberAccent,
    background = Navy900,
    surface = Navy800,
    surfaceVariant = Navy700,
    primaryContainer = Navy700,
    secondaryContainer = Color(0xFF1E4E52),
    tertiaryContainer = Color(0xFF5B4614),
    onPrimary = Mist,
    onSecondary = Mist,
    onTertiary = Navy950,
    onPrimaryContainer = Mist,
    onSecondaryContainer = Mist,
    onTertiaryContainer = Mist,
    onBackground = Mist,
    onSurface = Mist,
    onSurfaceVariant = Slate,
)

@Composable
fun ParkPingTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = ParkPingDarkColors,
        typography = ParkPingTypography,
        content = content,
    )
}
