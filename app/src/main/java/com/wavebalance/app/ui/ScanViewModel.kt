package com.wavebalance.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wavebalance.app.data.ScanStatus
import com.wavebalance.app.data.WifiScanEngine
import com.wavebalance.app.model.AccessPoint
import com.wavebalance.app.model.ActiveConnectionInfo
import com.wavebalance.app.model.FrequencyBand
import com.wavebalance.app.model.RssiSample
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

class ScanViewModel(application: Application) : AndroidViewModel(application) {

    val engine = WifiScanEngine(application)

    val activeConnection: StateFlow<ActiveConnectionInfo?> = engine.activeConnection
    val scanStatus: StateFlow<ScanStatus> = engine.scanStatus
    val isMockMode: StateFlow<Boolean> = engine.isMockMode

    private val _selectedBandFilter = MutableStateFlow<FrequencyBand?>(null)
    val selectedBandFilter: StateFlow<FrequencyBand?> = _selectedBandFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedAp = MutableStateFlow<AccessPoint?>(null)
    val selectedAp: StateFlow<AccessPoint?> = _selectedAp.asStateFlow()

    private val _rssiHistory = MutableStateFlow<Map<String, List<RssiSample>>>(emptyMap())
    val rssiHistory: StateFlow<Map<String, List<RssiSample>>> = _rssiHistory.asStateFlow()

    private val _roamingHistory = MutableStateFlow<List<com.wavebalance.app.model.RoamingEvent>>(emptyList())
    val roamingHistory: StateFlow<List<com.wavebalance.app.model.RoamingEvent>> = _roamingHistory.asStateFlow()

    private var lastActiveConn: ActiveConnectionInfo? = null

    val stickyClientAlert: StateFlow<com.wavebalance.app.model.StickyClientAlert?> = combine(
        activeConnection,
        engine.accessPoints
    ) { conn, aps ->
        com.wavebalance.app.model.RoamingMonitorEngine.evaluateStickyClient(conn, aps)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val filteredAccessPoints: StateFlow<List<AccessPoint>> = combine(
        engine.accessPoints,
        _selectedBandFilter,
        _searchQuery
    ) { aps, band, query ->
        aps.filter { ap ->
            val matchesBand = band == null || ap.band == band
            val matchesQuery = query.isBlank() ||
                    ap.ssid.contains(query, ignoreCase = true) ||
                    ap.bssid.contains(query, ignoreCase = true)
            matchesBand && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalApCount: StateFlow<Int> = engine.accessPoints
        .combine(MutableStateFlow(Unit)) { aps, _ -> aps.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        // Start background sampling loop for historical sparklines
        startRssiSampler()

        // Monitor roaming handovers
        viewModelScope.launch {
            activeConnection.collect { current ->
                val prev = lastActiveConn
                if (prev != null && current != null) {
                    val event = com.wavebalance.app.model.RoamingMonitorEngine.detectRoamingTransition(prev, current)
                    if (event != null) {
                        _roamingHistory.value = _roamingHistory.value + event
                    }
                }
                lastActiveConn = current
            }
        }
    }

    private fun startRssiSampler() {
        viewModelScope.launch {
            while (isActive) {
                delay(3000)
                recordCurrentSamples()
            }
        }
    }

    private fun recordCurrentSamples() {
        val now = System.currentTimeMillis()
        val currentHistory = _rssiHistory.value.toMutableMap()

        // 1. Record active connection if present
        val active = activeConnection.value
        if (active != null && active.bssid.isNotBlank()) {
            val history = currentHistory[active.bssid]?.toMutableList() ?: mutableListOf()
            // In mock mode or real, apply small realistic variance to demonstrate live sparklines
            val variance = if (isMockMode.value) Random.nextInt(-2, 3) else 0
            val reading = (active.rssi + variance).coerceIn(-95, -30)
            history.add(RssiSample(now, reading))
            if (history.size > 20) history.removeAt(0)
            currentHistory[active.bssid] = history
        }

        // 2. Also record selected AP if different
        val selected = _selectedAp.value
        if (selected != null && selected.bssid != active?.bssid) {
            val history = currentHistory[selected.bssid]?.toMutableList() ?: mutableListOf()
            val variance = Random.nextInt(-2, 3)
            val reading = (selected.rssi + variance).coerceIn(-95, -30)
            history.add(RssiSample(now, reading))
            if (history.size > 20) history.removeAt(0)
            currentHistory[selected.bssid] = history
        }

        _rssiHistory.value = currentHistory
    }

    fun selectAccessPoint(ap: AccessPoint?) {
        _selectedAp.value = ap
        if (ap != null) {
            // Seed initial samples if empty
            val current = _rssiHistory.value
            if (current[ap.bssid].isNullOrEmpty()) {
                val now = System.currentTimeMillis()
                val initial = (0 until 12).map { i ->
                    val offset = (11 - i) * 3000L
                    val variance = Random.nextInt(-2, 3)
                    RssiSample(now - offset, (ap.rssi + variance).coerceIn(-95, -30))
                }
                val mutable = current.toMutableMap()
                mutable[ap.bssid] = initial
                _rssiHistory.value = mutable
            }
        }
    }

    fun hasPermissions(): Boolean = engine.hasPermissions()
    fun isLocationServiceEnabled(): Boolean = engine.isLocationServiceEnabled()

    fun triggerScan() {
        engine.triggerScan()
    }

    fun setBandFilter(band: FrequencyBand?) {
        _selectedBandFilter.value = band
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleMockMode(enabled: Boolean) {
        engine.setMockMode(enabled)
    }

    fun toggleHomeTag(bssid: String) {
        engine.toggleHomeTag(bssid)
    }

    fun simulateChannelMigration(newChannel: Int, newWidth: com.wavebalance.app.model.ChannelWidth = com.wavebalance.app.model.ChannelWidth.WIDTH_80) {
        engine.simulateChannelMigration(newChannel, newWidth)
    }

    fun simulateRoamToCandidate() {
        val alert = stickyClientAlert.value
        if (alert != null && alert.candidateBssid.isNotBlank()) {
            engine.simulateRoam(alert.candidateBssid, alert.candidateChannel, alert.candidateBand, alert.candidateRssi)
        }
    }

    fun simulateWalkDegradation() {
        engine.simulateWalkDegradation()
    }

    fun shareAuditReport(context: android.content.Context) {
        val recommendation = com.wavebalance.app.model.ChannelOptimizerEngine.evaluateBand(
            band = activeConnection.value?.band ?: FrequencyBand.BAND_5_GHZ,
            allAps = engine.accessPoints.value,
            currentChannel = activeConnection.value?.channel ?: 36
        )
        val markdown = com.wavebalance.app.model.RfAuditReportGenerator.generateMarkdownReport(
            activeConnection = activeConnection.value,
            allAps = engine.accessPoints.value,
            recommendation = recommendation
        )
        val sendIntent = android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            putExtra(android.content.Intent.EXTRA_TEXT, markdown)
            putExtra(android.content.Intent.EXTRA_SUBJECT, "WaveBalance RF Airspace Audit - ${activeConnection.value?.cleanSsid ?: "Wi-Fi"}")
            type = "text/plain"
        }
        val shareIntent = android.content.Intent.createChooser(sendIntent, "Share RF Airspace Audit Report")
        shareIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    }

    override fun onCleared() {
        super.onCleared()
        engine.unregister()
    }
}
