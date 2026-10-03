package com.wavebalance.app.model

import androidx.compose.ui.geometry.Offset
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Represents a single pinned physical RF measurement in the site survey.
 */
data class SurveyPoint(
    val id: String = UUID.randomUUID().toString(),
    val x: Float, // Normalized 0.0f..1.0f on floor grid
    val y: Float, // Normalized 0.0f..1.0f on floor grid
    val roomName: String,
    val bssid: String,
    val ssid: String,
    val rssi: Int, // Signal in dBm (e.g. -55)
    val frequencyMhz: Int,
    val channel: Int,
    val band: FrequencyBand,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Predefined architectural room zone on the 2D floor blueprint.
 */
data class RoomZone(
    val id: String,
    val name: String,
    val left: Float,   // Normalized 0.0f..1.0f
    val top: Float,    // Normalized 0.0f..1.0f
    val right: Float,  // Normalized 0.0f..1.0f
    val bottom: Float, // Normalized 0.0f..1.0f
    val centerWaypointX: Float = (left + right) / 2f,
    val centerWaypointY: Float = (top + bottom) / 2f
) {
    fun contains(x: Float, y: Float): Boolean {
        return x in left..right && y in top..bottom
    }
}

/**
 * Summary analytics derived from the site survey measurement collection.
 */
data class SurveyAnalytics(
    val totalPoints: Int,
    val avgRssi: Int,
    val coveragePercent: Int, // % of surveyed points >= -65 dBm (Good/Optimal)
    val deadZonesDetected: Int, // Points <= -75 dBm
    val weakZonesDetected: Int, // Points between -74 dBm and -66 dBm
    val optimalZonesDetected: Int, // Points >= -65 dBm
    val bestRoom: String?,
    val bestRssi: Int?,
    val worstRoom: String?,
    val worstRssi: Int?,
    val recommendedMeshOffset: Offset?, // Normalized (x, y) coordinates for mesh node placement
    val recommendedMeshRoom: String?,
    val recommendationSummary: String
)

/**
 * Mathematical engine for RF propagation interpolation, coverage modeling, and dead zone remediation.
 */
object SiteSurveyEngine {

    val DEFAULT_ROOM_ZONES = listOf(
        RoomZone("living", "Living Room", 0.05f, 0.08f, 0.52f, 0.52f),
        RoomZone("office", "Office / Lab", 0.56f, 0.08f, 0.95f, 0.45f),
        RoomZone("kitchen", "Kitchen / Dining", 0.05f, 0.56f, 0.48f, 0.92f),
        RoomZone("bedroom", "Master Bedroom", 0.52f, 0.49f, 0.95f, 0.74f),
        RoomZone("patio", "Patio / Balcony", 0.52f, 0.78f, 0.95f, 0.92f)
    )

    /**
     * Resolves the room zone corresponding to a normalized coordinate (x, y).
     */
    fun findRoomForCoordinate(x: Float, y: Float, zones: List<RoomZone> = DEFAULT_ROOM_ZONES): String {
        return zones.firstOrNull { it.contains(x, y) }?.name ?: "Open Space"
    }

    /**
     * Calculates the interpolated RSSI at coordinate (x, y) using Inverse Distance Weighting (IDW)
     * with free-space / indoor path loss exponent p = 2.0.
     *
     * S(x, y) = sum(w_i * RSSI_i) / sum(w_i), where w_i = 1 / ((d_i)^2 + epsilon)
     */
    fun interpolateRssi(
        x: Float,
        y: Float,
        points: List<SurveyPoint>,
        powerExponent: Float = 2.0f,
        epsilon: Float = 0.002f
    ): Float {
        if (points.isEmpty()) return -85f

        var totalWeight = 0f
        var weightedSum = 0f

        for (pt in points) {
            val dx = x - pt.x
            val dy = y - pt.y
            val distSq = dx * dx + dy * dy
            val dist = sqrt(distSq)

            if (dist < 0.02f) {
                // Point is virtually coincident with a survey pin
                return pt.rssi.toFloat()
            }

            val weight = 1f / (distSq.pow(powerExponent / 2f) + epsilon)
            weightedSum += weight * pt.rssi
            totalWeight += weight
        }

        if (totalWeight == 0f) return -85f
        val idwResult = weightedSum / totalWeight

        // Natural perimeter attenuation: if the coordinate is far from all survey points,
        // decay towards ambient noise floor (-88 dBm)
        val minDistance = points.minOf { pt ->
            val dx = x - pt.x
            val dy = y - pt.y
            sqrt(dx * dx + dy * dy)
        }

        val attenuationFactor = ((minDistance - 0.25f) / 0.85f).coerceIn(0f, 1f)
        return (idwResult * (1f - attenuationFactor) + (-88f * attenuationFactor)).coerceIn(-95f, -30f)
    }

    /**
     * Evaluates all survey measurements and generates high-level coverage analytics.
     */
    fun computeAnalytics(
        points: List<SurveyPoint>,
        zones: List<RoomZone> = DEFAULT_ROOM_ZONES
    ): SurveyAnalytics {
        if (points.isEmpty()) {
            return SurveyAnalytics(
                totalPoints = 0,
                avgRssi = -85,
                coveragePercent = 0,
                deadZonesDetected = 0,
                weakZonesDetected = 0,
                optimalZonesDetected = 0,
                bestRoom = null,
                bestRssi = null,
                worstRoom = null,
                worstRssi = null,
                recommendedMeshOffset = null,
                recommendedMeshRoom = null,
                recommendationSummary = "No survey points recorded. Tap on the floor plan or walk through rooms to pin RF measurements."
            )
        }

        val avgRssi = points.map { it.rssi }.average().toInt()
        val optimalCount = points.count { it.rssi >= -65 }
        val weakCount = points.count { it.rssi in -74..-66 }
        val deadCount = points.count { it.rssi <= -75 }
        val coveragePercent = ((optimalCount.toFloat() / points.size.toFloat()) * 100).toInt()

        val bestPoint = points.maxByOrNull { it.rssi }
        val worstPoint = points.minByOrNull { it.rssi }

        // Determine optimal mesh node placement:
        // If dead zones or weak zones exist, place mesh node halfway between the primary AP (best point)
        // and the most severe dead zone (worst point) to maintain a strong wireless backhaul link.
        val (recommendedOffset, meshRoom, summary) = if (deadCount > 0 && bestPoint != null && worstPoint != null) {
            val meshX = (bestPoint.x * 0.45f + worstPoint.x * 0.55f).coerceIn(0.1f, 0.9f)
            val meshY = (bestPoint.y * 0.45f + worstPoint.y * 0.55f).coerceIn(0.1f, 0.9f)
            val room = findRoomForCoordinate(meshX, meshY, zones)
            Triple(
                Offset(meshX, meshY),
                room,
                "Dead zone detected in ${worstPoint.roomName} (${worstPoint.rssi} dBm). Place an AP/Mesh satellite in $room to provide optimal line-of-sight backhaul and eliminate dead zone."
            )
        } else if (weakCount > 0 && worstPoint != null) {
            val room = worstPoint.roomName
            Triple(
                Offset(worstPoint.x, worstPoint.y),
                room,
                "Marginal coverage in $room (${worstPoint.rssi} dBm). Adjust AP transmission power or position an additional mesh node nearby."
            )
        } else {
            Triple(
                null,
                null,
                "Pristine airspace coverage! 100% of surveyed points exhibit solid signal strength (>= -65 dBm) suitable for 4K streaming and low-latency gaming."
            )
        }

        return SurveyAnalytics(
            totalPoints = points.size,
            avgRssi = avgRssi,
            coveragePercent = coveragePercent,
            deadZonesDetected = deadCount,
            weakZonesDetected = weakCount,
            optimalZonesDetected = optimalCount,
            bestRoom = bestPoint?.roomName,
            bestRssi = bestPoint?.rssi,
            worstRoom = worstPoint?.roomName,
            worstRssi = worstPoint?.rssi,
            recommendedMeshOffset = recommendedOffset,
            recommendedMeshRoom = meshRoom,
            recommendationSummary = summary
        )
    }

    /**
     * Generates a pre-populated realistic walkthrough dataset covering standard domestic/office rooms.
     */
    fun generateSimulatedWalkthrough(
        activeSsid: String = "SuNsTeR",
        activeBssid: String = "1a:2b:3c:4d:5e:6f"
    ): List<SurveyPoint> {
        val now = System.currentTimeMillis()
        return listOf(
            SurveyPoint(
                x = 0.28f,
                y = 0.28f,
                roomName = "Living Room (Main AP)",
                bssid = activeBssid,
                ssid = activeSsid,
                rssi = -44,
                frequencyMhz = 5180,
                channel = 36,
                band = FrequencyBand.BAND_5_GHZ,
                timestamp = now - 180000L
            ),
            SurveyPoint(
                x = 0.72f,
                y = 0.24f,
                roomName = "Office / Lab",
                bssid = activeBssid,
                ssid = activeSsid,
                rssi = -56,
                frequencyMhz = 5180,
                channel = 36,
                band = FrequencyBand.BAND_5_GHZ,
                timestamp = now - 140000L
            ),
            SurveyPoint(
                x = 0.25f,
                y = 0.74f,
                roomName = "Kitchen / Dining",
                bssid = activeBssid,
                ssid = activeSsid,
                rssi = -62,
                frequencyMhz = 5180,
                channel = 36,
                band = FrequencyBand.BAND_5_GHZ,
                timestamp = now - 100000L
            ),
            SurveyPoint(
                x = 0.75f,
                y = 0.62f,
                roomName = "Master Bedroom",
                bssid = activeBssid,
                ssid = activeSsid,
                rssi = -71,
                frequencyMhz = 5180,
                channel = 36,
                band = FrequencyBand.BAND_5_GHZ,
                timestamp = now - 60000L
            ),
            SurveyPoint(
                x = 0.75f,
                y = 0.85f,
                roomName = "Patio / Balcony",
                bssid = activeBssid,
                ssid = activeSsid,
                rssi = -81, // Realistic dead zone through exterior wall
                frequencyMhz = 2412,
                channel = 1,
                band = FrequencyBand.BAND_2_4_GHZ,
                timestamp = now - 20000L
            )
        )
    }

    /**
     * Synthesizes an exportable Markdown Site Survey Audit Report with waypoint inventory.
     */
    fun generateSurveyReportMarkdown(
        points: List<SurveyPoint>,
        analytics: SurveyAnalytics,
        activeSsid: String
    ): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val timestampStr = dateFormat.format(Date())

        val sb = StringBuilder()
        sb.appendLine("# WaveBalance Wi-Fi Site Survey & Coverage Heatmap Report")
        sb.appendLine("**Generated:** $timestampStr")
        sb.appendLine("**Target Network SSID:** `$activeSsid`")
        sb.appendLine("**Total Sampled Locations:** ${analytics.totalPoints}")
        sb.appendLine("**Airspace Coverage Score:** ${analytics.coveragePercent}% Adequate Coverage (>= -65 dBm)")
        sb.appendLine("**Average Signal Strength:** ${analytics.avgRssi} dBm")
        sb.appendLine()
        sb.appendLine("---")
        sb.appendLine()
        sb.appendLine("## 1. Survey Coverage Assessment")
        sb.appendLine("- **Optimal Zones (>= -65 dBm):** ${analytics.optimalZonesDetected} locations (${if (points.isNotEmpty()) (analytics.optimalZonesDetected * 100 / points.size) else 0}%)")
        sb.appendLine("- **Weak Zones (-66 to -74 dBm):** ${analytics.weakZonesDetected} locations")
        sb.appendLine("- **Dead Zones (<= -75 dBm):** ${analytics.deadZonesDetected} locations")
        sb.appendLine("- **Best Measured Room:** ${analytics.bestRoom ?: "N/A"} (${analytics.bestRssi?.let { "$it dBm" } ?: "N/A"})")
        sb.appendLine("- **Worst Measured Room:** ${analytics.worstRoom ?: "N/A"} (${analytics.worstRssi?.let { "$it dBm" } ?: "N/A"})")
        sb.appendLine()
        sb.appendLine("### Recommendation Directive")
        sb.appendLine("> ${analytics.recommendationSummary}")
        if (analytics.recommendedMeshOffset != null && analytics.recommendedMeshRoom != null) {
            val meshX = String.format(Locale.US, "%.2f", analytics.recommendedMeshOffset.x)
            val meshY = String.format(Locale.US, "%.2f", analytics.recommendedMeshOffset.y)
            sb.appendLine("- **Recommended Mesh Satellite Placement:** `${analytics.recommendedMeshRoom}` at coordinate `($meshX, $meshY)`")
        }
        sb.appendLine()
        sb.appendLine("---")
        sb.appendLine()
        sb.appendLine("## 2. Pinned Waypoint Inventory")
        sb.appendLine("| # | Room / Location | Signal (dBm) | Band | Channel | BSSID | Time |")
        sb.appendLine("|---|---|---|---|---|---|---|")
        points.forEachIndexed { idx, pt ->
            val time = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(pt.timestamp))
            val bandLabel = when (pt.band) {
                FrequencyBand.BAND_2_4_GHZ -> "2.4 GHz"
                FrequencyBand.BAND_5_GHZ -> "5 GHz"
                FrequencyBand.BAND_6_GHZ -> "6 GHz"
                FrequencyBand.UNKNOWN -> "Unknown"
            }
            sb.appendLine("| ${idx + 1} | **${pt.roomName}** | `${pt.rssi} dBm` | $bandLabel | Ch ${pt.channel} | `${pt.bssid}` | $time |")
        }
        sb.appendLine()
        sb.appendLine("---")
        sb.appendLine("*Report synthesized by WaveBalance Adaptive RF Spectrum Suite*")
        return sb.toString()
    }
}
