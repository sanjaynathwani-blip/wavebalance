package com.wavebalance.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wavebalance.app.model.DiagnosticSamplePoint
import com.wavebalance.app.ui.theme.CardSurfaceSlate
import com.wavebalance.app.ui.theme.DarkSurfaceContainer
import com.wavebalance.app.ui.theme.NeonCyan
import com.wavebalance.app.ui.theme.PrimaryContainerBlue
import com.wavebalance.app.ui.theme.SecondaryContainerEmerald
import com.wavebalance.app.ui.theme.TertiaryContainerAmber
import kotlin.math.max

/**
 * Real-time continuous trajectory graph plotting speed (Mbps) and latency (ms) during diagnostic runs.
 */
@Composable
fun SpeedLatencyGraph(
    samples: List<DiagnosticSamplePoint>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header with legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "REAL-TIME TRAJECTORY",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))

                // Cyan Speed Legend
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(NeonCyan, CircleShape)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Mbps",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = NeonCyan
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Amber Latency Legend
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(TertiaryContainerAmber, CircleShape)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "ms RTT",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TertiaryContainerAmber
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Graph Canvas
            Box(modifier = Modifier.fillMaxSize()) {
                if (samples.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Awaiting benchmark execution...",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        // Grid lines (3 horizontal guides)
                        for (i in 1..3) {
                            val y = h * (i / 4f)
                            drawLine(
                                color = CardSurfaceSlate.copy(alpha = 0.5f),
                                start = Offset(0f, y),
                                end = Offset(w, y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        val maxSpeed = max(50f, samples.maxOfOrNull { it.speedMbps } ?: 50f)
                        val maxPing = max(60f, samples.maxOfOrNull { it.pingMs } ?: 60f)
                        val maxTime = max(1f, samples.lastOrNull()?.elapsedSec ?: 1f)

                        // 1. Throughput Fill & Line (Cyan)
                        val speedPath = Path()
                        val speedFillPath = Path()
                        var firstSpeed = true

                        samples.forEach { sample ->
                            val x = (sample.elapsedSec / maxTime) * w
                            val y = h - (sample.speedMbps / maxSpeed) * (h - 10.dp.toPx()) - 5.dp.toPx()

                            if (firstSpeed) {
                                speedPath.moveTo(x, y)
                                speedFillPath.moveTo(x, h)
                                speedFillPath.lineTo(x, y)
                                firstSpeed = false
                            } else {
                                speedPath.lineTo(x, y)
                                speedFillPath.lineTo(x, y)
                            }
                        }

                        if (!firstSpeed) {
                            speedFillPath.lineTo(w, h)
                            speedFillPath.close()

                            // Fill
                            drawPath(
                                path = speedFillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(NeonCyan.copy(alpha = 0.25f), Color.Transparent),
                                    startY = 0f,
                                    endY = h
                                )
                            )

                            // Stroke
                            drawPath(
                                path = speedPath,
                                color = NeonCyan,
                                style = Stroke(
                                    width = 2.5.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }

                        // 2. Latency Line (Amber)
                        val pingPath = Path()
                        var firstPing = true

                        samples.filter { it.pingMs > 0f }.forEach { sample ->
                            val x = (sample.elapsedSec / maxTime) * w
                            val y = h - (sample.pingMs / maxPing) * (h - 10.dp.toPx()) - 5.dp.toPx()

                            if (firstPing) {
                                pingPath.moveTo(x, y)
                                firstPing = false
                            } else {
                                pingPath.lineTo(x, y)
                            }
                        }

                        if (!firstPing) {
                            drawPath(
                                path = pingPath,
                                color = TertiaryContainerAmber,
                                style = Stroke(
                                    width = 2.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
