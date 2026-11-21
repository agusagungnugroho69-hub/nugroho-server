package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.RouterConfig
import com.example.data.model.RouterProtocol
import com.example.ui.theme.NocAmber
import com.example.ui.theme.NocRedOffline

@Composable
fun RouterEditorSheet(
    initialRouter: RouterConfig?,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onSave: (RouterConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialRouter?.name ?: "") }
    var host by remember { mutableStateOf(initialRouter?.host ?: "") }
    var portText by remember { mutableStateOf((initialRouter?.port ?: 8728).toString()) }
    var username by remember { mutableStateOf(initialRouter?.username ?: "") }
    var password by remember { mutableStateOf(initialRouter?.password ?: "") }
    var showPassword by remember { mutableStateOf(false) }
    var rosMajorVersion by remember { mutableIntStateOf(initialRouter?.routerOsMajorVersion ?: 6) }
    var validationError by remember { mutableStateOf<String?>(null) }

    fun inferredProtocol(port: Int): RouterProtocol = when (port) {
        8729 -> RouterProtocol.ROS_API_TLS
        80 -> RouterProtocol.ROS_REST_HTTP
        443 -> RouterProtocol.ROS_REST_HTTPS
        8728 -> RouterProtocol.ROS_API_PLAIN
        else -> initialRouter?.protocol ?: RouterProtocol.ROS_API_PLAIN
    }

    fun buildCandidate(): RouterConfig? {
        val cleanHost = host.trim()
        val cleanUser = username.trim()
        val parsedPort = portText.trim().toIntOrNull()
        if (cleanHost.isEmpty()) {
            validationError = "Host / IP router MikroTik wajib diisi."
            return null
        }
        if (parsedPort == null || parsedPort !in 1..65535) {
            validationError = "Port API harus di antara 1 sampai 65535."
            return null
        }
        val isSim = initialRouter?.isSimulated ?: false
        if (!isSim && cleanUser.isEmpty()) {
            validationError = "Username MikroTik wajib diisi."
            return null
        }
        validationError = null
        val finalName = name.trim().ifEmpty { cleanHost }
        return RouterConfig(
            id = initialRouter?.id ?: 0L,
            name = finalName,
            host = cleanHost,
            port = parsedPort,
            username = cleanUser.ifEmpty { "admin" },
            password = password,
            protocol = inferredProtocol(parsedPort),
            routerOsMajorVersion = rosMajorVersion,
            monitoredInterface = initialRouter?.monitoredInterface ?: "ether1",
            cpuAlertThreshold = initialRouter?.cpuAlertThreshold ?: 85,
            isSimulated = isSim,
            createdAt = initialRouter?.createdAt ?: System.currentTimeMillis()
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        BackHandler(onBack = onDismiss)

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Header matching Gambar 2 (< Tambah Server + Theme Toggle)
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
                                .clickable(onClick = onDismiss)
                                .testTag("router_editor_back"),
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
                            text = if (initialRouter == null) "Tambah Server" else "Edit Server",
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

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (initialRouter == null) {
                            "Tambahkan server MikroTik untuk mulai mengelola jaringan."
                        } else {
                            "Perbarui konfigurasi koneksi API server MikroTik."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    ServerPillField(
                        label = "Nama Server",
                        value = name,
                        placeholder = "MAIN SERVER",
                        helper = "Berikan nama yang mudah diidentifikasi",
                        icon = Icons.Outlined.Storage,
                        onValueChange = { name = it },
                        testTag = "router_name_input"
                    )

                    ServerPillField(
                        label = "Host",
                        value = host,
                        placeholder = "192.168.88.1",
                        helper = "IP router MikroTik (format x.x.x.x)",
                        icon = Icons.Outlined.Language,
                        onValueChange = { host = it },
                        testTag = "router_host_input"
                    )

                    ServerPillField(
                        label = "Port",
                        value = portText,
                        placeholder = "8728",
                        helper = "Default API MikroTik: 8728",
                        icon = Icons.Outlined.AccountTree,
                        keyboardType = KeyboardType.Number,
                        onValueChange = { portText = it.filter { c -> c.isDigit() } },
                        testTag = "router_port_input"
                    )

                    ServerPillField(
                        label = "Username",
                        value = username,
                        placeholder = "admin",
                        helper = "User untuk login MikroTik via API",
                        icon = Icons.Outlined.Person,
                        onValueChange = { username = it },
                        testTag = "router_username_input"
                    )

                    ServerPillField(
                        label = "Password",
                        value = password,
                        placeholder = "••••••••",
                        helper = "Dienkripsi AES-256 sebelum disimpan",
                        icon = Icons.Outlined.Lock,
                        isPassword = true,
                        passwordVisible = showPassword,
                        onTogglePassword = { showPassword = !showPassword },
                        onValueChange = { password = it },
                        testTag = "router_password_input"
                    )

                    // Versi RouterOS selector matching Gambar 2
                    Text(
                        text = "Versi RouterOS",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RouterOsVersionCard(
                            label = "RouterOS 6",
                            selected = rosMajorVersion == 6,
                            onClick = { rosMajorVersion = 6 },
                            modifier = Modifier.weight(1f)
                        )
                        RouterOsVersionCard(
                            label = "RouterOS 7",
                            selected = rosMajorVersion == 7,
                            onClick = { rosMajorVersion = 7 },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    validationError?.let { err ->
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodySmall,
                            color = NocRedOffline
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = { buildCandidate()?.let { onSave(it) } },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NocAmber,
                            contentColor = Color(0xFF0B0E14)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("save_router_button")
                    ) {
                        Text(
                            text = if (initialRouter == null) "Tambah Server" else "Simpan Perubahan",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun ServerPillField(
    label: String,
    value: String,
    placeholder: String,
    helper: String?,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePassword: (() -> Unit)? = null,
    onValueChange: (String) -> Unit,
    testTag: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
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
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(NocAmber.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = NocAmber,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                            )
                        }
                        BasicTextField(
                            value = value,
                            onValueChange = onValueChange,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                            visualTransformation = if (isPassword && !passwordVisible) {
                                PasswordVisualTransformation()
                            } else {
                                VisualTransformation.None
                            },
                            cursorBrush = SolidColor(NocAmber),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(testTag)
                        )
                    }
                }

                if (isPassword && onTogglePassword != null) {
                    IconButton(onClick = onTogglePassword, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            contentDescription = "Tampilkan password",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        if (!helper.isNullOrBlank()) {
            Text(
                text = helper,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

@Composable
private fun RouterOsVersionCard(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (selected) NocAmber else MaterialTheme.colorScheme.outline
    val containerColor = if (selected) NocAmber.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface
    val textColor = if (selected) NocAmber else MaterialTheme.colorScheme.onSurface

    Surface(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = containerColor
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}
