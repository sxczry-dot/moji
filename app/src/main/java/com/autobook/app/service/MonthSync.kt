package com.autobook.app.service

import android.content.Context
import com.autobook.app.AutoBookApp
import com.autobook.app.data.Transaction
import com.autobook.app.widget.BudgetWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object MonthSync {

    private val debounceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var refreshJob: Job? = null

    suspend fun refresh(context: Context, txs: List<Transaction>) {
        // 连续入账时合并刷新（防抖），避免每笔都全量重建小组件 RemoteViews
        refreshJob?.cancel()
        refreshJob = debounceScope.launch {
            delay(500)
            BudgetWidgetProvider.refresh(context, txs)
        }
    }

    suspend fun refreshCurrentMonth(context: Context, book: String) {
        refresh(context, AutoBookApp.instance.repository.getCurrentMonthOnce(book))
    }
}
