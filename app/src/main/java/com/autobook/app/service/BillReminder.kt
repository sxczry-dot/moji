package com.autobook.app.service

import android.content.Context
import com.autobook.app.util.AppSettings
import com.autobook.app.util.BillDueParser
import com.autobook.app.util.formatAmount

/**
 * 信用卡还款提醒：解析账单短信记录的还款日，提前 3 天/1 天/当天各提醒一次。
 * 由守护任务（GuardWorker/GuardReceiver）与 App 打开时触发检查。
 */
object BillReminder {

    private const val CHANNEL_ID = "bill_due"

    private var nextId = 4001

    fun checkAndNotify(context: Context) {
        try {
            val dues = AppSettings.getBillDues(context)
            if (dues.isEmpty()) return
            val today = java.time.LocalDate.now()
            for (due in dues) {
                val days = java.time.temporal.ChronoUnit.DAYS.between(today, due.dueDate)
                if (days < 0) continue
                if (days.toInt() !in setOf(0, 1, 3)) continue
                val key = "d${due.dueDate}_d$days"
                if (AppSettings.isBillNotified(context, key)) continue
                AppSettings.markBillNotified(context, key)
                send(context, due, days)
            }
        } catch (_: Exception) {
        }
    }

    private fun send(context: Context, due: BillDueParser.BillDue, days: Long) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as android.app.NotificationManager
        val channel = android.app.NotificationChannel(
            CHANNEL_ID, "账单提醒", android.app.NotificationManager.IMPORTANCE_DEFAULT
        ).apply { setShowBadge(true) }
        manager.createNotificationChannel(channel)

        val intent = android.content.Intent(context, com.autobook.app.MainActivity::class.java)
        val pending = android.app.PendingIntent.getActivity(
            context, 0, intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        val amountText = if (due.amount > 0) "，应还 ¥${formatAmount(due.amount)}" else ""
        val text = when (days) {
            0L -> "信用卡今天到期还款$amountText，别忘了"
            1L -> "信用卡明天到期还款$amountText"
            else -> "信用卡还有 $days 天到期还款$amountText"
        }
        val notification = androidx.core.app.NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("默记 · 还款提醒")
            .setContentText(text)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        try {
            androidx.core.app.NotificationManagerCompat.from(context).notify(nextId++, notification)
        } catch (_: SecurityException) {
        }
    }
}
