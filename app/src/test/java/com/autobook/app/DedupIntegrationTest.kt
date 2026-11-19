package com.autobook.app

import androidx.test.core.app.ApplicationProvider
import com.autobook.app.data.AppDatabase
import com.autobook.app.data.SaveResult
import com.autobook.app.data.Transaction
import com.autobook.app.util.AppSettings
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** 去重候选池集成测试：24h 候选窗口能捕到延迟到达的跨渠道记录，但不误吞真实交易 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = AutoBookApp::class)
class DedupIntegrationTest {

    private val dao by lazy {
        AppDatabase.get(ApplicationProvider.getApplicationContext()).transactionDao()
    }

    @Before
    fun setup() = runBlocking { dao.deleteAll() }

    @Test
    fun `延迟到达的跨渠道记录仍被判重`() = runBlocking {
        val now = System.currentTimeMillis()
        val t0 = now - 20 * 3600_000L
        // 20 小时前到达的微信记录（超出旧 15 分钟候选池，但在 24h 新候选池内）
        dao.insert(
            Transaction(
                amount = 45.0, type = Transaction.TYPE_EXPENSE, category = "餐饮",
                merchant = "美团", source = Transaction.SOURCE_WECHAT,
                timestamp = t0, localTimestamp = t0, insertedAt = t0,
                book = AppSettings.DEFAULT_BOOK
            )
        )
        // 同一笔交易的银行短信晚到 20 小时：交易时间差 60s、同额同商户，应判重合并
        val late = Transaction(
            amount = 45.0, type = Transaction.TYPE_EXPENSE, category = "",
            merchant = "美团", source = Transaction.SOURCE_SMS,
            timestamp = t0 + 60_000, localTimestamp = t0 + 60_000,
            book = AppSettings.DEFAULT_BOOK
        )
        val result = dao.insertAndGetMonth(late, now)
        assertTrue(result is SaveResult.Duplicate)
    }

    @Test
    fun `候选池放宽不误吞真实交易`() = runBlocking {
        val now = System.currentTimeMillis()
        val t0 = now - 20 * 3600_000L
        dao.insert(
            Transaction(
                amount = 45.0, type = Transaction.TYPE_EXPENSE, category = "餐饮",
                merchant = "美团", source = Transaction.SOURCE_WECHAT,
                timestamp = t0, localTimestamp = t0, insertedAt = t0,
                book = AppSettings.DEFAULT_BOOK
            )
        )
        // 同渠道同商户同金额但间隔 10 分钟：超出同渠道判重窗口，是两笔真实交易，应正常入账
        val second = Transaction(
            amount = 45.0, type = Transaction.TYPE_EXPENSE, category = "",
            merchant = "美团", source = Transaction.SOURCE_WECHAT,
            timestamp = t0 + 10 * 60_000, localTimestamp = t0 + 10 * 60_000,
            book = AppSettings.DEFAULT_BOOK
        )
        val result = dao.insertAndGetMonth(second, now)
        assertTrue(result is SaveResult.Inserted)
    }
}
