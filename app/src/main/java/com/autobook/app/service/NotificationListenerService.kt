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

        @Volatile
        var isConnected = false
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
    }

    override fun onListenerDisconnected() {
        isConnected = false
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (_: Exception) {
        }
        disconnectHandler.removeCallbacks(disconnectAlertRunnable)
        disconnectHandler.postDelayed(disconnectAlertRunnable, 10_000)
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
            this, 0, intent,
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
        if (!AppSettings.isPersistentNotification(this)) return

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
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

        try {
            startForeground(NOTIFICATION_ID, notification)
        } catch (_: Exception) {
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
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
        val combinedText = listOfNotNull(text, subText)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifEmpty { null }

        val parsed = NotificationParser.parse(pkg, title, combinedText)
        val full = "${title ?: ""} ${combinedText ?: ""}"

        val time = com.autobook.app.util.TimeUtils.formatShortTime(System.currentTimeMillis())
        val channel = when {
            pkg == NotificationParser.WECHAT_PKG -> "微信"
            pkg == NotificationParser.ALIPAY_PKG -> "支付宝"
            pkg == NotificationParser.UNIONPAY_PKG -> "云闪付"
            pkg == NotificationParser.JD_PKG -> "京东"
            else -> bankName ?: pkg
        }
        val safeText = (combinedText ?: "").replace("\\n", " ").take(60)

        if (parsed == null) {
            val reason = com.autobook.app.util.NotifDiagnose.reasonOf(full)
            val result = if (reason != null) "未匹配（$reason）" else "未匹配"
            AppSettings.addNotifLog(this, "$time | $channel | $result | $safeText")
            return
        }

        val source = when {
            pkg == NotificationParser.WECHAT_PKG -> Transaction.SOURCE_WECHAT
            pkg == NotificationParser.ALIPAY_PKG -> Transaction.SOURCE_ALIPAY
            pkg == NotificationParser.UNIONPAY_PKG -> Transaction.SOURCE_UNIONPAY
            pkg == NotificationParser.JD_PKG -> "京东"
            else -> bankName ?: pkg
        }

        CoroutineScope(Dispatchers.IO).launch {
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
        }
    }
}
