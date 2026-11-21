package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.model.RouterConfig
import com.example.ui.components.RouterEditorSheet
import com.example.ui.navigation.Routes
import com.example.ui.screens.AlertsTelegramScreen
import com.example.ui.screens.AreaMapsHubScreen
import com.example.ui.screens.AreaMapsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FtthLokasiScreen
import com.example.ui.screens.FtthTopologiScreen
import com.example.ui.screens.GenieAcsScreen
import com.example.ui.screens.HomeHubScreen
import com.example.ui.screens.LaporanPajakScreen
import com.example.ui.screens.LogAktivitasScreen
import com.example.ui.screens.MapsFtthScreen
import com.example.ui.screens.PppoeScreen
import com.example.ui.screens.ProdukScreen
import com.example.ui.screens.RadiusScreen
import com.example.ui.screens.RouterDetailScreen
import com.example.ui.screens.RoutersScreen
import com.example.ui.screens.SalesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StatistikDashboardScreen
import com.example.ui.screens.TagihanScreen
import com.example.ui.screens.TiketScreen
import com.example.ui.screens.TransaksiScreen
import com.example.ui.theme.MikroMonTheme
import com.example.ui.theme.NocAmber
import com.example.viewmodel.IspModulesViewModel
import com.example.viewmodel.MainViewModel

private data class BottomTabItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val modulesViewModel: IspModulesViewModel = viewModel()
            val settings by viewModel.settings.collectAsStateWithLifecycle()

            MikroMonTheme(darkTheme = settings.darkTheme) {
                MikroMonRoot(
                    viewModel = viewModel,
                    modulesViewModel = modulesViewModel
                )
            }
        }
    }
}

