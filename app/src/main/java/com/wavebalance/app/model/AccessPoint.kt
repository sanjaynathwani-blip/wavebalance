package com.wavebalance.app.model

data class AccessPoint(
    val bssid: String,
    val ssid: String,
    val rssi: Int,
    val frequencyMhz: Int,
    val channel: Int = FrequencyBand.frequencyToChannel(frequencyMhz),
    val band: FrequencyBand = FrequencyBand.fromFrequency(frequencyMhz),
    val standard: WifiStandard = WifiStandard.UNKNOWN,
    val channelWidth: ChannelWidth = ChannelWidth.WIDTH_20,
    val capabilities: String = "",
    val securityType: String = parseSecurity(capabilities),
    val isConnected: Boolean = false,
    val isUserTaggedHome: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
) {
    val displayName: String
        get() = if (ssid.isBlank() || ssid == "<unknown ssid>") "Hidden Network ($bssid)" else ssid

    /**
     * Estimated SNR in dB using standard terrestrial noise floor (-95 dBm)
     */
    val snr: Int
        get() = (rssi - (-95)).coerceAtLeast(0)

    /**
     * Normalized 0..100% signal rating
     */
    val signalPercent: Int
        get() {
            // Typical Wi-Fi RSSI bounds: -100 dBm (0%) to -50 dBm (100%)
            return when {
                rssi >= -50 -> 100
                rssi <= -100 -> 0
                else -> 2 * (rssi + 100)
            }.coerceIn(0, 100)
        }

    companion object {
        fun parseSecurity(caps: String): String {
            return when {
                caps.contains("WPA3-SAE") || caps.contains("SAE") -> "WPA3 Personal"
                caps.contains("WPA3") && caps.contains("Enterprise") -> "WPA3 Enterprise"
                caps.contains("WPA2") && caps.contains("WPA-") -> "WPA2/WPA3"
                caps.contains("WPA2-PSK") || caps.contains("WPA2") -> "WPA2 Personal"
                caps.contains("WPA-PSK") -> "WPA Personal"
                caps.contains("WEP") -> "WEP"
                caps.contains("OWE") -> "OWE (Enhanced Open)"
                else -> "Open"
            }
        }
    }
}
