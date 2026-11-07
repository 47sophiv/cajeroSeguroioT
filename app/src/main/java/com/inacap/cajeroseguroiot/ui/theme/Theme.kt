package com.inacap.cajeroseguroiot.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = BankPrimary,
    onPrimary = Color.White,
    secondary = BankAccentTeal,
    onSecondary = Color.White,
    background = BankNavyDark,
    surface = BankNavySurface,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = BankAlert
)

private val LightColorScheme = lightColorScheme(
    primary = BankPrimaryVariant,
    onPrimary = Color.White,
    secondary = BankAccentTeal,
    onSecondary = Color.White,
    background = Color(0xFFF1F5F9),
    surface = Color.White,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    error = BankAlert
)

@Composable
fun CajeroSeguroTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
