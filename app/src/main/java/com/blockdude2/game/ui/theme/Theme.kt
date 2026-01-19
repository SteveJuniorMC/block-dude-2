package com.blockdude2.game.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = AccentOrange,
    secondary = PrimaryBlue,
    tertiary = PlayerColor,
    background = DarkBackground,
    surface = SurfaceColor,
    onPrimary = TextWhite,
    onSecondary = TextWhite,
    onTertiary = DarkBackground,
    onBackground = TextWhite,
    onSurface = TextWhite
)

@Composable
fun BlockDude2Theme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
