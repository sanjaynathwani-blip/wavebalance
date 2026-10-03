package com.wavebalance.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppDestination(
    val label: String,
    val icon: ImageVector,
    val contentDescription: String
) {
    DASHBOARD(
        label = "Dashboard",
        icon = Icons.Default.GridView,
        contentDescription = "WaveBalance Dashboard"
    ),
    RADAR(
        label = "Radar",
        icon = Icons.Default.WifiTethering,
        contentDescription = "Spectrum Radar & APs"
    ),
    DETAILS(
        label = "AP Details",
        icon = Icons.Default.Router,
        contentDescription = "Access Point Deep Dive"
    ),
    OPTIMIZER(
        label = "Optimizer",
        icon = Icons.Default.AutoFixHigh,
        contentDescription = "Channel Optimizer"
    )
}
