package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.FtthNodeEntity
import com.example.data.model.FtthNodeType
import com.example.data.model.PppoeClient
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.NocAmber
import com.example.ui.theme.NocRedOffline
import java.util.Locale

@Composable
fun TambahDistribusiDialog(
    existingNodes: List<FtthNodeEntity>,
    initialLat: Double,
    initialLng: Double,
    onDismiss: () -> Unit,
    onSubmit: (
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
    ) -> Unit
) {
    var selectedType by remember { mutableStateOf(FtthNodeType.ODP) }
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var isCodeManuallyEdited by remember { mutableStateOf(false) }
    var limitPortText by remember { mutableStateOf("8") }
    var extraPortText by remember { mutableStateOf("0") }
    var address by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedParentId by remember { mutableStateOf<Long?>(null) }
    var latText by remember { mutableStateOf(String.format(Locale.US, "%.6f", initialLat)) }
    var lngText by remember { mutableStateOf(String.format(Locale.US, "%.6f", initialLng)) }
    var showParentPicker by remember { mutableStateOf(false) }

    val selectedParent = remember(existingNodes, selectedParentId) {
        existingNodes.firstOrNull { it.id == selectedParentId }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header matching Gambar 4: centered title + amber Batal
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = "Tambah Distribusi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    Text(
                        text = "Batal",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = NocAmber,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onDismiss)
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                            .testTag("dist_cancel_button")
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 560.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "TYPE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.8.sp
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FtthNodeType.entries.forEach { t ->
                            val isSelected = selectedType == t
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) NocAmber.copy(alpha = 0.14f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) NocAmber else MaterialTheme.colorScheme.outline,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { selectedType = t }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                                    .testTag("dist_type_${t.name.lowercase()}")
                            ) {
                                Text(
                                    text = t.label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) NocAmber else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    ModalInputField(
                        label = "Nama",
                        value = name,
                        placeholder = "ODP Melati",
                        onValueChange = { newName ->
                            name = newName
                            if (!isCodeManuallyEdited) {
                                code = newName
                                    .trimStart()
                                    .uppercase(Locale.ROOT)
                                    .replace(Regex("\\s+"), "-")
                                    .replace(Regex("[^A-Z0-9-]"), "")
                            }
                        },
                        testTag = "dist_name_input"
                    )

                    ModalInputField(
                        label = "Kode",
                        value = code,
                        placeholder = "ODP-MELATI",
                        onValueChange = { newCode ->
                            val upper = newCode.uppercase(Locale.ROOT)
                            code = upper
                            isCodeManuallyEdited = upper.isNotBlank()
                        },
                        capitalization = KeyboardCapitalization.Characters,
                        useMono = true,
                        testTag = "dist_code_input"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ModalInputField(
                            label = "Limit Port",
                            value = limitPortText,
                            placeholder = "8",
                            onValueChange = { limitPortText = it.filter { c -> c.isDigit() } },
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.weight(1f)
                        )
                        ModalInputField(
                            label = "Extra Port",
                            value = extraPortText,
                            placeholder = "0",
                            onValueChange = { extraPortText = it.filter { c -> c.isDigit() } },
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    ModalInputField(
                        label = "Alamat",
                        value = address,
                        placeholder = "Jl. Contoh No. 1",
                        onValueChange = { address = it }
                    )

                    ModalInputField(
                        label = "Deskripsi",
                        value = description,
                        placeholder = "Opsional",
                        onValueChange = { description = it }
                    )

                    // Parent Picker Card matching Gambar 4 & 5
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                            .clickable { showParentPicker = true }
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                            .testTag("dist_parent_picker"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(NocAmber.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AccountTree,
                                    contentDescription = null,
                                    tint = NocAmber,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Distribusi Sumber (Parent)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = selectedParent?.let { "${it.code} • ${it.name}" } ?: "Tanpa parent",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = "Pilih Parent",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = "KOORDINAT (DARI PETA)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.8.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ModalInputField(
                            label = "Latitude",
                            value = latText,
                            placeholder = "-6.922603",
                            onValueChange = { latText = it },
                            keyboardType = KeyboardType.Decimal,
                            useMono = true,
                            modifier = Modifier.weight(1f)
                        )
                        ModalInputField(
                            label = "Longitude",
                            value = lngText,
                            placeholder = "111.477886",
                            onValueChange = { lngText = it },
                            keyboardType = KeyboardType.Decimal,
                            useMono = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    val parsedLat = latText.replace(',', '.').toDoubleOrNull()
                    val parsedLng = lngText.replace(',', '.').toDoubleOrNull()
                    val canSubmit = name.isNotBlank() && parsedLat != null && parsedLng != null

                    Button(
                        onClick = {
                            val finalName = name.trim()
                            val finalCode = code.trim().ifBlank {
                                finalName.uppercase().replace(Regex("[^A-Z0-9]+"), "-").trim('-')
                            }
                            onSubmit(
                                selectedType,
                                finalName,
                                finalCode,
                                limitPortText.toIntOrNull()?.coerceAtLeast(1) ?: 8,
                                extraPortText.toIntOrNull()?.coerceAtLeast(0) ?: 0,
                                address.trim(),
                                description.trim(),
                                selectedParentId,
                                parsedLat ?: initialLat,
                                parsedLng ?: initialLng
                            )
                        },
                        enabled = canSubmit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("dist_submit_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NocAmber,
                            contentColor = Color(0xFF0B0E14)
                        )
                    ) {
                        Text(
                            text = "Tambah",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }

    if (showParentPicker) {
        AlertDialog(
            onDismissRequest = { showParentPicker = false },
            title = { Text("Pilih Distribusi Sumber (Parent)") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedParentId = null
                                    showParentPicker = false
                                }
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Tanpa parent (Root)", fontWeight = FontWeight.SemiBold)
                            if (selectedParentId == null) {
                                Icon(Icons.Rounded.Check, contentDescription = null, tint = NocAmber)
                            }
                        }
                        HorizontalDivider()
                    }
                    items(existingNodes, key = { it.id }) { node ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedParentId = node.id
                                    showParentPicker = false
                                }
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${node.type.label} • ${node.name}", fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = node.code,
                                    fontFamily = JetBrainsMonoFamily,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (selectedParentId == node.id) {
                                Icon(Icons.Rounded.Check, contentDescription = null, tint = NocAmber)
                            }
                        }
                        HorizontalDivider()
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showParentPicker = false }) { Text("Tutup") }
            }
        )
    }
}

