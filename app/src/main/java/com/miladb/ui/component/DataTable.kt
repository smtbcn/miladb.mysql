package com.miladb.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.miladb.data.model.TableData

/**
 * Kaydırılabilir veri tablosu bileşeni - MySQL tarzı modern görünüm.
 * 
 * Özellikler:
 * - Sabit header (kolon başlıkları)
 * - Yatay ve dikey kaydırma
 * - Dinamik kolon başlıkları
 * - Satır tıklama ve uzun basma desteği
 * - Zebra striping (alternatif satır renkleri)
 * - Profesyonel tablo görünümü
 * 
 * @param tableData Tablo verileri
 * @param onRowClick Satır tıklama callback (opsiyonel)
 * @param onRowLongPress Satır uzun basma callback (opsiyonel)
 * @param modifier Modifier (opsiyonel)
 */
@Composable
fun DataTable(
    tableData: TableData,
    onRowClick: ((rowIndex: Int, rowData: List<String>) -> Unit)? = null,
    onRowLongPress: ((rowIndex: Int, rowData: List<String>) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Dinamik kolon genişlikleri - içeriğe göre
    val columnWidths = tableData.columns.map { column ->
        when {
            column.length < 10 -> 120.dp
            column.length < 20 -> 180.dp
            else -> 240.dp
        }
    }
    
    // Paylaşılan scroll state - header ve satırlar birlikte kaydırılacak
    val horizontalScrollState = rememberScrollState()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Üst boşluk - TopAppBar ile tablo header arasında margin
        Spacer(modifier = Modifier.height(8.dp))
        
        // Header - Kolon başlıkları (MySQL tarzı) - Daha açık ton
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(horizontalScrollState)
                    .padding(vertical = 12.dp)
            ) {
                tableData.columns.forEachIndexed { index, column ->
                    Box(
                        modifier = Modifier
                            .width(columnWidths[index])
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = column,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        
        Divider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
        
        // Veri satırları - Modern tablo görünümü
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentPadding = PaddingValues(0.dp)
        ) {
            itemsIndexed(tableData.rows) { rowIndex, row ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (rowIndex % 2 == 0) {
                                    MaterialTheme.colorScheme.surface
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                }
                            )
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = {
                                        onRowClick?.invoke(rowIndex, row)
                                    },
                                    onLongPress = {
                                        onRowLongPress?.invoke(rowIndex, row)
                                    }
                                )
                            }
                            .horizontalScroll(horizontalScrollState)
                            .padding(vertical = 12.dp)
                    ) {
                        row.forEachIndexed { cellIndex, cellValue ->
                            Box(
                                modifier = Modifier
                                    .width(columnWidths[cellIndex])
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = cellValue,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    
                    // Satır ayırıcı çizgi
                    Divider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}
