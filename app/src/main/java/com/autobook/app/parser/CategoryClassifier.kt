package com.autobook.app.parser

import com.autobook.app.data.Categories

object CategoryClassifier {

    private val rules = ParseRules.categoryRules

    fun classifyExpense(text: String): String {
        val t = text.lowercase()
        for ((category, keywords) in rules) {
            if (keywords.any { t.contains(it.lowercase()) }) return category
        }
        return Categories.OTHER
    }

    fun classifyIncome(text: String): String = Categories.INCOME
}
