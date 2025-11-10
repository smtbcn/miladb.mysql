package com.miladb.data.source.export

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.miladb.data.model.TableData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter

/**
 * SQL formatında dışa aktarım yapan sınıf.
 * 
 * CREATE TABLE ve INSERT INTO komutlarını içeren SQL dosyası oluşturur.
 */
class SqlExporter(private val context: Context) {
    
    /**
     * Tablo yapısını ve verilerini SQL formatında dışa aktarır.
     * 
     * @param tableData Tablo verileri
     * @param createTableStatement CREATE TABLE komutu
     * @param fileName Dosya adı
     * @return Result<Uri> Oluşturulan dosyanın URI'si
     */
    suspend fun export(
        tableData: TableData,
        createTableStatement: String,
        fileName: String
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            // Downloads klasörüne kaydet
            val downloadsDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            )
            
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            
            val file = File(downloadsDir, fileName)
            
            FileWriter(file).use { writer ->
                // Header
                writer.write("-- SQL Export\n")
                writer.write("-- Database: ${tableData.databaseName}\n")
                writer.write("-- Table: ${tableData.tableName}\n")
                writer.write("-- Generated: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date())}\n\n")
                
                // CREATE TABLE statement
                writer.write("-- Table structure for `${tableData.tableName}`\n")
                writer.write("DROP TABLE IF EXISTS `${tableData.tableName}`;\n")
                writer.write(createTableStatement)
                writer.write(";\n\n")
                
                // INSERT statements
                if (tableData.rows.isNotEmpty()) {
                    writer.write("-- Data for table `${tableData.tableName}`\n")
                    writer.write("INSERT INTO `${tableData.tableName}` (")
                    
                    // Kolon isimleri
                    writer.write(tableData.columns.joinToString(", ") { "`$it`" })
                    writer.write(") VALUES\n")
                    
                    // Satırlar
                    tableData.rows.forEachIndexed { index, row ->
                        writer.write("(")
                        writer.write(row.joinToString(", ") { value ->
                            if (value == "NULL" || value.isBlank()) {
                                "NULL"
                            } else {
                                // String escape
                                val escaped = value
                                    .replace("\\", "\\\\")
                                    .replace("'", "\\'")
                                    .replace("\n", "\\n")
                                    .replace("\r", "\\r")
                                "'$escaped'"
                            }
                        })
                        writer.write(")")
                        
                        if (index < tableData.rows.size - 1) {
                            writer.write(",\n")
                        } else {
                            writer.write(";\n")
                        }
                    }
                }
            }
            
            Result.success(Uri.fromFile(file))
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
