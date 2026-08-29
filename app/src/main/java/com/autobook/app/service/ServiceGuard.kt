package com.autobook.app.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

object ServiceGuard {

    private const val REQUEST_CODE = 400
    private const val WORK_NAME = "autobook_guard"

    fun schedule(context: Context) {
        scheduleAlarm(context)
        scheduleWorker(context)
    }

    private fun scheduleAlarm(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = System.currentTimeMillis() + 10 * 60 * 1000
        if (android.os.Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent(context))
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent(context))
        }
    }

    private fun scheduleWorker(context: Context) {
        try {
            val request = PeriodicWorkRequestBuilder<GuardWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        } catch (_: Exception) {
        }
    }

    fun isListenerEnabled(context: Context): Boolean {
        val flat = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false
        return flat.split(":").any { it.startsWith(context.packageName) }
    }

    fun requestRebind(context: Context) {
        try {
            val component = ComponentName(context, NotificationListenerService::class.java)
            android.service.notification.NotificationListenerService.requestRebind(component)
        } catch (_: Exception) {
        }
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, GuardReceiver::class.java)
        return PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

class GuardReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            if (ServiceGuard.isListenerEnabled(context)) {
                ServiceGuard.requestRebind(context)
            }
        } catch (_: Exception) {
        }
        ServiceGuard.schedule(context)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                RecurringManager.checkAndRun(context)
            } catch (_: Exception) {
            }
        }
    }
}

class GuardWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        return try {
            if (ServiceGuard.isListenerEnabled(applicationContext)) {
                ServiceGuard.requestRebind(applicationContext)
            }
            kotlinx.coroutines.runBlocking {
                RecurringManager.checkAndRun(applicationContext)
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
