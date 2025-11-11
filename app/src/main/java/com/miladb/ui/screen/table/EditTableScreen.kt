package com.miladb.ui.screen.table

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import com.miladb.R
import com.miladb.data.model.ColumnDefinition
import com.miladb.data.model.TableStructureUiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTableScreen(
    tableViewModel: TableViewModel,
    database: String,
    table: String,
    onSaved: () -> Unit,
    onCancelled: () -> Unit
) {
    val tableStructureState by tableViewModel.tableStructureState.collectAsState()
    val tableOperationState by tableViewModel.tableOperationState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val newColumns = remember { mutableStateListOf<ColumnState>() }
    var focusTargetIndex by remember { mutableStateOf<Int?>(null) }

    // Yapıyı yükle
    LaunchedEffect(database, table) {
        tableViewModel.loadTableStructure(database, table)
    }

    // İşlem durumunu izle
    LaunchedEffect(tableOperationState) {
        when (tableOperationState) {
            is com.miladb.data.model.TableOperationUiState.Success -> {
                snackbarHostState.showSnackbar(
                    message = (tableOperationState as com.miladb.data.model.TableOperationUiState.Success).message,
                    duration = SnackbarDuration.Short
                )
                tableViewModel.resetTableOperationState()
                onSaved()
            }
            is com.miladb.data.model.TableOperationUiState.Error -> {
                snackbarHostState.showSnackbar(
                    message = (tableOperationState as com.miladb.data.model.TableOperationUiState.Error).message,
                    duration = SnackbarDuration.Long
                )
            }
            else -> Unit
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("${table} - Düzenle") },
                navigationIcon = {
                    IconButton(onClick = onCancelled) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    TextButton(onClick = {
                        newColumns.add(
                            ColumnState(
                                name = "",
                                type = "VARCHAR",
                                length = "255",
                                nullable = true,
                                isPrimaryKey = false,
                                isAutoIncrement = false
                            )
                        )
                        focusTargetIndex = newColumns.lastIndex
                    }) {
                        Text(stringResource(R.string.add_column_button))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF003258),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        contentWindowInsets = WindowInsets.ime,
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .imePadding(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCancelled,
                    modifier = Modifier.weight(1f)
                ) { Text(text = "İptal") }

                Button(
                    onClick = {
                        // Validasyon: en az bir yeni kolon eklenmiş olmalı ve adları boş olmamalı
                        if (newColumns.isEmpty()) {
                            // Kullanıcıya bilgi
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("En az bir yeni kolon ekleyin")
                            }
                            return@Button
                        }
                        if (newColumns.any { it.name.isBlank() }) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Tüm yeni kolonların adı olmalı")
                            }
                            return@Button
                        }

                        val defs = newColumns.map { it.toColumnDefinition() }
                        tableViewModel.addColumns(database, table, defs)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = tableOperationState !is com.miladb.data.model.TableOperationUiState.Processing
                ) {
                    if (tableOperationState is com.miladb.data.model.TableOperationUiState.Processing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Kaydediliyor…")
                    } else {
                        Text("Değişiklikleri Kaydet")
                    }
                }
            }
        }
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
                ) { focusManager.clearFocus() },
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "Mevcut Kolonlar", style = MaterialTheme.typography.titleMedium)

            when (val state = tableStructureState) {
                is TableStructureUiState.Loading -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                is TableStructureUiState.Error -> {
                    Text(text = state.message, color = MaterialTheme.colorScheme.error)
                }
                is TableStructureUiState.Success -> {
                    state.structure.columns.forEach { col ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(text = col.name, style = MaterialTheme.typography.titleSmall)
                                val typeStr = col.length?.let { "${col.type}(${it})" } ?: col.type
                                Text(text = "Tip: $typeStr")
                            }
                        }
                    }
                }
                else -> {}
            }

            Divider()

            Text(text = "Yeni Kolonlar", style = MaterialTheme.typography.titleMedium)

            newColumns.forEachIndexed { index, column ->
                EditColumnEditor(
                    column = column,
                    onRemove = { newColumns.removeAt(index) },
                    shouldFocus = (focusTargetIndex == index)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditColumnEditor(
    column: ColumnState,
    onRemove: () -> Unit,
    shouldFocus: Boolean
) {
    var typeExpanded by remember { mutableStateOf(false) }
    val columnTypes = listOf(
        "INT", "BIGINT", "SMALLINT", "TINYINT",
        "VARCHAR", "TEXT", "LONGTEXT", "CHAR",
        "DECIMAL", "FLOAT", "DOUBLE",
        "DATE", "DATETIME", "TIMESTAMP", "TIME",
        "BOOLEAN", "BLOB", "JSON"
    )
    val nameFocusRequester = remember { FocusRequester() }

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
                Text(text = "Kolon", style = MaterialTheme.typography.titleSmall)
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.remove_column_button)
                    )
                }
            }

            OutlinedTextField(
                value = column.name,
                onValueChange = { column.name = it },
                label = { Text(stringResource(R.string.column_name_label)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(nameFocusRequester),
                singleLine = true
            )

            LaunchedEffect(shouldFocus) {
                if (shouldFocus) {
                    nameFocusRequester.requestFocus()
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
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