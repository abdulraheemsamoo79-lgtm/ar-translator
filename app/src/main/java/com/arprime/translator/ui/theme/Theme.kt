package com.arprime.translator.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Teal = Color(0xFF0F766E)
private val TealLight = Color(0xFF14B8A6)
private val Amber = Color(0xFFF59E0B)

private val LightColors = lightColorScheme(
    primary = Teal,
    secondary = Amber,
    tertiary = TealLight
)

private val DarkColors = darkColorScheme(
    primary = TealLight,
    secondary = Amber,
    tertiary = Teal
)

@Composable
fun ArTranslatorTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
