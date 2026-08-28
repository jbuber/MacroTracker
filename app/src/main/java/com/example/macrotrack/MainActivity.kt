package com.example.macrotrack

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.macrotrack.data.AppDatabase
import com.example.macrotrack.data.DatabaseInitializer
import com.example.macrotrack.data.FoodItemEntity
import com.example.macrotrack.data.FoodLogEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import com.example.macrotrack.MacroViewModel
import com.example.macrotrack.ui.MacroTrackTheme
// OR if you are referencing the class directly:

import androidx.lifecycle.ViewModel

import com.example.macrotrack.ui.MacroTrackTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = AppDatabase.getDatabase(this)

        lifecycleScope.launch {
            DatabaseInitializer.populateDatabase(this@MainActivity, database)
        }

        setContent {
            MacroTrackTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: MacroViewModel = viewModel()
                    DailyLogScreen(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MacroTrackerMainScreen(database: AppDatabase) {
    // 1. INITIALIZE YOUR VIEWMODEL HERE
    val viewModel: MacroViewModel = viewModel()

    // 2. COLLECT STATES FROM THE VIEWMODEL
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()

    var selectedFood by remember { mutableStateOf<FoodItemEntity?>(null) }
    var amountMultiplier by remember { mutableStateOf("1.0") }
    var selectedMealType by remember { mutableStateOf("Breakfast") }

    var loggedItems by remember { mutableStateOf<List<FoodLogEntity>>(emptyList()) }
    var showGraphView by remember { mutableStateOf(false) }

    // State to control your custom food popup dialog
    var showAddDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    // Load logs on start
    LaunchedEffect(Unit) {
        loggedItems = withContext(Dispatchers.IO) { database.foodDao().getAllLogs() }
    }

    if (showGraphView) {
        // Graph View Screen
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Button(onClick = { showGraphView = false }) {
                Text("Back to Tracker")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Past Month Macro & Sugar Trends", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(24.dp))
            Box(modifier = Modifier.fillMaxWidth().height(250.dp), contentAlignment = Alignment.Center) {
                Text("📈 Graph for Protein, Carbs, Fat, and Sugar goes here")
            }
        }
    } else {
        // Main Tracker Screen
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("MacroTrack", style = MaterialTheme.typography.headlineMedium)
                Button(onClick = { showGraphView = true }) {
                    Text("View Past Month Graph")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. USE THE PROPER VIEWMODEL INSTANCE FOR SEARCH
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { query ->
                    viewModel.updateSearchQuery(query)
                },
                label = { Text("Search food...") },
                modifier = Modifier.fillMaxWidth()
            )

            // 4. ADD BUTTON FOR UNLISTED / CUSTOM FOOD ITEMS
            TextButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("+ Can't find food? Add custom item")
            }

            // Search Results Popup/List preview
            if (searchResults.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(searchResults) { food ->
                            ListItem(
                                headlineContent = { Text(food.name) },
                                supportingContent = { Text("${food.brand} - ${food.servingSize}") },
                                trailingContent = {
                                    Text("${food.calories} kcal", style = MaterialTheme.typography.bodySmall)
                                },
                                modifier = Modifier.clickable {
                                    selectedFood = food
                                    viewModel.updateSearchQuery("")
                                }
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
                        }
                    }
                }
            }

            // If a food is selected, prompt for amount/count and Meal Section
            selectedFood?.let { food ->
                Spacer(modifier = Modifier.height(8.dp))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Selected: ${food.name}", style = MaterialTheme.typography.titleMedium)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = amountMultiplier,
                                onValueChange = { amountMultiplier = it },
                                label = { Text("Amount / Count") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Select Meal Section:")
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            listOf("Breakfast", "Lunch", "Dinner", "Snack").forEach { meal ->
                                Button(
                                    onClick = { selectedMealType = meal },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selectedMealType == meal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                    )
                                ) {
                                    Text(meal)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = {
                            val mult = amountMultiplier.toFloatOrNull() ?: 1.0f
                            val newLog = FoodLogEntity(
                                foodName = food.name,
                                mealType = selectedMealType,
                                multiplier = mult,
                                calories = (food.calories * mult).toInt(),
                                protein = food.protein * mult,
                                carbs = food.carbs * mult,
                                fat = food.fat * mult,
                                sugar = food.sugar * mult
                            )
                            coroutineScope.launch {
                                withContext(Dispatchers.IO) {
                                    database.foodDao().insertLog(newLog)
                                    loggedItems = database.foodDao().getAllLogs()
                                }
                                selectedFood = null
                                amountMultiplier = "1.0"
                            }
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text("Add to $selectedMealType")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Display Sections: Breakfast, Lunch, Dinner, Snack with Delete option
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf("Breakfast", "Lunch", "Dinner", "Snack").forEach { mealCategory ->
                    item {
                        Text(text = mealCategory, style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(4.dp))

                        val categoryItems = loggedItems.filter { it.mealType == mealCategory }
                        if (categoryItems.isEmpty()) {
                            Text("No items logged yet.", style = MaterialTheme.typography.bodySmall)
                        } else {
                            categoryItems.forEach { logItem ->
                                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("${logItem.foodName} (x${logItem.multiplier})")
                                            Text(
                                                "Cal: ${logItem.calories} | P: ${logItem.protein}g | C: ${logItem.carbs}g | F: ${logItem.fat}g | S: ${logItem.sugar}g",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                        IconButton(onClick = {
                                            coroutineScope.launch {
                                                withContext(Dispatchers.IO) {
                                                    database.foodDao().deleteLog(logItem)
                                                    loggedItems = database.foodDao().getAllLogs()
                                                }
                                            }
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete Entry")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 5. DIALOG POPUP FOR CUSTOM FOOD INSERTION
    if (showAddDialog) {
        AddCustomFoodDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, serving, protein, fat, sugar, carbs ->
                viewModel.insertCustomFood(
                    name = name,
                    servingSize = serving,
                    protein = protein,
                    fat = fat,
                    sugar = sugar,
                    carbs = carbs
                )
            }
        )
    }
}