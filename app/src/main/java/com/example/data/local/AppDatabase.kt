package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.data.model.ActivityLogEntity
import com.example.data.model.AlertLogEntity
import com.example.data.model.AlertType
import com.example.data.model.CableRouteEntity
import com.example.data.model.CpeDeviceEntity
import com.example.data.model.CustomerEntity
import com.example.data.model.FtthCustomerPinEntity
import com.example.data.model.FtthNodeEntity
import com.example.data.model.FtthNodeType
import com.example.data.model.InternetPackageEntity
import com.example.data.model.InvoiceEntity
import com.example.data.model.InvoiceStatus
import com.example.data.model.NetworkAreaEntity
import com.example.data.model.RouterConfig
import com.example.data.model.RouterProtocol
import com.example.data.model.SalesRepEntity
import com.example.data.model.SupportTicketEntity
import com.example.data.model.TicketPriority
import com.example.data.model.TicketStatus
import com.example.data.model.TransactionEntity

class Converters {
    @TypeConverter
    fun fromProtocol(protocol: RouterProtocol): String = protocol.name

    @TypeConverter
    fun toProtocol(value: String): RouterProtocol =
        runCatching { RouterProtocol.valueOf(value) }.getOrDefault(RouterProtocol.ROS_API_PLAIN)

    @TypeConverter
    fun fromAlertType(type: AlertType): String = type.name

    @TypeConverter
    fun toAlertType(value: String): AlertType =
        runCatching { AlertType.valueOf(value) }.getOrDefault(AlertType.ROUTER_DOWN)

    @TypeConverter
    fun fromInvoiceStatus(status: InvoiceStatus): String = status.name

    @TypeConverter
    fun toInvoiceStatus(value: String): InvoiceStatus =
        runCatching { InvoiceStatus.valueOf(value) }.getOrDefault(InvoiceStatus.BELUM_BAYAR)

    @TypeConverter
    fun fromTicketPriority(priority: TicketPriority): String = priority.name

    @TypeConverter
    fun toTicketPriority(value: String): TicketPriority =
        runCatching { TicketPriority.valueOf(value) }.getOrDefault(TicketPriority.SEDANG)

    @TypeConverter
    fun fromTicketStatus(status: TicketStatus): String = status.name

    @TypeConverter
    fun toTicketStatus(value: String): TicketStatus =
        runCatching { TicketStatus.valueOf(value) }.getOrDefault(TicketStatus.TERBUKA)

    @TypeConverter
    fun fromFtthNodeType(type: FtthNodeType): String = type.name

    @TypeConverter
    fun toFtthNodeType(value: String): FtthNodeType =
        runCatching { FtthNodeType.valueOf(value) }.getOrDefault(FtthNodeType.ODP)
}

@Database(
    entities = [
        RouterConfig::class,
        AlertLogEntity::class,
        InvoiceEntity::class,
        TransactionEntity::class,
        InternetPackageEntity::class,
        NetworkAreaEntity::class,
        SalesRepEntity::class,
        SupportTicketEntity::class,
        ActivityLogEntity::class,
        CpeDeviceEntity::class,
        FtthNodeEntity::class,
        FtthCustomerPinEntity::class,
        CableRouteEntity::class,
        CustomerEntity::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun routerDao(): RouterDao
    abstract fun alertLogDao(): AlertLogDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun transactionDao(): TransactionDao
    abstract fun internetPackageDao(): InternetPackageDao
    abstract fun networkAreaDao(): NetworkAreaDao
    abstract fun salesRepDao(): SalesRepDao
    abstract fun supportTicketDao(): SupportTicketDao
    abstract fun activityLogDao(): ActivityLogDao
    abstract fun cpeDeviceDao(): CpeDeviceDao
    abstract fun ftthNodeDao(): FtthNodeDao
    abstract fun ftthCustomerPinDao(): FtthCustomerPinDao
    abstract fun cableRouteDao(): CableRouteDao
    abstract fun customerDao(): CustomerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mikromon_database.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
