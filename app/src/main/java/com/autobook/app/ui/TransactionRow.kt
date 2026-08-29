package com.autobook.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.LocalIndication
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autobook.app.util.formatTime
import com.autobook.app.util.formatCategory
import com.autobook.app.util.formatAmount
import com.autobook.app.data.Transaction
import com.autobook.app.ui.theme.ExpenseRed
import com.autobook.app.ui.theme.IncomeBlue
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SwipeableTransactionRow(
    tx: Transaction,
    hideAmount: Boolean = false,
    starred: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val maxRevealPx = with(density) { 144.dp.toPx() }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val offsetAnim = remember { androidx.compose.animation.core.Animatable(0f) }
    val rowInteraction = remember { MutableInteractionSource() }
    val rowPressed by rowInteraction.collectIsPressedAsState()

    fun settle(open: Boolean) {
        scope.launch {
            offsetAnim.animateTo(
                if (open) maxRevealPx else 0f,
                androidx.compose.animation.core.spring(
                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                    stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                )
            )
        }
    }

    Box(
        modifier = modifier
            .height(IntrinsicSize.Min)
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 0.dp)
                .padding(end = 20.dp)
                .graphicsLayer {
                    val scale = if (rowPressed) 0.98f else 1f
                    scaleX = scale
                    scaleY = scale
                },
            horizontalArrangement = Arrangement.End
        ) {
            Box(
                modifier = Modifier
                    .width(72.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable {
                        settle(false)
                        onEdit()
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        androidx.compose.material.icons.Icons.Outlined.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text("编辑", color = MaterialTheme.colorScheme.onPrimary, fontSize = 13.sp)
                }
            }
            Box(
                modifier = Modifier
                    .width(72.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
                    .background(ExpenseRed)
                    .clickable {
                        settle(false)
                        onDelete()
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        androidx.compose.material.icons.Icons.Outlined.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onError,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text("删除", color = MaterialTheme.colorScheme.onError, fontSize = 13.sp)
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = -offsetAnim.value }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            settle(offsetAnim.value > maxRevealPx / 2f)
                        },
                        onDragCancel = {
                            settle(false)
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            val current = offsetAnim.value
                            val next = current - dragAmount
                            val damped = when {
                                next > maxRevealPx -> maxRevealPx + (next - maxRevealPx) * 0.35f
                                next < 0f -> next * 0.35f
                                else -> next
                            }
                            offsetAnim.snapTo(damped)
                        }
                    }
                }
        ) {
            TransactionRow(
                tx, hideAmount,
                starred = starred,
                onClick = {
                    settle(false)
                    onClick?.invoke()
                },
                onLongClick = onLongClick,
                interactionSource = rowInteraction
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionRow(
    tx: Transaction,
    hideAmount: Boolean = false,
    starred: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null
) {
    val isExpense = tx.type == Transaction.TYPE_EXPENSE
    val color = if (isExpense) ExpenseRed else IncomeBlue
    val sign = if (isExpense) "-" else "+"
    val amountText = if (hideAmount) "****" else "$sign${formatAmount(tx.amount)}"

    val innerSource = remember { MutableInteractionSource() }
    val source = interactionSource ?: innerSource
    val pressed by source.collectIsPressedAsState()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .graphicsLayer {
                val scale = if (pressed) 0.98f else 1f
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(16.dp))
            .let {
                when {
                    onLongClick != null -> it.combinedClickable(
                        interactionSource = source,
                        indication = LocalIndication.current,
                        onClick = onClick ?: {},
                        onLongClick = onLongClick
                    )
                    onClick != null -> it.clickable(
                        interactionSource = source,
                        indication = LocalIndication.current,
                        onClick = onClick
                    )
                    else -> it
                }
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (pressed) 0.dp else 2.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                CategoryUi.colorOf(tx.category).copy(alpha = 0.22f),
                                CategoryUi.colorOf(tx.category).copy(alpha = 0.08f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = CategoryUi.iconOf(tx.category),
                    contentDescription = tx.category,
                    tint = CategoryUi.colorOf(tx.category),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (starred) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = "星标",
                            tint = Color(0xFFF5B301),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        text = tx.merchant,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )

                }
                Text(
                    text = "${formatCategory(tx.category)} · ${formatTime(tx.timestamp)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = amountText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}
