package com.wavebalance.app

import com.wavebalance.app.model.AccessPoint
import com.wavebalance.app.model.ActiveConnectionInfo
import com.wavebalance.app.model.ChannelWidth
import com.wavebalance.app.model.FrequencyBand
import com.wavebalance.app.model.OptimizerRecommendation
import com.wavebalance.app.model.RfAuditReportGenerator
import com.wavebalance.app.model.WifiStandard
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RfAuditReportGeneratorTest {

    private val sampleActiveConn = ActiveConnectionInfo(
        ssid = "\"WaveBalance_HQ\"",
        bssid = "00:1A:2B:3C:4D:5E",
        rssi = -54,
        frequencyMhz = 5240,
        linkSpeedMbps = 866,
        standard = WifiStandard.WIFI_6,
        channel = 48,
        band = FrequencyBand.BAND_5_GHZ
    )

    private val sampleAps = listOf(
        AccessPoint(
            bssid = "00:1A:2B:3C:4D:5E",
            ssid = "WaveBalance_HQ",
            rssi = -54,
            frequencyMhz = 5240,
            channel = 48,
            band = FrequencyBand.BAND_5_GHZ,
            standard = WifiStandard.WIFI_6,
            channelWidth = ChannelWidth.WIDTH_80,
            capabilities = "[WPA3-SAE-CCMP]",
            isConnected = true
        ),
        AccessPoint(
            bssid = "00:1A:2B:3C:4D:5F",
            ssid = "WaveBalance_IoT",
            rssi = -60,
            frequencyMhz = 2412,
            channel = 1,
            band = FrequencyBand.BAND_2_4_GHZ,
            standard = WifiStandard.WIFI_4,
            channelWidth = ChannelWidth.WIDTH_20,
            capabilities = "[WPA2-PSK-CCMP]",
            isConnected = false
        ),
        AccessPoint(
            bssid = "00:1A:2B:3C:4D:60",
            ssid = "WaveBalance_Ultra6G",
            rssi = -58,
            frequencyMhz = 6175,
            channel = 45,
            band = FrequencyBand.BAND_6_GHZ,
            standard = WifiStandard.WIFI_6E,
            channelWidth = ChannelWidth.WIDTH_160,
            capabilities = "[WPA3-SAE-CCMP]",
            isConnected = false
        )
    )

    private val sampleRecommendation = OptimizerRecommendation(
        band = FrequencyBand.BAND_5_GHZ,
        currentChannel = 48,
        recommendedChannel = 149,
        recommendedBandwidth = ChannelWidth.WIDTH_80,
        currentScore = 65,
        recommendedScore = 95,
        scoreDelta = 30,
        eliminatedCollisions = 2,
        channelScores = emptyList(),
        ownRadiosIgnored = 0,
        stepByStepGuide = emptyList(),
        reasonSummary = "Switch to UNII-3 channel 149 to eliminate UNII-1 contention.",
        routerDirectivesText = "1. Navigate to 5 GHz Wireless Settings\n2. Set Control Channel to 149\n3. Set Channel Width to 80 MHz"
    )

    @Test
    fun testGenerateMarkdownReportComplete() {
        val report = RfAuditReportGenerator.generateMarkdownReport(
            activeConnection = sampleActiveConn,
            allAps = sampleAps,
            recommendation = sampleRecommendation
        )

        assertNotNull(report)
        assertTrue(report.contains("# 📡 WaveBalance RF Airspace Audit Report"))
        assertTrue(report.contains("## 1. Active Wireless Link Telemetry"))
        assertTrue(report.contains("`WaveBalance_HQ`"))
        assertTrue(report.contains("00:1A:2B:3C:4D:5E"))
        assertTrue(report.contains("-54 dBm"))
        assertTrue(report.contains("866 Mbps"))
        assertTrue(report.contains("Wi-Fi 6 (802.11ax)"))

        // Density Section
        assertTrue(report.contains("## 2. Spectrum Density & Congestion Overview"))
        assertTrue(report.contains("3 Access Points"))
        assertTrue(report.contains("2.4 GHz Band**: 1 APs"))
        assertTrue(report.contains("5 GHz Band**: 1 APs"))
        assertTrue(report.contains("6 GHz Band**: 1 APs"))

        // Recommendation Section
        assertTrue(report.contains("## 3. Channel Optimizer & Headroom Analysis"))
        assertTrue(report.contains("Channel 149"))
        assertTrue(report.contains("+30 pts"))
        assertTrue(report.contains("Set Control Channel to 149"))

        // Inventory Table
        assertTrue(report.contains("## 4. Detected Access Point Inventory"))
        assertTrue(report.contains("| `WaveBalance_HQ` *(Connected)* | 48 | 5 GHz | -54 dBm |"))
        assertTrue(report.contains("| `WaveBalance_IoT` | 1 | 2.4 GHz | -60 dBm |"))
        assertTrue(report.contains("| `WaveBalance_Ultra6G` | 45 | 6 GHz | -58 dBm |"))
    }

    @Test
    fun testGenerateMarkdownReportDisconnectedState() {
        val report = RfAuditReportGenerator.generateMarkdownReport(
            activeConnection = null,
            allAps = emptyList(),
            recommendation = null
        )

        assertNotNull(report)
        assertTrue(report.contains("Disconnected"))
        assertTrue(report.contains("No active connection"))
        assertTrue(report.contains("A total of **0 Access Points** were detected"))
        assertTrue(report.contains("No optimization recommendation available."))
    }
}
