package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.InternetPackageEntity
import com.example.data.model.PppoeClient
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.NocAmber
import com.example.ui.theme.NocEmeraldOnline
import com.example.ui.theme.NocRedOffline
import com.example.util.Formatters
import java.util.Locale

@Composable
fun ProdukScreen(
    packages: List<InternetPackageEntity>,
    pppoeClients: List<PppoeClient>,
    isSyncing: Boolean,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onBack: () -> Unit,
    onSyncFromMikroTik: () -> Unit,
    onSavePackage: (InternetPackageEntity) -> Unit,
    onDeletePackage: (InternetPackageEntity) -> Unit
) {
    var editingPackage by remember { mutableStateOf<InternetPackageEntity?>(null) }
    var showFormScreen by remember { mutableStateOf(false) }

    if (showFormScreen) {
        BackHandler {
            showFormScreen = false
            editingPackage = null
        }
        TambahProdukFormScreen(
            initialPackage = editingPackage,
            isSyncing = isSyncing,
            darkTheme = darkTheme,
            onToggleTheme = onToggleTheme,
            onBack = {
                showFormScreen = false
                editingPackage = null
            },
            onSyncFromMikroTik = onSyncFromMikroTik,
            onSubmit = { pkg ->
                onSavePackage(pkg)
                showFormScreen = false
                editingPackage = null
            }
        )
        return
    }

    var searchQuery by remember { mutableStateOf("") }

    val totalProduk = packages.size
    val aktifProduk = packages.count { it.isActive }
    val pajakProduk = packages.count { it.usePpn }
    val totalPelanggan = pppoeClients.size

    val filtered = remember(packages, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) packages
        else packages.filter {
            it.name.lowercase().contains(q) ||
                it.pppoeProfile.lowercase().contains(q) ||
                it.poolName.lowercase().contains(q)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingPackage = null
                    showFormScreen = true
                },
                shape = CircleShape,
                containerColor = NocAmber,
                contentColor = Color(0xFF0B0E14),
                modifier = Modifier.testTag("produk_add_fab")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Tambah Produk", modifier = Modifier.size(26.dp))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header matching Gambar 1 (< Produk + Theme toggle)
            ProdukTopBar(
                title = "Produk",
                darkTheme = darkTheme,
                onToggleTheme = onToggleTheme,
                onBack = onBack,
                backTag = "produk_back_button"
            )

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
                            ProdukKpiCard(
                                count = totalProduk,
                                label = "PRODUK",
                                countColor = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            ProdukKpiCard(
                                count = aktifProduk,
                                label = "AKTIF",
                                countColor = NocEmeraldOnline,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ProdukKpiCard(
                                count = pajakProduk,
                                label = "PAJAK",
                                countColor = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            ProdukKpiCard(
                                count = totalPelanggan,
                                label = "PELANGGAN",
                                countColor = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Search bar matching Gambar 1
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
                                        text = "Cari nama / kode / pool...",
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
                                        .testTag("produk_search_input")
                                )
                            }
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Bersihkan", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "${filtered.size} produk",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Empty state before user adds any product
                if (packages.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Inventory2,
                                    contentDescription = null,
                                    tint = NocAmber,
                                    modifier = Modifier.size(34.dp)
                                )
                                Text(
                                    text = "Belum Ada Produk",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Tekan tombol + di kanan bawah untuk menambahkan produk paket internet PPPoE.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else if (filtered.isEmpty()) {
                    item {
                        EmptyModuleBanner(
                            title = "Produk Tidak Ditemukan",
                            subtitle = "Tidak ada produk yang cocok dengan pencarian \"$searchQuery\"."
                        )
                    }
                } else {
                    items(filtered, key = { it.id }) { pkg ->
                        val linkedCount = pppoeClients.count {
                            it.profile.equals(pkg.pppoeProfile, ignoreCase = true) ||
                                it.profile.equals(pkg.name, ignoreCase = true)
                        }
                        ProdukListCard(
                            pkg = pkg,
                            customerCount = linkedCount,
                            onEdit = {
                                editingPackage = pkg
                                showFormScreen = true
                            },
                            onDelete = { onDeletePackage(pkg) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProdukTopBar(
    title: String,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onBack: () -> Unit,
    backTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                    .clickable(onClick = onBack)
                    .testTag(backTag),
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
                text = title,
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

@Composable
private fun ProdukKpiCard(
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
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = count.toString(),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = countColor
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

@Composable
private fun ProdukListCard(
    pkg: InternetPackageEntity,
    customerCount: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val statusColor = if (pkg.isActive) NocEmeraldOnline else NocRedOffline
    val statusLabel = if (pkg.isActive) "Aktif" else "Nonaktif"
    val speedChip = if (pkg.downloadMbps > 0) "${pkg.downloadMbps} Mbps" else pkg.downloadLimitText

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
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
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(statusColor.copy(alpha = 0.14f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(statusColor))
                    Text(
                        text = statusLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Edit Produk", tint = NocAmber, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Hapus Produk", tint = NocRedOffline, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Text(
                text = pkg.name.uppercase(Locale.ROOT),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = Formatters.formatRupiah(pkg.monthlyPrice),
                fontFamily = JetBrainsMonoFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = NocAmber
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = speedChip,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$customerCount pelanggan",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (pkg.usePpn) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(NocAmber.copy(alpha = 0.14f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "PPN",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NocAmber
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// GAMBAR 2 & 3: TAMBAH / EDIT PRODUK SCREEN
// =========================================================================
@Composable
private fun TambahProdukFormScreen(
    initialPackage: InternetPackageEntity?,
    isSyncing: Boolean,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onBack: () -> Unit,
    onSyncFromMikroTik: () -> Unit,
    onSubmit: (InternetPackageEntity) -> Unit
) {
    val isEdit = initialPackage != null

    var name by remember { mutableStateOf(initialPackage?.name.orEmpty()) }
    var code by remember { mutableStateOf(initialPackage?.pppoeProfile.orEmpty()) }
    var isCodeManual by remember { mutableStateOf(initialPackage != null) }
    var priceDigits by remember { mutableStateOf(initialPackage?.monthlyPrice?.toString().orEmpty()) }

    var localAddress by remember { mutableStateOf(initialPackage?.localAddress ?: "192.168.1.1") }
    var poolName by remember { mutableStateOf(initialPackage?.poolName.orEmpty()) }
    val ranges = remember {
        mutableStateListOf<String>().apply {
            val saved = initialPackage?.poolRanges?.split(";")?.filter { it.isNotBlank() }.orEmpty()
            if (saved.isNotEmpty()) addAll(saved) else add("")
        }
    }

    var isAdvancedLimit by remember { mutableStateOf(initialPackage?.isAdvancedLimit ?: false) }
    var uploadLimit by remember { mutableStateOf(initialPackage?.uploadLimitText ?: "5M") }
    var downloadLimit by remember { mutableStateOf(initialPackage?.downloadLimitText ?: "5M") }

    var burstEnabled by remember { mutableStateOf(initialPackage?.burstEnabled ?: false) }
    var burstUpload by remember { mutableStateOf(initialPackage?.burstUpload.orEmpty()) }
    var burstDownload by remember { mutableStateOf(initialPackage?.burstDownload.orEmpty()) }
    var burstThreshold by remember { mutableStateOf(initialPackage?.burstThreshold.orEmpty()) }
    var burstTime by remember { mutableStateOf(initialPackage?.burstTime.orEmpty()) }

    var usePpn by remember { mutableStateOf(initialPackage?.usePpn ?: false) }
    var isActive by remember { mutableStateOf(initialPackage?.isActive ?: true) }
    var isVisible by remember { mutableStateOf(initialPackage?.isVisible ?: true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ProdukTopBar(
            title = if (isEdit) "Edit Produk" else "Tambah Produk",
            darkTheme = darkTheme,
            onToggleTheme = onToggleTheme,
            onBack = onBack,
            backTag = "tambah_produk_back"
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Lengkapi data paket layanan PPPoE dan konfigurasi jaringan.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Amber Warning Banner matching Gambar 2
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NocAmber.copy(alpha = 0.45f), RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                color = NocAmber.copy(alpha = 0.14f)
            ) {
                Text(
                    text = "KODE PRODUK sebagai PPP Profile — pastikan berbeda untuk setiap produk.",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NocAmber,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                )
            }

            // Section 1: Data Produk
            ProdukSectionCard(title = "Data Produk") {
                ProdukIconField(
                    icon = Icons.Outlined.Inventory2,
                    label = "Nama Produk",
                    value = name,
                    placeholder = "Paket Home 10 Mbps",
                    onValueChange = { newName ->
                        name = newName
                        if (!isCodeManual) {
                            code = newName
                                .trimStart()
                                .uppercase(Locale.ROOT)
                                .replace(Regex("\\s+"), "-")
                                .replace(Regex("[^A-Z0-9-]"), "")
                        }
                    },
                    testTag = "produk_name_input"
                )

                ProdukIconField(
                    icon = Icons.Outlined.LocalOffer,
                    label = "Kode Produk",
                    value = code,
                    placeholder = "PKT-10-MBPS",
                    onValueChange = { newCode ->
                        val upper = newCode.uppercase(Locale.ROOT)
                        code = upper
                        isCodeManual = upper.isNotBlank()
                    },
                    capitalization = KeyboardCapitalization.Characters,
                    useMono = true,
                    testTag = "produk_code_input"
                )

                Text(
                    text = "Dipakai sebagai nama PPP Profile",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 12.dp)
                )

                OutlinedButton(
                    onClick = onSyncFromMikroTik,
                    enabled = !isSyncing,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.2.dp, NocAmber),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NocAmber)
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = NocAmber)
                    } else {
                        Icon(Icons.Rounded.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sync Profile dari MikroTik", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                ProdukIconField(
                    icon = Icons.Outlined.Payments,
                    label = "Harga Produk",
                    value = priceDigits,
                    placeholder = "Rp 100.000",
                    onValueChange = { priceDigits = it.filter { c -> c.isDigit() } },
                    keyboardType = KeyboardType.Number,
                    useMono = true,
                    testTag = "produk_price_input"
                )
            }

            // Section 2: Jaringan & Pool
            ProdukSectionCard(title = "Jaringan & Pool") {
                ProdukIconField(
                    icon = Icons.Outlined.Language,
                    label = "Alamat Lokal",
                    value = localAddress,
                    placeholder = "192.168.1.1",
                    onValueChange = { localAddress = it },
                    useMono = true
                )

                ProdukIconField(
                    icon = Icons.Outlined.AccountTree,
                    label = "Pool Name",
                    value = poolName,
                    placeholder = "pool-home",
                    onValueChange = { poolName = it },
                    useMono = true
                )

                Text(
                    text = "Pool IP",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Format: IP_AWAL-IP_AKHIR. Contoh: 192.168.1.10-192.168.1.100",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ranges.forEachIndexed { idx, rangeVal ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ProdukPlainField(
                            label = "Range ${idx + 1}",
                            value = rangeVal,
                            placeholder = "192.168.1.2-192.168.1.254",
                            onValueChange = { ranges[idx] = it },
                            useMono = true,
                            modifier = Modifier.weight(1f)
                        )
                        if (idx == 0) {
                            IconButton(onClick = { ranges.add("") }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Rounded.Add, contentDescription = "Tambah range", tint = NocAmber)
                            }
                        } else {
                            IconButton(onClick = { ranges.removeAt(idx) }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Rounded.Close, contentDescription = "Hapus range", tint = NocRedOffline)
                            }
                        }
                    }
                }

                Text(
                    text = "+ Tambah range",
                    color = NocAmber,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { ranges.add("") }
                        .padding(vertical = 4.dp)
                )
            }

            // Section 3: Batas Kecepatan
            ProdukSectionCard(title = "Batas Kecepatan") {
                Text(
                    text = "Opsi limit",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ProdukToggleChip(
                        label = "Simpel",
                        selected = !isAdvancedLimit,
                        onClick = { isAdvancedLimit = false },
                        modifier = Modifier.weight(1f)
                    )
                    ProdukToggleChip(
                        label = "Advanced",
                        selected = isAdvancedLimit,
                        onClick = { isAdvancedLimit = true },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ProdukPlainField(
                        label = if (isAdvancedLimit) "Max Upload" else "Upload",
                        value = uploadLimit,
                        placeholder = "5M",
                        onValueChange = { uploadLimit = it.uppercase(Locale.ROOT) },
                        useMono = true,
                        modifier = Modifier.weight(1f)
                    )
                    ProdukPlainField(
                        label = if (isAdvancedLimit) "Max Download" else "Download",
                        value = downloadLimit,
                        placeholder = "5M",
                        onValueChange = { downloadLimit = it.uppercase(Locale.ROOT) },
                        useMono = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Burst Toggle Card matching Gambar 3
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Burst", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            text = "Aktifkan limit burst untuk upload/download",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = burstEnabled,
                        onCheckedChange = { burstEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = NocAmber)
                    )
                }

                AnimatedVisibility(visible = burstEnabled) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ProdukPlainField(
                                label = "Burst Upload",
                                value = burstUpload,
                                placeholder = "10M",
                                onValueChange = { burstUpload = it.uppercase(Locale.ROOT) },
                                useMono = true,
                                modifier = Modifier.weight(1f)
                            )
                            ProdukPlainField(
                                label = "Burst Download",
                                value = burstDownload,
                                placeholder = "10M",
                                onValueChange = { burstDownload = it.uppercase(Locale.ROOT) },
                                useMono = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ProdukPlainField(
                                label = "Burst Threshold",
                                value = burstThreshold,
                                placeholder = "4M/4M",
                                onValueChange = { burstThreshold = it.uppercase(Locale.ROOT) },
                                useMono = true,
                                modifier = Modifier.weight(1f)
                            )
                            ProdukPlainField(
                                label = "Burst Time",
                                value = burstTime,
                                placeholder = "8s/8s",
                                onValueChange = { burstTime = it },
                                useMono = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Text(
                    text = "Unit bandwidth harus huruf BESAR: K, M, G. Contoh: 5M, 100K, 1G",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Section 4: Pajak matching Gambar 3
            ProdukSectionCard(title = "Pajak") {
                Text("PPN", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ProdukToggleChip(
                        label = "Ya",
                        selected = usePpn,
                        onClick = { usePpn = true },
                        modifier = Modifier.weight(1f)
                    )
                    ProdukToggleChip(
                        label = "Tidak",
                        selected = !usePpn,
                        onClick = { usePpn = false },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Section 5: Status & Visibilitas matching Gambar 3
            ProdukSectionCard(title = "Status & Visibilitas") {
                ProdukSwitchRow(
                    title = "Produk Aktif",
                    subtitle = "Nonaktifkan jika produk sedang tidak tersedia",
                    checked = isActive,
                    onCheckedChange = { isActive = it }
                )
                ProdukSwitchRow(
                    title = "Tampilkan Produk",
                    subtitle = "Tampilkan saat pendaftaran pelanggan",
                    checked = isVisible,
                    onCheckedChange = { isVisible = it }
                )
            }

            val parsedPrice = priceDigits.toLongOrNull() ?: 100_000L
            val dlMbps = parseBandwidthToMbps(downloadLimit)
            val ulMbps = parseBandwidthToMbps(uploadLimit)
            val finalCode = code.trim().ifBlank {
                name.trim().uppercase(Locale.ROOT).replace(Regex("\\s+"), "-").replace(Regex("[^A-Z0-9-]"), "")
            }

            Button(
                onClick = {
                    val entity = InternetPackageEntity(
                        id = initialPackage?.id ?: 0L,
                        name = name.trim(),
                        pppoeProfile = finalCode.ifBlank { name.trim().uppercase(Locale.ROOT) },
                        downloadMbps = dlMbps,
                        uploadMbps = ulMbps,
                        monthlyPrice = parsedPrice,
                        description = if (poolName.isNotBlank()) "Pool: $poolName (${uploadLimit}/${downloadLimit})" else "Limit ${uploadLimit}/${downloadLimit}",
                        isActive = isActive,
                        localAddress = localAddress.trim().ifBlank { "192.168.1.1" },
                        poolName = poolName.trim(),
                        poolRanges = ranges.filter { it.isNotBlank() }.joinToString(";"),
                        isAdvancedLimit = isAdvancedLimit,
                        uploadLimitText = uploadLimit.ifBlank { "5M" },
                        downloadLimitText = downloadLimit.ifBlank { "5M" },
                        burstEnabled = burstEnabled,
                        burstUpload = burstUpload.trim(),
                        burstDownload = burstDownload.trim(),
                        burstThreshold = burstThreshold.trim(),
                        burstTime = burstTime.trim(),
                        usePpn = usePpn,
                        isVisible = isVisible
                    )
                    onSubmit(entity)
                },
                enabled = name.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("produk_submit_button"),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NocAmber,
                    contentColor = Color(0xFF0B0E14)
                )
            ) {
                Text(
                    text = if (isEdit) "Simpan Perubahan" else "Tambah Produk",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun parseBandwidthToMbps(raw: String): Int {
    val clean = raw.trim().uppercase(Locale.ROOT)
    val num = clean.takeWhile { it.isDigit() }.toIntOrNull() ?: return 5
    return when {
        clean.endsWith("G") -> num * 1000
        clean.endsWith("K") -> (num / 1000).coerceAtLeast(1)
        else -> num.coerceAtLeast(1)
    }
}

@Composable
private fun ProdukSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            content()
        }
    }
}

@Composable
private fun ProdukIconField(
    icon: ImageVector,
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    useMono: Boolean = false,
    testTag: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
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
            Icon(icon, contentDescription = null, tint = NocAmber, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                        fontFamily = if (useMono) JetBrainsMonoFamily else MaterialTheme.typography.bodyMedium.fontFamily
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType, capitalization = capitalization),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = if (useMono) JetBrainsMonoFamily else MaterialTheme.typography.bodyMedium.fontFamily
                    ),
                    cursorBrush = SolidColor(NocAmber),
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
                )
            }
        }
    }
}

@Composable
private fun ProdukPlainField(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    useMono: Boolean = false
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Box(modifier = Modifier.fillMaxWidth()) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    fontFamily = if (useMono) JetBrainsMonoFamily else MaterialTheme.typography.bodyMedium.fontFamily
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = if (useMono) JetBrainsMonoFamily else MaterialTheme.typography.bodyMedium.fontFamily
                ),
                cursorBrush = SolidColor(NocAmber),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ProdukToggleChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) NocAmber.copy(alpha = 0.14f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            )
            .border(
                1.dp,
                if (selected) NocAmber else MaterialTheme.colorScheme.outline,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) NocAmber else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ProdukSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = NocAmber)
        )
    }
}
