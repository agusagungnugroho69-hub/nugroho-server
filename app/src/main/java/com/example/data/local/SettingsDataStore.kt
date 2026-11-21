package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.TelegramSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mikromon_prefs")

class SettingsDataStore(private val context: Context) {
    private object Keys {
        val BOT_TOKEN = stringPreferencesKey("telegram_bot_token")
        val CHAT_ID = stringPreferencesKey("telegram_chat_id")
        val TELEGRAM_ENABLED = booleanPreferencesKey("telegram_enabled")
        val ALERT_ROUTER_DOWN = booleanPreferencesKey("alert_router_down")
        val ALERT_HIGH_CPU = booleanPreferencesKey("alert_high_cpu")
        val ALERT_PPPOE_CHANGE = booleanPreferencesKey("alert_pppoe_change")
        val CPU_THRESHOLD = intPreferencesKey("cpu_threshold_percent")
        val POLLING_INTERVAL = intPreferencesKey("polling_interval_sec")
        val LOCAL_NOTIF = booleanPreferencesKey("local_notif_enabled")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val ISP_NAME = stringPreferencesKey("isp_name")
        val ISP_SUBTITLE = stringPreferencesKey("isp_subtitle")
        val ADMIN_NAME = stringPreferencesKey("admin_name")
        val GENIEACS_URL = stringPreferencesKey("genieacs_url")
        val PPN_PERCENT = intPreferencesKey("ppn_percent")
    }

    private fun readFromPrefs(prefs: Preferences): TelegramSettings = TelegramSettings(
        botToken = prefs[Keys.BOT_TOKEN] ?: "",
        chatId = prefs[Keys.CHAT_ID] ?: "",
        enabled = prefs[Keys.TELEGRAM_ENABLED] ?: false,
        alertOnRouterDown = prefs[Keys.ALERT_ROUTER_DOWN] ?: true,
        alertOnHighCpu = prefs[Keys.ALERT_HIGH_CPU] ?: true,
        alertOnPppoeChange = prefs[Keys.ALERT_PPPOE_CHANGE] ?: false,
        cpuThresholdPercent = prefs[Keys.CPU_THRESHOLD] ?: 85,
        pollingIntervalSec = prefs[Keys.POLLING_INTERVAL] ?: 5,
        localNotificationsEnabled = prefs[Keys.LOCAL_NOTIF] ?: true,
        darkTheme = prefs[Keys.DARK_THEME] ?: true,
        ispName = prefs[Keys.ISP_NAME]
            ?.takeUnless { it.isBlank() || it.equals("MIKROMON NETWORK", ignoreCase = true) }
            ?.uppercase()
            ?: "NUGROHO NETWORK",
        ispSubtitle = prefs[Keys.ISP_SUBTITLE] ?: "Fiber & MikroTik Monitoring",
        adminName = prefs[Keys.ADMIN_NAME] ?: "Network Admin",
        genieAcsUrl = prefs[Keys.GENIEACS_URL] ?: "",
        ppnPercent = prefs[Keys.PPN_PERCENT] ?: 11
    )

    val settingsFlow: Flow<TelegramSettings> = context.dataStore.data.map(::readFromPrefs)

    suspend fun updateSettings(transform: (TelegramSettings) -> TelegramSettings) {
        context.dataStore.edit { prefs ->
            val updated = transform(readFromPrefs(prefs))
            prefs[Keys.BOT_TOKEN] = updated.botToken
            prefs[Keys.CHAT_ID] = updated.chatId
            prefs[Keys.TELEGRAM_ENABLED] = updated.enabled
            prefs[Keys.ALERT_ROUTER_DOWN] = updated.alertOnRouterDown
            prefs[Keys.ALERT_HIGH_CPU] = updated.alertOnHighCpu
            prefs[Keys.ALERT_PPPOE_CHANGE] = updated.alertOnPppoeChange
            prefs[Keys.CPU_THRESHOLD] = updated.cpuThresholdPercent
            prefs[Keys.POLLING_INTERVAL] = updated.pollingIntervalSec
            prefs[Keys.LOCAL_NOTIF] = updated.localNotificationsEnabled
            prefs[Keys.DARK_THEME] = updated.darkTheme
            prefs[Keys.ISP_NAME] = updated.ispName
            prefs[Keys.ISP_SUBTITLE] = updated.ispSubtitle
            prefs[Keys.ADMIN_NAME] = updated.adminName
            prefs[Keys.GENIEACS_URL] = updated.genieAcsUrl
            prefs[Keys.PPN_PERCENT] = updated.ppnPercent
        }
    }
}
