package com.autobook.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert
    suspend fun insert(tx: Transaction): Long

    @Insert
    suspend fun insertAll(txs: List<Transaction>)

    /** CSV 导入：补到达时间 + 空来源兜底，让导入记录参与自动记账的去重候选池 */
    @androidx.room.Transaction
    suspend fun insertAllImported(txs: List<Transaction>) {
        val now = System.currentTimeMillis()
        insertAll(txs.map { it.copy(insertedAt = now, source = it.source.ifEmpty { "导入" }) })
    }

    /** 备份恢复：单事务内完成删+插，避免中途失败造成全部数据丢失 */
    @androidx.room.Transaction
    suspend fun replaceAll(txs: List<Transaction>) {
        val now = System.currentTimeMillis()
        deleteAll()
        insertAll(txs.map { it.copy(insertedAt = now, source = it.source.ifEmpty { "导入" }) })
    }

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    suspend fun getAllOnce(): List<Transaction>

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Delete
    suspend fun delete(tx: Transaction)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): Transaction?

    @Update
    suspend fun update(tx: Transaction)

    @Query("UPDATE transactions SET book = :to WHERE book = :from")
    suspend fun moveBook(from: String, to: String)

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAll(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE book = :book AND timestamp >= :start AND timestamp < :end ORDER BY timestamp DESC")
    fun getByRange(book: String, start: Long, end: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE book = :book AND timestamp >= :start AND timestamp < :end")
    suspend fun getByRangeOnce(book: String, start: Long, end: Long): List<Transaction>

    @Query("SELECT * FROM transactions WHERE insertedAt >= :start ORDER BY insertedAt DESC")
    suspend fun getRecent(start: Long): List<Transaction>

    @Query(
        "SELECT merchant FROM transactions WHERE merchant != '' " +
            "GROUP BY merchant ORDER BY COUNT(*) DESC LIMIT :limit"
    )
    suspend fun getTopMerchants(limit: Int): List<String>

    @Query("SELECT category FROM transactions WHERE type = 0 AND category != ''")
    suspend fun getExpenseCategoriesOnce(): List<String>

    data class MerchantCount(val merchant: String, val count: Int)

    @Query(
        "SELECT merchant, COUNT(*) AS count FROM transactions WHERE merchant != '' " +
            "GROUP BY merchant ORDER BY count DESC LIMIT :limit"
    )
    suspend fun getMerchantCounts(limit: Int): List<MerchantCount>

    @Query("UPDATE transactions SET merchant = :to WHERE merchant IN (:froms)")
    suspend fun mergeMerchants(froms: List<String>, to: String)

    @Query("SELECT tags FROM transactions WHERE tags != ''")
    suspend fun getUsedTagsOnce(): List<String>

    @androidx.room.Transaction
    suspend fun insertAndGetMonth(tx: Transaction, now: Long): SaveResult {
        val txWithArrival = tx.copy(insertedAt = now)
        val recent = getRecent(now - DedupChecker.DEDUP_WINDOW_MS)
        val dup = recent.firstOrNull { DedupChecker.isDuplicateOf(it, txWithArrival) }
        if (dup != null) return SaveResult.Duplicate(dup)
        val id = insert(txWithArrival)
        val (start, end) = com.autobook.app.util.TimeUtils.monthRangeOf(now)
        return SaveResult.Inserted(id, getByRangeOnce(tx.book, start, end))
    }

}


sealed class SaveResult {
    data class Inserted(val txId: Long, val monthTxs: List<Transaction>) : SaveResult()
    data class Duplicate(val existing: Transaction) : SaveResult()
}
