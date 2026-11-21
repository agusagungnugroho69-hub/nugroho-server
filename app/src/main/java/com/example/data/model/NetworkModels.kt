package com.example.data.model

data class InterfaceStats(
    val id: String,
    val name: String,
    val type: String,
    val isRunning: Boolean,
    val isDisabled: Boolean,
    val macAddress: String = "",
    val rxBytes: Long = 0L,
    val txBytes: Long = 0L,
    val rxBps: Long = 0L,
    val txBps: Long = 0L,
    val comment: String = ""
)

data class PppoeClient(
    val id: String,
    val secretId: String? = null,
    val name: String,
    val service: String = "pppoe",
    val callerId: String = "-",
    val address: String = "-",
    val uptime: String = "-",
    val profile: String = "default",
    val encoding: String = "",
    val isOnline: Boolean = true,
    val isDisabled: Boolean = false,
    val lastLoggedOut: String = "",
    val comment: String = "",
    val routerId: Long = 0L,
    val routerName: String = ""
)

data class PppSecretInfo(
    val id: String,
    val name: String,
    val password: String = "",
    val service: String = "pppoe",
    val profile: String = "default",
    val remoteAddress: String = "-",
    val localAddress: String = "-",
    val callerId: String = "-",
    val lastLoggedOut: String = "",
    val comment: String = "",
    val isDisabled: Boolean = false,
    val isOnline: Boolean = false,
    val activeAddress: String = "-",
    val activeUptime: String = "-"
)

data class PppProfileInfo(
    val id: String,
    val name: String,
    val localAddress: String = "-",
    val remoteAddress: String = "-",
    val rateLimit: String = "-",
    val onlyOne: String = "default",
    val comment: String = ""
)

data class IpPoolInfo(
    val id: String,
    val name: String,
    val ranges: String = "-",
    val nextPool: String = "",
    val comment: String = ""
)

data class TrafficSample(
    val timestampMillis: Long,
    val rxBps: Long,
    val txBps: Long
) {
    val rxMbps: Float get() = rxBps / 1_000_000f
    val txMbps: Float get() = txBps / 1_000_000f
}

data class RouterTelemetry(
    val routerId: Long,
    val isOnline: Boolean,
    val identity: String = "",
    val boardName: String = "-",
    val rosVersion: String = "-",
    val architecture: String = "-",
    val cpuModel: String = "-",
    val cpuLoadPercent: Int = 0,
    val cpuCount: Int = 1,
    val freeMemoryBytes: Long = 0L,
    val totalMemoryBytes: Long = 0L,
    val freeHddBytes: Long = 0L,
    val totalHddBytes: Long = 0L,
    val uptime: String = "-",
    val activePppoeCount: Int = 0,
    val totalSecretCount: Int = 0,
    val offlineSecretCount: Int = 0,
    val rxBitsPerSec: Long = 0L,
    val txBitsPerSec: Long = 0L,
    val monitoredInterface: String = "ether1",
    val interfaces: List<InterfaceStats> = emptyList(),
    val pppoeClients: List<PppoeClient> = emptyList(),
    val pppSecrets: List<PppSecretInfo> = emptyList(),
    val pppProfiles: List<PppProfileInfo> = emptyList(),
    val ipPools: List<IpPoolInfo> = emptyList(),
    val latencyMs: Long = 0L,
    val lastUpdatedMillis: Long = System.currentTimeMillis(),
    val errorMessage: String? = null,
    val isSimulated: Boolean = false
) {
    val usedMemoryBytes: Long
        get() = (totalMemoryBytes - freeMemoryBytes).coerceAtLeast(0L)

    val memoryUsagePercent: Int
        get() = if (totalMemoryBytes > 0L) {
            ((usedMemoryBytes * 100L) / totalMemoryBytes).toInt().coerceIn(0, 100)
        } else 0

    val usedHddBytes: Long
        get() = (totalHddBytes - freeHddBytes).coerceAtLeast(0L)

    val storageUsagePercent: Int
        get() = if (totalHddBytes > 0L) {
            ((usedHddBytes * 100L) / totalHddBytes).toInt().coerceIn(0, 100)
        } else 0
}

data class ConnectionTestResult(
    val success: Boolean,
    val identity: String = "",
    val boardName: String = "",
    val rosVersion: String = "",
    val latencyMs: Long = 0L,
    val interfaces: List<String> = emptyList(),
    val message: String = ""
)
