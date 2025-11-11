package com.miladb.data.model

/**
 * Tablo tanımı (CREATE TABLE için).
 * 
 * @property tableName Oluşturulacak tablo adı
 * @property columns Kolon tanımları listesi
 */
data class TableDefinition(
    val tableName: String,
    val columns: List<ColumnDefinition>,
    val tableCollation: String? = null
)

/**
 * Kolon tanımı.
 * 
 * @property name Kolon adı
 * @property type Kolon tipi (VARCHAR, INT, DATE vb.)
 * @property length Kolon uzunluğu (opsiyonel)
 * @property nullable NULL değer alabilir mi
 * @property isPrimaryKey Primary key mi
 * @property isAutoIncrement Auto increment mi
 * @property defaultValue Varsayılan değer (opsiyonel)
 */
data class ColumnDefinition(
    val name: String,
    val type: String,
    val length: Int? = null,
    val nullable: Boolean,
    val isPrimaryKey: Boolean,
    val isAutoIncrement: Boolean,
    val defaultValue: String? = null
)
