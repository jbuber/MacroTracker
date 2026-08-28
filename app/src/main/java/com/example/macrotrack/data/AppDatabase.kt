package com.example.macrotrack.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.macrotrack.MacroDao
import com.example.macrotrack.MealEntry
import com.example.macrotrack.DailyLog
import com.example.macrotrack.data.FoodLogEntity
import com.example.macrotrack.data.FoodItemEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        FoodItemEntity::class,
        FoodLogEntity::class,
        MealEntry::class,
        DailyLog::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun foodDao(): MacroDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "macro_track_db"
                )
                    .fallbackToDestructiveMigration() // Safely clears and recreates tables on version/schema updates
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Seeds initial Aldi/grocery data on first app creation
                            INSTANCE?.let { database ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    populateInitialData(database.foodDao())
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun populateInitialData(dao: MacroDao) {
            // Starter Aldi staples
            dao.insertFoodItem(
                FoodItemEntity(
                    id = "aldi_eggs_1",
                    name = "Goldhen Large White Eggs (1 egg)",
                    brand = "Goldhen",
                    servingSize = "50g",
                    calories = 70,
                    protein = 6f,
                    carbs = 0.4f,
                    fat = 5f,
                    sugar = 0f
                )
            )
            dao.insertFoodItem(
                FoodItemEntity(
                    id = "aldi_milk_1",
                    name = "Friendly Farms Whole Milk (1 cup)",
                    brand = "Friendly Farms",
                    servingSize = "240ml",
                    calories = 150,
                    protein = 8f,
                    carbs = 12f,
                    fat = 8f,
                    sugar = 12f
                )
            )
        }
    }
}