package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CustomerEntity
import com.example.data.model.RouterConfig
import com.example.data.model.RouterTelemetry
import com.example.ui.theme.NocAmber
import com.example.ui.theme.NocEmeraldOnline
import com.example.ui.theme.NocRedOffline

@Composable
fun RoutersScreen(
    routers: List<RouterConfig>,
    telemetryByRouter: Map<Long, RouterTelemetry>,
    customers: List<CustomerEntity>,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onOpenMonitoring: () -> Unit,
    onAddRouter: () -> Unit,
    onEditRouter: (RouterConfig) -> Unit,
    onDeleteRouter: (RouterConfig) -> Unit,
    onOpenDetail: (Long) -> Unit,
    onLoadDemoLab: () -> Unit,
    onToggleSimOffline: (RouterConfig) -> Unit,
    onToggleSimCpuSpike: (RouterConfig) -> Unit,
    isSimOffline: (Long) -> Boolean,
    isSimCpuSpike: (Long) -> Boolean,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var routerToDelete by remember { mutableStateOf<RouterConfig?>(null) }

    val serverCount = routers.size
    val onlineCount = routers.count { telemetryByRouter[it.id]?.isOnline == true }
    val offlineCount = (serverCount - onlineCount).coerceAtLeast(0)
    // Pelanggan count comes strictly from Menu Pelanggan (0 if Menu Pelanggan hasn't been filled)
    val totalCustomerCount = customers.size
    val maxQuota = maxOf(2, serverCount)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddRouter,
                shape = CircleShape,
                containerColor = NocAmber,
                contentColor = Color(0xFF0B0E14),
                modifier = Modifier.testTag("fab_add_router")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Tambah Server MikroTik", modifier = Modifier.size(26.dp))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Top bar matching Gambar 1 (< Mikrotik | Monitoring | Theme)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onBack != null) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                                .clickable(onClick = onBack)
                                .testTag("routers_back_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Kembali",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Text(
                        text = "Mikrotik",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onOpenMonitoring)
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("mikrotik_monitoring_button"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MonitorHeart,
                            contentDescription = null,
                            tint = NocAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Monitoring",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = NocAmber
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                            .clickable(onClick = onToggleTheme),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (darkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                            contentDescription = "Ganti Tema",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 92.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 2x2 Summary KPI Grid matching Gambar 1
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MikrotikKpiCard(
                                count = serverCount,
                                label = "SERVER",
                                countColor = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            MikrotikKpiCard(
                                count = onlineCount,
                                label = "TERHUBUNG",
                                countColor = NocEmeraldOnline,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MikrotikKpiCard(
                                count = offlineCount,
                                label = "TERPUTUS",
                                countColor = NocRedOffline,
                                modifier = Modifier.weight(1f)
                            )
                            MikrotikKpiCard(
                                count = totalCustomerCount,
                                label = "PELANGGAN",
                                countColor = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = "Kuota router $serverCount/$maxQuota",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                if (routers.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Column(
                                modifier = Modifier.padding(22.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Router,
                                    contentDescription = null,
                                    tint = NocAmber,
                                    modifier = Modifier.size(38.dp)
                                )
                                Text(
                                    text = "Belum Ada Server MikroTik",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Tekan tombol + di kanan bawah untuk menambahkan server MikroTik.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedButton(onClick = onLoadDemoLab) {
                                    Icon(Icons.Rounded.Science, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Muat Router Simulasi Lab")
                                }
                            }
                        }
                    }
                } else {
                    items(routers, key = { it.id }) { router ->
                        val telemetry = telemetryByRouter[router.id]
                        val routerCustomerCount = customers.count { c ->
                            c.routerId == router.id ||
                                (c.routerId == 0L && c.routerName.equals(router.name, ignoreCase = true))
                        }
                        MikrotikServerListCard(
                            router = router,
                            telemetry = telemetry,
                            customerCount = routerCustomerCount,
                            onOpenDetail = { onOpenDetail(router.id) },
                            onEdit = { onEditRouter(router) },
                            onDelete = { routerToDelete = router },
                            onToggleSimOffline = { onToggleSimOffline(router) },
                            onToggleSimCpuSpike = { onToggleSimCpuSpike(router) },
                            simOfflineActive = isSimOffline(router.id),
                            simCpuSpikeActive = isSimCpuSpike(router.id)
                        )
                    }
                }
            }
        }
    }

    routerToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { routerToDelete = null },
            title = { Text("Hapus Server?") },
            text = { Text("Konfigurasi ${target.name} (${target.host}) akan dihapus dari aplikasi.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteRouter(target)
                        routerToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NocRedOffline)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { routerToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun MikrotikKpiCard(
    count: Int,
    label: String,
    countColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = count.toString(),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = countColor
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MikrotikServerListCard(
    router: RouterConfig,
    telemetry: RouterTelemetry?,
    customerCount: Int,
    onOpenDetail: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleSimOffline: () -> Unit,
    onToggleSimCpuSpike: () -> Unit,
    simOfflineActive: Boolean,
    simCpuSpikeActive: Boolean
) {
    val isOnline = telemetry?.isOnline == true
    val dotColor = if (isOnline) NocEmeraldOnline else NocRedOffline
    val rosLabel = telemetry?.rosVersion?.takeIf { it.isNotBlank() && it != "-" }?.let { ver ->
        val major = ver.substringBefore(".").toIntOrNull() ?: router.routerOsMajorVersion
        "ROS v$major"
    } ?: "ROS v${router.routerOsMajorVersion}"

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .clickable(onClick = onOpenDetail),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(NocAmber.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Storage,
                        contentDescription = null,
                        tint = NocAmber,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                        Text(
                            text = router.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "$rosLabel · $customerCount pelanggan",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Edit server",
                        tint = NocAmber,
                        modifier = Modifier.size(19.dp)
                    )
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Hapus server",
                        tint = NocRedOffline,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (router.isSimulated) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = simOfflineActive,
                        onClick = onToggleSimOffline,
                        label = {
                            Text(
                                text = if (simOfflineActive) "Pulihkan Online" else "Simulasi DOWN",
                                fontSize = 11.sp
                            )
                        }
                    )
                    FilterChip(
                        selected = simCpuSpikeActive,
                        onClick = onToggleSimCpuSpike,
                        label = {
                            Text(
                                text = if (simCpuSpikeActive) "Normalkan CPU" else "Simulasi CPU 95%",
                                fontSize = 11.sp
                            )
                        }
                    )
                }
            }
        }
    }
}
