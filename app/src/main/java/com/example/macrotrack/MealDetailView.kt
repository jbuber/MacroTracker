package com.example.macrotrack

data class MealDetailView(
    val entryId: Long,
    val name: String,
    val servings: Float,
    val protein: Float,
    val fat: Float,
    val sugar: Float,
    val carbs: Float
)