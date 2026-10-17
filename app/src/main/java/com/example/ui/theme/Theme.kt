package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ClassicExperienceColorScheme = lightColorScheme(
    primary = ClassicCyan,
    onPrimary = Color.White,
    primaryContainer = ClassicCyan,
    onPrimaryContainer = Color.White,
    secondary = ClassicBlue,
    onSecondary = Color.White,
    tertiary = ClassicGreen,
    onTertiary = Color.White,
    background = ClassicGrayBg,
    onBackground = ClassicTextPrimary,
    surface = ClassicCardBg,
    onSurface = ClassicTextPrimary,
    surfaceVariant = Color(0xFFF0F3F5),
    onSurfaceVariant = ClassicTextSecondary,
    outline = ClassicBorder
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ClassicExperienceColorScheme,
        typography = Typography,
        content = content
    )
}
