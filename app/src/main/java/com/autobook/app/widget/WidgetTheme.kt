package com.autobook.app.widget

object WidgetTheme {

    fun backgroundResource(context: android.content.Context): Int {
        val seed = com.autobook.app.util.AppSettings.getThemeColor(context)
        val dark = isDarkBackground(context)
        return when (seed) {
            0xFF00B578 -> if (dark) com.autobook.app.R.drawable.widget_bg_green_dark
            else com.autobook.app.R.drawable.widget_bg_green

            0xFF3B82F6 -> if (dark) com.autobook.app.R.drawable.widget_bg_blue_dark
            else com.autobook.app.R.drawable.widget_bg_blue

            0xFFF59E0B -> if (dark) com.autobook.app.R.drawable.widget_bg_orange_dark
            else com.autobook.app.R.drawable.widget_bg_orange

            0xFF8B5CF6 -> if (dark) com.autobook.app.R.drawable.widget_bg_purple_dark
            else com.autobook.app.R.drawable.widget_bg_purple

            0xFFEF4444 -> if (dark) com.autobook.app.R.drawable.widget_bg_red_dark
            else com.autobook.app.R.drawable.widget_bg_red

            0xFF06B6D4 -> if (dark) com.autobook.app.R.drawable.widget_bg_cyan_dark
            else com.autobook.app.R.drawable.widget_bg_cyan

            else -> -1
        }
    }

    fun backgroundColor(context: android.content.Context): Int {
        val seed = com.autobook.app.util.AppSettings.getThemeColor(context)
        val r = ((seed shr 16) and 0xFFL).toInt()
        val g = ((seed shr 8) and 0xFFL).toInt()
        val b = (seed and 0xFFL).toInt()
        val factor = if (isDarkBackground(context)) 0.62f else 1f
        return android.graphics.Color.rgb(
            (r * factor).toInt(),
            (g * factor).toInt(),
            (b * factor).toInt()
        )
    }

    fun isDarkBackground(context: android.content.Context): Boolean {
        return com.autobook.app.util.AppSettings.isDarkModeActive(context)
    }

    fun refreshAll(context: android.content.Context) {
        val mgr = android.appwidget.AppWidgetManager.getInstance(context)
        listOf(
            com.autobook.app.widget.QuickAddWidgetProvider::class.java,
            com.autobook.app.widget.BudgetWidgetProvider::class.java
        ).forEach { cls ->
            val ids = mgr.getAppWidgetIds(android.content.ComponentName(context, cls))
            if (ids.isNotEmpty()) {
                val intent = android.content.Intent(context, cls).apply {
                    action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                context.sendBroadcast(intent)
            }
        }
    }
}
