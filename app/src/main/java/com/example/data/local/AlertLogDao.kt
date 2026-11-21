package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.AlertLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertLogDao {
    @Query("SELECT * FROM alert_logs ORDER BY timestamp DESC LIMIT 200")
    fun getAllLogs(): Flow<List<AlertLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AlertLogEntity): Long

    @Query("DELETE FROM alert_logs")
    suspend fun clearAllLogs()
}
