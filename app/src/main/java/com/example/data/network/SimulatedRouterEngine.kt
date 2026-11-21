package com.example.data.network

import com.example.data.model.InterfaceStats
import com.example.data.model.IpPoolInfo
import com.example.data.model.PppProfileInfo
import com.example.data.model.PppSecretInfo
import com.example.data.model.PppoeClient
import com.example.data.model.RouterConfig
import com.example.data.model.RouterTelemetry
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.sin

/**
 * Virtual MikroTik Lab Simulator used ONLY for routers explicitly created with isSimulated = true.
 * Lets network engineers test graphs, PPPoE management, Router DOWN, and CPU Spike alerts
 * even when away from a physical MikroTik LAN.
 */
class SimulatedRouterEngine {

    private data class SimState(
        var forcedOffline: Boolean = false,
        var forcedCpuSpike: Boolean = false,
        var tick: Int = 0,
        var uptimeSeconds: Long = 142_800L,
        val disabledInterfaces: MutableSet<String> = mutableSetOf(),
        val kickedClients: MutableSet<String> = mutableSetOf(),
        val disabledSecrets: MutableSet<String> = mutableSetOf()
    )

    private val states = ConcurrentHashMap<Long, SimState>()

    private fun getState(routerId: Long): SimState =
        states.getOrPut(routerId) {
            SimState(uptimeSeconds = 86_400L + (routerId * 41_230L))
        }

    fun toggleForcedOffline(routerId: Long): Boolean {
        val s = getState(routerId)
        s.forcedOffline = !s.forcedOffline
        return s.forcedOffline
    }

    fun toggleForcedCpuSpike(routerId: Long): Boolean {
        val s = getState(routerId)
        s.forcedCpuSpike = !s.forcedCpuSpike
        return s.forcedCpuSpike
    }

    fun isForcedOffline(routerId: Long): Boolean = getState(routerId).forcedOffline
    fun isForcedCpuSpike(routerId: Long): Boolean = getState(routerId).forcedCpuSpike

