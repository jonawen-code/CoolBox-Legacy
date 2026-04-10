package com.example.coolbox.util

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.coolbox.data.AppDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Manages backup and restore of the application's data.
 * Backups are stored as ZIP files containing:
 * 1. the SQLite database file
 * 2. a JSON representation of SharedPreferences
 */
object BackupManager {
    private const val TAG = "BackupManager"
    private const val DB_FILE_NAME = "coolbox_database"
    private const val PREFS_FILE_NAME = "coolbox_prefs.json"
    private val gson = Gson()
    
    /**
     * Build 4.0.7: Generate sortable backup filename
     * Format: [Prefix]_YYYYMMDD###[Extension]
     * Example: CoolBox_Backup_20260323001.cbk
     */
    fun generateBackupFileName(prefix: String, extension: String): String {
        val sdf = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault())
        val datePart = sdf.format(java.util.Date())
        val suffix = (System.currentTimeMillis() % 1000).toInt()
        val seq = String.format("%03d", suffix)
        return "${prefix}_${datePart}${seq}${extension}"
    }

    /**
     * Exports DB and SharedPreferences to a ZIP file at the given Uri.
     */
    fun exportData(context: Context, uri: Uri): Boolean {
        return try {
            // 1. Flush and close database
            AppDatabase.getDatabase(context).apply {
                openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)", null).use { it.moveToFirst() }
                close()
            }
            AppDatabase.closeDatabase()

            val dbFile = context.getDatabasePath(DB_FILE_NAME)
            val prefs = context.getSharedPreferences("coolbox_prefs", Context.MODE_PRIVATE)
            val prefsMap = prefs.all

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                ZipOutputStream(BufferedOutputStream(outputStream)).use { zipOut ->
                    // Add Database File
                    if (dbFile.exists()) {
                        zipOut.putNextEntry(ZipEntry(DB_FILE_NAME))
                        dbFile.inputStream().use { it.copyTo(zipOut) }
                        zipOut.closeEntry()
                    }

                    // Add Preferences JSON
                    zipOut.putNextEntry(ZipEntry(PREFS_FILE_NAME))
                    val json = gson.toJson(prefsMap)
                    zipOut.write(json.toByteArray())
                    zipOut.closeEntry()
                }
            }
            Log.d(TAG, "Export successful to $uri")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Export failed", e)
            false
        }
    }

    /**
     * Imports DB and SharedPreferences from a ZIP file at the given Uri.
     * WARNING: This overwrites current data!
     */
    fun importData(context: Context, uri: Uri): Boolean {
        return try {
            AppDatabase.closeDatabase()
            
            val dbFile = context.getDatabasePath(DB_FILE_NAME)
            val prefs = context.getSharedPreferences("coolbox_prefs", Context.MODE_PRIVATE)

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(BufferedInputStream(inputStream)).use { zipIn ->
                    var entry: ZipEntry? = zipIn.getNextEntry()
                    while (entry != null) {
                        when (entry.name) {
                            DB_FILE_NAME -> {
                                dbFile.outputStream().use { output ->
                                    zipIn.copyTo(output)
                                }
                            }
                            PREFS_FILE_NAME -> {
                                val reader = BufferedReader(InputStreamReader(zipIn))
                                val json = reader.readText()
                                val type = object : TypeToken<Map<String, Any?>>() {}.type
                                val map: Map<String, Any?> = gson.fromJson(json, type)
                                
                                val editor = prefs.edit()
                                editor.clear()
                                map.forEach { (key, value) ->
                                    when (value) {
                                        is String -> editor.putString(key, value)
                                        is Boolean -> editor.putBoolean(key, value)
                                        is Float -> editor.putFloat(key, value)
                                        is Long -> editor.putLong(key, value)
                                        is Int -> editor.putInt(key, value)
                                        is Double -> {
                                            // GSON might parse integers as doubles
                                            if (value == value.toInt().toDouble()) {
                                                editor.putInt(key, value.toInt())
                                            } else {
                                                editor.putFloat(key, value.toFloat())
                                            }
                                        }
                                        is List<*> -> {
                                            // Handle StringSet if needed, but SharedPreferences.all returns them as List/Set
                                            @Suppress("UNCHECKED_CAST")
                                            editor.putStringSet(key, (value as List<String>).toSet())
                                        }
                                    }
                                }
                                editor.commit()
                            }
                        }
                        zipIn.closeEntry()
                        entry = zipIn.getNextEntry()
                    }
                }
            }
            Log.d(TAG, "Import successful from $uri")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Import failed", e)
            false
        }
    }
    /**
     * 从备份文件中增量合并食品数据（不覆盖现有设置和其它数据）
     */
    fun importDataIncremental(context: Context, uri: Uri): Boolean {
        return try {
            val tempDbFile = File(context.cacheDir, "temp_import.db")
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(BufferedInputStream(inputStream)).use { zipIn ->
                    var entry: ZipEntry? = zipIn.getNextEntry()
                    while (entry != null) {
                        if (entry.name == DB_FILE_NAME) {
                            tempDbFile.outputStream().use { output ->
                                zipIn.copyTo(output)
                            }
                            break
                        }
                        zipIn.closeEntry()
                        entry = zipIn.getNextEntry()
                    }
                }
            }

            if (!tempDbFile.exists()) return false

            val externalDb = android.database.sqlite.SQLiteDatabase.openDatabase(
                tempDbFile.absolutePath, null, android.database.sqlite.SQLiteDatabase.OPEN_READONLY
            )

            val items = mutableListOf<com.example.coolbox.data.FoodEntity>()
            externalDb.query("food_items", null, null, null, null, null, null).use { cursor ->
                val idIdx = cursor.getColumnIndex("id")
                val iconIdx = cursor.getColumnIndex("icon")
                val nameIdx = cursor.getColumnIndex("name")
                val fridgeIdx = cursor.getColumnIndex("fridgeName")
                val inDateIdx = cursor.getColumnIndex("inputDateMs")
                val exDateIdx = cursor.getColumnIndex("expiryDateMs")
                val qtyIdx = cursor.getColumnIndex("quantity")
                val catIdx = cursor.getColumnIndex("category")
                val unitIdx = cursor.getColumnIndex("unit")
                val modifiedIdx = cursor.getColumnIndex("lastModifiedMs")
                val deletedIdx = cursor.getColumnIndex("isDeleted")
                val itemTypeIdx = cursor.getColumnIndex("itemType")

                while (cursor.moveToNext()) {
                    items.add(com.example.coolbox.data.FoodEntity(
                        id = if (idIdx >= 0) cursor.getString(idIdx) else java.util.UUID.randomUUID().toString(),
                        icon = if (iconIdx >= 0) cursor.getString(iconIdx) else "",
                        name = if (nameIdx >= 0) cursor.getString(nameIdx) else "MergeItem",
                        fridgeName = if (fridgeIdx >= 0) cursor.getString(fridgeIdx) else "Default",
                        inputDateMs = if (inDateIdx >= 0) cursor.getLong(inDateIdx) else System.currentTimeMillis(),
                        expiryDateMs = if (exDateIdx >= 0) cursor.getLong(exDateIdx) else 0L,
                        quantity = if (qtyIdx >= 0) cursor.getDouble(qtyIdx) else 1.0,
                        weightPerPortion = 0.0,
                        portions = 1,
                        category = if (catIdx >= 0) cursor.getString(catIdx) else "Other",
                        unit = if (unitIdx >= 0) cursor.getString(unitIdx) else "pcs",
                        remark = "",
                        lastModifiedMs = if (modifiedIdx >= 0) cursor.getLong(modifiedIdx) else System.currentTimeMillis(),
                        isDeleted = if (deletedIdx >= 0) cursor.getInt(deletedIdx) == 1 else false,
                        itemType = if (itemTypeIdx >= 0) cursor.getInt(itemTypeIdx) else 0
                    ))
                }
            }
            externalDb.close()
            tempDbFile.delete()

            if (items.isNotEmpty()) {
                val db = AppDatabase.getDatabase(context)
                kotlinx.coroutines.runBlocking {
                    db.foodDao().insertAll(items)
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Incremental import failed", e)
            false
        }
    }

    /**
     * 将食品数据导出为 CSV 格式
     */
    fun exportToCsv(context: Context, uri: Uri): Boolean {
        return try {
            val db = AppDatabase.getDatabase(context)
            val foods = kotlinx.coroutines.runBlocking { db.foodDao().getAllFoodsSync() }
            
            context.contentResolver.openOutputStream(uri)?.use { output ->
                val writer = BufferedWriter(OutputStreamWriter(output, "UTF-8"))
                writer.write("\uFEFF") // BOM for Excel UTF-8 compliance
                writer.write("ID,Name,Fridge,InputDate,ExpiryDate,Quantity,Unit,Category,Remark,ItemType\n")
                
                for (food in foods) {
                    val row = StringBuilder()
                    row.append("\"${food.id}\",")
                    row.append("\"${food.name.replace("\"", "\"\"")}\",")
                    row.append("\"${food.fridgeName.replace("\"", "\"\"")}\",")
                    row.append("${food.inputDateMs},")
                    row.append("${food.expiryDateMs},")
                    row.append("${food.quantity},")
                    row.append("\"${food.unit}\",")
                    row.append("\"${food.category}\",")
                    row.append("\"${food.remark.replace("\"", "\"\"")}\",")
                    row.append("${food.itemType}")
                    row.append("\n")
                    writer.write(row.toString())
                }
                writer.flush()
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "CSV Export failed", e)
            false
        }
    }

    /**
     * 从 CSV 文件导入食品数据
     */
    fun importDataFromCsv(context: Context, uri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val reader = BufferedReader(InputStreamReader(input, "UTF-8"))
                val header = reader.readLine() // ID,Name,...
                
                val items = mutableListOf<com.example.coolbox.data.FoodEntity>()
                var line = reader.readLine()
                while (line != null) {
                    if (line.trim().isEmpty()) { line = reader.readLine(); continue }
                    val parts = parseCsvLine(line)
                    if (parts.size >= 8) {
                        items.add(com.example.coolbox.data.FoodEntity(
                            id = parts[0],
                            icon = "", // CSV 暂不存图标
                            name = parts[1],
                            fridgeName = parts[2],
                            inputDateMs = parts[3].toLongOrNull() ?: System.currentTimeMillis(),
                            expiryDateMs = parts[4].toLongOrNull() ?: 0L,
                            quantity = parts[5].toDoubleOrNull() ?: 1.0,
                            weightPerPortion = 0.0,
                            portions = 1,
                            category = if (parts.size > 7) parts[7] else "Other",
                            unit = if (parts.size > 6) parts[6] else "pcs",
                            remark = if (parts.size > 8) parts[8] else "",
                            lastModifiedMs = System.currentTimeMillis(),
                            isDeleted = false,
                            itemType = if (parts.size > 9) parts[9].toIntOrNull() ?: 0 else 0
                        ))
                    }
                    line = reader.readLine()
                }
                
                if (items.isNotEmpty()) {
                    val db = AppDatabase.getDatabase(context)
                    kotlinx.coroutines.runBlocking {
                        db.foodDao().insertAll(items)
                    }
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "CSV Import failed", e)
            false
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var inQuotes = false
        var current = StringBuilder()
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                    current.append('"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString())
                current = StringBuilder()
            } else {
                current.append(c)
            }
            i++
        }
        result.add(current.toString())
        return result
    }
}
