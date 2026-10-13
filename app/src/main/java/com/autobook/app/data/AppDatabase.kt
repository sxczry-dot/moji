package com.autobook.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Transaction::class], version = 5, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val renames = listOf(
                    "餐饮" to "餐饮美食",
                    "购物" to "购物消费",
                    "交通" to "交通出行",
                    "教育" to "教育学习",
                    "美容" to "美容护理",
                    "医疗" to "医疗健康",
                    "娱乐" to "娱乐休闲",
                    "人情" to "人情社交",
                    "通讯" to "通讯网络"
                )
                for ((old, new) in renames) {
                    db.execSQL(
                        "UPDATE transactions SET category = REPLACE(category, '$old', '$new') " +
                            "WHERE category LIKE '$old%'"
                    )
                }
                db.execSQL(
                    "UPDATE transactions SET category = '其他' " +
                        "WHERE category = '居住' OR category LIKE '居住/%' " +
                        "OR category = '宠物' OR category LIKE '宠物/%'"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE transactions ADD COLUMN insertedAt INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "UPDATE transactions SET insertedAt = timestamp WHERE insertedAt = 0"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_transactions_book_timestamp ON transactions(book, timestamp)"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE transactions ADD COLUMN refNo TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    "ALTER TABLE transactions ADD COLUMN book TEXT NOT NULL DEFAULT '默认账本'"
                )
            }
        }

        fun get(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "autobook.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5).build().also { instance = it }
            }
        }
    }
}
