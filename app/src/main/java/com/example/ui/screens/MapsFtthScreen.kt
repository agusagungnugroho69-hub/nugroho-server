package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.AddLocationAlt
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.FitScreen
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.OpenWith
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.model.CableRouteEntity
import com.example.data.model.FtthCustomerPinEntity
import com.example.data.model.FtthNodeEntity
import com.example.data.model.FtthNodeType
import com.example.ui.components.CustomerPinRenderItem
import com.example.ui.components.FtthBasemap
import com.example.ui.components.FtthInteractiveMapCanvas
import com.example.ui.components.GeoProjection
import com.example.ui.components.LatLngPoint
import com.example.ui.components.NearestRoutePreview
import com.example.ui.components.PinLinkStatus
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.NocAmber
import com.example.ui.theme.NocCyanTelemetry
import com.example.ui.theme.NocEmeraldOnline
import com.example.ui.theme.NocRedOffline
import com.example.util.Formatters
import com.example.viewmodel.DashboardUiState
import java.util.Locale

@Composable
fun MapsFtthScreen(
    uiState: DashboardUiState,
    nodes: List<FtthNodeEntity>,
    customerPins: List<FtthCustomerPinEntity>,
    cableRoutes: List<CableRouteEntity>,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onBack: () -> Unit,
    onAddNode: (FtthNodeType, String, String, Int, Int, String, String, Long?, Double, Double) -> Unit,
    onMoveNode: (FtthNodeEntity, Double, Double) -> Unit,
    onDeleteNode: (FtthNodeEntity) -> Unit,
    onAddCustomerPin: (String, String, Long?, Double, Double, String) -> Unit,
    onMoveCustomerPin: (FtthCustomerPinEntity, Double, Double) -> Unit,
    onDeleteCustomerPin: (FtthCustomerPinEntity) -> Unit,
    onAddCableRoute: (String, String, Int, Int) -> Unit,
    onDeleteCableRoute: (CableRouteEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    // Camera default centered around Central/East Java FTTH coordinate from user screenshots
    val initialCenter = remember(nodes, customerPins) {
        val firstNode = nodes.firstOrNull()
        val firstPin = customerPins.firstOrNull()
        when {
            firstNode != null -> LatLngPoint(firstNode.latitude, firstNode.longitude)
            firstPin != null -> LatLngPoint(firstPin.latitude, firstPin.longitude)
            else -> LatLngPoint(-6.922603, 111.477886)
        }
    }

    var centerLat by remember { mutableStateOf(initialCenter.lat) }
    var centerLng by remember { mutableStateOf(initialCenter.lng) }
    var zoom by remember { mutableFloatStateOf(15.2f) }

    // Basemap & layer toggles matching Gambar 6 (Kontrol Peta)
    var basemap by remember { mutableStateOf(FtthBasemap.SATELIT) }
    var showMapControls by remember { mutableStateOf(false) }
    var showCustomerLayer by remember { mutableStateOf(true) }
    var showDistributionLayer by remember { mutableStateOf(true) }
    var showCustomerLineLayer by remember { mutableStateOf(true) }
    var showDistributionLineLayer by remember { mutableStateOf(false) }
    var showCableRouteLayer by remember { mutableStateOf(true) }
    var dragMarkerMode by remember { mutableStateOf(false) }

    // Search & live status overlay state
    var searchQuery by remember { mutableStateOf("") }
    var showOfflineList by remember { mutableStateOf(false) }

    // Long-press context sheet (Gambar 3)
    var contextLatLng by remember { mutableStateOf<LatLngPoint?>(null) }

    // Tambah Distribusi modal (Gambar 4 & 5)
    var addDistLatLng by remember { mutableStateOf<LatLngPoint?>(null) }

    // Interactive Cable Route drawing mode
    var isDrawingCable by remember { mutableStateOf(false) }
    var draftCablePoints by remember { mutableStateOf<List<LatLngPoint>>(emptyList()) }
    var showSaveCableDialog by remember { mutableStateOf(false) }

    // Nearest route result
    var nearestRoute by remember { mutableStateOf<NearestRoutePreview?>(null) }

    // Inspected Node / Customer Pin dialogs
    var selectedNode by remember { mutableStateOf<FtthNodeEntity?>(null) }
    var pinForNode by remember { mutableStateOf<FtthNodeEntity?>(null) }
    var selectedCustomerPin by remember { mutableStateOf<CustomerPinRenderItem?>(null) }
    var showLegendHelp by remember { mutableStateOf(false) }

    // Map customer pins to live PPPoE telemetry status
    val renderedPins = remember(customerPins, uiState.allPppoeClients) {
        customerPins.map { pin ->
            val match = uiState.allPppoeClients.firstOrNull { client ->
                (pin.pppoeUsername.isNotBlank() && client.name.equals(pin.pppoeUsername, ignoreCase = true)) ||
                    client.name.equals(pin.name, ignoreCase = true)
            }
            val status = when {
                match == null -> PinLinkStatus.OFFLINE
                match.isDisabled -> PinLinkStatus.ISOLIR
                match.isOnline -> PinLinkStatus.ONLINE
                else -> PinLinkStatus.OFFLINE
            }
            val speedLabel = if (status == PinLinkStatus.ONLINE && uiState.onlineCustomers > 0) {
                val avgDown = (uiState.totalRxBps / uiState.onlineCustomers.coerceAtLeast(1)).coerceAtLeast(128_000L)
                val avgUp = (uiState.totalTxBps / uiState.onlineCustomers.coerceAtLeast(1)).coerceAtLeast(64_000L)
                "${Formatters.formatBps(avgDown)}/${Formatters.formatBps(avgUp)}"
            } else ""
            CustomerPinRenderItem(pin = pin, status = status, speedLabel = speedLabel)
        }
    }

    // Live counts for floating card (prefer live PPPoE counts, include pins if no router loaded)
    val liveOnlineCount = if (uiState.allPppoeClients.isNotEmpty()) {
        uiState.onlineCustomers
    } else {
        renderedPins.count { it.status == PinLinkStatus.ONLINE }
    }
    val offlinePppoeClients = remember(uiState.allPppoeClients) {
        uiState.allPppoeClients.filter { !it.isOnline }
    }
    val liveOfflineCount = if (uiState.allPppoeClients.isNotEmpty()) {
        uiState.offlineCustomers
    } else {
        renderedPins.count { it.status == PinLinkStatus.OFFLINE }
    }

    val searchMatches = remember(searchQuery, nodes, renderedPins) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) emptyList<Pair<String, LatLngPoint>>()
        else {
            val nMatches = nodes.filter {
                it.name.lowercase().contains(q) || it.code.lowercase().contains(q)
            }.map { "${it.type.label} ${it.code} • ${it.name}" to LatLngPoint(it.latitude, it.longitude) }
            val pMatches = renderedPins.filter {
                it.pin.name.lowercase().contains(q) || it.pin.pppoeUsername.lowercase().contains(q)
            }.map { "Pelanggan ${it.pin.name}" to LatLngPoint(it.pin.latitude, it.pin.longitude) }
            (nMatches + pMatches).take(6)
        }
    }

    fun fitAllMarkers() {
        val allPoints = buildList {
            nodes.forEach { add(LatLngPoint(it.latitude, it.longitude)) }
            customerPins.forEach { add(LatLngPoint(it.latitude, it.longitude)) }
        }
        if (allPoints.isEmpty()) {
            centerLat = -6.922603
            centerLng = 111.477886
            zoom = 15.2f
            return
        }
        val minLat = allPoints.minOf { it.lat }
        val maxLat = allPoints.maxOf { it.lat }
        val minLng = allPoints.minOf { it.lng }
        val maxLng = allPoints.maxOf { it.lng }
        centerLat = (minLat + maxLat) / 2.0
        centerLng = (minLng + maxLng) / 2.0
        val span = maxOf(maxLat - minLat, maxLng - minLng)
        zoom = when {
            span < 0.002 -> 17.5f
            span < 0.008 -> 16.0f
            span < 0.02 -> 14.8f
            span < 0.08 -> 13.0f
            else -> 11.2f
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Header Bar matching Gambar 3 (sleek black bar always visible)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(2f)
                .background(Color(0xFF0E1218))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MapTopIconButton(
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Kembali",
                        onClick = onBack,
                        testTag = "maps_back_button"
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Maps & FTTH",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MapTopIconButton(
                        icon = Icons.Rounded.Timeline,
                        contentDescription = "Mode Rute Kabel",
                        isActive = isDrawingCable,
                        onClick = {
                            showMapControls = false
                            if (isDrawingCable) {
                                if (draftCablePoints.size >= 2) showSaveCableDialog = true
                                else {
                                    isDrawingCable = false
                                    draftCablePoints = emptyList()
                                }
                            } else {
                                isDrawingCable = true
                                showCableRouteLayer = true
                                draftCablePoints = emptyList()
                            }
                        },
                        testTag = "maps_cable_mode_button"
                    )
                    MapTopIconButton(
                        icon = Icons.Rounded.Tune,
                        contentDescription = "Kontrol Peta",
                        isActive = showMapControls,
                        onClick = { showMapControls = !showMapControls },
                        testTag = "maps_controls_button"
                    )
                    MapTopIconButton(
                        icon = if (darkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                        contentDescription = "Ganti Tema",
                        onClick = onToggleTheme,
                        testTag = "maps_theme_button"
                    )
                }
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
        }

        // Full-Screen Map Canvas + Floating Overlays (strictly clipped so tiles never cover header)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clipToBounds()
        ) {
            FtthInteractiveMapCanvas(
                basemap = basemap,
                centerLat = centerLat,
                centerLng = centerLng,
                zoom = zoom,
                nodes = nodes,
                customerPins = renderedPins,
                cableRoutes = cableRoutes,
                draftCablePoints = draftCablePoints,
                nearestRoute = nearestRoute,
                isDrawingCable = isDrawingCable,
                showCustomerLayer = showCustomerLayer,
                showDistributionLayer = showDistributionLayer,
                showCustomerLineLayer = showCustomerLineLayer,
                showDistributionLineLayer = showDistributionLineLayer,
                showCableRouteLayer = showCableRouteLayer,
                dragMarkerMode = dragMarkerMode,
                onCameraMove = { lat, lng, z ->
                    centerLat = lat
                    centerLng = lng
                    zoom = z
                },
                onMapTap = { tappedPoint ->
                    if (isDrawingCable) {
                        draftCablePoints = draftCablePoints + tappedPoint
                    } else if (showMapControls) {
                        showMapControls = false
                    }
                },
                onMapLongPress = { pressedPoint ->
                    if (!isDrawingCable) {
                        contextLatLng = pressedPoint
                    }
                },
                onNodeTap = { node -> selectedNode = node },
                onCustomerPinTap = { item -> selectedCustomerPin = item },
                onNodeDragged = { node, newPos -> onMoveNode(node, newPos.lat, newPos.lng) },
                onCustomerPinDragged = { pin, newPos -> onMoveCustomerPin(pin, newPos.lat, newPos.lng) }
            )

            // Floating Search + Live Status Card (Gambar 2)
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 10.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xEB131820)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Search bar input
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1A202B))
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                                tint = Color(0xFF7B8798),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Cari customer / distribusi",
                                        color = Color(0xFF7B8798),
                                        fontSize = 13.sp
                                    )
                                }
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                                    cursorBrush = SolidColor(NocAmber),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            if (searchQuery.isNotEmpty()) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Bersihkan pencarian",
                                    tint = Color(0xFF9AA5B4),
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable { searchQuery = "" }
                                )
                            }
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.06f))

                        // Live Online & Traffic row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(NocEmeraldOnline)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$liveOnlineCount online",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "↓ ${Formatters.formatBps(uiState.totalRxBps)}",
                                    color = Color(0xFFC7D0DC),
                                    fontFamily = JetBrainsMonoFamily,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "↑ ${Formatters.formatBps(uiState.totalTxBps)}",
                                    color = Color(0xFFC7D0DC),
                                    fontFamily = JetBrainsMonoFamily,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.06f))

                        // Bottom chips: ● Status live | X offline ⌄
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF103224))
                                    .border(1.dp, NocEmeraldOnline.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(NocEmeraldOnline)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (uiState.isPollingLive) "Status live" else "Terjeda",
                                    color = NocEmeraldOnline,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF28191D))
                                    .border(1.dp, NocRedOffline.copy(alpha = 0.28f), RoundedCornerShape(10.dp))
                                    .clickable { showOfflineList = !showOfflineList }
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.WifiOff,
                                    contentDescription = null,
                                    tint = NocRedOffline,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "$liveOfflineCount offline",
                                    color = Color(0xFFE2E8F0),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Icon(
                                    imageVector = if (showOfflineList) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }

                        // Expandable Offline list
                        AnimatedVisibility(visible = showOfflineList) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF0E131B))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (offlinePppoeClients.isEmpty()) {
                                    Text(
                                        text = "Tidak ada pelanggan offline saat ini.",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp
                                    )
                                } else {
                                    offlinePppoeClients.take(5).forEach { client ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    val pin = customerPins.firstOrNull {
                                                        it.pppoeUsername.equals(client.name, true) || it.name.equals(client.name, true)
                                                    }
                                                    if (pin != null) {
                                                        centerLat = pin.latitude
                                                        centerLng = pin.longitude
                                                        zoom = 17.5f
                                                    }
                                                    showOfflineList = false
                                                }
                                                .padding(vertical = 3.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(client.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                            Text(client.routerName, color = NocRedOffline, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // Search results dropdown
                        AnimatedVisibility(visible = searchMatches.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF0E131B))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                searchMatches.forEach { (label, pt) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                centerLat = pt.lat
                                                centerLng = pt.lng
                                                zoom = 17.6f
                                                searchQuery = ""
                                            }
                                            .padding(vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.LocationOn,
                                            contentDescription = null,
                                            tint = NocAmber,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(label, color = Color.White, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }
                }

                // Active Mode Indicators (Drawing Cable / Drag Marker / Nearest Route Result)
                if (isDrawingCable) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val meters = GeoProjection.totalRouteMeters(draftCablePoints)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xF0151A24),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, NocAmber.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Mode Gambar Rute Kabel", color = NocAmber, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(
                                    text = "Ketuk peta tambah titik (${draftCablePoints.size} titik • ${meters}m)",
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 11.sp
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { if (draftCablePoints.isNotEmpty()) draftCablePoints = draftCablePoints.dropLast(1) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Rounded.Undo, contentDescription = "Undo", tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                                TextButton(
                                    onClick = {
                                        isDrawingCable = false
                                        draftCablePoints = emptyList()
                                    }
                                ) {
                                    Text("Batal", color = NocRedOffline, fontSize = 12.sp)
                                }
                                Button(
                                    onClick = { showSaveCableDialog = true },
                                    enabled = draftCablePoints.size >= 2,
                                    colors = ButtonDefaults.buttonColors(containerColor = NocAmber, contentColor = Color.Black),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Simpan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                nearestRoute?.let { preview ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xF0111924),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, NocCyanTelemetry.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Rute Terdekat: ${preview.targetNode.code}",
                                    color = NocCyanTelemetry,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "${preview.targetNode.name} • Jarak lurus ${preview.distanceMeters} m",
                                    color = Color.White,
                                    fontSize = 11.sp
                                )
                            }
                            IconButton(onClick = { nearestRoute = null }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Rounded.Close, contentDescription = "Tutup rute", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // Floating Kontrol Peta Popover (Gambar 6)
            androidx.compose.animation.AnimatedVisibility(
                visible = showMapControls,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 12.dp, end = 12.dp)
            ) {
                MapControlPopover(
                    basemap = basemap,
                    showCustomer = showCustomerLayer,
                    showDistribution = showDistributionLayer,
                    showCustomerLine = showCustomerLineLayer,
                    showDistributionLine = showDistributionLineLayer,
                    showCableRoute = showCableRouteLayer,
                    dragMarker = dragMarkerMode,
                    onSelectBasemap = { basemap = it },
                    onToggleCustomer = { showCustomerLayer = !showCustomerLayer },
                    onToggleDistribution = { showDistributionLayer = !showDistributionLayer },
                    onToggleCustomerLine = { showCustomerLineLayer = !showCustomerLineLayer },
                    onToggleDistributionLine = { showDistributionLineLayer = !showDistributionLineLayer },
                    onToggleCableRoute = { showCableRouteLayer = !showCableRouteLayer },
                    onToggleDragMarker = { dragMarkerMode = !dragMarkerMode },
                    onFitAllMarkers = {
                        fitAllMarkers()
                        showMapControls = false
                    },
                    onClose = { showMapControls = false }
                )
            }

            // Bottom-Left Zoom + Legend Pill matching Gambar 2 ("15.2 | ● Online ● Offline ● Isolir")
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 14.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xEB131820)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = String.format(Locale.US, "%.1f", zoom),
                        color = Color(0xFF94A3B8),
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 11.sp
                    )
                    Text(text = "|", color = Color(0xFF475569), fontSize = 11.sp)
                    LegendDotItem(label = "Online", color = NocEmeraldOnline)
                    LegendDotItem(label = "Offline", color = NocRedOffline)
                    LegendDotItem(label = "Isolir", color = NocAmber)
                }
            }

            // Bottom-Right Map Control Buttons (+, -, Recenter, Quick Context Action, Info)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clickable { contextLatLng = LatLngPoint(centerLat, centerLng) }
                            .testTag("map_open_context_sheet"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AddLocationAlt,
                            contentDescription = "Aksi Peta (Tambah Distribusi / Rute)",
                            tint = Color(0xFF1E293B),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clickable { zoom = (zoom + 0.8f).coerceAtMost(19f) }
                                .testTag("map_zoom_in"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "Perbesar", tint = Color(0xFF1E293B), modifier = Modifier.size(20.dp))
                        }
                        HorizontalDivider(modifier = Modifier.width(26.dp), color = Color(0xFFE2E8F0))
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clickable { zoom = (zoom - 0.8f).coerceAtLeast(4f) }
                                .testTag("map_zoom_out"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Remove, contentDescription = "Perkecil", tint = Color(0xFF1E293B), modifier = Modifier.size(20.dp))
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clickable { fitAllMarkers() }
                            .testTag("map_recenter"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.MyLocation, contentDescription = "Pusatkan Peta", tint = Color(0xFF1E293B), modifier = Modifier.size(19.dp))
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clickable { showLegendHelp = true }
                            .testTag("map_info_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Info, contentDescription = "Panduan Peta", tint = Color(0xFF1E293B), modifier = Modifier.size(20.dp))
                    }
                }
            }

            // Gambar 3: Long-Press Bottom Context Action Sheet
            androidx.compose.animation.AnimatedVisibility(
                visible = contextLatLng != null,
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                val tappedGeo = contextLatLng ?: LatLngPoint(centerLat, centerLng)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    color = Color(0xF813171F)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        ContextSheetRow(
                            icon = Icons.Outlined.LocationOn,
                            label = "Tambah distribusi",
                            testTag = "map_ctx_add_dist",
                            onClick = {
                                addDistLatLng = tappedGeo
                                contextLatLng = null
                            }
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(horizontal = 16.dp))
                        ContextSheetRow(
                            icon = Icons.Rounded.Timeline,
                            label = "Gambar rute kabel",
                            testTag = "map_ctx_draw_cable",
                            onClick = {
                                isDrawingCable = true
                                showCableRouteLayer = true
                                draftCablePoints = listOf(tappedGeo)
                                contextLatLng = null
                            }
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(horizontal = 16.dp))
                        ContextSheetRow(
                            icon = Icons.Outlined.MyLocation,
                            label = "Cari rute terdekat",
                            testTag = "map_ctx_nearest_route",
                            onClick = {
                                val nearest = nodes.minByOrNull { n ->
                                    GeoProjection.haversineMeters(tappedGeo, LatLngPoint(n.latitude, n.longitude))
                                }
                                if (nearest != null) {
                                    val dist = GeoProjection.haversineMeters(tappedGeo, LatLngPoint(nearest.latitude, nearest.longitude))
                                    nearestRoute = NearestRoutePreview(from = tappedGeo, targetNode = nearest, distanceMeters = dist)
                                }
                                contextLatLng = null
                            }
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(horizontal = 16.dp))
                        ContextSheetRow(
                            icon = Icons.Rounded.Close,
                            label = "Tutup",
                            testTag = "map_ctx_close",
                            onClick = { contextLatLng = null }
                        )
                    }
                }
            }
        }
    }

    // Modal Tambah Distribusi (Gambar 4 & 5)
    addDistLatLng?.let { targetGeo ->
        TambahDistribusiDialog(
            existingNodes = nodes,
            initialLat = targetGeo.lat,
            initialLng = targetGeo.lng,
            onDismiss = { addDistLatLng = null },
            onSubmit = { type, name, code, limit, extra, addr, desc, parentId, lat, lng ->
                onAddNode(type, name, code, limit, extra, addr, desc, parentId, lat, lng)
                centerLat = lat
                centerLng = lng
                addDistLatLng = null
            }
        )
    }

    // Save Cable Route Dialog
    if (showSaveCableDialog) {
        val totalM = GeoProjection.totalRouteMeters(draftCablePoints)
        SaveCableRouteDialog(
            pointCount = draftCablePoints.size,
            totalMeters = totalM,
            onDismiss = { showSaveCableDialog = false },
            onSave = { routeName, coreCount ->
                onAddCableRoute(routeName, GeoProjection.encodeRoutePoints(draftCablePoints), totalM, coreCount)
                showSaveCableDialog = false
                isDrawingCable = false
                draftCablePoints = emptyList()
            }
        )
    }

    // Node Inspection + Pin Customer shortcut
    selectedNode?.let { node ->
        val linkedCount = customerPins.count { it.parentNodeId == node.id }
        val parentCode = node.parentId?.let { pid -> nodes.firstOrNull { it.id == pid }?.code }
        NodeDetailSheetDialog(
            node = node,
            linkedCustomers = linkedCount,
            parentCode = parentCode,
            onDismiss = { selectedNode = null },
            onAddCustomerPin = {
                pinForNode = node
                selectedNode = null
            },
            onDeleteNode = {
                onDeleteNode(node)
                selectedNode = null
            }
        )
    }

    pinForNode?.let { targetNode ->
        AddCustomerPinDialog(
            targetNode = targetNode,
            pppoeClients = uiState.allPppoeClients,
            onDismiss = { pinForNode = null },
            onSubmit = { custName, pppoeUser, lat, lng, addr ->
                onAddCustomerPin(custName, pppoeUser, targetNode.id, lat, lng, addr)
                pinForNode = null
            }
        )
    }

    selectedCustomerPin?.let { item ->
        AlertDialog(
            onDismissRequest = { selectedCustomerPin = null },
            title = { Text(item.pin.name, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("PPPoE: ${item.pin.pppoeUsername.ifBlank { "-" }}", fontFamily = JetBrainsMonoFamily, fontSize = 12.sp)
                    Text("Status: ${item.status.name}")
                    if (item.pin.address.isNotBlank()) Text("Alamat: ${item.pin.address}")
                    Text(
                        text = String.format(Locale.US, "Koordinat: %.6f, %.6f", item.pin.latitude, item.pin.longitude),
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 11.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedCustomerPin = null }) { Text("Tutup") }
            },
            dismissButton = {
                TextButton(onClick = {
                    onDeleteCustomerPin(item.pin)
                    selectedCustomerPin = null
                }) {
                    Text("Hapus Pin", color = NocRedOffline)
                }
            }
        )
    }

    if (showLegendHelp) {
        AlertDialog(
            onDismissRequest = { showLegendHelp = false },
            title = { Text("Panduan Maps & FTTH") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Tekan & tahan di area peta untuk membuka menu Tambah distribusi, Gambar rute kabel, atau Cari rute terdekat.")
                    Text("• Ketuk ikon ODP/ODC di peta untuk melihat kapasitas port atau menempelkan pin pelanggan ke node tersebut.")
                    Text("• Buka Kontrol Peta (ikon slider) untuk mengganti basemap (Satelit, Satelit polos, OSM, Gelap) atau mengaktifkan mode Geser marker.")
                    if (cableRoutes.isNotEmpty()) {
                        HorizontalDivider()
                        Text("Rute kabel tersimpan (${cableRoutes.size}):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        LazyColumn(modifier = Modifier.heightIn(max = 120.dp)) {
                            items(cableRoutes, key = { it.id }) { route ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${route.name} (${route.lengthMeters}m • ${route.coreCount}C)", fontSize = 12.sp)
                                    Text(
                                        text = "Hapus",
                                        color = NocRedOffline,
                                        fontSize = 11.sp,
                                        modifier = Modifier.clickable { onDeleteCableRoute(route) }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLegendHelp = false }) { Text("Mengerti") }
            }
        )
    }
}

@Composable
private fun MapTopIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    isActive: Boolean = false,
    testTag: String
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isActive) NocAmber.copy(alpha = 0.22f)
                else Color(0xFF1B212C)
            )
            .border(
                1.dp,
                if (isActive) NocAmber else Color.White.copy(alpha = 0.1f),
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isActive) NocAmber else Color.White,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun LegendDotItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(text = label, color = Color(0xFFE2E8F0), fontSize = 11.sp)
    }
}

