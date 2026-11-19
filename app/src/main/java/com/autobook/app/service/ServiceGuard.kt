package com.autobook.app.service

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

object ServiceGuard {

    private const val WORK_NAME = "autobook_guard"

    fun schedule(context: Context) {
        scheduleWorker(context)
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
        return flat.split(":").any { it.substringBefore('/') == context.packageName }
    }

    fun requestRebind(context: Context) {
        try {
            val component = ComponentName(context, NotificationListenerService::class.java)
            android.service.notification.NotificationListenerService.requestRebind(component)
        } catch (_: Exception) {
        }
    }
}

class GuardWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        return try {
            // 仅监听断开时 requestRebind，健康连接不做无谓的解绑重连
            if (ServiceGuard.isListenerEnabled(applicationContext) &&
                !NotificationListenerService.isConnected
            ) {
                ServiceGuard.requestRebind(applicationContext)
            }
            kotlinx.coroutines.runBlocking {
                RecurringManager.checkAndRun(applicationContext)
                BillReminder.checkAndNotify(applicationContext)
                AutoBackup.checkAndRun(applicationContext)
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
