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
    val ipAddress: String = ""
) {
    val snr: Int
        get() = (rssi - (-95)).coerceAtLeast(0)

    val cleanSsid: String
        get() = ssid.removeSurrounding("\"")
}
