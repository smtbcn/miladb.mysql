package com.miladb.data.repository

import android.net.Uri
import com.miladb.data.model.TableData
import com.miladb.data.source.export.CsvExporter
import com.miladb.data.source.export.ExcelExporter
import com.miladb.data.source.export.SqlExporter
import com.miladb.util.toMilaDbError

/**
 * Dışa aktarma işlemlerini yöneten repository.
 * Excel, CSV ve SQL formatlarında veri dışa aktarımı sağlar.
 * 
 * Özellikler:
 * - Excel (.xlsx) formatında dışa aktarma
 * - CSV (UTF-8) formatında dışa aktarma
 * - SQL (CREATE TABLE + INSERT) formatında dışa aktarma
 * - Hata yönetimi ile Result döndürür
 * - Exporter sınıflarını soyutlar
 */
class ExportRepository(
    private val excelExporter: ExcelExporter,
    private val csvExporter: CsvExporter,
    private val sqlExporter: SqlExporter,
    private val databaseRepository: DatabaseRepository
) {
    
    /**
     * Tablo verilerini Excel formatında dışa aktarır.
     * 
     * @param tableData Dışa aktarılacak tablo verileri
     * @param fileName Dosya adı (opsiyonel)
     * @return Result<Uri> Oluşturulan dosyanın URI'si
     */
    suspend fun exportToExcel(
        tableData: TableData,
        fileName: String? = null
    ): Result<Uri> {
        return try {
            excelExporter.export(tableData, fileName)
        } catch (e: Exception) {
            val error = e.toMilaDbError()
            Result.failure(Exception(error.toUserMessage(), e))
        }
    }
    
    /**
     * Tablo verilerini CSV formatında dışa aktarır.
     * 
     * @param tableData Dışa aktarılacak tablo verileri
     * @param fileName Dosya adı (opsiyonel)
     * @return Result<Uri> Oluşturulan dosyanın URI'si
     */
    suspend fun exportToCsv(
        tableData: TableData,
        fileName: String? = null
    ): Result<Uri> {
        return try {
            csvExporter.export(tableData, fileName)
        } catch (e: Exception) {
            val error = e.toMilaDbError()
            Result.failure(Exception(error.toUserMessage(), e))
        }
    }
    
    /**
     * Tablo verilerini SQL formatında dışa aktarır.
     * CREATE TABLE ve INSERT INTO komutlarını içerir.
     * 
     * @param tableData Dışa aktarılacak tablo verileri
     * @param fileName Dosya adı (opsiyonel)
     * @return Result<Uri> Oluşturulan dosyanın URI'si
     */
    suspend fun exportToSql(
        tableData: TableData,
        fileName: String? = null
    ): Result<Uri> {
        return try {
            // CREATE TABLE komutunu al
            val createTableResult = databaseRepository.getCreateTableStatement(
                tableData.databaseName,
                tableData.tableName
            )
            
            if (createTableResult.isFailure) {
                return Result.failure(createTableResult.exceptionOrNull()!!)
            }
            
            val createTableStatement = createTableResult.getOrNull()!!
            
            // SQL export
            sqlExporter.export(tableData, createTableStatement, fileName ?: "${tableData.tableName}.sql")
        } catch (e: Exception) {
            val error = e.toMilaDbError()
            Result.failure(Exception(error.toUserMessage(), e))
        }
    }
}
