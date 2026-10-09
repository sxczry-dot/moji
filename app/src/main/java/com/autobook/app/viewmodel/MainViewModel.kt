package com.autobook.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autobook.app.AutoBookApp
import com.autobook.app.data.Transaction
import com.autobook.app.data.expenseSum
import com.autobook.app.data.incomeSum
import com.autobook.app.service.MonthSync
import com.autobook.app.util.AppSettings
import com.autobook.app.util.TimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AutoBookApp.instance.repository

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    val currentMonth: StateFlow<YearMonth> = _currentMonth

    private val _currentBook = MutableStateFlow(AppSettings.getCurrentBook(app))
    val currentBook: StateFlow<String> = _currentBook

    private val _hideAmount = MutableStateFlow(AppSettings.isHideAmount(app))
    val hideAmount: StateFlow<Boolean> = _hideAmount

    private val _fontScale = MutableStateFlow(AppSettings.getFontScale(app))
    val fontScale: StateFlow<Float> = _fontScale

    private val _themeColor = MutableStateFlow(AppSettings.getThemeColor(app))
    val themeColor: StateFlow<Long> = _themeColor

    private val _darkMode = MutableStateFlow(AppSettings.getDarkMode(app))
    val darkMode: StateFlow<Int> = _darkMode

    val monthTransactions: StateFlow<List<Transaction>> =
        combine(_currentBook, _currentMonth) { book, ym -> book to ym }
            .flatMapLatest { (book, ym) -> repo.getByMonth(book, ym.year, ym.monthValue - 1) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions: StateFlow<List<Transaction>> = repo.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setCurrentBook(book: String) {
        AppSettings.setCurrentBook(getApplication(), book)
        _currentBook.value = book
        viewModelScope.launch(Dispatchers.IO) {
            MonthSync.refreshCurrentMonth(getApplication(), book)
        }
    }

    fun setHideAmount(value: Boolean) {
        AppSettings.setHideAmount(getApplication(), value)
        _hideAmount.value = value
        viewModelScope.launch(Dispatchers.IO) {
            MonthSync.refreshCurrentMonth(getApplication(), _currentBook.value)
        }
    }

    fun setFontScale(value: Float) {
        AppSettings.setFontScale(getApplication(), value)
        _fontScale.value = value
    }

    fun setThemeColor(value: Long) {
        AppSettings.setThemeColor(getApplication(), value)
        _themeColor.value = value
        com.autobook.app.widget.WidgetTheme.refreshAll(getApplication())
    }

    fun setDarkMode(value: Int) {
        AppSettings.setDarkMode(getApplication(), value)
        _darkMode.value = value
        com.autobook.app.widget.WidgetTheme.refreshAll(getApplication())
        viewModelScope.launch(Dispatchers.IO) {
            MonthSync.refreshCurrentMonth(getApplication(), _currentBook.value)
        }
    }

    data class MonthStat(
        val yearMonth: YearMonth,
        val expense: Double,
        val income: Double
    )

    val lastSixMonths: StateFlow<List<MonthStat>> =
        combine(allTransactions, _currentMonth) { txs, ym ->
            (5 downTo 0).map { offset ->
                val m = ym.minusMonths(offset.toLong())
                val (start, end) = TimeUtils.monthRange(m.year, m.monthValue - 1)
                val inMonth = txs.filter { it.timestamp in start until end }
                MonthStat(
                    yearMonth = m,
                    expense = inMonth.expenseSum(),
                    income = inMonth.incomeSum()
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setMonth(ym: YearMonth) {
        _currentMonth.value = ym
    }

    fun previousMonth() {
        _currentMonth.value = _currentMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        val next = _currentMonth.value.plusMonths(1)
        if (!next.isAfter(YearMonth.now())) {
            _currentMonth.value = next
        }
    }

    fun addManual(amount: Double, type: Int, category: String, merchant: String, tags: String, timestamp: Long = System.currentTimeMillis(), book: String? = null) {
        if (merchant.isNotBlank()) {
            AppSettings.learnCategory(getApplication(), merchant, com.autobook.app.data.Categories.primaryOf(category))
        }
        viewModelScope.launch(Dispatchers.IO) {
            repo.add(
                Transaction(
                    amount = amount,
                    type = type,
                    category = category,
                    merchant = merchant.ifEmpty { Transaction.MERCHANT_FALLBACK },
                    source = Transaction.SOURCE_MANUAL,
                    timestamp = timestamp,
                    tags = tags.trim(),
                    book = book ?: _currentBook.value
                )
            )
            MonthSync.refreshCurrentMonth(getApplication(), _currentBook.value)
        }
    }

    fun delete(tx: Transaction) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.remove(tx)
            MonthSync.refreshCurrentMonth(getApplication(), _currentBook.value)
        }
    }

    fun update(
        tx: Transaction,
        amount: Double,
        type: Int,
        category: String,
        merchant: String,
        tags: String,
        timestamp: Long,
        book: String? = null
    ) {
        val learnedMerchant = merchant.ifBlank { tx.merchant }
        AppSettings.learnCategory(getApplication(), learnedMerchant, com.autobook.app.data.Categories.primaryOf(category))
        viewModelScope.launch(Dispatchers.IO) {
            repo.update(
                tx.copy(
                    amount = amount,
                    type = type,
                    category = category,
                    merchant = merchant.ifEmpty { tx.source.ifEmpty { Transaction.MERCHANT_FALLBACK } },
                    tags = tags.trim(),
                    timestamp = timestamp,
                    book = book ?: tx.book
                )
            )
            MonthSync.refreshCurrentMonth(getApplication(), _currentBook.value)
        }
    }
}
