package com.wavebalance.app

import com.wavebalance.app.ui.adaptive.AdaptiveLayoutPolicy
import org.junit.Assert.*
import org.junit.Test

class AdaptiveLayoutPolicyTest {
    @Test fun navigationLeavesMoreContentRoomOnTablets() {
        assertFalse(AdaptiveLayoutPolicy.useNavigationPanel(840f, 800f))
        assertFalse(AdaptiveLayoutPolicy.useNavigationPanel(1199f, 800f))
        assertTrue(AdaptiveLayoutPolicy.useNavigationPanel(1200f, 800f))
    }

    @Test fun panesUseContentSpaceAfterNavigationAndInsets() {
        assertFalse(AdaptiveLayoutPolicy.useTwoPanes(840f - 248f, 600f))
        assertFalse(AdaptiveLayoutPolicy.useTwoPanes(839f, 600f))
        assertTrue(AdaptiveLayoutPolicy.useTwoPanes(840f, 600f))
        assertTrue(AdaptiveLayoutPolicy.useTwoPanes(1280f - 248f, 700f))
    }

    @Test fun shortLandscapeWindowsStaySinglePaneWithReachableNavigation() {
        assertFalse(AdaptiveLayoutPolicy.useTwoPanes(1400f, 479f))
        assertFalse(AdaptiveLayoutPolicy.useNavigationPanel(1400f, 479f))
        assertTrue(AdaptiveLayoutPolicy.useTwoPanes(1400f, 480f))
    }

    @Test fun enlargedTextRequiresMoreRoomForPanesAndNavigation() {
        assertFalse(AdaptiveLayoutPolicy.useTwoPanes(1200f, 700f, 2f))
        assertTrue(AdaptiveLayoutPolicy.useTwoPanes(1680f, 700f, 2f))
        assertFalse(AdaptiveLayoutPolicy.useNavigationPanel(1600f, 900f, 2f))
        assertTrue(AdaptiveLayoutPolicy.useNavigationPanel(2400f, 900f, 2f))
        assertFalse(AdaptiveLayoutPolicy.useTwoPanes(800f, 700f, 0.8f))
    }
}
