package com.azerostudio.kadan.ui.theme

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically

/*
 * Kadan motion language — v1.4 design lock.
 *
 * One motion, everywhere. No screen invents its own easing, duration or
 * direction. The spec is deliberately small:
 *
 *  - Hero: drops in from above on a spring (the Fotara-style spec, in
 *    Kadan's own numbers: -1/3 of its height, spring damping 0.8,
 *    medium-low stiffness) plus a fade. It is the only element allowed
 *    a spring — it is the first thing the eye lands on.
 *  - Stats: fade + small rise, staggered 80 ms apart. Same tween,
 *    same distance, only the delay differs.
 *  - Task rows: LazyColumn animateItem (Compose default), no custom
 *    animation here — rows must never fight the list.
 *
 * Anything not listed here does not animate. A reminder app that
 * animates everything feels slow; a reminder app that animates one
 * thing on entry feels alive.
 */
val HeroEnterTransition: EnterTransition =
    slideInVertically(
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = Spring.StiffnessMediumLow
        ),
        initialOffsetY = { -it / 3 }
    ) + fadeIn(animationSpec = tween(durationMillis = 300))

fun statEnterTransition(delayMillis: Int): EnterTransition =
    fadeIn(
        animationSpec = tween(durationMillis = 300, delayMillis = delayMillis)
    ) + slideInVertically(
        animationSpec = tween(durationMillis = 300, delayMillis = delayMillis),
        initialOffsetY = { it / 4 }
    )
