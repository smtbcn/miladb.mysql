package com.miladb.ui.screen.table

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.miladb.R
import com.miladb.data.model.ColumnDefinition
import com.miladb.data.model.TableDefinition
import com.miladb.data.model.TableOperationUiState

/**
 * Tablo oluşturma ekranı.
 * 
 * Özellikler:
 * - Tablo adı girişi
 * - Kolon ekleme/çıkarma
 * - Her kolon için: ad, tip, uzunluk, NULL, PK, AI
 * - "Oluştur" ve "İptal" butonları
 * - Validasyon
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTableScreen(
    tableViewModel: TableViewModel,
    database: String,
    onCreated: () -> Unit,
    onCancelled: () -> Unit
) {
    val tableOperationState by tableViewModel.tableOperationState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var tableName by remember { mutableStateOf("") }
    val columns = remember { mutableStateListOf<ColumnState>() }
    var showValidationError by remember { mutableStateOf(false) }
    var validationErrorMessage by remember { mutableStateOf("") }
    
    // İşlem durumunu izle
    LaunchedEffect(tableOperationState) {
        when (tableOperationState) {
            is TableOperationUiState.Success -> {
                snackbarHostState.showSnackbar(
                    message = (tableOperationState as TableOperationUiState.Success).message,
                    duration = SnackbarDuration.Short
                )
                tableViewModel.resetTableOperationState()
                onCreated()
            }
            is TableOperationUiState.Error -> {
                snackbarHostState.showSnackbar(
                    message = (tableOperationState as TableOperationUiState.Error).message,
                    duration = SnackbarDuration.Long
                )
                tableViewModel.resetTableOperationState()
            }
            else -> {}
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.create_table_title)) },
                navigationIcon = {
                    IconButton(onClick = onCancelled) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back_button)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF003258), // Koyu mavi
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        contentWindowInsets = WindowInsets.ime // Klavye açıldığında içeriği yukarı kaydır
    ) { paddingValues ->
        val focusManager = LocalFocusManager.current
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    focusManager.clearFocus() // Klavyeyi kapat
                },
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tablo adı
            OutlinedTextField(
                value = tableName,
                onValueChange = { tableName = it },
                label = { Text(stringResource(R.string.table_name_label)) },
                placeholder = { Text(stringResource(R.string.table_name_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Divider()
            
            // Kolonlar
            Text(
                text = "Kolonlar",
                style = MaterialTheme.typography.titleMedium
            )
            
            columns.forEachIndexed { index, column ->
                ColumnEditor(
                    column = column,
                    onRemove = { columns.removeAt(index) }
                )
            }
            
            // Kolon ekle butonu
            OutlinedButton(
                onClick = {
                    columns.add(
                        ColumnState(
                            name = "",
                            type = "VARCHAR",
                            length = "255",
                            nullable = true,
                            isPrimaryKey = false,
                            isAutoIncrement = false
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.add_column_button))
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Butonlar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCancelled,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.cancel_button))
                }
                
                Button(
                    onClick = {
                        // Validasyon
                        when {
                            tableName.isBlank() -> {
                                validationErrorMessage = "Tablo adı boş olamaz"
                                showValidationError = true
                                return@Button
                            }
                            columns.isEmpty() -> {
                                validationErrorMessage = "En az bir kolon eklemelisiniz"
                                showValidationError = true
                                return@Button
                            }
                            columns.any { it.name.isBlank() } -> {
                                validationErrorMessage = "Tüm kolonların adı olmalıdır"
                                showValidationError = true
                                return@Button
                            }
                            columns.none { it.isPrimaryKey } -> {
                                validationErrorMessage = "En az bir primary key seçmelisiniz"
                                showValidationError = true
                                return@Button
                            }
                        }
                        
                        // Tablo oluştur
                        val tableDefinition = TableDefinition(
                            tableName = tableName,
                            columns = columns.map { it.toColumnDefinition() }
                        )
                        tableViewModel.createTable(database, tableDefinition)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = tableOperationState !is TableOperationUiState.Processing
                ) {
                    if (tableOperationState is TableOperationUiState.Processing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(stringResource(R.string.create_button))
                    }
                }
            }
        }
        
        // Validasyon hatası dialogu
        if (showValidationError) {
            AlertDialog(
                onDismissRequest = { showValidationError = false },
                title = { Text(stringResource(R.string.warning)) },
                text = { Text(validationErrorMessage) },
                confirmButton = {
                    TextButton(onClick = { showValidationError = false }) {
                        Text(stringResource(R.string.ok))
                    }
                }
            )
        }
    }
}

/**
 * Kolon state sınıfı - Compose state ile çalışır.
 */
class ColumnState(
    name: String = "",
    type: String = "VARCHAR",
    length: String = "255",
    nullable: Boolean = true,
    isPrimaryKey: Boolean = false,
    isAutoIncrement: Boolean = false
) {
    var name by mutableStateOf(name)
    var type by mutableStateOf(type)
    var length by mutableStateOf(length)
    var nullable by mutableStateOf(nullable)
    var isPrimaryKey by mutableStateOf(isPrimaryKey)
    var isAutoIncrement by mutableStateOf(isAutoIncrement)
    
    fun toColumnDefinition() = ColumnDefinition(
        name = name,
        type = type,
        length = length.toIntOrNull(),
        nullable = nullable,
        isPrimaryKey = isPrimaryKey,
        isAutoIncrement = isAutoIncrement
    )
}

/**
 * Kolon düzenleyici bileşeni.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColumnEditor(
    column: ColumnState,
    onRemove: () -> Unit
) {
    var typeExpanded by remember { mutableStateOf(false) }
    val columnTypes = listOf(
        "INT", "BIGINT", "SMALLINT", "TINYINT",
        "VARCHAR", "TEXT", "LONGTEXT", "CHAR",
        "DECIMAL", "FLOAT", "DOUBLE",
        "DATE", "DATETIME", "TIMESTAMP", "TIME",
        "BOOLEAN", "BLOB", "JSON"
    )
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                Text(
                    text = "Kolon",
                    style = MaterialTheme.typography.titleSmall
                )
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.remove_column_button))
                }
            }
            
            OutlinedTextField(
                value = column.name,
                onValueChange = { column.name = it },
                label = { Text(stringResource(R.string.column_name_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Tip dropdown
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = column.type,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.column_type_label)) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        singleLine = true
                    )
                    
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        columnTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    column.type = type
                                    typeExpanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }
                
                OutlinedTextField(
                    value = column.length,
                    onValueChange = { column.length = it },
                    label = { Text(stringResource(R.string.column_length_label)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    enabled = column.type in listOf("VARCHAR", "CHAR", "DECIMAL")
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = column.nullable,
                        onCheckedChange = { column.nullable = it },
                        enabled = !column.isPrimaryKey
                    )
                    Text(stringResource(R.string.column_nullable_label))
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = column.isPrimaryKey,
                        onCheckedChange = { 
                            column.isPrimaryKey = it
                            if (it) column.nullable = false
                        }
                    )
                    Text(stringResource(R.string.column_primary_key_label))
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = column.isAutoIncrement,
                        onCheckedChange = { column.isAutoIncrement = it },
                        enabled = column.type in listOf("INT", "BIGINT", "SMALLINT", "TINYINT")
                    )
                    Text(stringResource(R.string.column_auto_increment_label))
                }
            }
        }
    }
}
