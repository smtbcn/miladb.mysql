package com.miladb.ui.screen.connection

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.miladb.R
import com.miladb.data.model.ConnectionConfig
import com.miladb.data.model.ConnectionUiState
import com.miladb.data.model.SavedConnection
import com.miladb.data.model.SshConfig
import com.miladb.ui.component.LoadingIndicator

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ConnectionScreen(
    viewModel: ConnectionViewModel,
    onConnectionSuccess: (database: String?) -> Unit
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val savedConnections by viewModel.savedConnections.collectAsState()
    val sshEnabled by viewModel.sshEnabled.collectAsState()
    val sslEnabled by viewModel.sslEnabled.collectAsState()
    val exportJson by viewModel.exportJson.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Form state
    var connectionName by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("3306") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var database by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var saveConnection by remember { mutableStateOf(true) } // Varsayılan olarak seçili
    
    // SSH form state
    var sshHost by remember { mutableStateOf("") }
    var sshPort by remember { mutableStateOf("22") }
    var sshUsername by remember { mutableStateOf("") }
    var sshPassword by remember { mutableStateOf("") }
    var sshPasswordVisible by remember { mutableStateOf(false) }
    
    // Gerçek şifreleri sakla (kayıtlı bağlantıdan geldiğinde)
    var realPassword by remember { mutableStateOf<String?>(null) }
    var realSshPassword by remember { mutableStateOf<String?>(null) }
    var realHost by remember { mutableStateOf<String?>(null) }
    var realSshHost by remember { mutableStateOf<String?>(null) }
    
    // Delete dialog
    var showDeleteDialog by remember { mutableStateOf(false) }
    var connectionToDelete by remember { mutableStateOf<SavedConnection?>(null) }
    
    // Form visibility - varsayılan olarak kapalı
    var showNewConnectionForm by remember { mutableStateOf(false) }
    
    // Import/Export dialogs
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }
    
    // Bağlantı durumunu izle
    LaunchedEffect(connectionState) {
        when (val state = connectionState) {
            is ConnectionUiState.Success -> {
                onConnectionSuccess(null)
                viewModel.resetConnectionState()
            }
            is ConnectionUiState.SuccessWithDatabase -> {
                // Veritabanı adı ile bağlanıldı, direkt tablo listesine git
                onConnectionSuccess(state.database)
                viewModel.resetConnectionState()
            }
            is ConnectionUiState.Error -> {
                snackbarHostState.showSnackbar(
                    message = state.message,
                    duration = SnackbarDuration.Long
                )
                viewModel.resetConnectionState()
            }
            else -> {}
        }
    }
    
    Scaffold(
        snackbarHost = { 
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(Alignment.Bottom)
                    .padding(bottom = 16.dp)
            )
        },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.connection_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF003258), // Koyu mavi
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = {
                        viewModel.exportConnections()
                        showExportDialog = true
                    }) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Dışa Aktar")
                    }
                    IconButton(onClick = {
                        showImportDialog = true
                    }) {
                        Icon(Icons.Default.FileUpload, contentDescription = "İçe Aktar")
                    }
                }
            )
        },

        contentWindowInsets = WindowInsets.ime // Klavye açıldığında içeriği yukarı kaydır
    ) { paddingValues ->
        if (connectionState is ConnectionUiState.Loading) {
            LoadingIndicator()
        } else {
            val focusManager = LocalFocusManager.current
            
            // Form gösteriliyorsa, tam ekran form
            if (showNewConnectionForm) {
                val scrollState = rememberScrollState()
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(scrollState)
                        .padding(16.dp)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            focusManager.clearFocus()
                        },
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Yeni Bağlantı",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        IconButton(onClick = { 
                            showNewConnectionForm = false
                            // Formu temizle
                            connectionName = ""
                            host = ""
                            port = "3306"
                            username = ""
                            password = ""
                            database = ""
                            realHost = null
                            realPassword = null
                            realSshHost = null
                            realSshPassword = null
                        }) {
                            Icon(Icons.Default.Close, "Kapat")
                        }
                    }
                    
                    OutlinedTextField(
                        value = connectionName,
                        onValueChange = { connectionName = it },
                        label = { Text("Bağlantı Adı") },
                        placeholder = { Text("Örn: Yerel Sunucu") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Label, null) }
                    )
                    
                    OutlinedTextField(
                        value = host,
                        onValueChange = { 
                            host = it
                            // Kullanıcı değiştirirse gerçek değeri güncelle
                            realHost = it
                        },
                        label = { Text(stringResource(R.string.host_label)) },
                        placeholder = { Text(stringResource(R.string.host_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it },
                        label = { Text(stringResource(R.string.port_label)) },
                        placeholder = { Text(stringResource(R.string.port_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text(stringResource(R.string.username_label)) },
                        placeholder = { Text(stringResource(R.string.username_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    OutlinedTextField(
                        value = password,
                        onValueChange = { 
                            password = it
                            // Kullanıcı değiştirirse gerçek değeri güncelle
                            realPassword = it
                        },
                        label = { Text(stringResource(R.string.password_label)) },
                        placeholder = { Text(stringResource(R.string.password_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (passwordVisible) 
                            VisualTransformation.None 
                        else 
                            PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) 
                                        Icons.Default.Visibility 
                                    else 
                                        Icons.Default.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        },
                        singleLine = true
                    )
                    
                    OutlinedTextField(
                        value = database,
                        onValueChange = { database = it },
                        label = { Text(stringResource(R.string.database_label)) },
                        placeholder = { Text(stringResource(R.string.database_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    // SSL Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.ssl_label))
                        Switch(
                            checked = sslEnabled,
                            onCheckedChange = { viewModel.setSslEnabled(it) }
                        )
                    }
                    
                    // SSH Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.ssh_label))
                        Switch(
                            checked = sshEnabled,
                            onCheckedChange = { viewModel.setSshEnabled(it) }
                        )
                    }
                    
                    // SSH Configuration
                    if (sshEnabled) {
                        Text(
                            text = "SSH Tünel Ayarları",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary // Açık mavi
                        )
                        
                        OutlinedTextField(
                            value = sshHost,
                            onValueChange = { 
                                sshHost = it
                                realSshHost = it
                            },
                            label = { Text(stringResource(R.string.ssh_host_label)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        
                        OutlinedTextField(
                            value = sshPort,
                            onValueChange = { sshPort = it },
                            label = { Text(stringResource(R.string.ssh_port_label)) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        
                        OutlinedTextField(
                            value = sshUsername,
                            onValueChange = { sshUsername = it },
                            label = { Text(stringResource(R.string.ssh_username_label)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        
                        OutlinedTextField(
                            value = sshPassword,
                            onValueChange = { 
                                sshPassword = it
                                realSshPassword = it
                            },
                            label = { Text(stringResource(R.string.ssh_password_label)) },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = if (sshPasswordVisible) 
                                VisualTransformation.None 
                            else 
                                PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { sshPasswordVisible = !sshPasswordVisible }) {
                                    Icon(
                                        imageVector = if (sshPasswordVisible) 
                                            Icons.Default.Visibility 
                                        else 
                                            Icons.Default.VisibilityOff,
                                        contentDescription = null
                                    )
                                }
                            },
                            singleLine = true
                        )
                    }
                    
                    // Save Connection Checkbox
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = saveConnection,
                            onCheckedChange = { saveConnection = it }
                        )
                        Text("Bu bağlantıyı kaydet")
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Connect Button
                    Button(
                        onClick = {
                            // Gerçek değerleri kullan (kayıtlı bağlantıdan geldiyse)
                            val actualHost = realHost ?: host
                            val actualPassword = realPassword ?: password
                            val actualSshHost = realSshHost ?: sshHost
                            val actualSshPassword = realSshPassword ?: sshPassword
                            
                            val sshConfig = if (sshEnabled && actualSshHost.isNotBlank()) {
                                SshConfig(
                                    host = actualSshHost,
                                    port = sshPort.toIntOrNull() ?: 22,
                                    username = sshUsername,
                                    password = actualSshPassword.ifBlank { null },
                                    remoteHost = actualHost,
                                    remotePort = port.toIntOrNull() ?: 3306
                                )
                            } else null
                            
                            val config = ConnectionConfig(
                                host = actualHost,
                                port = port.toIntOrNull() ?: 3306,
                                username = username,
                                password = actualPassword,
                                database = database.ifBlank { null },
                                useSsl = sslEnabled,
                                sshConfig = sshConfig
                            )
                            
                            // Bağlantıyı kaydet - kopya kontrolü ile
                            if (saveConnection && connectionName.isNotBlank()) {
                                // Aynı sunucu bilgilerine sahip kayıt var mı kontrol et
                                val existingConnection = savedConnections.find { 
                                    it.host == actualHost && 
                                    it.port == (port.toIntOrNull() ?: 3306) && 
                                    it.username == username &&
                                    it.database == database.ifBlank { null }
                                }
                                
                                if (existingConnection == null) {
                                    // Yeni kayıt oluştur
                                    val savedConn = SavedConnection(
                                        id = viewModel.connectionStorage.generateId(),
                                        name = connectionName,
                                        host = actualHost,
                                        port = port.toIntOrNull() ?: 3306,
                                        username = username,
                                        password = actualPassword,
                                        database = database.ifBlank { null },
                                        useSsl = sslEnabled,
                                        useSsh = sshEnabled,
                                        sshHost = if (sshEnabled) actualSshHost else null,
                                        sshPort = if (sshEnabled) sshPort.toIntOrNull() else null,
                                        sshUsername = if (sshEnabled) sshUsername else null,
                                        sshPassword = if (sshEnabled) actualSshPassword else null
                                    )
                                    viewModel.saveConnection(savedConn)
                                }
                                // Eğer aynı kayıt varsa, yeni kayıt oluşturma (kopya önleme)
                            }
                            
                            viewModel.connect(config)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = (host.isNotBlank() || realHost != null) && 
                                 username.isNotBlank() && 
                                 (password.isNotBlank() || realPassword != null) &&
                                 (!sshEnabled || ((sshHost.isNotBlank() || realSshHost != null) && sshUsername.isNotBlank()))
                    ) {
                        Icon(Icons.Default.Link, null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.connect_button))
                    }
                }
            } else {
                // Ana ekran - Kayıtlı bağlantılar veya boş ekran
                if (savedConnections.isEmpty()) {
                    // Boş ekran - İlk kullanım
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "Henüz kayıtlı bağlantı yok",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Başlamak için yeni bir bağlantı ekleyin",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(24.dp))
                            Button(
                                onClick = { showNewConnectionForm = true },
                                modifier = Modifier.height(48.dp)
                            ) {
                                Icon(Icons.Default.Add, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Yeni Bağlantı Ekle")
                            }
                        }
                    }
                } else {
                    // Kayıtlı bağlantılar listesi
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 16.dp,
                            bottom = 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Başlık ve buton
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Bağlantılarım",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                
                                Button(
                                    onClick = { showNewConnectionForm = true },
                                    modifier = Modifier.height(40.dp)
                                ) {
                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Yeni")
                                }
                            }
                        }
                        
                        // Demo bağlantısı açıklaması (sadece demo bağlantısı varsa göster)
                        if (savedConnections.any { it.id == "demo_connection_001" }) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "Demo sunucu ile uygulamayı deneyebilirsiniz. Kendi sunucularınızı ekleyerek devam edin.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                        
                        // Bağlantı kartları
                        items(savedConnections) { savedConn ->
                                SavedConnectionCard(
                                    connection = savedConn,
                                    onClick = {
                                        // Direkt bağlan
                                        val sshConfig = if (savedConn.useSsh && !savedConn.sshHost.isNullOrBlank()) {
                                            SshConfig(
                                                host = savedConn.sshHost,
                                                port = savedConn.sshPort ?: 22,
                                                username = savedConn.sshUsername ?: "",
                                                password = savedConn.sshPassword,
                                                remoteHost = savedConn.host,
                                                remotePort = savedConn.port
                                            )
                                        } else null
                                        
                                        val config = ConnectionConfig(
                                            host = savedConn.host,
                                            port = savedConn.port,
                                            username = savedConn.username,
                                            password = savedConn.password ?: "",
                                            database = savedConn.database,
                                            useSsl = savedConn.useSsl,
                                            sshConfig = sshConfig
                                        )
                                        
                                        viewModel.connect(config)
                                    },
                                    onLongClick = {
                                        connectionToDelete = savedConn
                                        showDeleteDialog = true
                                    }
                                )
                            }
                            
                            // Versiyon bilgisi
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 32.dp, bottom = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "v${com.miladb.BuildConfig.VERSION_NAME}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }
        }
    }
    
    // Export dialog
    if (showExportDialog) {
        val clipboard = LocalClipboardManager.current
        AlertDialog(
            onDismissRequest = {
                showExportDialog = false
                viewModel.clearExportJson()
            },
            title = { Text("Bağlantıları Dışa Aktar (JSON)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Aşağıdaki JSON'u kopyalayarak dışa aktarabilirsiniz.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = exportJson ?: "[]",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp),
                        maxLines = 10
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    clipboard.setText(AnnotatedString(exportJson ?: "[]"))
                    showExportDialog = false
                    viewModel.clearExportJson()
                }) { Text("Kopyala") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showExportDialog = false
                    viewModel.clearExportJson()
                }) { Text("Kapat") }
            }
        )
    }
    
    // Import dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Bağlantıları İçe Aktar (JSON)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "JSON formatında bir dizi bağlantı girin.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        placeholder = { Text("[ { \"name\": \"Sunucu\", \"host\": \"127.0.0.1\", ... } ]") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 160.dp),
                        maxLines = 12
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.importConnectionsFromJson(importJsonText) { ok ->
                        if (ok) {
                            importJsonText = ""
                            showImportDialog = false
                        }
                    }
                }) { Text("İçe Aktar") }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) { Text("İptal") }
            }
        )
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog && connectionToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                connectionToDelete = null
            },
            title = { Text("Bağlantıyı Sil") },
            text = { Text("'${connectionToDelete!!.name}' bağlantısını silmek istediğinizden emin misiniz?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteConnection(connectionToDelete!!.id)
                        showDeleteDialog = false
                        connectionToDelete = null
                    }
                ) {
                    Text("Sil")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        connectionToDelete = null
                    }
                ) {
                    Text("İptal")
                }
            }
        )
    }
}

/**
 * IP adresini sansürler (örn: 192.168.1.1 -> 192.168.**.** )
 */
private fun maskIpAddress(ip: String): String {
    val parts = ip.split(".")
    return if (parts.size == 4) {
        "${parts[0]}.${parts[1]}.**.**"
    } else {
        // IPv6 veya hostname ise ilk kısmı göster
        if (ip.length > 10) {
            ip.take(6) + "***"
        } else {
            ip
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SavedConnectionCard(
    connection: SavedConnection,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Storage,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary, // Açık mavi
                modifier = Modifier.size(32.dp)
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = connection.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    // Demo bağlantısı için özel etiket
                    if (connection.id == "demo_connection_001") {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            Text(
                                text = "DEMO",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = "${connection.username}@${maskIpAddress(connection.host)}:${connection.port}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (connection.useSsh) {
                Icon(
                    imageVector = Icons.Default.VpnKey,
                    contentDescription = "SSH",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
