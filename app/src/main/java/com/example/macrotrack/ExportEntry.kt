package com.example.macrotrack

data class ExportEntry(
    val date: String,
    val mealType: String,
    val foodName: String,
    val servings: Float,
    val protein: Float,
    val fat: Float,
    val sugar: Float,
    val carbs: Float
)
