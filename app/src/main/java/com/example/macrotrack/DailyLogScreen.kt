package com.example.macrotrack

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.macrotrack.data.FoodItemEntity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyLogScreen(viewModel: MacroViewModel) {
    val context = LocalContext.current
    val currentDate by viewModel.currentDate.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()

    val proteinGoal by viewModel.proteinGoal.collectAsState()
    val fatGoal by viewModel.fatGoal.collectAsState()
    val sugarGoal by viewModel.sugarGoal.collectAsState()
    val carbsGoal by viewModel.carbsGoal.collectAsState()

    var selectedMealType by remember { mutableStateOf("Breakfast") }
    var selectedFood by remember { mutableStateOf<FoodItemEntity?>(null) }
    var servingInput by remember { mutableStateOf("1.0") }
    var selectedUnit by remember { mutableStateOf("Servings") }
    var expanded by remember { mutableStateOf(false) }
    var unitDropdownExpanded by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showGraphView by remember { mutableStateOf(false) }
    var showCustomFoodDialog by remember { mutableStateOf(false) }
    var showGoalsDialog by remember { mutableStateOf(false) }

    var graphData by remember { mutableStateOf<List<DailyTotals>>(emptyList()) }

    LaunchedEffect(showGraphView) {
        if (showGraphView) {
            viewModel.getPastMonthData { data ->
                graphData = data
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            viewModel.exportData { csv ->
                context.contentResolver.openOutputStream(it)?.use { stream ->
                    stream.write(csv.toByteArray())
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.importData(context, it) { count ->
                Toast.makeText(context, "Imported $count entries successfully!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val mealTypes = listOf("Breakfast", "Lunch", "Dinner", "Snack")

    // Collect entries for macro totals calculation
    val breakfastEntries by viewModel.getEntriesForMeal("Breakfast").collectAsState(initial = emptyList())
    val lunchEntries by viewModel.getEntriesForMeal("Lunch").collectAsState(initial = emptyList())
    val dinnerEntries by viewModel.getEntriesForMeal("Dinner").collectAsState(initial = emptyList())
    val snackEntries by viewModel.getEntriesForMeal("Snack").collectAsState(initial = emptyList())

    val allEntries = breakfastEntries + lunchEntries + dinnerEntries + snackEntries
    val totalProtein = allEntries.sumOf { (it.protein * it.servings).toDouble() }.toFloat()
    val totalFat = allEntries.sumOf { (it.fat * it.servings).toDouble() }.toFloat()
    val totalSugar = allEntries.sumOf { (it.sugar * it.servings).toDouble() }.toFloat()
    val totalCarbs = allEntries.sumOf { (it.carbs * it.servings).toDouble() }.toFloat()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Image(
                        painter = painterResource(id = R.drawable.banner),
                        contentDescription = "Macro Tracker",
                        modifier = Modifier.height(40.dp),
                        contentScale = ContentScale.Fit
                    )
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("View Past Month Graph") },
                                onClick = {
                                    showMenu = false
                                    showGraphView = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Update Macro Goals") },
                                onClick = {
                                    showMenu = false
                                    showGoalsDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export CSV") },
                                onClick = {
                                    showMenu = false
                                    exportLauncher.launch("macrotrack_export_${System.currentTimeMillis()}.csv")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Import CSV") },
                                onClick = {
                                    showMenu = false
                                    importLauncher.launch(arrayOf("text/comma-separated-values", "text/csv", "*/*"))
                                }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "v1.9.2-ExpandedStore",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (showGraphView) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                ) {
                    Button(onClick = { showGraphView = false }) {
                        Text("Back to Tracker")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Past Month Macro & Sugar Trends", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    MacroGraph(data = graphData)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Legend()
                    
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        "Showing data for the last 30 days.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // --- Daily Macro Summary & Progress Bars ---
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.9f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Daily Macro Progress", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(12.dp))

                                MacroProgressBar("Protein", totalProtein, proteinGoal)
                                Spacer(modifier = Modifier.height(8.dp))
                                MacroProgressBar("Fat", totalFat, fatGoal)
                                Spacer(modifier = Modifier.height(8.dp))
                                MacroProgressBar("Sugar", totalSugar, sugarGoal)
                                Spacer(modifier = Modifier.height(8.dp))
                                MacroProgressBar("Carbs", totalCarbs, carbsGoal)
                            }
                        }
                    }

                    // --- Add Food Section ---
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Add Food Item", style = MaterialTheme.typography.titleMedium)
                                    TextButton(onClick = { showCustomFoodDialog = true }) {
                                        Text("+ Custom Food")
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    mealTypes.forEach { meal ->
                                        Button(
                                            onClick = { selectedMealType = meal },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (selectedMealType == meal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
                                            )
                                        ) {
                                            Text(meal, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                ExposedDropdownMenuBox(
                                    expanded = expanded,
                                    onExpandedChange = { expanded = !expanded }
                                ) {
                                    OutlinedTextField(
                                        value = searchQuery,
                                        onValueChange = {
                                            viewModel.updateSearchQuery(it)
                                            expanded = true
                                        },
                                        label = { Text("Search Aldi Items...") },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor()
                                    )

                                    ExposedDropdownMenu(
                                        expanded = expanded && searchResults.isNotEmpty(),
                                        onDismissRequest = { expanded = false }
                                    ) {
                                        searchResults.forEach { item ->
                                            DropdownMenuItem(
                                                text = { Text("${item.name} (${item.servingSize})") },
                                                trailingIcon = {
                                                    IconButton(onClick = { viewModel.toggleFavorite(item) }) {
                                                        Icon(
                                                            imageVector = if (item.isFavorite) Icons.Default.Star else Icons.Outlined.Star,
                                                            contentDescription = "Favorite",
                                                            tint = if (item.isFavorite) Color(0xFFFFD700) else MaterialTheme.colorScheme.outline
                                                        )
                                                    }
                                                },
                                                onClick = {
                                                    selectedFood = item
                                                    viewModel.updateSearchQuery(item.name)
                                                    expanded = false
                                                    val parsed = UnitConverter.parseServingSize(item.servingSize)
                                                    if (parsed != null) {
                                                        selectedUnit = parsed.baseUnit
                                                        servingInput = if (parsed.baseQuantity % 1f == 0f) 
                                                            parsed.baseQuantity.toInt().toString() 
                                                            else parsed.baseQuantity.toString()
                                                    } else {
                                                        selectedUnit = "Servings"
                                                        servingInput = "1"
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                selectedFood?.let { food ->
                                    Text(
                                        text = "Selected: ${food.name}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Base serving: ${food.servingSize}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = servingInput,
                                        onValueChange = { servingInput = it },
                                        label = { Text("Amount") },
                                        modifier = Modifier.weight(1f)
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    val parsed = selectedFood?.let { UnitConverter.parseServingSize(it.servingSize) }
                                    val units = parsed?.let { UnitConverter.getAvailableUnits(it) } ?: listOf("Servings")

                                    ExposedDropdownMenuBox(
                                        expanded = unitDropdownExpanded,
                                        onExpandedChange = { unitDropdownExpanded = !unitDropdownExpanded },
                                        modifier = Modifier.weight(1.2f)
                                    ) {
                                        OutlinedTextField(
                                            value = selectedUnit,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Unit") },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitDropdownExpanded) },
                                            modifier = Modifier.menuAnchor(),
                                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                                        )
                                        ExposedDropdownMenu(
                                            expanded = unitDropdownExpanded,
                                            onDismissRequest = { unitDropdownExpanded = false }
                                        ) {
                                            units.forEach { unit ->
                                                DropdownMenuItem(
                                                    text = { Text(unit) },
                                                    onClick = {
                                                        selectedUnit = unit
                                                        unitDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = {
                                        val amount = servingInput.toFloatOrNull() ?: 1f
                                        selectedFood?.let { food ->
                                            val parsed = UnitConverter.parseServingSize(food.servingSize)
                                            val servings = if (parsed != null) {
                                                UnitConverter.calculateServings(amount, selectedUnit, parsed)
                                            } else {
                                                amount
                                            }
                                            viewModel.addMealEntry(selectedMealType, food.id, servings)
                                            selectedFood = null
                                            viewModel.updateSearchQuery("")
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = selectedFood != null
                                ) {
                                    Text("Add to $selectedMealType")
                                }
                            }
                        }
                    }

                    // --- Date Selection ---
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { viewModel.changeDate(-1) }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Previous Day")
                            }
                            Text(
                                text = currentDate,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            IconButton(onClick = { viewModel.changeDate(1) }) {
                                Icon(Icons.Default.ArrowForward, contentDescription = "Next Day")
                            }
                        }
                    }
                }

                // --- Display Meal Categories with Delete Support ---
                items(mealTypes) { meal ->
                    MealCategoryCard(meal, viewModel)
                }
                }
            }
        }
    }

    if (showCustomFoodDialog) {
        CustomFoodDialog(
            onDismiss = { showCustomFoodDialog = false },
            onSave = { name, serving, p, f, s, c ->
                viewModel.insertCustomFood(name, serving, p, f, s, c)
                showCustomFoodDialog = false
            }
        )
    }

    if (showGoalsDialog) {
        MacroGoalsDialog(
            currentProtein = proteinGoal,
            currentFat = fatGoal,
            currentSugar = sugarGoal,
            currentCarbs = carbsGoal,
            onDismiss = { showGoalsDialog = false },
            onSave = { p, f, s, c ->
                viewModel.updateGoals(p, f, s, c)
                showGoalsDialog = false
            }
        )
    }
}

@Composable
fun MacroGoalsDialog(
    currentProtein: Float,
    currentFat: Float,
    currentSugar: Float,
    currentCarbs: Float,
    onDismiss: () -> Unit,
    onSave: (Float, Float, Float, Float) -> Unit
) {
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Male") }
    
    var protein by remember { mutableStateOf(currentProtein.toInt().toString()) }
    var fat by remember { mutableStateOf(currentFat.toInt().toString()) }
    var sugar by remember { mutableStateOf(currentSugar.toInt().toString()) }
    var carbs by remember { mutableStateOf(currentCarbs.toInt().toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Daily Macro Goals") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Calculator (Optional)", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = weight, onValueChange = { weight = it }, label = { Text("Weight (lb)") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = height, onValueChange = { height = it }, label = { Text("Height (in)") }, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = age, onValueChange = { age = it }, label = { Text("Age") }, modifier = Modifier.fillMaxWidth())
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = gender == "Male", onClick = { gender = "Male" })
                    Text("Male")
                    Spacer(modifier = Modifier.width(8.dp))
                    RadioButton(selected = gender == "Female", onClick = { gender = "Female" })
                    Text("Female")
                }

                Button(
                    onClick = {
                        val w = weight.toFloatOrNull()?.let { it * 0.453592f } ?: 0f // to kg
                        val h = height.toFloatOrNull()?.let { it * 2.54f } ?: 0f // to cm
                        val a = age.toIntOrNull() ?: 0
                        
                        if (w > 0 && h > 0 && a > 0) {
                            val bmr = if (gender == "Male") {
                                10 * w + 6.25 * h - 5 * a + 5
                            } else {
                                10 * w + 6.25 * h - 5 * a - 161
                            }
                            val tdee = bmr * 1.2f // Sedentary baseline
                            
                            // Simple macro split: 30% Protein, 30% Fat, 40% Carbs
                            protein = (tdee * 0.3f / 4f).toInt().toString()
                            fat = (tdee * 0.3f / 9f).toInt().toString()
                            carbs = (tdee * 0.4f / 4f).toInt().toString()
                            sugar = "50" // Default sugar limit
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Calculate Suggestions")
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text("Manual Goals", style = MaterialTheme.typography.labelLarge)
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = protein, onValueChange = { protein = it }, label = { Text("Protein (g)") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = fat, onValueChange = { fat = it }, label = { Text("Fat (g)") }, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = sugar, onValueChange = { sugar = it }, label = { Text("Sugar (g)") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = carbs, onValueChange = { carbs = it }, label = { Text("Carbs (g)") }, modifier = Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val p = protein.toFloatOrNull() ?: 0f
                val f = fat.toFloatOrNull() ?: 0f
                val s = sugar.toFloatOrNull() ?: 0f
                val c = carbs.toFloatOrNull() ?: 0f
                onSave(p, f, s, c)
            }) {
                Text("Save Goals")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun MacroProgressBar(label: String, current: Float, goal: Float) {
    val progress = (current / goal).coerceIn(0f, 1f)
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("$label: ${current}g / ${goal}g", style = MaterialTheme.typography.bodySmall)
            Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
        )
    }
}

@Composable
fun CustomFoodDialog(onDismiss: () -> Unit, onSave: (String, String, Float, Float, Float, Float) -> Unit) {
    var name by remember { mutableStateOf("") }
    var servingSize by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var sugar by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Custom Food Item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Food Name") })
                OutlinedTextField(value = servingSize, onValueChange = { servingSize = it }, label = { Text("Serving Size (e.g., 100g)") })
                OutlinedTextField(value = protein, onValueChange = { protein = it }, label = { Text("Protein (g)") })
                OutlinedTextField(value = fat, onValueChange = { fat = it }, label = { Text("Fat (g)") })
                OutlinedTextField(value = sugar, onValueChange = { sugar = it }, label = { Text("Sugar (g)") })
                OutlinedTextField(value = carbs, onValueChange = { carbs = it }, label = { Text("Carbs (g)") })
            }
        },
        confirmButton = {
            Button(onClick = {
                val p = protein.toFloatOrNull() ?: 0f
                val f = fat.toFloatOrNull() ?: 0f
                val s = sugar.toFloatOrNull() ?: 0f
                val c = carbs.toFloatOrNull() ?: 0f
                if (name.isNotBlank()) {
                    onSave(name, servingSize.ifBlank { "1 serving" }, p, f, s, c)
                }
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealCategoryCard(mealType: String, viewModel: MacroViewModel) {
    val entries by viewModel.getEntriesForMeal(mealType).collectAsState(initial = emptyList())
    val currentDate by viewModel.currentDate.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(mealType, style = MaterialTheme.typography.titleLarge)
                if (entries.isNotEmpty()) {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy meal to date",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            if (entries.isEmpty()) {
                Text("No items logged yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
            } else {
                entries.forEach { entry ->
                    val p = entry.protein * entry.servings
                    val f = entry.fat * entry.servings
                    val s = entry.sugar * entry.servings
                    val c = entry.carbs * entry.servings

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(entry.name, style = MaterialTheme.typography.bodyMedium)
                            Text("Servings: ${entry.servings} | P:${p}g F:${f}g S:${s}g C:${c}g", style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { viewModel.deleteMealEntry(entry.entryId) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete entry", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val targetDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                            .format(DateTimeFormatter.ISO_DATE)
                        viewModel.copyMealToDate(currentDate, mealType, targetDate)
                    }
                    showDatePicker = false
                }) {
                    Text("Copy")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun MacroGraph(data: List<DailyTotals>) {
    val proteinColor = Color(0xFF4CAF50) // Green
    val fatColor = Color(0xFFFFC107)    // Amber
    val carbsColor = Color(0xFF2196F3)   // Blue
    val sugarColor = Color(0xFFF44336)   // Red

    if (data.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
            Text("No data available for the last 30 days.")
        }
        return
    }

    val maxVal = data.maxOf { maxOf(it.protein, it.fat, it.carbs, it.sugar) }.coerceAtLeast(100f) * 1.2f

    Canvas(modifier = Modifier
        .fillMaxWidth()
        .height(250.dp)
        .padding(horizontal = 8.dp)
        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        val width = size.width
        val height = size.height
        val spacing = width / (data.size - 1).coerceAtLeast(1)

        fun drawLineForMacro(color: Color, getValue: (DailyTotals) -> Float) {
            val path = Path()
            data.forEachIndexed { index, totals ->
                val x = index * spacing
                val y = height - (getValue(totals) / maxVal) * height
                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }
            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 3.dp.toPx())
            )
            
            // Draw points
            data.forEachIndexed { index, totals ->
                val x = index * spacing
                val y = height - (getValue(totals) / maxVal) * height
                drawCircle(
                    color = color,
                    radius = 4.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }

        drawLineForMacro(proteinColor) { it.protein }
        drawLineForMacro(fatColor) { it.fat }
        drawLineForMacro(carbsColor) { it.carbs }
        drawLineForMacro(sugarColor) { it.sugar }
        
        // Draw baseline
        drawLine(Color.Gray.copy(alpha = 0.5f), Offset(0f, height), Offset(width, height), strokeWidth = 1f)
    }
}

@Composable
fun Legend() {
    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        LegendItem("Protein", Color(0xFF4CAF50))
        LegendItem("Fat", Color(0xFFFFC107))
        LegendItem("Carbs", Color(0xFF2196F3))
        LegendItem("Sugar", Color(0xFFF44336))
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(12.dp).background(color))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
