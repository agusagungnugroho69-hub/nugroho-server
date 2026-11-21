package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.ActivityLogEntity
import com.example.data.model.CableRouteEntity
import com.example.data.model.CpeDeviceEntity
import com.example.data.model.CustomerEntity
import com.example.data.model.FtthCustomerPinEntity
import com.example.data.model.FtthNodeEntity
import com.example.data.model.FtthNodeType
import com.example.data.model.InternetPackageEntity
import com.example.data.model.InvoiceEntity
import com.example.data.model.InvoiceStatus
import com.example.data.model.NetworkAreaEntity
import com.example.data.model.RadiusEntry
import com.example.data.model.RouterConfig
import com.example.data.model.SalesRepEntity
import com.example.data.model.SupportTicketEntity
import com.example.data.model.TicketPriority
import com.example.data.model.TicketStatus
import com.example.data.model.TransactionEntity
import com.example.data.network.GenieAcsClient
import com.example.data.network.MikroTikDataSource
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class IspModulesViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val dataSource = MikroTikDataSource()
    private val genieAcsClient = GenieAcsClient()

    val invoices: StateFlow<List<InvoiceEntity>> = db.invoiceDao().getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<TransactionEntity>> = db.transactionDao().getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val packages: StateFlow<List<InternetPackageEntity>> = db.internetPackageDao().getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val areas: StateFlow<List<NetworkAreaEntity>> = db.networkAreaDao().getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val salesReps: StateFlow<List<SalesRepEntity>> = db.salesRepDao().getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tickets: StateFlow<List<SupportTicketEntity>> = db.supportTicketDao().getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activityLogs: StateFlow<List<ActivityLogEntity>> = db.activityLogDao().getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cpeDevices: StateFlow<List<CpeDeviceEntity>> = db.cpeDeviceDao().getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ftthNodes: StateFlow<List<FtthNodeEntity>> = db.ftthNodeDao().getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customerPins: StateFlow<List<FtthCustomerPinEntity>> = db.ftthCustomerPinDao().getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cableRoutes: StateFlow<List<CableRouteEntity>> = db.cableRouteDao().getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customers: StateFlow<List<CustomerEntity>> = db.customerDao().getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _radiusEntries = MutableStateFlow<List<RadiusEntry>>(emptyList())
    val radiusEntries: StateFlow<List<RadiusEntry>> = _radiusEntries.asStateFlow()

    private val _isSyncingExternal = MutableStateFlow(false)
    val isSyncingExternal: StateFlow<Boolean> = _isSyncingExternal.asStateFlow()

    private val _message = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val message: SharedFlow<String> = _message.asSharedFlow()

    fun logActivity(module: String, action: String, detail: String) {
        viewModelScope.launch {
            db.activityLogDao().insert(
                ActivityLogEntity(module = module, action = action, detail = detail)
            )
        }
    }

    // --- Tagihan & Transaksi ---
    fun addInvoice(
        customerName: String,
        customerPppoe: String,
        packageName: String,
        areaName: String,
        amount: Long,
        dueDateLabel: String,
        ppnPercent: Int,
        note: String
    ) {
        viewModelScope.launch {
            val dpp = if (ppnPercent > 0) (amount * 100L) / (100 + ppnPercent) else amount
            val ppn = (amount - dpp).coerceAtLeast(0L)
            val code = "INV-${System.currentTimeMillis().toString().takeLast(8)}"
            db.invoiceDao().insert(
                InvoiceEntity(
                    invoiceNumber = code,
                    customerName = customerName.trim(),
                    customerPppoe = customerPppoe.trim(),
                    packageName = packageName.ifBlank { "Paket Fiber" },
                    areaName = areaName.ifBlank { "Umum" },
                    amount = amount,
                    dppAmount = dpp,
                    ppnAmount = ppn,
                    dueDateLabel = dueDateLabel.ifBlank { "25 Nov 2026" },
                    status = InvoiceStatus.BELUM_BAYAR,
                    note = note.trim()
                )
            )
            logActivity("Tagihan", "Buat Invoice", "$code untuk $customerName (Rp $amount)")
            _message.tryEmit("Tagihan $code berhasil dibuat.")
        }
    }

    fun markInvoicePaid(invoice: InvoiceEntity, method: String) {
        viewModelScope.launch {
            db.invoiceDao().update(
                invoice.copy(status = InvoiceStatus.LUNAS, paymentMethod = method)
            )
            val ref = "TRX-${System.currentTimeMillis().toString().takeLast(7)}"
            db.transactionDao().insert(
                TransactionEntity(
                    referenceCode = ref,
                    invoiceNumber = invoice.invoiceNumber,
                    customerName = invoice.customerName,
                    amount = invoice.amount,
                    method = method,
                    verified = true,
                    note = "Pembayaran ${invoice.packageName}"
                )
            )
            logActivity("Transaksi", "Verifikasi Bayar", "${invoice.invoiceNumber} lunas via $method")
            _message.tryEmit("Tagihan ${invoice.invoiceNumber} ditandai Lunas ($method).")
        }
    }

    fun markInvoiceOverdue(invoice: InvoiceEntity) {
        viewModelScope.launch {
            db.invoiceDao().update(invoice.copy(status = InvoiceStatus.TERLAMBAT))
            logActivity("Tagihan", "Jatuh Tempo", "${invoice.invoiceNumber} ditandai terlambat")
        }
    }

    fun deleteInvoice(invoice: InvoiceEntity) {
        viewModelScope.launch {
            db.invoiceDao().delete(invoice)
            logActivity("Tagihan", "Hapus Invoice", invoice.invoiceNumber)
        }
    }

    fun toggleTransactionVerified(tx: TransactionEntity) {
        viewModelScope.launch {
            db.transactionDao().update(tx.copy(verified = !tx.verified))
            logActivity("Transaksi", "Ubah Status Verifikasi", "${tx.referenceCode} -> ${!tx.verified}")
        }
    }

    // --- Produk / Paket Internet ---
    fun upsertPackage(pkg: InternetPackageEntity) {
        viewModelScope.launch {
            if (pkg.id == 0L) {
                db.internetPackageDao().insert(pkg)
                logActivity("Produk", "Tambah Produk", "${pkg.name} (${pkg.pppoeProfile})")
                _message.tryEmit("Produk ${pkg.name} disimpan.")
            } else {
                db.internetPackageDao().update(pkg)
                logActivity("Produk", "Edit Produk", "${pkg.name} (${pkg.pppoeProfile})")
                _message.tryEmit("Perubahan ${pkg.name} disimpan.")
            }
        }
    }

    fun deletePackage(pkg: InternetPackageEntity) {
        viewModelScope.launch {
            db.internetPackageDao().delete(pkg)
            logActivity("Produk", "Hapus Produk", pkg.name)
        }
    }

    fun syncProfilesFromRouters(routers: List<RouterConfig>) {
        if (routers.isEmpty()) {
            _message.tryEmit("Tambahkan router MikroTik terlebih dahulu.")
            return
        }
        viewModelScope.launch {
            _isSyncingExternal.value = true
            var addedCount = 0
            for (router in routers) {
                val res = dataSource.fetchPppProfiles(router)
                res.getOrNull()?.forEach { prof ->
                    val pName = prof["name"] ?: return@forEach
                    if (pName == "default" || pName == "default-encryption") return@forEach
                    val rate = prof["rate-limit"].orEmpty()
                    val speeds = rate.split("/")
                    val ul = speeds.getOrNull(0)?.filter { it.isDigit() }?.toIntOrNull() ?: 20
                    val dl = speeds.getOrNull(1)?.filter { it.isDigit() }?.toIntOrNull() ?: ul
                    val alreadyExists = packages.value.any { it.pppoeProfile.equals(pName, ignoreCase = true) }
                    if (!alreadyExists) {
                        db.internetPackageDao().insert(
                            InternetPackageEntity(
                                name = pName,
                                pppoeProfile = pName,
                                downloadMbps = dl,
                                uploadMbps = ul,
                                monthlyPrice = (dl * 10_000L).coerceAtLeast(100_000L),
                                description = "Sinkronisasi dari ${router.name} ($rate)"
                            )
                        )
                        addedCount++
                    }
                }
            }
            _isSyncingExternal.value = false
            logActivity("Produk", "Sinkronisasi MikroTik", "$addedCount profil PPPoE baru diimpor")
            _message.tryEmit("Sinkronisasi selesai: $addedCount profil baru ditambahkan.")
        }
    }

    // --- Area & Maps / ODP ---
    fun addArea(
        name: String,
        code: String,
        odpCount: Int,
        totalPorts: Int,
        usedPorts: Int,
        coordinates: String,
        routerName: String
    ) {
        viewModelScope.launch {
            db.networkAreaDao().insert(
                NetworkAreaEntity(
                    name = name.trim(),
                    code = code.trim().uppercase(),
                    odpCount = odpCount,
                    totalPorts = totalPorts,
                    usedPorts = usedPorts.coerceAtMost(totalPorts),
                    coordinates = coordinates.trim(),
                    routerName = routerName.trim()
                )
            )
            logActivity("Area & Maps", "Tambah Area", "$name ($code)")
            _message.tryEmit("Area distribusi $name disimpan.")
        }
    }

    fun deleteArea(area: NetworkAreaEntity) {
        viewModelScope.launch {
            db.networkAreaDao().delete(area)
            logActivity("Area & Maps", "Hapus Area", area.name)
        }
    }

    // --- GenieACS (TR-069) ---
    fun syncGenieAcs(baseUrl: String) {
        viewModelScope.launch {
            _isSyncingExternal.value = true
            val res = genieAcsClient.fetchDevices(baseUrl)
            _isSyncingExternal.value = false
            if (res.isSuccess) {
                val items = res.getOrDefault(emptyList())
                db.cpeDeviceDao().clearAll()
                db.cpeDeviceDao().insertAll(items)
                logActivity("GenieACS", "Sinkronisasi TR-069", "${items.size} perangkat ONT diambil")
                _message.tryEmit("Berhasil sinkronisasi ${items.size} perangkat TR-069.")
            } else {
                _message.tryEmit("GenieACS gagal: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun addManualCpe(
        serialNumber: String,
        model: String,
        customerName: String,
        ipAddress: String,
        rxPowerDbm: String,
        ssid: String
    ) {
        viewModelScope.launch {
            db.cpeDeviceDao().insert(
                CpeDeviceEntity(
                    deviceId = serialNumber.trim(),
                    serialNumber = serialNumber.trim(),
                    model = model.trim().ifBlank { "ONT HG8245H5" },
                    customerName = customerName.trim(),
                    ipAddress = ipAddress.trim(),
                    rxPowerDbm = rxPowerDbm.trim().ifBlank { "-19.4 dBm" },
                    ssid = ssid.trim(),
                    isOnline = true,
                    lastInform = "Baru saja"
                )
            )
            logActivity("GenieACS", "Tambah Perangkat CPE", "$serialNumber ($customerName)")
        }
    }

    fun rebootCpe(baseUrl: String, device: CpeDeviceEntity) {
        viewModelScope.launch {
            if (baseUrl.isBlank()) {
                _message.tryEmit("Atur URL GenieACS NBI terlebih dahulu untuk reboot jarak jauh.")
                return@launch
            }
            val res = genieAcsClient.rebootDevice(baseUrl, device.deviceId)
            if (res.isSuccess) {
                logActivity("GenieACS", "Reboot CPE", device.serialNumber)
                _message.tryEmit("Perintah reboot dikirim ke ${device.serialNumber}")
            } else {
                _message.tryEmit("Gagal reboot CPE: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    // --- RADIUS ---
    fun refreshRadiusFromRouters(routers: List<RouterConfig>) {
        if (routers.isEmpty()) {
            _radiusEntries.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isSyncingExternal.value = true
            val list = mutableListOf<RadiusEntry>()
            for (r in routers) {
                val res = dataSource.fetchRadiusEntries(r)
                res.getOrNull()?.forEach { row ->
                    list.add(
                        RadiusEntry(
                            id = row[".id"] ?: "-",
                            service = row["service"] ?: "ppp",
                            address = row["address"] ?: "-",
                            secret = "••••••",
                            timeout = row["timeout"] ?: "300ms",
                            status = if (row["disabled"] == "true") "Nonaktif" else "Aktif",
                            routerName = r.name
                        )
                    )
                }
            }
            _radiusEntries.value = list
            _isSyncingExternal.value = false
            logActivity("Radius", "Cek Server RADIUS", "Ditemukan ${list.size} entri NAS/RADIUS")
        }
    }

    // --- Sales ---
    fun addSalesRep(name: String, phone: String, area: String, target: Int, completed: Int) {
        viewModelScope.launch {
            db.salesRepDao().insert(
                SalesRepEntity(
                    name = name.trim(),
                    phone = phone.trim(),
                    areaAssigned = area.trim(),
                    targetInstalls = target.coerceAtLeast(1),
                    completedInstalls = completed.coerceAtLeast(0)
                )
            )
            logActivity("Sales", "Tambah Tim Sales", "$name (Target: $target)")
            _message.tryEmit("Data sales $name disimpan.")
        }
    }

    fun incrementSalesInstall(rep: SalesRepEntity) {
        viewModelScope.launch {
            db.salesRepDao().update(rep.copy(completedInstalls = rep.completedInstalls + 1))
            logActivity("Sales", "Tambah Pemasangan", "${rep.name} -> ${rep.completedInstalls + 1}")
        }
    }

    fun deleteSalesRep(rep: SalesRepEntity) {
        viewModelScope.launch {
            db.salesRepDao().delete(rep)
        }
    }

    // --- Tiket Gangguan ---
    fun addTicket(
        customerName: String,
        subject: String,
        description: String,
        priority: TicketPriority,
        technician: String
    ) {
        viewModelScope.launch {
            val code = "TIK-${System.currentTimeMillis().toString().takeLast(5)}"
            db.supportTicketDao().insert(
                SupportTicketEntity(
                    ticketCode = code,
                    customerName = customerName.trim(),
                    subject = subject.trim(),
                    description = description.trim(),
                    priority = priority,
                    status = TicketStatus.TERBUKA,
                    technician = technician.trim().ifBlank { "Tim NOC" }
                )
            )
            logActivity("Tiket", "Tiket Baru", "$code - $subject ($customerName)")
            _message.tryEmit("Tiket $code berhasil dibuat.")
        }
    }

    fun advanceTicketStatus(ticket: SupportTicketEntity) {
        val next = when (ticket.status) {
            TicketStatus.TERBUKA -> TicketStatus.PROSES
            TicketStatus.PROSES -> TicketStatus.SELESAI
            TicketStatus.SELESAI -> TicketStatus.TERBUKA
        }
        viewModelScope.launch {
            db.supportTicketDao().update(ticket.copy(status = next))
            logActivity("Tiket", "Ubah Status Tiket", "${ticket.ticketCode} -> ${next.label}")
        }
    }

    fun deleteTicket(ticket: SupportTicketEntity) {
        viewModelScope.launch {
            db.supportTicketDao().delete(ticket)
        }
    }

    fun clearActivityLogs() {
        viewModelScope.launch {
            db.activityLogDao().clearAll()
            _message.tryEmit("Log aktivitas dibersihkan.")
        }
    }

    // --- FTTH Distribusi, Customer Pins & Rute Kabel ---
    fun addFtthNode(
        type: FtthNodeType,
        name: String,
        code: String,
        limitPort: Int,
        extraPort: Int,
        address: String,
        description: String,
        parentId: Long?,
        latitude: Double,
        longitude: Double
    ) {
        viewModelScope.launch {
            db.ftthNodeDao().insert(
                FtthNodeEntity(
                    type = type,
                    name = name.trim(),
                    code = code.trim().uppercase(),
                    limitPort = limitPort.coerceAtLeast(1),
                    extraPort = extraPort.coerceAtLeast(0),
                    usedPorts = 0,
                    address = address.trim(),
                    description = description.trim(),
                    parentId = parentId,
                    latitude = latitude,
                    longitude = longitude
                )
            )
            logActivity("Maps & FTTH", "Tambah ${type.label}", "$name ($code) @ $latitude, $longitude")
            _message.tryEmit("${type.label} $name ditambahkan ke peta.")
        }
    }

    fun updateFtthNodePosition(node: FtthNodeEntity, newLat: Double, newLng: Double) {
        viewModelScope.launch {
            db.ftthNodeDao().update(node.copy(latitude = newLat, longitude = newLng))
            logActivity("Maps & FTTH", "Geser Marker", "${node.code} dipindah ke $newLat, $newLng")
        }
    }

    fun deleteFtthNode(node: FtthNodeEntity) {
        viewModelScope.launch {
            db.ftthNodeDao().delete(node)
            logActivity("Maps & FTTH", "Hapus Distribusi", "${node.name} (${node.code})")
        }
    }

    fun addCustomerPin(
        name: String,
        pppoeUsername: String,
        parentNodeId: Long?,
        latitude: Double,
        longitude: Double,
        address: String = ""
    ) {
        viewModelScope.launch {
            db.ftthCustomerPinDao().insert(
                FtthCustomerPinEntity(
                    name = name.trim(),
                    pppoeUsername = pppoeUsername.trim(),
                    parentNodeId = parentNodeId,
                    latitude = latitude,
                    longitude = longitude,
                    address = address.trim()
                )
            )
            logActivity("Maps & FTTH", "Pin Pelanggan", "$name ($pppoeUsername)")
            _message.tryEmit("Pin pelanggan $name ditambahkan.")
        }
    }

    fun updateCustomerPinPosition(pin: FtthCustomerPinEntity, newLat: Double, newLng: Double) {
        viewModelScope.launch {
            db.ftthCustomerPinDao().update(pin.copy(latitude = newLat, longitude = newLng))
        }
    }

    fun deleteCustomerPin(pin: FtthCustomerPinEntity) {
        viewModelScope.launch {
            db.ftthCustomerPinDao().delete(pin)
        }
    }

    fun addCableRoute(name: String, pointsEncoded: String, lengthMeters: Int, coreCount: Int = 12) {
        viewModelScope.launch {
            db.cableRouteDao().insert(
                CableRouteEntity(
                    name = name.trim().ifBlank { "Rute Kabel FO" },
                    pointsEncoded = pointsEncoded,
                    lengthMeters = lengthMeters,
                    coreCount = coreCount
                )
            )
            logActivity("Maps & FTTH", "Simpan Rute Kabel", "$name (${lengthMeters}m)")
            _message.tryEmit("Rute kabel $name (${lengthMeters}m) disimpan.")
        }
    }

    fun deleteCableRoute(route: CableRouteEntity) {
        viewModelScope.launch {
            db.cableRouteDao().delete(route)
        }
    }

    // --- Pelanggan Management ---
    fun upsertCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            val clean = customer.copy(
                name = customer.name.trim(),
                pppoeUsername = customer.pppoeUsername.trim(),
                customerCode = customer.customerCode.trim(),
                phone = customer.phone.trim(),
                email = customer.email.trim(),
                address = customer.address.trim(),
                dusun = customer.dusun.trim()
            )
            if (clean.id == 0L) {
                db.customerDao().insert(clean)
                logActivity("Pelanggan", "Tambah Pelanggan", "${clean.name} (${clean.pppoeUsername.ifBlank { clean.customerCode }})")
                _message.tryEmit("Pelanggan ${clean.name} berhasil ditambahkan.")
            } else {
                db.customerDao().update(clean)
                logActivity("Pelanggan", "Edit Pelanggan", "${clean.name} (${clean.pppoeUsername.ifBlank { clean.customerCode }})")
                _message.tryEmit("Perubahan ${clean.name} disimpan.")
            }

            // Sinkronkan pin peta FTTH otomatis jika koordinat diisi
            if (clean.latitude != null && clean.longitude != null) {
                val existingPin = db.ftthCustomerPinDao().findByPppoeOrName(clean.pppoeUsername, clean.name)
                val fullAddr = listOf(clean.address, clean.dusun, clean.village, clean.district, clean.regency, clean.province)
                    .filter { it.isNotBlank() }
                    .joinToString(", ")
                if (existingPin == null) {
                    db.ftthCustomerPinDao().insert(
                        FtthCustomerPinEntity(
                            name = clean.name,
                            pppoeUsername = clean.pppoeUsername,
                            parentNodeId = clean.distributionNodeId,
                            latitude = clean.latitude,
                            longitude = clean.longitude,
                            address = fullAddr
                        )
                    )
                } else {
                    db.ftthCustomerPinDao().update(
                        existingPin.copy(
                            name = clean.name,
                            pppoeUsername = clean.pppoeUsername,
                            parentNodeId = clean.distributionNodeId,
                            latitude = clean.latitude,
                            longitude = clean.longitude,
                            address = fullAddr
                        )
                    )
                }
            }
        }
    }

    fun toggleCustomerStatus(customer: CustomerEntity) {
        viewModelScope.launch {
            val nextActive = !customer.isActive
            val nextStatus = if (nextActive) "Aktif" else "Nonaktif"
            db.customerDao().update(customer.copy(isActive = nextActive, statusLabel = nextStatus))
            logActivity("Pelanggan", "Ubah Status", "${customer.name} -> $nextStatus")
            _message.tryEmit("Status ${customer.name} diubah ke $nextStatus.")
        }
    }

    fun deleteCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            db.customerDao().delete(customer)
            logActivity("Pelanggan", "Hapus Pelanggan", customer.name)
            _message.tryEmit("Pelanggan ${customer.name} dihapus.")
        }
    }
}