    fun fetchTelemetry(router: RouterConfig): RouterTelemetry {
        val state = getState(router.id)
        state.tick += 1
        state.uptimeSeconds += 5

        if (state.forcedOffline) {
            return RouterTelemetry(
                routerId = router.id,
                isOnline = false,
                identity = router.name,
                monitoredInterface = router.monitoredInterface,
                lastUpdatedMillis = System.currentTimeMillis(),
                errorMessage = "[SIMULASI] Timeout menghubungi ${router.host}:${router.port}",
                isSimulated = true
            )
        }

        val phase = state.tick * 0.45 + router.id * 1.1
        val isSecondary = router.id % 2L == 0L
        val baseRxMbps = if (isSecondary) 118.0 else 285.0
        val baseTxMbps = if (isSecondary) 42.0 else 96.0
        val wave = sin(phase) * 0.22 + sin(phase * 2.3) * 0.11

        val rxBps = ((baseRxMbps * (1.0 + wave)) * 1_000_000.0).toLong().coerceAtLeast(1_200_000L)
        val txBps = ((baseTxMbps * (1.0 + wave * 0.85)) * 1_000_000.0).toLong().coerceAtLeast(500_000L)

        val cpu = if (state.forcedCpuSpike) {
            (91 + (state.tick % 7)).coerceAtMost(99)
        } else {
            val baseCpu = if (isSecondary) 24 else 38
            (baseCpu + (sin(phase) * 9).toInt()).coerceIn(8, 72)
        }

        val totalMem = if (isSecondary) 512L * 1024L * 1024L else 1024L * 1024L * 1024L
        val usedRatio = if (isSecondary) 0.46 else 0.58
        val freeMem = (totalMem * (1.0 - usedRatio - sin(phase) * 0.02)).toLong()

        val ifaces = listOf(
            InterfaceStats(
                id = "*1",
                name = "ether1-isp-main",
                type = "ether",
                isRunning = !state.disabledInterfaces.contains("*1"),
                isDisabled = state.disabledInterfaces.contains("*1"),
                macAddress = "48:A9:8A:12:04:01",
                rxBytes = 8_420_000_000L + state.tick * 45_000_000L,
                txBytes = 2_910_000_000L + state.tick * 15_000_000L,
                rxBps = rxBps,
                txBps = txBps,
                comment = "Uplink Fiber Transit"
            ),
            InterfaceStats(
                id = "*2",
                name = "sfp-sfpplus1-olt",
                type = "ether",
                isRunning = !state.disabledInterfaces.contains("*2"),
                isDisabled = state.disabledInterfaces.contains("*2"),
                macAddress = "48:A9:8A:12:04:02",
                rxBytes = 3_110_000_000L + state.tick * 12_000_000L,
                txBytes = 7_980_000_000L + state.tick * 41_000_000L,
                rxBps = txBps,
                txBps = rxBps,
                comment = "Distribusi OLT EPON/GPON"
            ),
            InterfaceStats(
                id = "*3",
                name = "bridge-pppoe",
                type = "bridge",
                isRunning = !state.disabledInterfaces.contains("*3"),
                isDisabled = state.disabledInterfaces.contains("*3"),
                macAddress = "48:A9:8A:12:04:03",
                rxBytes = 5_100_000_000L + state.tick * 18_000_000L,
                txBytes = 4_850_000_000L + state.tick * 29_000_000L,
                rxBps = (rxBps * 0.7).toLong(),
                txBps = (txBps * 0.8).toLong(),
                comment = "VLAN Pelanggan PPPoE"
            )
        )

        val sampleUsers = listOf(
            Triple("budi.santoso@fiber", "10.20.1.14", "20M-Home"),
            Triple("warung.maju@fiber", "10.20.1.27", "30M-Bisnis"),
            Triple("siti.rahma@fiber", "10.20.1.41", "15M-Hemat"),
            Triple("toko.berkah@fiber", "10.20.1.58", "50M-Dedicated"),
            Triple("agus.nugroho@fiber", "10.20.1.63", "30M-Bisnis"),
            Triple("kos.melati@fiber", "10.20.1.79", "50M-Dedicated"),
            Triple("rina.cell@fiber", "10.20.1.84", "20M-Home"),
            Triple("pakde.rt04@fiber", "10.20.1.92", "15M-Hemat")
        )

        val clients = sampleUsers.mapIndexed { idx, (uName, ip, prof) ->
            val secId = "*S${router.id}_$idx"
            val actId = "*A${router.id}_$idx"
            val isDisabled = state.disabledSecrets.contains(secId)
            val isOfflineByDefault = idx >= 6
            val isOnline = !isDisabled && !isOfflineByDefault && !state.kickedClients.contains(actId)
            PppoeClient(
                id = if (isOnline) actId else secId,
                secretId = secId,
                name = "${uName}.${router.id}",
                service = "pppoe",
                callerId = "C4:AD:34:%02X:%02X:%02X".format((idx * 17) % 255, (idx * 31) % 255, router.id.toInt() % 255),
                address = if (isOnline) ip else "-",
                uptime = if (isOnline) formatUptime(state.uptimeSeconds - idx * 3600L) else "-",
                profile = prof,
                encoding = "MPPE128",
                isOnline = isOnline,
                isDisabled = isDisabled,
                lastLoggedOut = if (!isOnline) "20 Nov 02:${10 + idx}:04" else "",
                comment = if (isDisabled) "Isolir tagihan" else "ODP-A${idx + 1}",
                routerId = router.id,
                routerName = router.name
            )
        }

        val activeCount = clients.count { it.isOnline }
        val offlineCount = clients.size - activeCount

        val selectedIface = router.monitoredInterface.takeIf { m -> ifaces.any { it.name == m } }
            ?: "ether1-isp-main"
        val ifaceStats = ifaces.firstOrNull { it.name == selectedIface } ?: ifaces.first()

        val totalHdd = 128L * 1024L * 1024L * 230L // ~29 GB
        val usedHdd = 54L * 1024L * 1024L
        val freeHdd = (totalHdd - usedHdd).coerceAtLeast(0L)

        val simSecrets = clients.map { c ->
            PppSecretInfo(
                id = c.secretId ?: c.id,
                name = c.name,
                password = "pass_${c.name.substringBefore('.')}",
                service = c.service,
                profile = c.profile,
                remoteAddress = if (c.isOnline) "-" else c.address,
                localAddress = "10.20.1.1",
                callerId = c.callerId,
                lastLoggedOut = c.lastLoggedOut,
                comment = c.comment,
                isDisabled = c.isDisabled,
                isOnline = c.isOnline,
                activeAddress = c.address,
                activeUptime = c.uptime
            )
        }

        val simProfiles = listOf(
            PppProfileInfo("*1", "15M-Hemat", "10.20.1.1", "pool-pppoe", "15M/15M", "yes"),
            PppProfileInfo("*2", "20M-Home", "10.20.1.1", "pool-pppoe", "20M/20M", "yes"),
            PppProfileInfo("*3", "30M-Bisnis", "10.20.1.1", "pool-pppoe", "30M/30M", "yes"),
            PppProfileInfo("*4", "50M-Dedicated", "10.20.1.1", "pool-vip", "50M/50M", "yes")
        )

        val simPools = listOf(
            IpPoolInfo("*1", "pool-pppoe", "10.20.1.10-10.20.1.250"),
            IpPoolInfo("*2", "pool-vip", "10.20.2.10-10.20.2.100")
        )

        return RouterTelemetry(
            routerId = router.id,
            isOnline = true,
            identity = router.name,
            boardName = if (isSecondary) "RB4011iGS+" else "CCR2004-1G-12S+2XS",
            rosVersion = "7.16.1 (stable)",
            architecture = "arm64",
            cpuModel = if (isSecondary) "AL21400" else "AL32400",
            cpuLoadPercent = cpu,
            cpuCount = 4,
            freeMemoryBytes = freeMem,
            totalMemoryBytes = totalMem,
            freeHddBytes = freeHdd,
            totalHddBytes = totalHdd,
            uptime = formatUptime(state.uptimeSeconds),
            activePppoeCount = activeCount,
            totalSecretCount = clients.size,
            offlineSecretCount = offlineCount,
            rxBitsPerSec = ifaceStats.rxBps,
            txBitsPerSec = ifaceStats.txBps,
            monitoredInterface = selectedIface,
            interfaces = ifaces,
            pppoeClients = clients,
            pppSecrets = simSecrets,
            pppProfiles = simProfiles,
            ipPools = simPools,
            latencyMs = if (isSecondary) 12L else 4L,
            lastUpdatedMillis = System.currentTimeMillis(),
            errorMessage = null,
            isSimulated = true
        )
    }

    fun kickClient(routerId: Long, activeId: String) {
        getState(routerId).kickedClients.add(activeId)
    }

    fun setSecretDisabled(routerId: Long, secretId: String, disabled: Boolean) {
        val s = getState(routerId)
        if (disabled) s.disabledSecrets.add(secretId) else s.disabledSecrets.remove(secretId)
    }

    fun setInterfaceDisabled(routerId: Long, interfaceId: String, disabled: Boolean) {
        val s = getState(routerId)
        if (disabled) s.disabledInterfaces.add(interfaceId) else s.disabledInterfaces.remove(interfaceId)
    }

    private fun formatUptime(totalSec: Long): String {
        val safe = totalSec.coerceAtLeast(0L)
        val days = safe / 86400
        val hours = (safe % 86400) / 3600
        val mins = (safe % 3600) / 60
        val secs = safe % 60
        return buildString {
            if (days > 0) append("${days}d")
            if (hours > 0 || days > 0) append("${hours}h")
            append("${mins}m${secs}s")
        }
    }
}
