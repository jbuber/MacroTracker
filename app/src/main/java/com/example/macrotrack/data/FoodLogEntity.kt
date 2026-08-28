package com.example.macrotrack.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "food_logs") // <-- This name MUST match "food_logs" exactly
data class FoodLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val foodName: String,
    val mealType: String,
    val multiplier: Float,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val sugar: Float,
    val timestamp: Long = System.currentTimeMillis()
)