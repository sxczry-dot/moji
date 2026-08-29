package com.autobook.app

import android.app.Application
import com.autobook.app.data.TransactionRepository

class AutoBookApp : Application() {

    val repository: TransactionRepository by lazy {
        TransactionRepository(this)
    }

    companion object {
        lateinit var instance: AutoBookApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        com.autobook.app.util.AppSettings.migrateCategoryNames(this)
        installCrashLogger()
    }

    private fun installCrashLogger() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val dir = getExternalFilesDir(null) ?: filesDir
                val file = java.io.File(dir, "crash_log.txt")
                val text =
                    java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.CHINA)
                        .format(java.util.Date()) +
                        "\n" + android.util.Log.getStackTraceString(throwable) + "\n\n"
                file.appendText(text)
                if (file.length() > 512 * 1024) {
                    file.writeText(text)
                }
            } catch (_: Exception) {
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
