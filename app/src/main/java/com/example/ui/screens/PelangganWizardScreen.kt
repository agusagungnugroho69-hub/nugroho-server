package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.Apartment
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.data.model.CustomerEntity
import com.example.data.model.FtthNodeEntity
import com.example.data.model.InternetPackageEntity
import com.example.data.model.PppSecretInfo
import com.example.data.model.RouterConfig
import com.example.data.model.RouterTelemetry
import com.example.data.model.SalesRepEntity
import com.example.data.repository.IndonesiaWilayahRepository
import com.example.data.repository.WilayahItem
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.NocAmber
import com.example.ui.theme.NocEmeraldOnline
import com.example.ui.theme.NocRedOffline
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

private enum class RegionPickerTarget(val title: String, val searchPlaceholder: String) {
    PROVINCE("Pilih provinsi", "Cari provinsi..."),
    REGENCY("Pilih kabupaten/kota", "Cari kabupaten/kota..."),
    DISTRICT("Pilih kecamatan", "Cari kecamatan..."),
    VILLAGE("Pilih desa/kelurahan", "Cari desa/kelurahan...")
}

private enum class ConnectionPickerTarget {
    SERVER, SECRET, PRODUCT, SALES
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PelangganWizardScreen(
    initialCustomer: CustomerEntity?,
    existingCustomers: List<CustomerEntity>,
    ftthNodes: List<FtthNodeEntity>,
    routers: List<RouterConfig>,
    telemetryByRouter: Map<Long, RouterTelemetry>,
    packages: List<InternetPackageEntity>,
    salesReps: List<SalesRepEntity>,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onSave: (CustomerEntity) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val isEdit = initialCustomer != null

    var step by remember { mutableIntStateOf(1) }
    var showMapPicker by remember { mutableStateOf(false) }
    var activeRegionPicker by remember { mutableStateOf<RegionPickerTarget?>(null) }
    var activeConnPicker by remember { mutableStateOf<ConnectionPickerTarget?>(null) }
    var showInstallDatePicker by remember { mutableStateOf(false) }

    // --- Step 1: Data Dasar ---
    var fullName by remember { mutableStateOf(initialCustomer?.name ?: "") }
    val defaultIdentity = remember(initialCustomer, existingCustomers) {
        if (!initialCustomer?.customerCode.isNullOrBlank()) {
            initialCustomer!!.customerCode
        } else {
            generateUniqueSixDigitId(existingCustomers.map { it.customerCode }.toSet())
        }
    }
    var identityCode by remember { mutableStateOf(defaultIdentity) }
    var phone by remember { mutableStateOf(initialCustomer?.phone ?: "") }
    var email by remember { mutableStateOf(initialCustomer?.email ?: "") }
    var emailManuallyEdited by remember {
        mutableStateOf(initialCustomer != null && initialCustomer.email.isNotBlank())
    }
    var waNotify by remember { mutableStateOf(initialCustomer?.whatsappNotifyEnabled ?: false) }

    // --- Step 2: Distribusi ---
    var distributionSearch by remember { mutableStateOf("") }
    var selectedNodeId by remember { mutableStateOf(initialCustomer?.distributionNodeId) }

    // --- Step 3: Alamat & Wilayah ---
    var address by remember { mutableStateOf(initialCustomer?.address ?: "") }
    var dusun by remember { mutableStateOf(initialCustomer?.dusun ?: "") }
    var selectedProvince by remember {
        mutableStateOf<WilayahItem?>(
            initialCustomer?.province?.takeIf { it.isNotBlank() }?.let { pname ->
                IndonesiaWilayahRepository.provinces.firstOrNull { it.name.equals(pname, ignoreCase = true) }
                    ?: WilayahItem("custom_prov", pname)
            }
        )
    }
    var selectedRegency by remember {
        mutableStateOf<WilayahItem?>(
            initialCustomer?.regency?.takeIf { it.isNotBlank() }?.let { WilayahItem("custom_reg", it) }
        )
    }
    var selectedDistrict by remember {
        mutableStateOf<WilayahItem?>(
            initialCustomer?.district?.takeIf { it.isNotBlank() }?.let { WilayahItem("custom_dist", it) }
        )
    }
    var selectedVillage by remember {
        mutableStateOf<WilayahItem?>(
            initialCustomer?.village?.takeIf { it.isNotBlank() }?.let { WilayahItem("custom_vill", it) }
        )
    }

    var regencyList by remember { mutableStateOf<List<WilayahItem>>(emptyList()) }
    var districtList by remember { mutableStateOf<List<WilayahItem>>(emptyList()) }
    var villageList by remember { mutableStateOf<List<WilayahItem>>(emptyList()) }
    var loadingRegion by remember { mutableStateOf(false) }

    LaunchedEffect(selectedProvince?.id) {
        val prov = selectedProvince
        if (prov == null) {
            regencyList = emptyList()
        } else {
            loadingRegion = true
            regencyList = IndonesiaWilayahRepository.getRegencies(prov)
            loadingRegion = false
        }
    }

    LaunchedEffect(selectedRegency?.id) {
        val reg = selectedRegency
        if (reg == null) {
            districtList = emptyList()
        } else {
            loadingRegion = true
            districtList = IndonesiaWilayahRepository.getDistricts(reg)
            loadingRegion = false
        }
    }

    LaunchedEffect(selectedDistrict?.id) {
        val dist = selectedDistrict
        if (dist == null) {
            villageList = emptyList()
        } else {
            loadingRegion = true
            villageList = IndonesiaWilayahRepository.getVillages(dist)
            loadingRegion = false
        }
    }

    // --- Step 4: Koneksi ---
    var selectedRouterId by remember {
        mutableStateOf(
            initialCustomer?.routerId?.takeIf { it != 0L } ?: routers.firstOrNull()?.id ?: 0L
        )
    }
    var pppoeUsername by remember { mutableStateOf(initialCustomer?.pppoeUsername ?: "") }
    var pppoePassword by remember { mutableStateOf(initialCustomer?.pppoePassword ?: "") }
    var passwordVisible by remember { mutableStateOf(false) }
    var selectedPackageId by remember {
        mutableStateOf(
            initialCustomer?.packageId
                ?: packages.firstOrNull { it.name == initialCustomer?.packageName }?.id
                ?: packages.firstOrNull()?.id
        )
    }
    var exemptAutoIsolir by remember { mutableStateOf(initialCustomer?.exemptAutoIsolir ?: false) }

    // --- Step 5: Lokasi ---
    var latText by remember {
        mutableStateOf(
            initialCustomer?.latitude?.let { String.format(Locale.US, "%.6f", it) } ?: ""
        )
    }
    var lngText by remember {
        mutableStateOf(
            initialCustomer?.longitude?.let { String.format(Locale.US, "%.6f", it) } ?: ""
        )
    }

    // --- Step 6: Billing ---
    var statusLabel by remember { mutableStateOf(initialCustomer?.statusLabel ?: "Aktif") }
    var installDateMillis by remember { mutableStateOf(initialCustomer?.installDateMillis) }
    var installDateLabel by remember { mutableStateOf(initialCustomer?.installDateLabel ?: "") }
    var selectedSalesId by remember { mutableStateOf(initialCustomer?.salesId) }
    var customIsolirEnabled by remember { mutableStateOf(initialCustomer?.customIsolirEnabled ?: false) }
    var customIsolirDayText by remember {
        mutableStateOf((initialCustomer?.customIsolirDay ?: 28).toString())
    }
    var customInvoiceEnabled by remember { mutableStateOf(initialCustomer?.customInvoiceEnabled ?: false) }
    var customInvoiceDayText by remember {
        mutableStateOf((initialCustomer?.customInvoiceDay ?: 28).toString())
    }

    val selectedNode = remember(ftthNodes, selectedNodeId) {
        ftthNodes.firstOrNull { it.id == selectedNodeId }
    }
    val selectedRouter = remember(routers, selectedRouterId) {
        routers.firstOrNull { it.id == selectedRouterId }
    }
    val routerTelemetry = telemetryByRouter[selectedRouterId]
    val routerSecrets = remember(routerTelemetry) {
        routerTelemetry?.pppSecrets.orEmpty()
    }
    val selectedPackage = remember(packages, selectedPackageId) {
        packages.firstOrNull { it.id == selectedPackageId }
    }
    val selectedSales = remember(salesReps, selectedSalesId) {
        salesReps.firstOrNull { it.id == selectedSalesId }
    }

    val parsedLat = latText.trim().replace(",", ".").toDoubleOrNull()
    val parsedLng = lngText.trim().replace(",", ".").toDoubleOrNull()

    val stepGpsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            fetchDeviceLocation(context) { pt ->
                latText = String.format(Locale.US, "%.6f", pt.lat)
                lngText = String.format(Locale.US, "%.6f", pt.lng)
            }
        } else {
            Toast.makeText(context, "Izin lokasi diperlukan untuk mengambil GPS.", Toast.LENGTH_SHORT).show()
        }
    }

    fun requestStepGps() {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (hasFine || hasCoarse) {
            fetchDeviceLocation(context) { pt ->
                latText = String.format(Locale.US, "%.6f", pt.lat)
                lngText = String.format(Locale.US, "%.6f", pt.lng)
            }
        } else {
            stepGpsLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    if (showMapPicker) {
        PelangganMapPickerScreen(
            initialLat = parsedLat,
            initialLng = parsedLng,
            selectedNode = selectedNode,
            ftthNodes = ftthNodes,
            customers = existingCustomers,
            darkTheme = darkTheme,
            onToggleTheme = onToggleTheme,
            onConfirm = { lat, lng ->
                latText = String.format(Locale.US, "%.6f", lat)
                lngText = String.format(Locale.US, "%.6f", lng)
                showMapPicker = false
            },
            onBack = { showMapPicker = false }
        )
        return
    }

    BackHandler {
        if (step > 1) step-- else onCancel()
    }

    val stepTitle = when (step) {
        1 -> "Data dasar"
        2 -> "Distribusi"
        3 -> "Alamat"
        4 -> "Koneksi"
        5 -> "Lokasi"
        else -> "Billing"
    }

    fun finishWizard() {
        val cleanName = fullName.trim()
        if (cleanName.isBlank()) {
            Toast.makeText(context, "Nama lengkap pelanggan wajib diisi.", Toast.LENGTH_SHORT).show()
            step = 1
            return
        }
        val cleanUser = pppoeUsername.trim()
        val matchedSecret = routerSecrets.any { it.name.equals(cleanUser, ignoreCase = true) }
        val isSynced = selectedRouter != null && cleanUser.isNotBlank() &&
            (routerTelemetry?.isOnline == true || matchedSecret)
        val isolirDay = (customIsolirDayText.toIntOrNull() ?: 28).coerceIn(1, 31)
        val invoiceDay = (customInvoiceDayText.toIntOrNull() ?: isolirDay).coerceIn(1, 31)

        val entity = CustomerEntity(
            id = initialCustomer?.id ?: 0L,
            name = cleanName,
            pppoeUsername = cleanUser,
            pppoePassword = pppoePassword,
            customerCode = identityCode.ifBlank { defaultIdentity },
            phone = phone.trim(),
            email = email.trim(),
            whatsappNotifyEnabled = waNotify,
            distributionNodeId = selectedNode?.id,
            distributionLabel = selectedNode?.name ?: "",
            address = address.trim(),
            dusun = dusun.trim(),
            province = selectedProvince?.name ?: "",
            regency = selectedRegency?.name ?: "",
            district = selectedDistrict?.name ?: "",
            village = selectedVillage?.name ?: "",
            connectionModel = "PPPoE",
            routerId = selectedRouter?.id ?: 0L,
            routerName = selectedRouter?.name ?: "",
            packageId = selectedPackage?.id,
            packageName = selectedPackage?.name ?: "",
            exemptAutoIsolir = exemptAutoIsolir,
            latitude = parsedLat,
            longitude = parsedLng,
            statusLabel = statusLabel,
            installDateMillis = installDateMillis,
            installDateLabel = installDateLabel,
            salesId = selectedSales?.id,
            salesName = selectedSales?.name ?: "",
            customIsolirEnabled = customIsolirEnabled,
            customIsolirDay = isolirDay,
            customInvoiceEnabled = customIsolirEnabled && customInvoiceEnabled,
            customInvoiceDay = invoiceDay,
            ispLabel = selectedNode?.code ?: "",
            isSyncedToRouter = isSynced,
            isActive = statusLabel == "Aktif",
            createdAt = initialCustomer?.createdAt ?: System.currentTimeMillis()
        )
        onSave(entity)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header: < Tambah Pelanggan + Theme Toggle
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
                        .clickable {
                            if (step > 1) step-- else onCancel()
                        }
                        .testTag("wizard_back_button"),
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
                    text = if (isEdit) "Edit Pelanggan" else "Tambah Pelanggan",
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
                    contentDescription = "Ubah tema",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(19.dp)
                )
            }
        }

        // Step label & 6-segment horizontal progress indicator
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Langkah $step dari 6 — $stepTitle",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (i in 1..6) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (i <= step) NocAmber
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
                            )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (step) {
                1 -> Step1DataDasarCard(
                    fullName = fullName,
                    onFullNameChange = { nextName ->
                        fullName = nextName
                        if (!emailManuallyEdited) {
                            email = generateNugrohoEmail(nextName)
                        }
                    },
                    identityCode = identityCode,
                    onIdentityChange = { nextId ->
                        identityCode = nextId.filter { it.isDigit() }.take(12)
                    },
                    onRegenerateIdentity = {
                        identityCode = generateUniqueSixDigitId(existingCustomers.map { it.customerCode }.toSet())
                    },
                    phone = phone,
                    onPhoneChange = { phone = it },
                    email = email,
                    onEmailChange = { nextEmail ->
                        emailManuallyEdited = nextEmail.isNotBlank()
                        email = nextEmail
                    },
                    waNotify = waNotify,
                    onWaNotifyChange = { waNotify = it }
                )

                2 -> Step2DistribusiCard(
                    nodes = ftthNodes,
                    customers = existingCustomers,
                    searchQuery = distributionSearch,
                    onSearchChange = { distributionSearch = it },
                    selectedNodeId = selectedNodeId,
                    onSelectNode = { id ->
                        selectedNodeId = if (selectedNodeId == id) null else id
                    }
                )

                3 -> Step3AlamatCard(
                    address = address,
                    onAddressChange = { address = it },
                    dusun = dusun,
                    onDusunChange = { dusun = it },
                    provinceName = selectedProvince?.name,
                    regencyName = selectedRegency?.name,
                    districtName = selectedDistrict?.name,
                    villageName = selectedVillage?.name,
                    onPickProvince = { activeRegionPicker = RegionPickerTarget.PROVINCE },
                    onPickRegency = {
                        if (selectedProvince == null) {
                            Toast.makeText(context, "Pilih provinsi terlebih dahulu.", Toast.LENGTH_SHORT).show()
                        } else {
                            activeRegionPicker = RegionPickerTarget.REGENCY
                        }
                    },
                    onPickDistrict = {
                        if (selectedRegency == null) {
                            Toast.makeText(context, "Pilih kabupaten/kota terlebih dahulu.", Toast.LENGTH_SHORT).show()
                        } else {
                            activeRegionPicker = RegionPickerTarget.DISTRICT
                        }
                    },
                    onPickVillage = {
                        if (selectedDistrict == null) {
                            Toast.makeText(context, "Pilih kecamatan terlebih dahulu.", Toast.LENGTH_SHORT).show()
                        } else {
                            activeRegionPicker = RegionPickerTarget.VILLAGE
                        }
                    }
                )

                4 -> Step4KoneksiCard(
                    selectedRouter = selectedRouter,
                    isRouterOnline = routerTelemetry?.isOnline == true,
                    pppoeUsername = pppoeUsername,
                    onUsernameChange = { pppoeUsername = it },
                    pppoePassword = pppoePassword,
                    onPasswordChange = { pppoePassword = it },
                    passwordVisible = passwordVisible,
                    onTogglePasswordVisible = { passwordVisible = !passwordVisible },
                    selectedPackage = selectedPackage,
                    exemptAutoIsolir = exemptAutoIsolir,
                    onExemptChange = { exemptAutoIsolir = it },
                    onOpenServerPicker = { activeConnPicker = ConnectionPickerTarget.SERVER },
                    onOpenSecretPicker = {
                        if (selectedRouter == null) {
                            Toast.makeText(context, "Tambahkan atau pilih Server MikroTik terlebih dahulu.", Toast.LENGTH_SHORT).show()
                        } else {
                            activeConnPicker = ConnectionPickerTarget.SECRET
                        }
                    },
                    onOpenProductPicker = { activeConnPicker = ConnectionPickerTarget.PRODUCT }
                )

                5 -> Step5LokasiCard(
                    latText = latText,
                    lngText = lngText,
                    onLatChange = { latText = it },
                    onLngChange = { lngText = it },
                    selectedNode = selectedNode,
                    hasCoordinates = parsedLat != null && parsedLng != null,
                    onOpenMapPicker = { showMapPicker = true },
                    onGpsClick = { requestStepGps() },
                    onPasteClick = {
                        val clip = clipboard.getText()?.text.orEmpty()
                        val parsed = parseCoordinatePair(clip)
                        if (parsed != null) {
                            latText = String.format(Locale.US, "%.6f", parsed.first)
                            lngText = String.format(Locale.US, "%.6f", parsed.second)
                            Toast.makeText(context, "Koordinat ditempel.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Clipboard tidak berisi format koordinat (lat, lng).", Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                else -> Step6BillingCard(
                    statusLabel = statusLabel,
                    onStatusChange = { statusLabel = it },
                    installDateLabel = installDateLabel,
                    onPickInstallDate = { showInstallDatePicker = true },
                    selectedSalesName = selectedSales?.name ?: "Tanpa sales",
                    onPickSales = { activeConnPicker = ConnectionPickerTarget.SALES },
                    customIsolirEnabled = customIsolirEnabled,
                    onCustomIsolirEnabledChange = { enabled ->
                        customIsolirEnabled = enabled
                        if (!enabled) customInvoiceEnabled = false
                    },
                    customIsolirDayText = customIsolirDayText,
                    onCustomIsolirDayChange = { txt ->
                        val digits = txt.filter { it.isDigit() }.take(2)
                        val clamped = digits.toIntOrNull()?.coerceIn(1, 31)?.toString() ?: ""
                        customIsolirDayText = if (txt.isEmpty()) "" else clamped
                    },
                    customInvoiceEnabled = customInvoiceEnabled,
                    onCustomInvoiceEnabledChange = { customInvoiceEnabled = it },
                    customInvoiceDayText = customInvoiceDayText,
                    onCustomInvoiceDayChange = { txt ->
                        val digits = txt.filter { it.isDigit() }.take(2)
                        val clamped = digits.toIntOrNull()?.coerceIn(1, 31)?.toString() ?: ""
                        customInvoiceDayText = if (txt.isEmpty()) "" else clamped
                    }
                )
            }

            // Navigation Footer Buttons
            if (step == 1) {
                Button(
                    onClick = {
                        if (fullName.trim().isBlank()) {
                            Toast.makeText(context, "Isi nama lengkap pelanggan terlebih dahulu.", Toast.LENGTH_SHORT).show()
                        } else {
                            step = 2
                        }
                    },
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NocAmber,
                        contentColor = Color(0xFF0B0E14)
                    ),
                    contentPadding = PaddingValues(vertical = 15.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wizard_next_button")
                ) {
                    Text("Lanjut", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { step-- },
                        shape = RoundedCornerShape(26.dp),
                        border = BorderStroke(1.2.dp, NocAmber),
                        contentPadding = PaddingValues(vertical = 15.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("wizard_prev_button")
                    ) {
                        Text("Kembali", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NocAmber)
                    }

                    Button(
                        onClick = {
                            if (step < 6) step++ else finishWizard()
                        },
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NocAmber,
                            contentColor = Color(0xFF0B0E14)
                        ),
                        contentPadding = PaddingValues(vertical = 15.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag(if (step == 6) "wizard_finish_button" else "wizard_next_button")
                    ) {
                        Text(
                            text = if (step == 6) {
                                if (isEdit) "Simpan Perubahan" else "Tambah Pelanggan"
                            } else {
                                "Lanjut"
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // --- Modal Pickers ---
    activeRegionPicker?.let { target ->
        val items = when (target) {
            RegionPickerTarget.PROVINCE -> IndonesiaWilayahRepository.provinces
            RegionPickerTarget.REGENCY -> regencyList
            RegionPickerTarget.DISTRICT -> districtList
            RegionPickerTarget.VILLAGE -> villageList
        }
        val currentName = when (target) {
            RegionPickerTarget.PROVINCE -> selectedProvince?.name
            RegionPickerTarget.REGENCY -> selectedRegency?.name
            RegionPickerTarget.DISTRICT -> selectedDistrict?.name
            RegionPickerTarget.VILLAGE -> selectedVillage?.name
        }
        SearchableWilayahPickerDialog(
            title = target.title,
            searchPlaceholder = target.searchPlaceholder,
            items = items,
            selectedName = currentName,
            isLoading = loadingRegion && target != RegionPickerTarget.PROVINCE,
            allowCustomEntry = target != RegionPickerTarget.PROVINCE,
            onSelect = { chosen ->
                when (target) {
                    RegionPickerTarget.PROVINCE -> {
                        if (selectedProvince?.name != chosen.name) {
                            selectedProvince = chosen
                            selectedRegency = null
                            selectedDistrict = null
                            selectedVillage = null
                        }
                    }
                    RegionPickerTarget.REGENCY -> {
                        if (selectedRegency?.name != chosen.name) {
                            selectedRegency = chosen
                            selectedDistrict = null
                            selectedVillage = null
                        }
                    }
                    RegionPickerTarget.DISTRICT -> {
                        if (selectedDistrict?.name != chosen.name) {
                            selectedDistrict = chosen
                            selectedVillage = null
                        }
                    }
                    RegionPickerTarget.VILLAGE -> {
                        selectedVillage = chosen
                    }
                }
                activeRegionPicker = null
            },
            onDismiss = { activeRegionPicker = null }
        )
    }

    activeConnPicker?.let { target ->
        when (target) {
            ConnectionPickerTarget.SERVER -> {
                SearchableSimplePickerDialog(
                    title = "Pilih Server MikroTik",
                    searchPlaceholder = "Cari server / IP...",
                    emptyMessage = "Belum ada server MikroTik yang disimpan. Tambahkan server melalui menu Mikrotik.",
                    items = routers,
                    matches = { r, q -> r.name.lowercase().contains(q) || r.host.lowercase().contains(q) },
                    isSelected = { it.id == selectedRouterId },
                    itemTitle = { it.name },
                    itemSubtitle = { r ->
                        val online = telemetryByRouter[r.id]?.isOnline == true
                        "${r.host}:${r.port} • ${if (online) "Terhubung" else "Terputus"}"
                    },
                    onSelect = { r ->
                        selectedRouterId = r.id
                        activeConnPicker = null
                    },
                    onDismiss = { activeConnPicker = null }
                )
            }
            ConnectionPickerTarget.SECRET -> {
                SearchableSimplePickerDialog(
                    title = "Ambil Secret MikroTik",
                    searchPlaceholder = "Cari secret username / profile...",
                    emptyMessage = if (routerTelemetry?.isOnline != true) {
                        "Router ${selectedRouter?.name ?: ""} sedang terputus atau secret belum tersinkron."
                    } else {
                        "Tidak ada PPP Secret di ${selectedRouter?.name ?: "router ini"}."
                    },
                    items = routerSecrets,
                    matches = { s, q ->
                        s.name.lowercase().contains(q) || s.profile.lowercase().contains(q)
                    },
                    isSelected = { it.name.equals(pppoeUsername, ignoreCase = true) },
                    itemTitle = { it.name },
                    itemSubtitle = { s ->
                        val stateTxt = when {
                            s.isDisabled -> "Disabled"
                            s.isOnline -> "Online (${s.activeAddress})"
                            else -> "Offline"
                        }
                        val pwHint = if (s.password.isNotBlank()) " • Password tersinkron" else ""
                        "Profile: ${s.profile} • $stateTxt$pwHint"
                    },
                    onSelect = { secret ->
                        pppoeUsername = secret.name
                        if (secret.password.isNotBlank()) {
                            pppoePassword = secret.password
                        }
                        // Also match saved product if pppoeProfile matches secret.profile
                        val matchedPkg = packages.firstOrNull {
                            it.pppoeProfile.equals(secret.profile, ignoreCase = true) ||
                                it.name.equals(secret.profile, ignoreCase = true)
                        }
                        if (matchedPkg != null && selectedPackageId == null) {
                            selectedPackageId = matchedPkg.id
                        }
                        activeConnPicker = null
                    },
                    onDismiss = { activeConnPicker = null }
                )
            }
            ConnectionPickerTarget.PRODUCT -> {
                val nf = NumberFormat.getNumberInstance(Locale("id", "ID"))
                SearchableSimplePickerDialog(
                    title = "Pilih Produk",
                    searchPlaceholder = "Cari nama produk / profile...",
                    emptyMessage = "Belum ada produk yang disimpan di Menu Produk.",
                    items = packages,
                    matches = { p, q ->
                        p.name.lowercase().contains(q) || p.pppoeProfile.lowercase().contains(q)
                    },
                    isSelected = { it.id == selectedPackageId },
                    itemTitle = { it.name },
                    itemSubtitle = { p ->
                        "Rp ${nf.format(p.monthlyPrice)} • ${p.downloadMbps}/${p.uploadMbps} Mbps • ${p.pppoeProfile}"
                    },
                    onSelect = { p ->
                        selectedPackageId = p.id
                        activeConnPicker = null
                    },
                    onDismiss = { activeConnPicker = null }
                )
            }
            ConnectionPickerTarget.SALES -> {
                val options: List<SalesRepEntity?> = listOf(null) + salesReps
                SearchableSimplePickerDialog(
                    title = "Pilih Sales (Opsional)",
                    searchPlaceholder = "Cari nama sales...",
                    emptyMessage = "Belum ada sales terdaftar.",
                    items = options,
                    matches = { s, q ->
                        if (s == null) "tanpa sales".contains(q)
                        else s.name.lowercase().contains(q) || s.areaAssigned.lowercase().contains(q)
                    },
                    isSelected = { it?.id == selectedSalesId },
                    itemTitle = { it?.name ?: "Tanpa sales" },
                    itemSubtitle = { s ->
                        if (s == null) "Pelanggan dipasang tanpa sales terdaftar"
                        else "${s.phone.ifBlank { "-" }} • Area: ${s.areaAssigned.ifBlank { "Umum" }}"
                    },
                    onSelect = { s ->
                        selectedSalesId = s?.id
                        activeConnPicker = null
                    },
                    onDismiss = { activeConnPicker = null }
                )
            }
        }
    }

    if (showInstallDatePicker) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = installDateMillis ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showInstallDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val ms = dateState.selectedDateMillis
                        if (ms != null) {
                            installDateMillis = ms
                            installDateLabel = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")).format(Date(ms))
                        }
                        showInstallDatePicker = false
                    }
                ) {
                    Text("Pilih", color = NocAmber, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showInstallDatePicker = false }) {
                    Text("Batal")
                }
            }
        ) {
            DatePicker(state = dateState)
        }
    }
}

// --- STEP 1: DATA DASAR (Gambar 2) ---
@Composable
private fun Step1DataDasarCard(
    fullName: String,
    onFullNameChange: (String) -> Unit,
    identityCode: String,
    onIdentityChange: (String) -> Unit,
    onRegenerateIdentity: () -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    waNotify: Boolean,
    onWaNotifyChange: (Boolean) -> Unit
) {
    WizardSectionCard(title = "Data dasar") {
        WizardInputFieldRow(
            icon = Icons.Outlined.Person,
            label = "Nama lengkap",
            value = fullName,
            onValueChange = onFullNameChange,
            placeholder = "Nama pelanggan",
            testTag = "wizard_fullname_input"
        )

        WizardInputFieldRow(
            icon = Icons.Outlined.Badge,
            label = "Identitas",
            value = identityCode,
            onValueChange = onIdentityChange,
            placeholder = "6 digit angka",
            keyboardType = KeyboardType.Number,
            trailingContent = {
                IconButton(
                    onClick = onRegenerateIdentity,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Generate ulang ID 6 digit",
                        tint = NocAmber,
                        modifier = Modifier.size(18.dp)
                    )
                }
            },
            testTag = "wizard_identity_input"
        )

        WizardInputFieldRow(
            icon = Icons.Outlined.Phone,
            label = "Nomor telepon",
            value = phone,
            onValueChange = onPhoneChange,
            placeholder = "08xxxxxxxxxx",
            keyboardType = KeyboardType.Phone,
            testTag = "wizard_phone_input"
        )

        WizardInputFieldRow(
            icon = Icons.Outlined.Email,
            label = "Email (opsional)",
            value = email,
            onValueChange = onEmailChange,
            placeholder = "namapelanggan@nugroho.co.id",
            keyboardType = KeyboardType.Email,
            testTag = "wizard_email_input"
        )

        Text(
            text = "WhatsApp belum terhubung. Notifikasi dinonaktifkan.",
            fontSize = 12.sp,
            color = NocRedOffline,
            modifier = Modifier.padding(top = 2.dp)
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f), RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Notifikasi WhatsApp pelanggan baru",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Gateway WhatsApp belum terhubung.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = waNotify,
                    onCheckedChange = onWaNotifyChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = NocAmber
                    )
                )
            }
        }
    }
}

