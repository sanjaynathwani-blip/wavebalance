package com.wavebalance.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wavebalance.app.model.AccessPoint
import com.wavebalance.app.model.FrequencyBand
import com.wavebalance.app.ui.theme.DarkSurfaceContainer
import com.wavebalance.app.ui.theme.DarkSurfaceContainerHigh
import com.wavebalance.app.ui.theme.PrimaryContainerBlue
import com.wavebalance.app.ui.theme.SecondaryContainerEmerald
import com.wavebalance.app.ui.theme.TertiaryContainerAmber

@Composable
fun SpectrumMiniWaterfall(
    accessPoints: List<AccessPoint>,
    activeBand: FrequencyBand = FrequencyBand.BAND_5_GHZ,
    modifier: Modifier = Modifier
) {
    val apsInBand = accessPoints.filter { it.band == activeBand }
    val channels = if (activeBand == FrequencyBand.BAND_5_GHZ) {
        listOf(36, 40, 44, 48, 52, 100, 149, 153, 157, 161)
    } else {
        listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)
    }

    val channelCounts = channels.associateWith { ch ->
        apsInBand.count { it.channel == ch }
    }

    val maxCount = (channelCounts.values.maxOrNull() ?: 1).coerceAtLeast(1)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Equalizer,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "Spectrum Congestion Preview",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = PrimaryContainerBlue.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = activeBand.label,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = PrimaryContainerBlue,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bars Row
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.background
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    channels.forEach { ch ->
                        val count = channelCounts[ch] ?: 0
                        val heightFraction = when {
                            count == 0 -> 0.12f
                            else -> ((count.toFloat() / maxCount) * 0.88f + 0.12f).coerceIn(0.12f, 1f)
                        }

                        val barColor = when {
                            count == 0 -> DarkSurfaceContainerHigh
                            count == 1 -> SecondaryContainerEmerald
                            count == 2 -> PrimaryContainerBlue
                            else -> TertiaryContainerAmber
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(heightFraction)
                                .padding(horizontal = 2.dp)
                                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                .background(barColor)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Axis labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (activeBand == FrequencyBand.BAND_5_GHZ) "Ch 36 (UNII-1)" else "Ch 1",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (apsInBand.any { channelCounts[it.channel] ?: 0 > 1 }) "Co-channel detected" else "Clear channels available",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (apsInBand.any { channelCounts[it.channel] ?: 0 > 1 }) TertiaryContainerAmber else SecondaryContainerEmerald
                )
                Text(
                    text = if (activeBand == FrequencyBand.BAND_5_GHZ) "Ch 161 (UNII-3)" else "Ch 11",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
