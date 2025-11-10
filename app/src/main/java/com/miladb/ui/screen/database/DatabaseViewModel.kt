package com.miladb.ui.screen.database

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miladb.data.model.DatabaseListUiState
import com.miladb.data.model.TableListUiState
import com.miladb.data.repository.DatabaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Veritabanı ve tablo listelerinin state ve iş mantığını yöneten ViewModel.
 * 
 * Özellikler:
 * - Veritabanı listeleme
 * - Tablo listeleme
 * - Asenkron işlemler (Coroutines)
 * - State management (StateFlow)
 */
class DatabaseViewModel(
    private val repository: DatabaseRepository
) : ViewModel() {
    
    // Database List State
    private val _databaseListState = MutableStateFlow<DatabaseListUiState>(
        DatabaseListUiState.Loading
    )
    val databaseListState: StateFlow<DatabaseListUiState> = _databaseListState.asStateFlow()
    
    // Table List State
    private val _tableListState = MutableStateFlow<TableListUiState>(TableListUiState.Idle)
    val tableListState: StateFlow<TableListUiState> = _tableListState.asStateFlow()
    
    /**
     * Veritabanlarını yükler.
     * Otomatik olarak çağrılır (init bloğunda).
     */
    fun loadDatabases() {
        viewModelScope.launch {
            _databaseListState.value = DatabaseListUiState.Loading
            
            val result = repository.getDatabases()
            
            _databaseListState.value = if (result.isSuccess) {
                DatabaseListUiState.Success(result.getOrNull() ?: emptyList())
            } else {
                DatabaseListUiState.Error(
                    result.exceptionOrNull()?.message ?: "Veritabanları yüklenemedi"
                )
            }
        }
    }
    
    /**
     * Belirtilen veritabanının tablolarını yükler.
     * 
     * @param database Veritabanı adı
     */
    fun loadTables(database: String) {
        viewModelScope.launch {
            _tableListState.value = TableListUiState.Loading
            
            val result = repository.getTables(database)
            
            _tableListState.value = if (result.isSuccess) {
                TableListUiState.Success(result.getOrNull() ?: emptyList())
            } else {
                TableListUiState.Error(
                    result.exceptionOrNull()?.message ?: "Tablolar yüklenemedi"
                )
            }
        }
    }
    
    /**
     * Tablo listesi durumunu sıfırlar.
     */
    fun resetTableListState() {
        _tableListState.value = TableListUiState.Idle
    }
}
