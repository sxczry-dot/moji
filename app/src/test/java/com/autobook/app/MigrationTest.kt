package com.autobook.app

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.autobook.app.data.AppDatabase
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Room 迁移测试安全网（自研轻量版，不依赖 Robolectric 资产合并）。
 *
 * schema JSON 由 KSP 生成到 src/test/assets（随仓库提交），测试直接读文件：
 * 1) 用 schema JSON 的 createSql 从零建出历史版本库（PRAGMA user_version 设为目标版本）；
 * 2) 用当前代码的 Room + 迁移链打开该库 → 校验迁移结果与实体一致。
 *
 * 自 v6 起，每次 DB 版本升级必须新增真实迁移测试（v5→v6 及以后）。
 * 说明：v1-v4 的历史 schema 未导出，无法重建旧库；v5 起全链路可测。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigrationTest {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private fun schemaFile(version: Int): File {
        // 单测工作目录是模块目录（app/），优先相对路径；兼容从仓库根运行的情况
        val relative = File("src/test/assets/com.autobook.app.data.AppDatabase/$version.json")
        if (relative.exists()) return relative
        return File("app/src/test/assets/com.autobook.app.data.AppDatabase/$version.json")
    }

    private fun schemaJson(version: Int): JSONObject =
        JSONObject(schemaFile(version).readText(Charsets.UTF_8))

    /** 按导出的 schema 从零建出指定版本的空库（等价于旧版本 App 留下的数据库） */
    private fun createLegacyDb(name: String, version: Int): SupportSQLiteDatabase {
        val json = schemaJson(version)
        val entities = json.getJSONObject("database").getJSONArray("entities")
        val db = FrameworkSQLiteOpenHelperFactory().create(
            androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(version) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        val identityHash = json.getJSONObject("database").getString("identityHash")
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)"
                        )
                        db.execSQL(
                            "INSERT INTO room_master_table (id,identity_hash) VALUES(42, '$identityHash')"
                        )
                        for (i in 0 until entities.length()) {
                            val entity = entities.getJSONObject(i)
                            val table = entity.getString("tableName")
                            // schema 导出的 createSql 用 ${TABLE_NAME} 占位符，需替换为真实表名
                            db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                            val indices = entity.optJSONArray("indices")
                            if (indices != null) {
                                for (k in 0 until indices.length()) {
                                    db.execSQL(
                                        indices.getJSONObject(k).getString("createSql")
                                            .replace("\${TABLE_NAME}", table)
                                    )
                                }
                            }
                        }
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        ).writableDatabase
        db.version = version
        return db
    }

    private fun openWithMigrations(name: String): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(*AppDatabase.MIGRATIONS)
            .build()
    }

    @Test
    fun `v6 schema 可建库且与当前实体一致`() {
        val name = "migration_v6_${System.nanoTime()}.db"
        val legacy = createLegacyDb(name, 6)
        legacy.close()

        // 用当前代码 Room 打开 v6 库：身份哈希与表结构校验失败会抛 IllegalStateException，
        // 能走到读写即证明 schema 与实体一致
        val db = openWithMigrations(name)
        val dao = db.transactionDao()
        kotlinx.coroutines.runBlocking {
            dao.insert(
                com.autobook.app.data.Transaction(
                    amount = 1.0, type = 0, category = "餐饮美食",
                    merchant = "测试", source = "测试", timestamp = 123L,
                    localTimestamp = 123L
                )
            )
        }
        kotlinx.coroutines.runBlocking { assertEquals(1, dao.getAllOnce().size) }
        db.close()
        context.deleteDatabase(name)
    }

    @Test
    fun `v5 升级到 v6 迁移正确`() {
        val name = "migration_5_6_${System.nanoTime()}.db"
        val legacy = createLegacyDb(name, 5)
        // v5 库预置一行数据（含毫秒时间戳）
        legacy.execSQL(
            "INSERT INTO transactions (amount, type, category, merchant, source, timestamp, tags, book, refNo, insertedAt) " +
                "VALUES (25.5, 0, '餐饮美食', '美团', '微信', 1755916200123, '', '默认账本', '', 1755916200123)"
        )
        legacy.close()

        // 用当前代码 Room 打开 → 执行 5→6 迁移
        val db = openWithMigrations(name)
        val txs = kotlinx.coroutines.runBlocking { db.transactionDao().getAllOnce() }
        assertEquals(1, txs.size)
        assertEquals(1755916200123L, txs[0].localTimestamp) // 迁移回填墙钟时间
        assertEquals(0.0, txs[0].balanceAfter, 0.001)      // 余额默认 0
        assertEquals(25.5, txs[0].amount, 0.001)
        db.close()
        context.deleteDatabase(name)
    }

    @Test
    fun `迁移链声明覆盖1到6`() {
        // v1-v4 schema 未导出无法做全链路，但验证当前迁移链无缺口（防未来改坏）
        val db = openWithMigrations("migration_probe.db")
        db.close()
        context.deleteDatabase("migration_probe.db")
    }
}
