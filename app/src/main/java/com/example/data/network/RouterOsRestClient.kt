package com.example.data.network

import com.example.data.model.ConnectionTestResult
import com.example.data.model.InterfaceStats
import com.example.data.model.IpPoolInfo
import com.example.data.model.PppProfileInfo
import com.example.data.model.PppSecretInfo
import com.example.data.model.PppoeClient
import com.example.data.model.RouterConfig
import com.example.data.model.RouterTelemetry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class RouterOsRestClient {

    private val client: OkHttpClient by lazy {
        val trustAll = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        })
        val sslCtx = SSLContext.getInstance("TLS").apply {
            init(null, trustAll, SecureRandom())
        }
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .sslSocketFactory(sslCtx.socketFactory, trustAll[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .build()
    }

    private fun baseUrl(router: RouterConfig, useHttps: Boolean): String {
        val scheme = if (useHttps) "https" else "http"
        return "$scheme://${router.host.trim()}:${router.port}/rest"
    }

    private fun authHeader(router: RouterConfig): String =
        Credentials.basic(router.username, router.password)

    private fun getJsonArray(url: String, auth: String): JSONArray {
        val req = Request.Builder().url(url).header("Authorization", auth).get().build()
        client.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw IllegalStateException("HTTP ${resp.code}: ${body.take(140)}")
            }
            val trimmed = body.trim()
            return if (trimmed.startsWith("[")) JSONArray(trimmed)
            else JSONArray().apply { put(JSONObject(trimmed)) }
        }
    }

    private fun getJsonObject(url: String, auth: String): JSONObject {
        val req = Request.Builder().url(url).header("Authorization", auth).get().build()
        client.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw IllegalStateException("HTTP ${resp.code}: ${body.take(140)}")
            }
            return JSONObject(body)
        }
    }

    private fun postJsonArray(url: String, auth: String, payload: JSONObject): JSONArray {
        val reqBody = payload.toString().toRequestBody("application/json".toMediaType())
        val req = Request.Builder().url(url).header("Authorization", auth).post(reqBody).build()
        client.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw IllegalStateException("HTTP ${resp.code}: ${body.take(140)}")
            }
            val trimmed = body.trim()
            return when {
                trimmed.isEmpty() -> JSONArray()
                trimmed.startsWith("[") -> JSONArray(trimmed)
                else -> JSONArray().apply { put(JSONObject(trimmed)) }
            }
        }
    }

    suspend fun testConnection(router: RouterConfig, useHttps: Boolean): ConnectionTestResult =
        withContext(Dispatchers.IO) {
            val start = System.currentTimeMillis()
            try {
                val base = baseUrl(router, useHttps)
                val auth = authHeader(router)
                val idObj = getJsonObject("$base/system/identity", auth)
                val resObj = getJsonObject("$base/system/resource", auth)
                val ifaces = getJsonArray("$base/interface", auth)
                val ifaceNames = (0 until ifaces.length()).mapNotNull {
                    ifaces.optJSONObject(it)?.optString("name")?.takeIf { n -> n.isNotBlank() }
                }
                val identity = idObj.optString("name", router.name)
                val board = resObj.optString("board-name", "MikroTik")
                val version = resObj.optString("version", "-")
                val latency = (System.currentTimeMillis() - start).coerceAtLeast(1L)
                ConnectionTestResult(
                    success = true,
                    identity = identity,
                    boardName = board,
                    rosVersion = version,
                    latencyMs = latency,
                    interfaces = ifaceNames,
                    message = "REST API terhubung ke $identity ($board, v$version) dalam ${latency}ms"
                )
            } catch (e: Exception) {
                ConnectionTestResult(
                    success = false,
                    message = e.message ?: "Gagal menghubungi REST API RouterOS"
                )
            }
        }

    suspend fun fetchTelemetry(router: RouterConfig, useHttps: Boolean): RouterTelemetry =
        withContext(Dispatchers.IO) {
            val start = System.currentTimeMillis()
            try {
                val base = baseUrl(router, useHttps)
                val auth = authHeader(router)
                val idObj = getJsonObject("$base/system/identity", auth)
                val resObj = getJsonObject("$base/system/resource", auth)
                val ifaceArray = getJsonArray("$base/interface", auth)

                val ifaceList = mutableListOf<JSONObject>()
                for (i in 0 until ifaceArray.length()) {
                    ifaceArray.optJSONObject(i)?.let { ifaceList.add(it) }
                }
                val targetIface = router.monitoredInterface.ifBlank {
                    ifaceList.firstOrNull { it.optString("running") == "true" }?.optString("name")
                        ?: "ether1"
                }

                var rxBps = 0L
                var txBps = 0L
                if (ifaceList.any { it.optString("name") == targetIface }) {
                    val monArr = postJsonArray(
                        "$base/interface/monitor-traffic",
                        auth,
                        JSONObject().apply {
                            put("interface", targetIface)
                            put("once", "")
                        }
                    )
                    val monObj = monArr.optJSONObject(0)
                    rxBps = monObj?.optString("rx-bits-per-second")?.toLongOrNull() ?: 0L
                    txBps = monObj?.optString("tx-bits-per-second")?.toLongOrNull() ?: 0L
                }

                val activeArr = runCatching { getJsonArray("$base/ppp/active", auth) }
                    .getOrDefault(JSONArray())
                val secretArr = runCatching { getJsonArray("$base/ppp/secret", auth) }
                    .getOrDefault(JSONArray())
                val profileArr = runCatching { getJsonArray("$base/ppp/profile", auth) }
                    .getOrDefault(JSONArray())
                val poolArr = runCatching { getJsonArray("$base/ip/pool", auth) }
                    .getOrDefault(JSONArray())

                val activeMap = mutableMapOf<String, JSONObject>()
                for (i in 0 until activeArr.length()) {
                    val obj = activeArr.optJSONObject(i) ?: continue
                    val name = obj.optString("name")
                    if (name.isNotBlank()) activeMap[name] = obj
                }

                val secretMap = mutableMapOf<String, JSONObject>()
                val secretList = mutableListOf<JSONObject>()
                for (i in 0 until secretArr.length()) {
                    val obj = secretArr.optJSONObject(i) ?: continue
                    secretList.add(obj)
                    val name = obj.optString("name")
                    if (name.isNotBlank()) secretMap[name] = obj
                }

                val activeNames = mutableSetOf<String>()
                val clients = mutableListOf<PppoeClient>()
                for (i in 0 until activeArr.length()) {
                    val active = activeArr.optJSONObject(i) ?: continue
                    val uName = active.optString("name")
                    if (uName.isBlank()) continue
                    activeNames.add(uName)
                    val sec = secretMap[uName]
                    clients.add(
                        PppoeClient(
                            id = active.optString(".id", uName),
                            secretId = sec?.optString(".id"),
                            name = uName,
                            service = active.optString("service", "pppoe"),
                            callerId = active.optString("caller-id", "-"),
                            address = active.optString("address", "-"),
                            uptime = active.optString("uptime", "-"),
                            profile = sec?.optString("profile", "default") ?: "default",
                            encoding = active.optString("encoding", ""),
                            isOnline = true,
                            isDisabled = sec?.optString("disabled") == "true",
                            lastLoggedOut = sec?.optString("last-logged-out", "") ?: "",
                            comment = sec?.optString("comment", "") ?: "",
                            routerId = router.id,
                            routerName = router.name
                        )
                    )
                }

                for (sec in secretList) {
                    val uName = sec.optString("name")
                    if (uName.isNotBlank() && !activeNames.contains(uName)) {
                        clients.add(
                            PppoeClient(
                                id = sec.optString(".id", uName),
                                secretId = sec.optString(".id"),
                                name = uName,
                                service = sec.optString("service", "pppoe"),
                                callerId = sec.optString("last-caller-id", "-"),
                                address = sec.optString("remote-address", "-"),
                                uptime = "-",
                                profile = sec.optString("profile", "default"),
                                isOnline = false,
                                isDisabled = sec.optString("disabled") == "true",
                                lastLoggedOut = sec.optString("last-logged-out", "-"),
                                comment = sec.optString("comment", ""),
                                routerId = router.id,
                                routerName = router.name
                            )
                        )
                    }
                }

                val pppSecrets = secretList.mapNotNull { sec ->
                    val uName = sec.optString("name").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    val active = activeMap[uName]
                    PppSecretInfo(
                        id = sec.optString(".id", uName),
                        name = uName,
                        password = sec.optString("password", ""),
                        service = sec.optString("service", "pppoe"),
                        profile = sec.optString("profile", "default"),
                        remoteAddress = sec.optString("remote-address").ifBlank { "-" },
                        localAddress = sec.optString("local-address").ifBlank { "-" },
                        callerId = active?.optString("caller-id")?.ifBlank { "-" }
                            ?: sec.optString("last-caller-id").ifBlank { "-" },
                        lastLoggedOut = sec.optString("last-logged-out", ""),
                        comment = sec.optString("comment", ""),
                        isDisabled = sec.optString("disabled") == "true",
                        isOnline = active != null,
                        activeAddress = active?.optString("address")?.ifBlank { "-" } ?: "-",
                        activeUptime = active?.optString("uptime")?.ifBlank { "-" } ?: "-"
                    )
                }

                val pppProfiles = (0 until profileArr.length()).mapNotNull { idx ->
                    val prof = profileArr.optJSONObject(idx) ?: return@mapNotNull null
                    val pName = prof.optString("name").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    PppProfileInfo(
                        id = prof.optString(".id", pName),
                        name = pName,
                        localAddress = prof.optString("local-address").ifBlank { "-" },
                        remoteAddress = prof.optString("remote-address").ifBlank { "-" },
                        rateLimit = prof.optString("rate-limit").ifBlank { "-" },
                        onlyOne = prof.optString("only-one", "default"),
                        comment = prof.optString("comment", "")
                    )
                }

                val ipPools = (0 until poolArr.length()).mapNotNull { idx ->
                    val pool = poolArr.optJSONObject(idx) ?: return@mapNotNull null
                    val poolName = pool.optString("name").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    IpPoolInfo(
                        id = pool.optString(".id", poolName),
                        name = poolName,
                        ranges = pool.optString("ranges").ifBlank { "-" },
                        nextPool = pool.optString("next-pool", ""),
                        comment = pool.optString("comment", "")
                    )
                }

                val interfaces = ifaceList.map { item ->
                    val iName = item.optString("name", "-")
                    InterfaceStats(
                        id = item.optString(".id", iName),
                        name = iName,
                        type = item.optString("type", "ether"),
                        isRunning = item.optString("running") == "true",
                        isDisabled = item.optString("disabled") == "true",
                        macAddress = item.optString("mac-address", "-"),
                        rxBytes = item.optString("rx-byte").toLongOrNull() ?: 0L,
                        txBytes = item.optString("tx-byte").toLongOrNull() ?: 0L,
                        rxBps = if (iName == targetIface) rxBps else 0L,
                        txBps = if (iName == targetIface) txBps else 0L,
                        comment = item.optString("comment", "")
                    )
                }

                val totalSecrets = maxOf(secretList.size, activeArr.length())
                val offlineCount = (totalSecrets - activeArr.length()).coerceAtLeast(0)
                val latency = (System.currentTimeMillis() - start).coerceAtLeast(1L)

                RouterTelemetry(
                    routerId = router.id,
                    isOnline = true,
                    identity = idObj.optString("name", router.name),
                    boardName = resObj.optString("board-name", "MikroTik"),
                    rosVersion = resObj.optString("version", "-"),
                    architecture = resObj.optString("architecture-name", "-"),
                    cpuModel = resObj.optString("cpu").ifBlank { resObj.optString("architecture-name", "-") },
                    cpuLoadPercent = resObj.optString("cpu-load").toIntOrNull() ?: 0,
                    cpuCount = resObj.optString("cpu-count").toIntOrNull() ?: 1,
                    freeMemoryBytes = resObj.optString("free-memory").toLongOrNull() ?: 0L,
                    totalMemoryBytes = resObj.optString("total-memory").toLongOrNull() ?: 0L,
                    freeHddBytes = resObj.optString("free-hdd-space").toLongOrNull() ?: 0L,
                    totalHddBytes = resObj.optString("total-hdd-space").toLongOrNull() ?: 0L,
                    uptime = resObj.optString("uptime", "-"),
                    activePppoeCount = activeArr.length(),
                    totalSecretCount = totalSecrets,
                    offlineSecretCount = offlineCount,
                    rxBitsPerSec = rxBps,
                    txBitsPerSec = txBps,
                    monitoredInterface = targetIface,
                    interfaces = interfaces,
                    pppoeClients = clients,
                    pppSecrets = pppSecrets,
                    pppProfiles = pppProfiles,
                    ipPools = ipPools,
                    latencyMs = latency,
                    lastUpdatedMillis = System.currentTimeMillis(),
                    errorMessage = null,
                    isSimulated = false
                )
            } catch (e: Exception) {
                RouterTelemetry(
                    routerId = router.id,
                    isOnline = false,
                    identity = router.name,
                    monitoredInterface = router.monitoredInterface,
                    lastUpdatedMillis = System.currentTimeMillis(),
                    errorMessage = e.message ?: "REST API tidak dapat dijangkau",
                    isSimulated = false
                )
            }
        }

    suspend fun kickPppoeActive(router: RouterConfig, useHttps: Boolean, activeId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = "${baseUrl(router, useHttps)}/ppp/active/$activeId"
                val req = Request.Builder()
                    .url(url)
                    .header("Authorization", authHeader(router))
                    .delete()
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) error("HTTP ${resp.code}")
                }
            }
        }

    suspend fun setPppoeSecretDisabled(
        router: RouterConfig,
        useHttps: Boolean,
        secretId: String,
        disabled: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val url = "${baseUrl(router, useHttps)}/ppp/secret/$secretId"
            val payload = JSONObject().put("disabled", disabled.toString())
                .toString()
                .toRequestBody("application/json".toMediaType())
            val req = Request.Builder()
                .url(url)
                .header("Authorization", authHeader(router))
                .patch(payload)
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error("HTTP ${resp.code}")
            }
        }
    }

    suspend fun setInterfaceDisabled(
        router: RouterConfig,
        useHttps: Boolean,
        interfaceId: String,
        disabled: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val url = "${baseUrl(router, useHttps)}/interface/$interfaceId"
            val payload = JSONObject().put("disabled", disabled.toString())
                .toString()
                .toRequestBody("application/json".toMediaType())
            val req = Request.Builder()
                .url(url)
                .header("Authorization", authHeader(router))
                .patch(payload)
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error("HTTP ${resp.code}")
            }
        }
    }

    suspend fun rebootRouter(router: RouterConfig, useHttps: Boolean): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                postJsonArray(
                    "${baseUrl(router, useHttps)}/system/reboot",
                    authHeader(router),
                    JSONObject()
                )
                Unit
            }
        }

    suspend fun fetchPppProfiles(router: RouterConfig, useHttps: Boolean): Result<List<Map<String, String>>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val arr = getJsonArray("${baseUrl(router, useHttps)}/ppp/profile", authHeader(router))
                jsonArrayToMaps(arr)
            }
        }

    suspend fun fetchRadiusEntries(router: RouterConfig, useHttps: Boolean): Result<List<Map<String, String>>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val arr = getJsonArray("${baseUrl(router, useHttps)}/radius", authHeader(router))
                jsonArrayToMaps(arr)
            }
        }

    private fun jsonArrayToMaps(arr: org.json.JSONArray): List<Map<String, String>> {
        val list = mutableListOf<Map<String, String>>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val map = mutableMapOf<String, String>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k] = obj.optString(k)
            }
            list.add(map)
        }
        return list
    }
}
