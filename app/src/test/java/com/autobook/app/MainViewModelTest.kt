package com.autobook.app

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.autobook.app.data.Transaction
import com.autobook.app.viewmodel.MainViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** MainViewModel 状态测试：事件消费语义、账本过滤、手动入账参数保真 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = AutoBookApp::class)
class MainViewModelTest {

    private lateinit var app: Application
    private lateinit var vm: MainViewModel

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext()
        vm = MainViewModel(app)
    }

    @After
    fun tearDown() {
        runBlocking {
            app.getSharedPreferences("autobook_settings", 0).edit().clear().commit()
        }
    }

    @Test
    fun `openTab 事件消费语义`() = runTest {
        assertEquals(-1, vm.openTabRequest.first())
        vm.postOpenTab(1)
        assertEquals(1, vm.openTabRequest.first())
        vm.consumeOpenTab()
        assertEquals(-1, vm.openTabRequest.first())
    }

    @Test
    fun `quickAdd 事件只触发一次`() = runTest {
        assertEquals(0, vm.quickAddRequest.first())
        vm.postQuickAdd()
        vm.postQuickAdd()
        assertEquals(2, vm.quickAddRequest.first())
        vm.consumeQuickAdd()
        assertEquals(0, vm.quickAddRequest.first())
    }

    @Test
    fun `edit 事件消费语义`() = runTest {
        assertEquals(-1L, vm.editRequest.first())
        vm.postEdit(42L)
        assertEquals(42L, vm.editRequest.first())
        vm.consumeEdit()
        assertEquals(-1L, vm.editRequest.first())
    }

    @Test
    fun `addManual 保留账本来源单号`() = runTest {
        val now = System.currentTimeMillis()
        vm.addManual(
            amount = 25.0,
            type = Transaction.TYPE_EXPENSE,
            category = "餐饮美食",
            merchant = "测试商户",
            tags = "",
            timestamp = now,
            book = "我的账本",
            source = "微信",
            refNo = "REF123"
        )
        // addManual 在 viewModelScope(IO) 异步写库，用真实时间轮询等待落库
        val saved = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            kotlinx.coroutines.withTimeout(5000L) {
                var found: Transaction? = null
                while (found == null) {
                    found = AutoBookApp.instance.repository.getAllOnce()
                        .firstOrNull { it.merchant == "测试商户" }
                    if (found == null) kotlinx.coroutines.delay(20)
                }
                found
            }
        }
        assertEquals("我的账本", saved!!.book)
        assertEquals("微信", saved.source)
        assertEquals("REF123", saved.refNo)
        assertEquals(25.0, saved.amount, 0.001)
    }

    @Test
    fun `切换账本后状态流立即更新`() = runTest {
        assertEquals("默认账本", vm.currentBook.first())
        vm.setCurrentBook("测试账本")
        assertEquals("测试账本", vm.currentBook.first())
    }
}
