package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.CustomerEntity
import com.example.data.model.FtthCustomerPinEntity
import com.example.data.model.FtthNodeEntity
import com.example.ui.components.CustomerPinRenderItem
import com.example.ui.components.FtthBasemap
import com.example.ui.components.FtthInteractiveMapCanvas
import com.example.ui.components.LatLngPoint
import com.example.ui.components.PinLinkStatus
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.NocAmber
import java.util.Locale

@Composable
fun PelangganMapPickerScreen(
    initialLat: Double?,
    initialLng: Double?,
    selectedNode: FtthNodeEntity?,
    ftthNodes: List<FtthNodeEntity>,
    customers: List<CustomerEntity>,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onConfirm: (Double, Double) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val defaultCenter = remember(initialLat, initialLng, selectedNode, ftthNodes) {
        when {
            initialLat != null && initialLng != null -> LatLngPoint(initialLat, initialLng)
            selectedNode != null -> LatLngPoint(selectedNode.latitude, selectedNode.longitude)
            ftthNodes.isNotEmpty() -> LatLngPoint(ftthNodes.first().latitude, ftthNodes.first().longitude)
            else -> LatLngPoint(-6.932347, 111.474114)
        }
    }

    var basemap by remember { mutableStateOf(FtthBasemap.SATELIT) }
    var centerLat by remember { mutableStateOf(defaultCenter.lat) }
    var centerLng by remember { mutableStateOf(defaultCenter.lng) }
    var zoom by remember { mutableFloatStateOf(17.5f) }
    var pickedPoint by remember { mutableStateOf(defaultCenter) }

    val existingPins = remember(customers) {
        customers.mapNotNull { cust ->
            val lat = cust.latitude ?: return@mapNotNull null
            val lng = cust.longitude ?: return@mapNotNull null
            CustomerPinRenderItem(
                pin = FtthCustomerPinEntity(
                    id = cust.id,
                    name = cust.pppoeUsername.ifBlank { cust.name },
                    pppoeUsername = cust.pppoeUsername,
                    parentNodeId = cust.distributionNodeId,
                    latitude = lat,
                    longitude = lng,
                    address = cust.address
                ),
                status = if (cust.isActive) PinLinkStatus.ONLINE else PinLinkStatus.OFFLINE,
                speedLabel = ""
            )
        }
    }

    val gpsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            fetchDeviceLocation(context) { pt ->
                pickedPoint = pt
                centerLat = pt.lat
                centerLng = pt.lng
                zoom = 18f
            }
        } else {
            Toast.makeText(context, "Izin lokasi diperlukan untuk mengambil GPS.", Toast.LENGTH_SHORT).show()
        }
    }

    fun triggerGps() {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (hasFine || hasCoarse) {
            fetchDeviceLocation(context) { pt ->
                pickedPoint = pt
                centerLat = pt.lat
                centerLng = pt.lng
                zoom = 18f
            }
        } else {
            gpsLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header: < Pilih Lokasi + Theme Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                        .clickable(onClick = onBack)
                        .testTag("map_picker_back_button"),
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
                    text = "Pilih Lokasi",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
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

        // Map Container with overlays matching Gambar 8
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            FtthInteractiveMapCanvas(
                basemap = basemap,
                centerLat = centerLat,
                centerLng = centerLng,
                zoom = zoom,
                nodes = ftthNodes,
                customerPins = existingPins,
                cableRoutes = emptyList(),
                draftCablePoints = emptyList(),
                nearestRoute = null,
                isDrawingCable = false,
                showCustomerLayer = true,
                showDistributionLayer = true,
                showCustomerLineLayer = false,
                showDistributionLineLayer = true,
                showCableRouteLayer = false,
                dragMarkerMode = false,
                onCameraMove = { nLat, nLng, nZoom ->
                    centerLat = nLat
                    centerLng = nLng
                    zoom = nZoom
                },
                onMapTap = { pt -> pickedPoint = pt },
                onMapLongPress = { pt -> pickedPoint = pt },
                onNodeTap = { node ->
                    // Let user snap near or onto node if tapped, or keep picking easily
                    pickedPoint = LatLngPoint(node.latitude, node.longitude)
                },
                onCustomerPinTap = { item ->
                    pickedPoint = LatLngPoint(item.pin.latitude, item.pin.longitude)
                },
                onNodeDragged = { _, _ -> },
                onCustomerPinDragged = { _, _ -> },
                pickerLatLng = pickedPoint,
                onPickerDragged = { next -> pickedPoint = next },
                modifier = Modifier.fillMaxSize()
            )

            // Floating Top-Left Bar: Satelit | Roadmap | GPS
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(14.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xE6111622),
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isSat = basemap == FtthBasemap.SATELIT
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSat) NocAmber.copy(alpha = 0.16f) else Color.Transparent)
                            .border(
                                width = if (isSat) 1.dp else 0.dp,
                                color = if (isSat) NocAmber else Color.Transparent,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { basemap = FtthBasemap.SATELIT }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("map_picker_satelit_btn")
                    ) {
                        Text(
                            text = "Satelit",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSat) NocAmber else Color.White.copy(alpha = 0.85f)
                        )
                    }

                    val isRoad = basemap == FtthBasemap.OSM
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isRoad) NocAmber.copy(alpha = 0.16f) else Color.Transparent)
                            .border(
                                width = if (isRoad) 1.dp else 0.dp,
                                color = if (isRoad) NocAmber else Color.Transparent,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { basemap = FtthBasemap.OSM }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("map_picker_roadmap_btn")
                    ) {
                        Text(
                            text = "Roadmap",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isRoad) NocAmber else Color.White.copy(alpha = 0.85f)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(NocAmber)
                            .clickable { triggerGps() }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("map_picker_gps_btn"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MyLocation,
                            contentDescription = "Ambil GPS",
                            tint = Color(0xFF0B0E14),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "GPS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0B0E14)
                        )
                    }
                }
            }

            // Floating Top-Right Zoom Controls (+ / -)
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(14.dp),
                shape = RoundedCornerShape(10.dp),
                color = Color.White,
                shadowElevation = 6.dp
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clickable { zoom = (zoom + 1f).coerceAtMost(19f) }
                            .testTag("map_picker_zoom_in"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "Perbesar", tint = Color(0xFF1E293B))
                    }
                    HorizontalDivider(modifier = Modifier.width(30.dp), color = Color(0xFFE2E8F0))
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clickable { zoom = (zoom - 1f).coerceAtLeast(4f) }
                            .testTag("map_picker_zoom_out"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Remove, contentDescription = "Perkecil", tint = Color(0xFF1E293B))
                    }
                }
            }

            // Floating Bottom Coordinate & Confirm Card
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                shape = RoundedCornerShape(18.dp),
                color = Color(0xF2151B27),
                shadowElevation = 10.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = String.format(Locale.US, "%.6f, %.6f", pickedPoint.lat, pickedPoint.lng),
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Ketuk peta atau geser marker",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Button(
                        onClick = { onConfirm(pickedPoint.lat, pickedPoint.lng) },
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NocAmber,
                            contentColor = Color(0xFF0B0E14)
                        ),
                        contentPadding = PaddingValues(vertical = 13.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("confirm_coordinates_button")
                    ) {
                        Text(
                            text = "Pakai Koordinat",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
fun fetchDeviceLocation(
    context: Context,
    onResult: (LatLngPoint) -> Unit
) {
    val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    if (!hasFine && !hasCoarse) {
        Toast.makeText(context, "Izin GPS belum diberikan.", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (lm == null) {
            Toast.makeText(context, "Layanan lokasi tidak tersedia.", Toast.LENGTH_SHORT).show()
            return
        }
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        val bestLast: Location? = providers.mapNotNull { prov ->
            runCatching {
                if (lm.isProviderEnabled(prov)) lm.getLastKnownLocation(prov) else null
            }.getOrNull()
        }.maxByOrNull { it.time }

        if (bestLast != null) {
            onResult(LatLngPoint(bestLast.latitude, bestLast.longitude))
            return
        }

        val activeProv = providers.firstOrNull { p -> runCatching { lm.isProviderEnabled(p) }.getOrDefault(false) }
        if (activeProv == null) {
            Toast.makeText(context, "Aktifkan GPS perangkat terlebih dahulu.", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(context, "Mencari sinyal GPS...", Toast.LENGTH_SHORT).show()
        @Suppress("DEPRECATION")
        lm.requestSingleUpdate(
            activeProv,
            object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    onResult(LatLngPoint(location.latitude, location.longitude))
                }
                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            },
            Looper.getMainLooper()
        )
    } catch (_: SecurityException) {
        Toast.makeText(context, "Akses lokasi ditolak.", Toast.LENGTH_SHORT).show()
    } catch (_: Throwable) {
        Toast.makeText(context, "Gagal membaca koordinat GPS.", Toast.LENGTH_SHORT).show()
    }
}
