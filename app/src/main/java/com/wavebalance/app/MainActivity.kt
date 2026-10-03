package com.wavebalance.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wavebalance.app.ui.ScanViewModel
import com.wavebalance.app.ui.WifiScanScreen
import com.wavebalance.app.ui.components.PermissionRationaleModal
import com.wavebalance.app.ui.navigation.AppDestination
import com.wavebalance.app.ui.screens.DashboardScreen
import com.wavebalance.app.ui.theme.DarkSurfaceContainer
import com.wavebalance.app.ui.theme.DarkSurfaceContainerHigh
import com.wavebalance.app.ui.theme.PrimaryContainerBlue
import com.wavebalance.app.ui.theme.SecondaryContainerEmerald
import com.wavebalance.app.ui.theme.TertiaryContainerAmber
import com.wavebalance.app.ui.theme.WaveBalanceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            WaveBalanceTheme {
                WaveBalanceAdaptiveApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaveBalanceAdaptiveApp(
    viewModel: ScanViewModel = viewModel()
) {
    var currentDestination by remember { mutableStateOf(AppDestination.DASHBOARD) }
    var showPermissionModal by remember { mutableStateOf(false) }

    val allAps by viewModel.filteredAccessPoints.collectAsState()
    val activeConn by viewModel.activeConnection.collectAsState()
    val isMockMode by viewModel.isMockMode.collectAsState()

    val collisionCount = remember(allAps, activeConn) {
        val ch = activeConn?.channel ?: -1
        if (ch > 0) allAps.count { it.channel == ch && !it.isConnected } else 0
    }

    val requiredPermissions = remember {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
        permissions.toTypedArray()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        if (allGranted) {
            viewModel.triggerScan()
        }
    }

    LaunchedEffect(Unit) {
        if (!viewModel.hasPermissions()) {
            showPermissionModal = true
        } else {
            viewModel.triggerScan()
        }
    }

    val navSuiteColors = NavigationSuiteDefaults.colors(
        navigationBarContainerColor = DarkSurfaceContainer,
        navigationBarContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        navigationRailContainerColor = DarkSurfaceContainer,
        navigationRailContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    NavigationSuiteScaffold(
        navigationSuiteColors = navSuiteColors,
        navigationSuiteItems = {
            AppDestination.entries.forEach { destination ->
                item(
                    icon = {
                        if (destination == AppDestination.OPTIMIZER && collisionCount > 0) {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        containerColor = TertiaryContainerAmber,
                                        contentColor = Color.Black
                                    ) {
                                        Text("$collisionCount")
                                    }
                                }
                            ) {
                                Icon(destination.icon, contentDescription = destination.contentDescription)
                            }
                        } else {
                            Icon(destination.icon, contentDescription = destination.contentDescription)
                        }
                    },
                    label = { Text(destination.label) },
                    selected = currentDestination == destination,
                    onClick = { currentDestination = destination }
                )
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top App Bar
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
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
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "WaveBalance",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isMockMode) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = CircleShape,
                                            color = TertiaryContainerAmber.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "MOCK",
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = TertiaryContainerAmber,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = when (currentDestination) {
                                        AppDestination.DASHBOARD -> "Wi-Fi Dashboard & RF Health"
                                        AppDestination.RADAR -> "Spectrum Scanner & Radar"
                                        AppDestination.DETAILS -> "Access Point Deep Dive"
                                        AppDestination.OPTIMIZER -> "Channel Interference Optimizer"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    actions = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Sim",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isMockMode) TertiaryContainerAmber else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            androidx.compose.material3.Switch(
                                checked = isMockMode,
                                onCheckedChange = { viewModel.toggleMockMode(it) },
                                modifier = Modifier.height(28.dp),
                                colors = androidx.compose.material3.SwitchDefaults.colors(
                                    checkedThumbColor = TertiaryContainerAmber,
                                    checkedTrackColor = TertiaryContainerAmber.copy(alpha = 0.3f)
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(onClick = { viewModel.triggerScan() }) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh Scan",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )

                // Body content based on destination
                Box(modifier = Modifier.weight(1f)) {
                    when (currentDestination) {
                        AppDestination.DASHBOARD -> {
                            DashboardScreen(
                                viewModel = viewModel,
                                onNavigateToRadar = { currentDestination = AppDestination.RADAR },
                                onNavigateToOptimizer = { currentDestination = AppDestination.OPTIMIZER },
                                onNavigateToDetails = {
                                    val activeBssid = activeConn?.bssid
                                    val target = allAps.find { it.bssid.equals(activeBssid, ignoreCase = true) } ?: allAps.firstOrNull()
                                    viewModel.selectAccessPoint(target)
                                    currentDestination = AppDestination.DETAILS
                                }
                            )
                        }
                        AppDestination.RADAR -> {
                            WifiScanScreen(
                                viewModel = viewModel,
                                onRequestPermissions = {
                                    showPermissionModal = false
                                    permissionLauncher.launch(requiredPermissions)
                                },
                                onNavigateToDetails = { ap ->
                                    viewModel.selectAccessPoint(ap)
                                    currentDestination = AppDestination.DETAILS
                                }
                            )
                        }
                        AppDestination.DETAILS -> {
                            com.wavebalance.app.ui.screens.ApDetailScreen(
                                viewModel = viewModel,
                                onNavigateBack = { currentDestination = AppDestination.RADAR }
                            )
                        }
                        AppDestination.OPTIMIZER -> {
                            com.wavebalance.app.ui.screens.OptimizerScreen(
                                viewModel = viewModel,
                                onNavigateToRadar = { currentDestination = AppDestination.RADAR }
                            )
                        }
                    }
                }
            }

            if (showPermissionModal) {
                PermissionRationaleModal(
                    onGrantClicked = {
                        showPermissionModal = false
                        permissionLauncher.launch(requiredPermissions)
                    },
                    onDismiss = {
                        showPermissionModal = false
                        viewModel.toggleMockMode(true)
                    }
                )
            }
        }
    }
}

@Composable
fun DestinationPlaceholderScreen(
    title: String,
    description: String,
    actionText: String,
    onAction: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = PrimaryContainerBlue,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(20.dp))
                androidx.compose.material3.Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = PrimaryContainerBlue,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Text(text = actionText, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
