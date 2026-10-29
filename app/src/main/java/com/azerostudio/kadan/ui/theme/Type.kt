package com.azerostudio.kadan.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/*
 * Material 3 type scale for Kadan.
 * Only the styles Kadan actually uses are overridden; everything else
 * keeps the Material 3 default (Roboto). Sizes are in sp so they respect
 * the user's system font-size setting — never use dp for text, and never
 * invent a new size in a screen when a role already exists.
 *  - titleMedium: reminder titles (the workhorse)
 *  - bodyMedium:  reminder notes / detail lines
 *  - labelLarge:  buttons, chips, form labels
 *  - bodySmall:   time / category metadata, always with onSurfaceVariant
 */
val KadanTypography = Typography(
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    )
)
