package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.PeopleOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CustomerEntity
import com.example.data.model.FtthNodeEntity
import com.example.data.model.InternetPackageEntity
import com.example.data.model.RouterConfig
import com.example.data.model.RouterTelemetry
import com.example.data.model.SalesRepEntity
import com.example.ui.theme.NocAmber
import com.example.ui.theme.NocEmeraldOnline
import com.example.ui.theme.NocRedOffline

private enum class CustomerFilter(val label: String) {
    ALL("Semua"),
    AKTIF("Aktif"),
    MENUNGGU("Menunggu"),
    NONAKTIF("Nonaktif"),
    BELUM_SYNC("Belum Sync")
}

@Composable
fun PppoeScreen(
    customers: List<CustomerEntity>,
    routers: List<RouterConfig>,
    telemetryByRouter: Map<Long, RouterTelemetry>,
    packages: List<InternetPackageEntity>,
    ftthNodes: List<FtthNodeEntity>,
    salesReps: List<SalesRepEntity>,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onSaveCustomer: (CustomerEntity) -> Unit,
    onToggleCustomerStatus: (CustomerEntity) -> Unit,
    onDeleteCustomer: (CustomerEntity) -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    var showFilterBar by remember { mutableStateOf(false) }
    var activeFilter by remember { mutableStateOf(CustomerFilter.ALL) }
    var showWizard by remember { mutableStateOf(false) }
    var editingCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var customerToDelete by remember { mutableStateOf<CustomerEntity?>(null) }

    if (showWizard) {
        PelangganWizardScreen(
            initialCustomer = editingCustomer,
            existingCustomers = customers,
            ftthNodes = ftthNodes,
            routers = routers,
            telemetryByRouter = telemetryByRouter,
            packages = packages,
            salesReps = salesReps,
            darkTheme = darkTheme,
            onToggleTheme = onToggleTheme,
            onSave = { saved ->
                onSaveCustomer(saved)
                showWizard = false
                editingCustomer = null
            },
            onCancel = {
                showWizard = false
                editingCustomer = null
            }
        )
        return
    }

    val totalCount = customers.size
    val activeCount = customers.count { it.statusLabel.equals("Aktif", ignoreCase = true) && it.isActive }
    val pppoeCount = customers.count { it.connectionModel.equals("PPPoE", ignoreCase = true) }
    val unsyncedCount = customers.count { !it.isSyncedToRouter }

    val filtered = remember(customers, query, activeFilter) {
        val q = query.trim().lowercase()
        customers.filter { c ->
            val matchFilter = when (activeFilter) {
                CustomerFilter.ALL -> true
                CustomerFilter.AKTIF -> c.statusLabel.equals("Aktif", ignoreCase = true) && c.isActive
                CustomerFilter.MENUNGGU -> c.statusLabel.equals("Menunggu", ignoreCase = true)
                CustomerFilter.NONAKTIF -> !c.isActive || c.statusLabel.equals("Nonaktif", ignoreCase = true)
                CustomerFilter.BELUM_SYNC -> !c.isSyncedToRouter
            }
            val matchQuery = q.isEmpty() ||
                c.name.lowercase().contains(q) ||
                c.pppoeUsername.lowercase().contains(q) ||
                c.address.lowercase().contains(q) ||
                c.phone.lowercase().contains(q) ||
                c.distributionLabel.lowercase().contains(q) ||
                c.packageName.lowercase().contains(q) ||
                c.customerCode.lowercase().contains(q)
            matchFilter && matchQuery
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingCustomer = null
                    showWizard = true
                },
                shape = CircleShape,
                containerColor = NocAmber,
                contentColor = Color(0xFF0B0E14),
                modifier = Modifier.testTag("fab_add_customer")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Tambah Pelanggan",
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header: < Pelanggan + Theme Toggle (Gambar 1)
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
                                .testTag("pppoe_back_button"),
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
                        text = "Pelanggan",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                        .clickable(onClick = onToggleTheme)
                        .testTag("pelanggan_theme_toggle"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (darkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                        contentDescription = "Ubah tema",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 92.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 2x2 KPI Grid matching Gambar 1
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            PelangganKpiCard(
                                value = totalCount.toString(),
                                label = "TOTAL",
                                valueColor = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            PelangganKpiCard(
                                value = activeCount.toString(),
                                label = "AKTIF",
                                valueColor = NocEmeraldOnline,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            PelangganKpiCard(
                                value = pppoeCount.toString(),
                                label = "PPPOE",
                                valueColor = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            PelangganKpiCard(
                                value = unsyncedCount.toString(),
                                label = "BELUM SYNC",
                                valueColor = if (unsyncedCount > 0) NocAmber else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Search bar + Filter icon button matching Gambar 1
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
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
                                Spacer(modifier = Modifier.width(10.dp))
                                Box(modifier = Modifier.weight(1f)) {
                                    if (query.isEmpty()) {
                                        Text(
                                            text = "Cari nama / secret / alamat...",
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    BasicTextField(
                                        value = query,
                                        onValueChange = { query = it },
                                        singleLine = true,
                                        textStyle = TextStyle(
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 14.sp
                                        ),
                                        cursorBrush = SolidColor(NocAmber),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("pppoe_search_input")
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (showFilterBar || activeFilter != CustomerFilter.ALL) NocAmber.copy(alpha = 0.16f)
                                    else MaterialTheme.colorScheme.surface
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (showFilterBar || activeFilter != CustomerFilter.ALL) NocAmber
                                    else MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { showFilterBar = !showFilterBar }
                                .testTag("pelanggan_filter_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Tune,
                                contentDescription = "Filter pelanggan",
                                tint = NocAmber,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                if (showFilterBar) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CustomerFilter.entries.forEach { cf ->
                                FilterChip(
                                    selected = activeFilter == cf,
                                    onClick = { activeFilter = cf },
                                    label = { Text(cf.label) }
                                )
                            }
                        }
                    }
                }

                // Count header ("3 dari 3 pelanggan")
                item {
                    Text(
                        text = "${filtered.size} dari $totalCount pelanggan",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (customers.isEmpty()) {
                    // Empty state strictly shown before any customer is added
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(NocAmber.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PeopleOutline,
                                        contentDescription = null,
                                        tint = NocAmber,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Text(
                                    text = "Belum Ada Pelanggan",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Tekan tombol + di kanan bawah untuk menambahkan pelanggan baru beserta lokasi, distribusi, dan akun PPPoE MikroTik.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else if (filtered.isEmpty()) {
                    item {
                        Text(
                            text = "Tidak ada pelanggan yang sesuai dengan filter atau pencarian.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 20.dp)
                        )
                    }
                } else {
                    items(filtered, key = { it.id }) { customer ->
                        PelangganListCard(
                            customer = customer,
                            onEdit = {
                                editingCustomer = customer
                                showWizard = true
                            },
                            onToggleStatus = { onToggleCustomerStatus(customer) },
                            onDelete = { customerToDelete = customer }
                        )
                    }
                }
            }
        }
    }

    customerToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { customerToDelete = null },
            title = { Text("Hapus Pelanggan?") },
            text = { Text("Data pelanggan ${target.name} akan dihapus permanen dari daftar.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCustomer(target)
                        customerToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NocRedOffline)
                ) {
                    Text("Hapus", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { customerToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun PelangganKpiCard(
    value: String,
    label: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PelangganListCard(
    customer: CustomerEntity,
    onEdit: () -> Unit,
    onToggleStatus: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val statusUpper = when {
        !customer.isActive || customer.statusLabel.equals("Nonaktif", ignoreCase = true) -> "NONAKTIF"
        customer.statusLabel.equals("Menunggu", ignoreCase = true) -> "MENUNGGU"
        else -> "AKTIF"
    }
    val statusColor = when (statusUpper) {
        "AKTIF" -> NocEmeraldOnline
        "MENUNGGU" -> NocAmber
        else -> NocRedOffline
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .clickable(onClick = onEdit)
            .testTag("customer_card_${customer.id}"),
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
                Text(
                    text = customer.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = statusColor.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = statusUpper,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }

                    Box {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { menuExpanded = true }
                                .testTag("customer_menu_${customer.id}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreHoriz,
                                contentDescription = "Opsi pelanggan",
                                tint = NocAmber,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit Pelanggan") },
                                onClick = {
                                    menuExpanded = false
                                    onEdit()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (customer.isActive) "Nonaktifkan / Isolir" else "Aktifkan") },
                                onClick = {
                                    menuExpanded = false
                                    onToggleStatus()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Hapus Pelanggan", color = NocRedOffline) },
                                onClick = {
                                    menuExpanded = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }

            // Chips Row matching Gambar 1
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (customer.packageName.isNotBlank()) {
                    PelangganMetaPill(
                        icon = Icons.Outlined.Inventory2,
                        prefix = null,
                        value = customer.packageName
                    )
                }
                if (customer.phone.isNotBlank()) {
                    PelangganMetaPill(
                        icon = Icons.Outlined.Phone,
                        prefix = null,
                        value = customer.phone
                    )
                }
                if (customer.pppoeUsername.isNotBlank()) {
                    PelangganMetaPill(
                        icon = Icons.Rounded.Wifi,
                        prefix = "PPPOE",
                        value = customer.pppoeUsername
                    )
                }
                val distOrServer = customer.distributionLabel.ifBlank { customer.routerName }
                if (distOrServer.isNotBlank()) {
                    PelangganMetaPill(
                        icon = Icons.Rounded.AccountTree,
                        prefix = null,
                        value = distOrServer
                    )
                }
            }
        }
    }
}

@Composable
private fun PelangganMetaPill(
    icon: ImageVector,
    prefix: String?,
    value: String
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier.border(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
            RoundedCornerShape(10.dp)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NocAmber,
                modifier = Modifier.size(13.dp)
            )
            if (!prefix.isNullOrBlank()) {
                Text(
                    text = prefix,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
