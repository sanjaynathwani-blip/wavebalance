package com.wavebalance.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wavebalance.app.model.ApMetricsCalculator
import com.wavebalance.app.model.RssiSample
import com.wavebalance.app.ui.theme.DarkSurfaceContainer
import com.wavebalance.app.ui.theme.DarkSurfaceContainerHigh
import com.wavebalance.app.ui.theme.PrimaryContainerBlue
import com.wavebalance.app.ui.theme.SecondaryContainerEmerald
import com.wavebalance.app.ui.theme.TertiaryContainerAmber

@Composable
fun RssiSparklineChart(
    samples: List<RssiSample>,
    currentRssi: Int,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    // Only real samples are drawn; nothing is filled in while history builds up
    val stats = remember(samples, currentRssi) {
        ApMetricsCalculator.computeRssiStats(samples, currentRssi)
    }
    val hasJitter = samples.size >= 3

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Title & Stability Jitter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Signal Stability & Sparkline",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val jitterColor = when {
                    !hasJitter -> MaterialTheme.colorScheme.onSurfaceVariant
                    stats.jitter <= 2.0 -> SecondaryContainerEmerald
                    else -> TertiaryContainerAmber
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = jitterColor.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (hasJitter) "Jitter: ±${stats.jitter} dB" else "Collecting samples…",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = jitterColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Canvas Chart Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(DarkSurfaceContainerHigh, shape = RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val w = size.width
                    val h = size.height

                    // Dynamic scale: min -95 dBm (bottom), max -35 dBm (top)
                    val dbmFloor = -95f
                    val dbmCeil = -35f
                    val dbmRange = dbmCeil - dbmFloor

                    fun rssiToY(rssi: Float): Float {
                        val fraction = (rssi - dbmFloor) / dbmRange
                        return h - (fraction.coerceIn(0f, 1f) * h)
                    }

                    // Reference Grid Lines
                    val gridLevels = listOf(-40, -60, -75, -85)
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)

                    gridLevels.forEach { level ->
                        val y = rssiToY(level.toFloat())
                        drawLine(
                            color = Color(0xFF334155).copy(alpha = 0.6f),
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1f,
                            pathEffect = dashEffect
                        )
                    }

                    // Low Latency Target Line (-65 dBm)
                    val targetY = rssiToY(-65f)
                    drawLine(
                        color = SecondaryContainerEmerald.copy(alpha = 0.4f),
                        start = Offset(0f, targetY),
                        end = Offset(w, targetY),
                        strokeWidth = 1.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                    )

                    if (samples.isEmpty()) return@Canvas

                    // Place samples by time, since scan-based samples arrive irregularly
                    val sorted = samples.sortedBy { it.timestamp }
                    val firstTime = sorted.first().timestamp
                    val span = (sorted.last().timestamp - firstTime).coerceAtLeast(1L)

                    val points = sorted.map { sample ->
                        val x = if (sorted.size == 1) w else (sample.timestamp - firstTime).toFloat() / span * w
                        val y = rssiToY(sample.rssi.toFloat())
                        Offset(x, y)
                    }

                    // Build smooth path
                    val strokePath = Path()
                    val fillPath = Path()

                    strokePath.moveTo(points[0].x, points[0].y)
                    fillPath.moveTo(points[0].x, h)
                    fillPath.lineTo(points[0].x, points[0].y)

                    for (i in 0 until points.size - 1) {
                        val p0 = points[i]
                        val p1 = points[i + 1]
                        val controlX = (p0.x + p1.x) / 2f
                        strokePath.cubicTo(
                            controlX, p0.y,
                            controlX, p1.y,
                            p1.x, p1.y
                        )
                        fillPath.cubicTo(
                            controlX, p0.y,
                            controlX, p1.y,
                            p1.x, p1.y
                        )
                    }

                    fillPath.lineTo(points.last().x, h)
                    fillPath.close()

                    // Draw Gradient Fill
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                PrimaryContainerBlue.copy(alpha = 0.28f),
                                PrimaryContainerBlue.copy(alpha = 0.02f)
                            ),
                            startY = 0f,
                            endY = h
                        )
                    )

                    // Draw Stroke Line
                    drawPath(
                        path = strokePath,
                        color = PrimaryContainerBlue,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw Points
                    points.forEachIndexed { idx, pt ->
                        val isLast = idx == points.size - 1
                        if (isLast) {
                            // Pulsing outer halo for latest point
                            drawCircle(
                                color = PrimaryContainerBlue.copy(alpha = 0.35f),
                                radius = 7.dp.toPx(),
                                center = pt
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 3.5.dp.toPx(),
                                center = pt
                            )
                        } else if (idx % 2 == 0) {
                            drawCircle(
                                color = PrimaryContainerBlue,
                                radius = 2.dp.toPx(),
                                center = pt
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Time & Threshold Labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = samples.minOfOrNull { it.timestamp }?.let { formatAge(System.currentTimeMillis() - it) } ?: "No samples yet",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "-65 dBm Target (Gaming/VoIP)",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = SecondaryContainerEmerald.copy(alpha = 0.8f)
                )
                Text(
                    text = "Now",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stats Chip Row: Current, Min, Max, Avg
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatPill(
                    label = "Current",
                    value = "${stats.current} dBm",
                    color = PrimaryContainerBlue,
                    modifier = Modifier.weight(1f)
                )
                StatPill(
                    label = "Max Peak",
                    value = "${stats.max} dBm",
                    color = SecondaryContainerEmerald,
                    modifier = Modifier.weight(1f)
                )
                StatPill(
                    label = "Min Dip",
                    value = "${stats.min} dBm",
                    color = TertiaryContainerAmber,
                    modifier = Modifier.weight(1f)
                )
                StatPill(
                    label = "Average",
                    value = "${stats.avg} dBm",
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatPill(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = DarkSurfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

private fun formatAge(ageMs: Long): String {
    val seconds = (ageMs / 1000).coerceAtLeast(0)
    return when {
        seconds < 90 -> "${seconds}s ago"
        seconds < 90 * 60 -> "${seconds / 60} min ago"
        else -> "${seconds / 3600} h ago"
    }
}
