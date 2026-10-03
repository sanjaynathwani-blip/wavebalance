package com.wavebalance.app.model

/**
 * One information element from an access point's beacon or probe response,
 * as Android reports it in ScanResult.getInformationElements() (API 30+).
 * For extension elements (id 255), [idExt] is the extension id and [bytes]
 * starts after it.
 */
class RawInformationElement(val id: Int, val idExt: Int, val bytes: ByteArray)

enum class PmfMode(val label: String) {
    REQUIRED("Required"),
    CAPABLE("Supported (optional)"),
    DISABLED("Not supported")
}

/**
 * Capabilities an access point advertises in its information elements.
 * Every field is null when the AP doesn't advertise it.
 */
data class ApCapabilities(
    // Highest number of receive spatial streams across HT, VHT and HE capabilities
    val maxSpatialStreams: Int? = null,
    // Protected Management Frames (802.11w), from the RSN capabilities field
    val pmf: PmfMode? = null,
    // 802.11k: Radio Measurement (RM Enabled Capabilities element present)
    val radioMeasurement: Boolean = false,
    // 802.11v: BSS Transition Management (Extended Capabilities bit 19)
    val bssTransition: Boolean = false,
    // 802.11r: Fast BSS Transition (Mobility Domain element present)
    val fastTransition: Boolean = false
)

object InformationElements {

    private const val EID_HT_CAPABILITIES = 45
    private const val EID_RSN = 48
    private const val EID_MOBILITY_DOMAIN = 54
    private const val EID_RM_ENABLED_CAPABILITIES = 70
    private const val EID_EXTENDED_CAPABILITIES = 127
    private const val EID_VHT_CAPABILITIES = 191
    private const val EID_EXTENSION = 255
    private const val EID_EXT_HE_CAPABILITIES = 35

    fun parse(elements: List<RawInformationElement>): ApCapabilities {
        var maxStreams: Int? = null
        var pmf: PmfMode? = null
        var radioMeasurement = false
        var bssTransition = false
        var fastTransition = false

        fun offerStreams(count: Int?) {
            if (count != null && count > 0) maxStreams = maxOf(maxStreams ?: 0, count)
        }

        for (ie in elements) {
            when {
                ie.id == EID_HT_CAPABILITIES -> offerStreams(htStreams(ie.bytes))
                ie.id == EID_VHT_CAPABILITIES -> offerStreams(vhtStreams(ie.bytes))
                ie.id == EID_EXTENSION && ie.idExt == EID_EXT_HE_CAPABILITIES -> offerStreams(heStreams(ie.bytes))
                ie.id == EID_RSN -> pmf = rsnPmf(ie.bytes) ?: pmf
                ie.id == EID_RM_ENABLED_CAPABILITIES -> radioMeasurement = true
                ie.id == EID_MOBILITY_DOMAIN -> fastTransition = true
                ie.id == EID_EXTENDED_CAPABILITIES -> bssTransition = bit(ie.bytes, 19)
            }
        }

        return ApCapabilities(
            maxSpatialStreams = maxStreams,
            pmf = pmf,
            radioMeasurement = radioMeasurement,
            bssTransition = bssTransition,
            fastTransition = fastTransition
        )
    }

    /**
     * HT Capabilities: 2 bytes capability info, 1 byte A-MPDU parameters, then the
     * Supported MCS Set whose first 4 bytes are the Rx MCS bitmask for streams 1-4.
     */
    internal fun htStreams(bytes: ByteArray): Int? {
        if (bytes.size < 7) return null
        var streams = 0
        for (i in 0 until 4) {
            if (bytes[3 + i].toInt() and 0xFF != 0) streams = i + 1
        }
        return streams.takeIf { it > 0 }
    }

    /**
     * VHT Capabilities: 4 bytes capability info, then the Rx VHT-MCS map
     * (2 bits per stream for streams 1-8, value 3 = not supported).
     */
    internal fun vhtStreams(bytes: ByteArray): Int? {
        if (bytes.size < 6) return null
        return mcsMapStreams(bytes[4], bytes[5])
    }

    /**
     * HE Capabilities (after the extension id): 6 bytes HE MAC capabilities,
     * 11 bytes HE PHY capabilities, then the Rx HE-MCS map for <= 80 MHz.
     */
    internal fun heStreams(bytes: ByteArray): Int? {
        if (bytes.size < 19) return null
        return mcsMapStreams(bytes[17], bytes[18])
    }

    private fun mcsMapStreams(low: Byte, high: Byte): Int? {
        val map = (low.toInt() and 0xFF) or ((high.toInt() and 0xFF) shl 8)
        var streams = 0
        for (ss in 0 until 8) {
            if ((map shr (ss * 2)) and 0b11 != 0b11) streams = ss + 1
        }
        return streams.takeIf { it > 0 }
    }

    /**
     * RSN element: version (2), group cipher (4), pairwise count (2) + suites (4 each),
     * AKM count (2) + suites (4 each), then RSN capabilities (2) where
     * bit 6 = MFP required and bit 7 = MFP capable.
     */
    internal fun rsnPmf(bytes: ByteArray): PmfMode? {
        var offset = 2 + 4
        if (bytes.size < offset + 2) return null
        val pairwiseCount = u16(bytes, offset)
        offset += 2 + pairwiseCount * 4
        if (bytes.size < offset + 2) return null
        val akmCount = u16(bytes, offset)
        offset += 2 + akmCount * 4
        if (bytes.size < offset + 2) return PmfMode.DISABLED
        val capabilities = u16(bytes, offset)
        return when {
            capabilities and (1 shl 6) != 0 -> PmfMode.REQUIRED
            capabilities and (1 shl 7) != 0 -> PmfMode.CAPABLE
            else -> PmfMode.DISABLED
        }
    }

    private fun u16(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xFF) or ((bytes[offset + 1].toInt() and 0xFF) shl 8)

    private fun bit(bytes: ByteArray, index: Int): Boolean {
        val byteIndex = index / 8
        if (byteIndex >= bytes.size) return false
        return (bytes[byteIndex].toInt() shr (index % 8)) and 1 == 1
    }
}
