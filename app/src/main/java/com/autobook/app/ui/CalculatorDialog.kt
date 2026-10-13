package com.autobook.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val keys = listOf(
    listOf("7", "8", "9", "÷"),
    listOf("4", "5", "6", "×"),
    listOf("1", "2", "3", "−"),
    listOf("0", ".", "=", "+")
)

@Composable
fun CalculatorDialog(
    onResult: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var expr by remember { mutableStateOf("") }

    fun press(key: String) {
        when (key) {
            "=" -> {
                val result = evaluate(expr)
                if (result != null) {
                    // 保留全精度续算，避免 10÷3=→3.33→×3=→9.99 的链式误差
                    expr = result.toString()
                }
            }
            "C" -> expr = ""
            else -> expr += key
        }
    }

    AlertDialog(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        onDismissRequest = onDismiss,
        title = { Text("计算器") },
        text = {
            Column {
                Text(
                    text = expr.ifEmpty { "0" },
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
                Spacer(Modifier.height(8.dp))
                keys.forEach { rowKeys ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowKeys.forEach { key ->
                            OutlinedButton(
                                onClick = { press(key) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 2.dp)
                            ) {
                                Text(key, fontSize = 18.sp)
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { expr = expr.dropLast(1) },
                        modifier = Modifier.weight(1f)
                    ) { Text("←") }
                    OutlinedButton(
                        onClick = { press("C") },
                        modifier = Modifier.weight(1f),
                    ) { Text("C") }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val result = evaluate(expr) ?: expr.toDoubleOrNull()
                    if (result != null) onResult(formatResult(result))
                }
            ) { Text("填入金额") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

fun evaluate(expr: String): Double? {
    if (expr.isBlank()) return null
    val cleaned = expr.replace("×", "*").replace("÷", "/").replace("−", "-")
    val tokens = Regex("""(\d+\.?\d*)|([+\-*/])""").findAll(cleaned)
        .map { it.value }.toList()
    if (tokens.isEmpty()) return null
    if (tokens.size == 1) return tokens[0].toDoubleOrNull()
    if (tokens.size % 2 == 0) return null

    fun applyOps(ops: List<String>, tokensIn: List<String>): List<String>? {
        val result = mutableListOf<String>()
        var i = 0
        while (i < tokensIn.size) {
            val t = tokensIn[i]
            if (t in ops && result.isNotEmpty() && i + 1 < tokensIn.size) {
                val left = result.removeAt(result.size - 1).toDoubleOrNull() ?: return null
                val right = tokensIn[i + 1].toDoubleOrNull() ?: return null
                if (t == "/" && right == 0.0) return null
                result.add((if (t == "*") left * right else left / right).toString())
                i += 2
            } else {
                result.add(t)
                i++
            }
        }
        return result
    }

    val afterMulDiv = applyOps(listOf("*", "/"), tokens) ?: return null
    var result = afterMulDiv[0].toDoubleOrNull() ?: return null
    var i = 1
    while (i < afterMulDiv.size) {
        val op = afterMulDiv[i]
        val num = afterMulDiv[i + 1].toDoubleOrNull() ?: return null
        result = when (op) {
            "+" -> result + num
            "-" -> result - num
            else -> return null
        }
        i += 2
    }
    return result
}

private fun formatResult(value: Double): String {
    return if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        // 固定中国小数格式：逗号小数分隔符的语言环境下计算器不再失效/串号
        String.format(java.util.Locale.CHINA, "%.2f", value)
    }
}
