package com.autobook.app.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.autobook.app.data.Transaction
import com.autobook.app.data.expenseSum
import com.autobook.app.data.localTime
import com.autobook.app.util.formatAmount
import com.autobook.app.util.formatCategory
import com.autobook.app.util.formatTime
import com.autobook.app.ui.theme.ExpenseRed
import com.autobook.app.ui.theme.IncomeBlue

@Composable
fun DrillDownDialog(
    title: String,
    transactions: List<Transaction>,
    onDismiss: () -> Unit,
    hideAmount: Boolean = false
) {
    AlertDialog(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (transactions.isEmpty()) {
                    com.autobook.app.ui.EmptyHint(
                        icon = Icons.Outlined.Inbox,
                        text = "暂无记录"
                    )
                } else {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "明细",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    transactions.sortedByDescending { it.timestamp }.forEach { tx ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = tx.merchant,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "${formatCategory(tx.category)} · ${formatTime(tx.localTime())}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = if (hideAmount) "****" else
                                    (if (tx.type == Transaction.TYPE_EXPENSE) "-" else "+") +
                                    formatAmount(tx.amount),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (tx.type == Transaction.TYPE_EXPENSE) ExpenseRed else IncomeBlue
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (hideAmount) "合计 ****" else "合计 ¥${formatAmount(transactions.expenseSum())}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}