@Composable
private fun ContextSheetRow(
    icon: ImageVector,
    label: String,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color(0xFFCBD5E1),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MapControlPopover(
    basemap: FtthBasemap,
    showCustomer: Boolean,
    showDistribution: Boolean,
    showCustomerLine: Boolean,
    showDistributionLine: Boolean,
    showCableRoute: Boolean,
    dragMarker: Boolean,
    onSelectBasemap: (FtthBasemap) -> Unit,
    onToggleCustomer: () -> Unit,
    onToggleDistribution: () -> Unit,
    onToggleCustomerLine: () -> Unit,
    onToggleDistributionLine: () -> Unit,
    onToggleCableRoute: () -> Unit,
    onToggleDragMarker: () -> Unit,
    onFitAllMarkers: () -> Unit,
    onClose: () -> Unit
) {
    Surface(
        modifier = Modifier
            .widthIn(min = 270.dp, max = 305.dp)
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xF5141923),
        shadowElevation = 10.dp
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
                Text(
                    text = "Kontrol Peta",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1F2735))
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Tutup", tint = Color(0xFF94A3B8), modifier = Modifier.size(15.dp))
                }
            }

            Text(
                text = "BASEMAP",
                color = Color(0xFF94A3B8),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FtthBasemap.entries.forEach { bm ->
                    val selected = basemap == bm
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (selected) NocAmber.copy(alpha = 0.14f) else Color(0xFF1B222E))
                            .border(
                                1.dp,
                                if (selected) NocAmber else Color.White.copy(alpha = 0.12f),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { onSelectBasemap(bm) }
                            .padding(horizontal = 11.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = bm.label,
                            color = if (selected) NocAmber else Color(0xFFE2E8F0),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Text(
                text = "LAYER",
                color = Color(0xFF94A3B8),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )

            LayerToggleRow(icon = Icons.Outlined.Group, label = "Customer", checked = showCustomer, onClick = onToggleCustomer)
            LayerToggleRow(icon = Icons.Outlined.Apartment, label = "Distribusi", checked = showDistribution, onClick = onToggleDistribution)
            LayerToggleRow(icon = Icons.Outlined.Link, label = "Line customer", checked = showCustomerLine, onClick = onToggleCustomerLine)
            LayerToggleRow(icon = Icons.Outlined.Link, label = "Line distribusi", checked = showDistributionLine, onClick = onToggleDistributionLine)
            LayerToggleRow(icon = Icons.Rounded.Timeline, label = "Rute kabel", checked = showCableRoute, onClick = onToggleCableRoute)

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            LayerToggleRow(icon = Icons.Rounded.OpenWith, label = "Geser marker", checked = dragMarker, onClick = onToggleDragMarker)

            OutlinedButton(
                onClick = onFitAllMarkers,
                modifier = Modifier.fillMaxWidth().height(40.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NocAmber.copy(alpha = 0.7f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NocAmber)
            ) {
                Icon(Icons.Rounded.FitScreen, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Fit semua marker", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun LayerToggleRow(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (checked) NocAmber.copy(alpha = 0.09f) else Color(0xFF171D28))
            .border(
                1.dp,
                if (checked) NocAmber.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.07f),
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (checked) NocAmber else Color(0xFF94A3B8),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(if (checked) NocAmber else Color.Transparent)
                .border(
                    1.5.dp,
                    if (checked) NocAmber else Color(0xFF64748B),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Color(0xFF0B0E14),
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
