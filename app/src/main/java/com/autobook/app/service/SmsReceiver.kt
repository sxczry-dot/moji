package com.autobook.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.autobook.app.data.Transaction
import com.autobook.app.parser.SmsParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        val sender = messages.firstOrNull()?.originatingAddress ?: return
        val body = messages.joinToString("") { it.messageBody ?: "" }

        val parsed = SmsParser.parse(sender, body)

        val time = com.autobook.app.util.TimeUtils.formatShortTime(System.currentTimeMillis())
        val safeBody = body.replace("\\n", " ").take(60)

        if (parsed == null) {
            val reason = com.autobook.app.util.NotifDiagnose.reasonOf(body)
            val result = if (reason != null) "未匹配（$reason）" else "未匹配"
            com.autobook.app.util.AppSettings.addNotifLog(
                context,
                "$time | 短信 | $result | $safeBody"
            )
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val learned = com.autobook.app.util.AppSettings.getLearnedCategory(context, parsed.merchant)
                when (TransactionSaver.save(context, parsed, Transaction.SOURCE_SMS)) {
                    TransactionSaver.Outcome.Inserted -> {
                        val tag = if (learned != null) "（学习分类）" else ""
                        com.autobook.app.util.AppSettings.addNotifLog(
                            context,
                            "$time | 短信 | 已记账 ${com.autobook.app.util.formatAmount(parsed.amount)} · ${parsed.merchant}$tag | $safeBody"
                        )
                    }
                    TransactionSaver.Outcome.Blocked -> {
                        com.autobook.app.util.AppSettings.addNotifLog(
                            context,
                            "$time | 短信 | 已屏蔽（命中屏蔽词）| $safeBody"
                        )
                    }
                    TransactionSaver.Outcome.Duplicate -> Unit
                    TransactionSaver.Outcome.Paused -> Unit
                    TransactionSaver.Outcome.Failed -> {
                        com.autobook.app.util.AppSettings.addNotifLog(
                            context,
                            "$time | 短信 | 保存失败 | $safeBody"
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SmsReceiver", "save failed", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
