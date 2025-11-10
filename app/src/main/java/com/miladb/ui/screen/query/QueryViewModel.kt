package com.miladb.ui.screen.query

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miladb.data.model.QueryUiState
import com.miladb.data.repository.DatabaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * SQL sorgu editörünün state ve iş mantığını yöneten ViewModel.
 * 
 * Özellikler:
 * - SQL sorgu çalıştırma
 * - Sorgu geçmişi yönetimi
 * - Asenkron işlemler (Coroutines)
 * - State management (StateFlow)
 */
class QueryViewModel(
    private val repository: DatabaseRepository
) : ViewModel() {
    
    // Query State
    private val _queryState = MutableStateFlow<QueryUiState>(QueryUiState.Idle)
    val queryState: StateFlow<QueryUiState> = _queryState.asStateFlow()
    
    // Query History
    private val _queryHistory = MutableStateFlow<List<String>>(emptyList())
    val queryHistory: StateFlow<List<String>> = _queryHistory.asStateFlow()
    
    /**
     * SQL sorgusunu çalıştırır.
     * 
     * @param query SQL sorgusu
     * @param database Veritabanı adı (opsiyonel)
     */
    fun executeQuery(query: String, database: String? = null) {
        viewModelScope.launch {
            _queryState.value = QueryUiState.Executing
            
            // Database belirtilmişse ve sorgu USE ile başlamıyorsa, USE database komutunu ekle
            val fullQuery = if (database != null && 
                !query.trim().uppercase().startsWith("USE") &&
                !query.contains(";")) {
                // Tek sorgu ise database prefix ekle
                "USE `$database`; $query"
            } else {
                // Çoklu sorgu veya zaten USE var ise olduğu gibi kullan
                query
            }
            
            val result = repository.executeQuery(fullQuery)
            
            _queryState.value = if (result.isSuccess) {
                // Geçmişe ekle
                addToHistory(query)
                QueryUiState.Success(result.getOrNull()!!)
            } else {
                QueryUiState.Error(
                    result.exceptionOrNull()?.message ?: "Sorgu çalıştırılamadı"
                )
            }
        }
    }
    
    /**
     * Sorgu geçmişine ekler.
     * 
     * @param query SQL sorgusu
     */
    fun addToHistory(query: String) {
        val currentHistory = _queryHistory.value.toMutableList()
        // Aynı sorgu varsa çıkar
        currentHistory.remove(query)
        // Başa ekle
        currentHistory.add(0, query)
        // Son 20 sorguyu tut
        if (currentHistory.size > 20) {
            currentHistory.removeAt(currentHistory.size - 1)
        }
        _queryHistory.value = currentHistory
    }
    
    /**
     * Sorgu durumunu sıfırlar.
     */
    fun resetQueryState() {
        _queryState.value = QueryUiState.Idle
    }
}
