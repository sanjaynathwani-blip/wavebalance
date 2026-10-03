package com.wavebalance.app.data

import com.wavebalance.app.model.AccessPoint
import com.wavebalance.app.model.ActiveConnectionInfo
import com.wavebalance.app.model.ChannelWidth
import com.wavebalance.app.model.FrequencyBand
import com.wavebalance.app.model.WifiStandard

object MockWifiDataProvider {

    fun getMockActiveConnection(): ActiveConnectionInfo {
        return ActiveConnectionInfo(
            ssid = "WaveBalance_HQ",
            bssid = "00:1A:2B:3C:4D:5E",
            rssi = -52,
            frequencyMhz = 5240, // Channel 48
            linkSpeedMbps = 1201,
            rxLinkSpeedMbps = 1201,
            txLinkSpeedMbps = 960,
            channel = 48,
            band = FrequencyBand.BAND_5_GHZ,
            standard = WifiStandard.WIFI_6,
            ipAddress = "192.168.1.145"
        )
    }

    fun getMockAccessPoints(): List<AccessPoint> {
        return listOf(
            AccessPoint(
                bssid = "00:1A:2B:3C:4D:5E",
                ssid = "WaveBalance_HQ",
                rssi = -52,
                frequencyMhz = 5240,
                channel = 48,
                band = FrequencyBand.BAND_5_GHZ,
                standard = WifiStandard.WIFI_6,
                channelWidth = ChannelWidth.WIDTH_80,
                capabilities = "[WPA3-SAE-CCMP][RSN-SAE-CCMP][ESS]",
                isConnected = true,
                isUserTaggedHome = true
            ),
            AccessPoint(
                bssid = "00:1A:2B:3C:4D:5F",
                ssid = "WaveBalance_HQ_IoT",
                rssi = -55,
                frequencyMhz = 2437,
                channel = 6,
                band = FrequencyBand.BAND_2_4_GHZ,
                standard = WifiStandard.WIFI_4,
                channelWidth = ChannelWidth.WIDTH_20,
                capabilities = "[WPA2-PSK-CCMP][ESS]",
                isConnected = false,
                isUserTaggedHome = true
            ),
            AccessPoint(
                bssid = "00:1A:2B:3C:4D:60",
                ssid = "WaveBalance_HQ_Ultra6G",
                rssi = -58,
                frequencyMhz = 6175,
                channel = 45,
                band = FrequencyBand.BAND_6_GHZ,
                standard = WifiStandard.WIFI_6E,
                channelWidth = ChannelWidth.WIDTH_160,
                capabilities = "[WPA3-SAE-CCMP][ESS]",
                isConnected = false,
                isUserTaggedHome = true
            ),
            AccessPoint(
                bssid = "70:3A:0E:11:22:33",
                ssid = "Pixel_Pro_Beast7",
                rssi = -61,
                frequencyMhz = 6495,
                channel = 109,
                band = FrequencyBand.BAND_6_GHZ,
                standard = WifiStandard.WIFI_7,
                channelWidth = ChannelWidth.WIDTH_320,
                capabilities = "[WPA3-SAE-CCMP][ESS]",
                isConnected = false,
                isUserTaggedHome = false
            ),
            AccessPoint(
                bssid = "C4:41:1E:AA:BB:CC",
                ssid = "Neighbor_Net_5G",
                rssi = -68,
                frequencyMhz = 5200,
                channel = 40,
                band = FrequencyBand.BAND_5_GHZ,
                standard = WifiStandard.WIFI_5,
                channelWidth = ChannelWidth.WIDTH_80,
                capabilities = "[WPA2-PSK-CCMP][ESS]",
                isConnected = false,
                isUserTaggedHome = false
            ),
            AccessPoint(
                bssid = "C4:41:1E:AA:BB:CD",
                ssid = "Neighbor_Net_2.4G",
                rssi = -64,
                frequencyMhz = 2412,
                channel = 1,
                band = FrequencyBand.BAND_2_4_GHZ,
                standard = WifiStandard.WIFI_4,
                channelWidth = ChannelWidth.WIDTH_20,
                capabilities = "[WPA2-PSK-CCMP][ESS]",
                isConnected = false,
                isUserTaggedHome = false
            ),
            AccessPoint(
                bssid = "A0:04:60:DE:AD:01",
                ssid = "Apartment_3B_Fast",
                rssi = -74,
                frequencyMhz = 2437,
                channel = 6,
                band = FrequencyBand.BAND_2_4_GHZ,
                standard = WifiStandard.WIFI_6,
                channelWidth = ChannelWidth.WIDTH_40,
                capabilities = "[WPA2-PSK-CCMP][WPA3-SAE][ESS]",
                isConnected = false,
                isUserTaggedHome = false
            ),
            AccessPoint(
                bssid = "A0:04:60:DE:AD:02",
                ssid = "Apartment_3B_DFS",
                rssi = -78,
                frequencyMhz = 5500,
                channel = 100,
                band = FrequencyBand.BAND_5_GHZ,
                standard = WifiStandard.WIFI_6,
                channelWidth = ChannelWidth.WIDTH_160,
                capabilities = "[WPA2-PSK-CCMP][WPA3-SAE][ESS]",
                isConnected = false,
                isUserTaggedHome = false
            ),
            AccessPoint(
                bssid = "88:DE:A9:77:88:99",
                ssid = "xfinitywifi",
                rssi = -84,
                frequencyMhz = 2462,
                channel = 11,
                band = FrequencyBand.BAND_2_4_GHZ,
                standard = WifiStandard.LEGACY,
                channelWidth = ChannelWidth.WIDTH_20,
                capabilities = "[ESS]",
                isConnected = false,
                isUserTaggedHome = false
            )
        )
    }
}
