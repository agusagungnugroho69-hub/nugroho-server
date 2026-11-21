package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AlertLogEntity
import com.example.data.model.AlertType
import com.example.data.model.TelegramSettings
import com.example.ui.components.StatusBadge
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.NocAmber
import com.example.ui.theme.NocBlueDownload
import com.example.ui.theme.NocEmeraldOnline
import com.example.ui.theme.NocRedOffline
import com.example.util.Formatters
import kotlin.math.roundToInt

@Composable
fun AlertsTelegramScreen(
    settings: TelegramSettings,
    alertLogs: List<AlertLogEntity>,
    isTestingTelegram: Boolean,
    hasNotificationPermission: () -> Boolean,
    onUpdateSettings: ((TelegramSettings) -> TelegramSettings) -> Unit,
    onSendTestTelegram: (String, String) -> Unit,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    var botToken by remember(settings.botToken) { mutableStateOf(settings.botToken) }
    var chatId by remember(settings.chatId) { mutableStateOf(settings.chatId) }
    var showToken by remember { mutableStateOf(false) }
    var cpuSlider by remember(settings.cpuThresholdPercent) {
        mutableFloatStateOf(settings.cpuThresholdPercent.toFloat())
    }

    LaunchedEffect(settings.botToken, settings.chatId) {
        if (botToken.isEmpty() && settings.botToken.isNotEmpty()) botToken = settings.botToken
        if (chatId.isEmpty() && settings.chatId.isNotEmpty()) chatId = settings.chatId
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onUpdateSettings { it.copy(localNotificationsEnabled = granted) }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Notifikasi & Telegram",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Telegram Bot Card
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Integrasi Bot Telegram",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Kirim alert otomatis saat router DOWN atau CPU tinggi",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.enabled,
                            onCheckedChange = { checked ->
                                onUpdateSettings {
                                    it.copy(
                                        botToken = botToken.trim(),
                                        chatId = chatId.trim(),
                                        enabled = checked
                                    )
                                }
                            },
                            modifier = Modifier.testTag("telegram_enable_switch")
                        )
                    }

                    OutlinedTextField(
                        value = botToken,
                        onValueChange = { botToken = it },
                        label = { Text("Telegram Bot Token") },
                        placeholder = { Text("123456789:AAH...") },
                        singleLine = true,
                        visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            TextButton(onClick = { showToken = !showToken }) {
                                Text(if (showToken) "Sembunyi" else "Lihat", fontSize = 11.sp)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("telegram_token_input")
                    )

                    OutlinedTextField(
                        value = chatId,
                        onValueChange = { chatId = it },
                        label = { Text("Chat ID / Group ID") },
                        placeholder = { Text("-1001234567890 atau 987654321") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("telegram_chatid_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onUpdateSettings {
                                    it.copy(
                                        botToken = botToken.trim(),
                                        chatId = chatId.trim()
                                    )
                                }
                                onSendTestTelegram(botToken.trim(), chatId.trim())
                            },
                            enabled = !isTestingTelegram && botToken.isNotBlank() && chatId.isNotBlank(),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("test_telegram_button")
                        ) {
                            if (isTestingTelegram) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Rounded.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Tes Telegram", fontSize = 13.sp)
                            }
                        }

                        Button(
                            onClick = {
                                onUpdateSettings {
                                    it.copy(
                                        botToken = botToken.trim(),
                                        chatId = chatId.trim()
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("save_telegram_button")
                        ) {
                            Text("Simpan Konfig", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Alert Triggers & Polling Settings Card
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Aturan Pemicu & Polling",
                        style = MaterialTheme.typography.titleMedium
                    )

                    SettingToggleRow(
                        title = "Notifikasi Router DOWN / UP",
                        subtitle = "Peringatan instan saat status koneksi berubah",
                        checked = settings.alertOnRouterDown,
                        onCheckedChange = { c -> onUpdateSettings { it.copy(alertOnRouterDown = c) } }
                    )

                    SettingToggleRow(
                        title = "Alert Beban CPU Tinggi",
                        subtitle = "Peringatan saat CPU melampaui ambang batas",
                        checked = settings.alertOnHighCpu,
                        onCheckedChange = { c -> onUpdateSettings { it.copy(alertOnHighCpu = c) } }
                    )

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Ambang Batas CPU Global", style = MaterialTheme.typography.bodySmall)
                            Text(
                                "${cpuSlider.roundToInt()}%",
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = cpuSlider,
                            onValueChange = { cpuSlider = it },
                            onValueChangeFinished = {
                                onUpdateSettings { it.copy(cpuThresholdPercent = cpuSlider.roundToInt()) }
                            },
                            valueRange = 50f..98f,
                            steps = 47
                        )
                    }

                    SettingToggleRow(
                        title = "Alert Perubahan PPPoE Aktif",
                        subtitle = "Laporkan saat jumlah pelanggan PPPoE naik/turun",
                        checked = settings.alertOnPppoeChange,
                        onCheckedChange = { c -> onUpdateSettings { it.copy(alertOnPppoeChange = c) } }
                    )

                    SettingToggleRow(
                        title = "Notifikasi Sistem Android",
                        subtitle = "Tampilkan notifikasi lokal di bar status HP",
                        checked = settings.localNotificationsEnabled,
                        onCheckedChange = { wantEnable ->
                            if (wantEnable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission()) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                onUpdateSettings { it.copy(localNotificationsEnabled = wantEnable) }
                            }
                        }
                    )

                    SettingToggleRow(
                        title = "Tema Gelap NOC",
                        subtitle = "Tampilan kontras tinggi untuk monitoring malam",
                        checked = settings.darkTheme,
                        onCheckedChange = { c -> onUpdateSettings { it.copy(darkTheme = c) } }
                    )

                    Text(
                        text = "Interval Polling Real-Time",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(3, 5, 10, 30).forEach { sec ->
                            FilterChip(
                                selected = settings.pollingIntervalSec == sec,
                                onClick = { onUpdateSettings { it.copy(pollingIntervalSec = sec) } },
                                label = { Text("$sec detik") }
                            )
                        }
                    }
                }
            }
        }

        // Alert Logs Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Riwayat Insiden & Telegram (${alertLogs.size})",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                if (alertLogs.isNotEmpty()) {
                    IconButton(onClick = onClearLogs) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteSweep,
                            contentDescription = "Bersihkan riwayat",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (alertLogs.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = "Belum ada catatan insiden atau notifikasi terkirim.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(alertLogs, key = { it.id }) { log ->
                AlertLogCard(log = log)
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun AlertLogCard(log: AlertLogEntity) {
    val typeColor = when (log.type) {
        AlertType.ROUTER_DOWN -> NocRedOffline
        AlertType.ROUTER_UP -> NocEmeraldOnline
        AlertType.CPU_HIGH -> NocAmber
        AlertType.PPPOE_CHANGE -> NocBlueDownload
        AlertType.TELEGRAM_TEST -> NocEmeraldOnline
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge(label = log.type.label, color = typeColor)
                Text(
                    text = Formatters.formatDateTime(log.timestamp),
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = log.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Telegram: ${log.telegramStatus}",
                fontFamily = JetBrainsMonoFamily,
                fontSize = 11.sp,
                color = if (log.telegramSent) NocEmeraldOnline else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
