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
        if (selectedUnit == "Servings") return amount

        // If user selected the base unit itself (e.g. "slice")
        if (selectedUnit.lowercase() == parsed.baseUnit.lowercase()) {
            return amount / parsed.baseQuantity
        }

        // Try metric conversion
        val inputMl = volumeToMl[selectedUnit.lowercase()]?.let { it * amount }
        val inputG = weightToG[selectedUnit.lowercase()]?.let { it * amount }

        // Determine base metric value
        val baseMetricValue = parsed.metricQuantity ?: run {
            // Try to infer from baseUnit if it's a known volume/weight unit
            val v = volumeToMl[parsed.baseUnit.lowercase()]
            val w = weightToG[parsed.baseUnit.lowercase()]
            if (v != null) v * parsed.baseQuantity
            else if (w != null) w * parsed.baseQuantity
            else null
        }

        if (baseMetricValue != null) {
            val isVolumeBase = parsed.metricUnit == "ml" || volumeToMl.containsKey(parsed.baseUnit.lowercase())
            
            if (isVolumeBase && inputMl != null) {
                // Volume to Volume
                return inputMl / baseMetricValue
            }
            
            if (!isVolumeBase && inputG != null) {
                // Weight to Weight
                return inputG / baseMetricValue
            }

            // Cross conversion (Density handling)
            // If user enters 'g' but base is 'ml', we assume 1g = 1ml (common for many foods/liquids)
            if (inputG != null && isVolumeBase) return inputG / baseMetricValue
            if (inputMl != null && !isVolumeBase) return inputMl / baseMetricValue
        }

        // Final fallback: identity or best effort
        return amount
    }
    
    fun getAvailableUnits(parsed: ParsedServing): List<String> {
        val units = mutableListOf("Servings")
        val base = parsed.baseUnit.lowercase()
        if (base.isNotBlank()) {
            units.add(parsed.baseUnit)
        }
        
        val isVolume = parsed.metricUnit == "ml" || 
                      volumeToMl.keys.any { base.contains(it) }
        
        if (isVolume) {
            units.addAll(listOf("ml", "cup", "tbsp", "fl oz"))
        } else {
            units.addAll(listOf("g", "oz", "lb"))
        }
        
        return units.distinct()
    }
}
