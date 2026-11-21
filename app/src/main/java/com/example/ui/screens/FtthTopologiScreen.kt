package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FtthCustomerPinEntity
import com.example.data.model.FtthNodeEntity
import com.example.data.model.FtthNodeType
import com.example.data.model.PppoeClient
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.NocAmber
import com.example.ui.theme.NocBlueDownload
import com.example.ui.theme.NocEmeraldOnline
import com.example.ui.theme.NocRedOffline
import com.example.util.Formatters
import java.util.Locale
import kotlin.math.roundToInt

private data class TopoNodeInfo(
    val node: FtthNodeEntity,
    val usedPorts: Int,
    val totalCapacity: Int,
    val onlineCount: Int,
    val offlineCount: Int,
    val isOnline: Boolean,
    val childrenCount: Int
)

private data class DiagramPlacedNode(
    val info: TopoNodeInfo,
    val x: Float,
    val y: Float,
    val parentId: Long?
)

@Composable
fun FtthTopologiScreen(
    nodes: List<FtthNodeEntity>,
    customerPins: List<FtthCustomerPinEntity>,
    pppoeClients: List<PppoeClient>,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
    onOpenMapsFtth: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedRootId by remember { mutableStateOf<Long?>(null) }
    var statusFilter by remember { mutableStateOf<String>("Semua") } // Semua, Online, Gangguan
    var capacityFilter by remember { mutableStateOf<String>("Semua") } // Semua, Tersedia, Penuh
    var lastSyncMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var selectedNodeDetail by remember { mutableStateOf<TopoNodeInfo?>(null) }

    // Default: Gambar 3 on HP & Tab, with 1-tap switcher to Model Web Diagram (Gambar 1 & 2)
    var useDiagramView by remember { mutableStateOf(false) }

    val infoMap = remember(nodes, customerPins, pppoeClients) {
        fun collectDescendantPins(nodeId: Long, visited: MutableSet<Long> = mutableSetOf()): List<FtthCustomerPinEntity> {
            if (!visited.add(nodeId)) return emptyList()
            val direct = customerPins.filter { it.parentNodeId == nodeId }
            val childNodes = nodes.filter { it.parentId == nodeId }
            return direct + childNodes.flatMap { collectDescendantPins(it.id, visited) }
        }

        nodes.associate { node ->
            val childrenCount = nodes.count { it.parentId == node.id }
            val directPins = customerPins.filter { it.parentNodeId == node.id }
            val allPins = collectDescendantPins(node.id)
            val cap = (node.limitPort + node.extraPort).coerceAtLeast(1)
            val used = maxOf(node.usedPorts, directPins.size + childrenCount).coerceAtMost(cap)

            var online = 0
            var offline = 0
            allPins.forEach { pin ->
                val match = pppoeClients.firstOrNull { client ->
                    (pin.pppoeUsername.isNotBlank() && client.name.equals(pin.pppoeUsername, ignoreCase = true)) ||
                        client.name.equals(pin.name, ignoreCase = true)
                }
                if (match != null && match.isOnline && !match.isDisabled) online++ else offline++
            }

            node.id to TopoNodeInfo(
                node = node,
                usedPorts = used,
                totalCapacity = cap,
                onlineCount = online,
                offlineCount = offline,
                isOnline = online > 0,
                childrenCount = childrenCount
            )
        }
    }

    val odcCount = remember(nodes) { nodes.count { it.type == FtthNodeType.ODC } }
    val odpCount = remember(nodes) { nodes.count { it.type == FtthNodeType.ODP } }
    val activeCount = remember(infoMap) { infoMap.values.count { it.isOnline } }
    val issueCount = remember(infoMap) { infoMap.values.count { !it.isOnline } }

    val allRoots = remember(nodes) {
        nodes.filter { it.parentId == null || nodes.none { p -> p.id == it.parentId } }
    }

    val visibleRoots = remember(allRoots, selectedRootId) {
        if (selectedRootId == null) allRoots
        else nodes.filter { it.id == selectedRootId }
    }

    val filteredNodeIds = remember(nodes, infoMap, searchQuery, statusFilter, capacityFilter) {
        val q = searchQuery.trim().lowercase()
        nodes.filter { n ->
            val info = infoMap[n.id] ?: return@filter false
            val matchesQuery = q.isEmpty() ||
                n.name.lowercase().contains(q) ||
                n.code.lowercase().contains(q)
            val matchesStatus = when (statusFilter) {
                "Online" -> info.isOnline
                "Gangguan" -> !info.isOnline
                else -> true
            }
            val matchesCapacity = when (capacityFilter) {
                "Tersedia" -> info.usedPorts < info.totalCapacity
                "Penuh" -> info.usedPorts >= info.totalCapacity
                else -> true
            }
            matchesQuery && matchesStatus && matchesCapacity
        }.map { it.id }.toSet()
    }

    val syncTimeLabel = remember(lastSyncMillis) { Formatters.formatDateTime(lastSyncMillis) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header matching Gambar 3 (HP & Tab) + toggle to Diagram Model Web (Gambar 1 & 2)
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
                        .testTag("topologi_back_button"),
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
                        text = "Topologi",
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

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (useDiagramView) NocAmber.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface)
                        .border(
                            1.dp,
                            if (useDiagramView) NocAmber else MaterialTheme.colorScheme.outline,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { useDiagramView = !useDiagramView }
                        .padding(horizontal = 10.dp)
                        .testTag("topologi_view_mode_toggle"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Icon(
                            imageVector = if (useDiagramView) Icons.Rounded.ViewList else Icons.Rounded.AccountTree,
                            contentDescription = "Ganti tampilan topologi",
                            tint = if (useDiagramView) NocAmber else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (useDiagramView) "List" else "Diagram",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (useDiagramView) NocAmber else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                        .clickable {
                            lastSyncMillis = System.currentTimeMillis()
                            onRefresh()
                        }
                        .testTag("topologi_refresh_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Segarkan Topologi",
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
                        .testTag("topologi_theme_button"),
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

        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val isWideWeb = maxWidth >= 840.dp || useDiagramView

            if (isWideWeb) {
                TopologiWebDiagramView(
                    nodes = nodes,
                    roots = visibleRoots,
                    infoMap = infoMap,
                    filteredNodeIds = filteredNodeIds,
                    odcCount = odcCount,
                    odpCount = odpCount,
                    activeCount = activeCount,
                    issueCount = issueCount,
                    searchQuery = searchQuery,
                    selectedRootId = selectedRootId,
                    allRoots = allRoots,
                    statusFilter = statusFilter,
                    capacityFilter = capacityFilter,
                    syncTimeLabel = syncTimeLabel,
                    onSearchChange = { searchQuery = it },
                    onSelectRoot = { selectedRootId = it },
                    onStatusChange = { statusFilter = it },
                    onCapacityChange = { capacityFilter = it },
                    onRefresh = {
                        lastSyncMillis = System.currentTimeMillis()
                        onRefresh()
                    },
                    onOpenMapsFtth = onOpenMapsFtth,
                    onNodeClick = { selectedNodeDetail = it }
                )
            } else {
                TopologiMobileTreeView(
                    nodes = nodes,
                    roots = visibleRoots,
                    allRoots = allRoots,
                    infoMap = infoMap,
                    filteredNodeIds = filteredNodeIds,
                    odcCount = odcCount,
                    odpCount = odpCount,
                    activeCount = activeCount,
                    issueCount = issueCount,
                    searchQuery = searchQuery,
                    selectedRootId = selectedRootId,
                    syncTimeLabel = syncTimeLabel,
                    onSearchChange = { searchQuery = it },
                    onSelectRoot = { selectedRootId = it },
                    onOpenMapsFtth = onOpenMapsFtth,
                    onNodeClick = { selectedNodeDetail = it }
                )
            }
        }
    }

    selectedNodeDetail?.let { info ->
        val node = info.node
        val parentCode = node.parentId?.let { pid -> nodes.firstOrNull { it.id == pid }?.code } ?: "Tanpa parent (Root)"
        AlertDialog(
            onDismissRequest = { selectedNodeDetail = null },
            title = { Text("${node.type.label} • ${node.name}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Kode: ${node.code}", fontFamily = JetBrainsMonoFamily, fontSize = 12.sp)
                    Text("Parent: $parentCode", style = MaterialTheme.typography.bodySmall)
                    Text("Status: ${if (info.isOnline) "Online / Aktif" else "Offline / Gangguan"}",
                        color = if (info.isOnline) NocEmeraldOnline else NocRedOffline,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("Kapasitas: ${info.usedPorts} / ${info.totalCapacity} port", fontFamily = JetBrainsMonoFamily, fontSize = 12.sp)
                    Text("Pelanggan: ${info.onlineCount} online • ${info.offlineCount} offline", fontSize = 12.sp)
                    if (node.address.isNotBlank()) Text("Alamat: ${node.address}", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedNodeDetail = null
                        onOpenMapsFtth()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NocAmber, contentColor = Color.Black)
                ) {
                    Text("Lihat di Peta")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedNodeDetail = null }) { Text("Tutup") }
            }
        )
    }
}

// =========================================================================
// GAMBAR 3: MODEL HP & TAB (Collapsible Indented Hierarchy Tree)
// =========================================================================
@Composable
private fun TopologiMobileTreeView(
    nodes: List<FtthNodeEntity>,
    roots: List<FtthNodeEntity>,
    allRoots: List<FtthNodeEntity>,
    infoMap: Map<Long, TopoNodeInfo>,
    filteredNodeIds: Set<Long>,
    odcCount: Int,
    odpCount: Int,
    activeCount: Int,
    issueCount: Int,
    searchQuery: String,
    selectedRootId: Long?,
    syncTimeLabel: String,
    onSearchChange: (String) -> Unit,
    onSelectRoot: (Long?) -> Unit,
    onOpenMapsFtth: () -> Unit,
    onNodeClick: (TopoNodeInfo) -> Unit
) {
    var areaMenuExpanded by remember { mutableStateOf(false) }
    val expandedMap = remember { mutableStateMapOf<Long, Boolean>() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 4 KPI Cards (ODC, ODP, Aktif, Gangguan) matching Gambar 3
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TopoMobileKpi(count = odcCount, label = "ODC", color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                TopoMobileKpi(count = odpCount, label = "ODP", color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                TopoMobileKpi(count = activeCount, label = "Aktif", color = NocEmeraldOnline, modifier = Modifier.weight(1f))
                TopoMobileKpi(count = issueCount, label = "Gangguan", color = NocRedOffline, modifier = Modifier.weight(1f))
            }
        }

        // Search input matching Gambar 3
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
                        Icon(Icons.Rounded.Search, contentDescription = null, tint = NocAmber, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Cari nama atau kode ODP/ODC...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchChange,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                            cursorBrush = SolidColor(NocAmber),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Rounded.Close, contentDescription = "Bersihkan", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // Area Dropdown matching Gambar 3
        item {
            Box {
                val selectedAreaName = selectedRootId?.let { id -> nodes.firstOrNull { it.id == id }?.name } ?: "Semua area"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                        .clickable { areaMenuExpanded = true }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(NocAmber.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.AccountTree, contentDescription = null, tint = NocAmber, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Area", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(selectedAreaName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                DropdownMenu(expanded = areaMenuExpanded, onDismissRequest = { areaMenuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Semua area") },
                        onClick = {
                            onSelectRoot(null)
                            areaMenuExpanded = false
                        }
                    )
                    allRoots.forEach { root ->
                        DropdownMenuItem(
                            text = { Text("${root.name} (${root.code})") },
                            onClick = {
                                onSelectRoot(root.id)
                                areaMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Timestamp matching Gambar 3
        item {
            Text(
                text = "Sinkron terakhir: $syncTimeLabel",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Tree List Container Card
        if (nodes.isEmpty()) {
            item {
                TopoEmptyCard(onOpenMapsFtth = onOpenMapsFtth)
            }
        } else {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp)),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        roots.forEach { root ->
                            MobileTreeBranch(
                                node = root,
                                allNodes = nodes,
                                infoMap = infoMap,
                                filteredNodeIds = filteredNodeIds,
                                hasActiveSearch = searchQuery.isNotBlank(),
                                expandedMap = expandedMap,
                                depth = 0,
                                onNodeClick = onNodeClick
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopoMobileKpi(count: Int, label: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(text = count.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
            Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MobileTreeBranch(
    node: FtthNodeEntity,
    allNodes: List<FtthNodeEntity>,
    infoMap: Map<Long, TopoNodeInfo>,
    filteredNodeIds: Set<Long>,
    hasActiveSearch: Boolean,
    expandedMap: MutableMap<Long, Boolean>,
    depth: Int,
    onNodeClick: (TopoNodeInfo) -> Unit
) {
    val info = infoMap[node.id] ?: return
    val children = remember(allNodes, node.id) { allNodes.filter { it.parentId == node.id } }
    val isExpanded = expandedMap[node.id] ?: (depth < 2 || hasActiveSearch)

    fun subtreeMatches(nId: Long, visited: MutableSet<Long> = mutableSetOf()): Boolean {
        if (!visited.add(nId)) return false
        if (nId in filteredNodeIds) return true
        return allNodes.filter { it.parentId == nId }.any { subtreeMatches(it.id, visited) }
    }
    if (hasActiveSearch && !subtreeMatches(node.id)) return

    val statusDotColor = if (info.isOnline) NocEmeraldOnline else NocRedOffline
    val badgeColor = Color(0xFF7C3AED)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNodeClick(info) }
                .padding(start = (12 + depth * 18).dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (children.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .clickable { expandedMap[node.id] = !isExpanded },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowDown else Icons.Rounded.ChevronRight,
                            contentDescription = "Expand",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(24.dp))
                }

                Spacer(modifier = Modifier.width(4.dp))

                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(statusDotColor)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeColor.copy(alpha = 0.16f))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = node.type.label.uppercase(Locale.ROOT),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = node.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${node.code} · ${info.usedPorts}/${info.totalCapacity} port",
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (children.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = children.size.toString(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        AnimatedVisibility(visible = isExpanded && depth < 6) {
            Column {
                children.forEach { child ->
                    MobileTreeBranch(
                        node = child,
                        allNodes = allNodes,
                        infoMap = infoMap,
                        filteredNodeIds = filteredNodeIds,
                        hasActiveSearch = hasActiveSearch,
                        expandedMap = expandedMap,
                        depth = depth + 1,
                        onNodeClick = onNodeClick
                    )
                }
            }
        }
    }
}

// =========================================================================
// GAMBAR 1 & 2: MODEL WEB (Monitoring ODP Tree + Struktur Jaringan Canvas)
// =========================================================================
@Composable
private fun TopologiWebDiagramView(
    nodes: List<FtthNodeEntity>,
    roots: List<FtthNodeEntity>,
    infoMap: Map<Long, TopoNodeInfo>,
    filteredNodeIds: Set<Long>,
    odcCount: Int,
    odpCount: Int,
    activeCount: Int,
    issueCount: Int,
    searchQuery: String,
    selectedRootId: Long?,
    allRoots: List<FtthNodeEntity>,
    statusFilter: String,
    capacityFilter: String,
    syncTimeLabel: String,
    onSearchChange: (String) -> Unit,
    onSelectRoot: (Long?) -> Unit,
    onStatusChange: (String) -> Unit,
    onCapacityChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onOpenMapsFtth: () -> Unit,
    onNodeClick: (TopoNodeInfo) -> Unit
) {
    var rootMenu by remember { mutableStateOf(false) }
    var statusMenu by remember { mutableStateOf(false) }
    var capMenu by remember { mutableStateOf(false) }

    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }
    var diagramZoom by remember { mutableFloatStateOf(1f) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    val textMeasurer = rememberTextMeasurer()

    // Compute orthogonal tree layout coordinates for all visible roots
    val placedNodes = remember(nodes, roots, infoMap, filteredNodeIds) {
        val result = mutableListOf<DiagramPlacedNode>()
        val cardW = 196f
        val cardH = 104f
        val gapX = 28f
        val gapY = 54f
        val visited = mutableSetOf<Long>()

        fun measureSubtreeWidth(nId: Long): Float {
            val children = nodes.filter { it.parentId == nId && it.id in filteredNodeIds }
            if (children.isEmpty()) return cardW
            val sumChildren = children.sumOf { measureSubtreeWidth(it.id).toDouble() }.toFloat() +
                (children.size - 1) * gapX
            return maxOf(cardW, sumChildren)
        }

        fun layoutNode(node: FtthNodeEntity, centerX: Float, topY: Float, parentId: Long?) {
            if (!visited.add(node.id)) return
            val info = infoMap[node.id] ?: return
            result.add(DiagramPlacedNode(info = info, x = centerX - cardW / 2f, y = topY, parentId = parentId))

            val children = nodes.filter { it.parentId == node.id && it.id in filteredNodeIds }
            if (children.isEmpty()) return
            val totalW = children.sumOf { measureSubtreeWidth(it.id).toDouble() }.toFloat() +
                (children.size - 1) * gapX
            var cursorX = centerX - totalW / 2f
            children.forEach { child ->
                val cw = measureSubtreeWidth(child.id)
                layoutNode(child, cursorX + cw / 2f, topY + cardH + gapY, node.id)
                cursorX += cw + gapX
            }
        }

        val activeRoots = roots.filter { it.id in filteredNodeIds }
        var rootCursorX = 0f
        activeRoots.forEach { r ->
            val rw = measureSubtreeWidth(r.id)
            layoutNode(r, rootCursorX + rw / 2f, 40f, null)
            rootCursorX += rw + 60f
        }
        // Center horizontally around x = 0
        if (result.isNotEmpty()) {
            val minX = result.minOf { it.x }
            val maxX = result.maxOf { it.x + cardW }
            val shiftX = -(minX + maxX) / 2f
            result.map { it.copy(x = it.x + shiftX) }
        } else emptyList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Title & Blue Info Sync Banner matching Gambar 1
        Column {
            Text(
                text = "Monitoring ODP Tree",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Topology jaringan distribusi ODC/ODP.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, NocBlueDownload.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            color = NocBlueDownload.copy(alpha = 0.14f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, tint = NocBlueDownload, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sinkronisasi data Maps & FTTH. Terakhir: $syncTimeLabel",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onRefresh,
                    colors = ButtonDefaults.buttonColors(containerColor = NocAmber, contentColor = Color(0xFF0B0E14)),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("Cek Sekarang", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Filter Bar matching Gambar 1
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
                    .horizontalScroll(rememberScrollState())
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .width(170.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(NocAmber),
                        decorationBox = { inner ->
                            if (searchQuery.isEmpty()) {
                                Text("cth: ODP-SR01", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            inner()
                        }
                    )
                }

                Box {
                    val areaTxt = selectedRootId?.let { id -> nodes.firstOrNull { it.id == id }?.name } ?: "Semua Area"
                    WebFilterChip(label = areaTxt, onClick = { rootMenu = true })
                    DropdownMenu(expanded = rootMenu, onDismissRequest = { rootMenu = false }) {
                        DropdownMenuItem(text = { Text("Semua Area") }, onClick = { onSelectRoot(null); rootMenu = false })
                        allRoots.forEach { r ->
                            DropdownMenuItem(text = { Text(r.name) }, onClick = { onSelectRoot(r.id); rootMenu = false })
                        }
                    }
                }

                Box {
                    WebFilterChip(label = "Status: $statusFilter", onClick = { statusMenu = true })
                    DropdownMenu(expanded = statusMenu, onDismissRequest = { statusMenu = false }) {
                        listOf("Semua", "Online", "Gangguan").forEach { opt ->
                            DropdownMenuItem(text = { Text(opt) }, onClick = { onStatusChange(opt); statusMenu = false })
                        }
                    }
                }

                Box {
                    WebFilterChip(label = "Kapasitas: $capacityFilter", onClick = { capMenu = true })
                    DropdownMenu(expanded = capMenu, onDismissRequest = { capMenu = false }) {
                        listOf("Semua", "Tersedia", "Penuh").forEach { opt ->
                            DropdownMenuItem(text = { Text(opt) }, onClick = { onCapacityChange(opt); capMenu = false })
                        }
                    }
                }

                Button(
                    onClick = {
                        panX = 0f
                        panY = 0f
                        diagramZoom = 1f
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NocAmber, contentColor = Color.Black),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Rounded.FilterAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Filter", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 4 KPI Cards matching Gambar 1 (Total ODC, Total ODP, ODP Aktif, ODP Gangguan)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WebKpiCard(icon = Icons.Outlined.Business, count = odcCount, label = "Total ODC", accent = NocBlueDownload, modifier = Modifier.weight(1f))
            WebKpiCard(icon = Icons.Outlined.AccountTree, count = odpCount, label = "Total ODP", accent = NocBlueDownload, modifier = Modifier.weight(1f))
            WebKpiCard(icon = Icons.Outlined.Bolt, count = activeCount, label = "ODP Aktif", accent = Color(0xFF06B6D4), modifier = Modifier.weight(1f))
            WebKpiCard(icon = Icons.Outlined.WarningAmber, count = issueCount, label = "ODP Gangguan", accent = NocRedOffline, modifier = Modifier.weight(1f))
        }

        // Struktur Jaringan Interactive Tree Diagram Canvas (Gambar 1 & 2)
        if (nodes.isEmpty()) {
            TopoEmptyCard(onOpenMapsFtth = onOpenMapsFtth)
        } else {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF0B0E14)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF111620))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Outlined.AccountTree, contentDescription = null, tint = NocAmber, modifier = Modifier.size(16.dp))
                            Text("Struktur Jaringan", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Text("Klik distribusi untuk membuka detail", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.06f))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clipToBounds()
                            .onSizeChanged { canvasSize = it }
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    panX += dragAmount.x
                                    panY += dragAmount.y
                                }
                            }
                            .pointerInput(placedNodes, diagramZoom, panX, panY) {
                                detectTapGestures { tap ->
                                    val originX = canvasSize.width / 2f + panX
                                    val originY = 24f + panY
                                    val cardW = 196f * diagramZoom
                                    val cardH = 104f * diagramZoom
                                    val hit = placedNodes.firstOrNull { p ->
                                        val sx = originX + p.x * diagramZoom
                                        val sy = originY + p.y * diagramZoom
                                        tap.x in sx..(sx + cardW) && tap.y in sy..(sy + cardH)
                                    }
                                    if (hit != null) onNodeClick(hit.info)
                                }
                            }
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize().clipToBounds()) {
                            // Subtle dot grid matching Gambar 1 & 2
                            val step = 28f
                            var gx = 12f
                            while (gx < size.width) {
                                var gy = 12f
                                while (gy < size.height) {
                                    drawCircle(color = Color.White.copy(alpha = 0.08f), radius = 1.4f, center = Offset(gx, gy))
                                    gy += step
                                }
                                gx += step
                            }

                            val originX = size.width / 2f + panX
                            val originY = 24f + panY
                            val cardW = 196f * diagramZoom
                            val cardH = 104f * diagramZoom

                            // Orthogonal connector lines
                            val byId = placedNodes.associateBy { it.info.node.id }
                            placedNodes.forEach { child ->
                                val parent = child.parentId?.let { byId[it] } ?: return@forEach
                                val startX = originX + parent.x * diagramZoom + cardW / 2f
                                val startY = originY + parent.y * diagramZoom + cardH
                                val endX = originX + child.x * diagramZoom + cardW / 2f
                                val endY = originY + child.y * diagramZoom
                                val midY = (startY + endY) / 2f
                                val path = Path().apply {
                                    moveTo(startX, startY)
                                    lineTo(startX, midY)
                                    lineTo(endX, midY)
                                    lineTo(endX, endY)
                                }
                                drawPath(
                                    path = path,
                                    color = Color(0xFF475569),
                                    style = Stroke(width = 2.2f)
                                )
                            }

                            // Draw each diagram node card matching Gambar 1 & 2
                            placedNodes.forEach { placed ->
                                drawWebTreeCard(
                                    info = placed.info,
                                    topLeft = Offset(originX + placed.x * diagramZoom, originY + placed.y * diagramZoom),
                                    cardSize = Size(cardW, cardH),
                                    zoom = diagramZoom,
                                    textMeasurer = textMeasurer
                                )
                            }
                        }

                        // Bottom-Left Zoom Controls matching Gambar 2
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(12.dp)
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xEE141923)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { diagramZoom = (diagramZoom + 0.15f).coerceAtMost(1.8f) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Rounded.Add, contentDescription = "Perbesar", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                                IconButton(onClick = { diagramZoom = (diagramZoom - 0.15f).coerceAtLeast(0.45f) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Rounded.Remove, contentDescription = "Perkecil", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = {
                                        panX = 0f
                                        panY = 0f
                                        diagramZoom = 0.85f
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Rounded.OpenInFull, contentDescription = "Fit Layar", tint = Color.White, modifier = Modifier.size(15.dp))
                                }
                                IconButton(
                                    onClick = {
                                        panX = 0f
                                        panY = 0f
                                        diagramZoom = 1f
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Rounded.MyLocation, contentDescription = "Reset Posisi", tint = Color.White, modifier = Modifier.size(15.dp))
                                }
                            }
                        }

                        // Bottom-Right Legend Pill matching Gambar 2 ("● Online ● Warning ● Offline")
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp)
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xEE141923)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                DiagramLegendDot("Online", NocBlueDownload)
                                DiagramLegendDot("Warning", NocAmber)
                                DiagramLegendDot("Offline", Color(0xFF64748B))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawWebTreeCard(
    info: TopoNodeInfo,
    topLeft: Offset,
    cardSize: Size,
    zoom: Float,
    textMeasurer: TextMeasurer
) {
    val bgColor = if (info.isOnline) Color(0xFF0F213B) else Color(0xFF141821)
    val borderColor = if (info.isOnline) NocBlueDownload.copy(alpha = 0.7f) else Color(0xFF2D3546)

    drawRoundRect(
        color = bgColor,
        topLeft = topLeft,
        size = cardSize,
        cornerRadius = CornerRadius(12f * zoom, 12f * zoom)
    )
    drawRoundRect(
        color = borderColor,
        topLeft = topLeft,
        size = cardSize,
        cornerRadius = CornerRadius(12f * zoom, 12f * zoom),
        style = Stroke(width = 2f)
    )

    val pad = 12f * zoom
    val titleLayout = textMeasurer.measure(
        text = info.node.name.take(16),
        style = TextStyle(color = Color.White, fontSize = (11f * zoom).sp, fontWeight = FontWeight.Bold)
    )
    drawText(titleLayout, topLeft = Offset(topLeft.x + pad, topLeft.y + pad))

    val statusText = if (info.isOnline) "Online" else "Offline"
    val statusBg = if (info.isOnline) NocBlueDownload.copy(alpha = 0.25f) else Color(0xFF242B38)
    val statusFg = if (info.isOnline) Color(0xFF60A5FA) else Color(0xFF94A3B8)
    val statusLayout = textMeasurer.measure(
        text = statusText,
        style = TextStyle(color = statusFg, fontSize = (8.5f * zoom).sp, fontWeight = FontWeight.Bold)
    )
    val pillW = statusLayout.size.width + 12f * zoom
    val pillH = statusLayout.size.height + 6f * zoom
    val pillTL = Offset(topLeft.x + cardSize.width - pad - pillW, topLeft.y + pad)
    drawRoundRect(
        color = statusBg,
        topLeft = pillTL,
        size = Size(pillW, pillH),
        cornerRadius = CornerRadius(8f * zoom, 8f * zoom)
    )
    drawText(statusLayout, topLeft = Offset(pillTL.x + 6f * zoom, pillTL.y + 3f * zoom))

    val codeLayout = textMeasurer.measure(
        text = info.node.code.take(18),
        style = TextStyle(color = Color(0xFF94A3B8), fontSize = (9f * zoom).sp)
    )
    drawText(codeLayout, topLeft = Offset(topLeft.x + pad, topLeft.y + pad + 18f * zoom))

    // PORT & CAPACITY footer
    val footerY = topLeft.y + cardSize.height - 34f * zoom
    val portLabel = textMeasurer.measure("PORT", TextStyle(color = Color(0xFF64748B), fontSize = (8f * zoom).sp, fontWeight = FontWeight.Bold))
    val capLabel = textMeasurer.measure("CAPACITY", TextStyle(color = Color(0xFF64748B), fontSize = (8f * zoom).sp, fontWeight = FontWeight.Bold))
    drawText(portLabel, topLeft = Offset(topLeft.x + pad, footerY))
    drawText(capLabel, topLeft = Offset(topLeft.x + cardSize.width * 0.52f, footerY))

    val portVal = textMeasurer.measure(
        text = "${info.onlineCount} / ${info.offlineCount}",
        style = TextStyle(color = Color(0xFF38BDF8), fontSize = (10.5f * zoom).sp, fontWeight = FontWeight.Bold)
    )
    val capVal = textMeasurer.measure(
        text = "${info.usedPorts} / ${info.totalCapacity}",
        style = TextStyle(color = Color.White, fontSize = (10.5f * zoom).sp, fontWeight = FontWeight.Bold)
    )
    drawText(portVal, topLeft = Offset(topLeft.x + pad, footerY + 13f * zoom))
    drawText(capVal, topLeft = Offset(topLeft.x + cardSize.width * 0.52f, footerY + 13f * zoom))
}

@Composable
private fun WebFilterChip(label: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun WebKpiCard(
    icon: ImageVector,
    count: Int,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(17.dp))
            }
            Column {
                Text(count.toString(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = accent)
                Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun DiagramLegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(color))
        Text(label, color = Color(0xFFE2E8F0), fontSize = 11.sp)
    }
}

@Composable
private fun TopoEmptyCard(onOpenMapsFtth: () -> Unit) {
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
                text = "Belum Ada Pohon Topologi",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Data topologi diambil dari titik distribusi dan parent yang Anda buat di halaman Maps & FTTH.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onOpenMapsFtth,
                colors = ButtonDefaults.buttonColors(containerColor = NocAmber, contentColor = Color(0xFF0B0E14)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Buka Maps & FTTH", fontWeight = FontWeight.Bold)
            }
        }
    }
}
