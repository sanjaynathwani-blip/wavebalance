package com.wavebalance.app

import com.wavebalance.app.model.ApplicationRating
import com.wavebalance.app.model.BufferbloatGrade
import com.wavebalance.app.model.DiagnosticPhase
import com.wavebalance.app.model.DiagnosticSamplePoint
import com.wavebalance.app.model.QosAssessment
import com.wavebalance.app.model.SpeedDiagnosticEngine
import com.wavebalance.app.model.SpeedDiagnosticResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeedDiagnosticEngineTest {

    @Test
    fun testCalculateJitter_emptyAndSingleSample() {
        assertEquals(0.0, SpeedDiagnosticEngine.calculateJitter(emptyList()), 0.001)
        assertEquals(0.0, SpeedDiagnosticEngine.calculateJitter(listOf(24.5)), 0.001)
    }

    @Test
    fun testCalculateJitter_identicalSamples() {
        val samples = listOf(20.0, 20.0, 20.0, 20.0)
        assertEquals(0.0, SpeedDiagnosticEngine.calculateJitter(samples), 0.001)
    }

    @Test
    fun testCalculateJitter_standardDeviationFormula() {
        // Samples: 10, 20, 30
        // Mean = 20
        // Variance = ((10-20)^2 + (20-20)^2 + (30-20)^2) / (3 - 1) = (100 + 0 + 100) / 2 = 100
        // Std Dev = sqrt(100) = 10.0
        val samples = listOf(10.0, 20.0, 30.0)
        assertEquals(10.0, SpeedDiagnosticEngine.calculateJitter(samples), 0.001)
    }

    @Test
    fun testEvaluateBufferbloatGrade() {
        assertEquals(BufferbloatGrade.A_PLUS, SpeedDiagnosticEngine.evaluateBufferbloatGrade(0.0))
        assertEquals(BufferbloatGrade.A_PLUS, SpeedDiagnosticEngine.evaluateBufferbloatGrade(5.0))
        assertEquals(BufferbloatGrade.A, SpeedDiagnosticEngine.evaluateBufferbloatGrade(5.1))
        assertEquals(BufferbloatGrade.A, SpeedDiagnosticEngine.evaluateBufferbloatGrade(15.0))
        assertEquals(BufferbloatGrade.B, SpeedDiagnosticEngine.evaluateBufferbloatGrade(15.1))
        assertEquals(BufferbloatGrade.B, SpeedDiagnosticEngine.evaluateBufferbloatGrade(30.0))
        assertEquals(BufferbloatGrade.C, SpeedDiagnosticEngine.evaluateBufferbloatGrade(30.1))
        assertEquals(BufferbloatGrade.C, SpeedDiagnosticEngine.evaluateBufferbloatGrade(60.0))
        assertEquals(BufferbloatGrade.D, SpeedDiagnosticEngine.evaluateBufferbloatGrade(60.1))
        assertEquals(BufferbloatGrade.D, SpeedDiagnosticEngine.evaluateBufferbloatGrade(120.0))
        assertEquals(BufferbloatGrade.F, SpeedDiagnosticEngine.evaluateBufferbloatGrade(120.1))
        assertEquals(BufferbloatGrade.F, SpeedDiagnosticEngine.evaluateBufferbloatGrade(350.0))
    }

    @Test
    fun testAssessQos_highPerformance() {
        val qos = SpeedDiagnosticEngine.evaluateQos(
            unloadedPing = 12.0,
            jitter = 2.1,
            downloadMbps = 450.0,
            uploadMbps = 95.0,
            bufferbloatGrade = BufferbloatGrade.A_PLUS
        )

        assertEquals(ApplicationRating.EXCELLENT, qos.gamingRating)
        assertEquals(ApplicationRating.EXCELLENT, qos.videoCallRating)
        assertEquals(ApplicationRating.EXCELLENT, qos.streamingRating)
        assertEquals(ApplicationRating.EXCELLENT, qos.cloudTransferRating)
    }

    @Test
    fun testAssessQos_congestedPoor() {
        val qos = SpeedDiagnosticEngine.evaluateQos(
            unloadedPing = 110.0,
            jitter = 38.0,
            downloadMbps = 8.5,
            uploadMbps = 1.2,
            bufferbloatGrade = BufferbloatGrade.D
        )

        assertEquals(ApplicationRating.POOR, qos.gamingRating)
        assertTrue(qos.videoCallRating == ApplicationRating.FAIR || qos.videoCallRating == ApplicationRating.POOR)
        assertEquals(ApplicationRating.POOR, qos.streamingRating)
        assertEquals(ApplicationRating.POOR, qos.cloudTransferRating)
    }

    @Test
    fun testGenerateSpeedReportMarkdown() {
        val qos = QosAssessment(
            gamingRating = ApplicationRating.EXCELLENT,
            gamingDetail = "Sub-30ms RTT & low jitter ensures competitive tournament-grade response.",
            videoCallRating = ApplicationRating.EXCELLENT,
            videoCallDetail = "Crystal-clear HD/4K conferencing.",
            streamingRating = ApplicationRating.EXCELLENT,
            streamingDetail = "Instant 4K/8K HDR streaming.",
            cloudTransferRating = ApplicationRating.EXCELLENT,
            cloudTransferDetail = "High-speed cloud backups."
        )

        val result = SpeedDiagnosticResult(
            unloadedPingMs = 14.2,
            jitterMs = 1.8,
            downloadSpeedMbps = 312.5,
            peakDownloadMbps = 345.0,
            uploadSpeedMbps = 84.0,
            loadedPingMs = 22.0,
            bufferbloatDeltaMs = 7.8,
            bufferbloatGrade = BufferbloatGrade.A,
            qosAssessment = qos,
            timestamp = System.currentTimeMillis()
        )

        val markdown = SpeedDiagnosticEngine.generateSpeedReportMarkdown(
            result = result,
            activeSsid = "GoogleFiber-5G",
            bssid = "00:11:22:33:44:55",
            theoreticalLinkSpeedMbps = 866
        )

        assertNotNull(markdown)
        assertTrue(markdown.contains("# WaveBalance Speed & Latency Diagnostic Audit"))
        assertTrue(markdown.contains("GoogleFiber-5G"))
        assertTrue(markdown.contains("00:11:22:33:44:55"))
        assertTrue(markdown.contains("866 Mbps"))
        assertTrue(markdown.contains("312.5 Mbps"))
        assertTrue(markdown.contains("84.0 Mbps"))
        assertTrue(markdown.contains("14.2 ms"))
        assertTrue(markdown.contains("1.8 ms"))
        assertTrue(markdown.contains("Bufferbloat Rating"))
        assertTrue(markdown.contains("Grade A"))
        assertTrue(markdown.contains("Bufferbloat Passed"))

        // Also test Grade C or lower generates SQM recommendation
        val congestedResult = result.copy(
            bufferbloatGrade = BufferbloatGrade.C,
            bufferbloatDeltaMs = 45.0
        )
        val sqmMarkdown = SpeedDiagnosticEngine.generateSpeedReportMarkdown(
            result = congestedResult,
            activeSsid = "GoogleFiber-5G",
            bssid = "00:11:22:33:44:55",
            theoreticalLinkSpeedMbps = 866
        )
        assertTrue(sqmMarkdown.contains("Bufferbloat Detected"))
        assertTrue(sqmMarkdown.contains("Smart Queue Management (SQM)"))
    }
}