// --- STEP 2: DISTRIBUSI (Gambar 3) ---
@Composable
private fun Step2DistribusiCard(
    nodes: List<FtthNodeEntity>,
    customers: List<CustomerEntity>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedNodeId: Long?,
    onSelectNode: (Long) -> Unit
) {
    val filtered = remember(nodes, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) nodes
        else nodes.filter {
            it.name.lowercase().contains(q) ||
                it.code.lowercase().contains(q) ||
                it.type.label.lowercase().contains(q) ||
                it.address.lowercase().contains(q)
        }
    }

    WizardSectionCard(title = "Distribusi") {
        // Search bar matching Gambar 3
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f), RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
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
                Spacer(modifier = Modifier.width(10.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Cari distribusi...",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        singleLine = true,
                        textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp),
                        cursorBrush = SolidColor(NocAmber),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        if (nodes.isEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
            ) {
                Text(
                    text = "Belum ada ODP, ODC, atau OLT yang tersimpan di Area & Maps. Anda dapat melanjutkan atau menambah titik distribusi di Maps & FTTH.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else if (filtered.isEmpty()) {
            Text(
                text = "Tidak ada titik distribusi yang cocok.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                filtered.forEach { node ->
                    val isSelected = node.id == selectedNodeId
                    val linkedCustomers = customers.count { it.distributionNodeId == node.id }
                    val effectiveUsed = maxOf(node.usedPorts, linkedCustomers)
                    val remaining = (node.limitPort - effectiveUsed).coerceAtLeast(0)

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) NocAmber else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { onSelectNode(node.id) }
                            .testTag("dist_node_${node.id}"),
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) NocAmber.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = NocAmber.copy(alpha = 0.16f)
                                    ) {
                                        Text(
                                            text = node.type.label.uppercase(),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NocAmber,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                        )
                                    }
                                    Text(
                                        text = node.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = "Terpilih",
                                        tint = NocAmber,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Text(
                                text = node.code.ifBlank { "-" },
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                DistMetricColumn("TERSISA", remaining.toString(), NocEmeraldOnline)
                                DistMetricColumn(
                                    "EXTRA",
                                    node.extraPort.toString(),
                                    if (node.extraPort > 0) NocEmeraldOnline else NocRedOffline
                                )
                                DistMetricColumn("PELANGGAN", linkedCustomers.toString(), NocAmber)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DistMetricColumn(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

// --- STEP 3: ALAMAT (Gambar 4 & 5) ---
@Composable
private fun Step3AlamatCard(
    address: String,
    onAddressChange: (String) -> Unit,
    dusun: String,
    onDusunChange: (String) -> Unit,
    provinceName: String?,
    regencyName: String?,
    districtName: String?,
    villageName: String?,
    onPickProvince: () -> Unit,
    onPickRegency: () -> Unit,
    onPickDistrict: () -> Unit,
    onPickVillage: () -> Unit
) {
    WizardSectionCard(title = "Alamat") {
        WizardInputFieldRow(
            icon = Icons.Outlined.Home,
            label = "Alamat",
            value = address,
            onValueChange = onAddressChange,
            placeholder = "Nama jalan / RT RW",
            testTag = "wizard_address_input"
        )

        WizardInputFieldRow(
            icon = Icons.Outlined.LocationOn,
            label = "Dusun",
            value = dusun,
            onValueChange = onDusunChange,
            placeholder = "Nama dusun",
            testTag = "wizard_dusun_input"
        )

        WizardPickerRow(
            icon = Icons.Outlined.Map,
            label = "Provinsi",
            value = provinceName ?: "Pilih provinsi",
            isPlaceholder = provinceName == null,
            enabled = true,
            onClick = onPickProvince,
            testTag = "wizard_pick_province"
        )

        WizardPickerRow(
            icon = Icons.Rounded.Apartment,
            label = "Kabupaten/Kota",
            value = regencyName ?: if (provinceName == null) "Pilih provinsi dulu" else "Pilih kabupaten/kota",
            isPlaceholder = regencyName == null,
            enabled = provinceName != null,
            onClick = onPickRegency,
            testTag = "wizard_pick_regency"
        )

        WizardPickerRow(
            icon = Icons.Rounded.AccountTree,
            label = "Kecamatan",
            value = districtName ?: if (regencyName == null) "Pilih kabupaten dulu" else "Pilih kecamatan",
            isPlaceholder = districtName == null,
            enabled = regencyName != null,
            onClick = onPickDistrict,
            testTag = "wizard_pick_district"
        )

        WizardPickerRow(
            icon = Icons.Rounded.Flag,
            label = "Desa/Kelurahan",
            value = villageName ?: if (districtName == null) "Pilih kecamatan dulu" else "Pilih desa/kelurahan",
            isPlaceholder = villageName == null,
            enabled = districtName != null,
            onClick = onPickVillage,
            testTag = "wizard_pick_village"
        )
    }
}

// --- STEP 4: KONEKSI (Gambar 6) ---
@Composable
private fun Step4KoneksiCard(
    selectedRouter: RouterConfig?,
    isRouterOnline: Boolean,
    pppoeUsername: String,
    onUsernameChange: (String) -> Unit,
    pppoePassword: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onTogglePasswordVisible: () -> Unit,
    selectedPackage: InternetPackageEntity?,
    exemptAutoIsolir: Boolean,
    onExemptChange: (Boolean) -> Unit,
    onOpenServerPicker: () -> Unit,
    onOpenSecretPicker: () -> Unit,
    onOpenProductPicker: () -> Unit
) {
    WizardSectionCard(title = "Koneksi") {
        // MikroTik connection pill
        val pillBg = when {
            selectedRouter == null -> MaterialTheme.colorScheme.surfaceVariant
            isRouterOnline -> NocEmeraldOnline.copy(alpha = 0.16f)
            else -> NocRedOffline.copy(alpha = 0.16f)
        }
        val pillFg = when {
            selectedRouter == null -> MaterialTheme.colorScheme.onSurfaceVariant
            isRouterOnline -> NocEmeraldOnline
            else -> NocRedOffline
        }
        val pillLabel = when {
            selectedRouter == null -> "MikroTik: Belum ada server"
            isRouterOnline -> "MikroTik: Terhubung"
            else -> "MikroTik: Terputus"
        }
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = pillBg
        ) {
            Text(
                text = pillLabel,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = pillFg,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Model koneksi",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .border(1.5.dp, NocAmber, RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Rounded.Wifi, contentDescription = null, tint = NocAmber, modifier = Modifier.size(20.dp))
                    Text(
                        text = "PPPoE",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Secret username & password",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        WizardPickerRow(
            icon = Icons.Outlined.Storage,
            label = "Server",
            value = selectedRouter?.name ?: "Pilih server MikroTik",
            isPlaceholder = selectedRouter == null,
            enabled = true,
            onClick = onOpenServerPicker,
            testTag = "wizard_pick_server"
        )

        WizardInputFieldRow(
            icon = Icons.Outlined.Person,
            label = "User PPPoE",
            value = pppoeUsername,
            onValueChange = onUsernameChange,
            placeholder = "username",
            testTag = "wizard_pppoe_username"
        )

        WizardPickerRow(
            icon = Icons.Rounded.Wifi,
            label = "Ambil secret existing",
            value = if (pppoeUsername.isNotBlank()) "Secret: $pppoeUsername" else "Pilih secret",
            isPlaceholder = pppoeUsername.isBlank(),
            enabled = selectedRouter != null,
            onClick = onOpenSecretPicker,
            testTag = "wizard_pick_secret"
        )

        WizardInputFieldRow(
            icon = Icons.Outlined.Key,
            label = "Password PPPoE",
            value = pppoePassword,
            onValueChange = onPasswordChange,
            placeholder = "••••••",
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingContent = {
                IconButton(
                    onClick = onTogglePasswordVisible,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = "Tampilkan password",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            },
            testTag = "wizard_pppoe_password"
        )

        WizardPickerRow(
            icon = Icons.Outlined.Inventory2,
            label = "Produk",
            value = selectedPackage?.name ?: "Pilih produk",
            isPlaceholder = selectedPackage == null,
            enabled = true,
            onClick = onOpenProductPicker,
            testTag = "wizard_pick_product"
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Kecualikan dari isolir otomatis",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Ya: tidak diisolir otomatis. Tidak: isolir otomatis tetap berlaku.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SelectableOptionCard(
                    title = "Ya",
                    subtitle = "Tidak diisolir otomatis",
                    selected = exemptAutoIsolir,
                    onClick = { onExemptChange(true) },
                    modifier = Modifier.weight(1f).testTag("wizard_exempt_yes")
                )
                SelectableOptionCard(
                    title = "Tidak",
                    subtitle = "Isolir otomatis berlaku",
                    selected = !exemptAutoIsolir,
                    onClick = { onExemptChange(false) },
                    modifier = Modifier.weight(1f).testTag("wizard_exempt_no")
                )
            }
        }
    }
}

// --- STEP 5: LOKASI (Gambar 7) ---
@Composable
private fun Step5LokasiCard(
    latText: String,
    lngText: String,
    onLatChange: (String) -> Unit,
    onLngChange: (String) -> Unit,
    selectedNode: FtthNodeEntity?,
    hasCoordinates: Boolean,
    onOpenMapPicker: () -> Unit,
    onGpsClick: () -> Unit,
    onPasteClick: () -> Unit
) {
    WizardSectionCard(title = "Lokasi") {
        // Pilih di peta clickable row matching Gambar 7
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                .clickable(onClick = onOpenMapPicker)
                .testTag("wizard_open_map_picker"),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(NocAmber.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Map, contentDescription = null, tint = NocAmber, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Pilih di peta",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (hasCoordinates) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = NocEmeraldOnline.copy(alpha = 0.18f)
                            ) {
                                Text(
                                    text = "TERPASANG",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NocEmeraldOnline,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = if (hasCoordinates) "${latText.trim()}, ${lngText.trim()}" else "Belum dipilih",
                        fontSize = 12.sp,
                        fontFamily = if (hasCoordinates) JetBrainsMonoFamily else MaterialTheme.typography.bodySmall.fontFamily,
                        color = if (hasCoordinates) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (selectedNode != null) {
            Text(
                text = "Distribusi ${selectedNode.code.ifBlank { selectedNode.name }} — peta fokus ke ${selectedNode.type.label}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CompactCoordinateBox(
                label = "Latitude",
                value = latText,
                onValueChange = onLatChange,
                placeholder = "-6.932347",
                modifier = Modifier.weight(1f).testTag("wizard_lat_input")
            )
            CompactCoordinateBox(
                label = "Longitude",
                value = lngText,
                onValueChange = onLngChange,
                placeholder = "111.474114",
                modifier = Modifier.weight(1f).testTag("wizard_lng_input")
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                    .clickable(onClick = onGpsClick)
                    .testTag("wizard_gps_button"),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Rounded.MyLocation, contentDescription = null, tint = NocAmber, modifier = Modifier.size(18.dp))
                    Text("GPS", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
            }

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                    .clickable(onClick = onPasteClick)
                    .testTag("wizard_paste_button"),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Outlined.ContentPaste, contentDescription = null, tint = NocAmber, modifier = Modifier.size(18.dp))
                    Text("Tempel", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

// --- STEP 6: BILLING (Gambar 9) ---
@Composable
private fun Step6BillingCard(
    statusLabel: String,
    onStatusChange: (String) -> Unit,
    installDateLabel: String,
    onPickInstallDate: () -> Unit,
    selectedSalesName: String,
    onPickSales: () -> Unit,
    customIsolirEnabled: Boolean,
    onCustomIsolirEnabledChange: (Boolean) -> Unit,
    customIsolirDayText: String,
    onCustomIsolirDayChange: (String) -> Unit,
    customInvoiceEnabled: Boolean,
    onCustomInvoiceEnabledChange: (Boolean) -> Unit,
    customInvoiceDayText: String,
    onCustomInvoiceDayChange: (String) -> Unit
) {
    WizardSectionCard(title = "Billing") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Status Pelanggan",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SelectableOptionCard(
                    title = "Aktif",
                    subtitle = null,
                    selected = statusLabel == "Aktif",
                    onClick = { onStatusChange("Aktif") },
                    modifier = Modifier.weight(1f).testTag("wizard_status_aktif")
                )
                SelectableOptionCard(
                    title = "Menunggu",
                    subtitle = null,
                    selected = statusLabel == "Menunggu",
                    onClick = { onStatusChange("Menunggu") },
                    modifier = Modifier.weight(1f).testTag("wizard_status_menunggu")
                )
            }
        }

        WizardPickerRow(
            icon = Icons.Outlined.CalendarMonth,
            label = "Tanggal pemasangan",
            value = installDateLabel.ifBlank { "Pilih tanggal" },
            isPlaceholder = installDateLabel.isBlank(),
            enabled = true,
            onClick = onPickInstallDate,
            testTag = "wizard_pick_install_date"
        )

        WizardPickerRow(
            icon = Icons.Outlined.Groups,
            label = "Sales (opsional)",
            value = selectedSalesName,
            isPlaceholder = false,
            enabled = true,
            onClick = onPickSales,
            testTag = "wizard_pick_sales"
        )

        // Custom tanggal isolir switch + free day 1..31 editor
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f), RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Custom tanggal isolir",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (customIsolirEnabled) "Setiap tanggal ${customIsolirDayText.ifBlank { "28" }} per bulan"
                            else "Hari 1–31. Default sistem: 28.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = customIsolirEnabled,
                        onCheckedChange = onCustomIsolirEnabledChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = NocAmber
                        ),
                        modifier = Modifier.testTag("wizard_custom_isolir_switch")
                    )
                }

                if (customIsolirEnabled) {
                    CustomDayPickerRow(
                        label = "Tanggal isolir (1–31)",
                        dayText = customIsolirDayText,
                        onDayChange = onCustomIsolirDayChange
                    )
                }
            }
        }

        // Custom tanggal invoice switch (unlocked when custom isolir enabled)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f), RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (customIsolirEnabled) 0.22f else 0.12f)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Custom tanggal invoice",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (customIsolirEnabled) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = when {
                                !customIsolirEnabled -> "Aktifkan custom isolir terlebih dahulu."
                                customInvoiceEnabled -> "Invoice terbit setiap tanggal ${customInvoiceDayText.ifBlank { "28" }}"
                                else -> "Ikuti tanggal isolir custom (${customIsolirDayText.ifBlank { "28" }})."
                            },
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = customInvoiceEnabled && customIsolirEnabled,
                        onCheckedChange = { if (customIsolirEnabled) onCustomInvoiceEnabledChange(it) },
                        enabled = customIsolirEnabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = NocAmber
                        ),
                        modifier = Modifier.testTag("wizard_custom_invoice_switch")
                    )
                }

                if (customIsolirEnabled && customInvoiceEnabled) {
                    CustomDayPickerRow(
                        label = "Tanggal invoice (1–31)",
                        dayText = customInvoiceDayText,
                        onDayChange = onCustomInvoiceDayChange
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomDayPickerRow(
    label: String,
    dayText: String,
    onDayChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .size(32.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        .clickable {
                            val cur = dayText.toIntOrNull() ?: 28
                            onDayChange((cur - 1).coerceIn(1, 31).toString())
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("-", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                Box(
                    modifier = Modifier
                        .width(52.dp)
                        .height(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, NocAmber, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    BasicTextField(
                        value = dayText,
                        onValueChange = onDayChange,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        ),
                        cursorBrush = SolidColor(NocAmber),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .size(32.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        .clickable {
                            val cur = dayText.toIntOrNull() ?: 28
                            onDayChange((cur + 1).coerceIn(1, 31).toString())
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("+", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }

        // Quick preset day chips
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(1, 5, 10, 15, 20, 25, 28, 30).forEach { preset ->
                val isChosen = dayText == preset.toString()
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isChosen) NocAmber.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .border(
                            1.dp,
                            if (isChosen) NocAmber else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onDayChange(preset.toString()) }
                ) {
                    Text(
                        text = "Tgl $preset",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isChosen) NocAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

// --- REUSABLE WIZARD CONTAINERS & MODAL PICKERS ---
@Composable
private fun WizardSectionCard(
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
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            content()
        }
    }
}

@Composable
private fun WizardInputFieldRow(
    icon: ImageVector,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingContent: (@Composable () -> Unit)? = null,
    testTag: String = ""
) {
    var focused by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (focused) 1.5.dp else 1.dp,
                color = if (focused) NocAmber else MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                shape = RoundedCornerShape(14.dp)
            ),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(NocAmber.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = NocAmber, modifier = Modifier.size(19.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    color = if (focused) NocAmber else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        singleLine = true,
                        visualTransformation = visualTransformation,
                        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        cursorBrush = SolidColor(NocAmber),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focused = it.isFocused }
                            .then(if (testTag.isNotBlank()) Modifier.testTag(testTag) else Modifier)
                    )
                }
            }
            trailingContent?.invoke()
        }
    }
}

@Composable
private fun WizardPickerRow(
    icon: ImageVector,
    label: String,
    value: String,
    isPlaceholder: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    testTag: String = ""
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .then(if (testTag.isNotBlank()) Modifier.testTag(testTag) else Modifier),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (enabled) 0.22f else 0.12f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(NocAmber.copy(alpha = if (enabled) 0.14f else 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) NocAmber else NocAmber.copy(alpha = 0.5f),
                    modifier = Modifier.size(19.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    fontSize = 14.sp,
                    fontWeight = if (isPlaceholder) FontWeight.Medium else FontWeight.Bold,
                    color = if (isPlaceholder) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SelectableOptionCard(
    title: String,
    subtitle: String?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) NocAmber else MaterialTheme.colorScheme.outline.copy(alpha = 0.65f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) NocAmber.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CompactCoordinateBox(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f), RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Box {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = TextStyle(
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(NocAmber),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// --- FULL-SCREEN WILAYAH PICKER DIALOG (Gambar 5) ---
@Composable
private fun SearchableWilayahPickerDialog(
    title: String,
    searchPlaceholder: String,
    items: List<WilayahItem>,
    selectedName: String?,
    isLoading: Boolean,
    allowCustomEntry: Boolean,
    onSelect: (WilayahItem) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(items, query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) items else items.filter { it.name.lowercase().contains(q) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Top Header matching Gambar 5 (< | Amber centered title)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Tutup",
                    tint = NocAmber,
                    modifier = Modifier
                        .size(28.dp)
                        .clickable(onClick = onDismiss)
                        .padding(2.dp)
                )
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = NocAmber
                    )
                }
                Spacer(modifier = Modifier.width(28.dp))
            }

            // Search Box
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f), RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(NocAmber.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Search, contentDescription = null, tint = NocAmber, modifier = Modifier.size(17.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text(searchPlaceholder, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp),
                            cursorBrush = SolidColor(NocAmber),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (isLoading && items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(color = NocAmber, modifier = Modifier.size(30.dp))
                        Text("Memuat data wilayah...", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filtered, key = { "${it.id}_${it.name}" }) { item ->
                        val isChosen = selectedName.equals(item.name, ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isChosen) NocAmber.copy(alpha = 0.12f) else Color.Transparent)
                                .clickable { onSelect(item) }
                                .padding(horizontal = 16.dp, vertical = 15.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.weight(1f)
                            )
                            if (isChosen) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Terpilih",
                                    tint = NocAmber,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    }

                    if (allowCustomEntry && query.trim().length >= 2 &&
                        filtered.none { it.name.equals(query.trim(), ignoreCase = true) }
                    ) {
                        val manualName = query.trim().uppercase()
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(WilayahItem("manual_$manualName", manualName)) }
                                    .padding(horizontal = 16.dp, vertical = 15.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Gunakan \"$manualName\"",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NocAmber
                                )
                            }
                        }
                    } else if (filtered.isEmpty()) {
                        item {
                            Text(
                                text = "Data tidak ditemukan.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> SearchableSimplePickerDialog(
    title: String,
    searchPlaceholder: String,
    emptyMessage: String,
    items: List<T>,
    matches: (T, String) -> Boolean,
    isSelected: (T) -> Boolean,
    itemTitle: (T) -> String,
    itemSubtitle: (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(items, query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) items else items.filter { matches(it, q) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Tutup",
                    tint = NocAmber,
                    modifier = Modifier
                        .size(28.dp)
                        .clickable(onClick = onDismiss)
                        .padding(2.dp)
                )
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = NocAmber
                    )
                }
                Spacer(modifier = Modifier.width(28.dp))
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f), RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(NocAmber.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Search, contentDescription = null, tint = NocAmber, modifier = Modifier.size(17.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text(searchPlaceholder, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp),
                            cursorBrush = SolidColor(NocAmber),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = emptyMessage,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filtered) { item ->
                        val chosen = isSelected(item)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (chosen) NocAmber.copy(alpha = 0.12f) else Color.Transparent)
                                .clickable { onSelect(item) }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = itemTitle(item),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                val sub = itemSubtitle(item)
                                if (sub.isNotBlank()) {
                                    Text(
                                        text = sub,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (chosen) {
                                Icon(Icons.Rounded.Check, contentDescription = "Terpilih", tint = NocAmber, modifier = Modifier.size(18.dp))
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    }
                }
            }
        }
    }
}

private fun generateNugrohoEmail(fullName: String): String {
    val slug = fullName.lowercase(Locale.ROOT).filter { it.isLetterOrDigit() }
    return if (slug.isBlank()) "" else "$slug@nugroho.co.id"
}

private fun generateUniqueSixDigitId(existingCodes: Set<String>): String {
    repeat(30) {
        val candidate = Random.nextInt(100_000, 1_000_000).toString()
        if (!existingCodes.contains(candidate)) return candidate
    }
    return ((System.currentTimeMillis() % 900_000L) + 100_000L).toString()
}

private fun parseCoordinatePair(raw: String): Pair<Double, Double>? {
    val parts = raw.trim().split(Regex("[,\\s]+")).mapNotNull { it.toDoubleOrNull() }
    if (parts.size >= 2) {
        val lat = parts[0]
        val lng = parts[1]
        if (lat in -90.0..90.0 && lng in -180.0..180.0) {
            return lat to lng
        }
    }
    return null
}
