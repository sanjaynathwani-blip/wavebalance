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
    val channelWidth: ChannelWidth = ChannelWidth.UNKNOWN
) {
    val snr: Int
        get() = (rssi - (-95)).coerceAtLeast(0)

    val cleanSsid: String
        get() = ssid.removeSurrounding("\"")

    val effectiveChannelWidth: ChannelWidth
        get() = if (channelWidth != ChannelWidth.UNKNOWN) {
            channelWidth
        } else when (band) {
            FrequencyBand.BAND_6_GHZ -> ChannelWidth.WIDTH_160
            FrequencyBand.BAND_5_GHZ -> ChannelWidth.WIDTH_80
            FrequencyBand.BAND_2_4_GHZ -> ChannelWidth.WIDTH_20
            else -> ChannelWidth.WIDTH_20
        }
}
