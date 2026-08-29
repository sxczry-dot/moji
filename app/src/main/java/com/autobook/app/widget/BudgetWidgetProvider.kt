package com.autobook.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.autobook.app.AutoBookApp
import com.autobook.app.MainActivity
import com.autobook.app.R
import com.autobook.app.data.Transaction
import com.autobook.app.data.expenseSum
import com.autobook.app.data.incomeSum
import com.autobook.app.util.formatAmountGrouped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class BudgetWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = AutoBookApp.instance.repository
                val cal = Calendar.getInstance()
                val book = com.autobook.app.util.AppSettings.getCurrentBook(context)
                val txs = repo.getByMonthOnce(book, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
                updateAll(context, appWidgetManager, appWidgetIds, txs)
            } catch (e: Exception) {
                android.util.Log.e("BudgetWidget", "update failed", e)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private fun updateAll(
            context: Context,
            manager: AppWidgetManager,
            ids: IntArray,
            txs: List<Transaction>
        ) {
            for (id in ids) {
                manager.updateAppWidget(id, buildViews(context, txs))
            }
        }

        private fun buildViews(context: Context, txs: List<Transaction>): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_budget)
            val hide = com.autobook.app.util.AppSettings.isHideAmount(context)
            val expense = txs.expenseSum()
            val income = txs.incomeSum()
            val topCategories = txs
                .filter { it.type == Transaction.TYPE_EXPENSE }
                .groupBy { com.autobook.app.data.Categories.primaryOf(it.category) }
                .map { (cat, list) -> cat to list.sumOf { it.amount } }
                .sortedByDescending { it.second }
                .take(3)
            val maxCat = topCategories.firstOrNull()?.second ?: 1.0
            val rows = listOf(
                Triple(R.id.widget_cat1_name, R.id.widget_cat1_bar, R.id.widget_cat1_amount),
                Triple(R.id.widget_cat2_name, R.id.widget_cat2_bar, R.id.widget_cat2_amount),
                Triple(R.id.widget_cat3_name, R.id.widget_cat3_bar, R.id.widget_cat3_amount)
            )
            rows.forEachIndexed { i, (nameId, barId, amountId) ->
                val entry = topCategories.getOrNull(i)
                if (entry != null) {
                    views.setTextViewText(nameId, entry.first)
                    views.setInt(barId, "setMax", 100)
                    views.setInt(barId, "setProgress", ((entry.second / maxCat) * 100).toInt())
                    views.setTextViewText(amountId, if (hide) "****" else formatAmountGrouped(entry.second))
                } else {
                    views.setTextViewText(nameId, "")
                    views.setInt(barId, "setProgress", 0)
                    views.setTextViewText(amountId, "")
                }
            }
            val bgRes = WidgetTheme.backgroundResource(context)
            if (bgRes > 0) {
                views.setInt(R.id.widget_root, "setBackgroundResource", bgRes)
            } else {
                views.setInt(
                    R.id.widget_root,
                    "setBackgroundColor",
                    WidgetTheme.backgroundColor(context)
                )
            }
            val bookName = com.autobook.app.util.AppSettings.getCurrentBook(context)
            views.setTextViewText(R.id.widget_title, bookName + " · 本月")
            val todayStart = com.autobook.app.util.TimeUtils.dayStartOf(System.currentTimeMillis())
            val todayExpense = txs.filter {
                it.type == Transaction.TYPE_EXPENSE && it.timestamp >= todayStart
            }.sumOf { it.amount }
            views.setTextViewText(
                R.id.widget_today,
                if (hide) "今日 ****" else "今日 ¥" + formatAmountGrouped(todayExpense)
            )
            views.setTextViewText(
                R.id.widget_balance,
                if (hide) "¥ ****" else "¥" + formatAmountGrouped(income - expense)
            )
            views.setTextViewText(
                R.id.widget_expense,
                if (hide) "¥ ****" else "¥" + formatAmountGrouped(expense)
            )
            views.setTextViewText(
                R.id.widget_income,
                if (hide) "¥ ****" else "¥" + formatAmountGrouped(income)
            )

            val openListIntent = Intent(context, MainActivity::class.java).apply {
                putExtra("open_tab", 0)
            }
            val openStatsIntent = Intent(context, MainActivity::class.java).apply {
                putExtra("open_tab", 1)
            }
            val quickAddIntent = Intent(context, MainActivity::class.java).apply {
                putExtra("quick_add", true)
            }
            val listPending = PendingIntent.getActivity(
                context, 0, openListIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val statsPending = PendingIntent.getActivity(
                context, 1, openStatsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val addPending = PendingIntent.getActivity(
                context, 2, quickAddIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, listPending)
            views.setOnClickPendingIntent(R.id.widget_today, listPending)
            views.setOnClickPendingIntent(R.id.widget_expense, listPending)
            views.setOnClickPendingIntent(R.id.widget_income, listPending)
            views.setOnClickPendingIntent(R.id.widget_balance, statsPending)
            views.setOnClickPendingIntent(R.id.widget_add_btn, addPending)
            return views
        }

        fun refresh(context: Context, txs: List<Transaction>? = null) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, BudgetWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isEmpty()) return

            if (txs != null) {
                updateAll(context, manager, ids, txs)
            } else {
                val intent = Intent(context, BudgetWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                context.sendBroadcast(intent)
            }
        }
    }
}
