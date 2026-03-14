// Version: V3.0.0-Pre21
package com.example.coolbox.data

import androidx.room.Entity
import androidx.room.PrimaryKey

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
    val isDeleted: Boolean = false
)
// Version: V3.0.0-Pre9
