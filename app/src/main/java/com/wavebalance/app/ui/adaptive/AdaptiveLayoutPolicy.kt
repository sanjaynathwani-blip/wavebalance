package com.wavebalance.app.ui.adaptive

/** Decisions use available dp, never device identity or orientation. */
object AdaptiveLayoutPolicy {
    fun useNavigationPanel(widthDp: Float, heightDp: Float, fontScale: Float = 1f): Boolean =
        widthDp >= 1200f * fontScale.coerceAtLeast(1f) && heightDp >= 480f

    fun useTwoPanes(contentWidthDp: Float, contentHeightDp: Float, fontScale: Float = 1f): Boolean =
        contentWidthDp >= 840f * fontScale.coerceAtLeast(1f) && contentHeightDp >= 480f
}
