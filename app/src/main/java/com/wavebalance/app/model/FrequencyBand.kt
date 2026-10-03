package com.wavebalance.app.model

enum class FrequencyBand(val label: String, val minFreqMhz: Int, val maxFreqMhz: Int) {
    BAND_2_4_GHZ("2.4 GHz", 2400, 2495),
    BAND_5_GHZ("5 GHz", 5150, 5895),
    BAND_6_GHZ("6 GHz", 5925, 7125),
    UNKNOWN("Unknown", 0, 0);

    companion object {
        fun fromFrequency(freqMhz: Int): FrequencyBand {
            return when {
                freqMhz in 2400..2495 -> BAND_2_4_GHZ
                freqMhz in 5150..5895 -> BAND_5_GHZ
                freqMhz in 5925..7125 -> BAND_6_GHZ
                else -> UNKNOWN
            }
        }

        fun frequencyToChannel(freqMhz: Int): Int {
            return when {
                freqMhz == 2484 -> 14
                freqMhz in 2412..2472 -> (freqMhz - 2407) / 5
                freqMhz in 5170..5825 -> (freqMhz - 5000) / 5
                freqMhz in 5835..5895 -> (freqMhz - 5000) / 5
                // 6 GHz (Wi-Fi 6E/7): channels 1 to 233, stepped by 5 MHz from 5950 MHz (channel 1 = 5955 MHz)
                freqMhz in 5945..7125 -> (freqMhz - 5950) / 5
                else -> 0
            }
        }

        fun channelToFrequency(channel: Int, band: FrequencyBand): Int {
            return when (band) {
                BAND_2_4_GHZ -> if (channel == 14) 2484 else 2407 + (channel * 5)
                BAND_5_GHZ -> 5000 + (channel * 5)
                BAND_6_GHZ -> 5950 + (channel * 5)
                UNKNOWN -> 5000 + (channel * 5)
            }
        }
    }
}
