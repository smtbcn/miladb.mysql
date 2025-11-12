package com.miladb.ui.screen.table

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.miladb.R
import com.miladb.data.model.ExportUiState
import com.miladb.data.model.RowOperationUiState
import com.miladb.data.model.TableDataUiState
import com.miladb.ui.component.DataTable
import com.miladb.ui.component.ErrorMessage
import com.miladb.ui.component.LoadingIndicator
import com.miladb.ui.screen.export.ExportViewModel

/**
 * Seçilen tablonun verilerini gösteren ekran.
 * 
 * Özellikler:
 * - Dinamik kolon başlıkları
 * - Kaydırılabilir tablo
 * - İlk 200 satır gösterimi
 * - Satıra tıklama ile düzenleme
 * - Satırı uzun basma ile silme
 * - Yeni kayıt ekleme butonu
 * - Export butonları (Excel, CSV)
 * - Loading animasyonu
 * - Geri navigasyonu
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableDataScreen(
    tableViewModel: TableViewModel,
    exportViewModel: ExportViewModel,
    database: String,
    table: String,
    onEditRow: (columns: List<String>, rowData: List<String>) -> Unit,
    onAddRow: () -> Unit,
    onBackPressed: () -> Unit
) {
    val tableDataState by tableViewModel.tableDataState.collectAsState()
    val rowOperationState by tableViewModel.rowOperationState.collectAsState()
    val exportState by exportViewModel.exportState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var showDeleteDialog by remember { mutableStateOf(false) }
    var rowToDelete by remember { mutableStateOf<Pair<Int, List<String>>?>(null) }
    var showExportMenu by remember { mutableStateOf(false) }
    
    // İlk yüklemede tablo verilerini getir
    LaunchedEffect(database, table) {
        tableViewModel.loadTableData(database, table)
    }
    
    // Satır işlemi durumunu izle
    LaunchedEffect(rowOperationState) {
        when (rowOperationState) {
            is RowOperationUiState.Success -> {
                snackbarHostState.showSnackbar(
                    message = (rowOperationState as RowOperationUiState.Success).message,
                    duration = SnackbarDuration.Short
                )
                tableViewModel.resetRowOperationState()
                // Verileri yenile
                tableViewModel.loadTableData(database, table)
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
    
    // Export durumunu izle
    LaunchedEffect(exportState) {
        when (exportState) {
            is ExportUiState.Success -> {
                val state = exportState as ExportUiState.Success
                snackbarHostState.showSnackbar(
                    message = "Dosya kaydedildi: ${state.fileName}",
                    duration = SnackbarDuration.Short
                )
                exportViewModel.resetExportState()
            }
            is ExportUiState.Error -> {
                snackbarHostState.showSnackbar(
                    message = (exportState as ExportUiState.Error).message,
                    duration = SnackbarDuration.Long
                )
                exportViewModel.resetExportState()
            }
            else -> {}
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(table) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back_button)
                        )
                    }
                },
                actions = {
                    // Export menu
                    IconButton(onClick = { showExportMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Dışa Aktar"
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showExportMenu,
                        onDismissRequest = { showExportMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("SQL Olarak Dışa Aktar") },
                            onClick = {
                                showExportMenu = false
                                val state = tableDataState
                                if (state is TableDataUiState.Success) {
                                    exportViewModel.exportToSql(state.tableData)
                                }
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Code, contentDescription = null)
                            }
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddRow
                // Açık mavi (default primary color)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.new_row_button)
                )
            }
        }
    ) { paddingValues ->
        when (val state = tableDataState) {
            is TableDataUiState.Idle -> {
                LoadingIndicator()
            }
            
            is TableDataUiState.Loading -> {
                LoadingIndicator()
            }
            
            is TableDataUiState.Success -> {
                if (state.tableData.rows.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.no_data),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    DataTable(
                        tableData = state.tableData,
                        onRowClick = { rowIndex, rowData ->
                            onEditRow(state.tableData.columns, rowData)
                        },
                        onRowLongPress = { rowIndex, rowData ->
                            rowToDelete = Pair(rowIndex, rowData)
                            showDeleteDialog = true
                        },
                        onEndReached = {
                            if (tableViewModel.canLoadMore()) {
                                tableViewModel.loadMoreTableData()
                            }
                        },
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
            
            is TableDataUiState.Error -> {
                ErrorMessage(
                    message = state.message,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
        
        // Loading overlay for export
        if (exportState is ExportUiState.Exporting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
    
    // Satır silme onay dialogu
    if (showDeleteDialog && rowToDelete != null) {
        val state = tableDataState
        if (state is TableDataUiState.Success) {
            val primaryKey = state.tableData.primaryKeyColumn
            if (primaryKey != null) {
                val pkIndex = state.tableData.columns.indexOf(primaryKey)
                val pkValue = rowToDelete!!.second[pkIndex]
                
                AlertDialog(
                    onDismissRequest = {
                        showDeleteDialog = false
                        rowToDelete = null
                    },
                    title = { Text(stringResource(R.string.delete_row_confirm_title)) },
                    text = { Text(stringResource(R.string.delete_row_confirm_message)) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                tableViewModel.deleteRow(database, table, primaryKey, pkValue)
                                showDeleteDialog = false
                                rowToDelete = null
                            }
                        ) {
                            Text(stringResource(R.string.yes))
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showDeleteDialog = false
                                rowToDelete = null
                            }
                        ) {
                            Text(stringResource(R.string.no))
                        }
                    }
                )
            } else {
                // Primary key yok, silme yapılamaz
                LaunchedEffect(Unit) {
                    snackbarHostState.showSnackbar(
                        message = "Bu tabloda primary key yok, silme işlemi yapılamaz",
                        duration = SnackbarDuration.Short
                    )
                    showDeleteDialog = false
                    rowToDelete = null
                }
            }
        }
    }
}
