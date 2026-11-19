package com.autobook.app

import androidx.test.core.app.ApplicationProvider
import com.autobook.app.data.AppDatabase
import com.autobook.app.data.Transaction
import com.autobook.app.service.RecurringManager
import com.autobook.app.util.AppSettings
import java.util.Calendar
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** 周期记账对账：已有同额同商户真实记录时跳过，避免与自动记账重复入账 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = AutoBookApp::class)
class RecurringManagerTest {

    private val context: AutoBookApp
        get() = ApplicationProvider.getApplicationContext()

    private val dao by lazy { AppDatabase.get(context).transactionDao() }

    private val todayRule = AppSettings.RecurringRule(
        id = 12345L,
        name = "爱奇艺会员",
        amount = 25.0,
        type = Transaction.TYPE_EXPENSE,
        category = "娱乐",
        day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH),
        merchant = "爱奇艺",
        period = 0,
        book = AppSettings.DEFAULT_BOOK
    )

    @Before
    fun setup() = runBlocking {
        dao.deleteAll()
        context.getSharedPreferences("autobook_settings", 0).edit().clear().commit()
        AppSettings.saveRecurringRules(context, listOf(todayRule))
    }

    @Test
    fun `已有同额同商户记录时跳过入账`() = runBlocking {
        val now = System.currentTimeMillis()
        // 模拟支付通知已自动入账的同额扣款
        dao.insert(
            Transaction(
                amount = 25.0, type = Transaction.TYPE_EXPENSE, category = "娱乐",
                merchant = "爱奇艺", source = Transaction.SOURCE_WECHAT,
                timestamp = now, localTimestamp = now, insertedAt = now,
                book = AppSettings.DEFAULT_BOOK
            )
        )
        RecurringManager.checkAndRun(context)
        val all = dao.getAllOnce()
        assertEquals(1, all.size)
        assertTrue(all.none { it.source == Transaction.SOURCE_RECURRING })
    }

    @Test
    fun `无对应记录时规则正常入账`() = runBlocking {
        RecurringManager.checkAndRun(context)
        val recurring = dao.getAllOnce().filter { it.source == Transaction.SOURCE_RECURRING }
        assertEquals(1, recurring.size)
        assertEquals(25.0, recurring[0].amount, 0.001)
        assertEquals("爱奇艺", recurring[0].merchant)
    }

    @Test
    fun `商户名不匹配的同额记录不阻止入账`() = runBlocking {
        val now = System.currentTimeMillis()
        // 同额但商户完全不同：是另一笔真实消费，不能当成周期扣款
        dao.insert(
            Transaction(
                amount = 25.0, type = Transaction.TYPE_EXPENSE, category = "餐饮",
                merchant = "美团外卖", source = Transaction.SOURCE_WECHAT,
                timestamp = now, localTimestamp = now, insertedAt = now,
                book = AppSettings.DEFAULT_BOOK
            )
        )
        RecurringManager.checkAndRun(context)
        val recurring = dao.getAllOnce().filter { it.source == Transaction.SOURCE_RECURRING }
        assertEquals(1, recurring.size)
    }

    @Test
    fun `通知没解析出商户名时同额当日记录也跳过`() = runBlocking {
        val now = System.currentTimeMillis()
        // 通知入账但商户名提取失败（用渠道名"微信"兜底）：同额同方向当日，视为周期扣款已捕获
        dao.insert(
            Transaction(
                amount = 25.0, type = Transaction.TYPE_EXPENSE, category = "其他",
                merchant = Transaction.SOURCE_WECHAT, source = Transaction.SOURCE_WECHAT,
                timestamp = now, localTimestamp = now, insertedAt = now,
                book = AppSettings.DEFAULT_BOOK
            )
        )
        RecurringManager.checkAndRun(context)
        val all = dao.getAllOnce()
        assertEquals(1, all.size)
        assertTrue(all.none { it.source == Transaction.SOURCE_RECURRING })
    }
}
