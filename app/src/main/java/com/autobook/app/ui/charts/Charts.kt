package com.autobook.app.ui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autobook.app.util.formatAmount

@Composable
fun PieChart(
    entries: List<Pair<String, Double>>,
    colorOf: (String) -> Color,
    modifier: Modifier = Modifier
) {
    val progress = androidx.compose.runtime.remember {
        androidx.compose.animation.core.Animatable(0f)
    }
    androidx.compose.runtime.LaunchedEffect(entries) {
        progress.snapTo(0f)
        progress.animateTo(
            1f,
            androidx.compose.animation.core.spring(
                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
            )
        )
    }
    Canvas(modifier = modifier) {
        val total = entries.sumOf { it.second }
        if (total <= 0) return@Canvas
        val strokeWidth = size.minDimension * 0.16f
        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
        val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
        var startAngle = -90f
        entries.forEach { (label, value) ->
            val sweep = (value / total * 360).toFloat() * progress.value
            if (sweep <= 0.5f) return@forEach
            drawArc(
                color = colorOf(label),
                startAngle = startAngle,
                sweepAngle = (sweep - 2f).coerceAtLeast(0.5f),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = strokeWidth,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            )
            startAngle += sweep
        }
    }
}

@Composable
fun BarChart(
    values: List<Double>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    onBarClick: ((Int) -> Unit)? = null,
    averageLine: Double? = null,
    hideValues: Boolean = false
) {
    val max = values.maxOrNull()?.takeIf { it > 0 } ?: 1.0
    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryDark = com.autobook.app.ui.theme.darker(primaryColor)
    val avgLineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
    val textMeasurer = androidx.compose.ui.text.rememberTextMeasurer()
    val barTextStyle = MaterialTheme.typography.labelSmall.copy(
        fontSize = 9.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    val growProgress = androidx.compose.runtime.remember {
        androidx.compose.animation.core.Animatable(0f)
    }
    androidx.compose.runtime.LaunchedEffect(values) {
        growProgress.snapTo(0f)
        growProgress.animateTo(
            1f,
            androidx.compose.animation.core.spring(
                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
            )
        )
    }

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .then(
                    if (onBarClick != null) {
                        Modifier.pointerInput(values.size) {
                            detectTapGestures { offset ->
                                val index = (offset.x / (size.width / values.size)).toInt()
                                if (index in values.indices) onBarClick(index)
                            }
                        }
                    } else {
                        Modifier
                    }
                )
        ) {
            val chartHeight = size.height - 24f
            val slot = size.width / values.size
            val barWidth = slot * 0.42f
            if (averageLine != null && averageLine > 0) {
                val avgY = (chartHeight - (averageLine / max * chartHeight).toFloat())
                    .coerceIn(0f, chartHeight)
                drawLine(
                    color = avgLineColor,
                    start = Offset(0f, avgY),
                    end = Offset(size.width, avgY),
                    strokeWidth = 2f,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                        floatArrayOf(12f, 8f)
                    )
                )
            }
            values.forEachIndexed { i, v ->
                val h = (v / max * chartHeight).toFloat() * growProgress.value
                val x = slot * i + (slot - barWidth) / 2f
                val top = chartHeight - h
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(primaryColor, primaryDark),
                        startY = top,
                        endY = chartHeight
                    ),
                    topLeft = Offset(x, top),
                    size = Size(barWidth, if (h > 0f) h else 4f),
                    cornerRadius = CornerRadius(10f, 10f)
                )

            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            labels.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
