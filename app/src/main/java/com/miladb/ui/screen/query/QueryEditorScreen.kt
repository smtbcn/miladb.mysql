package com.miladb.ui.screen.query

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.miladb.R
import com.miladb.data.model.QueryResult
import com.miladb.data.model.QueryUiState
import com.miladb.ui.component.DataTable
import com.miladb.ui.component.LoadingIndicator

/**
 * SQL sorgu editörü ekranı.
 * 
 * Özellikler:
 * - Çok satırlı SQL editörü
 * - "Çalıştır" butonu
 * - Sorgu sonucunu göster (SELECT için tablo, diğerleri için etkilenen satır sayısı)
 * - Sorgu geçmişi listesi
 * - Geçmişten sorgu seçme
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueryEditorScreen(
    queryViewModel: QueryViewModel,
    database: String,
    onBackPressed: () -> Unit
) {
    val queryState by queryViewModel.queryState.collectAsState()
    val queryHistory by queryViewModel.queryHistory.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var queryText by remember { mutableStateOf("") }
    var showHistoryMenu by remember { mutableStateOf(false) }
    
    // Hata durumunu izle
    LaunchedEffect(queryState) {
        if (queryState is QueryUiState.Error) {
            snackbarHostState.showSnackbar(
                message = (queryState as QueryUiState.Error).message,
                duration = SnackbarDuration.Long
            )
            queryViewModel.resetQueryState()
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets.ime, // Klavye açıldığında içeriği yukarı kaydır
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.query_editor_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back_button)
                        )
                    }
                },
                actions = {
                    // Geçmiş sorgular dropdown menu
                    Box {
                        IconButton(onClick = { showHistoryMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Geçmiş Sorgular"
                            )
                        }
                        
                        DropdownMenu(
                            expanded = showHistoryMenu,
                            onDismissRequest = { showHistoryMenu = false }
                        ) {
                            if (queryHistory.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Geçmiş sorgu yok") },
                                    onClick = { },
                                    enabled = false
                                )
                            } else {
                                // Son 10 sorguyu göster
                                queryHistory.take(10).forEach { query ->
                                    DropdownMenuItem(
                                        text = { 
                                            Text(
                                                text = query,
                                                maxLines = 2,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        },
                                        onClick = {
                                            queryText = query
                                            showHistoryMenu = false
                                        }
                                    )
                                    if (query != queryHistory.take(10).last()) {
                                        Divider()
                                    }
                                }
                            }
                        }
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
        val focusManager = LocalFocusManager.current
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    focusManager.clearFocus() // Klavyeyi kapat
                }
        ) {
            // SQL Editörü
            OutlinedTextField(
                value = queryText,
                onValueChange = { queryText = it },
                label = { Text("SQL Sorgusu") },
                placeholder = { Text(stringResource(R.string.query_hint)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(16.dp),
                maxLines = 10
            )
            
            // Çalıştır butonu
            Button(
                onClick = {
                    if (queryText.isNotBlank()) {
                        queryViewModel.executeQuery(queryText, database)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                enabled = queryText.isNotBlank() && queryState !is QueryUiState.Executing
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.execute_button))
            }
            
            Divider(modifier = Modifier.padding(vertical = 16.dp))
            
            // Sonuç alanı
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when (val state = queryState) {
                    is QueryUiState.Idle -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Sorgu çalıştırın",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    is QueryUiState.Executing -> {
                        LoadingIndicator()
                    }
                    
                    is QueryUiState.Success -> {
                        when (val result = state.result) {
                            is QueryResult.SelectResult -> {
                                // SELECT sonucu - tablo göster
                                DataTable(tableData = result.tableData)
                            }
                            is QueryResult.ModifyResult -> {
                                // INSERT/UPDATE/DELETE sonucu
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary, // Açık mavi
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Text(
                                            text = stringResource(
                                                R.string.affected_rows,
                                                result.affectedRows
                                            ),
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    is QueryUiState.Error -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
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
    }
}


