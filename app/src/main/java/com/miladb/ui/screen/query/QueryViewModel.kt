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

    // Pagination State for SELECT results
    private var lastQuery: String? = null
    private var lastDatabase: String? = null
    private var pageLimit: Int = 200
    private var currentOffset: Int = 0
    private var isLoadingMore: Boolean = false
    private var hasMore: Boolean = true
    
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
            
            val isSelect = query.trim().uppercase().startsWith("SELECT")
            lastQuery = query
            lastDatabase = database
            currentOffset = 0
            hasMore = true

            // SELECT ise ilk sayfayı LIMIT/OFFSET ile çalıştır
            val finalQuery = if (isSelect) addLimitOffsetToSelect(query, pageLimit, currentOffset) else fullQuery
            val result = repository.executeQuery(
                if (database != null && !finalQuery.trim().uppercase().startsWith("USE"))
                    "USE `$database`; $finalQuery"
                else finalQuery
            )
            
            _queryState.value = if (result.isSuccess) {
                // Geçmişe ekle
                addToHistory(query)
                val res = result.getOrNull()!!
                if (res is com.miladb.data.model.QueryResult.SelectResult) {
                    currentOffset = res.tableData.rows.size
                    hasMore = res.tableData.rows.size >= pageLimit
                }
                QueryUiState.Success(res)
            } else {
                QueryUiState.Error(
                    result.exceptionOrNull()?.message ?: "Sorgu çalıştırılamadı"
                )
            }
        }
    }

    /**
     * SELECT sorgusu için bir sonraki sayfayı yükler ve sonuçları birleştirir.
     */
    fun loadMoreSelect() {
        if (isLoadingMore || !hasMore) return
        val query = lastQuery ?: return
        val database = lastDatabase

        viewModelScope.launch {
            isLoadingMore = true
            val nextQuery = addLimitOffsetToSelect(query, pageLimit, currentOffset)
            val final = if (database != null && !nextQuery.trim().uppercase().startsWith("USE"))
                "USE `$database`; $nextQuery" else nextQuery
            val result = repository.executeQuery(final)
            if (result.isSuccess) {
                val res = result.getOrNull()!!
                val currentState = _queryState.value
                if (currentState is QueryUiState.Success && res is com.miladb.data.model.QueryResult.SelectResult) {
                    val merged = currentState.result.let { existing ->
                        if (existing is com.miladb.data.model.QueryResult.SelectResult) {
                            existing.copy(
                                tableData = existing.tableData.copy(
                                    rows = existing.tableData.rows + res.tableData.rows
                                )
                            )
                        } else {
                            res
                        }
                    }
                    _queryState.value = QueryUiState.Success(merged)
                    currentOffset += res.tableData.rows.size
                    hasMore = res.tableData.rows.size >= pageLimit
                }
            }
            isLoadingMore = false
        }
    }

    fun canLoadMore(): Boolean = hasMore && !isLoadingMore

    // Basit yardımcı: SELECT sonuna LIMIT/OFFSET ekle veya yoksa ekle
    private fun addLimitOffsetToSelect(query: String, limit: Int, offset: Int): String {
        val trimmed = query.trim().removeSuffix(";")
        val hasLimit = Regex("(?i)\\blimit\\b").containsMatchIn(trimmed)
        return if (!hasLimit) {
            "$trimmed LIMIT $limit OFFSET $offset"
        } else {
            // Mevcut LIMIT varsa, sorguyu bir derived table ile sarmala ve yeni LIMIT uygula
            "SELECT * FROM ( $trimmed ) AS _q LIMIT $limit OFFSET $offset"
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
