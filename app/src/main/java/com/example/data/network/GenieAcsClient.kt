package com.example.data.network

import com.example.data.model.CpeDeviceEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class GenieAcsClient {

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()
    }

    suspend fun fetchDevices(baseUrl: String): Result<List<CpeDeviceEntity>> =
        withContext(Dispatchers.IO) {
            val cleanBase = baseUrl.trim().trimEnd('/')
            if (cleanBase.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("URL server GenieACS NBI belum diatur"))
            }
            runCatching {
                val req = Request.Builder().url("$cleanBase/devices/").get().build()
                client.newCall(req).execute().use { resp ->
                    val raw = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) error("HTTP ${resp.code}: ${raw.take(120)}")
                    val arr = JSONArray(raw)
                    val list = mutableListOf<CpeDeviceEntity>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        val id = obj.optString("_id")
                        val devInfo = obj.optJSONObject("_deviceId")
                        val sn = devInfo?.optString("_SerialNumber").orEmpty().ifBlank { id }
                        val product = devInfo?.optString("_ProductClass").orEmpty().ifBlank { "TR-069 ONT" }
                        val lastInform = obj.optString("_lastInform", "-")
                        val ip = extractValue(obj, "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.ExternalIPAddress")
                            ?: extractValue(obj, "Device.IP.Interface.1.IPv4Address.1.IPAddress")
                            ?: "-"
                        val rx = extractValue(obj, "InternetGatewayDevice.WANDevice.1.X_GponInterafceConfig.RXPower")
                            ?: extractValue(obj, "VirtualParameters.RXPower")
                            ?: "-"
                        val ssid = extractValue(obj, "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.SSID")
                            ?: extractValue(obj, "Device.WiFi.SSID.1.SSID")
                            ?: "-"
                        val pppUser = extractValue(obj, "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANPPPConnection.1.Username")
                            ?: sn
                        list.add(
                            CpeDeviceEntity(
                                deviceId = id,
                                serialNumber = sn,
                                model = product,
                                customerName = pppUser,
                                ipAddress = ip,
                                rxPowerDbm = rx,
                                ssid = ssid,
                                isOnline = lastInform != "-",
                                lastInform = lastInform.take(19).replace("T", " ")
                            )
                        )
                    }
                    list
                }
            }
        }

    suspend fun rebootDevice(baseUrl: String, deviceId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            val cleanBase = baseUrl.trim().trimEnd('/')
            runCatching {
                val encoded = URLEncoder.encode(deviceId, "UTF-8")
                val body = JSONObject().put("name", "reboot")
                    .toString()
                    .toRequestBody("application/json".toMediaType())
                val req = Request.Builder()
                    .url("$cleanBase/devices/$encoded/tasks?connection_request")
                    .post(body)
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) error("HTTP ${resp.code}")
                }
            }
        }

    private fun extractValue(root: JSONObject, path: String): String? {
        var cur: Any? = root
        for (part in path.split(".")) {
            cur = (cur as? JSONObject)?.opt(part) ?: return null
        }
        return (cur as? JSONObject)?.opt("_value")?.toString()
    }
}
