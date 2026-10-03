package com.wavebalance.app.model

enum class ChannelWidth(val mhz: Int, val label: String) {
    WIDTH_20(20, "20 MHz"),
    WIDTH_40(40, "40 MHz"),
    WIDTH_80(80, "80 MHz"),
    WIDTH_160(160, "160 MHz"),
    WIDTH_320(320, "320 MHz"),
    UNKNOWN(20, "20 MHz");

    companion object {
        fun fromScanResult(widthInt: Int): ChannelWidth {
            return when (widthInt) {
                0 -> WIDTH_20
                1 -> WIDTH_40
                2 -> WIDTH_80
                3 -> WIDTH_160
                4 -> WIDTH_80 // 80+80
                5 -> WIDTH_320
                else -> WIDTH_20
            }
        }
    }
}
