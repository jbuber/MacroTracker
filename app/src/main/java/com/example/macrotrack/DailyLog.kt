package com.example.macrotrack

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_logs")
data class DailyLog(
    @PrimaryKey val date: String // Format: "YYYY-MM-DD"
)