package com.wavebalance.app.ui.adaptive

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Window width classes, following the Material 3 breakpoints.
 * COMPACT: phones in portrait. MEDIUM: foldables, small tablets, narrow windows.
 * EXPANDED: tablets in landscape, laptops (e.g. a Googlebook window), desktops.
 */
enum class WindowLayout {
    COMPACT,
    MEDIUM,
    EXPANDED;

    val isCompact: Boolean get() = this == COMPACT
    val isExpanded: Boolean get() = this == EXPANDED

    companion object {
        fun fromWidth(width: Dp): WindowLayout = when {
            width < 600.dp -> COMPACT
            width < 840.dp -> MEDIUM
            else -> EXPANDED
        }
    }
}

/**
 * The width class of the whole app window, so screens can pick a layout
 * without measuring the window themselves.
 */
val LocalWindowLayout = staticCompositionLocalOf { WindowLayout.COMPACT }
