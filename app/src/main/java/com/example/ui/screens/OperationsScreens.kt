package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActivityLogEntity
import com.example.data.model.SalesRepEntity
import com.example.data.model.SupportTicketEntity
import com.example.data.model.TelegramSettings
import com.example.data.model.TicketPriority
import com.example.data.model.TicketStatus
import com.example.ui.components.ModuleScaffold
import com.example.ui.components.ResourceGaugeRow
import com.example.ui.components.StatusBadge
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.NocAmber
import com.example.ui.theme.NocBlueDownload
import com.example.ui.theme.NocEmeraldOnline
import com.example.ui.theme.NocPurpleAccent
import com.example.ui.theme.NocRedOffline
import com.example.util.Formatters

@Composable
fun SalesScreen(
    salesReps: List<SalesRepEntity>,
    onBack: () -> Unit,
    onAddSalesRep: (name: String, phone: String, area: String, target: Int, completed: Int) -> Unit,
    onIncrementInstall: (SalesRepEntity) -> Unit,
    onDeleteSalesRep: (SalesRepEntity) -> Unit
) {
    var showAdd by remember { mutableStateOf(false) }

    ModuleScaffold(
        title = "Sales & Target Pasang",
        subtitle = "Pemantauan tim sales, pencapaian target & komisi",
        onBack = onBack,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAdd = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Tambah Sales")
            }
        }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (salesReps.isEmpty()) {
                item {
                    EmptyModuleBanner(
                        title = "Belum Ada Data Sales",
                        subtitle = "Tambahkan anggota tim sales atau teknisi pasang baru untuk memantau realisasi target."
                    )
                }
            } else {
                items(salesReps, key = { it.id }) { rep ->
                    val pct = ((rep.completedInstalls * 100) / rep.targetInstalls.coerceAtLeast(1)).coerceIn(0, 100)
                    val commission = rep.completedInstalls * rep.commissionPerInstall

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(rep.name, style = MaterialTheme.typography.titleMedium)
                                    Text("${rep.areaAssigned} • ${rep.phone}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { onDeleteSalesRep(rep) }) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Hapus sales", tint = NocRedOffline)
                                }
                            }
                            ResourceGaugeRow(
                                label = "Pencapaian (${rep.completedInstalls}/${rep.targetInstalls} pasang)",
                                percent = pct,
                                detailText = "$pct%"
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Estimasi Komisi: ${Formatters.formatRupiah(commission)}",
                                    fontFamily = JetBrainsMonoFamily,
                                    fontSize = 12.sp,
                                    color = NocEmeraldOnline
                                )
                                OutlinedButton(onClick = { onIncrementInstall(rep) }) {
                                    Icon(Icons.Rounded.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("+1 Pasang", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var area by remember { mutableStateOf("Kecamatan Utara") }
        var target by remember { mutableStateOf("15") }
        var done by remember { mutableStateOf("0") }

        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Tambah Tim Sales") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Sales / Teknisi") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("No. HP / WhatsApp") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = area, onValueChange = { area = it }, label = { Text("Area Tugas") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = target, onValueChange = { target = it.filter { c -> c.isDigit() } }, label = { Text("Target") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = done, onValueChange = { done = it.filter { c -> c.isDigit() } }, label = { Text("Terealisasi") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (name.isNotBlank()) {
                        onAddSalesRep(name, phone, area, target.toIntOrNull() ?: 10, done.toIntOrNull() ?: 0)
                        showAdd = false
                    }
                }) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Batal") } }
        )
    }
}

