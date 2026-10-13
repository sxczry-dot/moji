package com.autobook.app.util

object RefNoParser {

    // 取流水号尾段（后4-8位）：银行流水号常以日期开头，取尾段才能区分同日多笔
    private val refNoRegex = Regex(
        """(?:流水号|订单号|交易号|流水)[^0-9]{0,12}(\d{6,})"""
    )

    fun parse(text: String): String {
        val m = refNoRegex.find(text) ?: return ""
        val digits = m.groupValues[1]
        return digits.takeLast(6)
    }
}
