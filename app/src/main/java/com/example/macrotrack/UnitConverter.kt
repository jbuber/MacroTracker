package com.example.macrotrack

import java.util.regex.Pattern

data class ParsedServing(
    val baseQuantity: Float,
    val baseUnit: String,
    val metricQuantity: Float?,
    val metricUnit: String? // "g" or "ml"
)

object UnitConverter {
    // Regex to handle "2 tbsp (28g)", "1 slice (45g)", "3/4 cup (170g)", etc.
    private val regex = Pattern.compile("^(\\d+(?:[./]\\d+)?(?:/\\d+)?)\\s*(.*?)(?:\\s*\\(([\\d.]+)\\s*(g|ml)\\))?$")

    fun parseServingSize(servingSize: String): ParsedServing? {
        val trimmed = servingSize.trim().lowercase()
        val matcher = regex.matcher(trimmed)
        if (matcher.find()) {
            val qStr = matcher.group(1)
            val unit = matcher.group(2)?.trim() ?: ""
            val mQStr = matcher.group(3)
            val mUnit = matcher.group(4)

            val quantity = parseValue(qStr)
            val metricQuantity = mQStr?.toFloatOrNull()
            
            return ParsedServing(quantity, unit, metricQuantity, mUnit)
        }
        return null
    }

    private fun parseValue(s: String?): Float {
        if (s == null) return 1f
        if (s.contains("/")) {
            val parts = s.split("/")
            if (parts.size == 2) {
                val n = parts[0].trim().toFloatOrNull() ?: 0f
                val d = parts[1].trim().toFloatOrNull() ?: 1f
                return if (d != 0f) n / d else 0f
            }
        }
        return s.toFloatOrNull() ?: 1f
    }

    // Standard conversion factors to ml
    private val volumeToMl = mapOf(
        "cup" to 240f,
        "cups" to 240f,
        "tbsp" to 15f,
        "tsp" to 5f,
        "fl oz" to 30f,
        "ml" to 1f
    )

    // Standard conversion factors to grams
    private val weightToG = mapOf(
        "g" to 1f,
        "oz" to 28.35f,
        "lb" to 453.6f,
        "kg" to 1000f
    )

    fun calculateServings(
        amount: Float,
        selectedUnit: String,
        parsed: ParsedServing
    ): Float {
        val selectedLower = selectedUnit.lowercase()
        if (selectedLower == "servings") return amount

        // If user selected the base unit itself (e.g. "cake", "slice")
        if (selectedLower == parsed.baseUnit.lowercase()) {
            return amount / parsed.baseQuantity
        }

        // Try metric conversion
        val inputMl = volumeToMl[selectedLower]?.let { it * amount }
        val inputG = weightToG[selectedLower]?.let { it * amount }

        // Determine base metric value
        val baseMetricValue = parsed.metricQuantity ?: run {
            // Try to infer from baseUnit if it's a known volume/weight unit
            val v = volumeToMl[parsed.baseUnit.lowercase()]
            val w = weightToG[parsed.baseUnit.lowercase()]
            if (v != null) v * parsed.baseQuantity
            else if (w != null) w * parsed.baseQuantity
            else 1f // fallback to 1 to avoid division by zero
        }

        val isVolumeBase = parsed.metricUnit == "ml" || volumeToMl.containsKey(parsed.baseUnit.lowercase())
        
        if (isVolumeBase) {
            if (inputMl != null) return inputMl / baseMetricValue
            if (inputG != null) return inputG / baseMetricValue // Assume 1g = 1ml fallback
        } else {
            if (inputG != null) return inputG / baseMetricValue
            if (inputMl != null) return inputMl / baseMetricValue // Assume 1ml = 1g fallback
        }

        // Final fallback: identity
        return amount
    }

    fun getAvailableUnits(parsed: ParsedServing): List<String> {
        val units = mutableListOf("Servings")
        if (parsed.baseUnit.isNotBlank()) {
            units.add(parsed.baseUnit)
        }
        
        // Show both weight and volume units for flexibility
        units.addAll(listOf("g", "oz", "lb"))
        units.addAll(listOf("cup", "tbsp", "ml", "fl oz"))
        
        return units.distinct()
    }
}
