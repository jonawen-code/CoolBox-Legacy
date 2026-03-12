// Build: 2.0.0
package com.example.coolbox.data

import androidx.lifecycle.LiveData

class FoodRepository(private val foodDao: FoodDao) {
    val allItems: LiveData<List<FoodEntity>> = foodDao.getAllItems()

    suspend fun insertItem(item: FoodEntity) {
        foodDao.insertItem(item)
    }

    suspend fun hardDelete(item: FoodEntity) {
        foodDao.hardDelete(item)
    }

    suspend fun getAllIncludeDeleted(): List<FoodEntity> {
        return foodDao.getAllIncludeDeleted()
    }

    suspend fun migrateCategory(oldCategory: String, newCategory: String) {
        foodDao.updateCategory(oldCategory, newCategory)
    }

    suspend fun migrateEmptyIcons(defaultIcon: String, timestamp: Long) {
        foodDao.migrateEmptyIcons(defaultIcon, timestamp)
    }
}

