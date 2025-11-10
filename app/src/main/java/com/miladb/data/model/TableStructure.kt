package com.miladb.data.model

/**
 * Tablo yapısı bilgileri.
 * 
 * @property columns Kolon bilgileri listesi
 */
data class TableStructure(
    val columns: List<ColumnInfo>
)

/**
 * Kolon bilgileri.
 * 
 * @property name Kolon adı
 * @property type Kolon tipi (VARCHAR, INT, DATE vb.)
 * @property length Kolon uzunluğu (opsiyonel)
 * @property nullable NULL değer alabilir mi
 * @property isPrimaryKey Primary key mi
 * @property isAutoIncrement Auto increment mi
 * @property defaultValue Varsayılan değer (opsiyonel)
 */
data class ColumnInfo(
    val name: String,
    val type: String,
    val length: Int? = null,
    val nullable: Boolean,
    val isPrimaryKey: Boolean,
    val isAutoIncrement: Boolean,
    val defaultValue: String? = null
)
