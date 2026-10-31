package com.azerostudio.kadan.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/*
 * KadanTheme — the single theme wrapper for the whole app.
 * MainActivity wraps ALL of setContent in this. Anything drawn outside
 * this wrapper silently gets the baseline Material theme, so never launch
 * a dialog or a new screen outside it.
 *
 * v1.2: dark theme is a separate charcoal scheme (Color.kt), NOT an
 * inversion of light. The caller (MainActivity) resolves ThemeMode to a
 * Boolean: SYSTEM follows isSystemInDarkTheme(), LIGHT forces light,
 * DARK forces dark. The choice is persisted in DataStore by
 * ThemePreferences, so it survives restarts.
 *
 * Dynamic colour (Material You) is OFF by default in v1.2: the ocean /
 * charcoal brand and due-time legibility matter more than wallpaper
 * matching. Pass dynamicColor = true only if a future version explicitly
 * wants Material You back.
 */
@Composable
fun KadanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = KadanTypography,
        shapes = KadanShapes,
        content = content
    )
}
