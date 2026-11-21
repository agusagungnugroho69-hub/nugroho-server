package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PeopleOutline
import androidx.compose.material.icons.rounded.PersonOff
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RouterConfig
import com.example.data.model.RouterTelemetry
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.RealtimeTrafficChartCard
import com.example.ui.components.ResourceGaugeRow
import com.example.ui.components.StatusBadge
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.NocAmber
import com.example.ui.theme.NocBlueDownload
import com.example.ui.theme.NocEmeraldOnline
import com.example.ui.theme.NocPurpleAccent
import com.example.ui.theme.NocRedOffline
import com.example.util.Formatters
import com.example.viewmodel.DashboardUiState

@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    pollingIntervalSec: Int,
    onAddRouterClick: () -> Unit,
    onLoadDemoLab: () -> Unit,
    onRefreshClick: () -> Unit,
    onToggleLivePolling: () -> Unit,
    onSelectGraphRouter: (Long?) -> Unit,
    onOpenRouterDetail: (Long) -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBack != null) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("dashboard_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Dashboard NOC",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatusBadge(
                            label = if (uiState.isPollingLive) "LIVE • ${pollingIntervalSec}s" else "JEDA",
                            color = if (uiState.isPollingLive) NocEmeraldOnline else NocAmber
                        )
                        if (uiState.hasAnySimulatedRouter) {
                            StatusBadge(
                                label = "SIMULASI LAB",
                                color = NocPurpleAccent,
                                showDot = false
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleLivePolling,
                        modifier = Modifier.testTag("toggle_polling_button")
                    ) {
                        Icon(
                            imageVector = if (uiState.isPollingLive) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = "Jeda atau lanjutkan polling",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onRefreshClick,
                        enabled = !uiState.isRefreshing,
                        modifier = Modifier.testTag("refresh_dashboard_button")
                    ) {
                        if (uiState.isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Refresh semua router",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    IconButton(
                        onClick = onAddRouterClick,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("header_add_router_button")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Tambah Router",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Empty State when no routers configured
        if (uiState.routers.isEmpty()) {
            item {
                DashboardEmptyState(
                    onAddRouterClick = onAddRouterClick,
                    onLoadDemoLab = onLoadDemoLab
                )
            }
        } else {
            // 6 KPI Cards Adaptive Grid
            item {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val columns = if (maxWidth >= 680.dp) 3 else 2
                    val kpiItems = listOf(
                        KpiData("Total Router", uiState.totalRouters.toString(), "${uiState.onlineRouters} aktif dipantau", Icons.Rounded.Router, NocAmber),
                        KpiData("Router Online", uiState.onlineRouters.toString(), "Koneksi API normal", Icons.Rounded.CloudDone, NocEmeraldOnline),
                        KpiData("Router Offline", uiState.offlineRouters.toString(), if (uiState.offlineRouters > 0) "Perlu pengecekan!" else "Semua terhubung", Icons.Rounded.CloudOff, NocRedOffline),
                        KpiData("Total Pelanggan", uiState.totalCustomers.toString(), "Terdaftar PPPoE secret", Icons.Rounded.Group, NocBlueDownload),
                        KpiData("Pelanggan Online", uiState.onlineCustomers.toString(), "Sesi PPPoE aktif", Icons.Rounded.PeopleOutline, NocEmeraldOnline),
                        KpiData("Pelanggan Offline", uiState.offlineCustomers.toString(), "Tidak terkoneksi", Icons.Rounded.PersonOff, NocRedOffline)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        kpiItems.chunked(columns).forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowItems.forEach { item ->
                                    KpiMetricCard(
                                        title = item.title,
                                        value = item.value,
                                        subtitle = item.subtitle,
                                        icon = item.icon,
                                        accentColor = item.color,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                repeat(columns - rowItems.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // Aggregate Bandwidth Banner
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        NocBlueDownload.copy(alpha = 0.08f),
                                        NocEmeraldOnline.copy(alpha = 0.08f)
                                    )
                                )
                            )
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.ArrowDownward,
                                contentDescription = "Total Download",
                                tint = NocBlueDownload,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "Total Traffic Download",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = Formatters.formatBps(uiState.totalRxBps),
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = NocBlueDownload
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Total Traffic Upload",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = Formatters.formatBps(uiState.totalTxBps),
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = NocEmeraldOnline
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Rounded.ArrowUpward,
                                contentDescription = "Total Upload",
                                tint = NocEmeraldOnline,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Router Filter Chips for Traffic Graph
            item {
                val selectedId = uiState.selectedGraphRouterId
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedId == null,
                            onClick = { onSelectGraphRouter(null) },
                            label = { Text("Semua Router") }
                        )
                        uiState.routers.forEach { r ->
                            FilterChip(
                                selected = selectedId == r.id,
                                onClick = { onSelectGraphRouter(r.id) },
                                label = { Text(r.name) }
                            )
                        }
                    }

                    val selectedRouter = uiState.routers.firstOrNull { it.id == selectedId }
                    val selectedTelemetry = selectedId?.let { uiState.telemetryByRouter[it] }
                    val samples = if (selectedId == null) {
                        uiState.aggregateTrafficHistory
                    } else {
                        uiState.trafficHistoryByRouter[selectedId].orEmpty()
                    }
                    val rxBps = if (selectedId == null) uiState.totalRxBps else (selectedTelemetry?.rxBitsPerSec ?: 0L)
                    val txBps = if (selectedId == null) uiState.totalTxBps else (selectedTelemetry?.txBitsPerSec ?: 0L)
                    val ifaceLabel = selectedRouter?.monitoredInterface
                        ?: "Agregat (${uiState.onlineRouters} router)"

                    RealtimeTrafficChartCard(
                        samples = samples,
                        currentRxBps = rxBps,
                        currentTxBps = txBps,
                        interfaceLabel = ifaceLabel
                    )
                }
            }

            // Section Title: Router Health List
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Status Perangkat MikroTik",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${uiState.routers.size} perangkat",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(uiState.routers, key = { it.id }) { router ->
                val telemetry = uiState.telemetryByRouter[router.id]
                DashboardRouterHealthCard(
                    router = router,
                    telemetry = telemetry,
                    onClick = { onOpenRouterDetail(router.id) }
                )
            }
        }
    }
}

private data class KpiData(
    val title: String,
    val value: String,
    val subtitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: androidx.compose.ui.graphics.Color
)

@Composable
private fun DashboardRouterHealthCard(
    router: RouterConfig,
    telemetry: RouterTelemetry?,
    onClick: () -> Unit
) {
    val isOnline = telemetry?.isOnline == true
    val statusColor = if (isOnline) NocEmeraldOnline else NocRedOffline

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("dashboard_router_card_${router.id}"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(statusColor.copy(alpha = 0.14f))
                            .border(1.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Router,
                            contentDescription = router.name,
                            tint = statusColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = router.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (router.isSimulated) {
                                StatusBadge(
                                    label = "LAB",
                                    color = NocPurpleAccent,
                                    showDot = false
                                )
                            }
                        }
                        Text(
                            text = "${router.host}:${router.port} • ${router.protocol.shortBadge}",
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusBadge(
                        label = if (isOnline) "ONLINE" else "OFFLINE",
                        color = statusColor
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = "Buka detail router",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isOnline && telemetry != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ResourceGaugeRow(
                        label = "CPU Usage",
                        percent = telemetry.cpuLoadPercent,
                        detailText = "${telemetry.cpuLoadPercent}%",
                        modifier = Modifier.weight(1f)
                    )
                    ResourceGaugeRow(
                        label = "RAM Usage",
                        percent = telemetry.memoryUsagePercent,
                        detailText = "${telemetry.memoryUsagePercent}%",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Uptime: ${telemetry.uptime}",
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "PPPoE: ${telemetry.activePppoeCount}/${telemetry.totalSecretCount}",
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 11.sp,
                        color = NocBlueDownload
                    )
                    Text(
                        text = "${telemetry.latencyMs} ms",
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 11.sp,
                        color = NocEmeraldOnline
                    )
                }
            } else {
                Text(
                    text = telemetry?.errorMessage ?: "Menghubungkan ke RouterOS API...",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (telemetry == null) MaterialTheme.colorScheme.onSurfaceVariant else NocRedOffline
                )
            }
        }
    }
}

@Composable
private fun DashboardEmptyState(
    onAddRouterClick: () -> Unit,
    onLoadDemoLab: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Speed,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Text(
                text = "Belum Ada Router MikroTik",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Tambahkan router MikroTik Anda melalui RouterOS Binary API (8728/8729) atau REST API v7 untuk memantau trafik real-time, CPU/RAM, PPPoE aktif, serta alert Telegram.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = onAddRouterClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("empty_add_router_button")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tambah Router MikroTik", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onLoadDemoLab,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("empty_load_demo_button")
            ) {
                Icon(Icons.Rounded.Science, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Muat Router Simulasi Lab")
            }
        }
    }
}
