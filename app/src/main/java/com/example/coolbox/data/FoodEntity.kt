// Version: V3.0.0-Pre22
package com.example.coolbox.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "food_items")
data class FoodEntity(
    @PrimaryKey val id: String,
    val icon: String,
    val name: String,
    val fridgeName: String,
    val inputDateMs: Long,
    val expiryDateMs: Long,
    val quantity: Double,
    val weightPerPortion: Double,
    val portions: Int,
    val category: String,
    val unit: String,
    val remark: String = "",
    val lastModifiedMs: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val itemType: Int = 0  // 0: Food, 1: Medicine
)

@Entity(tableName = "fridge_settings")
data class FridgeSettings(
    @PrimaryKey val fridgeName: String,
    val spaceAffinity: Int = 2 // 0: Food, 1: Medicine, 2: Both
)

@Dao
interface FridgeSettingsDao {
    @Query("SELECT * FROM fridge_settings")
    fun getAll(): kotlinx.coroutines.flow.Flow<List<FridgeSettings>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(settings: FridgeSettings)

    @Query("SELECT * FROM fridge_settings WHERE fridgeName = :name")
    fun getByName(name: String): FridgeSettings?

    @Query("DELETE FROM fridge_settings WHERE fridgeName = :name")
    fun deleteByName(name: String)

    @Query("DELETE FROM fridge_settings")
    fun clearAll()
}

// Version: V3.0.0-Pre9
