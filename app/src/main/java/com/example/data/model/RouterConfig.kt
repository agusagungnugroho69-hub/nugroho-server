package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routers")
data class RouterConfig(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val host: String,
    val port: Int = 8728,
    val username: String = "admin",
    val password: String = "",
    val protocol: RouterProtocol = RouterProtocol.ROS_API_PLAIN,
    val routerOsMajorVersion: Int = 7,
    val monitoredInterface: String = "ether1",
    val cpuAlertThreshold: Int = 85,
    val isSimulated: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
