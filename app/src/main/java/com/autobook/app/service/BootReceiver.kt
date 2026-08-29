package com.autobook.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        ServiceGuard.schedule(context)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                RecurringManager.checkAndRun(context)
            } catch (_: Exception) {
            }
        }
    }
}
