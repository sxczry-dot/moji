package com.autobook.app.data

import android.content.Context
import com.autobook.app.util.TimeUtils
import kotlinx.coroutines.flow.Flow

class TransactionRepository(context: Context) {

    private val dao = AppDatabase.get(context).transactionDao()

    fun getAll(): Flow<List<Transaction>> = dao.getAll()

    fun getByMonth(book: String, year: Int, month: Int): Flow<List<Transaction>> {
        val (start, end) = TimeUtils.monthRange(year, month)
        return dao.getByRange(book, start, end)
    }

    suspend fun getByMonthOnce(book: String, year: Int, month: Int): List<Transaction> {
        val (start, end) = TimeUtils.monthRange(year, month)
        return dao.getByRangeOnce(book, start, end)
    }

    suspend fun add(tx: Transaction) {
        // 补到达时间：让手动/周期记录参与自动记账的去重候选池，避免与通知重复记账
        dao.insert(tx.copy(insertedAt = System.currentTimeMillis()))
    }

    suspend fun addAll(txs: List<Transaction>) {
        dao.insertAll(txs)
    }

    suspend fun replaceAll(txs: List<Transaction>) {
        dao.deleteAll()
        dao.insertAll(txs)
    }

    suspend fun getAllOnce(): List<Transaction> = dao.getAllOnce()

    suspend fun moveBook(from: String, to: String) {
        dao.moveBook(from, to)
    }

    suspend fun insertAndGetMonth(tx: Transaction, now: Long): SaveResult {
        return dao.insertAndGetMonth(tx, now)
    }

    suspend fun getCurrentMonthOnce(book: String): List<Transaction> {
        val (start, end) = TimeUtils.currentMonthRange()
        return dao.getByRangeOnce(book, start, end)
    }

    suspend fun remove(tx: Transaction) {
        dao.delete(tx)
    }

    suspend fun deleteById(id: Long) {
        dao.deleteById(id)
    }

    suspend fun getById(id: Long): Transaction? = dao.getById(id)

    suspend fun getTopMerchants(limit: Int): List<String> = dao.getTopMerchants(limit)

    suspend fun getExpenseCategoriesOnce(): List<String> = dao.getExpenseCategoriesOnce()

    suspend fun getUsedTagsOnce(): List<String> = dao.getUsedTagsOnce()

    suspend fun getMerchantCounts(limit: Int): List<TransactionDao.MerchantCount> =
        dao.getMerchantCounts(limit)

    suspend fun mergeMerchants(froms: List<String>, to: String) {
        dao.mergeMerchants(froms, to)
    }

    suspend fun update(tx: Transaction) {
        dao.update(tx)
    }
}
