// Version: V3.0.0-Pre21
package com.example.coolbox.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [FoodEntity::class], version = 7, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao

    companion object {
        private val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE food_items ADD COLUMN category TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE food_items ADD COLUMN lastModifiedMs INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE food_items ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                // V3.0.0-Pre10 Enforcement: Re-verify or reinforce column
                // SQLite doesn't have "IF NOT EXISTS" for ADD COLUMN, but we can check or just do a safe op
                // For now, we'll just log or do a dummy op to trigger the version bump
                database.execSQL("UPDATE food_items SET isDeleted = 0 WHERE isDeleted IS NULL")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun closeDatabase() {
            INSTANCE?.close()
            INSTANCE = null
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "coolbox_database"
                )
                .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                .fallbackToDestructiveMigration()
                .build()
                @Suppress("UNCHECKED_CAST")
                INSTANCE = instance
                instance
            }
        }
        fun exportDatabase(context: Context) {
            try {
                // Flush WAL to main database file
                val db = getDatabase(context)
                db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)", null).close()

                val dbFile = context.getDatabasePath("coolbox_database")
                if (!dbFile.exists()) return

                val exportDir = android.os.Environment.getExternalStoragePublicDirectory("CoolBox")
                if (!exportDir.exists()) exportDir.mkdirs()

                val exportFile = java.io.File(exportDir, "sync.db")
                
                dbFile.inputStream().use { input ->
                    exportFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                android.util.Log.d("CoolBox", "DB Exported to: ${exportFile.absolutePath}")
                
                // Trigger Cloud Sync if enabled
                val prefs = context.getSharedPreferences("coolbox_prefs", Context.MODE_PRIVATE)
                val isSyncEnabled = prefs.getBoolean("sync_enabled", false)
                if (isSyncEnabled) {
                    val serverUrl = prefs.getString("sync_server_url", "http://192.168.31.94:3001/coolbox") ?: "http://192.168.31.94:3001/coolbox"
                    com.example.coolbox.util.CloudSyncManager.uploadDatabase(context, serverUrl)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
// Version: V3.0.0-Pre7
