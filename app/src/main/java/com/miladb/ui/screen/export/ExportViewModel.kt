package com.miladb.ui.screen.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miladb.data.model.ExportUiState
import com.miladb.data.model.TableData
import com.miladb.data.repository.ExportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Dışa aktarma işlemlerinin state ve iş mantığını yöneten ViewModel.
 * 
 * Özellikler:
 * - Excel dışa aktarma
 * - CSV dışa aktarma
 * - Asenkron işlemler (Coroutines)
 * - State management (StateFlow)
 */
class ExportViewModel(
    private val repository: ExportRepository
) : ViewModel() {
    
    // Export State
    private val _exportState = MutableStateFlow<ExportUiState>(ExportUiState.Idle)
    val exportState: StateFlow<ExportUiState> = _exportState.asStateFlow()
    
    /**
     * Excel formatında dışa aktarır.
     * 
     * @param tableData Tablo verileri
     * @param fileName Dosya adı (opsiyonel)
     */
    fun exportToExcel(tableData: TableData, fileName: String? = null) {
        viewModelScope.launch {
            _exportState.value = ExportUiState.Exporting
            
            val result = repository.exportToExcel(tableData, fileName)
            
            _exportState.value = if (result.isSuccess) {
                val uri = result.getOrNull()!!
                val finalFileName = fileName ?: "${tableData.tableName}.xlsx"
                ExportUiState.Success(uri, finalFileName)
            } else {
                ExportUiState.Error(
                    result.exceptionOrNull()?.message ?: "Excel dışa aktarılamadı"
                )
            }
        }
    }
    
    /**
     * CSV formatında dışa aktarır.
     * 
     * @param tableData Tablo verileri
     * @param fileName Dosya adı (opsiyonel)
     */
    fun exportToCsv(tableData: TableData, fileName: String? = null) {
        viewModelScope.launch {
            _exportState.value = ExportUiState.Exporting
            
            val result = repository.exportToCsv(tableData, fileName)
            
            _exportState.value = if (result.isSuccess) {
                val uri = result.getOrNull()!!
                val finalFileName = fileName ?: "${tableData.tableName}.csv"
                ExportUiState.Success(uri, finalFileName)
            } else {
                ExportUiState.Error(
                    result.exceptionOrNull()?.message ?: "CSV dışa aktarılamadı"
                )
            }
        }
    }
    
    /**
     * SQL formatında dışa aktarır.
     * 
     * @param tableData Tablo verileri
     * @param fileName Dosya adı (opsiyonel)
     */
    fun exportToSql(tableData: TableData, fileName: String? = null) {
        viewModelScope.launch {
            _exportState.value = ExportUiState.Exporting
            
            val result = repository.exportToSql(tableData, fileName)
            
            _exportState.value = if (result.isSuccess) {
                val uri = result.getOrNull()!!
                val finalFileName = fileName ?: "${tableData.tableName}.sql"
                ExportUiState.Success(uri, finalFileName)
            } else {
                ExportUiState.Error(
                    result.exceptionOrNull()?.message ?: "SQL dışa aktarılamadı"
                )
            }
        }
    }
    
    /**
     * Dışa aktarma durumunu sıfırlar.
     */
    fun resetExportState() {
        _exportState.value = ExportUiState.Idle
    }
}
