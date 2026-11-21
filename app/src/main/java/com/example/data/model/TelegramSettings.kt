package com.example.data.model

data class TelegramSettings(
    val botToken: String = "",
    val chatId: String = "",
    val enabled: Boolean = false,
    val alertOnRouterDown: Boolean = true,
    val alertOnHighCpu: Boolean = true,
    val alertOnPppoeChange: Boolean = false,
    val cpuThresholdPercent: Int = 85,
    val pollingIntervalSec: Int = 5,
    val localNotificationsEnabled: Boolean = true,
    val darkTheme: Boolean = true,
    val ispName: String = "NUGROHO NETWORK",
    val ispSubtitle: String = "Fiber & MikroTik Monitoring",
    val adminName: String = "Network Admin",
    val genieAcsUrl: String = "",
    val ppnPercent: Int = 11
)
