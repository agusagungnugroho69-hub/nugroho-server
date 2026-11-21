package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.RouterConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface RouterDao {
    @Query("SELECT * FROM routers ORDER BY createdAt ASC")
    fun getAllRouters(): Flow<List<RouterConfig>>

    @Query("SELECT * FROM routers WHERE id = :id")
    suspend fun getRouterById(id: Long): RouterConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRouter(router: RouterConfig): Long

    @Update
    suspend fun updateRouter(router: RouterConfig)

    @Delete
    suspend fun deleteRouter(router: RouterConfig)

    @Query("DELETE FROM routers WHERE id = :id")
    suspend fun deleteRouterById(id: Long)
}
