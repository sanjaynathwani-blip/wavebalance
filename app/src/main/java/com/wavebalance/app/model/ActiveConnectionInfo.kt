package com.wavebalance.app.model

data class ActiveConnectionInfo(
    val ssid: String,
    val bssid: String,
    val rssi: Int,
    val frequencyMhz: Int,
    val linkSpeedMbps: Int,
    val rxLinkSpeedMbps: Int = -1,
    val txLinkSpeedMbps: Int = -1,
    val channel: Int = FrequencyBand.frequencyToChannel(frequencyMhz),
    val band: FrequencyBand = FrequencyBand.fromFrequency(frequencyMhz),
    val standard: WifiStandard = WifiStandard.UNKNOWN,
    val ipAddress: String = "",
    // UNKNOWN when the connected AP isn't in the latest scan results
    val channelWidth: ChannelWidth = ChannelWidth.UNKNOWN,
    // Highest link rates both the device and the AP support (Android 11+), -1 if unknown
    val maxSupportedTxLinkSpeedMbps: Int = -1,
    val maxSupportedRxLinkSpeedMbps: Int = -1
) {
    val cleanSsid: String
        get() = ssid.removeSurrounding("\"")
}
