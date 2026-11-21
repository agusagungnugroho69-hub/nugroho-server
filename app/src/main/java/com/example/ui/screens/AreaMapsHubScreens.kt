package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Workspaces
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FtthCustomerPinEntity
import com.example.data.model.FtthNodeEntity
import com.example.data.model.FtthNodeType
import com.example.data.model.PppoeClient
import com.example.ui.components.ModuleScaffold
import com.example.ui.components.StatusBadge
import com.example.ui.navigation.Routes
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.NocAmber
import com.example.ui.theme.NocBlueDownload
import com.example.ui.theme.NocEmeraldOnline
import com.example.ui.theme.NocPurpleAccent
import com.example.ui.theme.NocRedOffline
import java.util.Locale

private data class AreaSubMenuItem(
    val route: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accentColor: Color,
    val testTag: String
)

@Composable
fun AreaMapsHubScreen(
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val subMenus = remember {
        listOf(
            AreaSubMenuItem(
                route = Routes.ROUTE_FTTH_LOKASI,
                title = "Lokasi",
                subtitle = "ODP, ODC, BTS & HTB",
                icon = Icons.Outlined.Layers,
                accentColor = Color(0xFF7C3AED),
                testTag = "area_sub_lokasi"
            ),
            AreaSubMenuItem(
                route = Routes.ROUTE_FTTH_TOPOLOGI,
                title = "Topologi",
                subtitle = "Pohon jaringan & kapasitas",
                icon = Icons.Outlined.AccountTree,
                accentColor = Color(0xFF0EA5E9),
                testTag = "area_sub_topologi"
            ),
            AreaSubMenuItem(
                route = Routes.ROUTE_FTTH_ZONA,
                title = "Zona",
                subtitle = "ODP dan paket per area",
                icon = Icons.Outlined.Workspaces,
                accentColor = Color(0xFF8B5CF6),
                testTag = "area_sub_zona"
            ),
            AreaSubMenuItem(
                route = Routes.ROUTE_MAPS_FTTH,
                title = "Maps & FTTH",
                subtitle = "Peta sebaran & kabel",
                icon = Icons.Outlined.Map,
                accentColor = Color(0xFF10B981),
                testTag = "area_sub_maps_ftth"
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header matching Gambar 1
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                        .clickable(onClick = onBack)
                        .testTag("area_hub_back_button"),
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
                Column {
                    Text(
                        text = "Area & Maps",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Distribusi & peta",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
                    contentDescription = "Ganti tema",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Pilih modul area dan pemetaan yang ingin dibuka.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    subMenus.forEachIndexed { idx, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigate(item.route) }
                                .padding(horizontal = 16.dp, vertical = 16.dp)
                                .testTag(item.testTag),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(item.accentColor.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.title,
                                        tint = item.accentColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = item.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (idx < subMenus.lastIndex) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private enum class LokasiHealthStatus(val label: String, val color: Color) {
    NORMAL("Normal", NocEmeraldOnline),
    HAMPIR_PENUH("Hampir penuh", NocAmber),
    GANGGUAN("Gangguan", NocRedOffline)
}

private data class LokasiNodeStats(
    val node: FtthNodeEntity,
    val parentName: String?,
    val usedPorts: Int,
    val totalCapacity: Int,
    val onlineCount: Int,
    val offlineCount: Int,
    val status: LokasiHealthStatus
)

@Composable
fun FtthLokasiScreen(
    nodes: List<FtthNodeEntity>,
    customerPins: List<FtthCustomerPinEntity>,
    pppoeClients: List<PppoeClient>,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
    onOpenMapsFtth: () -> Unit,
    onDeleteNode: (FtthNodeEntity) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf<LokasiHealthStatus?>(null) }

    // Derive live location telemetry strictly from Maps & FTTH nodes + pins
    val nodeStatsList = remember(nodes, customerPins, pppoeClients) {
        fun collectDescendantPins(nodeId: Long, visited: MutableSet<Long> = mutableSetOf()): List<FtthCustomerPinEntity> {
            if (!visited.add(nodeId)) return emptyList()
            val direct = customerPins.filter { it.parentNodeId == nodeId }
            val childNodes = nodes.filter { it.parentId == nodeId }
            return direct + childNodes.flatMap { collectDescendantPins(it.id, visited) }
        }

        nodes.map { node ->
            val parentName = node.parentId?.let { pid -> nodes.firstOrNull { it.id == pid }?.name }
            val directChildrenCount = nodes.count { it.parentId == node.id }
            val directPins = customerPins.filter { it.parentNodeId == node.id }
            val descendantPins = collectDescendantPins(node.id)

            val totalCapacity = (node.limitPort + node.extraPort).coerceAtLeast(1)
            val computedUsed = maxOf(node.usedPorts, directPins.size + directChildrenCount)
                .coerceAtMost(totalCapacity)
            val usageRatio = computedUsed.toFloat() / totalCapacity.toFloat()

            var online = 0
            var offline = 0
            descendantPins.forEach { pin ->
                val match = pppoeClients.firstOrNull { client ->
                    (pin.pppoeUsername.isNotBlank() && client.name.equals(pin.pppoeUsername, ignoreCase = true)) ||
                        client.name.equals(pin.name, ignoreCase = true)
                }
                if (match != null && match.isOnline && !match.isDisabled) online++ else offline++
            }

            val health = when {
                computedUsed >= totalCapacity -> LokasiHealthStatus.GANGGUAN
                usageRatio >= 0.5f -> LokasiHealthStatus.HAMPIR_PENUH
                else -> LokasiHealthStatus.NORMAL
            }

            LokasiNodeStats(
                node = node,
                parentName = parentName,
                usedPorts = computedUsed,
                totalCapacity = totalCapacity,
                onlineCount = online,
                offlineCount = offline,
                status = health
            )
        }
    }

    val totalCount = nodeStatsList.size
    val normalCount = nodeStatsList.count { it.status == LokasiHealthStatus.NORMAL }
    val almostFullCount = nodeStatsList.count { it.status == LokasiHealthStatus.HAMPIR_PENUH }
    val issueCount = nodeStatsList.count { it.status == LokasiHealthStatus.GANGGUAN }

    val filtered = remember(nodeStatsList, searchQuery, selectedStatusFilter) {
        val q = searchQuery.trim().lowercase()
        nodeStatsList.filter { item ->
            val matchesStatus = selectedStatusFilter == null || item.status == selectedStatusFilter
            val matchesQuery = q.isEmpty() ||
                item.node.name.lowercase().contains(q) ||
                item.node.code.lowercase().contains(q) ||
                item.node.address.lowercase().contains(q) ||
                (item.parentName?.lowercase()?.contains(q) == true)
            matchesStatus && matchesQuery
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header matching screenshot ("Lokasi" / "Area & Maps" + Refresh + Theme)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                        .clickable(onClick = onBack)
                        .testTag("lokasi_back_button"),
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
                Column {
                    Text(
                        text = "Lokasi",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Area & Maps",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                        .clickable(onClick = onRefresh)
                        .testTag("lokasi_refresh_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Segarkan Lokasi",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                        .clickable(onClick = onToggleTheme)
                        .testTag("lokasi_theme_button"),
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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 4 Summary KPI Cards matching screenshot
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LokasiKpiCard(
                        count = totalCount,
                        label = "Total",
                        valueColor = MaterialTheme.colorScheme.onSurface,
                        selected = selectedStatusFilter == null,
                        onClick = { selectedStatusFilter = null },
                        modifier = Modifier.weight(1f)
                    )
                    LokasiKpiCard(
                        count = normalCount,
                        label = "Normal",
                        valueColor = NocEmeraldOnline,
                        selected = selectedStatusFilter == LokasiHealthStatus.NORMAL,
                        onClick = {
                            selectedStatusFilter = if (selectedStatusFilter == LokasiHealthStatus.NORMAL) null else LokasiHealthStatus.NORMAL
                        },
                        modifier = Modifier.weight(1f)
                    )
                    LokasiKpiCard(
                        count = almostFullCount,
                        label = "Hampir penuh",
                        valueColor = NocAmber,
                        selected = selectedStatusFilter == LokasiHealthStatus.HAMPIR_PENUH,
                        onClick = {
                            selectedStatusFilter = if (selectedStatusFilter == LokasiHealthStatus.HAMPIR_PENUH) null else LokasiHealthStatus.HAMPIR_PENUH
                        },
                        modifier = Modifier.weight(1f)
                    )
                    LokasiKpiCard(
                        count = issueCount,
                        label = "Gangguan",
                        valueColor = NocRedOffline,
                        selected = selectedStatusFilter == LokasiHealthStatus.GANGGUAN,
                        onClick = {
                            selectedStatusFilter = if (selectedStatusFilter == LokasiHealthStatus.GANGGUAN) null else LokasiHealthStatus.GANGGUAN
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Search bar matching screenshot
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(NocAmber.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                                tint = NocAmber,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Cari nama, kode, atau alamat...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                                cursorBrush = SolidColor(NocAmber),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("lokasi_search_input")
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Bersihkan",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "${filtered.size} lokasi",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (nodes.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Belum Ada Data Lokasi",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Data lokasi diambil langsung dari Maps & FTTH. Tambahkan titik distribusi melalui halaman Maps & FTTH terlebih dahulu.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = onOpenMapsFtth,
                                colors = ButtonDefaults.buttonColors(containerColor = NocAmber, contentColor = Color(0xFF0B0E14)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("lokasi_open_maps_button")
                            ) {
                                Text("Buka Maps & FTTH", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else if (filtered.isEmpty()) {
                item {
                    EmptyModuleBanner(
                        title = "Tidak Ditemukan",
                        subtitle = "Tidak ada lokasi di Maps & FTTH yang cocok dengan filter atau kata kunci pencarian."
                    )
                }
            } else {
                items(filtered, key = { it.node.id }) { stats ->
                    LokasiDistribusiCard(
                        stats = stats,
                        onOpenOnMap = onOpenMapsFtth,
                        onDelete = { onDeleteNode(stats.node) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LokasiKpiCard(
    count: Int,
    label: String,
    valueColor: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(
                1.dp,
                if (selected) valueColor.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline,
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = count.toString(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LokasiDistribusiCard(
    stats: LokasiNodeStats,
    onOpenOnMap: () -> Unit,
    onDelete: () -> Unit
) {
    val node = stats.node
    val progress = (stats.usedPorts.toFloat() / stats.totalCapacity.toFloat()).coerceIn(0f, 1f)
    val typeAccent = Color(0xFF7C3AED)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .clickable(onClick = onOpenOnMap),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(typeAccent.copy(alpha = 0.16f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = node.type.label.uppercase(Locale.ROOT),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = typeAccent
                        )
                    }
                    Text(
                        text = node.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(stats.status.color.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stats.status.label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = stats.status.color
                    )
                }
            }

            Text(
                text = if (stats.parentName != null) "${node.code} • ${stats.parentName}" else node.code,
                fontFamily = JetBrainsMonoFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = node.address.ifBlank {
                        String.format(Locale.US, "%.6f, %.6f", node.latitude, node.longitude)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Port terpakai",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${stats.usedPorts} / ${stats.totalCapacity}",
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = stats.status.color,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(NocEmeraldOnline)
                        )
                        Text(
                            text = "${stats.onlineCount} online",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NocEmeraldOnline
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        )
                        Text(
                            text = "${stats.offlineCount} offline",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Hapus lokasi",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
