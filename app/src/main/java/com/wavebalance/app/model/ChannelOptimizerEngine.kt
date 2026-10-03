package com.wavebalance.app.model

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class ChannelRating(val label: String, val colorHex: Long) {
    OPTIMAL("Optimal", 0xFF10B981),      // Emerald Green
    GOOD("Good", 0xFF38BDF8),            // Cyan
    FAIR("Fair", 0xFFF59E0B),            // Amber
    CONGESTED("Congested", 0xFFEF4444)   // Red
}

data class RouterStep(
    val stepNumber: Int,
    val title: String,
    val description: String,
    val parameterHighlight: String? = null
)

data class ChannelScore(
    val channel: Int,
    val frequencyMhz: Int,
    val band: FrequencyBand,
    val isDfs: Boolean,
    val score: Int, // 0 to 100
    val rating: ChannelRating,
    val coChannelCount: Int,
    val adjacentChannelCount: Int,
    val maxInterferingRssi: Int?,
    val conflictingSsids: List<String>,
    val recommendedWidth: ChannelWidth,
    val isCurrentChannel: Boolean = false,
    val isRecommended: Boolean = false
)

data class OptimizerRecommendation(
    val band: FrequencyBand,
    val currentChannel: Int,
    val currentScore: Int,
    val recommendedChannel: Int,
    val recommendedScore: Int,
    val recommendedBandwidth: ChannelWidth,
    val scoreDelta: Int,
    val eliminatedCollisions: Int,
    val channelScores: List<ChannelScore>,
    val stepByStepGuide: List<RouterStep>,
    val routerDirectivesText: String,
    val reasonSummary: String
)

object ChannelOptimizerEngine {

    /**
     * Standard non-overlapping channels and standard operating frequencies
     */
    private val CHANNELS_2_4_GHZ = listOf(1, 6, 11)
    private val ALL_CHANNELS_2_4_GHZ = (1..11).toList()

    private val CHANNELS_5_GHZ = listOf(
        // UNII-1
        36, 40, 44, 48,
        // UNII-2A (DFS)
        52, 56, 60, 64,
        // UNII-2C (DFS)
        100, 104, 108, 112, 116, 120, 124, 128, 132, 136, 140, 144,
        // UNII-3
        149, 153, 157, 161, 165
    )

    // Primary 80 MHz channel blocks in 5 GHz
    val PRIMARY_80MHZ_5GHZ = listOf(36, 52, 100, 116, 132, 149)

    private val CHANNELS_6_GHZ_PSC = listOf(
        37, 53, 69, 85, 101, 117, 133, 149, 165, 181, 197, 213
    )

    fun isDfsChannel(channel: Int, band: FrequencyBand): Boolean {
        return band == FrequencyBand.BAND_5_GHZ && channel in 52..144
    }

    /**
     * Evaluate all candidate channels for a given frequency band
     */
    fun evaluateBand(
        band: FrequencyBand,
        allAps: List<AccessPoint>,
        currentChannel: Int,
        targetWidth: ChannelWidth = ChannelWidth.WIDTH_80
    ): OptimizerRecommendation {
        val candidateChannels = when (band) {
            FrequencyBand.BAND_2_4_GHZ -> CHANNELS_2_4_GHZ
            FrequencyBand.BAND_5_GHZ -> CHANNELS_5_GHZ
            FrequencyBand.BAND_6_GHZ -> CHANNELS_6_GHZ_PSC
            FrequencyBand.UNKNOWN -> CHANNELS_5_GHZ
        }

        val apsInBand = allAps.filter { it.band == band }

        val scoredChannels = candidateChannels.map { ch ->
            evaluateChannel(ch, band, apsInBand, currentChannel, targetWidth)
        }

        // Determine recommended channel: highest score, preferring non-DFS if scores are tied or within 3 points
        val sortedCandidates = scoredChannels.sortedWith(
            compareByDescending<ChannelScore> { it.score }
                .thenBy { if (it.isDfs) 1 else 0 } // Prefer non-DFS if score is equal
        )

        val bestCandidate = sortedCandidates.firstOrNull() ?: scoredChannels.first()
        val currentScored = scoredChannels.find { it.channel == currentChannel }
            ?: evaluateChannel(currentChannel, band, apsInBand, currentChannel, targetWidth)

        // Mark the recommended channel
        val finalChannelScores = scoredChannels.map { cs ->
            cs.copy(
                isRecommended = cs.channel == bestCandidate.channel,
                isCurrentChannel = cs.channel == currentChannel
            )
        }

        val scoreDelta = max(0, bestCandidate.score - currentScored.score)
        val eliminatedCollisions = max(0, currentScored.coChannelCount + currentScored.adjacentChannelCount - (bestCandidate.coChannelCount + bestCandidate.adjacentChannelCount))

        val stepByStep = buildStepByStepGuide(
            band = band,
            currentChannel = currentChannel,
            recommendedChannel = bestCandidate.channel,
            bandwidth = targetWidth,
            isDfs = bestCandidate.isDfs
        )

        val directivesText = buildClipboardDirectives(
            band = band,
            currentChannel = currentChannel,
            recommendedChannel = bestCandidate.channel,
            bandwidth = targetWidth,
            currentScore = currentScored.score,
            recommendedScore = bestCandidate.score,
            isDfs = bestCandidate.isDfs
        )

        val reason = when {
            bestCandidate.channel == currentChannel && currentScored.score >= 90 ->
                "Your current Channel $currentChannel is already optimal with minimal spectral congestion."
            scoreDelta >= 25 ->
                "Shifting to Channel ${bestCandidate.channel} avoids $eliminatedCollisions conflicting networks, boosting signal clarity by +$scoreDelta pts."
            scoreDelta > 0 ->
                "Channel ${bestCandidate.channel} provides cleaner RF headroom with less airtime contention."
            else ->
                "Airspace is balanced. Channel ${bestCandidate.channel} offers the cleanest signal propagation."
        }

        return OptimizerRecommendation(
            band = band,
            currentChannel = currentChannel,
            currentScore = currentScored.score,
            recommendedChannel = bestCandidate.channel,
            recommendedScore = bestCandidate.score,
            recommendedBandwidth = targetWidth,
            scoreDelta = scoreDelta,
            eliminatedCollisions = eliminatedCollisions,
            channelScores = finalChannelScores,
            stepByStepGuide = stepByStep,
            routerDirectivesText = directivesText,
            reasonSummary = reason
        )
    }

