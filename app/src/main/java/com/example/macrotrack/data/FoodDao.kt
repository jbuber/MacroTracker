package com.example.macrotrack.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FoodDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<FoodItemEntity>)

    @Query("SELECT * FROM food_items WHERE name LIKE '%' || :query || '%'")
    suspend fun searchFood(query: String): List<FoodItemEntity>

    @Query("SELECT * FROM food_logs")
    suspend fun getAllLogs(): List<FoodLogEntity>

    @androidx.room.Insert
    suspend fun insertLog(log: FoodLogEntity)

    @androidx.room.Delete
    suspend fun deleteLog(log: FoodLogEntity)
}