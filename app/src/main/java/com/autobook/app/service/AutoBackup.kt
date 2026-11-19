package com.autobook.app.service

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.autobook.app.AutoBookApp
import com.autobook.app.data.BackupCodec
import com.autobook.app.util.AppSettings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 自动周期备份：用户选定一个 SAF 目录后，每 7 天静默写入一份完整备份。
 * 由 GuardWorker（15 分钟周期）触发检查；目录失效（被删/拔卡/授权被撤）时自动停用。
 */
object AutoBackup {

    const val INTERVAL_MS = 7L * 24 * 60 * 60 * 1000
    private const val KEEP_COUNT = 8

    /** 到期检查并执行；返回本次是否写入了新备份 */
    suspend fun checkAndRun(context: Context, now: Long = System.currentTimeMillis()): Boolean {
        val treeUri = AppSettings.getBackupTreeUri(context) ?: return false
        if (now - AppSettings.getLastBackupAt(context) < INTERVAL_MS) return false
        return try {
            val root = DocumentFile.fromTreeUri(context, Uri.parse(treeUri))
            if (root == null || !root.canWrite()) {
                // 目录不可用：停用自动备份，避免每次守护任务都白跑
                AppSettings.setBackupTreeUri(context, null)
                return false
            }
            val data = BackupCodec.encode(
                AppSettings.exportAllSettings(context),
                AutoBookApp.instance.repository.getAllOnce()
            ).toByteArray(Charsets.UTF_8)
            val name = "默记自动备份_" +
                SimpleDateFormat("yyyyMMdd_HHmm", Locale.CHINA).format(Date(now)) + ".abk"
            // 先写临时名再改名：写入中断不会留下半截"看似正常"的备份文件
            val tmp = root.createFile("application/octet-stream", "$name.tmp") ?: return false
            val ok = context.contentResolver.openOutputStream(tmp.uri)?.use { out ->
                out.write(data)
                true
            } ?: false
            if (ok && tmp.renameTo(name)) {
                pruneOld(root)
                AppSettings.setLastBackupAt(context, now)
                true
            } else {
                tmp.delete()
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("AutoBackup", "auto backup failed", e)
            false
        }
    }

    /** 只保留最近 KEEP_COUNT 份自动备份，防止无限堆积占空间 */
    private fun pruneOld(root: DocumentFile) {
        try {
            val backups = root.listFiles()
                .filter { it.name?.startsWith("默记自动备份_") == true && it.name!!.endsWith(".abk") }
                .sortedByDescending { it.name }
            backups.drop(KEEP_COUNT).forEach { it.delete() }
        } catch (_: Exception) {
        }
    }
}
