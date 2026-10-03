package com.wavebalance.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Router
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wavebalance.app.model.RoomZone
import com.wavebalance.app.model.SiteSurveyEngine
import com.wavebalance.app.model.SurveyAnalytics
import com.wavebalance.app.model.SurveyPoint
import com.wavebalance.app.ui.theme.CardSurfaceSlate
import com.wavebalance.app.ui.theme.DarkSurfaceContainer
import com.wavebalance.app.ui.theme.ErrorRed
import com.wavebalance.app.ui.theme.NeonCyan
import com.wavebalance.app.ui.theme.SecondaryContainerEmerald
import com.wavebalance.app.ui.theme.TertiaryContainerAmber

enum class HeatmapDisplayMode {
    FULL_HEATMAP,
    DEAD_ZONES_ONLY,
    BLUEPRINT_ONLY
}

@Composable
fun RfHeatmapCanvas(
    points: List<SurveyPoint>,
    analytics: SurveyAnalytics,
    zones: List<RoomZone> = SiteSurveyEngine.DEFAULT_ROOM_ZONES,
    cursorX: Float,
    cursorY: Float,
    displayMode: HeatmapDisplayMode = HeatmapDisplayMode.FULL_HEATMAP,
    onCursorMoved: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    val infiniteTransition = rememberInfiniteTransition(label = "beacon")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header with interactive prompt
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = "Heatmap",
                        tint = NeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "FLOOR PLAN RF HEATMAP",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                }

                Text(
                    text = "Tap canvas to place pin",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main 2D Heatmap & Blueprint Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.15f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF070B14))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val normalizedX = (offset.x / size.width).coerceIn(0.02f, 0.98f)
                            val normalizedY = (offset.y / size.height).coerceIn(0.02f, 0.98f)
                            onCursorMoved(normalizedX, normalizedY)
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height

                    // 1. Draw subtle background coordinate grid
                    drawCoordinateGrid(canvasWidth, canvasHeight)

                    // 2. Draw 2D Interpolated Heatmap Field (if enabled and points exist)
                    if (displayMode != HeatmapDisplayMode.BLUEPRINT_ONLY && points.isNotEmpty()) {
                        drawHeatmapRaster(
                            points = points,
                            canvasWidth = canvasWidth,
                            canvasHeight = canvasHeight,
                            deadZonesOnly = displayMode == HeatmapDisplayMode.DEAD_ZONES_ONLY
                        )
                    }

                    // 3. Draw architectural room blueprint walls and zone labels
                    drawArchitecturalBlueprint(
                        zones = zones,
                        canvasWidth = canvasWidth,
                        canvasHeight = canvasHeight,
                        textMeasurer = textMeasurer
                    )

                    // 4. Draw recommended mesh satellite AP placement (if detected)
                    val meshOffset = analytics.recommendedMeshOffset
                    if (meshOffset != null) {
                        drawRecommendedMeshNode(
                            meshOffset = meshOffset,
                            canvasWidth = canvasWidth,
                            canvasHeight = canvasHeight,
                            pulseScale = pulseScale,
                            textMeasurer = textMeasurer
                        )
                    }

                    // 5. Draw pinned survey waypoints
                    points.forEachIndexed { index, pt ->
                        drawSurveyPointPin(
                            point = pt,
                            index = index + 1,
                            canvasWidth = canvasWidth,
                            canvasHeight = canvasHeight,
                            textMeasurer = textMeasurer
                        )
                    }

                    // 6. Draw active cursor reticle
                    drawPlacementCursor(
                        cursorX = cursorX,
                        cursorY = cursorY,
                        canvasWidth = canvasWidth,
                        canvasHeight = canvasHeight,
                        pulseScale = pulseScale
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // RF Signal Color Gradient Legend Bar
            HeatmapLegendBar()
        }
    }
}

/**
 * Draws subtle gridlines across the blueprint.
 */
private fun DrawScope.drawCoordinateGrid(width: Float, height: Float) {
    val step = 24.dp.toPx()
    val gridColor = Color(0xFF131D31)

    var x = step
    while (x < width) {
        drawLine(
            color = gridColor,
            start = Offset(x, 0f),
            end = Offset(x, height),
            strokeWidth = 1f
        )
        x += step
    }

    var y = step
    while (y < height) {
        drawLine(
            color = gridColor,
            start = Offset(0f, y),
            end = Offset(width, y),
            strokeWidth = 1f
        )
        y += step
    }
}

/**
 * Evaluates the 2D IDW RF interpolation across a discrete grid and renders smooth color-coded RF energy cells.
 */
