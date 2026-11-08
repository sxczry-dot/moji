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

    fun checkAndNotify(context: Context) {
        try {
            // 无通知权限时不标记"已提醒"（下个 tick 重试），避免提醒被静默吞掉
            if (!com.autobook.app.util.Permissions.isNotifGranted(context)) return
            val dues = AppSettings.getBillDues(context)
            if (dues.isEmpty()) return
            val today = java.time.LocalDate.now()
            for (due in dues) {
                val days = java.time.temporal.ChronoUnit.DAYS.between(today, due.dueDate)
                if (days < 0) continue
                if (days.toInt() !in setOf(0, 1, 3)) continue
                val key = "d${due.dueDate}_${due.cardTail}_d$days"
                if (AppSettings.isBillNotified(context, key)) continue
                if (send(context, due, days)) {
                    AppSettings.markBillNotified(context, key)
                }
            }
        } catch (_: Exception) {
        }
    }

    /** 发送成功返回 true 才允许落"已提醒"标记 */
    private fun send(context: Context, due: BillDueParser.BillDue, days: Long): Boolean {
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
        return try {
            // 通知 id 由还款日+卡尾号派生：多路并发 tick 不会撞 id 互相覆盖
            val notifyId = 4001 + (due.dueDate.toEpochDay() * 31 + due.cardTail.hashCode()).toInt() and 0xFFFF
            androidx.core.app.NotificationManagerCompat.from(context).notify(notifyId, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }
}
