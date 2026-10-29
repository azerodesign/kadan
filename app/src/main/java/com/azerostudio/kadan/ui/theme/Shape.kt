package com.azerostudio.kadan.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/*
 * Material 3 shape scale for Kadan, set once here.
 * Screens use MaterialTheme.shapes.medium / .large etc., never a literal
 * RoundedCornerShape(...) at a call site.
 */
val KadanShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)
