package com.bhumivera.scanner.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BhumiveraColors = darkColorScheme(
    primary = Color(0xFFD4AF37),
    onPrimary = Color(0xFF14251C),
    secondary = Color(0xFF9BB0A5),
    background = Color(0xFF101A15),
    surface = Color(0xFF17241D),
    onBackground = Color(0xFFF4F0E7),
    onSurface = Color(0xFFF4F0E7),
    error = Color(0xFFFFB4AB),
)

@Composable
fun BhumiveraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BhumiveraColors,
        content = content,
    )
}
