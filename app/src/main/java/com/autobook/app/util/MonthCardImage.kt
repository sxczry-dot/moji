package com.autobook.app.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.os.Build
import android.provider.MediaStore
import com.autobook.app.data.Categories
import com.autobook.app.data.Transaction
import com.autobook.app.data.expenseSum
import com.autobook.app.data.incomeSum
import java.time.YearMonth

object MonthCardImage {

    const val WIDTH = 1080
    const val HEIGHT = 1440

    fun render(
        context: Context,
        yearMonth: YearMonth,
        txs: List<Transaction>,
        yearMode: Boolean = false
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val seed = AppSettings.getThemeColor(context)
        val baseColor = Color.rgb(
            ((seed shr 16) and 0xFFL).toInt(),
            ((seed shr 8) and 0xFFL).toInt(),
            (seed and 0xFFL).toInt()
        )
        val darkActive = AppSettings.isDarkModeActive(context)
        val topFactor = if (darkActive) 0.42f else 1f
        val bottomFactor = if (darkActive) 0.18f else 0.62f
        val topColor = Color.rgb(
            (Color.red(baseColor) * topFactor).toInt(),
            (Color.green(baseColor) * topFactor).toInt(),
            (Color.blue(baseColor) * topFactor).toInt()
        )
        val bottomColor = Color.rgb(
            (Color.red(baseColor) * bottomFactor).toInt(),
            (Color.green(baseColor) * bottomFactor).toInt(),
            (Color.blue(baseColor) * bottomFactor).toInt()
        )
        canvas.drawRect(
            0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(),
            Paint().apply {
                shader = LinearGradient(
                    0f, 0f, 0f, HEIGHT.toFloat(),
                    topColor, bottomColor, Shader.TileMode.CLAMP
                )
            }
        )

        val white = Paint().apply { color = Color.WHITE }
        val whiteDim = Paint().apply { color = Color.WHITE; alpha = 165 }
        val whiteDim2 = Paint().apply { color = Color.WHITE; alpha = 110 }

        white.textSize = 42f
        white.isFakeBoldText = true
        white.textAlign = Paint.Align.CENTER
        val titleText = if (yearMode) {
            "默记 · ${yearMonth.year}年度账单"
        } else {
            "默记 · ${yearMonth.year}年${yearMonth.monthValue}月账单"
        }
        canvas.drawText(titleText, WIDTH / 2f, 120f, white)

        val expense = txs.expenseSum()
        val income = txs.incomeSum()
        val balance = income - expense

        whiteDim.textSize = 36f
        whiteDim.textAlign = Paint.Align.CENTER
        canvas.drawText(if (yearMode) "全年结余" else "本月结余", WIDTH / 2f, 320f, whiteDim)

        val balanceText = "¥" + formatAmountGrouped(balance)
        var balanceSize = 116f
        white.textSize = balanceSize
        white.isFakeBoldText = true
        white.textAlign = Paint.Align.CENTER
        // 大额结余自动缩字号，避免超出画布被裁切
        while (white.measureText(balanceText) > WIDTH - 120f && balanceSize > 44f) {
            balanceSize -= 4f
            white.textSize = balanceSize
        }
        canvas.drawText(balanceText, WIDTH / 2f, 470f, white)

        whiteDim.textSize = 40f
        whiteDim.textAlign = Paint.Align.LEFT
        canvas.drawText("支出  ¥" + formatAmountGrouped(expense), 140f, 640f, whiteDim)
        whiteDim.textAlign = Paint.Align.RIGHT
        canvas.drawText("收入  ¥" + formatAmountGrouped(income), WIDTH - 140f, 640f, whiteDim)

        var rankingY = 760f
        if (yearMode) {
            val topDay = txs
                .filter { it.type == Transaction.TYPE_EXPENSE }
                .groupBy { com.autobook.app.util.TimeUtils.dayStartOf(it.timestamp) }
                .maxByOrNull { (_, list) -> list.sumOf { it.amount } }
            if (topDay != null) {
                val daySum = topDay.value.sumOf { it.amount }
                val dayText = java.text.SimpleDateFormat("MM月dd日", java.util.Locale.CHINA)
                    .format(java.util.Date(topDay.key))
                whiteDim.textSize = 34f
                whiteDim.textAlign = Paint.Align.LEFT
                canvas.drawText(
                    "花钱最多的一天：$dayText（¥${formatAmountGrouped(daySum)}）",
                    140f, 700f, whiteDim
                )
                rankingY = 800f
            }
        }

        whiteDim.textSize = 36f
        whiteDim.textAlign = Paint.Align.LEFT
        canvas.drawText("支出排行", 140f, rankingY, whiteDim)

        val topCategories = txs
            .filter { it.type == Transaction.TYPE_EXPENSE }
            .groupBy { Categories.primaryOf(it.category) }
            .map { (cat, list) -> cat to list.sumOf { it.amount } }
            .sortedByDescending { it.second }
            .take(6)
        val maxCat = topCategories.firstOrNull()?.second ?: 1.0
        var y = rankingY + 80f
        topCategories.forEachIndexed { i, (cat, amount) ->
            val barColor = Color.WHITE
            val barPaint = Paint().apply {
                color = barColor
                alpha = 200 - i * 20
            }
            whiteDim2.textSize = 34f
            whiteDim2.textAlign = Paint.Align.LEFT
            canvas.drawText("${i + 1}. $cat", 140f, y, whiteDim2)
            val barLeft = 420f
            val barWidth = (amount / maxCat).toFloat() * 400f
            canvas.drawRoundRect(
                barLeft, y - 30f, barLeft + barWidth, y - 8f,
                10f, 10f, barPaint
            )
            whiteDim2.textAlign = Paint.Align.RIGHT
            canvas.drawText("¥" + formatAmountGrouped(amount), WIDTH - 140f, y, whiteDim2)
            y += 78f
        }

        whiteDim2.textSize = 30f
        whiteDim2.textAlign = Paint.Align.CENTER
        canvas.drawText(
            "共 ${txs.size} 笔记录 · 数据来自默记 · xiuchenshen.com",
            WIDTH / 2f,
            HEIGHT - 100f,
            whiteDim2
        )

        return bitmap
    }

    fun saveToGallery(context: Context, bitmap: Bitmap): String? {
        val name = "默记账单_${System.currentTimeMillis()}.png"
        return try {
            if (Build.VERSION.SDK_INT >= 29) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, name)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        "Pictures/默记"
                    )
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
                ) ?: return null
                try {
                    val out = context.contentResolver.openOutputStream(uri)
                        ?: throw java.io.IOException("无法打开输出流")
                    out.use { stream ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    }
                    val done = ContentValues().apply {
                        put(MediaStore.Images.Media.IS_PENDING, 0)
                    }
                    context.contentResolver.update(uri, done, null, null)
                    uri.toString()
                } catch (e: Exception) {
                    // 写入失败删除残留条目，避免相册出现空文件
                    context.contentResolver.delete(uri, null, null)
                    throw e
                }
            } else {
                val dir = java.io.File(
                    context.getExternalFilesDir(null), "默记账单"
                ).apply { mkdirs() }
                val file = java.io.File(dir, name)
                file.outputStream().use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                file.absolutePath
            }
        } catch (e: Exception) {
            android.util.Log.e("MonthCardImage", "save failed", e)
            null
        }
    }
}
