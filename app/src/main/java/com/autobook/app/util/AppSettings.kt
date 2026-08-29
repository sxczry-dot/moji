package com.autobook.app.util

import android.content.Context
import com.autobook.app.data.Transaction

object AppSettings {

    private const val PREFS = "autobook_settings"
    const val DEFAULT_BOOK = "默认账本"

    private const val KEY_HIDE_AMOUNT = "hide_amount"
    private const val KEY_BOOKS = "books"
    private const val KEY_CURRENT_BOOK = "current_book"
    private const val KEY_RECURRING = "recurring_rules"
    private const val KEY_RECURRING_DONE_PREFIX = "rec_done_"
    private const val KEY_FONT_SCALE = "font_scale"
    private const val KEY_THEME_COLOR = "theme_color"
    private const val KEY_BLOCK_WORDS = "block_words"
    private const val KEY_DARK_MODE = "dark_mode"
    private const val KEY_PERSISTENT_NOTIF = "persistent_notification"
    private const val KEY_NOTIF_LOG = "notif_log"
    private const val KEY_LEARNED_CATS = "learned_categories"
    private const val KEY_CUSTOM_CHILDREN = "custom_children"
    private const val KEY_ONBOARDED = "onboarded"
    private const val KEY_SEARCH_HISTORY = "search_history"
    private const val KEY_STARRED = "starred_ids"
    private const val KEY_AUTO_PAUSED = "auto_paused"
    private const val KEY_BUDGET_PREFIX = "budget_"
    private const val KEY_LOCK_ENABLED = "app_lock_enabled"
    private const val KEY_LOCK_PIN = "app_lock_pin_hash"

    const val MAX_SEARCH_HISTORY = 10

