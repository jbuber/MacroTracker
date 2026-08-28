package com.example.macrotrack.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "food_items")
data class FoodItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val brand: String,
    val servingSize: String,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val sugar: Float = 0f,
    val isFavorite: Boolean = false
)