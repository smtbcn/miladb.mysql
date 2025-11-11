package com.miladb.data.repository

import com.miladb.data.model.*
import com.miladb.data.source.connection.JdbcConnectionManager
import com.miladb.util.toMilaDbError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.ResultSet

/**
 * Veritabanı işlemlerini yöneten repository.
 * JDBC bağlantısı üzerinden veritabanı operasyonlarını gerçekleştirir.
 * 
 * Tüm işlemler:
 * - Ana thread dışında çalışır (Dispatchers.IO)
 * - Prepared statement kullanır (SQL injection koruması)
 * - Hata yönetimi ile Result döndürür
 */
class DatabaseRepository(
    private val connectionManager: JdbcConnectionManager
) {
    
    /**
     * Sunucudaki tüm veritabanlarını listeler.
     * Eğer belirli bir veritabanı ile bağlanıldıysa, sadece o veritabanını döndürür.
     * 
     * @return Result<List<String>> Veritabanı isimleri listesi
     */
    suspend fun getDatabases(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val connection = connectionManager.getConnection()
                ?: return@withContext Result.failure(Exception("Bağlantı yok"))
            
            // Eğer belirli bir veritabanı ile bağlanıldıysa, sadece onu döndür
            val connectedDatabase = connectionManager.getConnectedDatabase()
            if (!connectedDatabase.isNullOrBlank()) {
                return@withContext Result.success(listOf(connectedDatabase))
            }
            
            // Veritabanı adı olmadan bağlanıldıysa, tüm veritabanlarını listele
            val databases = mutableListOf<String>()
            
            connection.createStatement().use { statement ->
                statement.executeQuery("SHOW DATABASES").use { resultSet ->
                    while (resultSet.next()) {
                        databases.add(resultSet.getString(1))
                    }
                }
            }
            
            Result.success(databases)
            
        } catch (e: Exception) {
            val error = e.toMilaDbError()
            Result.failure(Exception(error.toUserMessage(), e))
        }
    }
    
    /**
     * Belirtilen veritabanındaki tabloları listeler.
     * 
     * @param database Veritabanı adı
     * @return Result<List<String>> Tablo isimleri listesi
     */
    suspend fun getTables(database: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val connection = connectionManager.getConnection()
                ?: return@withContext Result.failure(Exception("Bağlantı yok"))
            
            val tables = mutableListOf<String>()
            
            connection.createStatement().use { statement ->
                statement.executeQuery("SHOW TABLES FROM `$database`").use { resultSet ->
                    while (resultSet.next()) {
                        tables.add(resultSet.getString(1))
                    }
                }
            }
            
            Result.success(tables.sorted()) // Alfabetik sırala
            
        } catch (e: Exception) {
            val error = e.toMilaDbError()
            Result.failure(Exception(error.toUserMessage(), e))
        }
    }
    
    /**
     * Belirtilen tablonun verilerini getirir (ilk 200 satır).
     * 
     * @param database Veritabanı adı
     * @param table Tablo adı
     * @return Result<TableData> Tablo verileri ve kolon bilgileri
     */
    suspend fun getTableData(database: String, table: String): Result<TableData> = 
        withContext(Dispatchers.IO) {
        try {
            val connection = connectionManager.getConnection()
                ?: return@withContext Result.failure(Exception("Bağlantı yok"))
            
            connection.createStatement().use { statement ->
                statement.executeQuery(
                    "SELECT * FROM `$database`.`$table` LIMIT 200"
                ).use { resultSet ->
                    val tableData = resultSetToTableData(resultSet, table, database)
                    Result.success(tableData)
                }
            }
            
        } catch (e: Exception) {
            val error = e.toMilaDbError()
            Result.failure(Exception(error.toUserMessage(), e))
        }
    }
    
    /**
     * Tablonun yapısını (kolonlar, tipler, constraints) getirir.
     * 
     * @param database Veritabanı adı
     * @param table Tablo adı
     * @return Result<TableStructure> Tablo yapısı bilgileri
     */
    suspend fun getTableStructure(database: String, table: String): Result<TableStructure> = 
        withContext(Dispatchers.IO) {
        try {
            val connection = connectionManager.getConnection()
                ?: return@withContext Result.failure(Exception("Bağlantı yok"))
            
            val columns = mutableListOf<ColumnInfo>()
            
            connection.createStatement().use { statement ->
                statement.executeQuery("DESCRIBE `$database`.`$table`").use { resultSet ->
                    while (resultSet.next()) {
                        val field = resultSet.getString("Field")
                        val type = resultSet.getString("Type")
                        val nullable = resultSet.getString("Null") == "YES"
                        val key = resultSet.getString("Key")
                        val default = resultSet.getString("Default")
                        val extra = resultSet.getString("Extra")
                        
                        // Tip ve uzunluğu parse et (örn: VARCHAR(255))
                        val (baseType, length) = parseColumnType(type)
                        
                        columns.add(
                            ColumnInfo(
                                name = field,
                                type = baseType,
                                length = length,
                                nullable = nullable,
                                isPrimaryKey = key == "PRI",
                                isAutoIncrement = extra.contains("auto_increment", ignoreCase = true),
                                defaultValue = default
                            )
                        )
                    }
                }
            }
            
            Result.success(TableStructure(columns))
            
        } catch (e: Exception) {
            val error = e.toMilaDbError()
            Result.failure(Exception(error.toUserMessage(), e))
        }
    }
    
    /**
     * Tablodaki bir satırı günceller.
     * 
     * @param database Veritabanı adı
     * @param table Tablo adı
     * @param primaryKeyColumn Primary key kolon adı
     * @param primaryKeyValue Primary key değeri
     * @param updates Güncellenecek kolon-değer çiftleri
     * @return Result<Int> Etkilenen satır sayısı
     */
    suspend fun updateRow(
        database: String,
        table: String,
        primaryKeyColumn: String,
        primaryKeyValue: String,
        updates: Map<String, String>
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val connection = connectionManager.getConnection()
                ?: return@withContext Result.failure(Exception("Bağlantı yok"))
            
            // UPDATE sorgusu oluştur
            val setClauses = updates.keys.joinToString(", ") { "`$it` = ?" }
            val sql = "UPDATE `$database`.`$table` SET $setClauses WHERE `$primaryKeyColumn` = ?"
            
            connection.prepareStatement(sql).use { statement ->
                // Parametreleri ayarla
                updates.values.forEachIndexed { index, value ->
                    statement.setString(index + 1, value)
                }
                statement.setString(updates.size + 1, primaryKeyValue)
                
                val affectedRows = statement.executeUpdate()
                Result.success(affectedRows)
            }
            
        } catch (e: Exception) {
            val error = e.toMilaDbError()
            Result.failure(Exception(error.toUserMessage(), e))
        }
    }
    
    /**
     * Tabloya yeni satır ekler.
     * 
     * @param database Veritabanı adı
     * @param table Tablo adı
     * @param values Kolon-değer çiftleri
     * @return Result<Int> Etkilenen satır sayısı
     */
    suspend fun insertRow(
        database: String,
        table: String,
        values: Map<String, String>
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val connection = connectionManager.getConnection()
                ?: return@withContext Result.failure(Exception("Bağlantı yok"))
            
            // INSERT sorgusu oluştur
            val columns = values.keys.joinToString(", ") { "`$it`" }
            val placeholders = values.keys.joinToString(", ") { "?" }
            val sql = "INSERT INTO `$database`.`$table` ($columns) VALUES ($placeholders)"
            
            connection.prepareStatement(sql).use { statement ->
                // Parametreleri ayarla
                values.values.forEachIndexed { index, value ->
                    statement.setString(index + 1, value)
                }
                
                val affectedRows = statement.executeUpdate()
                Result.success(affectedRows)
            }
            
        } catch (e: Exception) {
            val error = e.toMilaDbError()
            Result.failure(Exception(error.toUserMessage(), e))
        }
    }
    
    /**
     * Tablodan satır siler.
     * 
     * @param database Veritabanı adı
     * @param table Tablo adı
     * @param primaryKeyColumn Primary key kolon adı
     * @param primaryKeyValue Primary key değeri
     * @return Result<Int> Etkilenen satır sayısı
     */
    suspend fun deleteRow(
        database: String,
        table: String,
        primaryKeyColumn: String,
        primaryKeyValue: String
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val connection = connectionManager.getConnection()
                ?: return@withContext Result.failure(Exception("Bağlantı yok"))
            
            val sql = "DELETE FROM `$database`.`$table` WHERE `$primaryKeyColumn` = ?"
            
            connection.prepareStatement(sql).use { statement ->
                statement.setString(1, primaryKeyValue)
                val affectedRows = statement.executeUpdate()
                Result.success(affectedRows)
            }
            
        } catch (e: Exception) {
            val error = e.toMilaDbError()
            Result.failure(Exception(error.toUserMessage(), e))
        }
    }

    
    /**
     * Yeni tablo oluşturur.
     * 
     * @param database Veritabanı adı
     * @param tableDefinition Tablo tanımı
     * @return Result<Unit> Başarı durumu
     */
    suspend fun createTable(
        database: String,
        tableDefinition: TableDefinition
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val connection = connectionManager.getConnection()
                ?: return@withContext Result.failure(Exception("Bağlantı yok"))
            
            // CREATE TABLE sorgusu oluştur
            val columnDefinitions = tableDefinition.columns.joinToString(", ") { column ->
                buildColumnDefinition(column, tableDefinition.tableCollation)
            }
            
            // Primary key'leri bul
            val primaryKeys = tableDefinition.columns
                .filter { it.isPrimaryKey }
                .joinToString(", ") { "`${it.name}`" }
            
            val primaryKeyClause = if (primaryKeys.isNotEmpty()) {
                ", PRIMARY KEY ($primaryKeys)"
            } else {
                ""
            }
            
            val tableOptions = tableDefinition.tableCollation?.let { coll ->
                val charset = coll.substringBefore("_")
                " DEFAULT CHARSET=$charset COLLATE=$coll"
            } ?: ""

            val sql = "CREATE TABLE `$database`.`${tableDefinition.tableName}` " +
                    "($columnDefinitions$primaryKeyClause)" + tableOptions
            
            connection.createStatement().use { statement ->
                statement.executeUpdate(sql)
            }
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            val error = e.toMilaDbError()
            Result.failure(Exception(error.toUserMessage(), e))
        }
    }
    
    /**
     * Tabloyu siler.
     * 
     * @param database Veritabanı adı
     * @param table Tablo adı
     * @return Result<Unit> Başarı durumu
     */
    suspend fun dropTable(
        database: String,
        table: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val connection = connectionManager.getConnection()
                ?: return@withContext Result.failure(Exception("Bağlantı yok"))
            
            val sql = "DROP TABLE `$database`.`$table`"
            
            connection.createStatement().use { statement ->
                statement.executeUpdate(sql)
            }
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            val error = e.toMilaDbError()
            Result.failure(Exception(error.toUserMessage(), e))
        }
    }
    
    /**
     * Özel SQL sorgusu çalıştırır.
     * Çoklu sorguları destekler (USE database; SELECT * FROM table gibi).
     * 
     * @param query SQL sorgusu
     * @return Result<QueryResult> Sorgu sonucu (SELECT için veri, diğerleri için etkilenen satır sayısı)
     */
    suspend fun executeQuery(query: String): Result<QueryResult> = withContext(Dispatchers.IO) {
        try {
            val connection = connectionManager.getConnection()
                ?: return@withContext Result.failure(Exception("Bağlantı yok"))
            
            // Sorguları ayır (noktalı virgülle ayrılmış)
            val queries = query.split(";")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            
            if (queries.isEmpty()) {
                return@withContext Result.failure(Exception("Geçerli bir sorgu giriniz"))
            }
            
            connection.createStatement().use { statement ->
                var lastResult: QueryResult? = null
                
                // Her sorguyu sırayla çalıştır
                for (singleQuery in queries) {
                    val trimmedQuery = singleQuery.trim()
                    
                    // SELECT sorgusu mu kontrol et
                    val isSelect = trimmedQuery.startsWith("SELECT", ignoreCase = true) ||
                                   trimmedQuery.startsWith("SHOW", ignoreCase = true) ||
                                   trimmedQuery.startsWith("DESCRIBE", ignoreCase = true) ||
                                   trimmedQuery.startsWith("DESC", ignoreCase = true)
                    
                    if (isSelect) {
                        // SELECT/SHOW/DESCRIBE sorgusu - sonuç döndürür
                        statement.executeQuery(trimmedQuery).use { resultSet ->
                            val tableData = resultSetToTableData(
                                resultSet,
                                "query_result",
                                "custom_query"
                            )
                            lastResult = QueryResult.SelectResult(tableData)
                        }
                    } else {
                        // INSERT/UPDATE/DELETE/CREATE/DROP/USE vb. - etkilenen satır sayısı döndürür
                        val affectedRows = statement.executeUpdate(trimmedQuery)
                        lastResult = QueryResult.ModifyResult(affectedRows)
                    }
                }
                
                // Son sorgunun sonucunu döndür
                Result.success(lastResult ?: QueryResult.ModifyResult(0))
            }
            
        } catch (e: Exception) {
            val error = e.toMilaDbError()
            Result.failure(Exception(error.toUserMessage(), e))
        }
    }
    
    /**
     * Tablonun CREATE TABLE komutunu getirir.
     * 
     * @param database Veritabanı adı
     * @param table Tablo adı
     * @return Result<String> CREATE TABLE komutu
     */
    suspend fun getCreateTableStatement(database: String, table: String): Result<String> = 
        withContext(Dispatchers.IO) {
        try {
            val connection = connectionManager.getConnection()
                ?: return@withContext Result.failure(Exception("Bağlantı yok"))
            
            connection.createStatement().use { statement ->
                statement.executeQuery("SHOW CREATE TABLE `$database`.`$table`").use { resultSet ->
                    if (resultSet.next()) {
                        val createStatement = resultSet.getString(2) // 2. kolon CREATE TABLE
                        Result.success(createStatement)
                    } else {
                        Result.failure(Exception("CREATE TABLE komutu alınamadı"))
                    }
                }
            }
            
        } catch (e: Exception) {
            val error = e.toMilaDbError()
            Result.failure(Exception(error.toUserMessage(), e))
        }
    }
    
    // ========== Yardımcı Fonksiyonlar ==========
    
    /**
     * ResultSet'i TableData'ya dönüştürür.
     * 
     * @param resultSet SQL sorgu sonucu
     * @param tableName Tablo adı
     * @param databaseName Veritabanı adı
     * @return TableData
     */
    private fun resultSetToTableData(
        resultSet: ResultSet,
        tableName: String,
        databaseName: String
    ): TableData {
        val metaData = resultSet.metaData
        val columnCount = metaData.columnCount
        
        // Kolon isimlerini al
        val columns = (1..columnCount).map { metaData.getColumnName(it) }
        
        // Primary key kolonunu bul - DESCRIBE TABLE kullanarak
        val primaryKeyColumn = try {
            val connection = resultSet.statement.connection
            connection.createStatement().use { stmt ->
                stmt.executeQuery("DESCRIBE `$databaseName`.`$tableName`").use { descResultSet ->
                    var pkColumn: String? = null
                    while (descResultSet.next()) {
                        val key = descResultSet.getString("Key")
                        if (key == "PRI") {
                            pkColumn = descResultSet.getString("Field")
                            break
                        }
                    }
                    pkColumn
                }
            }
        } catch (e: Exception) {
            null
        }
        
        // Satırları al
        val rows = mutableListOf<List<String>>()
        while (resultSet.next()) {
            val row = (1..columnCount).map { index ->
                resultSet.getString(index) ?: "NULL"
            }
            rows.add(row)
        }
        
        return TableData(
            columns = columns,
            rows = rows,
            tableName = tableName,
            databaseName = databaseName,
            primaryKeyColumn = primaryKeyColumn
        )
    }
    
    /**
     * Kolon tipini parse eder (örn: VARCHAR(255) -> VARCHAR, 255).
     * 
     * @param type Kolon tipi string
     * @return Pair<String, Int?> Base tip ve uzunluk
     */
    private fun parseColumnType(type: String): Pair<String, Int?> {
        val regex = """(\w+)(?:\((\d+)\))?""".toRegex()
        val match = regex.find(type)
        
        return if (match != null) {
            val baseType = match.groupValues[1]
            val length = match.groupValues.getOrNull(2)?.toIntOrNull()
            Pair(baseType, length)
        } else {
            Pair(type, null)
        }
    }
    
    /**
     * Kolon tanımı string'i oluşturur (CREATE TABLE için).
     * 
     * @param column Kolon tanımı
     * @return SQL kolon tanımı string
     */
    private fun buildColumnDefinition(column: ColumnDefinition, tableCollation: String? = null): String {
        val parts = mutableListOf<String>()
        
        // Kolon adı ve tipi
        val typeWithLength = if (column.length != null) {
            "${column.type}(${column.length})"
        } else {
            column.type
        }
        parts.add("`${column.name}` $typeWithLength")
        
        // NULL/NOT NULL
        parts.add(if (column.nullable) "NULL" else "NOT NULL")
        
        // AUTO_INCREMENT
        if (column.isAutoIncrement) {
            parts.add("AUTO_INCREMENT")
        }
        
        // DEFAULT değer
        if (column.defaultValue != null) {
            parts.add("DEFAULT '${column.defaultValue}'")
        }
        
        // String tiplerinde kolasyon uygula
        val typeUpper = column.type.uppercase()
        val isStringType = typeUpper in setOf("CHAR", "VARCHAR", "TEXT", "TINYTEXT", "MEDIUMTEXT", "LONGTEXT")
        if (isStringType && tableCollation != null) {
            parts.add("COLLATE $tableCollation")
        }

        return parts.joinToString(" ")
    }
}
