package com.example.coolbox.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [FoodEntity::class], version = 6, exportSchema = false)
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
                .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
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
                val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                val isSyncEnabled = prefs.getBoolean("sync_enabled", false)
                if (isSyncEnabled) {
                    val serverUrl = prefs.getString("sync_server_url", "http://192.168.31.94:3000/coolbox") ?: "http://192.168.31.94:3000/coolbox"
                    com.example.coolbox.util.CloudSyncManager.uploadDatabase(context, serverUrl)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
