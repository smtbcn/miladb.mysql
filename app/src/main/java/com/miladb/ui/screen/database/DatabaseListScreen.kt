package com.miladb.ui.screen.database

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.miladb.R
import com.miladb.data.model.DatabaseListUiState
import com.miladb.ui.component.ErrorMessage
import com.miladb.ui.component.LoadingIndicator

/**
 * Sunucudaki veritabanlarını listeleyen ekran.
 * 
 * Özellikler:
 * - Veritabanı listesi (LazyColumn)
 * - Loading animasyonu
 * - Veritabanı seçimi
 * - Hata durumu gösterimi
 * - Material 3 tasarım
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatabaseListScreen(
    viewModel: DatabaseViewModel,
    onDatabaseSelected: (String) -> Unit,
    onBackPressed: () -> Unit
) {
    val databaseListState by viewModel.databaseListState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // İlk yüklemede veritabanlarını getir
    LaunchedEffect(Unit) {
        viewModel.loadDatabases()
    }
    
    // Hata durumunu izle
    LaunchedEffect(databaseListState) {
        if (databaseListState is DatabaseListUiState.Error) {
            snackbarHostState.showSnackbar(
                message = (databaseListState as DatabaseListUiState.Error).message,
                duration = SnackbarDuration.Long
            )
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.database_list_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
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
        when (val state = databaseListState) {
            is DatabaseListUiState.Loading -> {
                LoadingIndicator()
            }
            
            is DatabaseListUiState.Success -> {
                if (state.databases.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.no_databases),
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
                        items(state.databases) { database ->
                            DatabaseCard(
                                databaseName = database,
                                onClick = { onDatabaseSelected(database) }
                            )
                        }
                    }
                }
            }
            
            is DatabaseListUiState.Error -> {
                ErrorMessage(
                    message = state.message,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

/**
 * Veritabanı kartı bileşeni.
 * 
 * @param databaseName Veritabanı adı
 * @param onClick Tıklama callback
 */
@Composable
private fun DatabaseCard(
    databaseName: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
                imageVector = Icons.Default.Storage,
                contentDescription = "Veritabanı",
                tint = MaterialTheme.colorScheme.primary, // Açık mavi
                modifier = Modifier.size(32.dp)
            )
            
            Text(
                text = databaseName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
