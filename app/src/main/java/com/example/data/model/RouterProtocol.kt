package com.example.data.model

enum class RouterProtocol(
    val displayName: String,
    val defaultPort: Int,
    val shortBadge: String
) {
    ROS_API_PLAIN("RouterOS API (TCP)", 8728, "API"),
    ROS_API_TLS("RouterOS API-SSL (TLS)", 8729, "API-SSL"),
    ROS_REST_HTTP("RouterOS v7 REST (HTTP)", 80, "REST"),
    ROS_REST_HTTPS("RouterOS v7 REST (HTTPS)", 443, "REST-SSL")
}
