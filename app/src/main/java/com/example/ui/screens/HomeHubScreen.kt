package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeviceHub
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.TelegramSettings
import com.example.ui.navigation.Routes
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.NocAmber
import com.example.ui.theme.NocBlueDownload
import com.example.ui.theme.NocEmeraldOnline
import com.example.ui.theme.NocPurpleAccent
import com.example.ui.theme.NocRedOffline
import com.example.util.Formatters
import com.example.viewmodel.DashboardUiState

private data class ModuleMenuItem(
    val route: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accentColor: Color,
    val testTag: String
)

private data class ModuleSection(
    val sectionTitle: String,
    val items: List<ModuleMenuItem>
)

@Composable
fun HomeHubScreen(
    uiState: DashboardUiState,
    settings: TelegramSettings,
    onToggleTheme: () -> Unit,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val sections = remember {
        listOf(
            ModuleSection(
                sectionTitle = "UTAMA",
                items = listOf(
                    ModuleMenuItem(Routes.ROUTE_DASHBOARD, "Dashboard", "Statistik & analitik", Icons.Outlined.BarChart, Color(0xFF4F7DF3), "mod_dashboard"),
                    ModuleMenuItem(Routes.ROUTE_PPPOE, "Pelanggan", "Data & akun user", Icons.Outlined.Group, Color(0xFF10B981), "mod_pelanggan"),
                    ModuleMenuItem(Routes.ROUTE_TAGIHAN, "Tagihan", "Pembayaran", Icons.Outlined.CreditCard, Color(0xFF3B82F6), "mod_tagihan"),
                    ModuleMenuItem(Routes.ROUTE_TRANSAKSI, "Transaksi", "Verifikasi bayar", Icons.Outlined.SwapHoriz, Color(0xFF06B6D4), "mod_transaksi")
                )
            ),
            ModuleSection(
                sectionTitle = "NETWORKING",
                items = listOf(
                    ModuleMenuItem(Routes.ROUTE_ROUTERS, "Mikrotik", "Router & perangkat", Icons.Outlined.Dns, Color(0xFF10B981), "mod_mikrotik"),
                    ModuleMenuItem(Routes.ROUTE_PRODUK, "Produk", "Paket internet", Icons.Outlined.Inventory2, Color(0xFFF59E0B), "mod_produk"),
                    ModuleMenuItem(Routes.ROUTE_AREA_MAPS, "Area & Maps", "Distribusi & area", Icons.Outlined.Layers, Color(0xFF8B5CF6), "mod_area_maps"),
                    ModuleMenuItem(Routes.ROUTE_GENIEACS, "GenieACS", "Device TR-069", Icons.Outlined.Memory, Color(0xFF06B6D4), "mod_genieacs"),
                    ModuleMenuItem(Routes.ROUTE_RADIUS, "Radius", "Koneksi & quota", Icons.Outlined.VpnKey, Color(0xFF14B8A6), "mod_radius")
                )
            ),
            ModuleSection(
                sectionTitle = "LAPORAN",
                items = listOf(
                    ModuleMenuItem(Routes.ROUTE_LAPORAN_PAJAK, "Laporan Pajak", "Rekap pajak", Icons.Outlined.ReceiptLong, Color(0xFF10B981), "mod_pajak"),
                    ModuleMenuItem(Routes.ROUTE_SALES, "Sales", "Tim & target", Icons.Outlined.ShoppingCart, Color(0xFFF59E0B), "mod_sales"),
                    ModuleMenuItem(Routes.ROUTE_LOG_AKTIVITAS, "Log Aktivitas", "Jejak perubahan", Icons.Outlined.Schedule, Color(0xFF94A3B8), "mod_log_aktivitas")
                )
            ),
            ModuleSection(
                sectionTitle = "LAYANAN",
                items = listOf(
                    ModuleMenuItem(Routes.ROUTE_TIKET, "Tiket", "Bantuan pelanggan", Icons.Outlined.ConfirmationNumber, Color(0xFFEC4899), "mod_tiket"),
                    ModuleMenuItem(Routes.ROUTE_ALERTS, "Notifikasi", "Notifikasi customer", Icons.Outlined.Notifications, Color(0xFFF97316), "mod_notifikasi")
                )
            )
        )
    }

    val totalBytes = remember(uiState.telemetryByRouter) {
        uiState.telemetryByRouter.values.sumOf { t ->
            t.interfaces.sumOf { it.rxBytes + it.txBytes }
        }
    }
    val totalRxBytes = remember(uiState.telemetryByRouter) {
        uiState.telemetryByRouter.values.sumOf { t -> t.interfaces.sumOf { it.rxBytes } }
    }
    val totalTxBytes = remember(uiState.telemetryByRouter) {
        uiState.telemetryByRouter.values.sumOf { t -> t.interfaces.sumOf { it.txBytes } }
    }

    val allOnline = uiState.totalRouters == 0 || uiState.onlineRouters > 0
    val statusBadgeColor = if (uiState.totalRouters == 0) NocAmber else if (allOnline) NocEmeraldOnline else NocRedOffline
    val statusBadgeText = when {
        uiState.totalRouters == 0 -> "Siap"
        uiState.onlineRouters == uiState.totalRouters -> "Online"
        uiState.onlineRouters > 0 -> "${uiState.onlineRouters}/${uiState.totalRouters} Online"
        else -> "Offline"
    }

    val heroGradient = if (settings.darkTheme) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF42280B),
                Color(0xFF1F160C),
                MaterialTheme.colorScheme.background
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFDE68A).copy(alpha = 0.55f),
                MaterialTheme.colorScheme.background
            )
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Warm Hero Header + Featured Monitoring Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(heroGradient)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Brand Row
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
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0D1117))
                                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.mikromon_icon_1795172654316),
                                contentDescription = "Logo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = settings.ispName.uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = settings.ispSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(50))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(statusBadgeColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = statusBadgeText,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(
                            onClick = onToggleTheme,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                                .testTag("home_theme_toggle")
                        ) {
                            Icon(
                                imageVector = if (settings.darkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                                contentDescription = "Ganti tema",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Greeting + Date Pill Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${Formatters.formatGreeting()}, ${settings.adminName}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                        modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(50))
                    ) {
                        Text(
                            text = Formatters.formatShortDayDate(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                // Featured Monitoring Card (Exact Screenshot Structure)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp)),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(NocAmber),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Speed,
                                    contentDescription = "Monitoring",
                                    tint = Color(0xFF1A1000),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Monitoring",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Inset 2-column dark box: SERVER & TOTAL TRAFFIC
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "SERVER",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeviceHub,
                                        contentDescription = null,
                                        tint = NocAmber,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${uiState.onlineRouters}/${uiState.totalRouters} server",
                                        fontFamily = JetBrainsMonoFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "${uiState.onlineCustomers} PPPoE online",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(54.dp)
                                    .background(MaterialTheme.colorScheme.outline)
                            )

                            Column(
                                modifier = Modifier
                                    .weight(1.15f)
                                    .padding(start = 12.dp)
                            ) {
                                Text(
                                    text = "TOTAL TRAFFIC",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = NocAmber
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (totalBytes > 0L) Formatters.formatBytes(totalBytes)
                                    else Formatters.formatBps(uiState.totalRxBps + uiState.totalTxBps),
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = NocAmber,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.ArrowDownward,
                                            contentDescription = "Download",
                                            tint = NocBlueDownload,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = if (totalRxBytes > 0L) Formatters.formatBytes(totalRxBytes)
                                            else Formatters.formatBps(uiState.totalRxBps),
                                            fontFamily = JetBrainsMonoFamily,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.ArrowUpward,
                                            contentDescription = "Upload",
                                            tint = NocPurpleAccent,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = if (totalTxBytes > 0L) Formatters.formatBytes(totalTxBytes)
                                            else Formatters.formatBps(uiState.totalTxBps),
                                            fontFamily = JetBrainsMonoFamily,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // Full-width bright Monitoring CTA button
                        Button(
                            onClick = { onNavigate(Routes.ROUTE_MONITORING) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (settings.darkTheme) Color(0xFFF3F6FA) else MaterialTheme.colorScheme.primary,
                                contentColor = if (settings.darkTheme) Color(0xFF0A0D12) else MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("hero_monitoring_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Storage,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Monitoring",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // Categorized Module Container Card
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Modul",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(50))
                        ) {
                            Text(
                                text = "14 modul",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    sections.forEach { section ->
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = section.sectionTitle,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = 0.8.sp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                HorizontalDivider(
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                                )
                            }

                            section.items.chunked(3).forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowItems.forEach { modItem ->
                                        ModuleGridTile(
                                            item = modItem,
                                            onClick = { onNavigate(modItem.route) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    repeat(3 - rowItems.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
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
private fun ModuleGridTile(
    item: ModuleMenuItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .testTag(item.testTag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(item.accentColor.copy(alpha = 0.14f))
                .border(1.dp, item.accentColor.copy(alpha = 0.28f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = item.accentColor,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = item.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = item.subtitle,
            style = MaterialTheme.typography.bodySmall,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
