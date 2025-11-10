package com.miladb.data.source.export

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.miladb.data.model.TableData
import com.miladb.util.toMilaDbError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Kotlin IO kullanarak CSV dışa aktarımı yapan sınıf.
 * 
 * Özellikler:
 * - .csv formatında dışa aktarma
 * - UTF-8 encoding
 * - Kolon başlıkları ilk satırda
 * - Özel karakterleri escape etme
 * - Downloads klasörüne kaydetme
 * - Ana thread dışında çalışır (Dispatchers.IO)
 */
class CsvExporter(private val context: Context) {
    
    companion object {
        private const val DELIMITER = ","
        private const val QUOTE = "\""
        private const val NEWLINE = "\n"
    }
    
    /**
     * Tablo verilerini .csv formatında dışa aktarır (UTF-8).
     * 
     * @param tableData Dışa aktarılacak tablo verileri
     * @param fileName Dosya adı (opsiyonel, otomatik oluşturulur)
     * @return Result<Uri> Oluşturulan dosyanın URI'si
     */
    suspend fun export(
        tableData: TableData,
        fileName: String? = null
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            // Dosya adı oluştur
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(Date())
            val finalFileName = fileName ?: "${tableData.tableName}_$timestamp.csv"
            
            // Downloads klasörüne dosya oluştur
            val downloadsDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            )
            val file = File(downloadsDir, finalFileName)
            
            // FileWriter ile UTF-8 encoding kullanarak yaz
            FileWriter(file, Charsets.UTF_8).use { writer ->
                // Başlık satırını yaz
                val headerLine = tableData.columns.joinToString(DELIMITER) { column ->
                    escapeCsvValue(column)
                }
                writer.write(headerLine)
                writer.write(NEWLINE)
                
                // Veri satırlarını yaz
                tableData.rows.forEach { row ->
                    val dataLine = row.joinToString(DELIMITER) { value ->
                        escapeCsvValue(value)
                    }
                    writer.write(dataLine)
                    writer.write(NEWLINE)
                }
            }
            
            // URI oluştur
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            Result.success(uri)
            
        } catch (e: Exception) {
            val error = e.toMilaDbError()
            Result.failure(Exception(error.toUserMessage(), e))
        }
    }
    
    /**
     * CSV değerini escape eder.
     * 
     * Kurallar:
     * - Virgül, çift tırnak veya yeni satır içeriyorsa çift tırnak içine al
     * - Çift tırnak karakterini iki katına çıkar
     * - NULL değerini boş string olarak yaz
     * 
     * @param value Escape edilecek değer
     * @return Escape edilmiş değer
     */
    private fun escapeCsvValue(value: String): String {
        // NULL değeri
        if (value == "NULL") {
            return ""
        }
        
        // Özel karakter kontrolü
        val needsQuoting = value.contains(DELIMITER) ||
                          value.contains(QUOTE) ||
                          value.contains("\n") ||
                          value.contains("\r")
        
        return if (needsQuoting) {
            // Çift tırnakları iki katına çıkar ve değeri çift tırnak içine al
            val escaped = value.replace(QUOTE, "$QUOTE$QUOTE")
            "$QUOTE$escaped$QUOTE"
        } else {
            value
        }
    }
}
