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
import com.autobook.app.data.localTime
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


// ===== 从 TransactionListScreen 拆出的辅助组件（机械拆分，无行为变更）=====

@Composable
fun UndoBar(
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
fun SaveSuccessOverlay(tick: Int, modifier: Modifier = Modifier) {
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
fun TopSummaryRow(
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

