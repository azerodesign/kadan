package com.azerostudio.kadan.ui.theme

import androidx.compose.ui.unit.dp

/*
 * Kadan spacing scale — v1.4 design lock.
 *
 * This is the Fotara lesson translated, not copied: a product looks
 * "cakep" when every screen uses the SAME numbers, not when one screen
 * invents a new padding. No screen may invent a padding, gap or radius
 * outside this scale + KadanShapes. If a layout needs a number that is
 * not here, add it here first, with a reason, so every screen moves
 * together.
 *
 *  - Screen: outer horizontal padding of a scrolling screen.
 *  - Section: gap between major blocks (hero -> stats -> filters).
 *  - Gap: gap inside a row of chips / small controls.
 *  - Card: padding inside a normal card.
 *  - Compact: padding inside a dense card (stat cards).
 *  - Hero: padding inside the hero card (the one element allowed to
 *    breathe more than a normal card).
 *  - Accent: the small strip on top of a stat card.
 */
object KadanSpacing {
    val Screen = 16.dp
    val Section = 12.dp
    val Gap = 8.dp
    val Card = 16.dp
    val Compact = 12.dp
    val Hero = 20.dp
    val Tiny = 4.dp
    val Small = 8.dp
    val Large = 16.dp
    val AccentWidth = 28.dp
    val AccentHeight = 4.dp
    // Bottom clearance so the last row never hides under the FAB.
    val FabClearance = 80.dp
}
