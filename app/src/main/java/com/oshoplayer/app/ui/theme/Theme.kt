package com.oshoplayer.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    background = BackgroundStart,
    surface = BackgroundMiddle,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    primary = TextPrimaryDark,
    secondary = TextSecondaryDark
)

private val LightColors = lightColorScheme(
    background = BackgroundStartLight,
    surface = BackgroundEndLight,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    primary = TextPrimaryLight,
    secondary = TextSecondaryLight
)

@Composable
fun OshoPlayerTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (useDarkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}
