package com.miladb.data.model

import android.net.Uri

/**
 * Bağlantı ekranı UI durumu.
 */
sealed class ConnectionUiState {
    object Idle : ConnectionUiState()
    object Loading : ConnectionUiState()
    object Success : ConnectionUiState()
    data class SuccessWithDatabase(val database: String) : ConnectionUiState()
    data class Error(val message: String) : ConnectionUiState()
}

/**
 * Veritabanı listesi UI durumu.
 */
sealed class DatabaseListUiState {
    object Loading : DatabaseListUiState()
    data class Success(val databases: List<String>) : DatabaseListUiState()
    data class Error(val message: String) : DatabaseListUiState()
}

/**
 * Tablo listesi UI durumu.
 */
sealed class TableListUiState {
    object Idle : TableListUiState()
    object Loading : TableListUiState()
    data class Success(val tables: List<String>) : TableListUiState()
    data class Error(val message: String) : TableListUiState()
}

/**
 * Tablo verileri UI durumu.
 */
sealed class TableDataUiState {
    object Idle : TableDataUiState()
    object Loading : TableDataUiState()
    data class Success(val tableData: TableData) : TableDataUiState()
    data class Error(val message: String) : TableDataUiState()
}

/**
 * Tablo yapısı UI durumu.
 */
sealed class TableStructureUiState {
    object Idle : TableStructureUiState()
    object Loading : TableStructureUiState()
    data class Success(val structure: TableStructure) : TableStructureUiState()
    data class Error(val message: String) : TableStructureUiState()
}

/**
 * Satır işlemleri UI durumu (INSERT, UPDATE, DELETE).
 */
sealed class RowOperationUiState {
    object Idle : RowOperationUiState()
    object Processing : RowOperationUiState()
    data class Success(val message: String) : RowOperationUiState()
    data class Error(val message: String) : RowOperationUiState()
}

/**
 * Tablo işlemleri UI durumu (CREATE TABLE, DROP TABLE).
 */
sealed class TableOperationUiState {
    object Idle : TableOperationUiState()
    object Processing : TableOperationUiState()
    data class Success(val message: String) : TableOperationUiState()
    data class Error(val message: String) : TableOperationUiState()
}

/**
 * SQL sorgu UI durumu.
 */
sealed class QueryUiState {
    object Idle : QueryUiState()
    object Executing : QueryUiState()
    data class Success(val result: QueryResult) : QueryUiState()
    data class Error(val message: String) : QueryUiState()
}

/**
 * Dışa aktarma UI durumu.
 */
sealed class ExportUiState {
    object Idle : ExportUiState()
    object Exporting : ExportUiState()
    data class Success(val fileUri: Uri, val fileName: String) : ExportUiState()
    data class Error(val message: String) : ExportUiState()
}
