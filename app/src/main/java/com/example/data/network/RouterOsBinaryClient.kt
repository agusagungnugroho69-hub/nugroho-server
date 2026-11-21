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
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class RouterOsBinaryClient {

    private class ApiSession(
        private val socket: Socket,
        private val input: InputStream,
        private val output: OutputStream
    ) : AutoCloseable {

        fun sendCommand(words: List<String>) {
            val buffer = ByteArrayOutputStream()
            for (word in words) {
                val bytes = word.toByteArray(Charsets.UTF_8)
                writeLength(buffer, bytes.size)
                buffer.write(bytes)
            }
            buffer.write(0) // End of sentence
            output.write(buffer.toByteArray())
            output.flush()
        }

        fun readResponse(): Pair<List<Map<String, String>>, String?> {
            val items = mutableListOf<Map<String, String>>()
            var trapMessage: String? = null
            while (true) {
                val sentence = readSentence()
                if (sentence.isEmpty()) continue
                val reply = sentence.first()
                val attrs = mutableMapOf<String, String>()
                for (i in 1 until sentence.size) {
                    val word = sentence[i]
                    if (word.startsWith("=")) {
                        val eqIdx = word.indexOf('=', 1)
                        if (eqIdx > 1) {
                            attrs[word.substring(1, eqIdx)] = word.substring(eqIdx + 1)
                        } else {
                            attrs[word.substring(1)] = ""
                        }
                    }
                }
                when (reply) {
                    "!re" -> items.add(attrs)
                    "!trap", "!fatal" -> {
                        trapMessage = attrs["message"] ?: "RouterOS API error ($reply)"
                        if (reply == "!fatal") return items to trapMessage
                    }
                    "!done" -> {
                        if (attrs.isNotEmpty() && items.isEmpty()) {
                            items.add(attrs)
                        }
                        return items to trapMessage
                    }
                }
            }
        }

        private fun readSentence(): List<String> {
            val words = mutableListOf<String>()
            while (true) {
                val len = readLength()
                if (len == 0) break
                val data = ByteArray(len)
                var offset = 0
                while (offset < len) {
                    val read = input.read(data, offset, len - offset)
                    if (read == -1) throw IllegalStateException("Koneksi terputus oleh router")
                    offset += read
                }
                words.add(String(data, Charsets.UTF_8))
            }
            return words
        }

        private fun readLength(): Int {
            val b0 = input.read()
            if (b0 == -1) throw IllegalStateException("Stream berakhir tak terduga")
            return when {
                (b0 and 0x80) == 0 -> b0
                (b0 and 0xC0) == 0x80 -> {
                    val b1 = readByteOrThrow()
                    ((b0 and 0x3F) shl 8) or b1
                }
                (b0 and 0xE0) == 0xC0 -> {
                    val b1 = readByteOrThrow()
                    val b2 = readByteOrThrow()
                    ((b0 and 0x1F) shl 16) or (b1 shl 8) or b2
                }
                (b0 and 0xF0) == 0xE0 -> {
                    val b1 = readByteOrThrow()
                    val b2 = readByteOrThrow()
                    val b3 = readByteOrThrow()
                    ((b0 and 0x0F) shl 24) or (b1 shl 16) or (b2 shl 8) or b3
                }
                else -> {
                    val b1 = readByteOrThrow()
                    val b2 = readByteOrThrow()
                    val b3 = readByteOrThrow()
                    val b4 = readByteOrThrow()
                    (b1 shl 24) or (b2 shl 16) or (b3 shl 8) or b4
                }
            }
        }

        private fun readByteOrThrow(): Int {
            val b = input.read()
            if (b == -1) throw IllegalStateException("Stream berakhir tak terduga")
            return b
        }

        private fun writeLength(out: ByteArrayOutputStream, len: Int) {
            when {
                len < 0x80 -> out.write(len)
                len < 0x4000 -> {
                    val v = len or 0x8000
                    out.write((v shr 8) and 0xFF)
                    out.write(v and 0xFF)
                }
                len < 0x200000 -> {
                    val v = len or 0xC00000
                    out.write((v shr 16) and 0xFF)
                    out.write((v shr 8) and 0xFF)
                    out.write(v and 0xFF)
                }
                len < 0x10000000 -> {
                    val v = len or -0x20000000
                    out.write((v shr 24) and 0xFF)
                    out.write((v shr 16) and 0xFF)
                    out.write((v shr 8) and 0xFF)
                    out.write(v and 0xFF)
                }
                else -> {
                    out.write(0xF0)
                    out.write((len shr 24) and 0xFF)
                    out.write((len shr 16) and 0xFF)
                    out.write((len shr 8) and 0xFF)
                    out.write(len and 0xFF)
                }
            }
        }

        override fun close() {
            runCatching { socket.close() }
        }
    }

    private fun openSession(router: RouterConfig, useTls: Boolean, timeoutMs: Int = 4500): ApiSession {
        val rawSocket = Socket()
        rawSocket.connect(InetSocketAddress(router.host.trim(), router.port), timeoutMs)
        rawSocket.soTimeout = timeoutMs

        val finalSocket: Socket = if (useTls) {
            val trustAll = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
            })
            val sslCtx = SSLContext.getInstance("TLS")
            sslCtx.init(null, trustAll, SecureRandom())
            val sslSocket = sslCtx.socketFactory.createSocket(
                rawSocket,
                router.host.trim(),
                router.port,
                true
            ) as SSLSocket
            sslSocket.soTimeout = timeoutMs
            sslSocket.startHandshake()
            sslSocket
        } else {
            rawSocket
        }

        val session = ApiSession(
            socket = finalSocket,
            input = BufferedInputStream(finalSocket.getInputStream()),
            output = finalSocket.getOutputStream()
        )
        login(session, router.username, router.password)
        return session
    }

    private fun login(session: ApiSession, username: String, password: String) {
        session.sendCommand(listOf("/login", "=name=$username", "=password=$password"))
        val (items, trap) = session.readResponse()
        if (trap != null) {
            session.close()
            throw IllegalStateException("Login gagal: $trap")
        }
        val retChallenge = items.firstOrNull()?.get("ret")
        if (!retChallenge.isNullOrBlank()) {
            // Legacy RouterOS (< 6.43) MD5 challenge-response login
            val challengeBytes = hexToBytes(retChallenge)
            val digest = MessageDigest.getInstance("MD5")
            digest.update(0.toByte())
            digest.update(password.toByteArray(Charsets.UTF_8))
            digest.update(challengeBytes)
            val responseHex = "00" + digest.digest().joinToString("") { "%02x".format(it) }
            session.sendCommand(listOf("/login", "=name=$username", "=response=$responseHex"))
            val (_, legacyTrap) = session.readResponse()
            if (legacyTrap != null) {
                session.close()
                throw IllegalStateException("Login legacy gagal: $legacyTrap")
            }
        }
    }

    suspend fun testConnection(router: RouterConfig, useTls: Boolean): ConnectionTestResult =
        withContext(Dispatchers.IO) {
            val start = System.currentTimeMillis()
            try {
                openSession(router, useTls, timeoutMs = 5000).use { session ->
                    session.sendCommand(listOf("/system/identity/print"))
                    val (idItems, _) = session.readResponse()
                    val identity = idItems.firstOrNull()?.get("name") ?: router.name

                    session.sendCommand(listOf("/system/resource/print"))
                    val (resItems, _) = session.readResponse()
                    val res = resItems.firstOrNull().orEmpty()

                    session.sendCommand(listOf("/interface/print"))
                    val (ifaceItems, _) = session.readResponse()
                    val ifaceNames = ifaceItems.mapNotNull { it["name"] }

                    val latency = (System.currentTimeMillis() - start).coerceAtLeast(1L)
                    ConnectionTestResult(
                        success = true,
                        identity = identity,
                        boardName = res["board-name"] ?: "MikroTik",
                        rosVersion = res["version"] ?: "-",
                        latencyMs = latency,
                        interfaces = ifaceNames,
                        message = "Terhubung ke $identity (${res["board-name"] ?: "MikroTik"}, v${res["version"] ?: "?"}) dalam ${latency}ms"
                    )
                }
            } catch (e: Exception) {
                ConnectionTestResult(
                    success = false,
                    message = e.message ?: "Gagal terhubung ke ${router.host}:${router.port}"
                )
            }
        }

    suspend fun fetchTelemetry(router: RouterConfig, useTls: Boolean): RouterTelemetry =
        withContext(Dispatchers.IO) {
            val start = System.currentTimeMillis()
            try {
                openSession(router, useTls, timeoutMs = 5000).use { session ->
                    session.sendCommand(listOf("/system/identity/print"))
                    val (idItems, _) = session.readResponse()
                    val identity = idItems.firstOrNull()?.get("name") ?: router.name

                    session.sendCommand(listOf("/system/resource/print"))
                    val (resItems, _) = session.readResponse()
                    val res = resItems.firstOrNull().orEmpty()

                    session.sendCommand(listOf("/interface/print"))
                    val (ifaceItems, _) = session.readResponse()

                    val targetIface = router.monitoredInterface.ifBlank {
                        ifaceItems.firstOrNull { it["running"] == "true" }?.get("name") ?: "ether1"
                    }

                    var rxBps = 0L
                    var txBps = 0L
                    if (ifaceItems.any { it["name"] == targetIface }) {
                        session.sendCommand(
                            listOf(
                                "/interface/monitor-traffic",
                                "=interface=$targetIface",
                                "=once="
                            )
                        )
                        val (trafficItems, _) = session.readResponse()
                        val traffic = trafficItems.firstOrNull().orEmpty()
                        rxBps = traffic["rx-bits-per-second"]?.toLongOrNull() ?: 0L
                        txBps = traffic["tx-bits-per-second"]?.toLongOrNull() ?: 0L
                    }

                    session.sendCommand(listOf("/ppp/active/print"))
                    val (activeItems, _) = session.readResponse()

                    session.sendCommand(listOf("/ppp/secret/print"))
                    val (secretItems, _) = session.readResponse()

                    val profileItems = runCatching {
                        session.sendCommand(listOf("/ppp/profile/print"))
                        val (items, trap) = session.readResponse()
                        if (trap != null) emptyList() else items
                    }.getOrDefault(emptyList())

                    val poolItems = runCatching {
                        session.sendCommand(listOf("/ip/pool/print"))
                        val (items, trap) = session.readResponse()
                        if (trap != null) emptyList() else items
                    }.getOrDefault(emptyList())

                    val interfaces = ifaceItems.map { item ->
                        val iName = item["name"] ?: "-"
                        InterfaceStats(
                            id = item[".id"] ?: iName,
                            name = iName,
                            type = item["type"] ?: "ether",
                            isRunning = item["running"] == "true",
                            isDisabled = item["disabled"] == "true",
                            macAddress = item["mac-address"] ?: "-",
                            rxBytes = item["rx-byte"]?.toLongOrNull() ?: 0L,
                            txBytes = item["tx-byte"]?.toLongOrNull() ?: 0L,
                            rxBps = if (iName == targetIface) rxBps else 0L,
                            txBps = if (iName == targetIface) txBps else 0L,
                            comment = item["comment"] ?: ""
                        )
                    }

                    val activeByName = activeItems.associateBy { it["name"] ?: "" }
                    val secretByName = secretItems.associateBy { it["name"] ?: "" }
                    val activeNames = mutableSetOf<String>()
                    val clients = mutableListOf<PppoeClient>()

                    for (active in activeItems) {
                        val uName = active["name"] ?: continue
                        activeNames.add(uName)
                        val sec = secretByName[uName]
                        clients.add(
                            PppoeClient(
                                id = active[".id"] ?: uName,
                                secretId = sec?.get(".id"),
                                name = uName,
                                service = active["service"] ?: "pppoe",
                                callerId = active["caller-id"] ?: "-",
                                address = active["address"] ?: "-",
                                uptime = active["uptime"] ?: "-",
                                profile = sec?.get("profile") ?: "default",
                                encoding = active["encoding"] ?: "",
                                isOnline = true,
                                isDisabled = sec?.get("disabled") == "true",
                                lastLoggedOut = sec?.get("last-logged-out") ?: "",
                                comment = sec?.get("comment") ?: active["comment"] ?: "",
                                routerId = router.id,
                                routerName = router.name
                            )
                        )
                    }

                    for (sec in secretItems) {
                        val uName = sec["name"] ?: continue
                        if (!activeNames.contains(uName)) {
                            clients.add(
                                PppoeClient(
                                    id = sec[".id"] ?: uName,
                                    secretId = sec[".id"],
                                    name = uName,
                                    service = sec["service"] ?: "pppoe",
                                    callerId = sec["last-caller-id"] ?: "-",
                                    address = sec["remote-address"] ?: "-",
                                    uptime = "-",
                                    profile = sec["profile"] ?: "default",
                                    isOnline = false,
                                    isDisabled = sec["disabled"] == "true",
                                    lastLoggedOut = sec["last-logged-out"] ?: "-",
                                    comment = sec["comment"] ?: "",
                                    routerId = router.id,
                                    routerName = router.name
                                )
                            )
                        }
                    }

                    val pppSecrets = secretItems.mapNotNull { sec ->
                        val uName = sec["name"] ?: return@mapNotNull null
                        val active = activeByName[uName]
                        PppSecretInfo(
                            id = sec[".id"] ?: uName,
                            name = uName,
                            password = sec["password"] ?: "",
                            service = sec["service"] ?: "pppoe",
                            profile = sec["profile"] ?: "default",
                            remoteAddress = sec["remote-address"]?.ifBlank { "-" } ?: "-",
                            localAddress = sec["local-address"]?.ifBlank { "-" } ?: "-",
                            callerId = active?.get("caller-id") ?: sec["last-caller-id"] ?: "-",
                            lastLoggedOut = sec["last-logged-out"] ?: "",
                            comment = sec["comment"] ?: "",
                            isDisabled = sec["disabled"] == "true",
                            isOnline = active != null,
                            activeAddress = active?.get("address") ?: "-",
                            activeUptime = active?.get("uptime") ?: "-"
                        )
                    }

                    val pppProfiles = profileItems.mapNotNull { prof ->
                        val pName = prof["name"] ?: return@mapNotNull null
                        PppProfileInfo(
                            id = prof[".id"] ?: pName,
                            name = pName,
                            localAddress = prof["local-address"]?.ifBlank { "-" } ?: "-",
                            remoteAddress = prof["remote-address"]?.ifBlank { "-" } ?: "-",
                            rateLimit = prof["rate-limit"]?.ifBlank { "-" } ?: "-",
                            onlyOne = prof["only-one"] ?: "default",
                            comment = prof["comment"] ?: ""
                        )
                    }

                    val ipPools = poolItems.mapNotNull { pool ->
                        val poolName = pool["name"] ?: return@mapNotNull null
                        IpPoolInfo(
                            id = pool[".id"] ?: poolName,
                            name = poolName,
                            ranges = pool["ranges"]?.ifBlank { "-" } ?: "-",
                            nextPool = pool["next-pool"] ?: "",
                            comment = pool["comment"] ?: ""
                        )
                    }

                    val totalSecrets = maxOf(secretItems.size, activeItems.size)
                    val offlineCount = (totalSecrets - activeItems.size).coerceAtLeast(0)
                    val latency = (System.currentTimeMillis() - start).coerceAtLeast(1L)

                    RouterTelemetry(
                        routerId = router.id,
                        isOnline = true,
                        identity = identity,
                        boardName = res["board-name"] ?: "MikroTik",
                        rosVersion = res["version"] ?: "-",
                        architecture = res["architecture-name"] ?: "-",
                        cpuModel = res["cpu"]?.ifBlank { res["architecture-name"] ?: "-" } ?: "-",
                        cpuLoadPercent = res["cpu-load"]?.toIntOrNull() ?: 0,
                        cpuCount = res["cpu-count"]?.toIntOrNull() ?: 1,
                        freeMemoryBytes = res["free-memory"]?.toLongOrNull() ?: 0L,
                        totalMemoryBytes = res["total-memory"]?.toLongOrNull() ?: 0L,
                        freeHddBytes = res["free-hdd-space"]?.toLongOrNull() ?: 0L,
                        totalHddBytes = res["total-hdd-space"]?.toLongOrNull() ?: 0L,
                        uptime = res["uptime"] ?: "-",
                        activePppoeCount = activeItems.size,
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
                }
            } catch (e: Exception) {
                RouterTelemetry(
                    routerId = router.id,
                    isOnline = false,
                    identity = router.name,
                    monitoredInterface = router.monitoredInterface,
                    lastUpdatedMillis = System.currentTimeMillis(),
                    errorMessage = e.message ?: "Router tidak dapat dijangkau",
                    isSimulated = false
                )
            }
        }

    suspend fun kickPppoeActive(router: RouterConfig, useTls: Boolean, activeId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                openSession(router, useTls).use { session ->
                    session.sendCommand(listOf("/ppp/active/remove", "=.id=$activeId"))
                    val (_, trap) = session.readResponse()
                    if (trap != null) error(trap)
                }
            }
        }

    suspend fun setPppoeSecretDisabled(
        router: RouterConfig,
        useTls: Boolean,
        secretId: String,
        disabled: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            openSession(router, useTls).use { session ->
                session.sendCommand(
                    listOf(
                        "/ppp/secret/set",
                        "=.id=$secretId",
                        "=disabled=${if (disabled) "yes" else "no"}"
                    )
                )
                val (_, trap) = session.readResponse()
                if (trap != null) error(trap)
            }
        }
    }

    suspend fun setInterfaceDisabled(
        router: RouterConfig,
        useTls: Boolean,
        interfaceId: String,
        disabled: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            openSession(router, useTls).use { session ->
                session.sendCommand(
                    listOf(
                        "/interface/set",
                        "=.id=$interfaceId",
                        "=disabled=${if (disabled) "yes" else "no"}"
                    )
                )
                val (_, trap) = session.readResponse()
                if (trap != null) error(trap)
            }
        }
    }

    suspend fun rebootRouter(router: RouterConfig, useTls: Boolean): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                openSession(router, useTls).use { session ->
                    session.sendCommand(listOf("/system/reboot"))
                    session.readResponse()
                    Unit
                }
            }
        }

    suspend fun fetchPppProfiles(router: RouterConfig, useTls: Boolean): Result<List<Map<String, String>>> =
        withContext(Dispatchers.IO) {
            runCatching {
                openSession(router, useTls).use { session ->
                    session.sendCommand(listOf("/ppp/profile/print"))
                    val (items, trap) = session.readResponse()
                    if (trap != null) error(trap)
                    items
                }
            }
        }

    suspend fun fetchRadiusEntries(router: RouterConfig, useTls: Boolean): Result<List<Map<String, String>>> =
        withContext(Dispatchers.IO) {
            runCatching {
                openSession(router, useTls).use { session ->
                    session.sendCommand(listOf("/radius/print"))
                    val (items, trap) = session.readResponse()
                    if (trap != null) error(trap)
                    items
                }
            }
        }

    private fun hexToBytes(hex: String): ByteArray {
        val clean = hex.trim()
        val len = clean.length
        val out = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            out[i / 2] = ((Character.digit(clean[i], 16) shl 4) +
                Character.digit(clean[i + 1], 16)).toByte()
            i += 2
        }
        return out
    }
}
