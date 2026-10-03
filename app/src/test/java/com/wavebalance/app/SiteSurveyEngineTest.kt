package com.wavebalance.app

import com.wavebalance.app.model.FrequencyBand
import com.wavebalance.app.model.SiteSurveyEngine
import com.wavebalance.app.model.SurveyPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SiteSurveyEngineTest {

    @Test
    fun testFindRoomForCoordinate() {
        // Known default zones
        assertEquals("Living Room", SiteSurveyEngine.findRoomForCoordinate(0.25f, 0.25f))
        assertEquals("Office / Lab", SiteSurveyEngine.findRoomForCoordinate(0.70f, 0.20f))
        assertEquals("Kitchen / Dining", SiteSurveyEngine.findRoomForCoordinate(0.20f, 0.70f))
        assertEquals("Master Bedroom", SiteSurveyEngine.findRoomForCoordinate(0.70f, 0.60f))
        assertEquals("Patio / Balcony", SiteSurveyEngine.findRoomForCoordinate(0.70f, 0.85f))
        assertEquals("Open Space", SiteSurveyEngine.findRoomForCoordinate(0.01f, 0.01f))
    }

    @Test
    fun testInterpolateRssiCoincidentPoint() {
        val points = listOf(
            SurveyPoint(
                x = 0.5f,
                y = 0.5f,
                roomName = "Center",
                bssid = "00:11:22:33:44:55",
                ssid = "TestNet",
                rssi = -42,
                frequencyMhz = 5180,
                channel = 36,
                band = FrequencyBand.BAND_5_GHZ
            )
        )

        val interpolated = SiteSurveyEngine.interpolateRssi(0.505f, 0.505f, points)
        // Within 0.02f distance, should return exact -42f
        assertEquals(-42f, interpolated, 0.1f)
    }

    @Test
    fun testInterpolateRssiMidpoint() {
        val points = listOf(
            SurveyPoint(
                x = 0.2f,
                y = 0.5f,
                roomName = "Left",
                bssid = "00:11:22:33:44:55",
                ssid = "TestNet",
                rssi = -40,
                frequencyMhz = 5180,
                channel = 36,
                band = FrequencyBand.BAND_5_GHZ
            ),
            SurveyPoint(
                x = 0.8f,
                y = 0.5f,
                roomName = "Right",
                bssid = "00:11:22:33:44:55",
                ssid = "TestNet",
                rssi = -80,
                frequencyMhz = 5180,
                channel = 36,
                band = FrequencyBand.BAND_5_GHZ
            )
        )

        // Midpoint at x=0.5, y=0.5 equidistant from both points
        val midpointRssi = SiteSurveyEngine.interpolateRssi(0.5f, 0.5f, points)
        // Expected ~ -60 dBm (within +/- 5 dB due to perimeter decay)
        assertTrue("Midpoint RSSI should be between -70 and -55 dBm, got $midpointRssi", midpointRssi in -70f..-55f)
    }

    @Test
    fun testComputeAnalyticsEmpty() {
        val analytics = SiteSurveyEngine.computeAnalytics(emptyList())
        assertEquals(0, analytics.totalPoints)
        assertEquals(0, analytics.coveragePercent)
        assertEquals(0, analytics.deadZonesDetected)
        assertNull(analytics.recommendedMeshOffset)
    }

    @Test
    fun testComputeAnalyticsWithDeadZoneAndRemediation() {
        val points = listOf(
            SurveyPoint(
                x = 0.2f,
                y = 0.2f,
                roomName = "Living Room",
                bssid = "aa:bb:cc:dd:ee:ff",
                ssid = "HomeMesh",
                rssi = -40, // Optimal
                frequencyMhz = 5180,
                channel = 36,
                band = FrequencyBand.BAND_5_GHZ
            ),
            SurveyPoint(
                x = 0.8f,
                y = 0.8f,
                roomName = "Patio / Balcony",
                bssid = "aa:bb:cc:dd:ee:ff",
                ssid = "HomeMesh",
                rssi = -82, // Dead Zone (<= -75)
                frequencyMhz = 2412,
                channel = 1,
                band = FrequencyBand.BAND_2_4_GHZ
            )
        )

        val analytics = SiteSurveyEngine.computeAnalytics(points)
        assertEquals(2, analytics.totalPoints)
        assertEquals(50, analytics.coveragePercent) // 1 out of 2 is >= -65
        assertEquals(1, analytics.deadZonesDetected)
        assertEquals("Living Room", analytics.bestRoom)
        assertEquals(-40, analytics.bestRssi)
        assertEquals("Patio / Balcony", analytics.worstRoom)
        assertEquals(-82, analytics.worstRssi)

        // Remediation mesh offset must be calculated
        assertNotNull(analytics.recommendedMeshOffset)
        val meshOffset = analytics.recommendedMeshOffset!!
        assertTrue("Mesh X should be positioned between 0.2 and 0.8", meshOffset.x in 0.3f..0.7f)
        assertTrue("Mesh Y should be positioned between 0.2 and 0.8", meshOffset.y in 0.3f..0.7f)
        assertNotNull(analytics.recommendedMeshRoom)
        assertTrue(analytics.recommendationSummary.contains("Dead zone detected"))
    }

    @Test
    fun testSimulatedWalkthroughDataset() {
        val points = SiteSurveyEngine.generateSimulatedWalkthrough("Office_Net", "11:22:33:44:55:66")
        assertEquals(5, points.size)
        assertTrue("Contains Living Room", points.any { it.roomName.contains("Living Room") })
        assertTrue("Contains Patio dead zone", points.any { it.rssi <= -75 })

        val analytics = SiteSurveyEngine.computeAnalytics(points)
        assertTrue("Coverage percent is calculated", analytics.coveragePercent in 40..80)
        assertEquals(1, analytics.deadZonesDetected)
    }

    @Test
    fun testGenerateSurveyReportMarkdown() {
        val points = SiteSurveyEngine.generateSimulatedWalkthrough("Office_Net", "11:22:33:44:55:66")
        val analytics = SiteSurveyEngine.computeAnalytics(points)
        val markdown = SiteSurveyEngine.generateSurveyReportMarkdown(points, analytics, "Office_Net")

        assertTrue("Report contains title", markdown.contains("# WaveBalance Wi-Fi Site Survey & Coverage Heatmap Report"))
        assertTrue("Report contains SSID", markdown.contains("`Office_Net`"))
        assertTrue("Report contains inventory table", markdown.contains("| # | Room / Location | Signal (dBm) | Band | Channel | BSSID | Time |"))
        assertTrue("Report mentions dead zones", markdown.contains("Dead Zones"))
        assertTrue("Report contains Living Room", markdown.contains("Living Room"))
    }
}
