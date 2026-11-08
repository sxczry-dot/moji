package com.autobook.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.autobook.app.ui.MainScreen
import com.autobook.app.ui.theme.AutoBookTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : androidx.fragment.app.FragmentActivity() {

    // 事件总线根治：Activity 与 Compose 共用同一 ViewModel，事件写入走 post*()、UI 消费后复位，
    // 不再使用 companion MutableStateFlow（StateFlow 无消费语义，是"请求只增不减/解锁被拉回 tab"类 bug 的根源）
    private val viewModel: com.autobook.app.viewmodel.MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        CoroutineScope(Dispatchers.IO).launch {
            com.autobook.app.service.RecurringManager.checkAndRun(applicationContext)
            com.autobook.app.service.BillReminder.checkAndNotify(applicationContext)
        }
        com.autobook.app.service.ServiceGuard.schedule(applicationContext)
        setContent {
            val viewModel: com.autobook.app.viewmodel.MainViewModel =
                androidx.lifecycle.viewmodel.compose.viewModel()
            val themeColor by viewModel.themeColor
                .collectAsStateWithLifecycle()
            val darkMode by viewModel.darkMode
                .collectAsStateWithLifecycle()
            val darkTheme = when (darkMode) {
                com.autobook.app.util.AppSettings.DARK_ALWAYS_LIGHT -> false
                com.autobook.app.util.AppSettings.DARK_ALWAYS_DARK -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            androidx.compose.runtime.SideEffect {
                androidx.core.view.WindowCompat.getInsetsController(
                    window, window.decorView
                ).isAppearanceLightStatusBars = !darkTheme
            }
            AutoBookTheme(
                darkTheme = darkTheme,
                seed = androidx.compose.ui.graphics.Color(themeColor)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var onboarded by androidx.compose.runtime.remember {
                        androidx.compose.runtime.mutableStateOf(
                            com.autobook.app.util.AppSettings.isOnboarded(this@MainActivity)
                        )
                    }
                    // 解锁状态用 saveable：配置变更（如深色模式切换）重建后保持解锁；
                    // 退后台/进程死亡时在 onPauseOrDispose 中重置为 false，锁不失效
                    var lockUnlocked by androidx.compose.runtime.saveable.rememberSaveable {
                        androidx.compose.runtime.mutableStateOf(false)
                    }
                    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
                        onPauseOrDispose {
                            if (!this@MainActivity.isChangingConfigurations &&
                                com.autobook.app.util.AppSettings.isAppLockEnabled(this@MainActivity)
                            ) {
                                lockUnlocked = false
                            }
                        }
                    }
                    androidx.compose.animation.AnimatedContent(
                        targetState = onboarded,
                        transitionSpec = {
                            androidx.compose.animation.fadeIn(
                                animationSpec = androidx.compose.animation.core.tween(300)
                            ) togetherWith androidx.compose.animation.fadeOut(
                                animationSpec = androidx.compose.animation.core.tween(250)
                            )
                        },
                        label = "onboarding_switch"
                    ) { done ->
                        if (done) {
                            // 锁屏用覆盖层而非替换分支：文件选择器等外部流程期间 MainScreen 组合保持存活，
                            // ActivityResult 回调不会被销毁丢弃（修复"开锁后备份/导出静默失效"）
                            androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
                                MainScreen()
                                if (com.autobook.app.util.AppSettings.isAppLockEnabled(this@MainActivity) &&
                                    !lockUnlocked
                                ) {
                                    androidx.compose.material3.Surface(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clickable(
                                                interactionSource = androidx.compose.runtime.remember {
                                                    androidx.compose.foundation.interaction.MutableInteractionSource()
                                                },
                                                indication = null,
                                                onClick = {}
                                            ),
                                        color = MaterialTheme.colorScheme.background
                                    ) {
                                        com.autobook.app.ui.LockScreen(
                                            onUnlocked = { lockUnlocked = true }
                                        )
                                    }
                                }
                            }
                        } else {
                            com.autobook.app.ui.OnboardingScreen(
                                onDone = { onboarded = true }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: android.content.Intent) {
        if (intent.getBooleanExtra("quick_add", false)) {
            viewModel.postQuickAdd()
        }
        intent.getLongExtra("edit_tx_id", -1L).let {
            if (it > 0) viewModel.postEdit(it)
        }
        intent.getIntExtra("open_tab", -1).let {
            if (it >= 0) viewModel.postOpenTab(it)
        }
        // 消费后清掉 extras：恢复备份/清空数据引起的 recreate 不会重放这些一次性事件
        intent.removeExtra("quick_add")
        intent.removeExtra("edit_tx_id")
        intent.removeExtra("open_tab")
        if (intent.action == "open_notification_settings") {
            intent.action = null
            startActivity(
                android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
        when (intent.action) {
            "com.autobook.app.action.QUICK_ADD" -> {
                viewModel.postQuickAdd()
                intent.action = null
            }
            "com.autobook.app.action.OPEN_LIST" -> {
                viewModel.postOpenTab(0)
                intent.action = null
            }
        }
    }
}
