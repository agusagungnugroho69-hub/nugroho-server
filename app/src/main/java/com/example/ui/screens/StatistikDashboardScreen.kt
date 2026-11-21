package com.example.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.InternetPackageEntity
import com.example.data.model.InvoiceEntity
import com.example.data.model.InvoiceStatus
import com.example.data.model.NetworkAreaEntity
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.NocAmber
import com.example.ui.theme.NocBlueDownload
import com.example.ui.theme.NocEmeraldOnline
import com.example.ui.theme.NocPurpleAccent
import com.example.ui.theme.NocRedOffline
import com.example.util.Formatters
import com.example.viewmodel.DashboardUiState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private data class DonutSlice(
    val label: String,
    val count: Int,
    val color: Color
)

@Composable
fun StatistikDashboardScreen(
    uiState: DashboardUiState,
    invoices: List<InvoiceEntity>,
    packages: List<InternetPackageEntity>,
    areas: List<NetworkAreaEntity>,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Derived billing & subscriber analytics from live Room + MikroTik state
    val totalInvoices = invoices.size
    val paidInvoices = remember(invoices) { invoices.filter { it.status == InvoiceStatus.LUNAS } }
    val unpaidInvoices = remember(invoices) { invoices.filter { it.status == InvoiceStatus.BELUM_BAYAR } }
    val overdueInvoices = remember(invoices) { invoices.filter { it.status == InvoiceStatus.TERLAMBAT } }

    val totalInvoiceAmount = remember(invoices) { invoices.sumOf { it.amount } }
    val paidAmount = remember(paidInvoices) { paidInvoices.sumOf { it.amount } }
    val unpaidAmount = remember(unpaidInvoices) { unpaidInvoices.sumOf { it.amount } }
    val overdueAmount = remember(overdueInvoices) { overdueInvoices.sumOf { it.amount } }
    val pendingAmount = unpaidAmount + overdueAmount
    val pendingCount = unpaidInvoices.size + overdueInvoices.size

    val lunasPct = if (totalInvoices > 0) ((paidInvoices.size * 100) / totalInvoices) else 0
    val pendingPct = if (totalInvoices > 0) (100 - lunasPct) else 0
    val collectionPct = if (totalInvoiceAmount > 0L) ((paidAmount * 100L) / totalInvoiceAmount).toInt() else 0

    // Customer stats combining live PPPoE + fallback from invoices if no router connected
    val pppoeClients = uiState.allPppoeClients
    val activeCustomers = if (pppoeClients.isNotEmpty()) {
        pppoeClients.count { it.isOnline }
    } else {
        invoices.map { it.customerName }.distinct().size
    }
    val inactiveCustomers = if (pppoeClients.isNotEmpty()) {
        pppoeClients.count { !it.isOnline && !it.isDisabled }
    } else 0
    val suspendedCustomers = if (pppoeClients.isNotEmpty()) {
        pppoeClients.count { it.isDisabled }
    } else 0
    val totalCustomers = maxOf(activeCustomers + inactiveCustomers + suspendedCustomers, uiState.totalCustomers)

    // 12-month buckets ending current month
    val monthBuckets = remember(invoices, totalCustomers) {
        val cal = Calendar.getInstance()
        val labelFmt = SimpleDateFormat("MMM yyyy", Locale.ENGLISH)
        val keys = mutableListOf<Pair<String, Pair<Int, Int>>>()
        cal.add(Calendar.MONTH, -11)
        repeat(12) {
            keys.add(
                labelFmt.format(cal.time) to (cal.get(Calendar.YEAR) to cal.get(Calendar.MONTH))
            )
            cal.add(Calendar.MONTH, 1)
        }
        keys
    }

    val revenueByMonth = remember(monthBuckets, paidInvoices) {
        val cal = Calendar.getInstance()
        monthBuckets.map { (label, ym) ->
            val sum = paidInvoices.filter { inv ->
                cal.timeInMillis = inv.createdAt
                cal.get(Calendar.YEAR) == ym.first && cal.get(Calendar.MONTH) == ym.second
            }.sumOf { it.amount }
            label to sum
        }
    }

    val customerGrowthByMonth = remember(monthBuckets, totalCustomers) {
        monthBuckets.mapIndexed { idx, (label, _) ->
            val value = if (idx == monthBuckets.lastIndex) totalCustomers else 0
            label to value
        }
    }

    var invoiceScopeFilter by remember { mutableStateOf("Semua Tagihan") }
    var scopeDropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Header Bar matching Screenshot 1
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
                        .testTag("statistik_back_button"),
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
                    text = "Statistik Dashboard",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
                    contentDescription = "Ganti tema",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Top 4 Compact KPI Cards (Horizontal Scrollable row like Screenshot 1)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TopStatMiniCard(
                        icon = Icons.Outlined.Payments,
                        iconTint = Color(0xFF14B8A6),
                        pillText = "0.0%",
                        pillColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        label = "PENDAPATAN",
                        value = Formatters.formatRupiah(paidAmount),
                        subLabel = "vs bulan lalu",
                        highlightAmber = false
                    )
                    TopStatMiniCard(
                        icon = Icons.Outlined.Group,
                        iconTint = NocBlueDownload,
                        pillText = "▲ $activeCustomers baru",
                        pillColor = NocEmeraldOnline,
                        label = "PELANGGAN AKTIF",
                        value = activeCustomers.toString(),
                        subLabel = "dari $totalCustomers total",
                        highlightAmber = false
                    )
                    TopStatMiniCard(
                        icon = Icons.Outlined.CheckCircle,
                        iconTint = NocEmeraldOnline,
                        pillText = "▲ ${paidInvoices.size}",
                        pillColor = NocEmeraldOnline,
                        label = "TAGIHAN LUNAS",
                        value = "$lunasPct%",
                        subLabel = "dari total tagihan",
                        highlightAmber = false
                    )
                    TopStatMiniCard(
                        icon = Icons.Outlined.Schedule,
                        iconTint = NocAmber,
                        pillText = "▼ ${Formatters.formatRupiah(pendingAmount)}",
                        pillColor = NocAmber,
                        label = "TERTUNDA",
                        value = "$pendingPct%",
                        subLabel = "$pendingCount tagihan",
                        highlightAmber = true
                    )
                }
            }

            // 2. Tren Pendapatan (12 bulan terakhir)
            item {
                val currentMonthLabel = revenueByMonth.lastOrNull()?.first ?: "Nov 2026"
                val currentMonthVal = revenueByMonth.lastOrNull()?.second ?: 0L
                val avgRev = if (revenueByMonth.isNotEmpty()) paidAmount / revenueByMonth.size else 0L

                StatSectionCard(
                    icon = Icons.Outlined.Payments,
                    iconTint = Color(0xFF14B8A6),
                    title = "Tren Pendapatan",
                    subtitle = "12 bulan terakhir",
                    trailingText = "$currentMonthLabel - ${Formatters.formatRupiah(currentMonthVal)}"
                ) {
                    MonthlyTrendLineChart(
                        points = revenueByMonth.map { it.first to it.second.toFloat() },
                        lineColor = Color(0xFF10B981),
                        yFormatter = { v -> "Rp ${v.toLong()}" }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        InsetStatBox("TOTAL", Formatters.formatRupiah(paidAmount), Modifier.weight(1f))
                        InsetStatBox("RATA-RATA", Formatters.formatRupiah(avgRev), Modifier.weight(1f))
                    }
                }
            }

            // 3. Pertumbuhan Pelanggan
            item {
                val latestLabel = customerGrowthByMonth.lastOrNull()?.first ?: "Nov 2026"
                val latestVal = customerGrowthByMonth.lastOrNull()?.second ?: 0

                StatSectionCard(
                    icon = Icons.AutoMirrored.Rounded.TrendingUp,
                    iconTint = NocBlueDownload,
                    title = "Pertumbuhan Pelanggan",
                    subtitle = "Pelanggan baru per bulan, 12 bulan terakhir",
                    trailingText = "$latestLabel - $latestVal"
                ) {
                    MonthlyTrendLineChart(
                        points = customerGrowthByMonth.map { it.first to it.second.toFloat() },
                        lineColor = NocBlueDownload,
                        yFormatter = { v -> v.toInt().toString() },
                        showOnlyLastPointIfZero = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        InsetStatBox("TOTAL BARU", totalCustomers.toString(), Modifier.weight(1f))
                        InsetStatBox("RATA-RATA", totalCustomers.toString(), Modifier.weight(1f))
                    }
                }
            }

            // 4. Tagihan Bulan Ini
            item {
                StatSectionCard(
                    icon = Icons.Outlined.ReceiptLong,
                    iconTint = NocBlueDownload,
                    title = "Tagihan Bulan Ini",
                    subtitle = "Kolektibilitas dan rincian nilai tagihan"
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularScoreGauge(
                            percent = collectionPct,
                            ringColor = NocEmeraldOnline,
                            title = "Tingkat koleksi",
                            subtitle = "${Formatters.formatRupiah(paidAmount)} dari ${Formatters.formatRupiah(totalInvoiceAmount)}",
                            modifier = Modifier.width(126.dp)
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                InsetStatBox("TOTAL", Formatters.formatRupiah(totalInvoiceAmount), Modifier.weight(1f))
                                InsetStatBox("TERBAYAR", Formatters.formatRupiah(paidAmount), Modifier.weight(1f), valueColor = NocEmeraldOnline)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                InsetStatBox("BELUM DIBAYAR", Formatters.formatRupiah(unpaidAmount), Modifier.weight(1f), valueColor = NocAmber)
                                InsetStatBox("TERLAMBAT", Formatters.formatRupiah(overdueAmount), Modifier.weight(1f), valueColor = NocRedOffline)
                            }
                        }
                    }
                }
            }

            // 5. Ringkasan Keuangan
            item {
                StatSectionCard(
                    icon = Icons.Outlined.AccountBalanceWallet,
                    iconTint = Color(0xFFEC4899),
                    title = "Ringkasan Keuangan",
                    subtitle = "Performa keseluruhan tagihan"
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularScoreGauge(
                            percent = lunasPct,
                            ringColor = Color(0xFFEF4444),
                            title = "Tagihan terbayar",
                            subtitle = "${paidInvoices.size} / $totalInvoices tagihan",
                            modifier = Modifier.width(126.dp)
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                InsetStatBox("TOTAL PENDAPATAN", Formatters.formatRupiah(paidAmount), Modifier.weight(1f), valueColor = NocEmeraldOnline)
                                InsetStatBox(
                                    "TERBAYAR",
                                    paidInvoices.size.toString(),
                                    Modifier.weight(1f),
                                    footnote = "dari $totalInvoices tagihan"
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                InsetStatBox(
                                    "BELUM DIBAYAR",
                                    Formatters.formatRupiah(unpaidAmount),
                                    Modifier.weight(1f),
                                    valueColor = NocAmber,
                                    footnote = "${unpaidInvoices.size} tagihan menunggu"
                                )
                                InsetStatBox(
                                    "TERLAMBAT",
                                    overdueInvoices.size.toString(),
                                    Modifier.weight(1f),
                                    valueColor = NocRedOffline,
                                    footnote = "lewat jatuh tempo"
                                )
                            }
                        }
                    }
                }
            }

            // 6. Status Tagihan (Donut + Filter + 3 Bottom Colored Cards)
            item {
                val slices = listOf(
                    DonutSlice("Terbayar", paidInvoices.size, NocEmeraldOnline),
                    DonutSlice("Belum Dibayar", unpaidInvoices.size, NocAmber),
                    DonutSlice("Terlambat", overdueInvoices.size, NocRedOffline)
                )
                val centerTitle = if (overdueInvoices.isNotEmpty()) "Terlambat" else "Total"
                val centerCount = totalInvoices

                StatSectionCard(
                    icon = Icons.Outlined.CreditCard,
                    iconTint = NocAmber,
                    title = "Status Tagihan",
                    subtitle = "Distribusi semua tagihan",
                    headerTrailing = {
                        Box {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                                    .clickable { scopeDropdownExpanded = true }
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(invoiceScopeFilter, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                            DropdownMenu(
                                expanded = scopeDropdownExpanded,
                                onDismissRequest = { scopeDropdownExpanded = false }
                            ) {
                                listOf("Semua Tagihan", "Bulan Ini", "Terlambat").forEach { opt ->
                                    DropdownMenuItem(
                                        text = { Text(opt) },
                                        onClick = {
                                            invoiceScopeFilter = opt
                                            scopeDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DonutRingChart(
                            slices = slices,
                            centerLabel = centerTitle,
                            centerValue = centerCount.toString(),
                            centerValueColor = if (overdueInvoices.isNotEmpty()) NocRedOffline else MaterialTheme.colorScheme.onSurface,
                            sizeDp = 128
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            slices.forEach { s ->
                                val pct = if (totalInvoices > 0) (s.count * 100f / totalInvoices) else 0f
                                DonutLegendRow(
                                    color = s.color,
                                    title = s.label,
                                    subtitle = "${s.count} tagihan",
                                    pctText = String.format(Locale.US, "%.1f%%", pct)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TintedSummaryPill("TERKUMPUL", Formatters.formatRupiah(paidAmount), NocEmeraldOnline, Modifier.weight(1f))
                        TintedSummaryPill("TERTUNDA", Formatters.formatRupiah(unpaidAmount), NocAmber, Modifier.weight(1f))
                        TintedSummaryPill("RISIKO", Formatters.formatRupiah(overdueAmount), NocRedOffline, Modifier.weight(1f))
                    }
                }
            }

            // 7. Status Pelanggan (Donut Chart)
            item {
                val custSlices = listOf(
                    DonutSlice("Aktif", activeCustomers, NocEmeraldOnline),
                    DonutSlice("Tidak Aktif", inactiveCustomers, Color(0xFF64748B)),
                    DonutSlice("Disuspend", suspendedCustomers, NocAmber)
                )
                StatSectionCard(
                    icon = Icons.Outlined.Group,
                    iconTint = NocEmeraldOnline,
                    title = "Status Pelanggan",
                    subtitle = "Distribusi seluruh pelanggan"
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        DonutRingChart(
                            slices = custSlices,
                            centerLabel = "Aktif",
                            centerValue = activeCustomers.toString(),
                            centerValueColor = NocEmeraldOnline,
                            sizeDp = 152
                        )
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            custSlices.forEach { s ->
                                val pct = if (totalCustomers > 0) (s.count * 100f / totalCustomers) else 0f
                                DonutLegendRow(
                                    color = s.color,
                                    title = s.label,
                                    subtitle = "${s.count} pelanggan",
                                    pctText = String.format(Locale.US, "%.1f%%", pct)
                                )
                            }
                        }
                    }
                }
            }

            // 8. Aktivitas Bulan Ini
            item {
                StatSectionCard(
                    icon = Icons.Outlined.MonitorHeart,
                    iconTint = NocBlueDownload,
                    title = "Aktivitas Bulan Ini",
                    subtitle = "Ringkasan aktivitas operasional"
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            InsetStatBox("PELANGGAN BARU", activeCustomers.toString(), Modifier.weight(1f))
                            InsetStatBox("TAGIHAN DIBUAT", totalInvoices.toString(), Modifier.weight(1f))
                        }
                        InsetStatBox("PENDAPATAN BULANAN", Formatters.formatRupiah(paidAmount), Modifier.fillMaxWidth())
                    }
                }
            }

            // 9. Infrastruktur
            item {
                StatSectionCard(
                    icon = Icons.Outlined.Dns,
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                    title = "Infrastruktur",
                    subtitle = "Kapasitas jaringan dan layanan"
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            InsetStatBox(
                                "SERVER",
                                uiState.totalRouters.toString(),
                                Modifier.weight(1f),
                                footnote = "${uiState.onlineRouters} aktif"
                            )
                            InsetStatBox(
                                "PRODUK",
                                packages.size.toString(),
                                Modifier.weight(1f),
                                footnote = "${packages.count { it.isActive }} aktif"
                            )
                        }
                        InsetStatBox(
                            "DISTRIBUSI",
                            areas.sumOf { it.odpCount }.toString(),
                            Modifier.fillMaxWidth(),
                            footnote = "titik distribusi (${areas.size} area)"
                        )
                    }
                }
            }

            // 10. Rincian Pelanggan (Horizontal Bar Chart)
            item {
                val barRows = listOf(
                    Triple("Aktif", activeCustomers, NocEmeraldOnline),
                    Triple("Tidak Aktif", inactiveCustomers, Color(0xFF64748B)),
                    Triple("Disuspend", suspendedCustomers, NocAmber),
                    Triple("Koneksi PPPoE", uiState.onlineCustomers, NocBlueDownload),
                    Triple("Koneksi Statis", 0, NocPurpleAccent)
                )
                val maxBar = maxOf(barRows.maxOfOrNull { it.second } ?: 1, 3)

                StatSectionCard(
                    icon = Icons.Rounded.Router,
                    iconTint = NocPurpleAccent,
                    title = "Rincian Pelanggan",
                    subtitle = "$totalCustomers pelanggan terdaftar"
                ) {
                    HorizontalCustomerBarChart(
                        rows = barRows,
                        maxValue = maxBar
                    )
                }
            }
        }
    }
}

