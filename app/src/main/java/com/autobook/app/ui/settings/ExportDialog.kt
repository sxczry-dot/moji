package com.autobook.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.autobook.app.data.localTime
import com.autobook.app.util.AppSettings
import java.time.YearMonth

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExportDialog(
    onDismiss: () -> Unit,
    onExport: (book: String?, year: Int?) -> Unit
) {
    val context = LocalContext.current
    val books = remember { AppSettings.getBooks(context) }
    var mode by remember { mutableIntStateOf(0) }
    var selectedBook by remember { mutableStateOf(books.firstOrNull()) }
    var selectedYear by remember { mutableIntStateOf(YearMonth.now().year) }
    var allTxs by remember { mutableStateOf<List<com.autobook.app.data.Transaction>>(emptyList()) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        allTxs = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            com.autobook.app.AutoBookApp.instance.repository.getAllOnce()
        }
    }

    val previewCount = allTxs.count { tx ->
        (mode != 1 || tx.book == selectedBook) &&
            (mode != 2 || java.util.Calendar.getInstance().apply {
                timeInMillis = tx.localTime()
            }.get(java.util.Calendar.YEAR) == selectedYear)
    }

    AlertDialog(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        onDismissRequest = onDismiss,
        title = { Text("导出范围") },
        text = {
            Column {
                FlowRow {
                    listOf("全部", "按账本", "按年份").forEachIndexed { index, label ->
                        FilterChip(
                            selected = mode == index,
                            onClick = { mode = index },
                            label = { Text(label) },
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }
                }
                if (mode == 1) {
                    Spacer(Modifier.height(10.dp))
                    FlowRow {
                        books.forEach { book ->
                            FilterChip(
                                selected = selectedBook == book,
                                onClick = { selectedBook = book },
                                label = { Text(book) },
                                modifier = Modifier.padding(end = 6.dp)
                            )
                        }
                    }
                }
                if (mode == 2) {
                    Spacer(Modifier.height(10.dp))
                    FlowRow {
                        val nowYear = YearMonth.now().year
                        (2019..nowYear).reversed().forEach { y ->
                            FilterChip(
                                selected = selectedYear == y,
                                onClick = { selectedYear = y },
                                label = { Text("$y") },
                                modifier = Modifier.padding(end = 6.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "将导出 $previewCount 笔记录",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onExport(
                        if (mode == 1) selectedBook else null,
                        if (mode == 2) selectedYear else null
                    )
                }
            ) { Text("导出") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