    /**
     * Compute RF congestion score for a specific channel
     */
    private fun evaluateChannel(
        channel: Int,
        band: FrequencyBand,
        apsInBand: List<AccessPoint>,
        currentChannel: Int,
        targetWidth: ChannelWidth
    ): ChannelScore {
        val freqMhz = FrequencyBand.channelToFrequency(channel, band)
        val candidateEnv = ApMetricsCalculator.getFrequencyEnvelope(freqMhz, targetWidth)

        var penaltyTotal = 0.0
        var coChannelCount = 0
        var adjacentCount = 0
        val conflictingSsids = mutableListOf<String>()
        var maxRssi: Int? = null

        for (ap in apsInBand) {
            // Ignore AP if it's the current user device's connection (so the user doesn't penalize themselves)
            if (ap.isConnected && ap.channel == currentChannel) continue

            // Signal power factor above -90 dBm noise floor
            val powerFactor = max(0, ap.rssi - (-90)).toDouble()

            if (ap.channel == channel) {
                // Co-channel interference: directly shares channel airtime and CSMA/CA clear channel assessment
                coChannelCount++
                val penalty = powerFactor * 2.2
                penaltyTotal += penalty
                conflictingSsids.add(ap.ssid.ifBlank { "Hidden Network" })
                maxRssi = maxOf(maxRssi ?: -120, ap.rssi)
            } else {
                // Check envelope overlap for adjacent interference
                val apEnv = ApMetricsCalculator.getFrequencyEnvelope(ap.frequencyMhz, ap.channelWidth)
                val overlapStart = max(candidateEnv.startMhz, apEnv.startMhz)
                val overlapEnd = min(candidateEnv.endMhz, apEnv.endMhz)

                if (overlapStart < overlapEnd) {
                    val overlapMhz = overlapEnd - overlapStart
                    adjacentCount++
                    // Adjacent channel bleed doesn't coordinate CSMA/CA frames, causing destructive CRC checksum corruptions
                    val overlapRatio = overlapMhz.toDouble() / targetWidth.mhz.toDouble()
                    val penalty = powerFactor * 1.6 * overlapRatio
                    penaltyTotal += penalty
                    conflictingSsids.add(ap.ssid.ifBlank { "Hidden Network" })
                    maxRssi = maxOf(maxRssi ?: -120, ap.rssi)
                }
            }
        }

        val isDfs = isDfsChannel(channel, band)
        // Minor penalty for DFS channels if non-DFS is equally clean (DFS requires channel evacuation on radar detection)
        if (isDfs) {
            penaltyTotal += 4.0
        }

        val finalScore = max(5, min(100, (100.0 - penaltyTotal).roundToInt()))
        val rating = when {
            finalScore >= 85 -> ChannelRating.OPTIMAL
            finalScore >= 70 -> ChannelRating.GOOD
            finalScore >= 50 -> ChannelRating.FAIR
            else -> ChannelRating.CONGESTED
        }

        return ChannelScore(
            channel = channel,
            frequencyMhz = freqMhz,
            band = band,
            isDfs = isDfs,
            score = finalScore,
            rating = rating,
            coChannelCount = coChannelCount,
            adjacentChannelCount = adjacentCount,
            maxInterferingRssi = maxRssi,
            conflictingSsids = conflictingSsids.distinct().take(4),
            recommendedWidth = targetWidth,
            isCurrentChannel = channel == currentChannel,
            isRecommended = false
        )
    }

