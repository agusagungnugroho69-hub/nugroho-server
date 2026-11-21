package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.SettingsDataStore
import com.example.data.model.AlertLogEntity
import com.example.data.model.ConnectionTestResult
import com.example.data.model.PppoeClient
import com.example.data.model.RouterConfig
import com.example.data.model.RouterTelemetry
import com.example.data.model.TelegramSettings
import com.example.data.model.TrafficSample
import com.example.data.network.MikroTikDataSource
import com.example.data.network.TelegramNotifier
import com.example.data.repository.AndroidNotificationHelper
import com.example.data.repository.MonitoringRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class DashboardUiState(
    val routers: List<RouterConfig> = emptyList(),
    val telemetryByRouter: Map<Long, RouterTelemetry> = emptyMap(),
    val trafficHistoryByRouter: Map<Long, List<TrafficSample>> = emptyMap(),
    val aggregateTrafficHistory: List<TrafficSample> = emptyList(),
    val selectedGraphRouterId: Long? = null,
    val totalRouters: Int = 0,
    val onlineRouters: Int = 0,
    val offlineRouters: Int = 0,
    val totalCustomers: Int = 0,
    val onlineCustomers: Int = 0,
    val offlineCustomers: Int = 0,
    val totalRxBps: Long = 0L,
    val totalTxBps: Long = 0L,
    val allPppoeClients: List<PppoeClient> = emptyList(),
    val isRefreshing: Boolean = false,
    val isPollingLive: Boolean = true,
    val hasAnySimulatedRouter: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val settingsStore = SettingsDataStore(application)
    private val notificationHelper = AndroidNotificationHelper(application)
    private val repository = MonitoringRepository(
        routerDao = database.routerDao(),
        alertLogDao = database.alertLogDao(),
        settingsDataStore = settingsStore,
        dataSource = MikroTikDataSource(),
        telegramNotifier = TelegramNotifier(),
        notificationHelper = notificationHelper
    )

    val settings: StateFlow<TelegramSettings> = repository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TelegramSettings())

    val alertLogs: StateFlow<List<AlertLogEntity>> = repository.alertLogsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _telemetryMap = MutableStateFlow<Map<Long, RouterTelemetry>>(emptyMap())
    private val _trafficHistoryMap = MutableStateFlow<Map<Long, List<TrafficSample>>>(emptyMap())
    private val _aggregateHistory = MutableStateFlow<List<TrafficSample>>(emptyList())
    private val _selectedGraphRouterId = MutableStateFlow<Long?>(null)
    private val _isRefreshing = MutableStateFlow(false)
    private val _isPollingLive = MutableStateFlow(true)

    private val _isTestingConnection = MutableStateFlow(false)
    val isTestingConnection: StateFlow<Boolean> = _isTestingConnection.asStateFlow()

    private val _connectionTestResult = MutableStateFlow<ConnectionTestResult?>(null)
    val connectionTestResult: StateFlow<ConnectionTestResult?> = _connectionTestResult.asStateFlow()

    private val _isTestingTelegram = MutableStateFlow(false)
    val isTestingTelegram: StateFlow<Boolean> = _isTestingTelegram.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    private var pollingJob: Job? = null

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.routersFlow,
        _telemetryMap,
        _trafficHistoryMap,
        _aggregateHistory,
        _selectedGraphRouterId,
        _isRefreshing,
        _isPollingLive
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val routers = values[0] as List<RouterConfig>
        @Suppress("UNCHECKED_CAST")
        val telemetryMap = values[1] as Map<Long, RouterTelemetry>
        @Suppress("UNCHECKED_CAST")
        val historyMap = values[2] as Map<Long, List<TrafficSample>>
        @Suppress("UNCHECKED_CAST")
        val aggHistory = values[3] as List<TrafficSample>
        val selectedId = values[4] as Long?
        val refreshing = values[5] as Boolean
        val pollingLive = values[6] as Boolean

        val onlineRouters = routers.count { telemetryMap[it.id]?.isOnline == true }
        val offlineRouters = routers.size - onlineRouters
        val activeCustomers = routers.sumOf { telemetryMap[it.id]?.activePppoeCount ?: 0 }
        val totalCustomers = routers.sumOf { telemetryMap[it.id]?.totalSecretCount ?: 0 }
        val offlineCustomers = (totalCustomers - activeCustomers).coerceAtLeast(0)
        val totalRx = routers.sumOf { r ->
            val t = telemetryMap[r.id]
            if (t?.isOnline == true) t.rxBitsPerSec else 0L
        }
        val totalTx = routers.sumOf { r ->
            val t = telemetryMap[r.id]
            if (t?.isOnline == true) t.txBitsPerSec else 0L
        }
        val allClients = routers.flatMap { r -> telemetryMap[r.id]?.pppoeClients.orEmpty() }

        DashboardUiState(
            routers = routers,
            telemetryByRouter = telemetryMap,
            trafficHistoryByRouter = historyMap,
            aggregateTrafficHistory = aggHistory,
            selectedGraphRouterId = selectedId?.takeIf { id -> routers.any { it.id == id } },
            totalRouters = routers.size,
            onlineRouters = onlineRouters,
            offlineRouters = offlineRouters,
            totalCustomers = totalCustomers,
            onlineCustomers = activeCustomers,
            offlineCustomers = offlineCustomers,
            totalRxBps = totalRx,
            totalTxBps = totalTx,
            allPppoeClients = allClients,
            isRefreshing = refreshing,
            isPollingLive = pollingLive,
            hasAnySimulatedRouter = routers.any { it.isSimulated }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    init {
        startPollingLoop()
    }

    private fun startPollingLoop() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                if (_isPollingLive.value) {
                    val currentRouters = uiState.value.routers
                    if (currentRouters.isNotEmpty()) {
                        executePoll(currentRouters)
                    }
                }
                val intervalSec = settings.value.pollingIntervalSec.coerceIn(2, 60)
                delay(intervalSec * 1000L)
            }
        }
    }

    fun refreshNow() {
        viewModelScope.launch {
            val currentRouters = uiState.value.routers
            if (currentRouters.isEmpty()) {
                _snackbarMessage.tryEmit("Tambahkan minimal satu router MikroTik terlebih dahulu.")
                return@launch
            }
            executePoll(currentRouters)
        }
    }

    private suspend fun executePoll(routers: List<RouterConfig>) {
        if (_isRefreshing.value) return
        _isRefreshing.value = true
        try {
            val (telemetryMap, historyMap) = repository.pollAllRouters(routers)
            _telemetryMap.value = telemetryMap
            _trafficHistoryMap.value = historyMap

            val aggRx = telemetryMap.values.filter { it.isOnline }.sumOf { it.rxBitsPerSec }
            val aggTx = telemetryMap.values.filter { it.isOnline }.sumOf { it.txBitsPerSec }
            val currentAgg = _aggregateHistory.value.toMutableList()
            currentAgg.add(
                TrafficSample(
                    timestampMillis = System.currentTimeMillis(),
                    rxBps = aggRx,
                    txBps = aggTx
                )
            )
            if (currentAgg.size > 36) currentAgg.removeAt(0)
            _aggregateHistory.value = currentAgg
        } finally {
            _isRefreshing.value = false
        }
    }

    fun toggleLivePolling() {
        _isPollingLive.value = !_isPollingLive.value
    }

    fun selectGraphRouter(routerId: Long?) {
        _selectedGraphRouterId.value = routerId
    }

    fun clearConnectionTestResult() {
        _connectionTestResult.value = null
    }

    fun testRouterConnection(router: RouterConfig) {
        viewModelScope.launch {
            _isTestingConnection.value = true
            _connectionTestResult.value = null
            val res = repository.testConnection(router)
            _connectionTestResult.value = res
            _isTestingConnection.value = false
        }
    }

    fun saveRouter(router: RouterConfig, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            val savedId = repository.addOrUpdateRouter(router)
            onSaved()
            _snackbarMessage.tryEmit("Router ${router.name} disimpan.")
            val updated = uiState.value.routers.ifEmpty { listOf(router.copy(id = savedId)) }
            executePoll(updated)
        }
    }

    fun deleteRouter(router: RouterConfig) {
        viewModelScope.launch {
            repository.deleteRouter(router)
            _snackbarMessage.tryEmit("Router ${router.name} dihapus.")
        }
    }

    fun updateMonitoredInterface(router: RouterConfig, ifaceName: String) {
        viewModelScope.launch {
            repository.addOrUpdateRouter(router.copy(monitoredInterface = ifaceName))
            _snackbarMessage.tryEmit("Interface monitor diubah ke $ifaceName")
            refreshNow()
        }
    }

    fun loadDemoLabRouters() {
        viewModelScope.launch {
            repository.loadDemoLabRouters()
            _snackbarMessage.tryEmit("Mode Simulasi Lab aktif — 2 router virtual ditambahkan.")
            delay(200)
            refreshNow()
        }
    }

    fun kickPppoeClient(client: PppoeClient) {
        val router = uiState.value.routers.firstOrNull { it.id == client.routerId } ?: return
        viewModelScope.launch {
            val res = repository.kickPppoeClient(router, client.id)
            if (res.isSuccess) {
                _snackbarMessage.tryEmit("Sesi PPPoE ${client.name} diputuskan.")
                executePoll(uiState.value.routers)
            } else {
                _snackbarMessage.tryEmit("Gagal memutus sesi: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun togglePppoeSecret(client: PppoeClient) {
        val router = uiState.value.routers.firstOrNull { it.id == client.routerId } ?: return
        val secretId = client.secretId ?: client.id
        val newDisabled = !client.isDisabled
        viewModelScope.launch {
            val res = repository.togglePppoeSecret(router, secretId, newDisabled)
            if (res.isSuccess) {
                val label = if (newDisabled) "dinonaktifkan (isolir)" else "diaktifkan kembali"
                _snackbarMessage.tryEmit("Akun ${client.name} $label.")
                executePoll(uiState.value.routers)
            } else {
                _snackbarMessage.tryEmit("Gagal ubah secret: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun inspectRouter(router: RouterConfig) {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val res = repository.pollAllRouters(listOf(router))
                val latest = res.first[router.id]
                if (latest != null) {
                    _telemetryMap.value = _telemetryMap.value.toMutableMap().apply {
                        put(router.id, latest)
                    }
                    _trafficHistoryMap.value = _trafficHistoryMap.value.toMutableMap().apply {
                        res.second[router.id]?.let { put(router.id, it) }
                    }
                    if (latest.isOnline) {
                        _snackbarMessage.tryEmit("Tersinkron ke ${router.name} (${latest.latencyMs}ms)")
                    } else {
                        _snackbarMessage.tryEmit("Gagal terhubung ke ${router.name}: ${latest.errorMessage ?: "Timeout"}")
                    }
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun toggleSecretDisabledById(
        router: RouterConfig,
        secretId: String,
        name: String,
        currentlyDisabled: Boolean
    ) {
        val newDisabled = !currentlyDisabled
        viewModelScope.launch {
            val res = repository.togglePppoeSecret(router, secretId, newDisabled)
            if (res.isSuccess) {
                val label = if (newDisabled) "dinonaktifkan (isolir)" else "diaktifkan kembali"
                _snackbarMessage.tryEmit("Secret $name $label.")
                executePoll(uiState.value.routers)
            } else {
                _snackbarMessage.tryEmit("Gagal ubah secret: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun toggleInterface(router: RouterConfig, interfaceId: String, interfaceName: String, currentlyDisabled: Boolean) {
        viewModelScope.launch {
            val res = repository.toggleInterface(router, interfaceId, !currentlyDisabled)
            if (res.isSuccess) {
                val state = if (!currentlyDisabled) "dinonaktifkan" else "diaktifkan"
                _snackbarMessage.tryEmit("Interface $interfaceName $state.")
                executePoll(uiState.value.routers)
            } else {
                _snackbarMessage.tryEmit("Gagal mengubah interface: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun rebootRouter(router: RouterConfig) {
        viewModelScope.launch {
            val res = repository.rebootRouter(router)
            if (res.isSuccess) {
                _snackbarMessage.tryEmit("Perintah reboot dikirim ke ${router.name}.")
            } else {
                _snackbarMessage.tryEmit("Gagal reboot: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun toggleSimulatedOffline(router: RouterConfig) {
        val offline = repository.toggleSimulatedOffline(router.id)
        _snackbarMessage.tryEmit(
            if (offline) "Simulasi: ${router.name} dibuat OFFLINE"
            else "Simulasi: ${router.name} kembali ONLINE"
        )
        refreshNow()
    }

    fun toggleSimulatedCpuSpike(router: RouterConfig) {
        val spike = repository.toggleSimulatedCpuSpike(router.id)
        _snackbarMessage.tryEmit(
            if (spike) "Simulasi: Lonjakan CPU tinggi di ${router.name}"
            else "Simulasi: CPU ${router.name} kembali normal"
        )
        refreshNow()
    }

    fun isSimulatedOffline(routerId: Long): Boolean = repository.isSimulatedOffline(routerId)
    fun isSimulatedCpuSpike(routerId: Long): Boolean = repository.isSimulatedCpuSpike(routerId)

    fun updateSettings(transform: (TelegramSettings) -> TelegramSettings) {
        viewModelScope.launch {
            repository.updateSettings(transform)
        }
    }

    fun sendTestTelegram(botToken: String, chatId: String) {
        viewModelScope.launch {
            _isTestingTelegram.value = true
            val res = repository.sendTestTelegram(botToken, chatId)
            _isTestingTelegram.value = false
            if (res.isSuccess) {
                _snackbarMessage.tryEmit("Tes Telegram berhasil terkirim!")
            } else {
                _snackbarMessage.tryEmit("Gagal Telegram: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun clearAlertLogs() {
        viewModelScope.launch {
            repository.clearAlertLogs()
            _snackbarMessage.tryEmit("Riwayat notifikasi dibersihkan.")
        }
    }

    fun hasNotificationPermission(): Boolean = notificationHelper.hasNotificationPermission()
}
