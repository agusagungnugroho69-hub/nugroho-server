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
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import com.example.data.model.CpeDeviceEntity
import com.example.data.model.InternetPackageEntity
import com.example.data.model.NetworkAreaEntity
import com.example.data.model.PppoeClient
import com.example.data.model.RadiusEntry
import com.example.data.model.RouterConfig
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
fun AreaMapsScreen(
    areas: List<NetworkAreaEntity>,
    routers: List<RouterConfig>,
    onBack: () -> Unit,
    onAddArea: (name: String, code: String, odp: Int, totalPorts: Int, usedPorts: Int, coords: String, routerName: String) -> Unit,
    onDeleteArea: (NetworkAreaEntity) -> Unit
) {
    var showAdd by remember { mutableStateOf(false) }

    ModuleScaffold(
        title = "Area & Peta Distribusi",
        subtitle = "Pemantauan wilayah ODP, kapasitas port & POP",
        onBack = onBack,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAdd = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Tambah Area")
            }
        }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (areas.isEmpty()) {
                item {
                    EmptyModuleBanner(
                        title = "Belum Ada Area Distribusi",
                        subtitle = "Tambahkan area ODP/POP fiber untuk memantau utilisasi port dan cakupan wilayah pelanggan."
                    )
                }
            } else {
                items(areas, key = { it.id }) { area ->
                    val pct = if (area.totalPorts > 0) ((area.usedPorts * 100) / area.totalPorts) else 0
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
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(area.name, style = MaterialTheme.typography.titleMedium)
                                        StatusBadge(label = area.code, color = NocPurpleAccent, showDot = false)
                                    }
                                    Text(
                                        text = "${area.odpCount} ODP • Router POP: ${area.routerName.ifBlank { "-" }}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (area.coordinates.isNotBlank()) {
                                        Text(
                                            text = "Koordinat: ${area.coordinates}",
                                            fontFamily = JetBrainsMonoFamily,
                                            fontSize = 11.sp,
                                            color = NocBlueDownload
                                        )
                                    }
                                }
                                IconButton(onClick = { onDeleteArea(area) }) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Hapus area", tint = NocRedOffline)
                                }
                            }
                            ResourceGaugeRow(
                                label = "Utilisasi Port ODP (${area.usedPorts}/${area.totalPorts} port terpakai)",
                                percent = pct,
                                detailText = "$pct%"
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        var name by remember { mutableStateOf("") }
        var code by remember { mutableStateOf("ODP-01") }
        var odp by remember { mutableStateOf("8") }
        var total by remember { mutableStateOf("64") }
        var used by remember { mutableStateOf("42") }
        var coords by remember { mutableStateOf("-7.7956, 110.3695") }
        var router by remember { mutableStateOf(routers.firstOrNull()?.name ?: "") }

        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Tambah Area / ODP") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Area / Kelurahan") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Kode Wilayah") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = odp, onValueChange = { odp = it.filter { c -> c.isDigit() } }, label = { Text("Jml ODP") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = used, onValueChange = { used = it.filter { c -> c.isDigit() } }, label = { Text("Terpakai") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = total, onValueChange = { total = it.filter { c -> c.isDigit() } }, label = { Text("Total Port") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(value = coords, onValueChange = { coords = it }, label = { Text("Koordinat GPS (Lat, Lng)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = router, onValueChange = { router = it }, label = { Text("Router POP Terhubung") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (name.isNotBlank()) {
                        onAddArea(
                            name,
                            code.ifBlank { "AREA" },
                            odp.toIntOrNull() ?: 1,
                            total.toIntOrNull() ?: 16,
                            used.toIntOrNull() ?: 0,
                            coords,
                            router
                        )
                        showAdd = false
                    }
                }) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Batal") } }
        )
    }
}

