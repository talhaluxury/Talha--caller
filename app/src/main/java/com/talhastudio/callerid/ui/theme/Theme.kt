package com.talhastudio.callerid.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Palette: pearl, frosted white, silver, black, champagne. No blue anywhere.
val Pearl = Color(0xFFFAF8F5)
val PearlDeep = Color(0xFFEDEBE8)
val Frost = Color(0xFFFFFFFF)
val Silver = Color(0xFFC9CBCF)
val SilverText = Color(0xFF7D8086)
val Ink = Color(0xFF111111)
val Champagne = Color(0xFFC9AA6B)

private val Scheme = lightColorScheme(
    primary = Ink, onPrimary = Color.White,
    secondary = Champagne, onSecondary = Ink,
    tertiary = Champagne,
    background = Pearl, onBackground = Ink,
    surface = Pearl, onSurface = Ink,
    surfaceVariant = Frost, onSurfaceVariant = SilverText,
    outline = Silver, outlineVariant = Silver,
    primaryContainer = Frost, onPrimaryContainer = Ink,
    secondaryContainer = Frost, onSecondaryContainer = Ink,
    surfaceTint = Color.Transparent,
    inverseSurface = Ink, inverseOnSurface = Color.White,
)

private val Type = Typography(
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Light, fontSize = 30.sp, letterSpacing = 0.5.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, lineHeight = 20.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 11.sp, letterSpacing = 1.sp),
)

@Composable
fun TalhaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, typography = Type, content = content)
}
