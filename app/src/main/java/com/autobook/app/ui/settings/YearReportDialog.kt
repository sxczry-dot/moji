package com.autobook.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autobook.app.data.Categories
import com.autobook.app.data.Transaction
import com.autobook.app.data.expenseSum
import com.autobook.app.data.incomeSum
import com.autobook.app.data.refundSum
import com.autobook.app.util.TimeUtils
import com.autobook.app.util.formatAmount
import com.autobook.app.viewmodel.MainViewModel
import java.time.YearMonth

@Composable
fun YearReportDialog(onDismiss: () -> Unit) {
    val viewModel: MainViewModel = viewModel()
    val allTxs by viewModel.allTransactions.collectAsStateWithLifecycle()
    val currentBook by viewModel.currentBook.collectAsStateWithLifecycle()

    val reportYear = remember { YearMonth.now().year }
    val (start, end) = TimeUtils.monthRange(reportYear, 0)
        .let { it.first to TimeUtils.monthRange(reportYear, 11).second }
    val yearTxs = allTxs.filter {
        it.timestamp in start until end && it.book == currentBook
    }

    val totalExpense = yearTxs.expenseSum()
    val totalIncome = yearTxs.incomeSum()
    val expenseCount = yearTxs.count { it.type == Transaction.TYPE_EXPENSE }

    val topCategory = yearTxs
        .filter { it.type == Transaction.TYPE_EXPENSE }
        .groupBy { Categories.primaryOf(it.category) }
        .maxByOrNull { (_, list) -> list.sumOf { it.amount } }

    val yearRefund = yearTxs.refundSum()

    val topMerchant = yearTxs
        .filter { it.type == Transaction.TYPE_EXPENSE && it.merchant.isNotBlank() }
        .groupBy { it.merchant }
        // "最常"按笔数统计，而非单笔大额即当选
        .maxByOrNull { (_, list) -> list.size }

    val topDay = yearTxs
        .filter { it.type == Transaction.TYPE_EXPENSE }
        .groupBy { TimeUtils.dayStartOf(it.timestamp) }
        .maxByOrNull { (_, list) -> list.sumOf { it.amount } }

    AlertDialog(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        onDismissRequest = onDismiss,
        title = { Text("${reportYear}年度报告") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                ReportLine(
                    "全年支出", "¥${formatAmount(totalExpense)}",
                    androidx.compose.material.icons.Icons.Outlined.TrendingDown
                )
                if (yearRefund > 0) {
                    ReportLine(
                        "全年退款", "¥${formatAmount(yearRefund)}",
                        androidx.compose.material.icons.Icons.Outlined.TrendingUp
                    )
                    ReportLine(
                        "全年净支出（扣退款）", "¥${formatAmount(totalExpense - yearRefund)}",
                        androidx.compose.material.icons.Icons.Outlined.TrendingDown
                    )
                }
                ReportLine(
                    "全年收入", "¥${formatAmount(totalIncome)}",
                    androidx.compose.material.icons.Icons.Outlined.TrendingUp
                )
                ReportLine(
                    "支出笔数", "$expenseCount 笔",
                    androidx.compose.material.icons.Icons.Outlined.ReceiptLong
                )
                ReportLine(
                    "支出最多分类",
                    topCategory?.let { "${it.key}（¥${formatAmount(it.value.sumOf { t -> t.amount })}）" } ?: "无",
                    androidx.compose.material.icons.Icons.Outlined.Category
                )
                ReportLine(
                    "最常消费商户",
                    topMerchant?.let {
                        "${it.key}（${it.value.size}笔 · ¥${formatAmount(it.value.sumOf { t -> t.amount })}）"
                    } ?: "无",
                    androidx.compose.material.icons.Icons.Outlined.Storefront
                )
                ReportLine(
                    "花钱最多的一天",
                    topDay?.let {
                        val dayTxs = it.value
                        val date = java.text.SimpleDateFormat("MM月dd日", java.util.Locale.CHINA)
                            .format(java.util.Date(it.key))
                        "$date（¥${formatAmount(dayTxs.sumOf { t -> t.amount })}）"
                    } ?: "无",
                    androidx.compose.material.icons.Icons.Outlined.CalendarMonth
                )
            }
        },
        confirmButton = {
            val context = androidx.compose.ui.platform.LocalContext.current
            TextButton(
                onClick = {
                    val text = buildString {
                        appendLine("【${reportYear}年度账单 · 默记】")
                        appendLine("全年支出：¥${formatAmount(totalExpense)}")
                        appendLine("全年收入：¥${formatAmount(totalIncome)}")
                        appendLine("支出笔数：$expenseCount 笔")
                        appendLine(
                            "支出最多分类：" +
                                (topCategory?.let {
                                    "${it.key}（¥${formatAmount(it.value.sumOf { t -> t.amount })}）"
                                } ?: "无")
                        )
                        appendLine(
                            "最常消费商户：" +
                                (topMerchant?.let {
                                    "${it.key}（¥${formatAmount(it.value.sumOf { t -> t.amount })}）"
                                } ?: "无")
                        )
                        appendLine(
                            "花钱最多的一天：" +
                                (topDay?.let {
                                    val dayTxs = it.value
                                    val date = java.text.SimpleDateFormat(
                                        "MM月dd日", java.util.Locale.CHINA
                                    ).format(java.util.Date(it.key))
                                    "$date（¥${formatAmount(dayTxs.sumOf { t -> t.amount })}）"
                                } ?: "无")
                        )
                    }
                    val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_TEXT, text)
                    }
                    context.startActivity(
                        android.content.Intent.createChooser(sendIntent, "分享年度报告")
                    )
                }
            ) { Text("分享") }
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

@Composable
private fun ReportLine(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Spacer(Modifier.height(10.dp))
    Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        androidx.compose.material3.Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}
