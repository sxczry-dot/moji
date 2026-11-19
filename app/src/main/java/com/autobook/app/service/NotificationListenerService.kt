package com.autobook.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.autobook.app.MainActivity
import com.autobook.app.data.Transaction
import com.autobook.app.parser.NotificationParser
import com.autobook.app.util.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationListenerService : android.service.notification.NotificationListenerService() {

    companion object {
        private const val CHANNEL_ID = "persistent_status"
        private const val NOTIFICATION_ID = 3001
        private const val ALERT_CHANNEL_ID = "disconnect_alert"
        private const val ALERT_NOTIFICATION_ID = 3002
        /** 补偿扫描节流间隔：onListenerConnected 可能频繁触发（重绑/重启），10 分钟内只扫一次 */
        private const val CATCH_UP_THROTTLE_MS = 10 * 60 * 1000L
        /** 只补扫 24 小时内的通知，更早的通知即使漏了也不补（避免老通知突然入账） */
        private const val CATCH_UP_MAX_AGE_MS = 24 * 60 * 60 * 1000L

        @Volatile
        var isConnected = false

        @Volatile
        private var lastCatchUpAt = 0L
    }

    private val disconnectHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val disconnectAlertRunnable = Runnable {
        if (!isConnected && ServiceGuard.isListenerEnabled(this)) {
            sendDisconnectAlert()
        }
    }

    override fun onListenerConnected() {
        isConnected = true
        disconnectHandler.removeCallbacks(disconnectAlertRunnable)
        updatePersistentNotification()
        catchUpMissedNotifications()
    }

    /**
     * 漏记补偿：监听重连时扫描通知栏里仍存活的通知，补处理断开期间漏掉的支付通知。
     * 已入账的会被去重窗口天然挡掉，未匹配的只进日志，不会重复入账。
     */
    private fun catchUpMissedNotifications() {
        val now = System.currentTimeMillis()
        if (now - lastCatchUpAt < CATCH_UP_THROTTLE_MS) return
        lastCatchUpAt = now
        val active = try {
            activeNotifications?.toList() ?: return
        } catch (e: Exception) {
            android.util.Log.e("NotificationListener", "activeNotifications failed", e)
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            for (sbn in active) {
                val age = now - sbn.postTime
                if (age < 0 || age >= CATCH_UP_MAX_AGE_MS) continue
                try {
                    processNotification(sbn, catchUp = true)
                } catch (e: Exception) {
                    android.util.Log.e("NotificationListener", "catchUp failed", e)
                }
            }
        }
    }

    override fun onListenerDisconnected() {
        isConnected = false
        disconnectHandler.removeCallbacks(disconnectAlertRunnable)
        disconnectHandler.postDelayed(disconnectAlertRunnable, 10_000)
        // 系统会在进程恢复后自动重连；主动请求一次重绑加速恢复
        ServiceGuard.requestRebind(this)
    }

    private fun sendDisconnectAlert() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            ALERT_CHANNEL_ID,
            "服务提醒",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { setShowBadge(false) }
        manager.createNotificationChannel(channel)

        val intent = Intent(this, MainActivity::class.java).apply {
            action = "open_notification_settings"
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val pending = PendingIntent.getActivity(
            this, 1, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("自动记账已暂停")
            .setContentText("通知监听被系统断开，点按前往重新开启")
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(this).notify(ALERT_NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
        }
    }

    private fun updatePersistentNotification() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (!AppSettings.isPersistentNotification(this)) {
            manager.cancel(NOTIFICATION_ID)
            return
        }

        val channel = NotificationChannel(
            CHANNEL_ID,
            "运行状态",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)

        val intent = Intent(this, MainActivity::class.java)
        val pending = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("默记运行中")
            .setContentText("自动记账正在后台工作，点按打开")
            .setContentIntent(pending)
            .setOngoing(true)
            .setSilent(true)
            .build()

        // 普通常驻通知即可，无需前台服务：监听服务本身由系统托管保活
        try {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        processNotification(sbn, catchUp = false)
    }

    private fun processNotification(sbn: StatusBarNotification, catchUp: Boolean) {
        val pkg = sbn.packageName
        val bankName = NotificationParser.bankNameOf(pkg)
        if (pkg != NotificationParser.WECHAT_PKG &&
            pkg != NotificationParser.ALIPAY_PKG &&
            pkg != NotificationParser.UNIONPAY_PKG &&
            pkg != NotificationParser.JD_PKG &&
            bankName == null
        ) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
        // BigTextStyle 的正文在 BIG_TEXT 里（展开后全文常比 TEXT 更完整）；
        // InboxStyle 的多行在 TEXT_LINES 里——不读会漏掉银行/支付 App 的交易正文
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?.mapNotNull { it?.toString() }
            ?.filter { it.isNotBlank() }
            ?.joinToString(" ")
        val combinedText = listOfNotNull(text, bigText, textLines, subText)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifEmpty { null }

        // 补偿扫描：交易时间按通知发布时间计，让漏记补回的账目落在正确的时间点，
        // 也让去重窗口与实时到达时的表现一致
        val parsed = NotificationParser.parse(pkg, title, combinedText)?.let { p ->
            if (catchUp && p.tradeTime <= 0L) p.copy(tradeTime = sbn.postTime) else p
        }
        val full = "${title ?: ""} ${combinedText ?: ""}"

        val time = com.autobook.app.util.TimeUtils.formatShortTime(System.currentTimeMillis())
        val catchUpTag = if (catchUp) "（补偿）" else ""
        val channel = when {
            pkg == NotificationParser.WECHAT_PKG -> "微信"
            pkg == NotificationParser.ALIPAY_PKG -> "支付宝"
            pkg == NotificationParser.UNIONPAY_PKG -> "云闪付"
            pkg == NotificationParser.JD_PKG -> "京东"
            else -> bankName ?: pkg
        } + catchUpTag
        val safeText = (combinedText ?: "").replace("\\n", " ").take(60)

        if (parsed == null) {
            // 补偿扫描只关心可入账的支付通知；未匹配的不刷日志（补偿扫到的多半是旧通知，刷屏没意义）
            if (catchUp) return
            val reason = com.autobook.app.util.NotifDiagnose.reasonOf(full)
            val result = if (reason != null) "未匹配（$reason）" else "未匹配"
            // SharedPreferences 读写挪到 IO 线程，避免主线程卡顿
            CoroutineScope(Dispatchers.IO).launch {
                AppSettings.addNotifLog(this@NotificationListenerService, "$time | $channel | $result | $safeText")
            }
            return
        }

        val source = when {
            pkg == NotificationParser.WECHAT_PKG -> Transaction.SOURCE_WECHAT
            pkg == NotificationParser.ALIPAY_PKG -> Transaction.SOURCE_ALIPAY
            pkg == NotificationParser.UNIONPAY_PKG -> Transaction.SOURCE_UNIONPAY
            pkg == NotificationParser.JD_PKG -> Transaction.SOURCE_JD
            else -> bankName ?: pkg
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val learned = com.autobook.app.util.AppSettings.getLearnedCategory(applicationContext, parsed.merchant)
                when (TransactionSaver.save(applicationContext, parsed, source)) {
                    TransactionSaver.Outcome.Inserted -> {
                        val tag = if (learned != null) "（学习分类）" else ""
                        AppSettings.addNotifLog(
                            applicationContext,
                            "$time | $channel | 已记账 ${com.autobook.app.util.formatAmount(parsed.amount)} · ${parsed.merchant}$tag | $safeText"
                        )
                    }
                    TransactionSaver.Outcome.Blocked -> {
                        AppSettings.addNotifLog(
                            applicationContext,
                            "$time | $channel | 已屏蔽（命中屏蔽词）| $safeText"
                        )
                    }
                    TransactionSaver.Outcome.Duplicate -> Unit
                    TransactionSaver.Outcome.Paused -> Unit
                    TransactionSaver.Outcome.Failed -> {
                        AppSettings.addNotifLog(
                            applicationContext,
                            "$time | $channel | 保存失败 | $safeText"
                        )
                    }
                }
            } catch (e: Exception) {
                // 保存链路异常兜底：与 SmsReceiver 对齐，避免异常带崩进程中断监听
                android.util.Log.e("NotificationListener", "save failed", e)
                try {
                    AppSettings.addNotifLog(
                        applicationContext,
                        "$time | $channel | 保存失败 | $safeText"
                    )
                } catch (_: Exception) {
                }
            }
        }
    }
}