private fun DrawScope.drawHeatmapRaster(
    points: List<SurveyPoint>,
    canvasWidth: Float,
    canvasHeight: Float,
    deadZonesOnly: Boolean
) {
    val cols = 22
    val rows = 20
    val cellWidth = canvasWidth / cols
    val cellHeight = canvasHeight / rows

    for (c in 0 until cols) {
        val normX = (c + 0.5f) / cols
        for (r in 0 until rows) {
            val normY = (r + 0.5f) / rows

            val estimatedRssi = SiteSurveyEngine.interpolateRssi(normX, normY, points)

            if (deadZonesOnly && estimatedRssi > -75f) {
                continue
            }

            val cellColor = getRssiColor(estimatedRssi)
            val alpha = when {
                estimatedRssi >= -55f -> 0.40f
                estimatedRssi >= -65f -> 0.35f
                estimatedRssi >= -74f -> 0.38f
                else -> 0.50f // Vivid red for dead zones
            }

            drawRoundRect(
                color = cellColor.copy(alpha = alpha),
                topLeft = Offset(c * cellWidth, r * cellHeight),
                size = Size(cellWidth + 1f, cellHeight + 1f),
                cornerRadius = CornerRadius(4f, 4f)
            )
        }
    }
}

/**
 * Draws architectural partitions, room zone borders, and labels.
 */
private fun DrawScope.drawArchitecturalBlueprint(
    zones: List<RoomZone>,
    canvasWidth: Float,
    canvasHeight: Float,
    textMeasurer: TextMeasurer
) {
    val wallColor = Color(0xFF334155)
    val dashedEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)

    for (zone in zones) {
        val left = zone.left * canvasWidth
        val top = zone.top * canvasHeight
        val width = (zone.right - zone.left) * canvasWidth
        val height = (zone.bottom - zone.top) * canvasHeight

        // Subtle room background tint
        drawRoundRect(
            color = Color(0xFF1E293B).copy(alpha = 0.25f),
            topLeft = Offset(left, top),
            size = Size(width, height),
            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
        )

        // Architectural room border
        drawRoundRect(
            color = wallColor,
            topLeft = Offset(left, top),
            size = Size(width, height),
            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
            style = Stroke(width = 1.5.dp.toPx(), pathEffect = dashedEffect)
        )

        // Room label text
        val textLayout = textMeasurer.measure(
            text = zone.name.uppercase(),
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF94A3B8)
            )
        )
        drawText(
            textLayoutResult = textLayout,
            topLeft = Offset(left + 8.dp.toPx(), top + 6.dp.toPx())
        )
    }
}

/**
 * Draws an interactive waypoint pin with glowing ring and RSSI label.
 */
private fun DrawScope.drawSurveyPointPin(
    point: SurveyPoint,
    index: Int,
    canvasWidth: Float,
    canvasHeight: Float,
    textMeasurer: TextMeasurer
) {
    val px = point.x * canvasWidth
    val py = point.y * canvasHeight
    val pinColor = getRssiColor(point.rssi.toFloat())

    // Outer glow aura
    drawCircle(
        color = pinColor.copy(alpha = 0.25f),
        radius = 16.dp.toPx(),
        center = Offset(px, py)
    )

    // Inner pin ring
    drawCircle(
        color = Color(0xFF0F172A),
        radius = 10.dp.toPx(),
        center = Offset(px, py),
        style = Fill
    )
    drawCircle(
        color = pinColor,
        radius = 10.dp.toPx(),
        center = Offset(px, py),
        style = Stroke(width = 2.dp.toPx())
    )

    // Pin index number inside ring
    val indexLayout = textMeasurer.measure(
        text = index.toString(),
        style = TextStyle(
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    )
    drawText(
        textLayoutResult = indexLayout,
        topLeft = Offset(px - indexLayout.size.width / 2f, py - indexLayout.size.height / 2f)
    )

    // RSSI Tag pill floating below
    val rssiText = "${point.rssi} dBm"
    val rssiLayout = textMeasurer.measure(
        text = rssiText,
        style = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = pinColor
        )
    )

    val tagWidth = rssiLayout.size.width + 12f
    val tagHeight = rssiLayout.size.height + 4f
    val tagX = px - tagWidth / 2f
    val tagY = py + 12.dp.toPx()

    drawRoundRect(
        color = Color(0xFF0B1120).copy(alpha = 0.9f),
        topLeft = Offset(tagX, tagY),
        size = Size(tagWidth, tagHeight),
        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
    )
    drawRoundRect(
        color = pinColor.copy(alpha = 0.6f),
        topLeft = Offset(tagX, tagY),
        size = Size(tagWidth, tagHeight),
        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
        style = Stroke(width = 1f)
    )
    drawText(
        textLayoutResult = rssiLayout,
        topLeft = Offset(tagX + 6f, tagY + 2f)
    )
}

/**
 * Draws a pulsing recommendation beacon where a mesh AP should be placed.
 */
