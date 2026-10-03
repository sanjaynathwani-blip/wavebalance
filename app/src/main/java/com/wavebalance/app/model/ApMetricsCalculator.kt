package com.wavebalance.app.model

import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

enum class InterferenceSeverity(val label: String, val colorHex: Long) {
    CLEAN("Clean Airspace", 0xFF10B981),      // Emerald
    LOW("Low Contention", 0xFF38BDF8),        // Cyan
    MODERATE("Moderate Contention", 0xFFF59E0B), // Amber
    HIGH("High Interference", 0xFFF97316),    // Orange
    SEVERE("Severe Collision", 0xFFEF4444)    // Red
}

data class InterferenceReport(
    val coChannelCount: Int,
    val adjacentChannelCount: Int,
    val coChannelAps: List<AccessPoint>,
    val adjacentAps: List<AccessPoint>,
    val severity: InterferenceSeverity,
    val summaryText: String,
    val recommendation: String
)

data class FrequencyEnvelope(
    val startMhz: Int,
    val endMhz: Int,
    val centerMhz: Int,
    val channelSpan: String
)

object ApMetricsCalculator {

    /**
     * Compute frequency envelope bounds based on primary frequency and channel bandwidth
     */
    fun getFrequencyEnvelope(centerFrequencyMhz: Int, channelWidth: ChannelWidth): FrequencyEnvelope {
        val widthMhz = channelWidth.mhz
        // Pass the centre of the whole channel (AccessPoint.centerFrequencyMhz), not the
        // primary channel: a 40/80/160 MHz channel extends asymmetrically around its primary.
        val halfWidth = widthMhz / 2
        val startMhz = centerFrequencyMhz - halfWidth
        val endMhz = centerFrequencyMhz + halfWidth
        val centerMhz = (startMhz + endMhz) / 2

        val startCh = FrequencyBand.frequencyToChannel(startMhz)
        val endCh = FrequencyBand.frequencyToChannel(endMhz)

        return FrequencyEnvelope(
            startMhz = startMhz,
            endMhz = endMhz,
            centerMhz = centerMhz,
            channelSpan = "Ch $startCh – Ch $endCh"
        )
    }

    /**
     * Calculate theoretical max PHY link rate in Mbps
     */
    fun calculateTheoreticalMaxPhy(
        standard: WifiStandard,
        channelWidth: ChannelWidth,
        spatialStreams: Int = 2
    ): Int {
        // Base rate per spatial stream at highest MCS
        val baseRatePerStream = when (standard) {
            WifiStandard.WIFI_7 -> when (channelWidth) {
                ChannelWidth.WIDTH_320 -> 2882
                ChannelWidth.WIDTH_160 -> 1441
                ChannelWidth.WIDTH_80 -> 720
                ChannelWidth.WIDTH_40 -> 344
                ChannelWidth.WIDTH_20 -> 172
                else -> 172
            }
            WifiStandard.WIFI_6, WifiStandard.WIFI_6E -> when (channelWidth) {
                ChannelWidth.WIDTH_320, ChannelWidth.WIDTH_160 -> 1201
                ChannelWidth.WIDTH_80 -> 600
                ChannelWidth.WIDTH_40 -> 287
                ChannelWidth.WIDTH_20 -> 143
                else -> 143
            }
            WifiStandard.WIFI_5 -> when (channelWidth) {
                ChannelWidth.WIDTH_320, ChannelWidth.WIDTH_160 -> 867
                ChannelWidth.WIDTH_80 -> 433
                ChannelWidth.WIDTH_40 -> 200
                ChannelWidth.WIDTH_20 -> 86
                else -> 86
            }
            WifiStandard.WIFI_4 -> when (channelWidth) {
                ChannelWidth.WIDTH_320, ChannelWidth.WIDTH_160, ChannelWidth.WIDTH_80, ChannelWidth.WIDTH_40 -> 150
                ChannelWidth.WIDTH_20 -> 72
                else -> 72
            }
            else -> 54
        }
        return baseRatePerStream * spatialStreams
    }

