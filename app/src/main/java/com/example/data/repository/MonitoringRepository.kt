package com.example.data.repository

import com.example.data.local.AlertLogDao
import com.example.data.local.RouterDao
import com.example.data.local.SettingsDataStore
import com.example.data.model.AlertLogEntity
import com.example.data.model.AlertType
import com.example.data.model.ConnectionTestResult
import com.example.data.model.RouterConfig
import com.example.data.model.RouterTelemetry
import com.example.data.model.TelegramSettings
import com.example.data.model.TrafficSample
import com.example.data.network.MikroTikDataSource
import com.example.data.network.TelegramNotifier
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.concurrent.ConcurrentHashMap

class MonitoringRepository(
    private val routerDao: RouterDao,
    private val alertLogDao: AlertLogDao,
    private val settingsDataStore: SettingsDataStore,
    private val dataSource: MikroTikDataSource,
    private val telegramNotifier: TelegramNotifier,
    private val notificationHelper: AndroidNotificationHelper
) {
    val routersFlow: Flow<List<RouterConfig>> = routerDao.getAllRouters()
    val alertLogsFlow: Flow<List<AlertLogEntity>> = alertLogDao.getAllLogs()
    val settingsFlow: Flow<TelegramSettings> = settingsDataStore.settingsFlow

    private val previousOnlineState = ConcurrentHashMap<Long, Boolean>()
    private val previousPppoeCount = ConcurrentHashMap<Long, Int>()
    private val lastCpuAlertMillis = ConcurrentHashMap<Long, Long>()

    private val trafficBuffers = ConcurrentHashMap<Long, MutableList<TrafficSample>>()

    suspend fun addOrUpdateRouter(router: RouterConfig): Long {
        return if (router.id == 0L) {
            routerDao.insertRouter(router)
        } else {
            routerDao.updateRouter(router)
            router.id
        }
    }

    suspend fun deleteRouter(router: RouterConfig) {
        routerDao.deleteRouter(router)
        previousOnlineState.remove(router.id)
        previousPppoeCount.remove(router.id)
        lastCpuAlertMillis.remove(router.id)
        trafficBuffers.remove(router.id)
    }

    suspend fun updateSettings(transform: (TelegramSettings) -> TelegramSettings) {
        settingsDataStore.updateSettings(transform)
    }

    suspend fun testConnection(router: RouterConfig): ConnectionTestResult =
        dataSource.testConnection(router)

    suspend fun pollAllRouters(routers: List<RouterConfig>): Pair<Map<Long, RouterTelemetry>, Map<Long, List<TrafficSample>>> {
        if (routers.isEmpty()) return emptyMap<Long, RouterTelemetry>() to emptyMap()
        val settings = settingsDataStore.settingsFlow.first()

        val telemetryList = coroutineScope {
            routers.map { router ->
                async { router to dataSource.fetchTelemetry(router) }
            }.awaitAll()
        }

        val telemetryMap = mutableMapOf<Long, RouterTelemetry>()
        val historySnapshot = mutableMapOf<Long, List<TrafficSample>>()

        for ((router, telemetry) in telemetryList) {
            telemetryMap[router.id] = telemetry

            val buffer = trafficBuffers.getOrPut(router.id) { mutableListOf() }
            synchronized(buffer) {
                buffer.add(
                    TrafficSample(
                        timestampMillis = telemetry.lastUpdatedMillis,
                        rxBps = if (telemetry.isOnline) telemetry.rxBitsPerSec else 0L,
                        txBps = if (telemetry.isOnline) telemetry.txBitsPerSec else 0L
                    )
                )
                if (buffer.size > 36) {
                    buffer.removeAt(0)
                }
                historySnapshot[router.id] = buffer.toList()
            }

            evaluateAlerts(router, telemetry, settings)
        }

        return telemetryMap to historySnapshot
    }

    private suspend fun evaluateAlerts(
        router: RouterConfig,
        telemetry: RouterTelemetry,
        settings: TelegramSettings
    ) {
        val prevOnline = previousOnlineState.put(router.id, telemetry.isOnline)
        val simTag = if (router.isSimulated) " [SIMULASI]" else ""

        // 1. Router DOWN / UP transition
        if (prevOnline != null && prevOnline != telemetry.isOnline && settings.alertOnRouterDown) {
            if (!telemetry.isOnline) {
                val msg = "Router ${router.name}$simTag (${router.host}:${router.port}) OFFLINE! ${telemetry.errorMessage ?: "Tidak merespons."}"
                val html = "🔴 <b>[ROUTER DOWN]</b>\n" +
                    "Router: <b>${router.name}$simTag</b>\n" +
                    "Host: <code>${router.host}:${router.port}</code>\n" +
                    "Detail: ${telemetry.errorMessage ?: "Koneksi terputus"}"
                dispatchAlert(router, AlertType.ROUTER_DOWN, msg, html, settings)
            } else {
                val msg = "Router ${router.name}$simTag (${router.host}) kembali ONLINE (${telemetry.latencyMs}ms)."
                val html = "🟢 <b>[ROUTER UP]</b>\n" +
                    "Router: <b>${router.name}$simTag</b>\n" +
                    "Host: <code>${router.host}</code>\n" +
                    "Latensi: ${telemetry.latencyMs}ms • ROS ${telemetry.rosVersion}"
                dispatchAlert(router, AlertType.ROUTER_UP, msg, html, settings)
            }
        }

        // 2. CPU Spike Alert (60s cooldown per router)
        val threshold = maxOf(50, minOf(settings.cpuThresholdPercent, router.cpuAlertThreshold))
        if (telemetry.isOnline && settings.alertOnHighCpu && telemetry.cpuLoadPercent >= threshold) {
            val now = System.currentTimeMillis()
            val lastSent = lastCpuAlertMillis[router.id] ?: 0L
            if (now - lastSent > 60_000L) {
                lastCpuAlertMillis[router.id] = now
                val msg = "CPU ${router.name}$simTag mencapai ${telemetry.cpuLoadPercent}% (batas ${threshold}%)."
                val html = "⚠️ <b>[CPU TINGGI]</b>\n" +
                    "Router: <b>${router.name}$simTag</b>\n" +
                    "Beban CPU: <b>${telemetry.cpuLoadPercent}%</b> (Batas: $threshold%)\n" +
                    "PPPoE Aktif: ${telemetry.activePppoeCount} user"
                dispatchAlert(router, AlertType.CPU_HIGH, msg, html, settings)
            }
        }

        // 3. PPPoE active user count change
        val prevPppoe = previousPppoeCount.put(router.id, telemetry.activePppoeCount)
        if (telemetry.isOnline && prevPppoe != null && prevPppoe != telemetry.activePppoeCount && settings.alertOnPppoeChange) {
            val diff = telemetry.activePppoeCount - prevPppoe
            val verb = if (diff > 0) "naik +$diff" else "turun $diff"
            val msg = "Pelanggan PPPoE ${router.name}$simTag $verb (Online: ${telemetry.activePppoeCount}/${telemetry.totalSecretCount})."
            val html = "📡 <b>[PPPoE UPDATE]</b>\n" +
                "Router: <b>${router.name}$simTag</b>\n" +
                "Perubahan: $verb\n" +
                "Online: ${telemetry.activePppoeCount} / ${telemetry.totalSecretCount} pelanggan"
            dispatchAlert(router, AlertType.PPPOE_CHANGE, msg, html, settings)
        }
    }

    private suspend fun dispatchAlert(
        router: RouterConfig,
        type: AlertType,
        plainMessage: String,
        telegramHtml: String,
        settings: TelegramSettings
    ) {
        if (settings.localNotificationsEnabled) {
            notificationHelper.notifyAlert(
                title = "MikroMon • ${type.label}",
                message = plainMessage,
                notificationId = (router.id.toInt() * 31) + type.ordinal
            )
        }

        var sent = false
        var status = "Notifikasi Telegram nonaktif"
        if (settings.enabled && settings.botToken.isNotBlank() && settings.chatId.isNotBlank()) {
            val res = telegramNotifier.sendMessage(settings.botToken, settings.chatId, telegramHtml)
            sent = res.isSuccess
            status = res.getOrElse { it.message ?: "Gagal mengirim ke Telegram" }
        }

        alertLogDao.insertLog(
            AlertLogEntity(
                routerId = router.id,
                routerName = router.name,
                type = type,
                message = plainMessage,
                telegramSent = sent,
                telegramStatus = status
            )
        )
    }

    suspend fun sendTestTelegram(botToken: String, chatId: String): Result<String> {
        val html = "✅ <b>[MIKROMON TES KONEKSI]</b>\n" +
            "Integrasi Bot Telegram berhasil terhubung.\n" +
            "Notifikasi otomatis siap memantau status router, CPU, dan PPPoE Anda."
        val res = telegramNotifier.sendMessage(botToken, chatId, html)
        alertLogDao.insertLog(
            AlertLogEntity(
                routerId = 0L,
                routerName = "Sistem Telegram",
                type = AlertType.TELEGRAM_TEST,
                message = "Uji coba pengiriman notifikasi ke Chat ID $chatId",
                telegramSent = res.isSuccess,
                telegramStatus = res.getOrElse { it.message ?: "Gagal" }
            )
        )
        return res
    }

    suspend fun clearAlertLogs() {
        alertLogDao.clearAllLogs()
    }

    suspend fun kickPppoeClient(router: RouterConfig, activeId: String): Result<Unit> =
        dataSource.kickPppoeActive(router, activeId)

    suspend fun togglePppoeSecret(router: RouterConfig, secretId: String, disabled: Boolean): Result<Unit> =
        dataSource.setPppoeSecretDisabled(router, secretId, disabled)

    suspend fun toggleInterface(router: RouterConfig, interfaceId: String, disabled: Boolean): Result<Unit> =
        dataSource.setInterfaceDisabled(router, interfaceId, disabled)

    suspend fun rebootRouter(router: RouterConfig): Result<Unit> =
        dataSource.rebootRouter(router)

    fun toggleSimulatedOffline(routerId: Long): Boolean =
        dataSource.simulatedEngine.toggleForcedOffline(routerId)

    fun toggleSimulatedCpuSpike(routerId: Long): Boolean =
        dataSource.simulatedEngine.toggleForcedCpuSpike(routerId)

    fun isSimulatedOffline(routerId: Long): Boolean =
        dataSource.simulatedEngine.isForcedOffline(routerId)

    fun isSimulatedCpuSpike(routerId: Long): Boolean =
        dataSource.simulatedEngine.isForcedCpuSpike(routerId)

    suspend fun loadDemoLabRouters() {
        val existing = routerDao.getAllRouters().first()
        if (existing.any { it.isSimulated }) return
        routerDao.insertRouter(
            RouterConfig(
                name = "CCR2004-Core-NOC",
                host = "10.10.0.1",
                port = 8728,
                username = "noc_monitor",
                password = "demo",
                monitoredInterface = "ether1-isp-main",
                cpuAlertThreshold = 85,
                isSimulated = true
            )
        )
        routerDao.insertRouter(
            RouterConfig(
                name = "RB4011-POP-Utara",
                host = "10.10.2.1",
                port = 8728,
                username = "noc_monitor",
                password = "demo",
                monitoredInterface = "ether1-isp-main",
                cpuAlertThreshold = 85,
                isSimulated = true
            )
        )
    }
}
