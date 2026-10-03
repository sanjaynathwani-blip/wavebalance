package com.wavebalance.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wavebalance.app.model.DiagnosticPhase
import com.wavebalance.app.ui.theme.CardSurfaceSlate
import com.wavebalance.app.ui.theme.NeonCyan
import com.wavebalance.app.ui.theme.PrimaryContainerBlue
import com.wavebalance.app.ui.theme.SecondaryContainerEmerald
import com.wavebalance.app.ui.theme.TertiaryContainerAmber
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Maps a speed in Mbps (0 to 1000) to a normalized fraction (0.0 to 1.0) using logarithmic progression.
 */
private fun speedToFraction(speedMbps: Float): Float {
    if (speedMbps <= 0f) return 0f
    // Non-linear mapping so lower speeds (10-100 Mbps) have clear resolution on the gauge
    val clamped = speedMbps.coerceIn(0f, 1000f)
    return (ln(1f + clamped) / ln(1001f)).coerceIn(0f, 1f)
}

/**
 * High-precision analog speedometer tachometer for network throughput & latency visualization.
 */
@Composable
fun SpeedometerCanvas(
    currentSpeedMbps: Float,
    currentPingMs: Float,
    phase: DiagnosticPhase,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val targetFraction = when (phase) {
        DiagnosticPhase.PING_JITTER -> (currentPingMs / 150f).coerceIn(0f, 1f)
        DiagnosticPhase.DOWNLOAD,
        DiagnosticPhase.UPLOAD,
        DiagnosticPhase.BUFFERBLOAT,
        DiagnosticPhase.COMPLETED -> speedToFraction(currentSpeedMbps)
        DiagnosticPhase.IDLE -> 0f
    }

    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "SpeedometerFraction"
    )

    // Gauge geometry constants
    val startAngle = 150f
    val totalSweep = 240f
    val currentAngle = startAngle + animatedFraction * totalSweep

    Box(
        modifier = modifier.size(280.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val center = Offset(canvasWidth / 2f, canvasHeight / 2f)
            val radius = min(canvasWidth, canvasHeight) / 2f - 24.dp.toPx()

            val arcTopLeft = Offset(center.x - radius, center.y - radius)
            val arcSize = Size(radius * 2, radius * 2)

            // 1. Background Inactive Arc Track
            drawArc(
                color = CardSurfaceSlate.copy(alpha = 0.6f),
                startAngle = startAngle,
                sweepAngle = totalSweep,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
            )

            // 2. Active Gradient Arc Track
            val activeSweep = animatedFraction * totalSweep
            if (activeSweep > 0.5f) {
                val activeBrush = Brush.sweepGradient(
                    0.0f to PrimaryContainerBlue,
                    0.4f to NeonCyan,
                    0.8f to SecondaryContainerEmerald,
                    1.0f to TertiaryContainerAmber,
                    center = center
                )
                drawArc(
                    brush = activeBrush,
                    startAngle = startAngle,
                    sweepAngle = activeSweep,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // 3. Tick Marks along the Arc (0, 10, 25, 50, 100, 250, 500, 1000 Mbps)
            val tickSpeeds = listOf(0f, 10f, 25f, 50f, 100f, 250f, 500f, 1000f)
            for (tickSpeed in tickSpeeds) {
                val tickFrac = speedToFraction(tickSpeed)
                val tickDeg = startAngle + tickFrac * totalSweep
                val tickRad = Math.toRadians(tickDeg.toDouble())

                val outerX = center.x + (radius + 12.dp.toPx()) * cos(tickRad).toFloat()
                val outerY = center.y + (radius + 12.dp.toPx()) * sin(tickRad).toFloat()
                val innerX = center.x + (radius - 2.dp.toPx()) * cos(tickRad).toFloat()
                val innerY = center.y + (radius - 2.dp.toPx()) * sin(tickRad).toFloat()

                val isMajor = tickSpeed in listOf(0f, 100f, 500f, 1000f)
                val tickColor = if (tickFrac <= animatedFraction) NeonCyan else Color.White.copy(alpha = 0.3f)
                val strokeW = if (isMajor) 2.5.dp.toPx() else 1.5.dp.toPx()

                drawLine(
                    color = tickColor,
                    start = Offset(innerX, innerY),
                    end = Offset(outerX, outerY),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
            }

            // 4. Tachometer Center Hub
            drawCircle(
                color = CardSurfaceSlate,
                radius = 24.dp.toPx(),
                center = center
            )
            drawCircle(
                color = NeonCyan,
                radius = 12.dp.toPx(),
                center = center
            )
            drawCircle(
                color = Color.White,
                radius = 4.dp.toPx(),
                center = center
            )

            // 5. Tachometer Needle with Glowing Tip
            val needleRad = Math.toRadians(currentAngle.toDouble())
            val needleLength = radius - 8.dp.toPx()
            val needleTip = Offset(
                center.x + needleLength * cos(needleRad).toFloat(),
                center.y + needleLength * sin(needleRad).toFloat()
            )
            val needleTail = Offset(
                center.x - 16.dp.toPx() * cos(needleRad).toFloat(),
                center.y - 16.dp.toPx() * sin(needleRad).toFloat()
            )

            // Needle Body
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(NeonCyan, SecondaryContainerEmerald),
                    start = needleTail,
                    end = needleTip
                ),
                start = needleTail,
                end = needleTip,
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Needle Tip Beacon
            drawCircle(
                color = NeonCyan.copy(alpha = 0.5f),
                radius = 8.dp.toPx(),
                center = needleTip
            )
            drawCircle(
                color = Color.White,
                radius = 3.dp.toPx(),
                center = needleTip
            )
        }

        // Digital readout sits in the open bottom of the 240° dial, below the
        // hub, so the needle and hub never cover the numbers
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 78.dp)
        ) {
            val displayValue = when (phase) {
                DiagnosticPhase.PING_JITTER -> String.format(Locale.US, "%.1f", currentPingMs)
                DiagnosticPhase.IDLE -> "0.0"
                else -> String.format(Locale.US, "%.1f", currentSpeedMbps)
            }

            val displayUnit = when (phase) {
                DiagnosticPhase.PING_JITTER -> "ms LATENCY"
                else -> "Mbps"
            }

            Text(
                text = displayValue,
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = NeonCyan
            )
            Text(
                text = displayUnit,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = phase.displayName.uppercase(Locale.US),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                color = when (phase) {
                    DiagnosticPhase.IDLE -> MaterialTheme.colorScheme.onSurfaceVariant
                    DiagnosticPhase.PING_JITTER -> TertiaryContainerAmber
                    DiagnosticPhase.DOWNLOAD -> NeonCyan
                    DiagnosticPhase.UPLOAD -> PrimaryContainerBlue
                    DiagnosticPhase.BUFFERBLOAT -> TertiaryContainerAmber
                    DiagnosticPhase.COMPLETED -> SecondaryContainerEmerald
                }
            )
        }
    }
}
