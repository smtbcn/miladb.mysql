package com.miladb.data.model

/**
 * SQL sorgu sonucu.
 * SELECT sorguları için veri, diğer sorgular için etkilenen satır sayısı döner.
 */
sealed class QueryResult {
    /**
     * SELECT sorgusu sonucu.
     * 
     * @property tableData Sorgu sonucu tablo verileri
     */
    data class SelectResult(val tableData: TableData) : QueryResult()
    
    /**
     * INSERT/UPDATE/DELETE sorgusu sonucu.
     * 
     * @property affectedRows Etkilenen satır sayısı
     */
    data class ModifyResult(val affectedRows: Int) : QueryResult()
}
