package com.spendora.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PurplePrimaryDark,
    onPrimary = PurpleOnPrimaryDark,
    primaryContainer = PurpleContainerDark,
    onPrimaryContainer = PurpleOnContainerDark,
    secondary = Color(0xFFA855F7),
    onSecondary = Color(0xFF2E1065),
    secondaryContainer = Color(0xFF3B1E54),
    onSecondaryContainer = Color(0xFFF3E8FF),
    background = BackgroundDark,
    onBackground = Color(0xFFECE6F0),
    surface = SurfaceDark,
    onSurface = Color(0xFFECE6F0),
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = OutlineDark,
    error = ExpenseRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = PurplePrimaryLight,
    onPrimary = PurpleOnPrimaryLight,
    primaryContainer = PurpleContainerLight,
    onPrimaryContainer = PurpleOnContainerLight,
    secondary = Color(0xFF9333EA),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF3E8FF),
    onSecondaryContainer = Color(0xFF3B0764),
    background = BackgroundLight,
    onBackground = Color(0xFF1D1B20),
    surface = SurfaceLight,
    onSurface = Color(0xFF1D1B20),
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = Color(0xFF49454F),
    outline = OutlineLight,
    error = ExpenseRed,
    onError = Color.White
)

@Composable
fun SpendoraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
