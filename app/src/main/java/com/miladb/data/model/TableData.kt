package com.miladb.data.model

/**
 * Tablo verileri ve metadata.
 * 
 * @property columns Kolon isimleri listesi
 * @property rows Satır verileri (her satır bir liste)
 * @property tableName Tablo adı
 * @property databaseName Veritabanı adı
 * @property primaryKeyColumn Primary key kolon adı (opsiyonel)
 */
data class TableData(
    val columns: List<String>,
    val rows: List<List<String>>,
    val tableName: String,
    val databaseName: String,
    val primaryKeyColumn: String? = null
)
