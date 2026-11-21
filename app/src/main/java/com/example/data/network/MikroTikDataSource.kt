package com.example.data.network

import com.example.data.model.ConnectionTestResult
import com.example.data.model.RouterConfig
import com.example.data.model.RouterProtocol
import com.example.data.model.RouterTelemetry

class MikroTikDataSource(
    private val binaryClient: RouterOsBinaryClient = RouterOsBinaryClient(),
    private val restClient: RouterOsRestClient = RouterOsRestClient(),
    val simulatedEngine: SimulatedRouterEngine = SimulatedRouterEngine()
) {
    suspend fun testConnection(router: RouterConfig): ConnectionTestResult {
        if (router.isSimulated) {
            return ConnectionTestResult(
                success = true,
                identity = router.name,
                boardName = "CCR2004-1G-12S+2XS (Simulasi)",
                rosVersion = "7.16.1",
                latencyMs = 4L,
                interfaces = listOf("ether1-isp-main", "sfp-sfpplus1-olt", "bridge-pppoe"),
                message = "Mode Simulasi Lab aktif untuk ${router.name}"
            )
        }
        return when (router.protocol) {
            RouterProtocol.ROS_API_PLAIN -> binaryClient.testConnection(router, useTls = false)
            RouterProtocol.ROS_API_TLS -> binaryClient.testConnection(router, useTls = true)
            RouterProtocol.ROS_REST_HTTP -> restClient.testConnection(router, useHttps = false)
            RouterProtocol.ROS_REST_HTTPS -> restClient.testConnection(router, useHttps = true)
        }
    }

    suspend fun fetchTelemetry(router: RouterConfig): RouterTelemetry {
        if (router.isSimulated) {
            return simulatedEngine.fetchTelemetry(router)
        }
        return when (router.protocol) {
            RouterProtocol.ROS_API_PLAIN -> binaryClient.fetchTelemetry(router, useTls = false)
            RouterProtocol.ROS_API_TLS -> binaryClient.fetchTelemetry(router, useTls = true)
            RouterProtocol.ROS_REST_HTTP -> restClient.fetchTelemetry(router, useHttps = false)
            RouterProtocol.ROS_REST_HTTPS -> restClient.fetchTelemetry(router, useHttps = true)
        }
    }

    suspend fun kickPppoeActive(router: RouterConfig, activeId: String): Result<Unit> {
        if (router.isSimulated) {
            simulatedEngine.kickClient(router.id, activeId)
            return Result.success(Unit)
        }
        return when (router.protocol) {
            RouterProtocol.ROS_API_PLAIN -> binaryClient.kickPppoeActive(router, false, activeId)
            RouterProtocol.ROS_API_TLS -> binaryClient.kickPppoeActive(router, true, activeId)
            RouterProtocol.ROS_REST_HTTP -> restClient.kickPppoeActive(router, false, activeId)
            RouterProtocol.ROS_REST_HTTPS -> restClient.kickPppoeActive(router, true, activeId)
        }
    }

    suspend fun setPppoeSecretDisabled(
        router: RouterConfig,
        secretId: String,
        disabled: Boolean
    ): Result<Unit> {
        if (router.isSimulated) {
            simulatedEngine.setSecretDisabled(router.id, secretId, disabled)
            return Result.success(Unit)
        }
        return when (router.protocol) {
            RouterProtocol.ROS_API_PLAIN -> binaryClient.setPppoeSecretDisabled(router, false, secretId, disabled)
            RouterProtocol.ROS_API_TLS -> binaryClient.setPppoeSecretDisabled(router, true, secretId, disabled)
            RouterProtocol.ROS_REST_HTTP -> restClient.setPppoeSecretDisabled(router, false, secretId, disabled)
            RouterProtocol.ROS_REST_HTTPS -> restClient.setPppoeSecretDisabled(router, true, secretId, disabled)
        }
    }

    suspend fun setInterfaceDisabled(
        router: RouterConfig,
        interfaceId: String,
        disabled: Boolean
    ): Result<Unit> {
        if (router.isSimulated) {
            simulatedEngine.setInterfaceDisabled(router.id, interfaceId, disabled)
            return Result.success(Unit)
        }
        return when (router.protocol) {
            RouterProtocol.ROS_API_PLAIN -> binaryClient.setInterfaceDisabled(router, false, interfaceId, disabled)
            RouterProtocol.ROS_API_TLS -> binaryClient.setInterfaceDisabled(router, true, interfaceId, disabled)
            RouterProtocol.ROS_REST_HTTP -> restClient.setInterfaceDisabled(router, false, interfaceId, disabled)
            RouterProtocol.ROS_REST_HTTPS -> restClient.setInterfaceDisabled(router, true, interfaceId, disabled)
        }
    }

    suspend fun rebootRouter(router: RouterConfig): Result<Unit> {
        if (router.isSimulated) {
            return Result.success(Unit)
        }
        return when (router.protocol) {
            RouterProtocol.ROS_API_PLAIN -> binaryClient.rebootRouter(router, false)
            RouterProtocol.ROS_API_TLS -> binaryClient.rebootRouter(router, true)
            RouterProtocol.ROS_REST_HTTP -> restClient.rebootRouter(router, false)
            RouterProtocol.ROS_REST_HTTPS -> restClient.rebootRouter(router, true)
        }
    }

    suspend fun fetchPppProfiles(router: RouterConfig): Result<List<Map<String, String>>> {
        if (router.isSimulated) {
            return Result.success(
                listOf(
                    mapOf("name" to "15M-Hemat", "rate-limit" to "15M/15M"),
                    mapOf("name" to "20M-Home", "rate-limit" to "20M/20M"),
                    mapOf("name" to "30M-Bisnis", "rate-limit" to "30M/30M"),
                    mapOf("name" to "50M-Dedicated", "rate-limit" to "50M/50M")
                )
            )
        }
        return when (router.protocol) {
            RouterProtocol.ROS_API_PLAIN -> binaryClient.fetchPppProfiles(router, false)
            RouterProtocol.ROS_API_TLS -> binaryClient.fetchPppProfiles(router, true)
            RouterProtocol.ROS_REST_HTTP -> restClient.fetchPppProfiles(router, false)
            RouterProtocol.ROS_REST_HTTPS -> restClient.fetchPppProfiles(router, true)
        }
    }

    suspend fun fetchRadiusEntries(router: RouterConfig): Result<List<Map<String, String>>> {
        if (router.isSimulated) {
            return Result.success(
                listOf(
                    mapOf(
                        ".id" to "*1",
                        "service" to "ppp,hotspot",
                        "address" to "10.10.0.10",
                        "timeout" to "300ms",
                        "disabled" to "false"
                    )
                )
            )
        }
        return when (router.protocol) {
            RouterProtocol.ROS_API_PLAIN -> binaryClient.fetchRadiusEntries(router, false)
            RouterProtocol.ROS_API_TLS -> binaryClient.fetchRadiusEntries(router, true)
            RouterProtocol.ROS_REST_HTTP -> restClient.fetchRadiusEntries(router, false)
            RouterProtocol.ROS_REST_HTTPS -> restClient.fetchRadiusEntries(router, true)
        }
    }
}