@Composable
private fun MikroMonRoot(
    viewModel: MainViewModel,
    modulesViewModel: IspModulesViewModel
) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val alertLogs by viewModel.alertLogs.collectAsStateWithLifecycle()
    val isTestingConnection by viewModel.isTestingConnection.collectAsStateWithLifecycle()
    val connectionTestResult by viewModel.connectionTestResult.collectAsStateWithLifecycle()
    val isTestingTelegram by viewModel.isTestingTelegram.collectAsStateWithLifecycle()

    val invoices by modulesViewModel.invoices.collectAsStateWithLifecycle()
    val transactions by modulesViewModel.transactions.collectAsStateWithLifecycle()
    val packages by modulesViewModel.packages.collectAsStateWithLifecycle()
    val areas by modulesViewModel.areas.collectAsStateWithLifecycle()
    val salesReps by modulesViewModel.salesReps.collectAsStateWithLifecycle()
    val tickets by modulesViewModel.tickets.collectAsStateWithLifecycle()
    val activityLogs by modulesViewModel.activityLogs.collectAsStateWithLifecycle()
    val cpeDevices by modulesViewModel.cpeDevices.collectAsStateWithLifecycle()
    val radiusEntries by modulesViewModel.radiusEntries.collectAsStateWithLifecycle()
    val isSyncingExternal by modulesViewModel.isSyncingExternal.collectAsStateWithLifecycle()
    val ftthNodes by modulesViewModel.ftthNodes.collectAsStateWithLifecycle()
    val customerPins by modulesViewModel.customerPins.collectAsStateWithLifecycle()
    val cableRoutes by modulesViewModel.cableRoutes.collectAsStateWithLifecycle()
    val customers by modulesViewModel.customers.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }
    LaunchedEffect(Unit) {
        modulesViewModel.message.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    var showEditorSheet by remember { mutableStateOf(false) }
    var editingRouter by remember { mutableStateOf<RouterConfig?>(null) }

    fun openAddRouter() {
        viewModel.clearConnectionTestResult()
        editingRouter = null
        showEditorSheet = true
    }

    fun openEditRouter(router: RouterConfig) {
        viewModel.clearConnectionTestResult()
        editingRouter = router
        showEditorSheet = true
    }

    val bottomTabs = remember {
        listOf(
            BottomTabItem(
                route = Routes.ROUTE_HOME,
                label = "Home",
                selectedIcon = Icons.Rounded.Home,
                unselectedIcon = Icons.Outlined.Home,
                testTag = "nav_home"
            ),
            BottomTabItem(
                route = Routes.ROUTE_HISTORY,
                label = "History",
                selectedIcon = Icons.Rounded.ReceiptLong,
                unselectedIcon = Icons.Outlined.ReceiptLong,
                testTag = "nav_history"
            ),
            BottomTabItem(
                route = Routes.ROUTE_ALERTS,
                label = "Notifikasi",
                selectedIcon = Icons.Rounded.Notifications,
                unselectedIcon = Icons.Outlined.Notifications,
                testTag = "nav_alerts"
            ),
            BottomTabItem(
                route = Routes.ROUTE_SETTINGS,
                label = "Settings",
                selectedIcon = Icons.Rounded.Settings,
                unselectedIcon = Icons.Outlined.Settings,
                testTag = "nav_settings"
            )
        )
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showTopLevelNav = bottomTabs.any { item ->
        currentDestination?.hierarchy?.any { it.route == item.route } == true
    }

    fun navigateToBottomTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 680.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (showTopLevelNav && !useNavigationRail) {
                    RoundedNocBottomBar(
                        items = bottomTabs,
                        currentDestination = currentDestination,
                        onSelect = ::navigateToBottomTab
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showTopLevelNav && useNavigationRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.windowInsetsPadding(WindowInsets(0))
                    ) {
                        bottomTabs.forEach { item ->
                            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToBottomTab(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = NocAmber,
                                    selectedTextColor = NocAmber,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = Routes.ROUTE_HOME,
                    modifier = Modifier.weight(1f)
                ) {
                    // Bottom Tab 1: Home Hub (Exact Screenshot Layout)
                    composable(Routes.ROUTE_HOME) {
                        HomeHubScreen(
                            uiState = uiState,
                            settings = settings,
                            onToggleTheme = {
                                viewModel.updateSettings { it.copy(darkTheme = !it.darkTheme) }
                            },
                            onNavigate = { targetRoute ->
                                if (bottomTabs.any { it.route == targetRoute }) {
                                    navigateToBottomTab(targetRoute)
                                } else {
                                    navController.navigate(targetRoute)
                                }
                            }
                        )
                    }

                    // Bottom Tab 2: History
                    composable(Routes.ROUTE_HISTORY) {
                        LogAktivitasScreen(
                            logs = activityLogs,
                            showBackButton = false,
                            onBack = { navController.popBackStack() },
                            onClearLogs = { modulesViewModel.clearActivityLogs() }
                        )
                    }

                    // Bottom Tab 3: Notifikasi
                    composable(Routes.ROUTE_ALERTS) {
                        AlertsTelegramScreen(
                            settings = settings,
                            alertLogs = alertLogs,
                            isTestingTelegram = isTestingTelegram,
                            hasNotificationPermission = { viewModel.hasNotificationPermission() },
                            onUpdateSettings = { transform -> viewModel.updateSettings(transform) },
                            onSendTestTelegram = { token, chatId ->
                                viewModel.sendTestTelegram(token, chatId)
                            },
                            onClearLogs = { viewModel.clearAlertLogs() }
                        )
                    }

                    // Bottom Tab 4: Settings
                    composable(Routes.ROUTE_SETTINGS) {
                        SettingsScreen(
                            settings = settings,
                            onUpdateSettings = { transform -> viewModel.updateSettings(transform) },
                            onOpenTelegramAlerts = { navigateToBottomTab(Routes.ROUTE_ALERTS) }
                        )
                    }

                    // Live MikroTik NOC Monitoring (from Hero Monitoring button)
                    composable(Routes.ROUTE_MONITORING) {
                        DashboardScreen(
                            uiState = uiState,
                            pollingIntervalSec = settings.pollingIntervalSec,
                            onAddRouterClick = ::openAddRouter,
                            onLoadDemoLab = {
                                viewModel.loadDemoLabRouters()
                                modulesViewModel.logActivity("Monitoring", "Simulasi Lab", "2 router virtual dimuat")
                            },
                            onRefreshClick = { viewModel.refreshNow() },
                            onToggleLivePolling = { viewModel.toggleLivePolling() },
                            onSelectGraphRouter = { viewModel.selectGraphRouter(it) },
                            onOpenRouterDetail = { routerId ->
                                navController.navigate(Routes.routerDetail(routerId))
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    // Module 1: Statistik Dashboard (matching Screenshots 1-4)
                    composable(Routes.ROUTE_DASHBOARD) {
                        StatistikDashboardScreen(
                            uiState = uiState,
                            invoices = invoices,
                            packages = packages,
                            areas = areas,
                            darkTheme = settings.darkTheme,
                            onToggleTheme = {
                                viewModel.updateSettings { it.copy(darkTheme = !it.darkTheme) }
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    // Module 2: Pelanggan PPPoE
                    composable(Routes.ROUTE_PPPOE) {
                        PppoeScreen(
                            customers = customers,
                            routers = uiState.routers,
                            telemetryByRouter = uiState.telemetryByRouter,
                            packages = packages,
                            ftthNodes = ftthNodes,
                            salesReps = salesReps,
                            darkTheme = settings.darkTheme,
                            onToggleTheme = {
                                viewModel.updateSettings { it.copy(darkTheme = !it.darkTheme) }
                            },
                            onSaveCustomer = { customer ->
                                modulesViewModel.upsertCustomer(customer)
                            },
                            onToggleCustomerStatus = { customer ->
                                modulesViewModel.toggleCustomerStatus(customer)
                            },
                            onDeleteCustomer = { customer ->
                                modulesViewModel.deleteCustomer(customer)
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    // Module 3: Tagihan
                    composable(Routes.ROUTE_TAGIHAN) {
                        TagihanScreen(
                            invoices = invoices,
                            packages = packages,
                            ppnPercent = settings.ppnPercent,
                            onBack = { navController.popBackStack() },
                            onCreateInvoice = { cust, pppoe, pkg, area, amt, due, note ->
                                modulesViewModel.addInvoice(cust, pppoe, pkg, area, amt, due, settings.ppnPercent, note)
                            },
                            onMarkPaid = { inv, method ->
                                modulesViewModel.markInvoicePaid(inv, method)
                            },
                            onMarkOverdue = { inv -> modulesViewModel.markInvoiceOverdue(inv) },
                            onDeleteInvoice = { inv -> modulesViewModel.deleteInvoice(inv) }
                        )
                    }

                    // Module 4: Transaksi
                    composable(Routes.ROUTE_TRANSAKSI) {
                        TransaksiScreen(
                            transactions = transactions,
                            onBack = { navController.popBackStack() },
                            onToggleVerified = { tx -> modulesViewModel.toggleTransactionVerified(tx) }
                        )
                    }

                    // Module 5: Mikrotik Multi-Router
                    composable(Routes.ROUTE_ROUTERS) {
                        RoutersScreen(
                            routers = uiState.routers,
                            telemetryByRouter = uiState.telemetryByRouter,
                            customers = customers,
                            darkTheme = settings.darkTheme,
                            onToggleTheme = {
                                viewModel.updateSettings { it.copy(darkTheme = !it.darkTheme) }
                            },
                            onOpenMonitoring = { navController.navigate(Routes.ROUTE_MONITORING) },
                            onAddRouter = ::openAddRouter,
                            onEditRouter = ::openEditRouter,
                            onDeleteRouter = { r ->
                                viewModel.deleteRouter(r)
                                modulesViewModel.logActivity("Mikrotik", "Hapus Router", r.name)
                            },
                            onOpenDetail = { routerId ->
                                navController.navigate(Routes.routerDetail(routerId))
                            },
                            onLoadDemoLab = { viewModel.loadDemoLabRouters() },
                            onToggleSimOffline = { viewModel.toggleSimulatedOffline(it) },
                            onToggleSimCpuSpike = { viewModel.toggleSimulatedCpuSpike(it) },
                            isSimOffline = { viewModel.isSimulatedOffline(it) },
                            isSimCpuSpike = { viewModel.isSimulatedCpuSpike(it) },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    // Module 6: Produk
                    composable(Routes.ROUTE_PRODUK) {
                        ProdukScreen(
                            packages = packages,
                            pppoeClients = uiState.allPppoeClients,
                            isSyncing = isSyncingExternal,
                            darkTheme = settings.darkTheme,
                            onToggleTheme = {
                                viewModel.updateSettings { it.copy(darkTheme = !it.darkTheme) }
                            },
                            onBack = { navController.popBackStack() },
                            onSyncFromMikroTik = { modulesViewModel.syncProfilesFromRouters(uiState.routers) },
                            onSavePackage = { modulesViewModel.upsertPackage(it) },
                            onDeletePackage = { modulesViewModel.deletePackage(it) }
                        )
                    }

                    // Module 7: Area & Maps Hub + Subpages
                    composable(Routes.ROUTE_AREA_MAPS) {
                        AreaMapsHubScreen(
                            darkTheme = settings.darkTheme,
                            onToggleTheme = {
                                viewModel.updateSettings { it.copy(darkTheme = !it.darkTheme) }
                            },
                            onBack = { navController.popBackStack() },
                            onNavigate = { target -> navController.navigate(target) }
                        )
                    }

                    composable(Routes.ROUTE_FTTH_LOKASI) {
                        FtthLokasiScreen(
                            nodes = ftthNodes,
                            customerPins = customerPins,
                            pppoeClients = uiState.allPppoeClients,
                            darkTheme = settings.darkTheme,
                            onToggleTheme = {
                                viewModel.updateSettings { it.copy(darkTheme = !it.darkTheme) }
                            },
                            onRefresh = { viewModel.refreshNow() },
                            onBack = { navController.popBackStack() },
                            onOpenMapsFtth = { navController.navigate(Routes.ROUTE_MAPS_FTTH) },
                            onDeleteNode = { modulesViewModel.deleteFtthNode(it) }
                        )
                    }

                    composable(Routes.ROUTE_FTTH_TOPOLOGI) {
                        FtthTopologiScreen(
                            nodes = ftthNodes,
                            customerPins = customerPins,
                            pppoeClients = uiState.allPppoeClients,
                            darkTheme = settings.darkTheme,
                            onToggleTheme = {
                                viewModel.updateSettings { it.copy(darkTheme = !it.darkTheme) }
                            },
                            onRefresh = { viewModel.refreshNow() },
                            onBack = { navController.popBackStack() },
                            onOpenMapsFtth = { navController.navigate(Routes.ROUTE_MAPS_FTTH) }
                        )
                    }

                    composable(Routes.ROUTE_FTTH_ZONA) {
                        AreaMapsScreen(
                            areas = areas,
                            routers = uiState.routers,
                            onBack = { navController.popBackStack() },
                            onAddArea = { name, code, odp, total, used, coords, routerName ->
                                modulesViewModel.addArea(name, code, odp, total, used, coords, routerName)
                            },
                            onDeleteArea = { modulesViewModel.deleteArea(it) }
                        )
                    }

                    composable(Routes.ROUTE_MAPS_FTTH) {
                        MapsFtthScreen(
                            uiState = uiState,
                            nodes = ftthNodes,
                            customerPins = customerPins,
                            cableRoutes = cableRoutes,
                            darkTheme = settings.darkTheme,
                            onToggleTheme = {
                                viewModel.updateSettings { it.copy(darkTheme = !it.darkTheme) }
                            },
                            onBack = { navController.popBackStack() },
                            onAddNode = { type, name, code, limit, extra, addr, desc, parentId, lat, lng ->
                                modulesViewModel.addFtthNode(type, name, code, limit, extra, addr, desc, parentId, lat, lng)
                            },
                            onMoveNode = { node, lat, lng ->
                                modulesViewModel.updateFtthNodePosition(node, lat, lng)
                            },
                            onDeleteNode = { modulesViewModel.deleteFtthNode(it) },
                            onAddCustomerPin = { name, pppoe, parentId, lat, lng, addr ->
                                modulesViewModel.addCustomerPin(name, pppoe, parentId, lat, lng, addr)
                            },
                            onMoveCustomerPin = { pin, lat, lng ->
                                modulesViewModel.updateCustomerPinPosition(pin, lat, lng)
                            },
                            onDeleteCustomerPin = { modulesViewModel.deleteCustomerPin(it) },
                            onAddCableRoute = { name, pts, meters, cores ->
                                modulesViewModel.addCableRoute(name, pts, meters, cores)
                            },
                            onDeleteCableRoute = { modulesViewModel.deleteCableRoute(it) }
                        )
                    }

                    // Module 8: GenieACS
                    composable(Routes.ROUTE_GENIEACS) {
                        GenieAcsScreen(
                            devices = cpeDevices,
                            genieAcsUrl = settings.genieAcsUrl,
                            isSyncing = isSyncingExternal,
                            onBack = { navController.popBackStack() },
                            onUpdateUrl = { url -> viewModel.updateSettings { it.copy(genieAcsUrl = url) } },
                            onSyncGenieAcs = { url -> modulesViewModel.syncGenieAcs(url) },
                            onAddManualCpe = { sn, model, cust, ip, rx, ssid ->
                                modulesViewModel.addManualCpe(sn, model, cust, ip, rx, ssid)
                            },
                            onRebootCpe = { dev -> modulesViewModel.rebootCpe(settings.genieAcsUrl, dev) }
                        )
                    }

                    // Module 9: Radius
                    composable(Routes.ROUTE_RADIUS) {
                        RadiusScreen(
                            radiusEntries = radiusEntries,
                            pppoeClients = uiState.allPppoeClients,
                            isSyncing = isSyncingExternal,
                            onBack = { navController.popBackStack() },
                            onRefreshRadius = { modulesViewModel.refreshRadiusFromRouters(uiState.routers) }
                        )
                    }

                    // Module 10: Laporan Pajak
                    composable(Routes.ROUTE_LAPORAN_PAJAK) {
                        LaporanPajakScreen(
                            invoices = invoices,
                            ppnPercent = settings.ppnPercent,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    // Module 11: Sales
                    composable(Routes.ROUTE_SALES) {
                        SalesScreen(
                            salesReps = salesReps,
                            onBack = { navController.popBackStack() },
                            onAddSalesRep = { name, phone, area, target, done ->
                                modulesViewModel.addSalesRep(name, phone, area, target, done)
                            },
                            onIncrementInstall = { modulesViewModel.incrementSalesInstall(it) },
                            onDeleteSalesRep = { modulesViewModel.deleteSalesRep(it) }
                        )
                    }

                    // Module 12: Log Aktivitas (opened from Module Grid with Back arrow)
                    composable(Routes.ROUTE_LOG_AKTIVITAS) {
                        LogAktivitasScreen(
                            logs = activityLogs,
                            showBackButton = true,
                            onBack = { navController.popBackStack() },
                            onClearLogs = { modulesViewModel.clearActivityLogs() }
                        )
                    }

                    // Module 13: Tiket
                    composable(Routes.ROUTE_TIKET) {
                        TiketScreen(
                            tickets = tickets,
                            onBack = { navController.popBackStack() },
                            onAddTicket = { cust, subj, desc, prio, tech ->
                                modulesViewModel.addTicket(cust, subj, desc, prio, tech)
                            },
                            onAdvanceStatus = { modulesViewModel.advanceTicketStatus(it) },
                            onDeleteTicket = { modulesViewModel.deleteTicket(it) }
                        )
                    }

                    // Router Detail Screen
                    composable(
                        route = Routes.ROUTE_ROUTER_DETAIL,
                        arguments = listOf(navArgument("routerId") { type = NavType.LongType })
                    ) { entry ->
                        val routerId = entry.arguments?.getLong("routerId") ?: 0L
                        val router = uiState.routers.firstOrNull { it.id == routerId }
                        val telemetry = uiState.telemetryByRouter[routerId]

                        RouterDetailScreen(
                            router = router,
                            telemetry = telemetry,
                            customers = customers,
                            isRefreshing = uiState.isRefreshing,
                            darkTheme = settings.darkTheme,
                            onToggleTheme = {
                                viewModel.updateSettings { it.copy(darkTheme = !it.darkTheme) }
                            },
                            onEditRouter = ::openEditRouter,
                            onBack = { navController.popBackStack() },
                            onRefresh = {
                                if (router != null) viewModel.inspectRouter(router) else viewModel.refreshNow()
                            },
                            onToggleSecretDisabled = { r, sec ->
                                viewModel.toggleSecretDisabledById(r, sec.id, sec.name, sec.isDisabled)
                                modulesViewModel.logActivity("Mikrotik", "Ubah Secret", sec.name)
                            },
                            onRebootRouter = { r ->
                                viewModel.rebootRouter(r)
                                modulesViewModel.logActivity("Mikrotik", "Reboot Router", r.name)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showEditorSheet) {
        RouterEditorSheet(
            initialRouter = editingRouter,
            darkTheme = settings.darkTheme,
            onToggleTheme = {
                viewModel.updateSettings { it.copy(darkTheme = !it.darkTheme) }
            },
            onSave = { saved ->
                viewModel.saveRouter(saved) {
                    showEditorSheet = false
                    modulesViewModel.logActivity("Mikrotik", "Simpan Router", "${saved.name} (${saved.host})")
                }
            },
            onDismiss = { showEditorSheet = false }
        )
    }
}

@Composable
private fun RoundedNocBottomBar(
    items: List<BottomTabItem>,
    currentDestination: androidx.navigation.NavDestination?,
    onSelect: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                val tint = if (selected) NocAmber else MaterialTheme.colorScheme.onSurfaceVariant

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (selected) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
                            else Color.Transparent
                        )
                        .clickable { onSelect(item.route) }
                        .testTag(item.testTag),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                        tint = tint,
                        modifier = Modifier.size(21.dp)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = item.label,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = tint,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
