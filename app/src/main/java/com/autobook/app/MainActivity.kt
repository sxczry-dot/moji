package com.autobook.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        CoroutineScope(Dispatchers.IO).launch {
            com.autobook.app.service.RecurringManager.checkAndRun(applicationContext)
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
                    var lockUnlocked by androidx.compose.runtime.remember {
                        androidx.compose.runtime.mutableStateOf(false)
                    }
                    var everPaused by androidx.compose.runtime.remember {
                        androidx.compose.runtime.mutableStateOf(false)
                    }
                    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
                        if (everPaused &&
                            com.autobook.app.util.AppSettings.isAppLockEnabled(this@MainActivity)
                        ) {
                            lockUnlocked = false
                        }
                        onPauseOrDispose { everPaused = true }
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
                            if (com.autobook.app.util.AppSettings.isAppLockEnabled(this@MainActivity) &&
                                !lockUnlocked
                            ) {
                                com.autobook.app.ui.LockScreen(
                                    onUnlocked = { lockUnlocked = true }
                                )
                            } else {
                                MainScreen()
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
            quickAddRequests.value++
        }
        intent.getLongExtra("edit_tx_id", -1L).let {
            if (it > 0) editRequests.value = it
        }
        intent.getIntExtra("open_tab", -1).let {
            if (it >= 0) openTabRequests.value = it
        }
        if (intent.action == "open_notification_settings") {
            startActivity(
                android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
        when (intent.action) {
            "com.autobook.app.action.QUICK_ADD" -> quickAddRequests.value++
            "com.autobook.app.action.OPEN_LIST" -> openTabRequests.value = 0
        }
    }

    companion object {
        val quickAddRequests = kotlinx.coroutines.flow.MutableStateFlow(0)

        val editRequests = kotlinx.coroutines.flow.MutableStateFlow(-1L)

        val openTabRequests = kotlinx.coroutines.flow.MutableStateFlow(-1)
    }
}
