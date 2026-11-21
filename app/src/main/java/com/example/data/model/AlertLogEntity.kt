package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AlertType(val label: String) {
    ROUTER_DOWN("Router DOWN"),
    ROUTER_UP("Router UP"),
    CPU_HIGH("CPU Tinggi"),
    PPPOE_CHANGE("Status PPPoE"),
    TELEGRAM_TEST("Tes Telegram")
}

@Entity(tableName = "alert_logs")
data class AlertLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routerId: Long,
    val routerName: String,
    val type: AlertType,
    val message: String,
    val telegramSent: Boolean,
    val telegramStatus: String,
    val timestamp: Long = System.currentTimeMillis()
)
