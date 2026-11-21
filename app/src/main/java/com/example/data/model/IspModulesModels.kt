package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class InvoiceStatus(val label: String) {
    BELUM_BAYAR("Belum bayar"),
    LUNAS("Lunas"),
    TERLAMBAT("Terlambat"),
    DIBATALKAN("Dibatalkan")
}

@Entity(tableName = "invoices")
data class InvoiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceNumber: String,
    val customerName: String,
    val customerPppoe: String = "",
    val packageName: String,
    val areaName: String = "Umum",
    val amount: Long,
    val dppAmount: Long,
    val ppnAmount: Long,
    val dueDateLabel: String,
    val status: InvoiceStatus = InvoiceStatus.BELUM_BAYAR,
    val paymentMethod: String = "-",
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val referenceCode: String,
    val invoiceNumber: String,
    val customerName: String,
    val amount: Long,
    val method: String, // Transfer Bank, Cash, QRIS
    val verified: Boolean = true,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "internet_packages")
data class InternetPackageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val pppoeProfile: String,
    val downloadMbps: Int,
    val uploadMbps: Int,
    val monthlyPrice: Long,
    val description: String = "",
    val isActive: Boolean = true,
    val localAddress: String = "192.168.1.1",
    val poolName: String = "",
    val poolRanges: String = "", // semicolon-separated IP ranges
    val isAdvancedLimit: Boolean = false,
    val uploadLimitText: String = "5M",
    val downloadLimitText: String = "5M",
    val burstEnabled: Boolean = false,
    val burstUpload: String = "",
    val burstDownload: String = "",
    val burstThreshold: String = "",
    val burstTime: String = "",
    val usePpn: Boolean = false,
    val isVisible: Boolean = true
)

@Entity(tableName = "network_areas")
data class NetworkAreaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val code: String,
    val odpCount: Int,
    val totalPorts: Int,
    val usedPorts: Int,
    val coordinates: String = "",
    val routerName: String = "",
    val status: String = "Normal"
)

@Entity(tableName = "sales_reps")
data class SalesRepEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String,
    val areaAssigned: String,
    val targetInstalls: Int,
    val completedInstalls: Int,
    val commissionPerInstall: Long = 50_000L
)

enum class TicketPriority(val label: String) {
    RENDAH("Rendah"),
    SEDANG("Sedang"),
    TINGGI("Tinggi"),
    DARURAT("Darurat")
}

enum class TicketStatus(val label: String) {
    TERBUKA("Terbuka"),
    PROSES("Dalam Proses"),
    SELESAI("Selesai")
}

@Entity(tableName = "support_tickets")
data class SupportTicketEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ticketCode: String,
    val customerName: String,
    val subject: String,
    val description: String,
    val priority: TicketPriority = TicketPriority.SEDANG,
    val status: TicketStatus = TicketStatus.TERBUKA,
    val technician: String = "Belum ditugaskan",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "activity_logs")
data class ActivityLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val module: String,
    val action: String,
    val detail: String,
    val actor: String = "Admin",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "cpe_devices")
data class CpeDeviceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deviceId: String,
    val serialNumber: String,
    val model: String,
    val customerName: String,
    val ipAddress: String,
    val rxPowerDbm: String,
    val ssid: String,
    val isOnline: Boolean,
    val lastInform: String
)

data class RadiusEntry(
    val id: String,
    val service: String,
    val address: String,
    val secret: String,
    val timeout: String,
    val status: String,
    val routerName: String
)

enum class FtthNodeType(val label: String) {
    ODP("ODP"),
    ODC("ODC"),
    POP("POP"),
    BTS("BTS"),
    HTB("HTB"),
    OTHER("Other")
}

@Entity(tableName = "ftth_nodes")
data class FtthNodeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: FtthNodeType = FtthNodeType.ODP,
    val name: String,
    val code: String,
    val limitPort: Int = 8,
    val extraPort: Int = 0,
    val usedPorts: Int = 0,
    val address: String = "",
    val description: String = "",
    val parentId: Long? = null,
    val latitude: Double,
    val longitude: Double,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "ftth_customer_pins")
data class FtthCustomerPinEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val pppoeUsername: String = "",
    val parentNodeId: Long? = null,
    val latitude: Double,
    val longitude: Double,
    val address: String = ""
)

@Entity(tableName = "cable_routes")
data class CableRouteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val pointsEncoded: String, // "lat,lng;lat,lng;..."
    val lengthMeters: Int = 0,
    val coreCount: Int = 12,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val pppoeUsername: String = "",
    val pppoePassword: String = "",
    val customerCode: String = "",
    val phone: String = "",
    val email: String = "",
    val whatsappNotifyEnabled: Boolean = false,
    val distributionNodeId: Long? = null,
    val distributionLabel: String = "",
    val address: String = "",
    val dusun: String = "",
    val province: String = "",
    val regency: String = "",
    val district: String = "",
    val village: String = "",
    val connectionModel: String = "PPPoE",
    val routerId: Long = 0L,
    val routerName: String = "",
    val packageId: Long? = null,
    val packageName: String = "",
    val exemptAutoIsolir: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val statusLabel: String = "Aktif",
    val installDateMillis: Long? = null,
    val installDateLabel: String = "",
    val salesId: Long? = null,
    val salesName: String = "",
    val customIsolirEnabled: Boolean = false,
    val customIsolirDay: Int = 28,
    val customInvoiceEnabled: Boolean = false,
    val customInvoiceDay: Int = 28,
    val ispLabel: String = "",
    val isSyncedToRouter: Boolean = false,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