    private fun buildStepByStepGuide(
        band: FrequencyBand,
        currentChannel: Int,
        recommendedChannel: Int,
        bandwidth: ChannelWidth,
        isDfs: Boolean
    ): List<RouterStep> {
        val bandLabel = when (band) {
            FrequencyBand.BAND_2_4_GHZ -> "2.4 GHz"
            FrequencyBand.BAND_5_GHZ -> "5 GHz"
            FrequencyBand.BAND_6_GHZ -> "6 GHz"
            FrequencyBand.UNKNOWN -> "5 GHz"
        }

        val steps = mutableListOf(
            RouterStep(
                stepNumber = 1,
                title = "Access Router Admin Portal",
                description = "Open your web browser and navigate to your router's gateway IP (commonly http://192.168.1.1, http://192.168.0.1, or http://192.168.50.1) and log in with your admin credentials.",
                parameterHighlight = "http://192.168.1.1"
            ),
            RouterStep(
                stepNumber = 2,
                title = "Navigate to $bandLabel Radio Settings",
                description = "Select 'Wireless Settings' or 'Advanced Wi-Fi', then choose the '$bandLabel Band' configuration tab.",
                parameterHighlight = "$bandLabel Radio Settings"
            ),
            RouterStep(
                stepNumber = 3,
                title = "Change Control Channel",
                description = "Switch 'Channel' or 'Control Channel' from Channel $currentChannel to Channel $recommendedChannel. Avoid 'Auto' channel to lock in this interference-free frequency.",
                parameterHighlight = "Channel $recommendedChannel"
            ),
            RouterStep(
                stepNumber = 4,
                title = "Configure Channel Bandwidth",
                description = "Set 'Channel Bandwidth' to ${bandwidth.label} for optimal throughput and clean spectral boundary enforcement.",
                parameterHighlight = bandwidth.label
            )
        )

        if (isDfs) {
            steps.add(
                RouterStep(
                    stepNumber = 5,
                    title = "DFS Radar Scan Notice",
                    description = "Channel $recommendedChannel operates within the UNII-2 DFS spectrum. Your router will perform a brief 60-second Channel Availability Check (CAC) before broadcasting.",
                    parameterHighlight = "DFS Radar CAC"
                )
            )
        }

        steps.add(
            RouterStep(
                stepNumber = if (isDfs) 6 else 5,
                title = "Apply & Reboot",
                description = "Click 'Apply' or 'Save Settings'. Your router will restart the $bandLabel radio. Reconnecting devices will now communicate on pristine spectrum!",
                parameterHighlight = "Save & Apply"
            )
        )

        return steps
    }

    private fun buildClipboardDirectives(
        band: FrequencyBand,
        currentChannel: Int,
        recommendedChannel: Int,
        bandwidth: ChannelWidth,
        currentScore: Int,
        recommendedScore: Int,
        isDfs: Boolean
    ): String {
        val bandLabel = when (band) {
            FrequencyBand.BAND_2_4_GHZ -> "2.4 GHz"
            FrequencyBand.BAND_5_GHZ -> "5 GHz"
            FrequencyBand.BAND_6_GHZ -> "6 GHz"
            FrequencyBand.UNKNOWN -> "5 GHz"
        }

        return """
            =========================================
            WAVEBALANCE WI-FI OPTIMIZATION DIRECTIVES
            =========================================
            Target Radio: $bandLabel Wireless Network
            Current Channel: Ch $currentChannel (RF Health Score: $currentScore/100)
            RECOMMENDED CHANNEL: Ch $recommendedChannel (RF Health Score: $recommendedScore/100)
            Recommended Bandwidth: ${bandwidth.label}
            Spectrum Type: ${if (isDfs) "UNII-2 DFS (Radar Detection Active)" else "Standard Non-DFS (Universal Support)"}
            
            ROUTER ACTION STEPS:
            1. Open Router Admin: http://192.168.1.1 (or router gateway)
            2. Go to: Wireless / Wi-Fi Settings -> $bandLabel Radio
            3. Set Channel: Change from $currentChannel -> $recommendedChannel
            4. Set Channel Width: ${bandwidth.label}
            5. Save & Reboot Router
            =========================================
        """.trimIndent()
    }
}
