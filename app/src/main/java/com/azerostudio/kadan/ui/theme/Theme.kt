package com.azerostudio.kadan.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/*
 * KadanTheme — the single theme wrapper for the whole app.
 * MainActivity wraps ALL of setContent in this. Anything drawn outside
 * this wrapper silently gets the baseline Material theme, so never launch
 * a dialog or a new screen outside it.
 *
 * Dark theme: a separate generated scheme (Color.kt), selected by
 * isSystemInDarkTheme() by default. There is no light/dark toggle in v1 —
 * following the system is the correct, lowest-surprise behaviour for a
 * personal utility app, and it keeps the settings surface at zero.
 *
 * Dynamic colour (Material You): only on Android 12+ (SDK 31+), only from
 * the user's wallpaper, with the static schemes in Color.kt as fallback.
 * It is deliberately NOT forced: a reminder's due-time legibility matters
 * more than wallpaper matching.
 */
@Composable
fun KadanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = KadanTypography,
        shapes = KadanShapes,
        content = content
    )
}