    /**
     * Analyze interference across the detected airspace
     */
    fun analyzeInterference(targetAp: AccessPoint, allAps: List<AccessPoint>): InterferenceReport {
        val targetEnv = getFrequencyEnvelope(targetAp.centerFrequencyMhz, targetAp.channelWidth)
        // The target's own router or mesh (its other SSIDs, bands and nodes) isn't interference
        val sameNetwork = NetworkGroups.group(allAps)[targetAp.bssid.lowercase()].orEmpty() + targetAp.bssid.lowercase()
        val otherAps = allAps.filter { it.bssid.lowercase() !in sameNetwork }

        val coChannel = mutableListOf<AccessPoint>()
        val adjacent = mutableListOf<AccessPoint>()

        for (other in otherAps) {
            if (other.band != targetAp.band) continue

            if (other.channel == targetAp.channel) {
                coChannel.add(other)
            } else {
                val otherEnv = getFrequencyEnvelope(other.centerFrequencyMhz, other.channelWidth)
                // Check if frequency ranges overlap: max(start1, start2) < min(end1, end2)
                val overlaps = maxOf(targetEnv.startMhz, otherEnv.startMhz) < minOf(targetEnv.endMhz, otherEnv.endMhz)
                if (overlaps) {
                    adjacent.add(other)
                }
            }
        }

        // Determine severity based on signal power of interfering nodes
        val strongestCoRssi = coChannel.maxOfOrNull { it.rssi } ?: -120
        val strongestAdjRssi = adjacent.maxOfOrNull { it.rssi } ?: -120

        val (severity, summary, recommendation) = when {
            coChannel.isEmpty() && adjacent.isEmpty() -> Triple(
                InterferenceSeverity.CLEAN,
                "Dedicated spectrum channel with zero competing airwaves.",
                "Optimal channel placement. No changes required."
            )
            strongestCoRssi >= -68 -> Triple(
                InterferenceSeverity.SEVERE,
                "Severe co-channel conflict with ${coChannel.size} nearby AP(s) sharing Channel ${targetAp.channel} at high signal.",
                "Co-channel interference forces 802.11 CSMA/CA airtime sharing. Reassign router primary channel to clear spectrum in Phase 6."
            )
            coChannel.isNotEmpty() -> Triple(
                InterferenceSeverity.HIGH,
                "${coChannel.size} neighboring AP(s) on Channel ${targetAp.channel} creating frame contention.",
                "Consider migrating to an unoccupied DFS or high 5 GHz channel."
            )
            strongestAdjRssi >= -72 || adjacent.size >= 2 -> Triple(
                InterferenceSeverity.MODERATE,
                "${adjacent.size} overlapping wide-band AP(s) encroaching on this envelope.",
                "Adjacent channel bleed can introduce spectral noise floor elevation. Narrowing bandwidth or shifting channels may stabilize jitter."
            )
            else -> Triple(
                InterferenceSeverity.LOW,
                "Faint neighboring overlap (${adjacent.size} AP) with minimal packet degradation.",
                "Signal overlap is below interference threshold. Safe for general operations."
            )
        }

        return InterferenceReport(
            coChannelCount = coChannel.size,
            adjacentChannelCount = adjacent.size,
            coChannelAps = coChannel,
            adjacentAps = adjacent,
            severity = severity,
            summaryText = summary,
            recommendation = recommendation
        )
    }

    /**
     * Computes statistics from rolling RSSI samples: min, max, avg, jitter (standard deviation)
     */
    data class RssiStats(
        val current: Int,
        val min: Int,
        val max: Int,
        val avg: Int,
        val jitter: Double
    )

    fun computeRssiStats(samples: List<RssiSample>, fallbackRssi: Int): RssiStats {
        if (samples.isEmpty()) {
            return RssiStats(
                current = fallbackRssi,
                min = fallbackRssi,
                max = fallbackRssi,
                avg = fallbackRssi,
                jitter = 0.0
            )
        }

        val rssiValues = samples.map { it.rssi }
        val current = rssiValues.last()
        val min = rssiValues.minOrNull() ?: fallbackRssi
        val max = rssiValues.maxOrNull() ?: fallbackRssi
        val avg = rssiValues.average().roundToInt()

        // Standard deviation (jitter)
        val variance = rssiValues.map { (it - avg).toDouble().pow(2.0) }.average()
        val jitter = sqrt(variance)

        return RssiStats(
            current = current,
            min = min,
            max = max,
            avg = avg,
            jitter = (jitter * 10).roundToInt() / 10.0
        )
    }
}
