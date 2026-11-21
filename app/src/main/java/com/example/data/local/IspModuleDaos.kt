package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ActivityLogEntity
import com.example.data.model.CpeDeviceEntity
import com.example.data.model.InternetPackageEntity
import com.example.data.model.InvoiceEntity
import com.example.data.model.NetworkAreaEntity
import com.example.data.model.SalesRepEntity
import com.example.data.model.SupportTicketEntity
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceDao {
    @Query("SELECT * FROM invoices ORDER BY createdAt DESC")
    fun getAll(): Flow<List<InvoiceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: InvoiceEntity): Long

    @Update
    suspend fun update(item: InvoiceEntity)

    @Delete
    suspend fun delete(item: InvoiceEntity)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAll(): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: TransactionEntity): Long

    @Update
    suspend fun update(item: TransactionEntity)
}

@Dao
interface InternetPackageDao {
    @Query("SELECT * FROM internet_packages ORDER BY monthlyPrice ASC")
    fun getAll(): Flow<List<InternetPackageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: InternetPackageEntity): Long

    @Update
    suspend fun update(item: InternetPackageEntity)

    @Delete
    suspend fun delete(item: InternetPackageEntity)
}

@Dao
interface NetworkAreaDao {
    @Query("SELECT * FROM network_areas ORDER BY name ASC")
    fun getAll(): Flow<List<NetworkAreaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: NetworkAreaEntity): Long

    @Update
    suspend fun update(item: NetworkAreaEntity)

    @Delete
    suspend fun delete(item: NetworkAreaEntity)
}

@Dao
interface SalesRepDao {
    @Query("SELECT * FROM sales_reps ORDER BY completedInstalls DESC")
    fun getAll(): Flow<List<SalesRepEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SalesRepEntity): Long

    @Update
    suspend fun update(item: SalesRepEntity)

    @Delete
    suspend fun delete(item: SalesRepEntity)
}

@Dao
interface SupportTicketDao {
    @Query("SELECT * FROM support_tickets ORDER BY createdAt DESC")
    fun getAll(): Flow<List<SupportTicketEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SupportTicketEntity): Long

    @Update
    suspend fun update(item: SupportTicketEntity)

    @Delete
    suspend fun delete(item: SupportTicketEntity)
}

@Dao
interface ActivityLogDao {
    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC LIMIT 250")
    fun getAll(): Flow<List<ActivityLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ActivityLogEntity): Long

    @Query("DELETE FROM activity_logs")
    suspend fun clearAll()
}

@Dao
interface CpeDeviceDao {
    @Query("SELECT * FROM cpe_devices ORDER BY customerName ASC")
    fun getAll(): Flow<List<CpeDeviceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CpeDeviceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: CpeDeviceEntity): Long

    @Delete
    suspend fun delete(item: CpeDeviceEntity)

    @Query("DELETE FROM cpe_devices")
    suspend fun clearAll()
}

@Dao
interface FtthNodeDao {
    @Query("SELECT * FROM ftth_nodes ORDER BY createdAt ASC")
    fun getAll(): Flow<List<com.example.data.model.FtthNodeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: com.example.data.model.FtthNodeEntity): Long

    @Update
    suspend fun update(item: com.example.data.model.FtthNodeEntity)

    @Delete
    suspend fun delete(item: com.example.data.model.FtthNodeEntity)
}

@Dao
interface FtthCustomerPinDao {
    @Query("SELECT * FROM ftth_customer_pins ORDER BY name ASC")
    fun getAll(): Flow<List<com.example.data.model.FtthCustomerPinEntity>>

    @Query("SELECT * FROM ftth_customer_pins WHERE (pppoeUsername != '' AND pppoeUsername = :pppoe) OR name = :name LIMIT 1")
    suspend fun findByPppoeOrName(pppoe: String, name: String): com.example.data.model.FtthCustomerPinEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: com.example.data.model.FtthCustomerPinEntity): Long

    @Update
    suspend fun update(item: com.example.data.model.FtthCustomerPinEntity)

    @Delete
    suspend fun delete(item: com.example.data.model.FtthCustomerPinEntity)
}

@Dao
interface CableRouteDao {
    @Query("SELECT * FROM cable_routes ORDER BY createdAt DESC")
    fun getAll(): Flow<List<com.example.data.model.CableRouteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: com.example.data.model.CableRouteEntity): Long

    @Delete
    suspend fun delete(item: com.example.data.model.CableRouteEntity)
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY createdAt DESC, name ASC")
    fun getAll(): Flow<List<com.example.data.model.CustomerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: com.example.data.model.CustomerEntity): Long

    @Update
    suspend fun update(item: com.example.data.model.CustomerEntity)

    @Delete
    suspend fun delete(item: com.example.data.model.CustomerEntity)
}
