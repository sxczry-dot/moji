package com.autobook.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.autobook.app.MainActivity
import com.autobook.app.R

class QuickAddWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val bgRes = WidgetTheme.backgroundResource(context)
        val bgColor = WidgetTheme.backgroundColor(context)
        for (id in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_quick)
            if (bgRes > 0) {
                views.setInt(R.id.widget_quick_root, "setBackgroundResource", bgRes)
            } else {
                views.setInt(R.id.widget_quick_root, "setBackgroundColor", bgColor)
            }
            val intent = Intent(context, MainActivity::class.java).apply {
                putExtra("quick_add", true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            val pending = PendingIntent.getActivity(
                context, 100, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_quick_root, pending)
            appWidgetManager.updateAppWidget(id, views)
        }
    }
}
