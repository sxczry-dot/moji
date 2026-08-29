package com.autobook.app.service

import android.content.Context
import com.autobook.app.AutoBookApp
import com.autobook.app.data.Transaction
import com.autobook.app.widget.BudgetWidgetProvider

object MonthSync {

    suspend fun refresh(context: Context, txs: List<Transaction>) {
        BudgetWidgetProvider.refresh(context, txs)
    }

    suspend fun refreshCurrentMonth(context: Context, book: String) {
        refresh(context, AutoBookApp.instance.repository.getCurrentMonthOnce(book))
    }
}
