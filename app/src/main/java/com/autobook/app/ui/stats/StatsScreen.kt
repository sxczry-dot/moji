package com.autobook.app.ui.stats

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.DonutLarge
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.outlined.CompareArrows
import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.autobook.app.data.Categories
import com.autobook.app.data.Transaction
import com.autobook.app.data.expenseSum
import com.autobook.app.data.incomeSum
import com.autobook.app.data.localTime
import com.autobook.app.data.refundSum
import com.autobook.app.ui.CategoryUi
import com.autobook.app.ui.theme.ExpenseRed
import com.autobook.app.ui.theme.PrimaryGreen
import com.autobook.app.ui.theme.IncomeBlue
import com.autobook.app.ui.MonthPickerLabel
import com.autobook.app.ui.charts.BarChart
import com.autobook.app.ui.charts.PieChart
import com.autobook.app.util.formatAmountGrouped
import com.autobook.app.viewmodel.MainViewModel

@Composable
fun StatsScreen(modifier: Modifier = Modifier, statsTick: Int = 0) {
    val viewModel: MainViewModel = viewModel()
    val monthTxs by viewModel.monthTransactions.collectAsStateWithLifecycle()
    val currentMonth by viewModel.currentMonth.collectAsStateWithLifecycle()
    val sixMonths by viewModel.lastSixMonths.collectAsStateWithLifecycle()
    val allTxs by viewModel.allTransactions.collectAsStateWithLifecycle()

    var dataMode by rememberSaveable { mutableIntStateOf(0) }

    val (yearStart, yearEnd) = com.autobook.app.util.TimeUtils.monthRange(
        currentMonth.year, 0
    ).let { it.first to com.autobook.app.util.TimeUtils.monthRange(currentMonth.year, 11).second }
    val currentBook by viewModel.currentBook.collectAsStateWithLifecycle()
    val hideAmount by viewModel.hideAmount.collectAsStateWithLifecycle()
    val yearTxs = remember(allTxs, currentBook, yearStart, yearEnd) {
        allTxs.filter {
            it.localTime() in yearStart until yearEnd && it.book == currentBook
        }
    }
    val displayTxs = if (dataMode == 0) monthTxs else yearTxs

    var categoryExpense by remember { mutableStateOf(emptyList<Pair<String, Double>>()) }
    androidx.compose.runtime.LaunchedEffect(displayTxs) {
        // 聚合计算放到后台线程，数据量大时不卡 UI
        categoryExpense = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            StatsCalculator.categoryExpense(displayTxs)
        }
    }

    val totalExpense = remember(categoryExpense) { categoryExpense.sumOf { it.second } }
    val totalIncome = remember(displayTxs) { displayTxs.incomeSum() }
    val refundTotal = remember(displayTxs) { displayTxs.refundSum() }

    val listState = rememberSaveable(saver = androidx.compose.foundation.lazy.LazyListState.Saver) {
        androidx.compose.foundation.lazy.LazyListState()
    }
    var viewMode by rememberSaveable { mutableIntStateOf(0) }
    var drillTitle by remember { mutableStateOf("") }
    var drillTxs by remember { mutableStateOf<List<Transaction>>(emptyList()) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var cardMessage by remember { mutableStateOf<String?>(null) }
    var cardUri by remember { mutableStateOf<String?>(null) }
    var cardPreview by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var budget by remember { mutableStateOf(0.0) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(currentMonth) {
        budget = com.autobook.app.util.AppSettings.getBudget(
            context,
            "%d-%02d".format(currentMonth.year, currentMonth.monthValue)
        )
    }
    val contentAlpha = remember { androidx.compose.animation.core.Animatable(1f) }
    androidx.compose.runtime.LaunchedEffect(dataMode, viewMode, currentMonth) {
        contentAlpha.snapTo(0.35f)
        contentAlpha.animateTo(1f, androidx.compose.animation.core.tween(220))
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { alpha = contentAlpha.value },
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("月度", "年度").forEachIndexed { index, label ->
                    androidx.compose.material3.FilterChip(
                        selected = dataMode == index,
                        onClick = { dataMode = index },
                        label = { Text(label, fontSize = 11.sp) },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                listOf("图表", "日历").forEachIndexed { index, label ->
                    androidx.compose.material3.FilterChip(
                        selected = viewMode == index,
                        onClick = { viewMode = index },
                        label = { Text(label, fontSize = 11.sp) },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 2.dp)
            ) {
                MonthPickerLabel(
                    yearMonth = currentMonth,
                    onSelect = viewModel::setMonth,
                    yearOnly = dataMode == 1
                )
            }
        }

        if (viewMode == 0 && dataMode == 0) {
            item {
                BudgetCard(
                    budget = budget,
                    spent = totalExpense,
                    hideAmount = hideAmount,
                    statsTick = statsTick,
                    onEdit = { showBudgetDialog = true }
                )
            }
        }

        item {
            val lastYearStart = java.util.Calendar.getInstance().apply {
                add(java.util.Calendar.YEAR, -1)
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            val lastYearEnd = (lastYearStart.clone() as java.util.Calendar).apply {
                add(java.util.Calendar.DAY_OF_YEAR, 1)
            }
            // 日期区间纳入 remember 键：跨午夜（无新交易时）"去年的今天"也会刷新
            val lastYearKey = "${lastYearStart.timeInMillis}_${lastYearEnd.timeInMillis}"
            val lastYearTodayTxs = remember(allTxs, currentBook, lastYearKey) {
                allTxs.filter {
                    it.localTime() in lastYearStart.timeInMillis until lastYearEnd.timeInMillis &&
                        it.book == currentBook
                }
            }
            val lastYearExpense = lastYearTodayTxs.expenseSum()
            if (lastYearExpense > 0) {
                Text(
                    text = if (hideAmount) {
                        "去年的今天，你花了 **** · ${lastYearTodayTxs.size} 笔"
                    } else {
                        "去年的今天，你花了 ¥${com.autobook.app.util.formatAmount(lastYearExpense)} · ${lastYearTodayTxs.size} 笔"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }
        }

        if (viewMode == 1) {
            item {
                CalendarMonthView(displayTxs, currentMonth, yearMode = dataMode == 1, hideAmount = hideAmount)
                Spacer(Modifier.height(16.dp))
            }
        } else {

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .animateItem(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
            ) {
                Column(Modifier.padding(20.dp)) {
Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.DonutLarge,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                    Text(
                        text = "分类支出",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )                    }
                    Spacer(Modifier.height(16.dp))
                    if (categoryExpense.isEmpty()) {
                        com.autobook.app.ui.EmptyHint(
                            icon = Icons.Outlined.DonutLarge,
                            text = if (dataMode == 0) "本月暂无支出记录" else "本年度暂无支出记录",
                            subText = "记几笔，这里就有分布图"
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            PieChart(
                                entries = categoryExpense,
                                colorOf = { CategoryUi.colorOf(it) },
                                modifier = Modifier.size(170.dp)
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "总支出",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (hideAmount) "****" else formatAmountGrouped(totalExpense),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (refundTotal > 0) {
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = if (hideAmount) {
                                            "净支出 ****"
                                        } else {
                                            "净支出 ${formatAmountGrouped(totalExpense - refundTotal)}"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = if (hideAmount) "退款 ****" else "退款 ${formatAmountGrouped(refundTotal)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = IncomeBlue
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                        categoryExpense.forEach { (cat, amount) ->
                            CategoryRankRow(cat, amount, totalExpense, hideAmount, statsTick = statsTick) {
                                drillTitle = cat
                                drillTxs = displayTxs.filter {
                                    it.type == Transaction.TYPE_EXPENSE &&
                                        Categories.primaryOf(it.category) == cat
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            val lastYearRange = com.autobook.app.util.TimeUtils.monthRange(
                currentMonth.year - 1, currentMonth.monthValue - 1
            )
            val lastYearExpense = allTxs.filter {
                it.localTime() in lastYearRange.first until lastYearRange.second &&
                    it.book == currentBook
            }.expenseSum()
            MonthCompareCard(
                displayTxs, sixMonths, dataMode, currentMonth, lastYearExpense,
                modifier = Modifier.animateItem(),
                hideAmount = hideAmount
            )
        }

        item {
            val expenseTxs = remember(displayTxs) {
                displayTxs.filter { it.type == Transaction.TYPE_EXPENSE }
            }
            val buckets = listOf(
                "10元以下" to { a: Double -> a < 10 },
                "10-50元" to { a: Double -> a >= 10 && a < 50 },
                "50-100元" to { a: Double -> a >= 50 && a < 100 },
                "100-500元" to { a: Double -> a >= 100 && a < 500 },
                "500元以上" to { a: Double -> a >= 500 }
            )
            val bucketCounts = buckets.map { (label, pred) ->
                label to expenseTxs.count { pred(it.amount) }
            }
            val maxCount = (bucketCounts.maxOfOrNull { it.second } ?: 0).takeIf { it > 0 } ?: 1
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .animateItem(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
            ) {
                Column(Modifier.padding(20.dp)) {
Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.BarChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (dataMode == 0) "本月金额分布" else "年度金额分布",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )                    }
                    Spacer(Modifier.height(12.dp))
                    if (expenseTxs.isEmpty()) {
                        com.autobook.app.ui.EmptyHint(
                            icon = Icons.Outlined.BarChart,
                            text = "暂无支出记录",
                            subText = "记一笔，这里就有金额分布"
                        )
                    } else {
                        val bucketAnim = remember(statsTick) {
                            androidx.compose.animation.core.Animatable(0f)
                        }
                        androidx.compose.runtime.LaunchedEffect(statsTick) {
                            bucketAnim.snapTo(0f)
                            bucketAnim.animateTo(
                                1f,
                                androidx.compose.animation.core.spring(
                                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                                    stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                                )
                            )
                        }
                        bucketCounts.forEachIndexed { index, (label, count) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(76.dp)
                                )
                                com.autobook.app.ui.FlatProgressBar(
                                    progress = count.toFloat() / maxCount * bucketAnim.value,
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.primary.copy(
                                        alpha = 0.35f + index * 0.16f
                                    )
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = "$count 笔",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            MerchantRankCard(displayTxs, modifier = Modifier.animateItem(), hideAmount = hideAmount, statsTick = statsTick) { name ->
                drillTitle = name
                drillTxs = displayTxs.filter {
                    it.type == Transaction.TYPE_EXPENSE && it.merchant == name
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .animateItem(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
            ) {
                Column(Modifier.padding(20.dp)) {
Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Timeline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (dataMode == 0) "本月每日支出趋势" else "本年每月支出趋势",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )                    }
                    Spacer(Modifier.height(16.dp))
                    if (dataMode == 0) {
                        val dailyExpense = displayTxs
                            .filter { it.type == Transaction.TYPE_EXPENSE }
                            .groupBy {
                                java.time.Instant.ofEpochMilli(it.timestamp)
                                    .atZone(java.time.ZoneId.systemDefault()).dayOfMonth
                            }
                            .mapValues { (_, list) -> list.sumOf { it.amount } }
                        val days = currentMonth.lengthOfMonth()
                        BarChart(
                            values = (1..days).map { dailyExpense[it] ?: 0.0 },
                            labels = (1..days).map { if (it == 1 || it % 5 == 1) "${it}日" else "" },
                            averageLine = if (days > 0) totalExpense / days else null,
                            hideValues = hideAmount,
                            onBarClick = { day ->
                                drillTitle = "${currentMonth.year}年${currentMonth.monthValue}月${day}日支出"
                                drillTxs = displayTxs.filter {
                                    it.type == Transaction.TYPE_EXPENSE &&
                                        java.time.Instant.ofEpochMilli(it.timestamp)
                                            .atZone(java.time.ZoneId.systemDefault()).dayOfMonth == day
                                }
                            }
                        )
                    } else {
                        val yearlyExpense = (1..12).map { m ->
                            yearTxs
                                .filter {
                                    it.type == Transaction.TYPE_EXPENSE &&
                                        java.time.Instant.ofEpochMilli(it.timestamp)
                                            .atZone(java.time.ZoneId.systemDefault()).monthValue == m
                                }
                                .sumOf { it.amount }
                        }
                        BarChart(
                            values = yearlyExpense,
                            labels = (1..12).map { "${it}月" },
                            hideValues = hideAmount,
                            onBarClick = { m ->
                                drillTitle = "${currentMonth.year}年${m}月支出"
                                drillTxs = yearTxs.filter {
                                    it.type == Transaction.TYPE_EXPENSE &&
                                        java.time.Instant.ofEpochMilli(it.timestamp)
                                            .atZone(java.time.ZoneId.systemDefault()).monthValue == m
                                }
                            }
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .animateItem(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
            ) {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Assessment,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (dataMode == 0) "月度报表" else "年度报表",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = {
                                scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                    val newBmp = com.autobook.app.util.MonthCardImage.render(
                                        context, currentMonth, displayTxs,
                                        yearMode = dataMode == 1
                                    )
                                    // 回收上一张预览位图，避免连续生成时累积大块内存
                                    cardPreview?.takeIf { !it.isRecycled }?.recycle()
                                    cardPreview = newBmp
                                }
                            }
                        ) { Text("卡片图", fontSize = 11.sp) }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ReportItem(
                            "支出",
                            if (hideAmount) "****" else formatAmountGrouped(totalExpense),
                            androidx.compose.material.icons.Icons.Outlined.TrendingDown
                        )
                        ReportItem(
                            "收入",
                            if (hideAmount) "****" else formatAmountGrouped(totalIncome),
                            androidx.compose.material.icons.Icons.Outlined.TrendingUp
                        )
                        ReportItem(
                            "笔数",
                            "${displayTxs.size}笔",
                            Icons.Outlined.ReceiptLong
                        )
                    }
                }
            }
        }
        }
    }

    if (showBudgetDialog) {
        var budgetInput by remember {
            mutableStateOf(if (budget > 0) com.autobook.app.util.formatAmount(budget) else "")
        }
        androidx.compose.material3.AlertDialog(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            onDismissRequest = { showBudgetDialog = false },
            title = { Text("本月预算") },
            text = {
                Column {
                    androidx.compose.material3.OutlinedTextField(
                        value = budgetInput,
                        onValueChange = { budgetInput = com.autobook.app.util.filterAmountInput(it) },
                        label = { Text("预算金额（元）") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                        )
                    )
                    if (budget > 0) {
                        Spacer(Modifier.height(6.dp))
                        TextButton(
                            onClick = {
                                com.autobook.app.util.AppSettings.clearBudget(
                                    context,
                                    "%d-%02d".format(currentMonth.year, currentMonth.monthValue)
                                )
                                budget = 0.0
                                showBudgetDialog = false
                            }
                        ) {
                            Text(
                                "清除预算",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = budgetInput.toDoubleOrNull()?.takeIf { it > 0 } != null,
                    onClick = {
                        val v = budgetInput.toDoubleOrNull() ?: return@TextButton
                        com.autobook.app.util.AppSettings.setBudget(
                            context,
                            "%d-%02d".format(currentMonth.year, currentMonth.monthValue),
                            v
                        )
                        budget = v
                        showBudgetDialog = false
                    }
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { showBudgetDialog = false }) { Text("取消") }
            }
        )
    }

    if (drillTitle.isNotEmpty()) {
        DrillDownDialog(
            title = drillTitle,
            transactions = drillTxs,
            hideAmount = hideAmount,
            onDismiss = {
                drillTitle = ""
                drillTxs = emptyList()
            }
        )
    }

    cardPreview?.let { bmp ->
        // 对话框退出组合后回收位图
        androidx.compose.runtime.DisposableEffect(bmp) {
            onDispose { if (!bmp.isRecycled) bmp.recycle() }
        }
        androidx.compose.material3.AlertDialog(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            onDismissRequest = { cardPreview = null },
            title = { Text("账单卡片预览") },
            text = {
                androidx.compose.foundation.Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "账单卡片",
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            val saved = com.autobook.app.util.MonthCardImage.saveToGallery(
                                context, bmp
                            )
                            cardUri = saved
                            cardMessage = if (saved != null) {
                                "账单卡片已保存到相册"
                            } else {
                                "保存失败，请重试"
                            }
                        }
                        cardPreview = null
                    }
                ) { Text("保存到相册") }
            },
            dismissButton = {
                TextButton(onClick = { cardPreview = null }) { Text("关闭") }
            }
        )
    }

    cardMessage?.let { msg ->
        androidx.compose.material3.AlertDialog(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            onDismissRequest = { cardMessage = null },
            title = { Text("账单卡片") },
            text = { Text(msg) },
            confirmButton = {
                if (cardUri?.startsWith("content://") == true) {
                    TextButton(
                        onClick = {
                            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "image/png"
                                putExtra(
                                    android.content.Intent.EXTRA_STREAM,
                                    android.net.Uri.parse(cardUri)
                                )
                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(
                                android.content.Intent.createChooser(shareIntent, "分享账单卡片")
                            )
                        }
                    ) { Text("分享") }
                }
                TextButton(onClick = { cardMessage = null }) { Text("关闭") }
            }
        )
    }
}