@Composable
private fun ModalInputField(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    useMono: Boolean = false,
    testTag: String? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontFamily = if (useMono) JetBrainsMonoFamily else MaterialTheme.typography.bodyMedium.fontFamily
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                capitalization = capitalization
            ),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontFamily = if (useMono) JetBrainsMonoFamily else MaterialTheme.typography.bodyMedium.fontFamily
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
        )
    }
}

@Composable
fun AddCustomerPinDialog(
    targetNode: FtthNodeEntity,
    pppoeClients: List<PppoeClient>,
    onDismiss: () -> Unit,
    onSubmit: (name: String, pppoeUsername: String, lat: Double, lng: Double, address: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var pppoeUser by remember { mutableStateOf("") }
    var address by remember { mutableStateOf(targetNode.address) }
    var latText by remember { mutableStateOf(String.format(Locale.US, "%.6f", targetNode.latitude + 0.00045)) }
    var lngText by remember { mutableStateOf(String.format(Locale.US, "%.6f", targetNode.longitude + 0.00045)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pin Pelanggan ke ${targetNode.code}") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (pppoeClients.isNotEmpty()) {
                    Text(
                        text = "Pilih cepat dari PPPoE aktif:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        pppoeClients.take(8).forEach { client ->
                            OutlinedButton(
                                onClick = {
                                    name = client.name
                                    pppoeUser = client.name
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(client.name, fontSize = 11.sp)
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Pelanggan") },
                    placeholder = { Text("Nia") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = pppoeUser,
                    onValueChange = { pppoeUser = it },
                    label = { Text("Username PPPoE (untuk status live)") },
                    placeholder = { Text("nia@fiber") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Alamat") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = latText,
                        onValueChange = { latText = it },
                        label = { Text("Latitude") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = lngText,
                        onValueChange = { lngText = it },
                        label = { Text("Longitude") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val lat = latText.replace(',', '.').toDoubleOrNull() ?: targetNode.latitude
                    val lng = lngText.replace(',', '.').toDoubleOrNull() ?: targetNode.longitude
                    onSubmit(name.trim(), pppoeUser.trim(), lat, lng, address.trim())
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = NocAmber, contentColor = Color.Black)
            ) {
                Text("Simpan Pin")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
fun SaveCableRouteDialog(
    pointCount: Int,
    totalMeters: Int,
    onDismiss: () -> Unit,
    onSave: (name: String, coreCount: Int) -> Unit
) {
    var name by remember { mutableStateOf("Feeder FO-${System.currentTimeMillis().toString().takeLast(3)}") }
    var coreText by remember { mutableStateOf("12") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Simpan Rute Kabel") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "$pointCount titik belok • Estimasi panjang $totalMeters meter",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Jalur Kabel") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = coreText,
                    onValueChange = { coreText = it.filter { c -> c.isDigit() } },
                    label = { Text("Jumlah Core (mis. 12 / 24)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name.trim(), coreText.toIntOrNull()?.coerceAtLeast(1) ?: 12) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = NocAmber, contentColor = Color.Black)
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
fun NodeDetailSheetDialog(
    node: FtthNodeEntity,
    linkedCustomers: Int,
    parentCode: String?,
    onDismiss: () -> Unit,
    onAddCustomerPin: () -> Unit,
    onDeleteNode: () -> Unit
) {
    val cap = (node.limitPort + node.extraPort).coerceAtLeast(1)
    val used = maxOf(node.usedPorts, linkedCustomers)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${node.type.label} • ${node.code}", fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Tutup")
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(node.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Parent: ${parentCode ?: "Tanpa parent"}", style = MaterialTheme.typography.bodySmall)
                Text("Kapasitas: $used / $cap port", fontFamily = JetBrainsMonoFamily, fontSize = 12.sp)
                if (node.address.isNotBlank()) {
                    Text("Alamat: ${node.address}", style = MaterialTheme.typography.bodySmall)
                }
                if (node.description.isNotBlank()) {
                    Text("Catatan: ${node.description}", style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    text = String.format(Locale.US, "Koordinat: %.6f, %.6f", node.latitude, node.longitude),
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onAddCustomerPin,
                colors = ButtonDefaults.buttonColors(containerColor = NocAmber, contentColor = Color.Black)
            ) {
                Text("Pin Pelanggan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDeleteNode) {
                Text("Hapus Titik", color = NocRedOffline)
            }
        }
    )
}
