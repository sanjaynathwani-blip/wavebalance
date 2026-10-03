package com.wavebalance.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Wifi1Bar
import androidx.compose.material.icons.filled.Wifi2Bar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.wavebalance.app.ui.theme.PrimaryContainerBlue
import com.wavebalance.app.ui.theme.SecondaryContainerEmerald
import com.wavebalance.app.ui.theme.TertiaryContainerAmber

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AccessPointItemCard(
    ap: AccessPoint,
    onToggleHomeTag: () -> Unit,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val signalColor = when {
        ap.rssi >= -60 -> SecondaryContainerEmerald
        ap.rssi >= -72 -> PrimaryContainerBlue
        ap.rssi >= -82 -> TertiaryContainerAmber
        else -> Color(0xFFEF4444)
    }

    val signalIcon = when {
        ap.rssi >= -65 -> Icons.Default.Wifi
        ap.rssi >= -78 -> Icons.Default.Wifi2Bar
        else -> Icons.Default.Wifi1Bar
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row: SSID + Signal Badge + Home Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(signalColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = signalIcon,
                            contentDescription = "Signal Strength",
                            tint = signalColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = ap.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            if (ap.isConnected) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = SecondaryContainerEmerald.copy(alpha = 0.2f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Connected",
                                            tint = SecondaryContainerEmerald,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "Active",
                                            fontSize = 10.sp,
                                            color = SecondaryContainerEmerald
                                        )
                                    }
                                }
                            }
                        }
                        Text(
                            text = ap.bssid,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Signal dBm Badge & Home Toggle
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = signalColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "${ap.rssi} dBm",
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = FontFamily.Monospace,
                            color = signalColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    IconButton(onClick = onToggleHomeTag) {
                        Icon(
                            imageVector = if (ap.isUserTaggedHome) Icons.Default.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Tag as Home AP",
                            tint = if (ap.isUserTaggedHome) TertiaryContainerAmber else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Badges row: Channel, Band, Wi-Fi Gen, Bandwidth, Security, SNR
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Band Chip
                val bandColor = when (ap.band) {
                    FrequencyBand.BAND_6_GHZ -> Color(0xFFC084FC) // Purple
                    FrequencyBand.BAND_5_GHZ -> PrimaryContainerBlue // Cyan
                    FrequencyBand.BAND_2_4_GHZ -> TertiaryContainerAmber // Amber
                    else -> MaterialTheme.colorScheme.outline
                }
                TagPill(text = ap.band.label, color = bandColor)

                // Channel Chip
                TagPill(
                    text = "Ch ${ap.channel} (${ap.frequencyMhz} MHz)",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Generation Chip
                if (ap.standard.generation.isNotBlank()) {
                    TagPill(
                        text = ap.standard.generation,
                        color = PrimaryContainerBlue
                    )
                }

                // Bandwidth Chip
                TagPill(
                    text = ap.channelWidth.label,
                    color = MaterialTheme.colorScheme.outline
                )

                // SNR Chip
                TagPill(
                    text = "SNR: ${ap.snr} dB",
                    color = SecondaryContainerEmerald
                )

                // Security Chip
                TagPill(
                    text = ap.securityType,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun TagPill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}
