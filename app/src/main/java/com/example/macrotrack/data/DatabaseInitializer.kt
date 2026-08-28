package com.example.macrotrack.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DatabaseInitializer {
    suspend fun populateDatabase(context: Context, database: AppDatabase) {
        withContext(Dispatchers.IO) {
            try {
                // Open the JSON file from assets
                val inputStream = context.assets.open("aldi_foods.json")
                val jsonString = inputStream.bufferedReader().use { it.readText() }

                // Parse JSON into a list of FoodItemEntity
                val listType = object : TypeToken<List<FoodItemEntity>>() {}.type
                val foodItems: List<FoodItemEntity> = Gson().fromJson(jsonString, listType)

                // Insert items into Room Database
                database.foodDao().insertAll(foodItems)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}