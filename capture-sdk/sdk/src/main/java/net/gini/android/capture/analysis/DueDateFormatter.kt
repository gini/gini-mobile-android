package net.gini.android.capture.analysis

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

/**
 * Formats a due date extraction value (`yyyy-MM-dd`) into the local display
 * style (`dd.MM.yyyy`). Falls back to the raw value if parsing fails.
 */
internal object DueDateFormatter {

    private const val INPUT_PATTERN = "yyyy-MM-dd"
    private const val OUTPUT_PATTERN = "dd.MM.yyyy"

    fun formatToLocalStyle(dateString: String): String {
        return try {
            val inputFormatter = DateTimeFormatter.ofPattern(INPUT_PATTERN, Locale.getDefault())
            val outputFormatter = DateTimeFormatter.ofPattern(OUTPUT_PATTERN, Locale.getDefault())
            LocalDate.parse(dateString, inputFormatter).format(outputFormatter)
        } catch (_: DateTimeParseException) {
            dateString // fallback if parsing fails
        }
    }
}