@Composable
fun LogAktivitasScreen(
    logs: List<ActivityLogEntity>,
    showBackButton: Boolean,
    onBack: () -> Unit,
    onClearLogs: () -> Unit
) {
    val content: @Composable () -> Unit = {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!showBackButton) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("History & Log Aktivitas", style = MaterialTheme.typography.headlineMedium)
                            Text("Jejak perubahan sistem dan aksi operator", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (logs.isNotEmpty()) {
                            IconButton(onClick = onClearLogs) {
                                Icon(Icons.Rounded.DeleteSweep, contentDescription = "Bersihkan log")
                            }
                        }
                    }
                }
            }

            if (logs.isEmpty()) {
                item {
                    EmptyModuleBanner(
                        title = "Belum Ada Aktivitas Tercatat",
                        subtitle = "Setiap aksi penting seperti tambah invoice, verifikasi bayar, sinkronisasi MikroTik, atau update tiket akan terekam di sini."
                    )
                }
            } else {
                items(logs, key = { it.id }) { log ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StatusBadge(label = log.module, color = NocBlueDownload, showDot = false)
                                Text(
                                    text = Formatters.formatDateTime(log.timestamp),
                                    fontFamily = JetBrainsMonoFamily,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(log.action, style = MaterialTheme.typography.titleSmall)
                            Text(log.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    if (showBackButton) {
        ModuleScaffold(
            title = "Log Aktivitas",
            subtitle = "Jejak perubahan & audit operasional",
            onBack = onBack,
            actions = {
                if (logs.isNotEmpty()) {
                    IconButton(onClick = onClearLogs) {
                        Icon(Icons.Rounded.DeleteSweep, contentDescription = "Bersihkan log")
                    }
                }
            }
        ) { content() }
    } else {
        content()
    }
}

@Composable
fun TiketScreen(
    tickets: List<SupportTicketEntity>,
    onBack: () -> Unit,
    onAddTicket: (customer: String, subject: String, desc: String, priority: TicketPriority, tech: String) -> Unit,
    onAdvanceStatus: (SupportTicketEntity) -> Unit,
    onDeleteTicket: (SupportTicketEntity) -> Unit
) {
    var showAdd by remember { mutableStateOf(false) }

    ModuleScaffold(
        title = "Tiket Bantuan Pelanggan",
        subtitle = "Penanganan gangguan LOS, lambat & teknisi",
        onBack = onBack,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAdd = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Buat Tiket")
            }
        }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (tickets.isEmpty()) {
                item {
                    EmptyModuleBanner(
                        title = "Belum Ada Tiket Gangguan",
                        subtitle = "Tekan + untuk mencatat laporan gangguan pelanggan dan menugaskan teknisi."
                    )
                }
            } else {
                items(tickets, key = { it.id }) { ticket ->
                    val statusColor = when (ticket.status) {
                        TicketStatus.TERBUKA -> NocRedOffline
                        TicketStatus.PROSES -> NocAmber
                        TicketStatus.SELESAI -> NocEmeraldOnline
                    }
                    val prioColor = when (ticket.priority) {
                        TicketPriority.DARURAT -> NocRedOffline
                        TicketPriority.TINGGI -> NocAmber
                        TicketPriority.SEDANG -> NocBlueDownload
                        TicketPriority.RENDAH -> NocPurpleAccent
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(ticket.ticketCode, fontFamily = JetBrainsMonoFamily, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                    Text(ticket.subject, style = MaterialTheme.typography.titleMedium)
                                    Text("${ticket.customerName} • Teknisi: ${ticket.technician}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                StatusBadge(label = ticket.status.label, color = statusColor)
                            }
                            if (ticket.description.isNotBlank()) {
                                Text(ticket.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StatusBadge(label = "Prioritas: ${ticket.priority.label}", color = prioColor, showDot = false)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedButton(onClick = { onAdvanceStatus(ticket) }) {
                                        Text("Ubah Status", fontSize = 12.sp)
                                    }
                                    IconButton(onClick = { onDeleteTicket(ticket) }) {
                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Hapus tiket", tint = NocRedOffline)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        var customer by remember { mutableStateOf("") }
        var subject by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        var tech by remember { mutableStateOf("Teknisi Lapangan") }
        var priority by remember { mutableStateOf(TicketPriority.TINGGI) }

        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Buat Tiket Gangguan") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = customer, onValueChange = { customer = it }, label = { Text("Nama Pelanggan") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Keluhan / Masalah") }, placeholder = { Text("mis. Modem merah LOS") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Detail Lokasi / Catatan") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = tech, onValueChange = { tech = it }, label = { Text("Teknisi Ditugaskan") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TicketPriority.entries.forEach { p ->
                            FilterChip(
                                selected = priority == p,
                                onClick = { priority = p },
                                label = { Text(p.label, fontSize = 10.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (customer.isNotBlank() && subject.isNotBlank()) {
                        onAddTicket(customer, subject, desc, priority, tech)
                        showAdd = false
                    }
                }) { Text("Buat Tiket") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Batal") } }
        )
    }
}

@Composable
fun SettingsScreen(
    settings: TelegramSettings,
    onUpdateSettings: ((TelegramSettings) -> TelegramSettings) -> Unit,
    onOpenTelegramAlerts: () -> Unit
) {
    var ispName by remember(settings.ispName) { mutableStateOf(settings.ispName) }
    var ispSubtitle by remember(settings.ispSubtitle) { mutableStateOf(settings.ispSubtitle) }
    var adminName by remember(settings.adminName) { mutableStateOf(settings.adminName) }
    var genieUrl by remember(settings.genieAcsUrl) { mutableStateOf(settings.genieAcsUrl) }
    var ppnText by remember(settings.ppnPercent) { mutableStateOf(settings.ppnPercent.toString()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text("Pengaturan Aplikasi", style = MaterialTheme.typography.headlineMedium)
                Text("Profil jaringan ISP, preferensi tampilan & integrasi", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Identitas ISP & Operator", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(value = ispName, onValueChange = { ispName = it }, label = { Text("Nama Network / ISP") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = ispSubtitle, onValueChange = { ispSubtitle = it }, label = { Text("Subjudul") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = adminName, onValueChange = { adminName = it }, label = { Text("Nama Sapaan Admin") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = genieUrl, onValueChange = { genieUrl = it }, label = { Text("URL Server GenieACS NBI") }, placeholder = { Text("http://192.168.10.50:7557") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = ppnText, onValueChange = { ppnText = it.filter { c -> c.isDigit() } }, label = { Text("Tarif PPN Default (%)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())

                    Button(
                        onClick = {
                            onUpdateSettings {
                                it.copy(
                                    ispName = ispName.trim().ifBlank { "NUGROHO NETWORK" }.uppercase(),
                                    ispSubtitle = ispSubtitle.trim().ifBlank { "Fiber & MikroTik Monitoring" },
                                    adminName = adminName.trim().ifBlank { "Network Admin" },
                                    genieAcsUrl = genieUrl.trim(),
                                    ppnPercent = (ppnText.toIntOrNull() ?: 11).coerceIn(0, 30)
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Text("Simpan Profil", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Tampilan & Polling", style = MaterialTheme.typography.titleMedium)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Mode Gelap NOC", style = MaterialTheme.typography.bodyLarge)
                            Text("Tema gelap dengan aksen oranye-amber", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = settings.darkTheme,
                            onCheckedChange = { c -> onUpdateSettings { it.copy(darkTheme = c) } }
                        )
                    }

                    Text("Interval Polling RouterOS", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(3, 5, 10, 30).forEach { sec ->
                            FilterChip(
                                selected = settings.pollingIntervalSec == sec,
                                onClick = { onUpdateSettings { it.copy(pollingIntervalSec = sec) } },
                                label = { Text("${sec}d") }
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onOpenTelegramAlerts,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Konfigurasi Bot Telegram & Alert")
                    }
                }
            }
        }
    }
}
