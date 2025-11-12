package com.miladb.ui.screen.table

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import com.miladb.R
import android.util.Log
import com.miladb.data.model.RowOperationUiState
import com.miladb.data.model.TableStructureUiState
import com.miladb.ui.component.LoadingIndicator
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding

/**
 * Satır düzenleme/ekleme ekranı.
 * 
 * Özellikler:
 * - Tüm kolonlar için düzenlenebilir alanlar
 * - Kolon tipleri gösterimi
 * - Primary key kolonunu disabled göster (sadece yeni kayıt değilse)
 * - "Kaydet" ve "İptal" butonları
 * - Validasyon
 * - Loading durumu
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RowEditorScreen(
    tableViewModel: TableViewModel,
    database: String,
    table: String,
    rowData: Map<String, String>?,
    isNewRow: Boolean,
    onSaved: () -> Unit,
    onCancelled: () -> Unit
) {
    val tableStructureState by tableViewModel.tableStructureState.collectAsState()
    val rowOperationState by tableViewModel.rowOperationState.collectAsState()
    val selectedRowData by tableViewModel.selectedRowData.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Form state - her kolon için değer
    val fieldValues = remember { mutableStateMapOf<String, String>() }
    
    // İlk yüklemede tablo yapısını getir
    LaunchedEffect(database, table) {
        tableViewModel.loadTableStructure(database, table)
    }
    
    // Mevcut satır verilerini form'a yükle
    LaunchedEffect(selectedRowData, tableStructureState) {
        if (tableStructureState is TableStructureUiState.Success) {
            if (!isNewRow && selectedRowData != null) {
                // Düzenleme modu - mevcut verileri yükle
                fieldValues.clear()
                fieldValues.putAll(selectedRowData!!)
            } else if (isNewRow) {
                // Yeni kayıt için boş değerler
                val structure = (tableStructureState as TableStructureUiState.Success).structure
                fieldValues.clear()
                structure.columns.forEach { column ->
                    // AUTO_INCREMENT ve AUTO TIMESTAMP kolonlarını atla
                    val hasAutoTimestamp = column.defaultValue?.let {
                        it.contains("CURRENT_TIMESTAMP", ignoreCase = true) ||
                        it.contains("NOW()", ignoreCase = true)
                    } ?: false
                    
                    if (!column.isAutoIncrement && !hasAutoTimestamp) {
                        fieldValues[column.name] = column.defaultValue ?: ""
                    }
                }
            }
        }
    }
    
    // Ekrandan çıkarken selectedRowData'yı temizle
    DisposableEffect(Unit) {
        onDispose {
            tableViewModel.clearSelectedRowData()
        }
    }
    
    // İşlem durumunu izle
    LaunchedEffect(rowOperationState) {
        when (rowOperationState) {
            is RowOperationUiState.Success -> {
                snackbarHostState.showSnackbar(
                    message = (rowOperationState as RowOperationUiState.Success).message,
                    duration = SnackbarDuration.Short
                )
                tableViewModel.resetRowOperationState()
                onSaved()
            }
            is RowOperationUiState.Error -> {
                snackbarHostState.showSnackbar(
                    message = (rowOperationState as RowOperationUiState.Error).message,
                    duration = SnackbarDuration.Long
                )
                tableViewModel.resetRowOperationState()
            }
            else -> {}
        }
    }
    
    val focusManager = LocalFocusManager.current

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets.ime,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isNewRow)
                            stringResource(R.string.add_row_title)
                        else
                            stringResource(R.string.edit_row_title)
                    )
                },
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
        }
    ) { paddingValues ->
        when (val state = tableStructureState) {
            is TableStructureUiState.Idle, is TableStructureUiState.Loading -> {
                LoadingIndicator()
            }
            
            is TableStructureUiState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { focusManager.clearFocus() },
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Her kolon için input field
                    state.structure.columns.forEach { column ->
                        // Auto increment kolonları yeni kayıtta gösterme
                        if (column.isAutoIncrement && isNewRow) {
                            return@forEach
                        }
                        
                        // CURRENT_TIMESTAMP veya NOW() default değeri varsa ve yeni kayıtsa gösterme
                        val hasAutoTimestamp = column.defaultValue?.let {
                            it.contains("CURRENT_TIMESTAMP", ignoreCase = true) ||
                            it.contains("NOW()", ignoreCase = true)
                        } ?: false
                        
                        if (hasAutoTimestamp && isNewRow) {
                            return@forEach
                        }
                        
                        val value = fieldValues[column.name] ?: ""
                        
                        // Düzenleme izinleri
                        val isEnabled = when {
                            // Yeni kayıt
                            isNewRow -> {
                                !column.isAutoIncrement && !hasAutoTimestamp
                            }
                            // Mevcut kayıt düzenleme
                            else -> {
                                !column.isPrimaryKey && !column.isAutoIncrement && !hasAutoTimestamp
                            }
                        }
                        
                        // Klavye seçenekleri: Metin alanlarında otomatik düzeltmeyi kapat
                        val typeUpper = column.type.uppercase()
                        // 'VARCHAR(255)' gibi varyasyonları da yakalamak için contains kullan
                        val isTextType =
                            typeUpper.contains("VARCHAR") ||
                            typeUpper.contains("CHAR") ||
                            typeUpper.contains("TEXT") ||
                            typeUpper.contains("JSON")

                        val isNumericType =
                            typeUpper.contains("INT") ||
                            typeUpper.contains("DECIMAL") ||
                            typeUpper.contains("FLOAT") ||
                            typeUpper.contains("DOUBLE")

                        val keyboardOptions = when {
                            isTextType -> KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                capitalization = KeyboardCapitalization.None,
                                autoCorrect = false
                            )
                            isNumericType -> KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                autoCorrect = false
                            )
                            else -> KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                capitalization = KeyboardCapitalization.None,
                                autoCorrect = false
                            )
                        }

                        OutlinedTextField(
                            value = value,
                            onValueChange = {
                                // Debug: IME’den gelen ham karakterleri gözlemlemek için
                                try {
                                    val codes = it.toCharArray().map { ch -> ch.code }
                                    Log.d("RowEditorInput", "col=${column.name} value='$it' codes=$codes")
                                } catch (_: Exception) {}
                                fieldValues[column.name] = it
                            },
                            label = { Text(column.name) },
                            supportingText = {
                                Text(
                                    buildString {
                                        append(column.type)
                                        if (column.length != null) append("(${column.length})")
                                        if (column.isPrimaryKey) append(" • PK")
                                        if (column.isAutoIncrement) append(" • AI")
                                        if (hasAutoTimestamp) append(" • AUTO")
                                        if (!column.nullable) append(" • NOT NULL")
                                        if (column.defaultValue != null && !hasAutoTimestamp) {
                                            append(" • Default: ${column.defaultValue}")
                                        }
                                    }
                                )
                            },
                            placeholder = {
                                if (column.nullable) {
                                    Text("NULL")
                                } else if (column.defaultValue != null && !hasAutoTimestamp) {
                                    Text(column.defaultValue!!)
                                }
                            },
                            // Türkçe yazımı için locale’u açıkça belirt
                            textStyle = LocalTextStyle.current.copy(
                                localeList = LocaleList(Locale("tr"))
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = isEnabled,
                            singleLine = column.type.uppercase() !in listOf("TEXT", "LONGTEXT", "MEDIUMTEXT"),
                            maxLines = if (column.type.uppercase() in listOf("TEXT", "LONGTEXT", "MEDIUMTEXT")) 5 else 1,
                            keyboardOptions = keyboardOptions
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Butonlar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .imePadding(),
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
                                val structure = state.structure
                                
                                // AUTO_INCREMENT ve CURRENT_TIMESTAMP kolonlarını hariç tut
                                val editableColumns = structure.columns.filter { column ->
                                    !column.isAutoIncrement && 
                                    !(column.defaultValue?.let {
                                        it.contains("CURRENT_TIMESTAMP", ignoreCase = true) ||
                                        it.contains("NOW()", ignoreCase = true)
                                    } ?: false)
                                }
                                
                                val requiredFields = editableColumns.filter { !it.nullable }
                                
                                val missingFields = requiredFields.filter {
                                    fieldValues[it.name].isNullOrBlank()
                                }
                                
                                if (missingFields.isNotEmpty()) {
                                    // Hata göster
                                    return@Button
                                }
                                
                                // Kaydet
                                if (isNewRow) {
                                    // Yeni kayıt ekle - sadece düzenlenebilir kolonları gönder
                                    val values = fieldValues.filterKeys { key ->
                                        editableColumns.any { it.name == key }
                                    }.filterValues { value ->
                                        value.isNotBlank() && value.uppercase() != "NULL"
                                    }
                                    
                                    tableViewModel.insertRow(database, table, values)
                                } else {
                                    // Mevcut kaydı güncelle - sadece düzenlenebilir kolonları gönder
                                    val primaryKey = structure.columns.find { it.isPrimaryKey }
                                    if (primaryKey != null) {
                                        val pkValue = selectedRowData?.get(primaryKey.name) ?: ""
                                        val updates = fieldValues.filterKeys { key ->
                                            editableColumns.any { it.name == key } && key != primaryKey.name
                                        }.filterValues { value ->
                                            value.isNotBlank() && value.uppercase() != "NULL"
                                        }
                                        
                                        tableViewModel.updateRow(
                                            database, table, primaryKey.name, pkValue, updates
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = rowOperationState !is RowOperationUiState.Processing
                        ) {
                            if (rowOperationState is RowOperationUiState.Processing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text(stringResource(R.string.save_button))
                            }
                        }
                    }
                }
            }
            
            is TableStructureUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
