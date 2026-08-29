package com.autobook.app.ui

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autobook.app.util.filterAmountInput
import com.autobook.app.util.formatAmount
import com.autobook.app.data.Categories
import com.autobook.app.data.Transaction
import com.autobook.app.ui.CategoryUi
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TransactionDialog(
    tx: Transaction?,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, type: Int, category: String, merchant: String, tags: String, timestamp: Long, book: String) -> Unit,
    onDelete: (() -> Unit)? = null,
    onConfirmAgain: ((amount: Double, type: Int, category: String, merchant: String, tags: String, timestamp: Long, book: String) -> Unit)? = null
) {
    val savedPrimary = tx?.let { Categories.primaryOf(it.category) }
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    var amountText by remember {
        mutableStateOf(tx?.let { formatAmount(it.amount) } ?: "")
    }
    var type by remember { mutableIntStateOf(tx?.type ?: Transaction.TYPE_EXPENSE) }
    var category by remember {
        mutableStateOf(
            when {
                tx == null -> Categories.FOOD
                tx.type == Transaction.TYPE_INCOME -> Categories.INCOME
                else -> savedPrimary ?: Categories.OTHER
            }
        )
    }
    var subCategory by remember {
        mutableStateOf(
            tx?.category?.takeIf { it.contains('/') }?.substringAfter('/')
        )
    }
    var merchant by remember { mutableStateOf(tx?.merchant ?: "") }
    var tags by remember { mutableStateOf(tx?.tags ?: "") }
    var showCalculator by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var editTimestamp by remember { mutableStateOf(tx?.timestamp ?: System.currentTimeMillis()) }
    var amountError by remember { mutableStateOf(false) }
    var topMerchants by remember { mutableStateOf<List<String>>(emptyList()) }
    var categoryOrder by remember { mutableStateOf<List<String>>(emptyList()) }
    var tagCandidates by remember { mutableStateOf<List<String>>(emptyList()) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val repo = com.autobook.app.AutoBookApp.instance.repository
            topMerchants = repo.getTopMerchants(200)
            categoryOrder = repo.getExpenseCategoriesOnce()
                .map { Categories.primaryOf(it) }
                .groupingBy { it }.eachCount()
                .entries.sortedByDescending { it.value }.map { it.key }
            tagCandidates = repo.getUsedTagsOnce()
                .flatMap { it.split(Regex("\\s+")) }
                .filter { it.isNotBlank() }
                .groupingBy { it }.eachCount()
                .entries.sortedByDescending { it.value }.map { it.key }
                .take(100)
        }
    }

    val amountFocus = remember { androidx.compose.ui.focus.FocusRequester() }
    val dialogScope = androidx.compose.runtime.rememberCoroutineScope()

    val amount = amountText.toDoubleOrNull()
    val context = androidx.compose.ui.platform.LocalContext.current
    var book by remember {
        mutableStateOf(
            tx?.book ?: com.autobook.app.util.AppSettings.getCurrentBook(context)
        )
    }
    val books = remember {
        com.autobook.app.util.AppSettings.getBooks(context)
    }
    val customChildren = com.autobook.app.util.AppSettings.getCustomChildren(context, category)
    val children = if (type == Transaction.TYPE_INCOME) {
        Categories.incomeChildren + customChildren
    } else {
        Categories.childrenOf(category, customChildren)
    }

    AlertDialog(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        onDismissRequest = onDismiss,
        title = { Text(if (tx == null) "手动记一笔" else "编辑记录") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = type == Transaction.TYPE_EXPENSE,
                        onClick = {
                            type = Transaction.TYPE_EXPENSE
                            if (category == Categories.INCOME) category = Categories.FOOD
                            subCategory = null
                        },
                        shape = SegmentedButtonDefaults.itemShape(0, 2)
                    ) { Text("支出") }
                    SegmentedButton(
                        selected = type == Transaction.TYPE_INCOME,
                        onClick = {
                            type = Transaction.TYPE_INCOME
                            category = Categories.INCOME
                            subCategory = null
                        },
                        shape = SegmentedButtonDefaults.itemShape(1, 2)
                    ) { Text("收入") }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = filterAmountInput(it) },
                    label = { Text("金额") },
                    isError = amountError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = androidx.compose.ui.text.input.ImeAction.Next
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }
                    ),
                    singleLine = true,
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (amountText.isNotEmpty()) {
                                androidx.compose.material3.IconButton(
                                    onClick = { amountText = "" }
                                ) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "清空",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            TextButton(onClick = { showCalculator = true }) {
                                Text("计算")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                )

                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("备注（可选）") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = androidx.compose.ui.text.input.ImeAction.Next
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .focusRequester(amountFocus)
                )
                if (amount != null && amount >= 1000) {
                    Text(
                        text = "¥${com.autobook.app.util.formatAmountGrouped(amount)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 12.dp, top = 4.dp)
                    )
                }
                val merchantSuggest = if (merchant.isBlank()) {
                    topMerchants.take(8)
                } else {
                    topMerchants.filter { it.contains(merchant, true) && it != merchant }.take(8)
                }
                if (merchantSuggest.isNotEmpty()) {
                    Text(
                        text = "历史商户",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    FlowRow(modifier = Modifier.fillMaxWidth()) {
                        merchantSuggest.forEach { name ->
                            FilterChip(
                                selected = false,
                                onClick = { merchant = name },
                                label = { Text(name, fontSize = 11.sp) },
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "账本",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
                FlowRow(modifier = Modifier.fillMaxWidth()) {
                    books.forEach { b ->
                        FilterChip(
                            selected = book == b,
                            onClick = { book = b },
                            label = { Text(b, fontSize = 11.sp) },
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                }

                if (tx != null) {
                    Spacer(Modifier.height(12.dp))
                    androidx.compose.material3.HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            androidx.compose.material.icons.Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "时间：${com.autobook.app.util.formatTime(editTimestamp)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { showTimePicker = true }) { Text("修改") }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            androidx.compose.material.icons.Icons.Outlined.Hub,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "来源：${tx.source.ifEmpty { "导入" }} · 账本：${tx.book}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (tx.refNo.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                androidx.compose.material.icons.Icons.Outlined.Numbers,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "流水号：${tx.refNo}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (type == Transaction.TYPE_EXPENSE) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "选择分类",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val orderMap = categoryOrder.withIndex().associate { it.value to it.index }
                        val orderedGroups = Categories.expenseGroups.sortedBy {
                            orderMap[it.name] ?: Int.MAX_VALUE
                        }
                        orderedGroups.forEach { group ->
                            val selected = category == group.name
                            val interactionSource = remember {
                                androidx.compose.foundation.interaction.MutableInteractionSource()
                            }
                            val pressed by interactionSource.collectIsPressedAsState()
                            val bounce = remember {
                                androidx.compose.animation.core.Animatable(1f)
                            }
                            androidx.compose.runtime.LaunchedEffect(selected) {
                                if (selected) {
                                    bounce.snapTo(0.82f)
                                    bounce.animateTo(
                                        1f,
                                        androidx.compose.animation.core.spring(
                                            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                                            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                                        )
                                    )
                                }
                            }
                            Column(
                                modifier = Modifier
                                    .width(60.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .then(
                                        if (selected) {
                                            Modifier.border(
                                                1.5.dp,
                                                MaterialTheme.colorScheme.primary,
                                                RoundedCornerShape(12.dp)
                                            )
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .graphicsLayer {
                                        val scale = bounce.value * (if (pressed) 0.95f else 1f)
                                        scaleX = scale
                                        scaleY = scale
                                    }
                                    .clickable(
                                        interactionSource = interactionSource,
                                        indication = LocalIndication.current
                                    ) {
                                        category = group.name
                                        subCategory = null
                                    }
                                    .padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = CategoryUi.iconOf(group.name),
                                    contentDescription = group.name,
                                    tint = if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = group.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "选择子分类",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                }

                if (children.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterChip(
                            selected = subCategory == null,
                            onClick = { subCategory = null },
                            label = { Text("不细分") },
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        children.forEach { child ->
                            FilterChip(
                                selected = subCategory == child,
                                onClick = { subCategory = child },
                                label = { Text(child) },
                                modifier = Modifier.padding(end = 6.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row {
                if (onConfirmAgain != null && tx == null) {
                    TextButton(
                        onClick = {
                            if (amount == null || amount <= 0 || amount > Transaction.MAX_AMOUNT) {
                                amountError = true
                            } else {
                                val fullCategory = subCategory?.let { "$category/$it" } ?: category
                                onConfirmAgain(amount, type, fullCategory, merchant, tags, editTimestamp, book)
                                amountText = ""
                                amountError = false
                                editTimestamp = System.currentTimeMillis()
                                dialogScope.launch { amountFocus.requestFocus() }
                            }
                        }
                    ) { Text("再记一笔") }
                }
                TextButton(
                    onClick = {
                        if (amount == null || amount <= 0 || amount > Transaction.MAX_AMOUNT) {
                            amountError = true
                        } else {
                            val fullCategory = subCategory?.let { "$category/$it" } ?: category
                            onConfirm(amount, type, fullCategory, merchant, tags, editTimestamp, book)
                        }
                    }
                ) { Text("保存") }
            }
        },
        dismissButton = {
            if (onDelete != null) {
                TextButton(onClick = onDelete) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            } else {
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        }
    )

    if (showCalculator) {
        CalculatorDialog(
            onResult = { amountText = it },
            onDismiss = { showCalculator = false }
        )
    }

    if (showTimePicker) {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = editTimestamp }
        var pickYear by remember { mutableIntStateOf(cal.get(java.util.Calendar.YEAR)) }
        var pickMonth by remember { mutableIntStateOf(cal.get(java.util.Calendar.MONTH) + 1) }
        var pickDay by remember { mutableIntStateOf(cal.get(java.util.Calendar.DAY_OF_MONTH)) }
        var pickHour by remember { mutableIntStateOf(cal.get(java.util.Calendar.HOUR_OF_DAY)) }
        var pickMinute by remember { mutableIntStateOf(cal.get(java.util.Calendar.MINUTE)) }
        AlertDialog(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            onDismissRequest = { showTimePicker = false },
            title = { Text("修改时间") },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = {
                                val c = java.util.Calendar.getInstance()
                                pickYear = c.get(java.util.Calendar.YEAR)
                                pickMonth = c.get(java.util.Calendar.MONTH) + 1
                                pickDay = c.get(java.util.Calendar.DAY_OF_MONTH)
                                pickHour = c.get(java.util.Calendar.HOUR_OF_DAY)
                                pickMinute = c.get(java.util.Calendar.MINUTE)
                            }
                        ) { Text("今天", fontSize = 11.sp) }
                        TextButton(
                            onClick = {
                                val c = java.util.Calendar.getInstance().apply {
                                    add(java.util.Calendar.DAY_OF_YEAR, -1)
                                }
                                pickYear = c.get(java.util.Calendar.YEAR)
                                pickMonth = c.get(java.util.Calendar.MONTH) + 1
                                pickDay = c.get(java.util.Calendar.DAY_OF_MONTH)
                            }
                        ) { Text("昨天", fontSize = 11.sp) }
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = "${pickYear}年${pickMonth}月",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        TextButton(
                            onClick = {
                                if (pickMonth > 1) pickMonth-- else {
                                    pickMonth = 12
                                    pickYear = (pickYear - 1).coerceAtLeast(2019)
                                }
                            }
                        ) { Text("◀", fontSize = 11.sp) }
                        TextButton(
                            onClick = {
                                if (pickMonth < 12) pickMonth++ else {
                                    pickMonth = 1
                                    if (pickYear < 2100) pickYear++
                                }
                            }
                        ) { Text("▶", fontSize = 11.sp) }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth()) {
                        listOf("一", "二", "三", "四", "五", "六", "日").forEach { label ->
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    val firstDayCal = java.util.Calendar.getInstance().apply {
                        clear()
                        set(pickYear, pickMonth - 1, 1)
                    }
                    val firstDow = firstDayCal.get(java.util.Calendar.DAY_OF_WEEK)
                    val leading = (firstDow + 5) % 7
                    val daysInMonth = firstDayCal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
                    for (week in 0..5) {
                        Row(Modifier.fillMaxWidth()) {
                            for (dow in 0..6) {
                                val dayNum = week * 7 + dow - leading + 1
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(2.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (dayNum == pickDay && dayNum in 1..daysInMonth) {
                                                MaterialTheme.colorScheme.primaryContainer
                                            } else {
                                                androidx.compose.ui.graphics.Color.Transparent
                                            }
                                        )
                                        .clickable(enabled = dayNum in 1..daysInMonth) {
                                            pickDay = dayNum
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (dayNum in 1..daysInMonth) {
                                        Text(
                                            text = dayNum.toString(),
                                            fontSize = 12.sp,
                                            fontWeight = if (dayNum == pickDay) {
                                                FontWeight.Bold
                                            } else {
                                                FontWeight.Normal
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "时间",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.weight(1f))
                        listOf(pickHour to "时", pickMinute to "分").forEach { (value, label) ->
                            Text(
                                text = String.format(java.util.Locale.CHINA, "%02d", value),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        if (label == "时") {
                                            pickHour = if (pickHour > 0) pickHour - 1 else 23
                                        } else {
                                            pickMinute = if (pickMinute > 0) pickMinute - 1 else 59
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                        TextButton(
                            onClick = { pickHour = if (pickHour < 23) pickHour + 1 else 0 }
                        ) { Text("时+", fontSize = 11.sp) }
                        TextButton(
                            onClick = { pickMinute = if (pickMinute < 59) pickMinute + 1 else 0 }
                        ) { Text("分+", fontSize = 11.sp) }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newCal = java.util.Calendar.getInstance().apply {
                            isLenient = false
                            clear()
                            try {
                                set(pickYear, pickMonth - 1, pickDay, pickHour, pickMinute, 0)
                            } catch (_: Exception) {
                                timeInMillis = editTimestamp
                            }
                        }
                        editTimestamp = newCal.timeInMillis
                        showTimePicker = false
                    }
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("取消") }
            }
        )
    }
}
