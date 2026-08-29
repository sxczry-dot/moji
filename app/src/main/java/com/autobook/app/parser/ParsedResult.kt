package com.autobook.app.parser

data class ParsedResult(
    val amount: Double,
    val type: Int,
    val merchant: String,
    val rawText: String = "",
    val tradeTime: Long = 0L,
    val refNo: String = ""
)
