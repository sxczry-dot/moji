package com.autobook.app.util

object LearnedMatch {

    const val MIN_FUZZY_LENGTH = 2

    fun categoryOf(entries: List<Pair<String, String>>, merchant: String): String? {
        val idx = fuzzyIndex(entries.map { it.first }, merchant) ?: return null
        return entries[idx].second
    }

    fun fuzzyIndex(keys: List<String>, merchant: String): Int? {
        if (merchant.isBlank()) return null
        keys.forEachIndexed { i, key ->
            if (key == merchant) return i
        }
        keys.forEachIndexed { i, key ->
            if (key.length >= MIN_FUZZY_LENGTH &&
                (merchant.contains(key) || key.contains(merchant))
            ) return i
        }
        return null
    }
}