    fun isAppLockEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_LOCK_ENABLED, false)

    fun setAppLockEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_LOCK_ENABLED, value).apply()
    }

    fun getLockPinHash(context: Context): String =
        prefs(context).getString(KEY_LOCK_PIN, "") ?: ""

    fun setLockPin(context: Context, pin: String) {
        val hash = hashPin(pin)
        prefs(context).edit().putString(KEY_LOCK_PIN, hash).apply()
    }

    fun verifyLockPin(context: Context, pin: String): Boolean {
        val saved = getLockPinHash(context)
        return saved.isNotEmpty() && hashPin(pin) == saved
    }

    private fun hashPin(pin: String): String {
        val salt = "autobook_salt_2026"
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val bytes = md.digest((salt + pin).toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun exportAllSettings(context: Context): String {
        val json = org.json.JSONObject()
        prefs(context).all.forEach { (k, v) ->
            when (v) {
                is String -> json.put(k, v)
                is Boolean -> json.put(k, v)
                is Int -> json.put(k, v)
                is Long -> json.put(k, v)
                is Float -> json.put(k, v)
            }
        }
        return json.toString()
    }

    fun importAllSettings(context: Context, jsonText: String) {
        val json = org.json.JSONObject(jsonText)
        val editor = prefs(context).edit()
        val keys = json.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            when (val v = json.get(k)) {
                is String -> editor.putString(k, v)
                is Boolean -> editor.putBoolean(k, v)
                is Int -> editor.putInt(k, v)
                is Long -> editor.putLong(k, v)
                is Double -> editor.putFloat(k, v.toFloat())
            }
        }
        editor.apply()
    }

    fun clearAllSettings(context: Context) {
        prefs(context).edit().clear().apply()
    }

    fun isAutoPaused(context: Context): Boolean =
        prefs(context).getBoolean(KEY_AUTO_PAUSED, false)

    fun setAutoPaused(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_AUTO_PAUSED, value).apply()
    }

    fun getBudget(context: Context, yearMonth: String): Double {
        val v = prefs(context).getFloat(KEY_BUDGET_PREFIX + yearMonth, -1f)
        return if (v < 0) 0.0 else v.toDouble()
    }

    fun setBudget(context: Context, yearMonth: String, value: Double) {
        prefs(context).edit().putFloat(KEY_BUDGET_PREFIX + yearMonth, value.toFloat()).apply()
    }

    fun clearBudget(context: Context, yearMonth: String) {
        prefs(context).edit().remove(KEY_BUDGET_PREFIX + yearMonth).apply()
    }

    fun getStarredIds(context: Context): Set<Long> =
        prefs(context).getString(KEY_STARRED, null)
            ?.split(",")
            ?.mapNotNull { it.toLongOrNull() }
            ?.toSet()
            ?: emptySet()

    fun isStarred(context: Context, id: Long): Boolean =
        id in getStarredIds(context)

    fun toggleStar(context: Context, id: Long) {
        val set = getStarredIds(context).toMutableSet()
        if (!set.add(id)) set.remove(id)
        prefs(context).edit().putString(KEY_STARRED, set.joinToString(",")).apply()
    }

    fun getSearchHistory(context: Context): List<String> {
        val saved = prefs(context).getString(KEY_SEARCH_HISTORY, null) ?: return emptyList()
        return saved.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }

    fun addSearchHistory(context: Context, word: String) {
        val w = word.trim()
        if (w.isBlank()) return
        val list = getSearchHistory(context).filter { it != w }.toMutableList()
        list.add(0, w)
        while (list.size > MAX_SEARCH_HISTORY) list.removeAt(list.size - 1)
        prefs(context).edit().putString(KEY_SEARCH_HISTORY, list.joinToString(",")).apply()
    }

    fun clearSearchHistory(context: Context) {
        prefs(context).edit().remove(KEY_SEARCH_HISTORY).apply()
    }

    fun isOnboarded(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ONBOARDED, false)

    fun setOnboarded(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_ONBOARDED, value).apply()
    }

    fun getCustomChildren(context: Context, primary: String): List<String> {
        val saved = prefs(context).getString(KEY_CUSTOM_CHILDREN + primary, null) ?: return emptyList()
        return saved.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }

    fun addCustomChild(context: Context, primary: String, child: String) {
        val list = (getCustomChildren(context, primary) + child).distinct()
        prefs(context).edit()
            .putString(KEY_CUSTOM_CHILDREN + primary, list.joinToString(","))
            .apply()
    }

    fun removeCustomChild(context: Context, primary: String, child: String) {
        val list = getCustomChildren(context, primary).filter { it != child }
        prefs(context).edit()
            .putString(KEY_CUSTOM_CHILDREN + primary, list.joinToString(","))
            .apply()
    }

    fun getAllLearned(context: Context): List<Pair<String, String>> {
        val saved = prefs(context).getString(KEY_LEARNED_CATS, null) ?: return emptyList()
        return saved.split(";")
            .mapNotNull { it.split("|").takeIf { p -> p.size == 2 } }
            .map { it[0] to it[1] }
    }

    fun removeLearned(context: Context, merchant: String) {
        val entries = getAllLearned(context).filter { it.first != merchant }
        prefs(context).edit()
            .putString(KEY_LEARNED_CATS, entries.joinToString(";") { "${it.first}|${it.second}" })
            .apply()
    }

    const val MAX_LEARNED = 500

    private const val KEY_CAT_MIGRATED = "cat_migrated_v5"

    fun migrateCategoryNames(context: Context) {
        if (prefs(context).getBoolean(KEY_CAT_MIGRATED, false)) return
        val learned = getAllLearned(context)
        if (learned.isNotEmpty()) {
            val migrated = learned.map { (merchant, cat) ->
                val primary = cat.substringBefore('/')
                val sub = cat.substringAfter('/', "")
                val newPrimary = com.autobook.app.data.Categories.mapLegacy(primary)
                val newCat = if (sub.isNotEmpty()) "$newPrimary/$sub" else newPrimary
                merchant to newCat
            }
            prefs(context).edit()
                .putString(KEY_LEARNED_CATS, migrated.joinToString(";") { "${it.first}|${it.second}" })
                .apply()
        }
        prefs(context).edit().putBoolean(KEY_CAT_MIGRATED, true).apply()
    }

    fun getLearnedCategory(context: Context, merchant: String): String? {
        val entries = getAllLearned(context)
        return LearnedMatch.categoryOf(entries, merchant)
    }

    fun learnCategory(context: Context, merchant: String, category: String) {
        if (merchant.isBlank()) return
        val saved = prefs(context).getString(KEY_LEARNED_CATS, null) ?: ""
        val entries = saved.split(";").filter { it.isNotBlank() }.toMutableList()
        entries.removeAll { it.split("|").firstOrNull() == merchant }
        entries.add(0, "$merchant|$category")
        while (entries.size > MAX_LEARNED) entries.removeAt(entries.size - 1)
        prefs(context).edit().putString(KEY_LEARNED_CATS, entries.joinToString(";")).apply()
    }


    const val MAX_NOTIF_LOG = 30

    fun addNotifLog(context: Context, line: String) {
        val saved = prefs(context).getString(KEY_NOTIF_LOG, null) ?: ""
        val lines = saved.split("\\n").filter { it.isNotBlank() }.toMutableList()
        lines.add(0, line)
        while (lines.size > MAX_NOTIF_LOG) lines.removeAt(lines.size - 1)
        prefs(context).edit().putString(KEY_NOTIF_LOG, lines.joinToString("\\n")).apply()
    }

    fun getNotifLog(context: Context): List<String> {
        val saved = prefs(context).getString(KEY_NOTIF_LOG, null) ?: return emptyList()
        return saved.split("\\n").filter { it.isNotBlank() }
    }

    fun clearNotifLog(context: Context) {
        prefs(context).edit().remove(KEY_NOTIF_LOG).apply()
    }

    fun isPersistentNotification(context: Context): Boolean =
        prefs(context).getBoolean(KEY_PERSISTENT_NOTIF, true)

    fun setPersistentNotification(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_PERSISTENT_NOTIF, value).apply()
    }

    const val DARK_FOLLOW_SYSTEM = 0
    const val DARK_ALWAYS_LIGHT = 1
    const val DARK_ALWAYS_DARK = 2

    fun getDarkMode(context: Context): Int =
        prefs(context).getInt(KEY_DARK_MODE, DARK_FOLLOW_SYSTEM)

    fun setDarkMode(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_DARK_MODE, value).apply()
    }

    fun isDarkModeActive(context: Context): Boolean {
        return when (getDarkMode(context)) {
            DARK_ALWAYS_LIGHT -> false
            DARK_ALWAYS_DARK -> true
            else -> (context.resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
        }
    }

    fun getBlockWords(context: Context): List<String> {
        val saved = prefs(context).getString(KEY_BLOCK_WORDS, null) ?: return emptyList()
        return saved.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }

    fun saveBlockWords(context: Context, words: List<String>) {
        prefs(context).edit().putString(KEY_BLOCK_WORDS, words.joinToString(",")).apply()
    }

    fun getThemeColor(context: Context): Long =
        prefs(context).getLong(KEY_THEME_COLOR, 0xFF00B578)

    fun setThemeColor(context: Context, value: Long) {
        prefs(context).edit().putLong(KEY_THEME_COLOR, value).apply()
    }

    const val FONT_TINY = 0.75f
    const val FONT_SMALL = 0.85f
    const val FONT_NORMAL = 1.0f
    const val FONT_LARGE = 1.15f
    const val FONT_HUGE = 1.3f

    fun getFontScale(context: Context): Float =
        prefs(context).getFloat(KEY_FONT_SCALE, FONT_NORMAL)

    fun setFontScale(context: Context, value: Float) {
        prefs(context).edit().putFloat(KEY_FONT_SCALE, value).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isHideAmount(context: Context): Boolean =
        prefs(context).getBoolean(KEY_HIDE_AMOUNT, false)

    fun setHideAmount(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_HIDE_AMOUNT, value).apply()
    }

    fun getBooks(context: Context): List<String> {
        val saved = prefs(context).getString(KEY_BOOKS, null) ?: return listOf(DEFAULT_BOOK)
        return listOf(DEFAULT_BOOK) + saved.split(",").filter { it.isNotBlank() }
    }

    fun addBook(context: Context, name: String) {
        val books = getBooks(context).toMutableSet().apply { add(name) }
        prefs(context).edit()
            .putString(KEY_BOOKS, books.filter { it != DEFAULT_BOOK }.joinToString(","))
            .apply()
    }

    fun renameBook(context: Context, oldName: String, newName: String) {
        if (oldName == DEFAULT_BOOK || newName.isBlank() || newName == oldName) return
        val books = getBooks(context).map { if (it == oldName) newName else it }
        prefs(context).edit()
            .putString(KEY_BOOKS, books.filter { it != DEFAULT_BOOK }.joinToString(","))
            .apply()
        if (getCurrentBook(context) == oldName) setCurrentBook(context, newName)
    }

    fun removeBook(context: Context, name: String) {
        if (name == DEFAULT_BOOK) return
        val books = getBooks(context).filter { it != name }
        prefs(context).edit()
            .putString(KEY_BOOKS, books.filter { it != DEFAULT_BOOK }.joinToString(","))
            .apply()
        if (getCurrentBook(context) == name) setCurrentBook(context, com.autobook.app.util.AppSettings.DEFAULT_BOOK)
    }

    fun getCurrentBook(context: Context): String {
        val saved = prefs(context).getString(KEY_CURRENT_BOOK, null) ?: return DEFAULT_BOOK
        return if (getBooks(context).contains(saved)) saved else com.autobook.app.util.AppSettings.DEFAULT_BOOK
    }

    fun setCurrentBook(context: Context, book: String) {
        prefs(context).edit().putString(KEY_CURRENT_BOOK, book).apply()
    }

    data class RecurringRule(
        val name: String,
        val amount: Double,
        val type: Int,
        val category: String,
        val day: Int,
        val merchant: String,
        val period: Int = 0,
        val book: String = DEFAULT_BOOK,
        val paused: Boolean = false
    ) {
        fun encode(): String =
            "$name|$amount|$type|$category|$day|$merchant|$period|$book|${if (paused) 1 else 0}"

        companion object {
            fun decode(raw: String): RecurringRule? {
                val parts = raw.split("|")
                if (parts.size < 6) return null
                return RecurringRule(
                    name = parts[0],
                    amount = parts[1].toDoubleOrNull() ?: return null,
                    type = parts[2].toIntOrNull() ?: return null,
                    category = parts[3],
                    day = parts[4].toIntOrNull() ?: return null,
                    merchant = parts[5],
                    period = parts.getOrNull(6)?.toIntOrNull() ?: 0,
                    book = parts.getOrNull(7)?.takeIf { it.isNotBlank() } ?: DEFAULT_BOOK,
                    paused = parts.getOrNull(8) == "1"
                )
            }
        }
    }

    fun getRecurringRules(context: Context): List<RecurringRule> {
        val saved = prefs(context).getString(KEY_RECURRING, null) ?: return emptyList()
        return saved.split(";").mapNotNull { RecurringRule.decode(it) }
    }

    fun saveRecurringRules(context: Context, rules: List<RecurringRule>) {
        prefs(context).edit()
            .putString(KEY_RECURRING, rules.joinToString(";") { it.encode() })
            .apply()
    }

    fun isRecurringDone(context: Context, key: String, year: Int, month: Int): Boolean {
        return prefs(context).getBoolean(KEY_RECURRING_DONE_PREFIX + key + "_" + year + month, false)
    }

    fun markRecurringDone(context: Context, key: String, year: Int, month: Int) {
        prefs(context).edit()
            .putBoolean(KEY_RECURRING_DONE_PREFIX + key + "_" + year + month, true)
            .apply()
    }
}
