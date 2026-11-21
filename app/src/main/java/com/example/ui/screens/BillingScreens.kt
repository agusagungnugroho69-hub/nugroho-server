package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.InternetPackageEntity
import com.example.data.model.InvoiceEntity
import com.example.data.model.InvoiceStatus
import com.example.data.model.TransactionEntity
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.ModuleScaffold
import com.example.ui.components.StatusBadge
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.NocAmber
import com.example.ui.theme.NocBlueDownload
import com.example.ui.theme.NocEmeraldOnline
import com.example.ui.theme.NocRedOffline
import com.example.util.Formatters

@Composable
fun TagihanScreen(
    invoices: List<InvoiceEntity>,
    packages: List<InternetPackageEntity>,
    ppnPercent: Int,
    onBack: () -> Unit,
    onCreateInvoice: (customer: String, pppoe: String, pkg: String, area: String, amount: Long, due: String, note: String) -> Unit,
    onMarkPaid: (InvoiceEntity, String) -> Unit,
    onMarkOverdue: (InvoiceEntity) -> Unit,
    onDeleteInvoice: (InvoiceEntity) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf<InvoiceStatus?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var invoiceToPay by remember { mutableStateOf<InvoiceEntity?>(null) }

    val totalLunas = remember(invoices) { invoices.filter { it.status == InvoiceStatus.LUNAS } }
    val totalBelum = remember(invoices) { invoices.filter { it.status == InvoiceStatus.BELUM_BAYAR } }
    val totalTelat = remember(invoices) { invoices.filter { it.status == InvoiceStatus.TERLAMBAT } }

    val filtered = remember(invoices, query, statusFilter) {
        val q = query.trim().lowercase()
        invoices.filter { inv ->
            val matchStatus = statusFilter == null || inv.status == statusFilter
            val matchQuery = q.isEmpty() ||
                inv.invoiceNumber.lowercase().contains(q) ||
                inv.customerName.lowercase().contains(q) ||
                inv.packageName.lowercase().contains(q)
            matchStatus && matchQuery
        }
    }

    ModuleScaffold(
        title = "Tagihan Pelanggan",
        subtitle = "Kelola invoice, bayar, dan jatuh tempo",
        onBack = onBack,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_invoice_fab")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Generate Tagihan")
            }
        }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KpiMetricCard(
                            title = "Total Invoice",
                            value = invoices.size.toString(),
                            subtitle = Formatters.formatRupiah(invoices.sumOf { it.amount }),
                            icon = Icons.Rounded.Receipt,
                            accentColor = NocAmber,
                            modifier = Modifier.weight(1f)
                        )
                        KpiMetricCard(
                            title = "Lunas",
                            value = totalLunas.size.toString(),
                            subtitle = Formatters.formatRupiah(totalLunas.sumOf { it.amount }),
                            icon = Icons.Rounded.CheckCircle,
                            accentColor = NocBlueDownload,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KpiMetricCard(
                            title = "Belum Dibayar",
                            value = totalBelum.size.toString(),
                            subtitle = Formatters.formatRupiah(totalBelum.sumOf { it.amount }),
                            icon = Icons.Rounded.Schedule,
                            accentColor = NocAmber,
                            modifier = Modifier.weight(1f)
                        )
                        KpiMetricCard(
                            title = "Lewat Tempo",
                            value = totalTelat.size.toString(),
                            subtitle = Formatters.formatRupiah(totalTelat.sumOf { it.amount }),
                            icon = Icons.Rounded.WarningAmber,
                            accentColor = NocRedOffline,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Cari no faktur atau nama pelanggan...") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = statusFilter == null,
                            onClick = { statusFilter = null },
                            label = { Text("Semua") }
                        )
                        InvoiceStatus.entries.forEach { st ->
                            FilterChip(
                                selected = statusFilter == st,
                                onClick = { statusFilter = st },
                                label = { Text(st.label) }
                            )
                        }
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    EmptyModuleBanner(
                        title = "Belum Ada Data Tagihan",
                        subtitle = "Tekan tombol + untuk membuat tagihan pelanggan ISP (otomatis menghitung DPP & PPN $ppnPercent%)."
                    )
                }
            } else {
                items(filtered, key = { it.id }) { inv ->
                    InvoiceItemCard(
                        invoice = inv,
                        onPayClick = { invoiceToPay = inv },
                        onMarkOverdue = { onMarkOverdue(inv) },
                        onDelete = { onDeleteInvoice(inv) }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateInvoiceDialog(
            packages = packages,
            onDismiss = { showCreateDialog = false },
            onSave = { cust, pppoe, pkg, area, amt, due, note ->
                onCreateInvoice(cust, pppoe, pkg, area, amt, due, note)
                showCreateDialog = false
            }
        )
    }

    invoiceToPay?.let { target ->
        var method by remember { mutableStateOf("Transfer Bank") }
        AlertDialog(
            onDismissRequest = { invoiceToPay = null },
            title = { Text("Bayar ${target.invoiceNumber}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("${target.customerName} • ${Formatters.formatRupiah(target.amount)}")
                    Text("Pilih metode pembayaran:", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Transfer Bank", "Cash", "QRIS").forEach { m ->
                            FilterChip(
                                selected = method == m,
                                onClick = { method = m },
                                label = { Text(m, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onMarkPaid(target, method)
                        invoiceToPay = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NocEmeraldOnline)
                ) {
                    Text("Konfirmasi Lunas")
                }
            },
            dismissButton = {
                TextButton(onClick = { invoiceToPay = null }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun InvoiceItemCard(
    invoice: InvoiceEntity,
    onPayClick: () -> Unit,
    onMarkOverdue: () -> Unit,
    onDelete: () -> Unit
) {
    val badgeColor = when (invoice.status) {
        InvoiceStatus.LUNAS -> NocBlueDownload
        InvoiceStatus.BELUM_BAYAR -> NocAmber
        InvoiceStatus.TERLAMBAT -> NocRedOffline
        InvoiceStatus.DIBATALKAN -> MaterialTheme.colorScheme.onSurfaceVariant
    }

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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = invoice.invoiceNumber,
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = invoice.customerName,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${invoice.packageName} • Tempo: ${invoice.dueDateLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusBadge(label = invoice.status.label, color = badgeColor)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("DPP / PPN", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "${Formatters.formatRupiah(invoice.dppAmount)} + ${Formatters.formatRupiah(invoice.ppnAmount)}",
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 11.sp
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Total Tagihan", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        Formatters.formatRupiah(invoice.amount),
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (invoice.status != InvoiceStatus.LUNAS) {
                    Button(
                        onClick = onPayClick,
                        colors = ButtonDefaults.buttonColors(containerColor = NocBlueDownload),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Icon(Icons.Rounded.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Bayar", fontSize = 12.sp)
                    }
                    if (invoice.status == InvoiceStatus.BELUM_BAYAR) {
                        OutlinedButton(
                            onClick = onMarkOverdue,
                            modifier = Modifier.height(40.dp)
                        ) {
                            Text("Telat", fontSize = 12.sp, color = NocRedOffline)
                        }
                    }
                } else {
                    Text(
                        text = "Metode: ${invoice.paymentMethod}",
                        style = MaterialTheme.typography.bodySmall,
                        color = NocEmeraldOnline,
                        modifier = Modifier.weight(1f)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Hapus tagihan", tint = NocRedOffline)
                }
            }
        }
    }
}

@Composable
private fun CreateInvoiceDialog(
    packages: List<InternetPackageEntity>,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, Long, String, String) -> Unit
) {
    var customer by remember { mutableStateOf("") }
    var pppoe by remember { mutableStateOf("") }
    var pkgName by remember { mutableStateOf(packages.firstOrNull()?.name ?: "Paket Fiber 20M") }
    var amountText by remember { mutableStateOf((packages.firstOrNull()?.monthlyPrice ?: 150_000L).toString()) }
    var area by remember { mutableStateOf("Area Pusat") }
    var dueDate by remember { mutableStateOf("28 Nov 2026") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Generate Tagihan Baru") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = customer,
                    onValueChange = { customer = it },
                    label = { Text("Nama Pelanggan") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = pppoe,
                    onValueChange = { pppoe = it },
                    label = { Text("Username PPPoE (Opsional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = pkgName,
                    onValueChange = { pkgName = it },
                    label = { Text("Paket Internet") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() } },
                    label = { Text("Nominal Tagihan (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text("Tanggal Jatuh Tempo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toLongOrNull() ?: 150_000L
                    if (customer.isNotBlank()) {
                        onSave(customer, pppoe, pkgName, area, amt, dueDate, note)
                    }
                }
            ) { Text("Buat Invoice") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@Composable
fun TransaksiScreen(
    transactions: List<TransactionEntity>,
    onBack: () -> Unit,
    onToggleVerified: (TransactionEntity) -> Unit
) {
    val totalVerified = remember(transactions) { transactions.filter { it.verified }.sumOf { it.amount } }
    val totalPending = remember(transactions) { transactions.filter { !it.verified }.sumOf { it.amount } }

    ModuleScaffold(
        title = "Transaksi Pembayaran",
        subtitle = "Verifikasi pemasukan & riwayat bayar",
        onBack = onBack
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiMetricCard(
                        title = "Terverifikasi",
                        value = transactions.count { it.verified }.toString(),
                        subtitle = Formatters.formatRupiah(totalVerified),
                        icon = Icons.Rounded.CheckCircle,
                        accentColor = NocEmeraldOnline,
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricCard(
                        title = "Menunggu Cek",
                        value = transactions.count { !it.verified }.toString(),
                        subtitle = Formatters.formatRupiah(totalPending),
                        icon = Icons.Rounded.Schedule,
                        accentColor = NocAmber,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (transactions.isEmpty()) {
                item {
                    EmptyModuleBanner(
                        title = "Belum Ada Transaksi",
                        subtitle = "Transaksi akan tercatat otomatis ketika tagihan ditandai Lunas pada menu Tagihan."
                    )
                }
            } else {
                items(transactions, key = { it.id }) { tx ->
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
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${tx.referenceCode} • ${tx.invoiceNumber}",
                                    fontFamily = JetBrainsMonoFamily,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = tx.customerName,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "${tx.method} • ${Formatters.formatDateTime(tx.timestamp)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = Formatters.formatRupiah(tx.amount),
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = NocEmeraldOnline
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                TextButton(onClick = { onToggleVerified(tx) }) {
                                    StatusBadge(
                                        label = if (tx.verified) "TERVERIFIKASI" else "PENDING",
                                        color = if (tx.verified) NocEmeraldOnline else NocAmber
                                    )
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
fun LaporanPajakScreen(
    invoices: List<InvoiceEntity>,
    ppnPercent: Int,
    onBack: () -> Unit
) {
    val paidInvoices = remember(invoices) { invoices.filter { it.status == InvoiceStatus.LUNAS } }
    val totalBruto = remember(paidInvoices) { paidInvoices.sumOf { it.amount } }
    val totalDpp = remember(paidInvoices) { paidInvoices.sumOf { it.dppAmount } }
    val totalPpn = remember(paidInvoices) { paidInvoices.sumOf { it.ppnAmount } }

    ModuleScaffold(
        title = "Laporan Pajak",
        subtitle = "Rekap DPP & PPN $ppnPercent% dari tagihan lunas",
        onBack = onBack
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    KpiMetricCard(
                        title = "Total Pendapatan Bruto (Lunas)",
                        value = Formatters.formatRupiah(totalBruto),
                        subtitle = "${paidInvoices.size} faktur terbayar",
                        icon = Icons.Rounded.Payments,
                        accentColor = NocEmeraldOnline,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KpiMetricCard(
                            title = "Total DPP",
                            value = Formatters.formatRupiah(totalDpp),
                            subtitle = "Dasar Pengenaan Pajak",
                            icon = Icons.Rounded.Receipt,
                            accentColor = NocBlueDownload,
                            modifier = Modifier.weight(1f)
                        )
                        KpiMetricCard(
                            title = "Total PPN ($ppnPercent%)",
                            value = Formatters.formatRupiah(totalPpn),
                            subtitle = "Pajak Pertambahan Nilai",
                            icon = Icons.Rounded.Receipt,
                            accentColor = NocAmber,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            if (paidInvoices.isEmpty()) {
                item {
                    EmptyModuleBanner(
                        title = "Belum Ada Rekap Pajak",
                        subtitle = "Rekapitulasi DPP & PPN akan terisi otomatis dari invoice yang berstatus Lunas."
                    )
                }
            } else {
                items(paidInvoices, key = { it.id }) { inv ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(inv.invoiceNumber, fontFamily = JetBrainsMonoFamily, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                Text(inv.customerName, style = MaterialTheme.typography.titleSmall)
                                Text("DPP: ${Formatters.formatRupiah(inv.dppAmount)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("PPN: ${Formatters.formatRupiah(inv.ppnAmount)}", fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold, color = NocAmber)
                                Text("Total: ${Formatters.formatRupiah(inv.amount)}", fontFamily = JetBrainsMonoFamily, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyModuleBanner(title: String, subtitle: String) {
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
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