private fun DrawScope.drawRecommendedMeshNode(
    meshOffset: Offset,
    canvasWidth: Float,
    canvasHeight: Float,
    pulseScale: Float,
    textMeasurer: TextMeasurer
) {
    val mx = meshOffset.x * canvasWidth
    val my = meshOffset.y * canvasHeight

    // Pulsing remediation halo
    drawCircle(
        color = SecondaryContainerEmerald.copy(alpha = 0.25f / pulseScale),
        radius = 24.dp.toPx() * pulseScale,
        center = Offset(mx, my)
    )

    // Emerald Router Node Ring
    drawCircle(
        color = Color(0xFF0A0F1D),
        radius = 12.dp.toPx(),
        center = Offset(mx, my),
        style = Fill
    )
    drawCircle(
        color = SecondaryContainerEmerald,
        radius = 12.dp.toPx(),
        center = Offset(mx, my),
        style = Stroke(width = 2.5.dp.toPx())
    )

    // Center star symbol
    val starLayout = textMeasurer.measure(
        text = "AP+",
        style = TextStyle(
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = SecondaryContainerEmerald
        )
    )
    drawText(
        textLayoutResult = starLayout,
        topLeft = Offset(mx - starLayout.size.width / 2f, my - starLayout.size.height / 2f)
    )

    // Banner label
    val bannerText = "RECOMMENDED MESH NODE"
    val bannerLayout = textMeasurer.measure(
        text = bannerText,
        style = TextStyle(
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold,
            color = SecondaryContainerEmerald
        )
    )
    val bWidth = bannerLayout.size.width + 10f
    val bHeight = bannerLayout.size.height + 4f
    val bX = mx - bWidth / 2f
    val bY = my - 24.dp.toPx()

    drawRoundRect(
        color = Color(0xFF022C22).copy(alpha = 0.9f),
        topLeft = Offset(bX, bY),
        size = Size(bWidth, bHeight),
        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
    )
    drawRoundRect(
        color = SecondaryContainerEmerald,
        topLeft = Offset(bX, bY),
        size = Size(bWidth, bHeight),
        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
        style = Stroke(width = 1f)
    )
    drawText(
        textLayoutResult = bannerLayout,
        topLeft = Offset(bX + 5f, bY + 2f)
    )
}

/**
 * Draws the active placement reticle cursor that tracks user touch.
 */
private fun DrawScope.drawPlacementCursor(
    cursorX: Float,
    cursorY: Float,
    canvasWidth: Float,
    canvasHeight: Float,
    pulseScale: Float
) {
    val cx = cursorX * canvasWidth
    val cy = cursorY * canvasHeight

    // Reticle crosshair lines
    val lineLen = 14.dp.toPx()
    drawLine(
        color = NeonCyan.copy(alpha = 0.7f),
        start = Offset(cx - lineLen, cy),
        end = Offset(cx + lineLen, cy),
        strokeWidth = 1.5.dp.toPx()
    )
    drawLine(
        color = NeonCyan.copy(alpha = 0.7f),
        start = Offset(cx, cy - lineLen),
        end = Offset(cx, cy + lineLen),
        strokeWidth = 1.5.dp.toPx()
    )

    // Outer reticle circle
    drawCircle(
        color = NeonCyan.copy(alpha = 0.4f),
        radius = 14.dp.toPx() * (0.9f + pulseScale * 0.15f),
        center = Offset(cx, cy),
        style = Stroke(width = 1.5.dp.toPx())
    )

    // Center focal dot
    drawCircle(
        color = NeonCyan,
        radius = 3.dp.toPx(),
        center = Offset(cx, cy),
        style = Fill
    )
}

/**
 * Returns color mapping according to RF signal strength benchmarks.
 */
private fun getRssiColor(rssi: Float): Color {
    return when {
        rssi >= -55f -> SecondaryContainerEmerald // Optimal (-55 dBm and better)
        rssi >= -65f -> NeonCyan // Good (-65 to -56 dBm)
        rssi >= -74f -> TertiaryContainerAmber // Marginal / High Jitter (-74 to -66 dBm)
        else -> ErrorRed // Severe Dead Zone (<= -75 dBm)
    }
}

@Composable
private fun HeatmapLegendBar() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "RF SIGNAL HEATMAP SCALE",
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "IDW Path Loss p=2.0",
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Multi-color gradient strip
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            ErrorRed,
                            TertiaryContainerAmber,
                            NeonCyan,
                            SecondaryContainerEmerald
                        )
                    )
                )
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Dead Zone (≤-75)", fontSize = 8.sp, color = ErrorRed, fontWeight = FontWeight.Bold)
            Text(text = "Weak (-70)", fontSize = 8.sp, color = TertiaryContainerAmber)
            Text(text = "Good (-60)", fontSize = 8.sp, color = NeonCyan)
            Text(text = "Optimal (≥-50 dBm)", fontSize = 8.sp, color = SecondaryContainerEmerald, fontWeight = FontWeight.Bold)
        }
    }
}
