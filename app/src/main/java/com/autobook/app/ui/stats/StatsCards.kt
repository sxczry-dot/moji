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

// ===== 从 StatsScreen 拆出的卡片类组件（机械拆分，无行为变更）=====

@Composable
fun CategoryRankRow(
    category: String,
    amount: Double,
    total: Double,
    hideAmount: Boolean = false,
    statsTick: Int = 0,
    onClick: () -> Unit
) {
    val ratio = if (total > 0) (amount / total).toFloat() else 0f
    val rankAnim = remember {
        androidx.compose.animation.core.Animatable(0f)
    }
    androidx.compose.runtime.LaunchedEffect(statsTick, ratio) {
        rankAnim.snapTo(0f)
        rankAnim.animateTo(
            ratio,
            androidx.compose.animation.core.spring(
                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
            )
        )
    }
    val animatedRatio = rankAnim.value
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                val scale = if (pressed) 0.98f else 1f
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            )
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(CategoryUi.colorOf(category))
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = category,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            modifier = Modifier.widthIn(min = 48.dp, max = 72.dp)
        )
        com.autobook.app.ui.FlatProgressBar(
            progress = animatedRatio,
            modifier = Modifier.weight(1f),
            color = CategoryUi.colorOf(category)
        )
        Spacer(Modifier.width(10.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = if (hideAmount) "****" else formatAmountGrouped(amount),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "%.2f%%".format(java.util.Locale.CHINA, animatedRatio * 100),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ReportItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(13.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
    }
}


@Composable
fun MonthCompareCard(
    displayTxs: List<Transaction>,
    sixMonths: List<com.autobook.app.viewmodel.MainViewModel.MonthStat>,
    dataMode: Int,
    currentMonth: java.time.YearMonth,
    lastYearExpense: Double = 0.0,
    modifier: Modifier = Modifier,
    hideAmount: Boolean = false
) {
    if (dataMode != 0) return
    val thisMonth = displayTxs.expenseSum()
    val lastMonth = sixMonths.firstOrNull {
        it.yearMonth == currentMonth.minusMonths(1)
    }?.expense ?: 0.0
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
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
                    imageVector = Icons.Outlined.CompareArrows,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
            Text(
                text = "消费对比",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth()) {
                CompareColumn(
                    label = "较上月",
                    base = lastMonth,
                    current = thisMonth,
                    hideAmount = hideAmount,
                    flatText = "与上月持平",
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(12.dp))
                CompareColumn(
                    label = "较去年同月",
                    base = lastYearExpense,
                    current = thisMonth,
                    hideAmount = hideAmount,
                    flatText = "与去年同月持平",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun CompareColumn(
    label: String,
    base: Double,
    current: Double,
    hideAmount: Boolean,
    flatText: String,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        if (base > 0) {
            val diff = current - base
            val pct = diff / base * 100
            val tone = if (diff > 0) ExpenseRed else PrimaryGreen
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (diff != 0.0) {
                    Icon(
                        if (diff > 0) {
                            androidx.compose.material.icons.Icons.Outlined.TrendingUp
                        } else {
                            androidx.compose.material.icons.Icons.Outlined.TrendingDown
                        },
                        contentDescription = null,
                        tint = tone,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    text = if (hideAmount) "****" else {
                        val sign = if (diff > 0) "+¥" else if (diff < 0) "-¥" else "¥"
                        sign + com.autobook.app.util.formatAmount(kotlin.math.abs(diff))
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (diff != 0.0) tone else MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(2.dp))
            if (diff != 0.0) {
                Text(
                    text = "${if (diff > 0) "多花" else "少花"} · ${String.format(java.util.Locale.CHINA, "%+.1f", pct)}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = tone
                )
            } else {
                Text(
                    text = flatText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Text(
                text = "无记录",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun MerchantRankCard(
    displayTxs: List<Transaction>,
    modifier: Modifier = Modifier,
    hideAmount: Boolean = false,
    statsTick: Int = 0,
    onDrill: (String) -> Unit
) {
    val topMerchants by androidx.compose.runtime.produceState(
        initialValue = emptyList<Pair<String, Double>>(), displayTxs
    ) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            StatsCalculator.merchantRanking(displayTxs, 5)
        }
    }
    if (topMerchants.isEmpty()) return
    // 防止 0/0=NaN（coerceIn 拦不住 NaN），进度条宽度归零
    val maxAmount = topMerchants.first().second.takeIf { it > 0 } ?: 1.0
    val merchantAnim = remember(statsTick) {
        androidx.compose.animation.core.Animatable(0f)
    }
    androidx.compose.runtime.LaunchedEffect(statsTick) {
        merchantAnim.snapTo(0f)
        merchantAnim.animateTo(
            1f,
            androidx.compose.animation.core.spring(
                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
            )
        )
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
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
                    imageVector = Icons.Outlined.Leaderboard,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
            Text(
                text = "商户排行",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )            }
            Spacer(Modifier.height(10.dp))
            topMerchants.forEachIndexed { index, (name, amount) ->
                val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                val pressed by interactionSource.collectIsPressedAsState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            val scale = if (pressed) 0.98f else 1f
                            scaleX = scale
                            scaleY = scale
                        }
                        .clickable(
                            interactionSource = interactionSource,
                            indication = LocalIndication.current
                        ) { onDrill(name) }
                        .padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        modifier = Modifier.widthIn(min = 60.dp, max = 110.dp)
                    )
                    com.autobook.app.ui.FlatProgressBar(
                        progress = (amount / maxAmount).toFloat() * merchantAnim.value,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.primary.copy(
                            alpha = 1f - index * 0.15f
                        )
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = if (hideAmount) "****" else com.autobook.app.util.formatAmountGrouped(amount),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun BudgetRing(ratio: Float, over: Boolean, modifier: Modifier = Modifier) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val progressColor = if (over) ExpenseRed else MaterialTheme.colorScheme.primary
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val stroke = size.minDimension * 0.1f
        val inset = stroke / 2f
        val arcSize = androidx.compose.ui.geometry.Size(
            size.width - stroke, size.height - stroke
        )
        val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
        drawArc(
            color = trackColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = stroke,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        )
        val sweep = 360f * ratio.coerceIn(0f, 1f)
        if (sweep > 1f) {
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = stroke,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            )
        }
    }
}

@Composable
fun BudgetLine(label: String, value: Double, hide: Boolean, color: androidx.compose.ui.graphics.Color) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = if (hide) "¥ ****" else "¥" + formatAmountGrouped(value),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
fun BudgetCard(
    budget: Double,
    spent: Double,
    hideAmount: Boolean,
    statsTick: Int,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
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
                    Icons.Outlined.Savings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "本月预算",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onEdit) {
                    Text(if (budget > 0) "修改" else "设置", fontSize = 11.sp)
                }
            }
            if (budget > 0) {
                Spacer(Modifier.height(12.dp))
                val ratio = (spent / budget).toFloat()
                val over = spent > budget
                val ringAnim = remember(statsTick) {
                    androidx.compose.animation.core.Animatable(0f)
                }
                androidx.compose.runtime.LaunchedEffect(statsTick, ratio) {
                    ringAnim.snapTo(0f)
                    ringAnim.animateTo(
                        ratio.coerceIn(0f, 1f),
                        androidx.compose.animation.core.spring(
                            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                        )
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(112.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BudgetRing(
                            ratio = ringAnim.value,
                            over = over,
                            modifier = Modifier.size(112.dp)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (over) "超支" else "已用",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (over) {
                                    if (hideAmount) "¥ ****" else "¥" + formatAmountGrouped(spent - budget)
                                } else {
                                    "%.0f%%".format(java.util.Locale.CHINA, ratio * 100)
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (over) ExpenseRed else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                        }
                    }
                    Spacer(Modifier.width(22.dp))
                    Column(Modifier.weight(1f)) {
                        BudgetLine("预算", budget, hideAmount, MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(8.dp))
                        BudgetLine("已用", spent, hideAmount, MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(8.dp))
                        if (over) {
                            BudgetLine("超支", spent - budget, hideAmount, ExpenseRed)
                        } else {
                            BudgetLine("剩余", budget - spent, hideAmount, MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            } else {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "还没有预算。设置一个每月预算，超支前心里有数。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


