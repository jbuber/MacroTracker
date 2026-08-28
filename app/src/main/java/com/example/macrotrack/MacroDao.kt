package com.example.macrotrack

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import kotlinx.coroutines.flow.Flow
import com.example.macrotrack.data.FoodItemEntity
import com.example.macrotrack.data.FoodLogEntity
// Import your other model classes if they are in subpackages, for example:
// import com.example.macrotrack.data.FoodItem
// import com.example.macrotrack.data.DailyLog
// import com.example.macrotrack.data.MealEntry
// import com.example.macrotrack.data.MealDetailView

@Dao
interface MacroDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: FoodLogEntity)

    @androidx.room.Delete
    suspend fun deleteLog(log: FoodLogEntity)

    @Query("SELECT * FROM food_logs")
    suspend fun getAllLogs(): List<FoodLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodItem(foodItem: FoodItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<FoodItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyLog(dailyLog: DailyLog)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealEntry(mealEntry: MealEntry)

    @Query("DELETE FROM meal_entries WHERE id = :entryId")
    suspend fun deleteMealEntry(entryId: Long)

    // Search query that prioritizes favorites first, then alphabetical order, searching name and brand
    @Query("SELECT * FROM food_items WHERE name LIKE '%' || :query || '%' OR brand LIKE '%' || :query || '%' ORDER BY isFavorite DESC, name ASC")
    fun searchFoodItems(query: String): Flow<List<FoodItemEntity>>

    @Query("SELECT * FROM food_items WHERE name LIKE '%' || :query || '%'")
    suspend fun searchFood(query: String): List<FoodItemEntity>

    // Toggle favorite status
    @Query("UPDATE food_items SET isFavorite = :isFav WHERE id = :foodId")
    suspend fun updateFavoriteStatus(foodId: String, isFav: Boolean)

    @Query("SELECT * FROM food_items WHERE name = :name LIMIT 1")
    suspend fun getFoodItemByName(name: String): FoodItemEntity?

    @RewriteQueriesToDropUnusedColumns
    @Query("""
        SELECT me.date, me.mealType, f.name as foodName, me.servings, 
               f.protein, f.fat, f.sugar, f.carbs 
        FROM meal_entries me
        INNER JOIN food_items f ON me.foodItemId = f.id
        WHERE me.date >= :startDate
        ORDER BY me.date ASC
    """)
    suspend fun getMealEntriesSince(startDate: String): List<ExportEntry>

    @RewriteQueriesToDropUnusedColumns
    @Query("""
        SELECT me.date, me.mealType, f.name as foodName, me.servings, 
               f.protein, f.fat, f.sugar, f.carbs 
        FROM meal_entries me
        INNER JOIN food_items f ON me.foodItemId = f.id
        ORDER BY me.date DESC
    """)
    suspend fun getAllMealEntriesWithDetails(): List<ExportEntry>

    @Query("""
        SELECT * FROM meal_entries 
        WHERE date = :date AND mealType = :mealType
    """)
    suspend fun getMealEntriesRaw(date: String, mealType: String): List<MealEntry>

    @RewriteQueriesToDropUnusedColumns
    @Query("""
        SELECT me.id as entryId, me.mealType, me.servings, 
               f.name, f.servingSize, f.protein, f.fat, f.sugar, f.carbs 
        FROM meal_entries me
        INNER JOIN food_items f ON me.foodItemId = f.id
        WHERE me.date = :date AND me.mealType = :mealType
    """)
    fun getMealEntriesForDayAndType(date: String, mealType: String): Flow<List<MealDetailView>>
}