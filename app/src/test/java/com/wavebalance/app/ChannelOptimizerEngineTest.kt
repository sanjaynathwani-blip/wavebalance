package com.wavebalance.app

import com.wavebalance.app.model.AccessPoint
import com.wavebalance.app.model.ChannelOptimizerEngine
import com.wavebalance.app.model.ChannelRating
import com.wavebalance.app.model.ChannelWidth
import com.wavebalance.app.model.FrequencyBand
import com.wavebalance.app.model.WifiStandard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChannelOptimizerEngineTest {

    private val testApCurrent = AccessPoint(
        bssid = "aa:bb:cc:dd:ee:01",
        ssid = "MyHome_5G",
        rssi = -50,
        frequencyMhz = 5240, // Ch 48
        channel = 48,
        channelWidth = ChannelWidth.WIDTH_80,
        band = FrequencyBand.BAND_5_GHZ,
        standard = WifiStandard.WIFI_6,
        capabilities = "[WPA2-PSK-CCMP][RSN-PSK-CCMP]",
        isConnected = true
    )

    @Test
    fun testEmptyAirspace_returnsOptimalScore() {
        val recommendation = ChannelOptimizerEngine.evaluateBand(
            band = FrequencyBand.BAND_5_GHZ,
            allAps = listOf(testApCurrent),
            currentChannel = 48,
            targetWidth = ChannelWidth.WIDTH_80
        )

        assertNotNull(recommendation)
        assertTrue(recommendation.recommendedScore >= 90)
    }

    @Test
    fun testCoChannelInterference_recommendsAlternativePristineChannel() {
        // Congest Channel 48 with strong competing neighbor
        val interferingAp = AccessPoint(
            bssid = "aa:bb:cc:dd:ee:02",
            ssid = "Neighbor_Strong",
            rssi = -55,
            frequencyMhz = 5240, // Co-channel Ch 48
            channel = 48,
            channelWidth = ChannelWidth.WIDTH_80,
            band = FrequencyBand.BAND_5_GHZ,
            standard = WifiStandard.WIFI_6,
            capabilities = "[WPA3-SAE]",
            isConnected = false
        )

        val recommendation = ChannelOptimizerEngine.evaluateBand(
            band = FrequencyBand.BAND_5_GHZ,
            allAps = listOf(testApCurrent, interferingAp),
            currentChannel = 48,
            targetWidth = ChannelWidth.WIDTH_80
        )

        // Recommended channel should move away from Ch 48 to clean UNII-3 or UNII-1
        assertTrue("Recommended channel should not be 48", recommendation.recommendedChannel != 48)
        assertTrue("Recommended score should be higher than current score", recommendation.recommendedScore > recommendation.currentScore)
        assertTrue("Headroom gain should be positive", recommendation.scoreDelta > 0)
    }

    @Test
    fun testRouterDirectives_containsRecommendedChannel() {
        val recommendation = ChannelOptimizerEngine.evaluateBand(
            band = FrequencyBand.BAND_5_GHZ,
            allAps = listOf(testApCurrent),
            currentChannel = 48,
            targetWidth = ChannelWidth.WIDTH_80
        )

        val directives = recommendation.routerDirectivesText
        assertTrue(directives.contains("WAVEBALANCE WI-FI OPTIMIZATION DIRECTIVES"))
        assertTrue(directives.contains("RECOMMENDED CHANNEL: Ch ${recommendation.recommendedChannel}"))
        assertTrue(directives.contains("Target Radio: 5 GHz"))
    }

    @Test
    fun testBandwidth24Ghz_evaluatesStandardChannels() {
        val ap24 = AccessPoint(
            bssid = "aa:bb:cc:dd:ee:03",
            ssid = "Home_2.4G",
            rssi = -60,
            frequencyMhz = 2437, // Ch 6
            channel = 6,
            channelWidth = ChannelWidth.WIDTH_20,
            band = FrequencyBand.BAND_2_4_GHZ,
            standard = WifiStandard.WIFI_4,
            capabilities = "[WPA2-PSK]",
            isConnected = true
        )

        val recommendation = ChannelOptimizerEngine.evaluateBand(
            band = FrequencyBand.BAND_2_4_GHZ,
            allAps = listOf(ap24),
            currentChannel = 6,
            targetWidth = ChannelWidth.WIDTH_20
        )

        assertTrue(listOf(1, 6, 11).contains(recommendation.recommendedChannel))
    }
}
