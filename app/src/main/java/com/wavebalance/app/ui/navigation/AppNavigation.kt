package com.wavebalance.app.ui.navigation

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wavebalance.app.model.ActiveConnectionInfo
import com.wavebalance.app.ui.theme.DarkSurfaceContainer
import com.wavebalance.app.ui.theme.DarkSurfaceContainerHigh
import com.wavebalance.app.ui.theme.DarkSurfaceContainerLow
import com.wavebalance.app.ui.theme.SignalExcellent
import com.wavebalance.app.ui.theme.SignalFair
import com.wavebalance.app.ui.theme.SignalGood
import com.wavebalance.app.ui.theme.SignalWeak
import com.wavebalance.app.ui.theme.TertiaryContainerAmber

val NavigationPanelWidth = 248.dp

/**
 * Permanent left navigation panel for laptop and desktop windows:
 * app identity, grouped destinations with shortcut hints, the current
 * connection and the simulated-data switch.
 */
@Composable
fun NavigationPanel(
    current: AppDestination,
    collisionCount: Int,
    activeConnection: ActiveConnectionInfo?,
    isMockMode: Boolean,
    onNavigate: (AppDestination) -> Unit,
    onMockModeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier
                .width(NavigationPanelWidth)
                .fillMaxHeight()
                .background(DarkSurfaceContainerLow)
                .padding(horizontal = 12.dp, vertical = 16.dp)
        ) {
            AppIdentity(modifier = Modifier.padding(horizontal = 8.dp))
            Spacer(modifier = Modifier.height(20.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                DestinationGroup.entries.forEach { group ->
                    Text(
                        text = group.label.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 6.dp)
                    )
                    AppDestination.entries.filter { it.group == group }.forEach { destination ->
                        NavigationDrawerItem(
                            icon = {
                                Icon(destination.icon, contentDescription = null, modifier = Modifier.size(20.dp))
                            },
                            label = {
                                Text(destination.label, style = MaterialTheme.typography.labelLarge)
                            },
                            badge = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (destination == AppDestination.OPTIMIZER && collisionCount > 0) {
                                        CollisionBadge(collisionCount)
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    ShortcutHint(destination.shortcut)
                                }
                            },
                            selected = destination == current,
                            onClick = { onNavigate(destination) },
                            shape = RoundedCornerShape(12.dp),
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f),
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedContainerColor = Color.Transparent,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier
                                .height(44.dp)
                                .padding(vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            ConnectionSummary(activeConnection)
            Spacer(modifier = Modifier.height(8.dp))
            SimulationToggle(isMockMode = isMockMode, onMockModeChange = onMockModeChange)
        }
        VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    }
}

/**
 * Navigation rail for medium windows (foldables, narrow desktop windows).
 */
@Composable
fun AppNavigationRail(
    current: AppDestination,
    collisionCount: Int,
    onNavigate: (AppDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationRail(
        modifier = modifier,
        containerColor = DarkSurfaceContainer,
        header = {
            AppLogo(modifier = Modifier.padding(vertical = 8.dp))
        }
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        AppDestination.entries.forEach { destination ->
            NavigationRailItem(
                icon = { DestinationIcon(destination, collisionCount) },
                label = { Text(destination.label, maxLines = 1) },
                selected = destination == current,
                onClick = { onNavigate(destination) },
                colors = NavigationRailItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

/**
 * Bottom navigation bar for phones.
 */
@Composable
fun AppBottomBar(
    current: AppDestination,
    collisionCount: Int,
    onNavigate: (AppDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = DarkSurfaceContainer
    ) {
        AppDestination.entries.filter { it.inBottomBar }.forEach { destination ->
            NavigationBarItem(
                icon = { DestinationIcon(destination, collisionCount) },
                label = { Text(destination.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                selected = destination == current,
                onClick = { onNavigate(destination) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

@Composable
fun AppLogo(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Radar,
            contentDescription = "WaveBalance",
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun AppIdentity(modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        AppLogo()
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = "WaveBalance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Wi-Fi analyzer",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DestinationIcon(destination: AppDestination, collisionCount: Int) {
    if (destination == AppDestination.OPTIMIZER && collisionCount > 0) {
        BadgedBox(badge = { CollisionBadge(collisionCount) }) {
            Icon(destination.icon, contentDescription = destination.contentDescription)
        }
    } else {
        Icon(destination.icon, contentDescription = destination.contentDescription)
    }
}

@Composable
private fun CollisionBadge(count: Int) {
    Badge(containerColor = TertiaryContainerAmber, contentColor = Color.Black) {
        Text("$count")
    }
}

@Composable
private fun ShortcutHint(key: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(DarkSurfaceContainerHigh)
            .padding(horizontal = 6.dp, vertical = 1.dp)
    ) {
        Text(
            text = key,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ConnectionSummary(activeConnection: ActiveConnectionInfo?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurfaceContainer)
            .padding(12.dp)
    ) {
        Text(
            text = "CONNECTED TO",
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (activeConnection == null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.WifiOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "No Wi-Fi connection",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val signalColor = when {
                activeConnection.rssi >= -50 -> SignalExcellent
                activeConnection.rssi >= -65 -> SignalGood
                activeConnection.rssi >= -75 -> SignalFair
                else -> SignalWeak
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Wifi,
                    contentDescription = null,
                    tint = signalColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = activeConnection.cleanSsid.ifBlank { "Hidden network" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "${activeConnection.band.label} · Ch ${activeConnection.channel}",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${activeConnection.rssi} dBm",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = signalColor
                )
            }
        }
    }
}

@Composable
private fun SimulationToggle(isMockMode: Boolean, onMockModeChange: (Boolean) -> Unit) {
    Column {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
            modifier = Modifier.padding(vertical = 4.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Simulated data",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isMockMode) TertiaryContainerAmber else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isMockMode) "Showing demo networks" else "Live Wi-Fi scans",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = isMockMode,
                onCheckedChange = onMockModeChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = TertiaryContainerAmber,
                    checkedTrackColor = TertiaryContainerAmber.copy(alpha = 0.3f)
                )
            )
        }
    }
}