@Composable
private fun TopStatMiniCard(
    icon: ImageVector,
    iconTint: Color,
    pillText: String,
    pillColor: Color,
    label: String,
    value: String,
    subLabel: String,
    highlightAmber: Boolean
) {
    val bgColor = if (highlightAmber) NocAmber.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
    val borderColor = if (highlightAmber) NocAmber.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outline

    Surface(
        modifier = Modifier
            .width(154.dp)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = bgColor
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconTint.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(pillColor.copy(alpha = 0.14f))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = pillText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = pillColor,
                        maxLines = 1
                    )
                }
            }
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Text(
                    text = value,
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = subLabel,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun StatSectionCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    trailingText: String? = null,
    headerTrailing: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(iconTint.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                headerTrailing?.invoke()
            }
            if (trailingText != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Text(
                        text = trailingText,
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun InsetStatBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    footnote: String? = null
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!footnote.isNullOrBlank()) {
            Text(
                text = footnote,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MonthlyTrendLineChart(
    points: List<Pair<String, Float>>,
    lineColor: Color,
    yFormatter: (Float) -> String,
    showOnlyLastPointIfZero: Boolean = false
) {
    val maxVal = (points.maxOfOrNull { it.second } ?: 0f).coerceAtLeast(2f)
    val outline = MaterialTheme.colorScheme.outline
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .width(46.dp)
                    .height(120.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(yFormatter(maxVal), fontSize = 9.sp, color = textColor, maxLines = 1)
                Text(yFormatter(maxVal / 2f), fontSize = 9.sp, color = textColor, maxLines = 1)
                Text(yFormatter(0f), fontSize = 9.sp, color = textColor, maxLines = 1)
            }

            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(120.dp)
            ) {
                val w = size.width
                val h = size.height
                val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                listOf(0.1f, 0.5f, 0.9f).forEach { yr ->
                    drawLine(
                        color = outline.copy(alpha = 0.6f),
                        start = Offset(0f, h * yr),
                        end = Offset(w, h * yr),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = dash
                    )
                }

                if (showOnlyLastPointIfZero && points.dropLast(1).all { it.second == 0f }) {
                    val lastVal = points.lastOrNull()?.second ?: 0f
                    val y = h * (0.9f - (lastVal / maxVal).coerceIn(0f, 1f) * 0.8f)
                    drawCircle(color = lineColor, radius = 4.dp.toPx(), center = Offset(w * 0.5f, y))
                } else if (points.size >= 2) {
                    val stepX = w / (points.size - 1).toFloat()
                    val coords = points.mapIndexed { idx, (_, v) ->
                        val norm = (v / maxVal).coerceIn(0f, 1f)
                        Offset(idx * stepX, h * (0.9f - norm * 0.8f))
                    }
                    for (i in 0 until coords.lastIndex) {
                        drawLine(
                            color = lineColor,
                            start = coords[i],
                            end = coords[i + 1],
                            strokeWidth = 2.2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                    coords.forEach { pt ->
                        drawCircle(color = lineColor, radius = 3.2.dp.toPx(), center = pt)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 46.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(points.firstOrNull()?.first.orEmpty(), fontSize = 9.sp, color = textColor)
            Text(points.getOrNull(points.size / 2)?.first.orEmpty(), fontSize = 9.sp, color = textColor)
            Text(points.lastOrNull()?.first.orEmpty(), fontSize = 9.sp, color = textColor)
        }
    }
}

@Composable
private fun CircularScoreGauge(
    percent: Int,
    ringColor: Color,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(74.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 8.dp.toPx()
                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = stroke)
                )
                drawArc(
                    color = ringColor,
                    startAngle = -90f,
                    sweepAngle = (percent.coerceIn(0, 100) / 100f) * 360f,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
            Text(
                text = "$percent%",
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DonutRingChart(
    slices: List<DonutSlice>,
    centerLabel: String,
    centerValue: String,
    centerValueColor: Color,
    sizeDp: Int
) {
    val total = slices.sumOf { it.count }
    val emptyTrack = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = Modifier.size(sizeDp.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 20.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(inset, inset)

            if (total <= 0) {
                drawArc(
                    color = emptyTrack,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke)
                )
            } else {
                var startAngle = -90f
                slices.forEach { s ->
                    if (s.count > 0) {
                        val sweep = (s.count.toFloat() / total.toFloat()) * 360f
                        drawArc(
                            color = s.color,
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = stroke)
                        )
                        startAngle += sweep
                    }
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = centerLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = centerValue,
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = centerValueColor
            )
        }
    }
}

@Composable
private fun DonutLegendRow(
    color: Color,
    title: String,
    subtitle: String,
    pctText: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(
            text = pctText,
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun TintedSummaryPill(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.28f), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = color.copy(alpha = 0.85f), maxLines = 1)
        Text(
            text = value,
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HorizontalCustomerBarChart(
    rows: List<Triple<String, Int, Color>>,
    maxValue: Int
) {
    val outline = MaterialTheme.colorScheme.outline
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        rows.forEach { (label, count, barColor) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = labelColor,
                    modifier = Modifier.width(96.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                ) {
                    val ratio = (count.toFloat() / maxValue.toFloat()).coerceIn(0f, 1f)
                    if (ratio > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(ratio)
                                .height(14.dp)
                                .clip(RoundedCornerShape(50))
                                .background(barColor)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = count.toString(),
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.width(24.dp)
                )
            }
        }
        // X-axis reference ticks
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 96.dp, end = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("0", fontSize = 9.sp, color = labelColor)
            Text((maxValue / 2).toString(), fontSize = 9.sp, color = labelColor)
            Text(maxValue.toString(), fontSize = 9.sp, color = labelColor)
        }
    }
}
