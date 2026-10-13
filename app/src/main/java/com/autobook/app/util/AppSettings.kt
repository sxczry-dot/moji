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
        // "覆盖"语义：先清空再写入（与恢复提示一致）
        editor.clear()
        val keys = json.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            when (val v = json.get(k)) {
                is String -> editor.putString(k, v)
                is Boolean -> editor.putBoolean(k, v)
                is Integer -> editor.putInt(k, v.toInt())
                is Long -> editor.putLong(k, v.toLong())
                is Double -> editor.putFloat(k, v.toFloat())
                is Float -> editor.putFloat(k, v)
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
        val clean = line.replace('\n', ' ').replace('\r', ' ')
        val lines = mutableListOf(clean)
        lines.addAll(getNotifLog(context))
        while (lines.size > MAX_NOTIF_LOG) lines.removeAt(lines.size - 1)
        prefs(context).edit().putString(KEY_NOTIF_LOG, lines.joinToString("\n")).apply()
    }

    fun getNotifLog(context: Context): List<String> {
        val saved = prefs(context).getString(KEY_NOTIF_LOG, null) ?: return emptyList()
        // 兼容旧版字面量 "\n" 分隔
        val parts = if (saved.contains("\\n")) saved.split("\\n") else saved.split("\n")
        return parts.filter { it.isNotBlank() }
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
        if (saved.trimStart().startsWith("[")) {
            return try {
                val arr = org.json.JSONArray(saved)
                (0 until arr.length())
                    .mapNotNull { arr.optString(it).trim().takeIf { s -> s.isNotBlank() } }
            } catch (_: Exception) {
                emptyList()
            }
        }
        // 旧逗号格式迁移到 JSON（词内含逗号不再被拆分）
        val list = saved.split(",").map { it.trim() }.filter { it.isNotBlank() }
        saveBlockWords(context, list)
        return list
    }

    fun saveBlockWords(context: Context, words: List<String>) {
        val arr = org.json.JSONArray()
        words.forEach { arr.put(it) }
        prefs(context).edit().putString(KEY_BLOCK_WORDS, arr.toString()).apply()
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
        // 周期规则同步迁移账本（否则规则继续向旧账本名写记录，成为幽灵账本）
        val rules = getRecurringRules(context)
        if (rules.any { it.book == oldName }) {
            saveRecurringRules(context, rules.map { if (it.book == oldName) it.copy(book = newName) else it })
        }
    }

    fun removeBook(context: Context, name: String) {
        if (name == DEFAULT_BOOK) return
        val books = getBooks(context).filter { it != name }
        prefs(context).edit()
            .putString(KEY_BOOKS, books.filter { it != DEFAULT_BOOK }.joinToString(","))
            .apply()
        if (getCurrentBook(context) == name) setCurrentBook(context, com.autobook.app.util.AppSettings.DEFAULT_BOOK)
        // 周期规则：引用被删账本的规则迁回默认账本
        val rules = getRecurringRules(context)
        if (rules.any { it.book == name }) {
            saveRecurringRules(
                context,
                rules.map { if (it.book == name) it.copy(book = DEFAULT_BOOK) else it }
            )
        }
    }

    fun getCurrentBook(context: Context): String {
        val saved = prefs(context).getString(KEY_CURRENT_BOOK, null) ?: return DEFAULT_BOOK
        return if (getBooks(context).contains(saved)) saved else com.autobook.app.util.AppSettings.DEFAULT_BOOK
    }

    fun setCurrentBook(context: Context, book: String) {
        prefs(context).edit().putString(KEY_CURRENT_BOOK, book).apply()
    }

    data class RecurringRule(
        val id: Long = 0L,
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
            org.json.JSONObject().apply {
                put("id", id)
                put("name", name)
                put("amount", amount)
                put("type", type)
                put("category", category)
                put("day", day)
                put("merchant", merchant)
                put("period", period)
                put("book", book)
                put("paused", paused)
            }.toString()

        companion object {
            fun decode(raw: String): RecurringRule? {
                val trimmed = raw.trim()
                if (trimmed.startsWith("{")) {
                    return try {
                        val o = org.json.JSONObject(trimmed)
                        RecurringRule(
                            id = o.optLong("id", 0L),
                            name = o.optString("name", ""),
                            amount = o.optDouble("amount", Double.NaN),
                            type = o.optInt("type", 0),
                            category = o.optString("category", ""),
                            day = o.optInt("day", 1),
                            merchant = o.optString("merchant", ""),
                            period = o.optInt("period", 0),
                            book = o.optString("book", DEFAULT_BOOK).ifBlank { DEFAULT_BOOK },
                            paused = o.optBoolean("paused", false)
                        ).takeIf { it.name.isNotBlank() && !it.amount.isNaN() }
                    } catch (_: Exception) {
                        null
                    }
                }
                // 旧格式 "name|amount|type|category|day|merchant|period|book|paused"
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
        val rules: List<RecurringRule> = if (saved.trimStart().startsWith("[")) {
            try {
                val arr = org.json.JSONArray(saved)
                (0 until arr.length()).mapNotNull { RecurringRule.decode(arr.getString(it)) }
            } catch (_: Exception) {
                emptyList()
            }
        } else {
            saved.split(";").mapNotNull { RecurringRule.decode(it) }
        }
        // 旧格式迁移：补稳定 id 并落盘（id 是完成标记 key 的一部分，编辑规则不会导致当周期重复执行）
        if (rules.any { it.id == 0L }) {
            val now = System.currentTimeMillis()
            val migrated = rules.mapIndexed { i, r -> if (r.id != 0L) r else r.copy(id = now + i) }
            saveRecurringRules(context, migrated)
            // 同步迁移完成标记（旧 key 由 name 组成），避免当月已执行过的规则升级后重记一次
            val p = prefs(context)
            val editor = p.edit()
            migrated.forEach { r ->
                val oldPrefix = KEY_RECURRING_DONE_PREFIX + "${r.name}_${r.day}_${r.period}_${r.book}"
                val newPrefix = KEY_RECURRING_DONE_PREFIX + recurringKey(r)
                if (oldPrefix != newPrefix) {
                    p.all.keys.filter { it.startsWith(oldPrefix) }.forEach { k ->
                        editor.putBoolean(newPrefix + k.removePrefix(oldPrefix), p.getBoolean(k, false))
                        editor.remove(k)
                    }
                }
            }
            editor.apply()
            return migrated
        }
        return rules
    }

    fun saveRecurringRules(context: Context, rules: List<RecurringRule>) {
        val arr = org.json.JSONArray()
        rules.forEach { arr.put(it.encode()) }
        prefs(context).edit().putString(KEY_RECURRING, arr.toString()).apply()
    }

    /** 完成标记 key 的稳定部分：编辑名称/金额不影响；改日期/周期/账本则视为新规则周期 */
    fun recurringKey(rule: RecurringRule): String =
        "${rule.id}_${rule.day}_${rule.period}_${rule.book}"

    fun clearRecurringDone(context: Context, rule: RecurringRule) {
        val prefix = KEY_RECURRING_DONE_PREFIX + recurringKey(rule)
        val p = prefs(context)
        val editor = p.edit()
        p.all.keys.filter { it.startsWith(prefix) }.forEach { editor.remove(it) }
        editor.apply()
    }

    fun isRecurringDone(context: Context, key: String, year: Int, month: Int): Boolean {
        return prefs(context).getBoolean(KEY_RECURRING_DONE_PREFIX + key + "_" + year + month, false)
    }

    fun markRecurringDone(context: Context, key: String, year: Int, month: Int) {
        prefs(context).edit()
            .putBoolean(KEY_RECURRING_DONE_PREFIX + key + "_" + year + month, true)
            .apply()
    }

    // ===== 信用卡账单还款日（JSON 列表）=====

    private const val KEY_BILL_DUES = "bill_dues"
    private const val KEY_BILL_NOTIFIED_PREFIX = "bill_notified_"

    fun getBillDues(context: Context): List<com.autobook.app.util.BillDueParser.BillDue> {
        val saved = prefs(context).getString(KEY_BILL_DUES, null) ?: return emptyList()
        if (!saved.trimStart().startsWith("[")) return emptyList()
        return try {
            val arr = org.json.JSONArray(saved)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.getJSONObject(i)
                com.autobook.app.util.BillDueParser.BillDue(
                    amount = o.optDouble("amount", 0.0),
                    minPayment = o.optDouble("min", 0.0),
                    dueDate = java.time.LocalDate.parse(o.optString("due", ""))
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun upsertBillDue(context: Context, due: com.autobook.app.util.BillDueParser.BillDue) {
        val list = getBillDues(context).filter { it.dueDate != due.dueDate }.toMutableList()
        list.add(due)
        saveBillDues(context, list)
    }

    fun saveBillDues(context: Context, dues: List<com.autobook.app.util.BillDueParser.BillDue>) {
        val arr = org.json.JSONArray()
        dues.forEach { d ->
            arr.put(
                org.json.JSONObject().apply {
                    put("amount", d.amount)
                    put("min", d.minPayment)
                    put("due", d.dueDate.toString())
                }
            )
        }
        prefs(context).edit().putString(KEY_BILL_DUES, arr.toString()).apply()
    }

    /** 还款入账后核销对应账单（金额匹配±0.02） */
    fun clearBillDuesMatching(context: Context, amount: Double) {
        val list = getBillDues(context).filter { kotlin.math.abs(it.amount - amount) >= 0.02 }
        saveBillDues(context, list)
    }

    fun isBillNotified(context: Context, key: String): Boolean =
        prefs(context).getBoolean(KEY_BILL_NOTIFIED_PREFIX + key, false)

    fun markBillNotified(context: Context, key: String) {
        prefs(context).edit().putBoolean(KEY_BILL_NOTIFIED_PREFIX + key, true).apply()
    }

    // ===== 保存式搜索筛选器（JSON 列表）=====

    private const val KEY_SAVED_FILTERS = "saved_filters"

    data class SavedFilter(
        val name: String,
        val category: String? = null,
        val min: Double? = null,
        val max: Double? = null,
        val fromMonth: String? = null, // "yyyy-MM"
        val toMonth: String? = null,
        val starOnly: Boolean = false
    )

    fun getSavedFilters(context: Context): List<SavedFilter> {
        val saved = prefs(context).getString(KEY_SAVED_FILTERS, null) ?: return emptyList()
        if (!saved.trimStart().startsWith("[")) return emptyList()
        return try {
            val arr = org.json.JSONArray(saved)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.getJSONObject(i)
                SavedFilter(
                    name = o.optString("name", ""),
                    category = o.optString("cat", "").ifBlank { null },
                    min = o.optDouble("min", Double.NaN).takeIf { !it.isNaN() },
                    max = o.optDouble("max", Double.NaN).takeIf { !it.isNaN() },
                    fromMonth = o.optString("from", "").ifBlank { null },
                    toMonth = o.optString("to", "").ifBlank { null },
                    starOnly = o.optBoolean("star", false)
                ).takeIf { it.name.isNotBlank() }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveSavedFilters(context: Context, filters: List<SavedFilter>) {
        val arr = org.json.JSONArray()
        filters.forEach { f ->
            arr.put(
                org.json.JSONObject().apply {
                    put("name", f.name)
                    put("cat", f.category ?: "")
                    f.min?.let { put("min", it) }
                    f.max?.let { put("max", it) }
                    put("from", f.fromMonth ?: "")
                    put("to", f.toMonth ?: "")
                    put("star", f.starOnly)
                }
            )
        }
        prefs(context).edit().putString(KEY_SAVED_FILTERS, arr.toString()).apply()
    }

    fun addSavedFilter(context: Context, filter: SavedFilter) {
        val list = getSavedFilters(context).filter { it.name != filter.name }.toMutableList()
        list.add(0, filter)
        saveSavedFilters(context, list)
    }

    fun removeSavedFilter(context: Context, name: String) {
        saveSavedFilters(context, getSavedFilters(context).filter { it.name != name })
    }
}
