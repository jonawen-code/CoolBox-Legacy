package com.example.coolbox.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface FoodDao {
    @Query("SELECT * FROM food_items WHERE isDeleted = 0 ORDER BY expiryDateMs ASC")
    fun getAllItems(): LiveData<List<FoodEntity>>

    @Query("SELECT * FROM food_items")
    suspend fun getAllIncludeDeleted(): List<FoodEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: FoodEntity)

    @Update
    suspend fun updateItem(item: FoodEntity)

    @Query("UPDATE food_items SET category = :newCategory WHERE category = :oldCategory")
    suspend fun updateCategory(oldCategory: String, newCategory: String)

    @Query("UPDATE food_items SET isDeleted = 1, lastModifiedMs = :timestamp WHERE id = :id")
    suspend fun softDelete(id: String, timestamp: Long)

    @Delete
    suspend fun hardDelete(item: FoodEntity)
}
