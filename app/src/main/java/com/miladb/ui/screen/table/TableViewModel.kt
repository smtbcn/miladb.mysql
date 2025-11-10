package com.miladb.ui.screen.table

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miladb.data.model.*
import com.miladb.data.repository.DatabaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Tablo verilerinin ve işlemlerinin state ve iş mantığını yöneten ViewModel.
 * 
 * Özellikler:
 * - Tablo verilerini yükleme
 * - Tablo yapısını yükleme
 * - CRUD işlemleri (INSERT, UPDATE, DELETE)
 * - Tablo oluşturma ve silme
 * - Asenkron işlemler (Coroutines)
 * - State management (StateFlow)
 */
class TableViewModel(
    private val repository: DatabaseRepository
) : ViewModel() {
    
    // Table Data State
    private val _tableDataState = MutableStateFlow<TableDataUiState>(TableDataUiState.Idle)
    val tableDataState: StateFlow<TableDataUiState> = _tableDataState.asStateFlow()
    
    // Table Structure State
    private val _tableStructureState = MutableStateFlow<TableStructureUiState>(
        TableStructureUiState.Idle
    )
    val tableStructureState: StateFlow<TableStructureUiState> = _tableStructureState.asStateFlow()
    
    // Row Operation State
    private val _rowOperationState = MutableStateFlow<RowOperationUiState>(
        RowOperationUiState.Idle
    )
    val rowOperationState: StateFlow<RowOperationUiState> = _rowOperationState.asStateFlow()
    
    // Table Operation State
    private val _tableOperationState = MutableStateFlow<TableOperationUiState>(
        TableOperationUiState.Idle
    )
    val tableOperationState: StateFlow<TableOperationUiState> = _tableOperationState.asStateFlow()
    
    // Selected Row Data (for editing)
    private val _selectedRowData = MutableStateFlow<Map<String, String>?>(null)
    val selectedRowData: StateFlow<Map<String, String>?> = _selectedRowData.asStateFlow()
    
    /**
     * Tablo verilerini yükler.
     * 
     * @param database Veritabanı adı
     * @param table Tablo adı
     */
    fun loadTableData(database: String, table: String) {
        viewModelScope.launch {
            _tableDataState.value = TableDataUiState.Loading
            
            val result = repository.getTableData(database, table)
            
            _tableDataState.value = if (result.isSuccess) {
                TableDataUiState.Success(result.getOrNull()!!)
            } else {
                TableDataUiState.Error(
                    result.exceptionOrNull()?.message ?: "Tablo verileri yüklenemedi"
                )
            }
        }
    }
    
    /**
     * Tablo yapısını yükler.
     * 
     * @param database Veritabanı adı
     * @param table Tablo adı
     */
    fun loadTableStructure(database: String, table: String) {
        viewModelScope.launch {
            _tableStructureState.value = TableStructureUiState.Loading
            
            val result = repository.getTableStructure(database, table)
            
            _tableStructureState.value = if (result.isSuccess) {
                TableStructureUiState.Success(result.getOrNull()!!)
            } else {
                TableStructureUiState.Error(
                    result.exceptionOrNull()?.message ?: "Tablo yapısı yüklenemedi"
                )
            }
        }
    }
    
    /**
     * Satırı günceller.
     * 
     * @param database Veritabanı adı
     * @param table Tablo adı
     * @param primaryKeyColumn Primary key kolon adı
     * @param primaryKeyValue Primary key değeri
     * @param updates Güncellenecek değerler
     */
    fun updateRow(
        database: String,
        table: String,
        primaryKeyColumn: String,
        primaryKeyValue: String,
        updates: Map<String, String>
    ) {
        viewModelScope.launch {
            _rowOperationState.value = RowOperationUiState.Processing
            
            val result = repository.updateRow(
                database, table, primaryKeyColumn, primaryKeyValue, updates
            )
            
            _rowOperationState.value = if (result.isSuccess) {
                RowOperationUiState.Success("Kayıt güncellendi")
            } else {
                RowOperationUiState.Error(
                    result.exceptionOrNull()?.message ?: "Kayıt güncellenemedi"
                )
            }
        }
    }
    
    /**
     * Yeni satır ekler.
     * 
     * @param database Veritabanı adı
     * @param table Tablo adı
     * @param values Eklenecek değerler
     */
    fun insertRow(
        database: String,
        table: String,
        values: Map<String, String>
    ) {
        viewModelScope.launch {
            _rowOperationState.value = RowOperationUiState.Processing
            
            val result = repository.insertRow(database, table, values)
            
            _rowOperationState.value = if (result.isSuccess) {
                RowOperationUiState.Success("Kayıt eklendi")
            } else {
                RowOperationUiState.Error(
                    result.exceptionOrNull()?.message ?: "Kayıt eklenemedi"
                )
            }
        }
    }
    
    /**
     * Satırı siler.
     * 
     * @param database Veritabanı adı
     * @param table Tablo adı
     * @param primaryKeyColumn Primary key kolon adı
     * @param primaryKeyValue Primary key değeri
     */
    fun deleteRow(
        database: String,
        table: String,
        primaryKeyColumn: String,
        primaryKeyValue: String
    ) {
        viewModelScope.launch {
            _rowOperationState.value = RowOperationUiState.Processing
            
            val result = repository.deleteRow(database, table, primaryKeyColumn, primaryKeyValue)
            
            _rowOperationState.value = if (result.isSuccess) {
                RowOperationUiState.Success("Kayıt silindi")
            } else {
                RowOperationUiState.Error(
                    result.exceptionOrNull()?.message ?: "Kayıt silinemedi"
                )
            }
        }
    }
    
    /**
     * Yeni tablo oluşturur.
     * 
     * @param database Veritabanı adı
     * @param tableDefinition Tablo tanımı
     */
    fun createTable(database: String, tableDefinition: TableDefinition) {
        viewModelScope.launch {
            _tableOperationState.value = TableOperationUiState.Processing
            
            val result = repository.createTable(database, tableDefinition)
            
            _tableOperationState.value = if (result.isSuccess) {
                TableOperationUiState.Success("Tablo oluşturuldu")
            } else {
                TableOperationUiState.Error(
                    result.exceptionOrNull()?.message ?: "Tablo oluşturulamadı"
                )
            }
        }
    }
    
    /**
     * Tabloyu siler.
     * 
     * @param database Veritabanı adı
     * @param table Tablo adı
     */
    fun dropTable(database: String, table: String) {
        viewModelScope.launch {
            _tableOperationState.value = TableOperationUiState.Processing
            
            val result = repository.dropTable(database, table)
            
            _tableOperationState.value = if (result.isSuccess) {
                TableOperationUiState.Success("Tablo silindi")
            } else {
                TableOperationUiState.Error(
                    result.exceptionOrNull()?.message ?: "Tablo silinemedi"
                )
            }
        }
    }
    
    /**
     * Satır işlemi durumunu sıfırlar.
     */
    fun resetRowOperationState() {
        _rowOperationState.value = RowOperationUiState.Idle
    }
    
    /**
     * Tablo işlemi durumunu sıfırlar.
     */
    fun resetTableOperationState() {
        _tableOperationState.value = TableOperationUiState.Idle
    }
    
    /**
     * Düzenlenecek satır verisini ayarlar.
     */
    fun setSelectedRowData(columns: List<String>, rowData: List<String>) {
        val dataMap = columns.zip(rowData).toMap()
        _selectedRowData.value = dataMap
    }
    
    /**
     * Seçili satır verisini temizler.
     */
    fun clearSelectedRowData() {
        _selectedRowData.value = null
    }
}
