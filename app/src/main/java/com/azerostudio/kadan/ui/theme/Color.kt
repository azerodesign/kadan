package com.azerostudio.kadan.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/*
 * THE ONLY file in Kadan where literal colours may appear.
 *
 * Kadan v1.2 brand — Biru Laut + Hitam Charcoal, gaya semi-tech / SaaS.
 *  - Biru Laut (Ocean Blue): deep ocean #0369A1 in light, bright ocean
 *    #38B6E8 in dark so it stays legible on charcoal.
 *  - Hitam Charcoal: dark background #0C1116, cards one layer lighter
 *    (#17222A), never pure black cards on pure black background.
 *  - Light scheme is the same brand in reverse: pale ocean-tinted
 *    surfaces (#F2F6F9) so light and dark feel like one product, not two.
 *
 * Rule (Material 3): screens must never use a hex value or
 * Color.White/Black directly. They read roles only, e.g.
 * MaterialTheme.colorScheme.primary, .surfaceContainerLow,
 * .onSurfaceVariant, .errorContainer. That is what makes light/dark
 * switching work without touching every screen.
 *
 * Dynamic colour (Material You) is OFF by default in KadanTheme for v1.2:
 * a reminder's due-time legibility and the ocean/charcoal brand matter
 * more than wallpaper matching.
 */
// Brand Accent Colors for gradients and ambient effects
val CyanFixedDim = Color(0xFF00DAF3)
val KadanBlue = Color(0xFF2563EB)
val KadanFocusDarkBackground = Color(0xFF070A10)

val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0369A1),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD6EEF9),
    onPrimaryContainer = Color(0xFF04395A),
    secondary = Color(0xFF0E7490),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCDEFF5),
    onSecondaryContainer = Color(0xFF073A44),
    tertiary = Color(0xFF0284C7),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD6EEF9),
    onTertiaryContainer = Color(0xFF04395A),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFF2F6F9),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0C1116),
    onSurfaceVariant = Color(0xFF4A5A66),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEDF2F6),
    surfaceContainer = Color(0xFFE2EAF0),
    surfaceContainerHigh = Color(0xFFD6E0E8),
    surfaceContainerHighest = Color(0xFFC9D6DF),
    outline = Color(0xFF7A8A96),
    outlineVariant = Color(0xFFC2CFD8),
    inverseSurface = Color(0xFF17222B),
    inverseOnSurface = Color(0xFFEAF2F7),
    inversePrimary = Color(0xFF7DD3F0),
    scrim = Color(0xFF000000)
)

val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF38B6E8),
    onPrimary = Color(0xFF04293D),
    primaryContainer = Color(0xFF0B3A52),
    onPrimaryContainer = Color(0xFFCDEFFB),
    secondary = Color(0xFF67C7D8),
    onSecondary = Color(0xFF042A33),
    secondaryContainer = Color(0xFF123A44),
    onSecondaryContainer = Color(0xFFC5EFF5),
    tertiary = Color(0xFF7DD3FC),
    onTertiary = Color(0xFF04293D),
    tertiaryContainer = Color(0xFF0B3A52),
    onTertiaryContainer = Color(0xFFCDEFFB),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    background = Color(0xFF0C1116),
    surface = Color(0xFF0C1116),
    onSurface = Color(0xFFE6EEF3),
    onSurfaceVariant = Color(0xFFA8B8C2),
    surfaceContainerLowest = Color(0xFF080B0E),
    surfaceContainerLow = Color(0xFF11181E),
    surfaceContainer = Color(0xFF17222A),
    surfaceContainerHigh = Color(0xFF1E2B35),
    surfaceContainerHighest = Color(0xFF283642),
    outline = Color(0xFF8A9BA6),
    outlineVariant = Color(0xFF33424D),
    inverseSurface = Color(0xFFE6EEF3),
    inverseOnSurface = Color(0xFF17222B),
    inversePrimary = Color(0xFF0369A1),
    scrim = Color(0xFF000000)
)
