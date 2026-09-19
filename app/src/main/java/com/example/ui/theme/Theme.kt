package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = HdrAmber,
    onPrimary = Color.Black,
    primaryContainer = HdrAmberDark,
    onPrimaryContainer = Color.White,
    secondary = HighlightCyan,
    onSecondary = Color.Black,
    secondaryContainer = TitaniumSurfaceElevated,
    onSecondaryContainer = TextPrimary,
    tertiary = ShadowIndigo,
    background = TitaniumDark,
    onBackground = TextPrimary,
    surface = TitaniumSurface,
    onSurface = TextPrimary,
    surfaceVariant = TitaniumSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = TitaniumSurfaceBorder
  )

private val LightColorScheme = DarkColorScheme // Camera UI is consistently dark for contrast accuracy

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force pro dark camera theme
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  MaterialTheme(colorScheme = DarkColorScheme, typography = Typography, content = content)
}
