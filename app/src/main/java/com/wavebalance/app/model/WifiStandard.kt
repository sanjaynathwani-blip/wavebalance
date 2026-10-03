package com.wavebalance.app.model

enum class WifiStandard(val label: String, val generation: String) {
    WIFI_7("802.11be", "Wi-Fi 7"),
    WIFI_6E("802.11ax 6GHz", "Wi-Fi 6E"),
    WIFI_6("802.11ax", "Wi-Fi 6"),
    WIFI_5("802.11ac", "Wi-Fi 5"),
    WIFI_4("802.11n", "Wi-Fi 4"),
    LEGACY("802.11a/b/g", "Legacy"),
    UNKNOWN("Unknown", "Wi-Fi");

    companion object {
        fun fromScanResultStandard(standardInt: Int, freqMhz: Int): WifiStandard {
            return when (standardInt) {
                8 -> WIFI_7 // ScanResult.WIFI_STANDARD_11BE
                6 -> { // ScanResult.WIFI_STANDARD_11AX
                    if (freqMhz in 5925..7125) WIFI_6E else WIFI_6
                }
                5 -> WIFI_5 // ScanResult.WIFI_STANDARD_11AC
                4 -> WIFI_4 // ScanResult.WIFI_STANDARD_11N
                1 -> LEGACY // ScanResult.WIFI_STANDARD_LEGACY
                else -> {
                    // Fallback infer from frequency or capabilities
                    if (freqMhz in 5925..7125) WIFI_6E
                    else if (freqMhz in 5150..5895) WIFI_5
                    else WIFI_4
                }
            }
        }
    }
}
