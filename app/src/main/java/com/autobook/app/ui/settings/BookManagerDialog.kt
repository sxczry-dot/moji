package com.autobook.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.autobook.app.data.Transaction
import com.autobook.app.util.AppSettings
import kotlinx.coroutines.launch

@Composable
fun BookManagerDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var books by remember { mutableStateOf(AppSettings.getBooks(context)) }
    var current by remember { mutableStateOf(AppSettings.getCurrentBook(context)) }
    var newBookName by remember { mutableStateOf("") }
    var renameTarget by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        onDismissRequest = onDismiss,
        title = { Text("账本管理") },
        text = {
            Column {
                Text(
                    text = "自动记账固定记入「默认账本」，手动记账记入当前选中账本。删除账本时，其中的记录会合并到「默认账本」，不会丢失。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                books.forEach { book ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.Icon(
                            androidx.compose.material.icons.Icons.Outlined.MenuBook,
                            contentDescription = null,
                            tint = if (book == current) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(16.dp)
                        )
                        RadioButton(
                            selected = book == current,
                            onClick = {
                                current = book
                                AppSettings.setCurrentBook(context, book)
                            }
                        )
                        Text(
                            text = book,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (book == current) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        if (book != com.autobook.app.util.AppSettings.DEFAULT_BOOK) {
                            TextButton(
                                onClick = { renameTarget = book }
                            ) { Text("重命名") }
                            TextButton(
                                onClick = {
                                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                        com.autobook.app.AutoBookApp.instance.repository.moveBook(
                                            book, com.autobook.app.util.AppSettings.DEFAULT_BOOK
                                        )
                                        AppSettings.removeBook(context, book)
                                        books = AppSettings.getBooks(context)
                                        current = AppSettings.getCurrentBook(context)
                                    }
                                }
                            ) { Text("删除", color = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newBookName,
                        onValueChange = { newBookName = it },
                        label = { Text("新账本名称") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.padding(4.dp))
                    TextButton(
                        onClick = {
                            val name = newBookName.trim()
                            if (name.isNotEmpty()) {
                                AppSettings.addBook(context, name)
                                books = AppSettings.getBooks(context)
                                newBookName = ""
                            }
                        }
                    ) { Text("添加") }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )

    renameTarget?.let { target ->
        var renameInput by remember { mutableStateOf(target) }
        androidx.compose.material3.AlertDialog(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            onDismissRequest = { renameTarget = null },
            title = { Text("重命名账本") },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text("新名称") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newName = renameInput.trim()
                        if (newName.isNotEmpty() && newName != target) {
                            AppSettings.renameBook(context, target, newName)
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                com.autobook.app.AutoBookApp.instance.repository.moveBook(target, newName)
                            }
                            books = AppSettings.getBooks(context)
                            current = AppSettings.getCurrentBook(context)
                        }
                        renameTarget = null
                    }
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("取消") }
            }
        )
    }
}
