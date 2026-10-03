package com.wavebalance.app

import com.wavebalance.app.model.AccessPoint
import com.wavebalance.app.model.ApMetricsCalculator
import com.wavebalance.app.model.ChannelWidth
import com.wavebalance.app.model.FrequencyBand
import com.wavebalance.app.model.InterferenceSeverity
import com.wavebalance.app.model.WifiStandard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ApMetricsCalculatorTest {

    @Test
    fun testFrequencyEnvelope_5Ghz80Mhz() {
        val envelope = ApMetricsCalculator.getFrequencyEnvelope(5240, ChannelWidth.WIDTH_80)
        assertEquals(5200, envelope.startMhz)
        assertEquals(5280, envelope.endMhz)
        assertEquals(5240, envelope.centerMhz)
    }

    @Test
    fun testCalculateTheoreticalMaxPhy_Wifi6_80Mhz_2x2() {
        // Wi-Fi 6, 80 MHz, 2 streams = 600 * 2 = 1200 Mbps
        val speed = ApMetricsCalculator.calculateTheoreticalMaxPhy(
            standard = WifiStandard.WIFI_6,
            channelWidth = ChannelWidth.WIDTH_80,
            spatialStreams = 2
        )
        assertEquals(1200, speed)
    }

    @Test
    fun testCalculateTheoreticalMaxPhy_Wifi7_320Mhz_2x2() {
        // Wi-Fi 7, 320 MHz, 2 streams = 2882 * 2 = 5764 Mbps
        val speed = ApMetricsCalculator.calculateTheoreticalMaxPhy(
            standard = WifiStandard.WIFI_7,
            channelWidth = ChannelWidth.WIDTH_320,
            spatialStreams = 2
        )
        assertEquals(5764, speed)
    }

    @Test
    fun testInterferenceAnalysis_detectsCoChannelCollision() {
        val target = AccessPoint(
            bssid = "00:11:22:33:44:55",
            ssid = "Main_AP",
            rssi = -60,
            frequencyMhz = 5180,
            channel = 36,
            channelWidth = ChannelWidth.WIDTH_80,
            band = FrequencyBand.BAND_5_GHZ,
            standard = WifiStandard.WIFI_6,
            capabilities = "[WPA2-PSK]",
            isConnected = true
        )

        val competitor = AccessPoint(
            bssid = "00:11:22:33:44:66",
            ssid = "Rival_AP",
            rssi = -65,
            frequencyMhz = 5180,
            channel = 36,
            channelWidth = ChannelWidth.WIDTH_80,
            band = FrequencyBand.BAND_5_GHZ,
            standard = WifiStandard.WIFI_6,
            capabilities = "[WPA2-PSK]",
            isConnected = false
        )

        val report = ApMetricsCalculator.analyzeInterference(target, listOf(target, competitor))
        assertEquals(1, report.coChannelCount)
        assertTrue(report.severity == InterferenceSeverity.HIGH || report.severity == InterferenceSeverity.SEVERE)
    }

    @Test
    fun testCleanAirspace_returnsCleanSeverity() {
        val target = AccessPoint(
            bssid = "00:11:22:33:44:55",
            ssid = "Main_AP",
            rssi = -60,
            frequencyMhz = 5745,
            channel = 149,
            channelWidth = ChannelWidth.WIDTH_80,
            band = FrequencyBand.BAND_5_GHZ,
            standard = WifiStandard.WIFI_6,
            capabilities = "[WPA2-PSK]",
            isConnected = true
        )

        val report = ApMetricsCalculator.analyzeInterference(target, listOf(target))
        assertEquals(0, report.coChannelCount)
        assertEquals(0, report.adjacentChannelCount)
        assertEquals(InterferenceSeverity.CLEAN, report.severity)
    }
}