@Composable
fun GenieAcsScreen(
    devices: List<CpeDeviceEntity>,
    genieAcsUrl: String,
    isSyncing: Boolean,
    onBack: () -> Unit,
    onUpdateUrl: (String) -> Unit,
    onSyncGenieAcs: (String) -> Unit,
    onAddManualCpe: (sn: String, model: String, cust: String, ip: String, rx: String, ssid: String) -> Unit,
    onRebootCpe: (CpeDeviceEntity) -> Unit
) {
    var urlInput by remember(genieAcsUrl) { mutableStateOf(genieAcsUrl) }
    var showAdd by remember { mutableStateOf(false) }

    ModuleScaffold(
        title = "GenieACS (TR-069)",
        subtitle = "Monitoring ONT/ONU pelanggan & sinyal optik",
        onBack = onBack,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAdd = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Tambah ONT Manual")
            }
        }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Server GenieACS NBI API", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = { urlInput = it },
                            label = { Text("URL GenieACS NBI (Port 7557)") },
                            placeholder = { Text("http://192.168.10.50:7557") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { onUpdateUrl(urlInput.trim()) },
                                modifier = Modifier.weight(1f)
                            ) { Text("Simpan URL") }
                            Button(
                                onClick = {
                                    onUpdateUrl(urlInput.trim())
                                    onSyncGenieAcs(urlInput.trim())
                                },
                                enabled = !isSyncing,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Rounded.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Tarik Device")
                                }
                            }
                        }
                    }
                }
            }

            if (devices.isEmpty()) {
                item {
                    EmptyModuleBanner(
                        title = "Belum Ada Perangkat TR-069",
                        subtitle = "Masukkan URL GenieACS NBI untuk sinkronisasi otomatis ONT/ONU atau tambahkan perangkat manual (+)."
                    )
                }
            } else {
                items(devices, key = { it.id }) { dev ->
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
                                    Text(dev.customerName, style = MaterialTheme.typography.titleMedium)
                                    Text("${dev.model} • SN ${dev.serialNumber}", fontFamily = JetBrainsMonoFamily, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                StatusBadge(
                                    label = if (dev.isOnline) "ONLINE" else "OFFLINE",
                                    color = if (dev.isOnline) NocEmeraldOnline else NocRedOffline
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("IP: ${dev.ipAddress}", fontFamily = JetBrainsMonoFamily, fontSize = 11.sp)
                                Text("RX: ${dev.rxPowerDbm}", fontFamily = JetBrainsMonoFamily, fontSize = 11.sp, color = NocAmber)
                                Text("SSID: ${dev.ssid}", fontFamily = JetBrainsMonoFamily, fontSize = 11.sp)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Inform: ${dev.lastInform}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                OutlinedButton(onClick = { onRebootCpe(dev) }) {
                                    Icon(Icons.Rounded.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(16.dp), tint = NocRedOffline)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reboot ONT", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        var sn by remember { mutableStateOf("") }
        var model by remember { mutableStateOf("Huawei HG8245H5") }
        var cust by remember { mutableStateOf("") }
        var ip by remember { mutableStateOf("10.20.1.14") }
        var rx by remember { mutableStateOf("-18.7 dBm") }
        var ssid by remember { mutableStateOf("FiberHome-WiFi") }

        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Catat Perangkat CPE / ONT") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = cust, onValueChange = { cust = it }, label = { Text("Pelanggan / PPPoE") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = sn, onValueChange = { sn = it }, label = { Text("Serial Number ONT") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("Model Perangkat") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = ip, onValueChange = { ip = it }, label = { Text("IP WAN") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = rx, onValueChange = { rx = it }, label = { Text("RX Optik") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(value = ssid, onValueChange = { ssid = it }, label = { Text("Nama WiFi (SSID)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (sn.isNotBlank() && cust.isNotBlank()) {
                        onAddManualCpe(sn, model, cust, ip, rx, ssid)
                        showAdd = false
                    }
                }) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Batal") } }
        )
    }
}

@Composable
fun RadiusScreen(
    radiusEntries: List<RadiusEntry>,
    pppoeClients: List<PppoeClient>,
    isSyncing: Boolean,
    onBack: () -> Unit,
    onRefreshRadius: () -> Unit
) {
    ModuleScaffold(
        title = "RADIUS & Autentikasi",
        subtitle = "Koneksi server RADIUS MikroTik & sesi aktif",
        onBack = onBack,
        actions = {
            IconButton(onClick = onRefreshRadius, enabled = !isSyncing) {
                if (isSyncing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Cek RADIUS")
                }
            }
        }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedButton(
                    onClick = onRefreshRadius,
                    enabled = !isSyncing,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Muat Konfigurasi /radius dari MikroTik")
                }
            }

            if (radiusEntries.isEmpty()) {
                item {
                    EmptyModuleBanner(
                        title = "Belum Ada Entri RADIUS",
                        subtitle = "Tekan tombol muat di atas saat router MikroTik terhubung untuk memeriksa konfigurasi /radius."
                    )
                }
            } else {
                items(radiusEntries, key = { "${it.routerName}_${it.id}_${it.address}" }) { entry ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(entry.routerName, style = MaterialTheme.typography.titleMedium)
                                StatusBadge(
                                    label = entry.status,
                                    color = if (entry.status == "Aktif") NocEmeraldOnline else NocAmber
                                )
                            }
                            Text(
                                text = "Server RADIUS: ${entry.address} • Service: ${entry.service}",
                                fontFamily = JetBrainsMonoFamily,
                                fontSize = 12.sp,
                                color = NocBlueDownload
                            )
                            Text(
                                text = "Timeout: ${entry.timeout}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Sesi Kuota / PPPoE Aktif (${pppoeClients.count { it.isOnline }})",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            val onlineClients = pppoeClients.filter { it.isOnline }
            if (onlineClients.isEmpty()) {
                item {
                    Text(
                        text = "Tidak ada sesi aktif yang terpantau saat ini.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(onlineClients, key = { "${it.routerId}_${it.name}" }) { client ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(client.name, style = MaterialTheme.typography.titleSmall)
                                Text("${client.address} • ${client.profile}", fontFamily = JetBrainsMonoFamily, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(client.uptime, fontFamily = JetBrainsMonoFamily, fontSize = 11.sp, color = NocEmeraldOnline)
                        }
                    }
                }
            }
        }
    }
}
