package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CustomerEntity
import com.example.data.model.IpPoolInfo
import com.example.data.model.PppProfileInfo
import com.example.data.model.PppSecretInfo
import com.example.data.model.RouterConfig
import com.example.data.model.RouterTelemetry
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.NocAmber
import com.example.ui.theme.NocBlueDownload
import com.example.ui.theme.NocEmeraldOnline
import com.example.ui.theme.NocPurpleAccent
import com.example.ui.theme.NocRedOffline
import com.example.util.Formatters

private enum class RouterDetailTab(val label: String) {
    RESOURCE("Resource"),
    SECRET("Secret"),
    PROFILE("Profile"),
    POOL("Pool")
}

private enum class SecretFilter(val label: String) {
    ALL("Semua"),
    ACTIVE("Active"),
    OFFLINE("Offline")
}

@Composable
fun RouterDetailScreen(
    router: RouterConfig?,
    telemetry: RouterTelemetry?,
    customers: List<CustomerEntity>,
    isRefreshing: Boolean,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onEditRouter: (RouterConfig) -> Unit,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onToggleSecretDisabled: (RouterConfig, PppSecretInfo) -> Unit,
    onRebootRouter: (RouterConfig) -> Unit
) {
    var activeTab by remember { mutableStateOf(RouterDetailTab.RESOURCE) }
    var customerSearch by remember { mutableStateOf("") }
    var secretSearch by remember { mutableStateOf("") }
    var secretFilter by remember { mutableStateOf(SecretFilter.ALL) }
    var profileSearch by remember { mutableStateOf("") }
    var poolSearch by remember { mutableStateOf("") }
    var showRebootDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (router == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Server MikroTik tidak ditemukan.")
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(onClick = onBack) { Text("Kembali") }
            }
            return@Scaffold
        }

        val isOnline = telemetry?.isOnline == true
        val statusColor = if (isOnline) NocEmeraldOnline else NocRedOffline
        val statusText = if (isOnline) "Terhubung" else "Terputus"
        val rosMajor = telemetry?.rosVersion?.takeIf { it.isNotBlank() && it != "-" }
            ?.substringBefore(".")?.toIntOrNull() ?: router.routerOsMajorVersion
        val identityText = telemetry?.identity?.ifBlank { router.username } ?: router.username

        // Customers linked from Menu Pelanggan (empty when Menu Pelanggan has not been filled!)
        val routerCustomers = remember(customers, router) {
            customers.filter { c ->
                c.routerId == router.id ||
                    (c.routerId == 0L && (c.routerName.isBlank() || c.routerName.equals(router.name, ignoreCase = true)))
            }
        }
        val filteredCustomers = remember(routerCustomers, customerSearch) {
            val q = customerSearch.trim().lowercase()
            if (q.isEmpty()) routerCustomers
            else routerCustomers.filter {
                it.name.lowercase().contains(q) ||
                    it.pppoeUsername.lowercase().contains(q) ||
                    it.customerCode.lowercase().contains(q) ||
                    it.ispLabel.lowercase().contains(q)
            }
        }

        val secrets = telemetry?.pppSecrets.orEmpty()
        val filteredSecrets = remember(secrets, secretSearch, secretFilter) {
            val q = secretSearch.trim().lowercase()
            secrets.filter { s ->
                val matchStatus = when (secretFilter) {
                    SecretFilter.ALL -> true
                    SecretFilter.ACTIVE -> s.isOnline
                    SecretFilter.OFFLINE -> !s.isOnline
                }
                val matchQuery = q.isEmpty() ||
                    s.name.lowercase().contains(q) ||
                    s.profile.lowercase().contains(q) ||
                    s.activeAddress.lowercase().contains(q) ||
                    s.remoteAddress.lowercase().contains(q)
                matchStatus && matchQuery
            }
        }

        val profiles = telemetry?.pppProfiles.orEmpty()
        val filteredProfiles = remember(profiles, profileSearch) {
            val q = profileSearch.trim().lowercase()
            if (q.isEmpty()) profiles
            else profiles.filter {
                it.name.lowercase().contains(q) ||
                    it.rateLimit.lowercase().contains(q) ||
                    it.remoteAddress.lowercase().contains(q)
            }
        }

        val pools = telemetry?.ipPools.orEmpty()
        val filteredPools = remember(pools, poolSearch) {
            val q = poolSearch.trim().lowercase()
            if (q.isEmpty()) pools
            else pools.filter {
                it.name.lowercase().contains(q) || it.ranges.lowercase().contains(q)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header matching Gambar 3 (< nugroho server | Edit | Theme Toggle)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
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
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                            .clickable(onClick = onBack)
                            .testTag("detail_back_button"),
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
                    Text(
                        text = router.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                            .clickable { onEditRouter(router) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit Server",
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
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Connection Status Strip + Periksa button
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(statusColor.copy(alpha = 0.16f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = statusText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = statusColor
                                )
                            }
                            Text(
                                text = "${router.host}:${router.port}",
                                fontFamily = JetBrainsMonoFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "ROS v$rosMajor · $identityText",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        OutlinedButton(
                            onClick = onRefresh,
                            enabled = !isRefreshing,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, NocAmber.copy(alpha = 0.55f)),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            modifier = Modifier
                                .height(40.dp)
                                .testTag("router_periksa_button")
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(15.dp),
                                    strokeWidth = 2.dp,
                                    color = NocAmber
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = null,
                                    tint = NocAmber,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Periksa",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = NocAmber
                            )
                        }
                    }
                }

                // 4-Tab Bar matching Gambar 3 (Resource | Secret | Profile | Pool)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RouterDetailTab.entries.forEach { tab ->
                            val selected = activeTab == tab
                            val countSuffix = when (tab) {
                                RouterDetailTab.RESOURCE -> ""
                                RouterDetailTab.SECRET -> if (secrets.isNotEmpty()) " (${secrets.size})" else ""
                                RouterDetailTab.PROFILE -> if (profiles.isNotEmpty()) " (${profiles.size})" else ""
                                RouterDetailTab.POOL -> if (pools.isNotEmpty()) " (${pools.size})" else ""
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (selected) NocAmber.copy(alpha = 0.16f) else Color.Transparent
                                    )
                                    .border(
                                        width = if (selected) 1.dp else 0.dp,
                                        color = if (selected) NocAmber.copy(alpha = 0.45f) else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { activeTab = tab }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                                    .testTag("tab_${tab.name.lowercase()}")
                            ) {
                                Text(
                                    text = "${tab.label}$countSuffix",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) NocAmber else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                when (activeTab) {
                    RouterDetailTab.RESOURCE -> {
                        // Resource Card matching Gambar 3
                        item {
                            ResourceSummaryCard(
                                isOnline = isOnline,
                                telemetry = telemetry,
                                onReboot = { showRebootDialog = true }
                            )
                        }

                        // Pelanggan Card matching Gambar 3 (empty if Menu Pelanggan hasn't been filled!)
                        item {
                            PelangganLinkedCard(
                                customers = filteredCustomers,
                                totalCount = routerCustomers.size,
                                searchQuery = customerSearch,
                                onSearchChange = { customerSearch = it },
                                telemetry = telemetry
                            )
                        }
                    }

                    RouterDetailTab.SECRET -> {
                        item {
                            RouterTabSearchBar(
                                query = secretSearch,
                                placeholder = "Cari secret / profile / IP...",
                                onQueryChange = { secretSearch = it }
                            )
                        }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SecretFilter.entries.forEach { sf ->
                                    val count = when (sf) {
                                        SecretFilter.ALL -> secrets.size
                                        SecretFilter.ACTIVE -> secrets.count { it.isOnline }
                                        SecretFilter.OFFLINE -> secrets.count { !it.isOnline }
                                    }
                                    FilterChip(
                                        selected = secretFilter == sf,
                                        onClick = { secretFilter = sf },
                                        label = { Text("${sf.label} ($count)") }
                                    )
                                }
                            }
                        }
                        if (!isOnline && secrets.isEmpty()) {
                            item {
                                EmptySyncBanner(
                                    title = "Belum Tersinkron",
                                    subtitle = "Tekan Periksa untuk menyinkronkan PPP Secret dari MikroTik yang terhubung.",
                                    onSyncClick = onRefresh
                                )
                            }
                        } else if (filteredSecrets.isEmpty()) {
                            item {
                                EmptySyncBanner(
                                    title = "Secret Kosong",
                                    subtitle = if (secrets.isEmpty()) {
                                        "Router belum memiliki PPP Secret."
                                    } else {
                                        "Tidak ada PPP Secret yang cocok dengan filter."
                                    },
                                    onSyncClick = onRefresh
                                )
                            }
                        } else {
                            items(filteredSecrets, key = { it.id }) { secret ->
                                SecretRowCard(
                                    secret = secret,
                                    onToggleDisabled = { onToggleSecretDisabled(router, secret) }
                                )
                            }
                        }
                    }

                    RouterDetailTab.PROFILE -> {
                        item {
                            RouterTabSearchBar(
                                query = profileSearch,
                                placeholder = "Cari nama profile / rate limit...",
                                onQueryChange = { profileSearch = it }
                            )
                        }
                        if (!isOnline && profiles.isEmpty()) {
                            item {
                                EmptySyncBanner(
                                    title = "Belum Tersinkron",
                                    subtitle = "Tekan Periksa untuk menyinkronkan PPP Profile dari MikroTik yang terhubung.",
                                    onSyncClick = onRefresh
                                )
                            }
                        } else if (filteredProfiles.isEmpty()) {
                            item {
                                EmptySyncBanner(
                                    title = "Profile Kosong",
                                    subtitle = "Tidak ada PPP Profile ditemukan di MikroTik.",
                                    onSyncClick = onRefresh
                                )
                            }
                        } else {
                            items(filteredProfiles, key = { it.id }) { prof ->
                                ProfileRowCard(prof)
                            }
                        }
                    }

                    RouterDetailTab.POOL -> {
                        item {
                            RouterTabSearchBar(
                                query = poolSearch,
                                placeholder = "Cari nama pool / range IP...",
                                onQueryChange = { poolSearch = it }
                            )
                        }
                        if (!isOnline && pools.isEmpty()) {
                            item {
                                EmptySyncBanner(
                                    title = "Belum Tersinkron",
                                    subtitle = "Tekan Periksa untuk menyinkronkan IP Pool dari MikroTik yang terhubung.",
                                    onSyncClick = onRefresh
                                )
                            }
                        } else if (filteredPools.isEmpty()) {
                            item {
                                EmptySyncBanner(
                                    title = "Pool Kosong",
                                    subtitle = "Router belum memiliki konfigurasi IP Pool (/ip/pool).",
                                    onSyncClick = onRefresh
                                )
                            }
                        } else {
                            items(filteredPools, key = { it.id }) { pool ->
                                PoolRowCard(pool)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRebootDialog && router != null) {
        AlertDialog(
            onDismissRequest = { showRebootDialog = false },
            title = { Text("Reboot ${router.name}?") },
            text = {
                Text("Semua sesi PPPoE dan trafik jaringan pada router ini akan terputus sementara selama proses restart.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRebootRouter(router)
                        showRebootDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NocRedOffline)
                ) {
                    Text("Ya, Reboot")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRebootDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun ResourceSummaryCard(
    isOnline: Boolean,
    telemetry: RouterTelemetry?,
    onReboot: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Resource",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (isOnline && telemetry != null) {
                    Text(
                        text = "Update ${Formatters.formatDotTime(telemetry.lastUpdatedMillis)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isOnline && telemetry != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularGaugeSubcard(
                        percent = telemetry.cpuLoadPercent,
                        title = "CPU",
                        subtitle = "${telemetry.cpuCount} core",
                        modifier = Modifier.weight(1f)
                    )
                    CircularGaugeSubcard(
                        percent = telemetry.memoryUsagePercent,
                        title = "Memori",
                        subtitle = "${Formatters.formatStorageSize(telemetry.usedMemoryBytes)} / ${Formatters.formatStorageSize(telemetry.totalMemoryBytes)}",
                        modifier = Modifier.weight(1f)
                    )
                    CircularGaugeSubcard(
                        percent = telemetry.storageUsagePercent,
                        title = "Storage",
                        subtitle = if (telemetry.totalHddBytes > 0L) {
                            "${Formatters.formatStorageSize(telemetry.usedHddBytes)} / ${Formatters.formatStorageSize(telemetry.totalHddBytes)}"
                        } else {
                            "-"
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SpecRow("Uptime", telemetry.uptime)
                    SpecRow("Version", telemetry.rosVersion)
                    SpecRow("Board", telemetry.boardName)
                    SpecRow("CPU", telemetry.cpuModel)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onReboot,
                        border = BorderStroke(1.dp, NocRedOffline.copy(alpha = 0.45f)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Rounded.PowerSettingsNew, contentDescription = null, tint = NocRedOffline, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reboot Router", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = NocRedOffline)
                    }
                }
            } else {
                Text(
                    text = telemetry?.errorMessage ?: "Router sedang terputus. Tekan Periksa untuk menyinkronkan resource dari MikroTik.",
                    style = MaterialTheme.typography.bodySmall,
                    color = NocRedOffline
                )
            }
        }
    }
}

@Composable
private fun CircularGaugeSubcard(
    percent: Int,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val safePct = percent.coerceIn(0, 100)
    val gaugeColor = when {
        safePct >= 85 -> NocRedOffline
        safePct >= 65 -> NocAmber
        else -> NocEmeraldOnline
    }
    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)

    Surface(
        modifier = modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f), RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier.size(72.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 6.dp.toPx()
                    val startAngle = 145f
                    val maxSweep = 250f
                    drawArc(
                        color = trackColor,
                        startAngle = startAngle,
                        sweepAngle = maxSweep,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    if (safePct > 0) {
                        drawArc(
                            color = gaugeColor,
                            startAngle = startAngle,
                            sweepAngle = maxSweep * (safePct / 100f),
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                }
                Text(
                    text = "$safePct%",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SpecRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PelangganLinkedCard(
    customers: List<CustomerEntity>,
    totalCount: Int,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    telemetry: RouterTelemetry?
) {
    val activeSecretNames = remember(telemetry) {
        telemetry?.pppSecrets?.filter { it.isOnline }?.map { it.name.lowercase() }?.toSet().orEmpty()
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
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
                Text(
                    text = "Pelanggan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(NocAmber.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = totalCount.toString(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NocAmber
                    )
                }
            }

            RouterTabSearchBar(
                query = searchQuery,
                placeholder = "Cari nama / username / identity...",
                onQueryChange = onSearchChange
            )

            if (totalCount == 0) {
                Text(
                    text = "Belum ada data pelanggan. Isi melalui menu Pelanggan untuk melihat daftar pelanggan di router ini.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (customers.isEmpty()) {
                Text(
                    text = "Tidak ada pelanggan yang cocok dengan pencarian \"$searchQuery\".",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    customers.forEach { customer ->
                        val isActive = activeSecretNames.contains(customer.pppoeUsername.lowercase())
                        val badgeColor = if (isActive) NocEmeraldOnline else NocRedOffline
                        val badgeLabel = if (isActive) "Active" else "Offline"

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f), RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(NocEmeraldOnline.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Person,
                                        contentDescription = null,
                                        tint = NocEmeraldOnline,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = customer.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = buildString {
                                            append(customer.pppoeUsername)
                                            if (customer.customerCode.isNotBlank()) {
                                                append(" · ${customer.customerCode}")
                                            }
                                        },
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (customer.ispLabel.isNotBlank()) {
                                        Text(
                                            text = customer.ispLabel.uppercase(),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(badgeColor.copy(alpha = 0.14f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(badgeColor))
                                    Text(badgeLabel, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = badgeColor)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RouterTabSearchBar(
    query: String,
    placeholder: String,
    onQueryChange: (String) -> Unit
) {
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
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(NocAmber.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Search, contentDescription = null, tint = NocAmber, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(NocAmber),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Rounded.Close, contentDescription = "Bersihkan", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(15.dp))
                }
            }
        }
    }
}

@Composable
private fun SecretRowCard(
    secret: PppSecretInfo,
    onToggleDisabled: () -> Unit
) {
    val badgeColor = when {
        secret.isDisabled -> NocAmber
        secret.isOnline -> NocEmeraldOnline
        else -> NocRedOffline
    }
    val badgeLabel = when {
        secret.isDisabled -> "Isolir"
        secret.isOnline -> "Active"
        else -> "Offline"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = secret.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Profile: ${secret.profile} · Service: ${secret.service}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeColor.copy(alpha = 0.14f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(badgeColor))
                    Text(badgeLabel, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = badgeColor)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    val ipShown = if (secret.isOnline && secret.activeAddress != "-") secret.activeAddress else secret.remoteAddress
                    Text(
                        text = "IP: $ipShown · MAC: ${secret.callerId}",
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (secret.isOnline && secret.activeUptime != "-") {
                        Text(
                            text = "Uptime: ${secret.activeUptime}",
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 11.sp,
                            color = NocEmeraldOnline
                        )
                    } else if (secret.lastLoggedOut.isNotBlank()) {
                        Text(
                            text = "Terakhir logout: ${secret.lastLoggedOut}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedButton(
                    onClick = onToggleDisabled,
                    border = BorderStroke(
                        1.dp,
                        if (secret.isDisabled) NocEmeraldOnline.copy(alpha = 0.5f) else NocAmber.copy(alpha = 0.5f)
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(
                        imageVector = if (secret.isDisabled) Icons.Rounded.CheckCircleOutline else Icons.Rounded.Block,
                        contentDescription = null,
                        tint = if (secret.isDisabled) NocEmeraldOnline else NocAmber,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (secret.isDisabled) "Aktifkan" else "Isolir",
                        fontSize = 11.sp,
                        color = if (secret.isDisabled) NocEmeraldOnline else NocAmber
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileRowCard(profile: PppProfileInfo) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = profile.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (profile.rateLimit != "-") {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(NocBlueDownload.copy(alpha = 0.14f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = profile.rateLimit,
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NocBlueDownload
                        )
                    }
                }
            }
            Text(
                text = "Local: ${profile.localAddress} · Remote/Pool: ${profile.remoteAddress}",
                fontFamily = JetBrainsMonoFamily,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (profile.comment.isNotBlank()) {
                Text(
                    text = profile.comment,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PoolRowCard(pool: IpPoolInfo) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = pool.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (pool.nextPool.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(NocPurpleAccent.copy(alpha = 0.14f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Next: ${pool.nextPool}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NocPurpleAccent
                        )
                    }
                }
            }
            Text(
                text = "Ranges: ${pool.ranges}",
                fontFamily = JetBrainsMonoFamily,
                fontSize = 12.sp,
                color = NocAmber
            )
        }
    }
}

@Composable
private fun EmptySyncBanner(
    title: String,
    subtitle: String,
    onSyncClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            OutlinedButton(onClick = onSyncClick) {
                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Periksa & Sinkronkan")
            }
        }
    }
}
