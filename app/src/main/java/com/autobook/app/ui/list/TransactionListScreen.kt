package com.autobook.app.ui.list

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.NoteAdd
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autobook.app.data.Transaction
import com.autobook.app.data.expenseSum
import com.autobook.app.data.incomeSum
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.autobook.app.ui.MonthPickerLabel
import com.autobook.app.ui.SwipeableTransactionRow
import com.autobook.app.ui.TransactionDialog
import com.autobook.app.ui.theme.ExpenseRed
import com.autobook.app.util.TimeUtils
import com.autobook.app.util.formatAmount
import com.autobook.app.util.formatAmountGrouped
import com.autobook.app.viewmodel.MainViewModel

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)
@Composable
fun TransactionListScreen(modifier: Modifier = Modifier, scrollTopTick: Int = 0) {
    val viewModel: MainViewModel = viewModel()
    val monthTxs by viewModel.monthTransactions.collectAsStateWithLifecycle()
    val allTxs by viewModel.allTransactions.collectAsStateWithLifecycle()
    val currentMonth by viewModel.currentMonth.collectAsStateWithLifecycle()
    val currentBook by viewModel.currentBook.collectAsStateWithLifecycle()
    val hideAmount by viewModel.hideAmount.collectAsStateWithLifecycle()
    var editTarget by remember { mutableStateOf<Transaction?>(null) }
    var longPressTarget by remember { mutableStateOf<Transaction?>(null) }
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    var showAddDialog by remember { mutableStateOf(false) }
    var pendingEditId by remember { mutableStateOf(-1L) }
    var searchText by remember { mutableStateOf("") }
    var showFilter by remember { mutableStateOf(false) }
    var showBookPicker by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Transaction?>(null) }
    var deletedTx by remember { mutableStateOf<Transaction?>(null) }
    var starOnly by remember { mutableStateOf(false) }
    var searchFocused by remember { mutableStateOf(false) }
    val listContext = androidx.compose.ui.platform.LocalContext.current
    val listFocusManager = androidx.compose.ui.platform.LocalFocusManager.current
    var starredIds by remember {
        mutableStateOf(com.autobook.app.util.AppSettings.getStarredIds(listContext))
    }
    var searchHistory by remember {
        mutableStateOf(com.autobook.app.util.AppSettings.getSearchHistory(listContext))
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        com.autobook.app.MainActivity.quickAddRequests.collect { tick ->
            if (tick > 0) showAddDialog = true
        }
    }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        com.autobook.app.MainActivity.editRequests.collect { id ->
            if (id > 0) {
                pendingEditId = id
                com.autobook.app.MainActivity.editRequests.value = -1L
            }
        }
    }
    androidx.compose.runtime.LaunchedEffect(allTxs, pendingEditId) {
        val id = pendingEditId
        if (id > 0) {
            allTxs.firstOrNull { it.id == id }?.let {
                pendingEditId = -1L
                editTarget = it
            }
        }
    }
    var filterCategory by remember { mutableStateOf<String?>(null) }
    var filterMin by remember { mutableStateOf("") }
    var filterMax by remember { mutableStateOf("") }
    var filterFromMonth by remember { mutableStateOf<java.time.YearMonth?>(null) }
    var filterToMonth by remember { mutableStateOf<java.time.YearMonth?>(null) }
    var sortMode by remember { mutableIntStateOf(0) }
    var switchToBottom by remember { mutableStateOf(false) }
    var saveSuccessTick by remember { mutableIntStateOf(0) }

    val expense = remember(monthTxs) { monthTxs.expenseSum() }
    val income = remember(monthTxs) { monthTxs.incomeSum() }
    val balance = income - expense

    val hasFilter = filterCategory != null ||
        filterMin.toDoubleOrNull() != null ||
        filterMax.toDoubleOrNull() != null ||
        filterFromMonth != null || filterToMonth != null ||
        starOnly
    val filterCount = listOf(
        filterCategory != null,
        filterMin.toDoubleOrNull() != null,
        filterMax.toDoubleOrNull() != null,
        filterFromMonth != null,
        filterToMonth != null,
        starOnly
    ).count { it }
    val searching = searchText.isNotBlank() || hasFilter
    val searchResults = if (searching) {
        remember(
            allTxs, searchText, filterCategory, filterMin, filterMax,
            filterFromMonth, filterToMonth, sortMode, starOnly, starredIds
        ) {
            val min = filterMin.toDoubleOrNull()
            val max = filterMax.toDoubleOrNull()
            allTxs.filter { tx ->
                val textMatch = searchText.isBlank() ||
                    tx.merchant.contains(searchText, true) ||
                    tx.category.contains(searchText, true)
                val catMatch = filterCategory == null ||
                    com.autobook.app.data.Categories.primaryOf(tx.category) == filterCategory
                val minMatch = min == null || tx.amount >= min
                val maxMatch = max == null || tx.amount <= max
                val fromMatch = filterFromMonth == null || tx.timestamp >= TimeUtils.monthRange(
                    filterFromMonth!!.year, filterFromMonth!!.monthValue - 1
                ).first
                val toMatch = filterToMonth == null || tx.timestamp < TimeUtils.monthRange(
                    filterToMonth!!.year, filterToMonth!!.monthValue - 1
                ).second
                val starMatch = !starOnly || tx.id in starredIds
                textMatch && catMatch && minMatch && maxMatch && fromMatch && toMatch && starMatch
            }.let { list ->
                when (sortMode) {
                    1 -> list.sortedBy { it.timestamp }
                    2 -> list.sortedByDescending { it.amount }
                    else -> list.sortedByDescending { it.timestamp }
                }
            }
        }
    } else {
        emptyList()
    }

    val listState = rememberSaveable(saver = androidx.compose.foundation.lazy.LazyListState.Saver) {
        androidx.compose.foundation.lazy.LazyListState()
    }
    val groupedMonthTxs = remember(monthTxs, sortMode) {
        when (sortMode) {
            1 -> monthTxs.sortedBy { it.timestamp }.groupBy { TimeUtils.dayStartOf(it.timestamp) }
            else -> monthTxs.groupBy { TimeUtils.dayStartOf(it.timestamp) }
        }
    }
    val amountSortedMonthTxs = remember(monthTxs) {
        monthTxs.sortedByDescending { it.amount }
    }
    val allUsedTags = remember(allTxs) {
        allTxs.flatMap { it.tags.split(Regex("\\s+")) }
            .filter { it.isNotBlank() }
            .groupingBy { it }.eachCount()
            .entries.sortedByDescending { it.value }.map { it.key }
            .take(15)
    }

    val listAlpha = remember { androidx.compose.animation.core.Animatable(1f) }
    val listSlideX = remember { androidx.compose.animation.core.Animatable(0f) }
    var lastMonthSeen by remember { mutableStateOf(currentMonth) }
    var lastMonthSwitchAt by remember { mutableStateOf(0L) }
    androidx.compose.runtime.LaunchedEffect(currentMonth, currentBook) {
        val dir = when {
            currentMonth.isAfter(lastMonthSeen) -> 1f
            currentMonth.isBefore(lastMonthSeen) -> -1f
            else -> 0f
        }
        lastMonthSeen = currentMonth
        lastMonthSwitchAt = System.currentTimeMillis()
        listAlpha.snapTo(0f)
        listSlideX.snapTo(dir * 90f)
        // 等新月份数据就绪，期间完全隐藏，避免先月末后月初的闪现
        val range = com.autobook.app.util.TimeUtils.monthRange(
            currentMonth.year, currentMonth.monthValue - 1
        )
        kotlinx.coroutines.withTimeoutOrNull(2000L) {
            androidx.compose.runtime.snapshotFlow { monthTxs }.first { txs ->
                txs.isEmpty() || txs.all { it.timestamp in range.first until range.second }
            }
        }
        kotlinx.coroutines.delay(80)
        if (switchToBottom) {
            switchToBottom = false
            val count = listState.layoutInfo.totalItemsCount
            if (count > 1) {
                listState.scrollToItem(count - 1)
                listState.scroll {
                    scrollBy(100000f)
                }
            }
        } else {
            listState.scrollToItem(0)
        }
        launch {
            listAlpha.animateTo(1f, androidx.compose.animation.core.tween(240))
        }
        listSlideX.animateTo(
            0f,
            androidx.compose.animation.core.spring(
                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
            )
        )
    }
    androidx.compose.runtime.LaunchedEffect(monthTxs) {
        if (System.currentTimeMillis() - lastMonthSwitchAt < 600) return@LaunchedEffect
        if (switchToBottom) {
            switchToBottom = false
            kotlinx.coroutines.delay(100)
            val count = listState.layoutInfo.totalItemsCount
            if (count > 1) {
                listState.scrollToItem(count - 1)
                listState.scroll {
                    scrollBy(100000f)
                }
            }
        } else {
            listState.scrollToItem(0)
        }
    }
    androidx.compose.runtime.LaunchedEffect(scrollTopTick) {
        if (scrollTopTick > 0) {
            listState.animateScrollToItem(0)
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0.dp),
        floatingActionButton = {
            val fabInteractionSource = remember {
                androidx.compose.foundation.interaction.MutableInteractionSource()
            }
            val fabPressed by fabInteractionSource.collectIsPressedAsState()
            val fabAtTop by remember {
                androidx.compose.runtime.derivedStateOf {
                    listState.firstVisibleItemIndex <= 1 &&
                        listState.firstVisibleItemScrollOffset < 200
                }
            }
            val fabAnim = remember { androidx.compose.animation.core.Animatable(1f) }
            androidx.compose.runtime.LaunchedEffect(fabAtTop) {
                fabAnim.animateTo(
                    if (fabAtTop) 1f else 0f,
                    androidx.compose.animation.core.tween(180)
                )
            }
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .graphicsLayer {
                        val scale = (if (fabPressed) 0.92f else 1f) * fabAnim.value
                        scaleX = scale
                        scaleY = scale
                        alpha = fabAnim.value
                    }
                    .shadow(
                        elevation = 8.dp,
                        shape = CircleShape,
                        ambientColor = Color.Black.copy(alpha = 0.25f),
                        spotColor = Color.Black.copy(alpha = 0.25f)
                    )
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                com.autobook.app.ui.theme.darker(
                                    MaterialTheme.colorScheme.primary, 0.85f
                                )
                            )
                        )
                    )
                    .clickable(
                        interactionSource = fabInteractionSource,
                        indication = androidx.compose.foundation.LocalIndication.current
                    ) { showAddDialog = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "手动记账",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = listAlpha.value
                        translationX = listSlideX.value.dp.toPx()
                    },
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                item {
                    TopSummaryRow(
                        yearMonth = currentMonth,
                        balance = balance,
                        expense = expense,
                        income = income,
                        onSelectMonth = { ym ->
                            switchToBottom = false
                            viewModel.setMonth(ym)
                        },
                        hideAmount = hideAmount
                    )
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { showBookPicker = true }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.MenuBook,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = currentBook,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.width(3.dp))
                            Icon(
                                Icons.Filled.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        OutlinedTextField(
                            value = searchText,
                            onValueChange = { searchText = it },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Search,
                                    contentDescription = "搜索",
                                    modifier = Modifier.size(15.dp)
                                )
                            },
                            trailingIcon = {
                                if (searchText.isNotEmpty()) {
                                    androidx.compose.material3.IconButton(
                                        onClick = { searchText = "" },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.Close,
                                            contentDescription = "清空搜索",
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            shape = RoundedCornerShape(14.dp),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                imeAction = androidx.compose.ui.text.input.ImeAction.Search
                            ),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                onSearch = { listFocusManager.clearFocus() }
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .onFocusChanged { state ->
                                    searchFocused = state.isFocused
                                    if (state.isFocused) {
                                        searchHistory =
                                            com.autobook.app.util.AppSettings.getSearchHistory(
                                                listContext
                                            )
                                    } else if (searchText.isNotBlank()) {
                                        com.autobook.app.util.AppSettings.addSearchHistory(
                                            listContext, searchText
                                        )
                                    }
                                }
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .height(40.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (hasFilter) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        Color.Transparent
                                    }
                                )
                                .clickable {
                                    if (hasFilter) {
                                        filterCategory = null
                                        filterMin = ""
                                        filterMax = ""
                                        filterFromMonth = null
                                        filterToMonth = null
                                        starOnly = false
                                    } else {
                                        showFilter = true
                                    }
                                }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (hasFilter) "筛选($filterCount)" else "筛选",
                                fontSize = 12.sp,
                                color = if (hasFilter) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        TextButton(
                            onClick = { sortMode = (sortMode + 1) % 3 },
                            modifier = Modifier.height(40.dp)
                        ) {
                            Icon(
                                imageVector = if (sortMode == 2) {
                                    Icons.Outlined.Payments
                                } else {
                                    Icons.Outlined.Schedule
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                when (sortMode) {
                                    0 -> "时间↓"
                                    1 -> "时间↑"
                                    else -> "金额↓"
                                },
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                if (searchFocused && searchHistory.isNotEmpty()) {
                    val candidates = if (searchText.isBlank()) {
                        searchHistory
                    } else {
                        searchHistory.filter { it.contains(searchText, true) }
                    }
                    if (candidates.isNotEmpty()) {
                        item {
                            androidx.compose.foundation.layout.FlowRow(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                            ) {
                                candidates.forEach { word ->
                                    FilterChip(
                                        selected = false,
                                        onClick = { searchText = word },
                                        label = { Text(word, fontSize = 11.sp) },
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                }
                                if (searchText.isBlank()) {
                                    TextButton(
                                        onClick = {
                                            com.autobook.app.util.AppSettings.clearSearchHistory(listContext)
                                            searchHistory = emptyList()
                                        }
                                    ) { Text("清空", fontSize = 11.sp) }
                                }
                            }
                        }
                    }
                }

                if (hasFilter) {
                    item {
                        androidx.compose.foundation.layout.FlowRow(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                        ) {
                            filterCategory?.let { cat ->
                                FilterChip(
                                    selected = true,
                                    onClick = { filterCategory = null },
                                    label = { Text("分类：$cat ✕", fontSize = 11.sp) },
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                            }
                            filterMin.toDoubleOrNull()?.let { min ->
                                FilterChip(
                                    selected = true,
                                    onClick = { filterMin = "" },
                                    label = { Text("≥ ¥$min ✕", fontSize = 11.sp) },
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                            }
                            filterMax.toDoubleOrNull()?.let { max ->
                                FilterChip(
                                    selected = true,
                                    onClick = { filterMax = "" },
                                    label = { Text("≤ ¥$max ✕", fontSize = 11.sp) },
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                            }
                            filterFromMonth?.let { ym ->
                                FilterChip(
                                    selected = true,
                                    onClick = { filterFromMonth = null },
                                    label = {
                                        Text(
                                            "从 ${ym.year}年${ym.monthValue}月 ✕",
                                            fontSize = 11.sp
                                        )
                                    },
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                            }
                            filterToMonth?.let { ym ->
                                FilterChip(
                                    selected = true,
                                    onClick = { filterToMonth = null },
                                    label = {
                                        Text(
                                            "到 ${ym.year}年${ym.monthValue}月 ✕",
                                            fontSize = 11.sp
                                        )
                                    },
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                            }
                            if (starOnly) {
                                FilterChip(
                                    selected = true,
                                    onClick = { starOnly = false },
                                    label = { Text("只看星标 ✕", fontSize = 11.sp) },
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                            }
                            TextButton(
                                onClick = {
                                    filterCategory = null
                                    filterMin = ""
                                    filterMax = ""
                                    filterFromMonth = null
                                    filterToMonth = null
                                    starOnly = false
                                }
                            ) { Text("全部清除", fontSize = 11.sp) }
                        }
                    }
                }

                if (searching) {
                    item {
                        Text(
                            text = "找到 ${searchResults.size} 笔记录",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                        )
                    }
                    if (searchResults.isEmpty()) {
                        item {
                            com.autobook.app.ui.EmptyHint(
                                icon = Icons.Outlined.SearchOff,
                                text = "没有找到相关记录"
                            )
                        }
                    } else {
                        items(searchResults, key = { "s_${it.id}" }) { tx ->
                            SwipeableTransactionRow(
                                tx, hideAmount = hideAmount,
                                starred = tx.id in starredIds,
                                onClick = { editTarget = tx },
                                onLongClick = { longPressTarget = tx },
                                onEdit = { editTarget = tx },
                                onDelete = { deleteTarget = tx },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                } else if (monthTxs.isEmpty()) {
                    item {
                        com.autobook.app.ui.EmptyHint(
                            icon = Icons.Outlined.ReceiptLong,
                            text = "这个月还没有记录",
                            subText = "点右下角按钮，记下第一笔"
                        )
                    }
                } else if (sortMode == 2) {
                    items(amountSortedMonthTxs, key = { it.id }) { tx ->
                        SwipeableTransactionRow(
                            tx, hideAmount = hideAmount,
                                starred = tx.id in starredIds,
                            onClick = { editTarget = tx },
                            onLongClick = { longPressTarget = tx },
                            onEdit = { editTarget = tx },
                            onDelete = { deleteTarget = tx },
                            modifier = Modifier.animateItem()
                        )
                    }
                } else {
                    groupedMonthTxs.entries.forEachIndexed { dayIndex, entry ->
                        val (day, txs) = entry
                        stickyHeader(key = "header_$day") {
                            val dayCal = java.util.Calendar.getInstance().apply {
                                timeInMillis = day
                            }
                            val isWeekend =
                                dayCal.get(java.util.Calendar.DAY_OF_WEEK) ==
                                    java.util.Calendar.SATURDAY ||
                                    dayCal.get(java.util.Calendar.DAY_OF_WEEK) ==
                                    java.util.Calendar.SUNDAY
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.background)
                                    .padding(vertical = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = com.autobook.app.util.formatDayWithWeek(day),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isWeekend) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = dayIndex == 0 &&
                                        !listState.canScrollBackward &&
                                        currentMonth.isBefore(java.time.YearMonth.now()),
                                    enter = androidx.compose.animation.fadeIn(
                                        animationSpec = androidx.compose.animation.core.tween(220)
                                    ),
                                    exit = androidx.compose.animation.fadeOut(
                                        animationSpec = androidx.compose.animation.core.tween(150)
                                    ),
                                    modifier = Modifier.align(Alignment.Center)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(
                                                MaterialTheme.colorScheme.primary.copy(
                                                    alpha = 0.12f
                                                )
                                            )
                                            .clickable {
                                                switchToBottom = true
                                                viewModel.nextMonth()
                                            }
                                            .padding(horizontal = 14.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "下月",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Icon(
                                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }
                        }
                        items(txs, key = { it.id }) { tx ->
                            SwipeableTransactionRow(
                                tx, hideAmount = hideAmount,
                                starred = tx.id in starredIds,
                                onClick = { editTarget = tx },
                                onLongClick = { longPressTarget = tx },
                                onEdit = { editTarget = tx },
                                onDelete = { deleteTarget = tx },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                    item(key = "month_prev_btn") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp, bottom = 8.dp)
                                .animateItem(),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    )
                                    .clickable {
                                        switchToBottom = false
                                        viewModel.previousMonth()
                                    }
                                    .padding(horizontal = 14.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "上月",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
            if (!searching) {
                val scrollProgress by remember {
                    androidx.compose.runtime.derivedStateOf {
                        val total = listState.layoutInfo.totalItemsCount
                        if (total <= 1) {
                            0f
                        } else {
                            listState.firstVisibleItemIndex.toFloat() / (total - 1)
                        }
                    }
                }
                com.autobook.app.ui.FlatProgressBar(
                    progress = scrollProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .align(Alignment.TopCenter),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                    trackColor = Color.Transparent,
                    height = 2.dp
                )
            }
            val edgeColor = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
                Color.White.copy(alpha = 0.08f)
            } else {
                Color.Black.copy(alpha = 0.06f)
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = listState.canScrollBackward,
                enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(220)),
                exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(220)),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(edgeColor, Color.Transparent)
                            )
                        )
                )
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = listState.canScrollForward,
                enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(220)),
                exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(220)),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, edgeColor)
                            )
                        )
                )
            }
            SaveSuccessOverlay(
                tick = saveSuccessTick,
                modifier = Modifier.align(Alignment.Center)
            )
            UndoBar(
                tx = deletedTx,
                onUndo = {
                    deletedTx?.let { tx ->
                        viewModel.addManual(
                            tx.amount, tx.type, tx.category,
                            tx.merchant, tx.tags, tx.timestamp
                        )
                    }
                    deletedTx = null
                },
                onTimeout = { deletedTx = null },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 88.dp)
            )
        }
    }

    if (showAddDialog) {
        TransactionDialog(
            tx = null,
            onDismiss = { showAddDialog = false },
            onConfirm = { amount, type, category, merchant, tags, timestamp, book ->
                viewModel.addManual(amount, type, category, merchant, tags, timestamp, book)
                showAddDialog = false
                saveSuccessTick++
            },
            onConfirmAgain = { amount, type, category, merchant, tags, timestamp, book ->
                viewModel.addManual(amount, type, category, merchant, tags, timestamp, book)
                saveSuccessTick++
            }
        )
    }

    if (showBookPicker) {
        androidx.compose.material3.AlertDialog(
            shape = RoundedCornerShape(24.dp),
            onDismissRequest = { showBookPicker = false },
            title = { Text("选择账本") },
            text = {
                Column {
                    com.autobook.app.util.AppSettings.getBooks(listContext).forEach { book ->
                        val isCurrent = book == currentBook
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isCurrent) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        Color.Transparent
                                    }
                                )
                                .clickable {
                                    viewModel.setCurrentBook(book)
                                    showBookPicker = false
                                }
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.MenuBook,
                                contentDescription = null,
                                tint = if (isCurrent) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = book,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )
                            if (isCurrent) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBookPicker = false }) { Text("关闭") }
            }
        )
    }

    deleteTarget?.let { target ->
        androidx.compose.material3.AlertDialog(
            shape = RoundedCornerShape(24.dp),
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除记录") },
            text = {
                Column {
                    Text(
                        text = "「${target.merchant}」 · ${com.autobook.app.util.formatCategory(target.category)} · ${com.autobook.app.util.formatTime(target.timestamp)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "金额 ${com.autobook.app.util.formatAmount(target.amount)}元，删除后无法恢复。"
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.delete(target)
                        deleteTarget = null
                        deletedTx = target
                    }
                ) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("取消") }
            }
        )
    }

    if (showFilter) {
        androidx.compose.material3.AlertDialog(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            onDismissRequest = { showFilter = false },
            title = { Text("筛选") },
            text = {
                Column {
                    androidx.compose.foundation.layout.FlowRow {
                        FilterChip(
                            selected = filterCategory == null,
                            onClick = { filterCategory = null },
                            label = { Text("全部分类") },
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        com.autobook.app.data.Categories.expenseGroups.forEach { group ->
                            FilterChip(
                                selected = filterCategory == group.name,
                                onClick = { filterCategory = group.name },
                                label = { Text(group.name) },
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                        FilterChip(
                            selected = starOnly,
                            onClick = { starOnly = !starOnly },
                            label = { Text("只看星标") },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                    androidx.compose.material3.HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "从",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(6.dp))
                        MonthPickerLabel(
                            yearMonth = filterFromMonth
                                ?: java.time.YearMonth.now().minusMonths(12),
                            onSelect = { filterFromMonth = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "到",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(6.dp))
                        MonthPickerLabel(
                            yearMonth = filterToMonth ?: java.time.YearMonth.now(),
                            onSelect = { filterToMonth = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    androidx.compose.material3.HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                    androidx.compose.material3.HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = filterMin,
                            onValueChange = {
                                filterMin = com.autobook.app.util.filterAmountInput(it)
                            },
                            label = { Text("最低金额") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(
                            value = filterMax,
                            onValueChange = {
                                filterMax = com.autobook.app.util.filterAmountInput(it)
                            },
                            label = { Text("最高金额") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFilter = false }) { Text("确定") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        filterCategory = null
                        filterMin = ""
                        filterMax = ""
                        filterFromMonth = null
                        filterToMonth = null
                        showFilter = false
                    }
                ) { Text("清除筛选") }
            }
        )
    }

    longPressTarget?.let { tx ->
        androidx.compose.material3.AlertDialog(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            onDismissRequest = { longPressTarget = null },
            title = { Text(tx.merchant) },
            text = {
                Text(
                    "${com.autobook.app.util.formatCategory(tx.category)} " +
                        "${if (tx.type == Transaction.TYPE_EXPENSE) "-" else "+"}" +
                        "${com.autobook.app.util.formatAmount(tx.amount)}"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        clipboard.setText(
                            androidx.compose.ui.text.AnnotatedString(
                                "${com.autobook.app.util.formatDay(tx.timestamp)} " +
                                    "${com.autobook.app.util.formatCategory(tx.category)} " +
                                    "${if (tx.type == Transaction.TYPE_EXPENSE) "-" else "+"}" +
                                    "${com.autobook.app.util.formatAmount(tx.amount)} ${tx.merchant}"
                            )
                        )
                        longPressTarget = null
                    }
                ) {
                    Icon(
                        Icons.Outlined.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("复制内容")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            viewModel.addManual(
                                tx.amount, tx.type, tx.category,
                                tx.merchant, tx.tags, System.currentTimeMillis(),
                                tx.book
                            )
                            longPressTarget = null
                            saveSuccessTick++
                        }
                    ) {
                        Icon(
                            Icons.Outlined.NoteAdd,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("复制账目")
                    }
                    TextButton(
                        onClick = {
                            com.autobook.app.util.AppSettings.toggleStar(listContext, tx.id)
                            starredIds = com.autobook.app.util.AppSettings.getStarredIds(listContext)
                            longPressTarget = null
                        }
                    ) {
                        Icon(
                            imageVector = if (tx.id in starredIds) {
                                Icons.Filled.Star
                            } else {
                                Icons.Outlined.StarBorder
                            },
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(if (tx.id in starredIds) "取消标星" else "标星")
                    }
                    TextButton(
                        onClick = {
                            val text =
                                "${com.autobook.app.util.formatDay(tx.timestamp)} " +
                                    "${com.autobook.app.util.formatCategory(tx.category)} " +
                                    "${if (tx.type == Transaction.TYPE_EXPENSE) "-" else "+"}" +
                                    "${com.autobook.app.util.formatAmount(tx.amount)} ${tx.merchant}"
                            val sendIntent =
                                android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(android.content.Intent.EXTRA_TEXT, text)
                                }
                            listContext.startActivity(
                                android.content.Intent.createChooser(sendIntent, "分享这笔记录")
                            )
                            longPressTarget = null
                        }
                    ) {
                        Icon(
                            Icons.Outlined.Share,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("分享")
                    }
                    TextButton(
                        onClick = {
                            longPressTarget = null
                            editTarget = tx
                        }
                    ) {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("编辑")
                    }
                }
            }
        )
    }

    editTarget?.let { tx ->
        TransactionDialog(
            tx = tx,
            onDismiss = { editTarget = null },
            onConfirm = { amount, type, category, merchant, tags, timestamp, book ->
                viewModel.update(tx, amount, type, category, merchant, tags, timestamp, book)
                editTarget = null
                saveSuccessTick++
            },
            onDelete = {
                viewModel.delete(tx)
                editTarget = null
                deletedTx = tx
            }
        )
    }
}

@Composable
private fun UndoBar(
    tx: Transaction?,
    onUndo: () -> Unit,
    onTimeout: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.runtime.LaunchedEffect(tx) {
        if (tx != null) {
            kotlinx.coroutines.delay(3000)
            onTimeout()
        }
    }
    androidx.compose.animation.AnimatedVisibility(
        visible = tx != null,
        enter = androidx.compose.animation.slideInVertically(
            animationSpec = androidx.compose.animation.core.tween(200)
        ) { it } + androidx.compose.animation.fadeIn(
            animationSpec = androidx.compose.animation.core.tween(200)
        ),
        exit = androidx.compose.animation.slideOutVertically(
            animationSpec = androidx.compose.animation.core.tween(200)
        ) { it } + androidx.compose.animation.fadeOut(
            animationSpec = androidx.compose.animation.core.tween(200)
        ),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(50),
                    ambientColor = Color.Black.copy(alpha = 0.2f),
                    spotColor = Color.Black.copy(alpha = 0.2f)
                )
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.onSurface)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "已删除「${tx?.merchant ?: ""}」",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.surface,
                maxLines = 1,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onUndo) {
                Text("撤销", fontSize = 12.sp, color = Color(0xFF4ADE80))
            }
        }
    }
}

@Composable
private fun SaveSuccessOverlay(tick: Int, modifier: Modifier = Modifier) {
    if (tick <= 0) return
    var visible by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(tick) {
        visible = true
        kotlinx.coroutines.delay(750)
        visible = false
    }
    val scale = remember { androidx.compose.animation.core.Animatable(0.3f) }
    val alpha = remember { androidx.compose.animation.core.Animatable(0f) }
    androidx.compose.runtime.LaunchedEffect(visible) {
        if (visible) {
            alpha.snapTo(0f)
            alpha.animateTo(1f, androidx.compose.animation.core.tween(100))
            scale.snapTo(0.3f)
            scale.animateTo(
                1f,
                androidx.compose.animation.core.spring(
                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                    stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                )
            )
        } else {
            alpha.animateTo(0f, androidx.compose.animation.core.tween(280))
        }
    }
    Box(
        modifier = modifier
            .size(84.dp)
            .graphicsLayer {
                this.alpha = alpha.value
                val s = scale.value
                scaleX = s
                scaleY = s
            }
            .shadow(
                elevation = 12.dp,
                shape = CircleShape,
                ambientColor = Color.Black.copy(alpha = 0.22f),
                spotColor = Color.Black.copy(alpha = 0.22f)
            )
            .clip(CircleShape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        com.autobook.app.ui.theme.darker(
                            MaterialTheme.colorScheme.primary, 0.85f
                        )
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Filled.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(42.dp)
        )
    }
}

@Composable
private fun TopSummaryRow(
    yearMonth: java.time.YearMonth,
    balance: Double,
    expense: Double,
    income: Double,
    onSelectMonth: (java.time.YearMonth) -> Unit,
    hideAmount: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MonthPickerLabel(
            yearMonth = yearMonth,
            onSelect = onSelectMonth,
            modifier = Modifier.weight(1.2f),
            twoLines = true
        )
        if (yearMonth != java.time.YearMonth.now()) {
            TextButton(
                onClick = { onSelectMonth(java.time.YearMonth.now()) }
            ) { Text("回本月", fontSize = 11.sp) }
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "结余",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val balanceFontSize = if (formatAmountGrouped(balance).length > 12) 16.sp else 20.sp
            androidx.compose.animation.AnimatedContent(
                targetState = if (hideAmount) "****" else formatAmountGrouped(balance),
                transitionSpec = {
                    (slideInVertically(
                        animationSpec = androidx.compose.animation.core.spring(
                            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                        )
                    ) { h -> h / 2 } +
                        fadeIn(animationSpec = androidx.compose.animation.core.tween(280))
                        togetherWith
                        slideOutVertically(
                            animationSpec = androidx.compose.animation.core.tween(200)
                        ) { h -> -h / 2 } +
                        fadeOut(animationSpec = androidx.compose.animation.core.tween(160)))
                },
                label = "balance_anim"
            ) { text ->
                Text(
                    text = if (hideAmount) {
                        androidx.compose.ui.text.AnnotatedString(text)
                    } else {
                        com.autobook.app.util.smallSymbolAmount(
                            text,
                            balanceFontSize,
                            if (balance >= 0) MaterialTheme.colorScheme.primary else ExpenseRed
                        )
                    },
                    fontSize = balanceFontSize,
                    fontWeight = FontWeight.Bold,
                    color = if (balance >= 0) MaterialTheme.colorScheme.primary else ExpenseRed,
                    maxLines = 1
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            androidx.compose.animation.AnimatedContent(
                targetState = if (hideAmount) "支出 ****" else formatAmountGrouped(expense),
                transitionSpec = {
                    (slideInVertically(
                        animationSpec = androidx.compose.animation.core.spring(
                            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                        )
                    ) { h -> h / 2 } +
                        fadeIn(animationSpec = androidx.compose.animation.core.tween(280))
                        togetherWith
                        slideOutVertically(
                            animationSpec = androidx.compose.animation.core.tween(200)
                        ) { h -> -h / 2 } +
                        fadeOut(animationSpec = androidx.compose.animation.core.tween(160)))
                },
                label = "expense_anim"
            ) { text ->
                Text(
                    text = if (hideAmount) {
                        androidx.compose.ui.text.AnnotatedString(text)
                    } else {
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = ExpenseRed)) { append("支出 ") }
                            withStyle(
                                SpanStyle(fontSize = 12.sp * 0.72f, color = ExpenseRed)
                            ) { append("¥") }
                            withStyle(SpanStyle(color = ExpenseRed)) { append(text) }
                        }
                    },
                    fontSize = 12.sp,
                    color = ExpenseRed
                )
            }
            Spacer(Modifier.height(2.dp))
            androidx.compose.animation.AnimatedContent(
                targetState = if (hideAmount) "收入 ****" else formatAmountGrouped(income),
                transitionSpec = {
                    (slideInVertically(
                        animationSpec = androidx.compose.animation.core.spring(
                            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                        )
                    ) { h -> h / 2 } +
                        fadeIn(animationSpec = androidx.compose.animation.core.tween(280))
                        togetherWith
                        slideOutVertically(
                            animationSpec = androidx.compose.animation.core.tween(200)
                        ) { h -> -h / 2 } +
                        fadeOut(animationSpec = androidx.compose.animation.core.tween(160)))
                },
                label = "income_anim"
            ) { text ->
                Text(
                    text = if (hideAmount) {
                        androidx.compose.ui.text.AnnotatedString(text)
                    } else {
                        buildAnnotatedString {
                            withStyle(
                                SpanStyle(color = com.autobook.app.ui.theme.IncomeBlue)
                            ) { append("收入 ") }
                            withStyle(
                                SpanStyle(
                                    fontSize = 12.sp * 0.72f,
                                    color = com.autobook.app.ui.theme.IncomeBlue
                                )
                            ) { append("¥") }
                            withStyle(
                                SpanStyle(color = com.autobook.app.ui.theme.IncomeBlue)
                            ) { append(text) }
                        }
                    },
                    fontSize = 12.sp,
                    color = com.autobook.app.ui.theme.IncomeBlue
                )
            }
        }
    }
}
