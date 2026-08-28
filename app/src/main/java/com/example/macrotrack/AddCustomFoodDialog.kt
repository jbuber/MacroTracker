package com.example.macrotrack

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AddCustomFoodDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, serving: String, protein: Float, fat: Float, sugar: Float, carbs: Float) -> Unit
) {
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
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Food Name") })
                OutlinedTextField(value = servingSize, onValueChange = { servingSize = it }, label = { Text("Serving Size (e.g., 1 cup)") })
                OutlinedTextField(value = protein, onValueChange = { protein = it }, label = { Text("Protein (g)") })
                OutlinedTextField(value = fat, onValueChange = { fat = it }, label = { Text("Fat (g)") })
                OutlinedTextField(value = carbs, onValueChange = { carbs = it }, label = { Text("Carbs (g)") })
                OutlinedTextField(value = sugar, onValueChange = { sugar = it }, label = { Text("Sugar (g)") })
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    name,
                    servingSize,
                    protein.toFloatOrNull() ?: 0f,
                    fat.toFloatOrNull() ?: 0f,
                    sugar.toFloatOrNull() ?: 0f,
                    carbs.toFloatOrNull() ?: 0f
                )
                onDismiss()
            }) {
                Text("Save & Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}