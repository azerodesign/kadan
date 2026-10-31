package com.azerostudio.kadan.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable

/*
 * KadanTopBar — the ONE top bar of Kadan, v1.4.
 *
 * Fotara's parity lesson: when every screen builds its own top bar,
 * icons shift position between screens and the product feels sloppy
 * even if each screen looks fine alone. So no screen builds a
 * TopAppBar directly anymore — every screen uses this, with the same
 * container colour, the same brand label above the same headline
 * size, and the same actions slot (same height, same touch targets).
 * A future screen adds actions here; it never restyles the bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KadanTopBar(
    title: String,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        title = {
            Column {
                Text(
                    text = "KADAN",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        },
        actions = actions
    )
}
