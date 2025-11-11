package com.miladb.ui.screen.table

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.miladb.data.model.TableListUiState
import com.miladb.ui.component.ErrorMessage
import com.miladb.ui.component.LoadingIndicator
import com.miladb.ui.screen.database.DatabaseViewModel

/**
 * Seçilen veritabanındaki tabloları listeleyen ekran.
 * 
 * Özellikler:
 * - Tablo listesi (LazyColumn)
 * - Alfabetik sıralama
 * - Loading animasyonu
 * - Tablo seçimi
 * - Uzun basma ile tablo silme
 * - Yeni tablo oluşturma butonu
 * - SQL editörü butonu
 * - Geri navigasyonu
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TableListScreen(
    viewModel: DatabaseViewModel,
    tableViewModel: TableViewModel,
    database: String,
    onTableSelected: (String) -> Unit,
    onCreateTable: () -> Unit,
    onSqlEditor: () -> Unit,
    onEditTable: (String) -> Unit,
    onBackPressed: () -> Unit
) {
    val tableListState by viewModel.tableListState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var showDeleteDialog by remember { mutableStateOf(false) }
    var tableToDelete by remember { mutableStateOf<String?>(null) }
    var deleteConfirmText by remember { mutableStateOf("") }
    
    // İlk yüklemede tabloları getir
    LaunchedEffect(database) {
        viewModel.loadTables(database)
    }
    
    // Hata durumunu izle
    LaunchedEffect(tableListState) {
        if (tableListState is TableListUiState.Error) {
            snackbarHostState.showSnackbar(
                message = (tableListState as TableListUiState.Error).message,
                duration = SnackbarDuration.Long
            )
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(database) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back_button)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSqlEditor) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = stringResource(R.string.sql_editor_button)
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
                onClick = onCreateTable
                // Açık mavi (default primary color)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.new_table_button)
                )
            }
        }
    ) { paddingValues ->
        when (val state = tableListState) {
            is TableListUiState.Idle -> {
                LoadingIndicator()
            }
            
            is TableListUiState.Loading -> {
                LoadingIndicator()
            }
            
            is TableListUiState.Success -> {
                if (state.tables.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.no_tables),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.tables) { table ->
                            TableCard(
                                tableName = table,
                                onClick = { onTableSelected(table) },
                                onLongClick = {
                                    tableToDelete = table
                                    deleteConfirmText = ""
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                }
            }
            
            is TableListUiState.Error -> {
                ErrorMessage(
                    message = state.message,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
    
    // Tablo silme onay dialogu
    if (showDeleteDialog && tableToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                tableToDelete = null
                deleteConfirmText = ""
            },
            title = { Text(stringResource(R.string.delete_table_confirm_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.delete_table_confirm_message))
                    
                    OutlinedTextField(
                        value = deleteConfirmText,
                        onValueChange = { deleteConfirmText = it },
                        label = { Text("Tablo adı: $tableToDelete") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Düzenleme bağlantısı
                    TextButton(
                        onClick = {
                            val t = tableToDelete
                            if (t != null) {
                                showDeleteDialog = false
                                tableToDelete = null
                                deleteConfirmText = ""
                                onEditTable(t)
                            }
                        }
                    ) {
                        Text(stringResource(id = R.string.edit_table_button))
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (deleteConfirmText == tableToDelete) {
                            tableViewModel.dropTable(database, tableToDelete!!)
                            showDeleteDialog = false
                            tableToDelete = null
                            deleteConfirmText = ""
                            // Tabloları yeniden yükle
                            viewModel.loadTables(database)
                        }
                    },
                    enabled = deleteConfirmText == tableToDelete
                ) {
                    Text(stringResource(R.string.yes))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        tableToDelete = null
                        deleteConfirmText = ""
                    }
                ) {
                    Text(stringResource(R.string.no))
                }
            }
        )
    }
}

/**
 * Tablo kartı bileşeni.
 * 
 * @param tableName Tablo adı
 * @param onClick Tıklama callback
 * @param onLongClick Uzun basma callback
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TableCard(
    tableName: String,
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
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.TableChart,
                contentDescription = "Tablo",
                tint = MaterialTheme.colorScheme.primary, // Açık mavi
                modifier = Modifier.size(32.dp)
            )
            
            Text(
                text = tableName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
