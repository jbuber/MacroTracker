package com.example.macrotrack

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.macrotrack.data.AppDatabase
import com.example.macrotrack.data.FoodItemEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import android.content.Context
import android.net.Uri
import java.time.temporal.ChronoUnit

data class DailyTotals(
    val date: String,
    val protein: Float,
    val fat: Float,
    val sugar: Float,
    val carbs: Float
)

class MacroViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).foodDao()
    private val prefs = application.getSharedPreferences("macro_prefs", Context.MODE_PRIVATE)

    // Current selected date formatted as "YYYY-MM-DD"
    private val _currentDate = MutableStateFlow(LocalDate.now().format(DateTimeFormatter.ISO_DATE))
    val currentDate: StateFlow<String> = _currentDate.asStateFlow()

    // Daily Macro Goals (Persisted)
    private val _proteinGoal = MutableStateFlow(prefs.getFloat("protein_goal", 150f))
    val proteinGoal: StateFlow<Float> = _proteinGoal.asStateFlow()

    private val _fatGoal = MutableStateFlow(prefs.getFloat("fat_goal", 65f))
    val fatGoal: StateFlow<Float> = _fatGoal.asStateFlow()

    private val _sugarGoal = MutableStateFlow(prefs.getFloat("sugar_goal", 50f))
    val sugarGoal: StateFlow<Float> = _sugarGoal.asStateFlow()

    private val _carbsGoal = MutableStateFlow(prefs.getFloat("carbs_goal", 200f))
    val carbsGoal: StateFlow<Float> = _carbsGoal.asStateFlow()

    fun updateGoals(protein: Float, fat: Float, sugar: Float, carbs: Float) {
        viewModelScope.launch {
            prefs.edit().apply {
                putFloat("protein_goal", protein)
                putFloat("fat_goal", fat)
                putFloat("sugar_goal", sugar)
                putFloat("carbs_goal", carbs)
                apply()
            }
            _proteinGoal.value = protein
            _fatGoal.value = fat
            _sugarGoal.value = sugar
            _carbsGoal.value = carbs
        }
    }

    // Search query for the food dropdown
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Live list of food items matching search (Reactive to DB changes and API results)
    @OptIn(ExperimentalCoroutinesApi::class)
    val searchResults: StateFlow<List<FoodItemEntity>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) return@flatMapLatest kotlinx.coroutines.flow.flowOf(emptyList())

            val localFlow = dao.searchFoodItems(query)
            val apiFlow = flow {
                if (query.length > 2) {
                    try {
                        val apiResponse = if (query.lowercase().contains("aldi")) {
                            OpenFoodApiService.instance.searchByBrand("Aldi")
                        } else {
                            OpenFoodApiService.instance.searchProducts(query)
                        }
                        
                        val apiMappedItems = apiResponse.products?.mapNotNull { prod ->
                            val name = prod.product_name ?: return@mapNotNull null
                            val brand = prod.brands ?: "Open Food Facts"
                            val cals = prod.nutriments?.energy_kcal_100g?.toInt() ?: 0
                            val protein = prod.nutriments?.proteins_100g ?: 0f
                            val carbs = prod.nutriments?.carbohydrates_100g ?: 0f
                            val fat = prod.nutriments?.fat_100g ?: 0f
                            val sugar = prod.nutriments?.sugars_100g ?: 0f

                            FoodItemEntity(
                                id = "off_${name.hashCode()}",
                                name = name,
                                brand = brand,
                                servingSize = prod.serving_size ?: "100g",
                                calories = cals,
                                protein = protein,
                                carbs = carbs,
                                fat = fat,
                                sugar = sugar
                            )
                        } ?: emptyList()
                        emit(apiMappedItems)
                    } catch (e: Exception) {
                        emit(emptyList())
                    }
                } else {
                    emit(emptyList())
                }
            }

            kotlinx.coroutines.flow.combine(localFlow, apiFlow) { local, api ->
                (local + api).distinctBy { it.name.lowercase() }
                    .sortedWith(compareByDescending<FoodItemEntity> { it.isFavorite }.thenBy { it.name.lowercase() })
            }
        }
        .flowOn(kotlinx.coroutines.Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleFavorite(item: FoodItemEntity) {
        viewModelScope.launch {
            dao.insertFoodItem(item.copy(isFavorite = !item.isFavorite))
        }
    }

    fun changeDate(daysOffset: Long) {
        val parsedDate = LocalDate.parse(_currentDate.value, DateTimeFormatter.ISO_DATE)
        _currentDate.value = parsedDate.plusDays(daysOffset).format(DateTimeFormatter.ISO_DATE)
    }

    fun addMealEntry(mealType: String, foodItemId: String, servings: Float) {
        viewModelScope.launch {
            val date = _currentDate.value
            dao.insertDailyLog(DailyLog(date = date))
            dao.insertMealEntry(
                MealEntry(
                    date = date,
                    mealType = mealType,
                    foodItemId = foodItemId,
                    servings = servings
                )
            )
        }
    }

    fun deleteMealEntry(entryId: Long) {
        viewModelScope.launch {
            dao.deleteMealEntry(entryId)
        }
    }

    fun insertCustomFood(name: String, servingSize: String, protein: Float, fat: Float, sugar: Float, carbs: Float) {
        viewModelScope.launch {
            dao.insertFoodItem(
                FoodItemEntity(
                    id = System.currentTimeMillis().toString(),
                    name = name,
                    brand = "Custom",
                    servingSize = servingSize,
                    calories = ((protein * 4) + (fat * 9) + (carbs * 4)).toInt(),
                    protein = protein,
                    fat = fat,
                    sugar = sugar,
                    carbs = carbs
                )
            )
            updateSearchQuery(name)
        }
    }

    fun toggleFavorite(foodId: String, currentStatus: Boolean) {
        viewModelScope.launch {
            dao.updateFavoriteStatus(foodId, !currentStatus)
        }
    }

    fun getEntriesForMeal(mealType: String): Flow<List<MealDetailView>> {
        return dao.getMealEntriesForDayAndType(_currentDate.value, mealType)
    }

    fun getPastMonthData(onResult: (List<DailyTotals>) -> Unit) {
        viewModelScope.launch {
            val startDate = LocalDate.now().minusMonths(1).format(DateTimeFormatter.ISO_DATE)
            val entries = dao.getMealEntriesSince(startDate)
            
            val grouped = entries.groupBy { it.date }
            val results = mutableListOf<DailyTotals>()
            
            var current = LocalDate.parse(startDate)
            val end = LocalDate.now()
            
            while (!current.isAfter(end)) {
                val dateStr = current.format(DateTimeFormatter.ISO_DATE)
                val dayEntries = grouped[dateStr] ?: emptyList()
                
                results.add(DailyTotals(
                    date = dateStr,
                    protein = dayEntries.sumOf { (it.protein * it.servings).toDouble() }.toFloat(),
                    fat = dayEntries.sumOf { (it.fat * it.servings).toDouble() }.toFloat(),
                    sugar = dayEntries.sumOf { (it.sugar * it.servings).toDouble() }.toFloat(),
                    carbs = dayEntries.sumOf { (it.carbs * it.servings).toDouble() }.toFloat()
                ))
                current = current.plusDays(1)
            }
            onResult(results)
        }
    }

    fun exportData(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val entries = dao.getAllMealEntriesWithDetails()
            val csv = CsvManager.generateCsv(entries)
            onResult(csv)
        }
    }

    fun importData(context: Context, uri: Uri, onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            val entries = CsvManager.parseCsv(context, uri)
            var count = 0
            val timestamp = System.currentTimeMillis()
            entries.forEachIndexed { index, entry ->
                var food = dao.getFoodItemByName(entry.foodName)
                if (food == null) {
                    food = FoodItemEntity(
                        id = "imported_${timestamp}_${index}_${entry.foodName.hashCode()}",
                        name = entry.foodName,
                        brand = "Imported",
                        servingSize = "1 serving",
                        calories = ((entry.protein * 4) + (entry.fat * 9) + (entry.carbs * 4)).toInt(),
                        protein = entry.protein,
                        fat = entry.fat,
                        sugar = entry.sugar,
                        carbs = entry.carbs
                    )
                    dao.insertFoodItem(food)
                }
                dao.insertDailyLog(DailyLog(date = entry.date))
                dao.insertMealEntry(
                    MealEntry(
                        date = entry.date,
                        mealType = entry.mealType,
                        foodItemId = food.id,
                        servings = entry.servings
                    )
                )
                count++
            }
            onComplete(count)
        }
    }

    fun copyMealToDate(sourceDate: String, mealType: String, targetDate: String) {
        viewModelScope.launch {
            val entries = dao.getMealEntriesRaw(sourceDate, mealType)
            if (entries.isNotEmpty()) {
                dao.insertDailyLog(DailyLog(date = targetDate))
                entries.forEach { entry ->
                    dao.insertMealEntry(
                        MealEntry(
                            date = targetDate,
                            mealType = mealType,
                            foodItemId = entry.foodItemId,
                            servings = entry.servings
                        )
                    )
                }
            }
        }
    }
}