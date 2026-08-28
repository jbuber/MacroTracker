package com.example.macrotrack

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meal_entries")
data class MealEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val date: String,
    val mealType: String, // "Breakfast", "Lunch", "Dinner", "Snack"
    val foodItemId: String,
    val servings: Float
)