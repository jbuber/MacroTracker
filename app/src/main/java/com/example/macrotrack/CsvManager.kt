package com.example.macrotrack

import android.content.Context
import android.net.Uri
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object CsvManager {
    private const val HEADER = "Date,Meal,Food Name,Servings,Protein,Fat,Sugar,Carbs"

    private val dateFormats = listOf(
        DateTimeFormatter.ISO_DATE,
        DateTimeFormatter.ofPattern("M/d/yyyy"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy"),
        DateTimeFormatter.ofPattern("d/M/yyyy"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd")
    )

    fun normalizeDate(dateStr: String): String {
        val trimmed = dateStr.trim()
        for (format in dateFormats) {
            try {
                return LocalDate.parse(trimmed, format).format(DateTimeFormatter.ISO_DATE)
            } catch (e: DateTimeParseException) {
                // Try next format
            }
        }
        return trimmed // Fallback
    }

    fun generateCsv(entries: List<ExportEntry>): String {
        val sb = StringBuilder()
        sb.append(HEADER).append("\n")
        entries.forEach { entry ->
            val escapedName = if (entry.foodName.contains(",")) "\"${entry.foodName.replace("\"", "\"\"")}\"" else entry.foodName
            sb.append("${entry.date},")
                .append("${entry.mealType},")
                .append("$escapedName,")
                .append("${entry.servings},")
                .append("${entry.protein},")
                .append("${entry.fat},")
                .append("${entry.sugar},")
                .append("${entry.carbs}\n")
        }
        return sb.toString()
    }

    fun parseCsv(context: Context, uri: Uri): List<ExportEntry> {
        val entries = mutableListOf<ExportEntry>()
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var line = reader.readLine()
                // Skip BOM if present and handle header
                if (line != null && line.startsWith("\uFEFF")) {
                    line = line.substring(1)
                }
                
                if (line != null && line.lowercase().startsWith("date")) {
                    line = reader.readLine() // Actually skip the header
                }

                while (line != null) {
                    val parts = parseCsvLine(line)
                    if (parts.size >= 8) {
                        try {
                            entries.add(
                                ExportEntry(
                                    date = normalizeDate(parts[0]),
                                    mealType = parts[1].trim(),
                                    foodName = parts[2].trim(' ', '\"'),
                                    servings = parts[3].trim().toFloat(),
                                    protein = parts[4].trim().toFloat(),
                                    fat = parts[5].trim().toFloat(),
                                    sugar = parts[6].trim().toFloat(),
                                    carbs = parts[7].trim().toFloat()
                                )
                            )
                        } catch (e: Exception) {
                            // Skip malformed lines
                        }
                    }
                    line = reader.readLine()
                }
            }
        }
        return entries
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        for (char in line) {
            when {
                char == '\"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current = StringBuilder()
                }
                else -> current.append(char)
            }
        }
        result.add(current.toString())
        return result
    }
}
