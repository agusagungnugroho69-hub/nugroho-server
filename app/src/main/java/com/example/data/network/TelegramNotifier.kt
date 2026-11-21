package com.example.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class TelegramNotifier {

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()
    }

    suspend fun sendMessage(
        botToken: String,
        chatId: String,
        htmlMessage: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanToken = botToken.trim()
        val cleanChatId = chatId.trim()
        if (cleanToken.isBlank() || cleanChatId.isBlank()) {
            return@withContext Result.failure(
                IllegalArgumentException("Bot Token dan Chat ID Telegram belum diisi")
            )
        }
        runCatching {
            val payload = JSONObject().apply {
                put("chat_id", cleanChatId)
                put("text", htmlMessage)
                put("parse_mode", "HTML")
                put("disable_web_page_preview", true)
            }
            val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url("https://api.telegram.org/bot$cleanToken/sendMessage")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                val json = runCatching { JSONObject(raw) }.getOrNull()
                if (response.isSuccessful && json?.optBoolean("ok") == true) {
                    "Terkirim ke Telegram"
                } else {
                    val desc = json?.optString("description")?.takeIf { it.isNotBlank() }
                        ?: "HTTP ${response.code}"
                    throw IllegalStateException(desc)
                }
            }
        }
    }
}
