package com.miladb.data.source.export

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.miladb.data.model.TableData
import com.miladb.util.toMilaDbError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Apache POI kullanarak Excel dışa aktarımı yapan sınıf.
 * 
 * Özellikler:
 * - .xlsx formatında dışa aktarma
 * - Kolon başlıkları ilk satırda
 * - Başlık satırı için özel stil (bold, mavi arka plan)
 * - Downloads klasörüne kaydetme
 * - Ana thread dışında çalışır (Dispatchers.IO)
 */
class ExcelExporter(private val context: Context) {
    
    /**
     * Tablo verilerini .xlsx formatında dışa aktarır.
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
            val finalFileName = fileName ?: "${tableData.tableName}_$timestamp.xlsx"
            
            // Downloads klasörüne dosya oluştur
            val downloadsDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            )
            val file = File(downloadsDir, finalFileName)
            
            // Workbook oluştur
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet(tableData.tableName)
            
            // Başlık stili oluştur
            val headerStyle = createHeaderStyle(workbook)
            
            // Başlık satırını yaz
            val headerRow = sheet.createRow(0)
            tableData.columns.forEachIndexed { index, columnName ->
                val cell = headerRow.createCell(index)
                cell.setCellValue(columnName)
                cell.cellStyle = headerStyle
            }
            
            // Veri satırlarını yaz
            tableData.rows.forEachIndexed { rowIndex, rowData ->
                val row = sheet.createRow(rowIndex + 1)
                rowData.forEachIndexed { cellIndex, cellValue ->
                    val cell = row.createCell(cellIndex)
                    cell.setCellValue(cellValue)
                }
            }
            
            // Kolon genişliklerini otomatik ayarla
            tableData.columns.indices.forEach { columnIndex ->
                sheet.autoSizeColumn(columnIndex)
            }
            
            // Dosyaya yaz
            FileOutputStream(file).use { outputStream ->
                workbook.write(outputStream)
            }
            
            workbook.close()
            
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
     * Başlık satırı için stil oluşturur.
     * 
     * @param workbook XSSFWorkbook instance
     * @return XSSFCellStyle Başlık stili
     */
    private fun createHeaderStyle(workbook: XSSFWorkbook) = workbook.createCellStyle().apply {
        val font = workbook.createFont().apply {
            bold = true
            color = IndexedColors.WHITE.index
        }
        
        // Stil ayarları
        setFont(font)
        fillForegroundColor = IndexedColors.BLUE.index
        fillPattern = FillPatternType.SOLID_FOREGROUND
    }
}
