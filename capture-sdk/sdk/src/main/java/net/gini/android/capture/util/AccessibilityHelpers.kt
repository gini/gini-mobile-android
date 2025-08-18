package net.gini.android.capture.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private const val ST_KEY_WORD = "st"
private const val ND_KEY_WORD = "nd"
private const val RD_KEY_WORD = "rd"
private const val TH_KEY_WORD = "th"

private val ST_DAYS = listOf(1, 21, 31)
private val ND_DAYS = listOf(2, 22)
private val RD_DAYS = listOf(3, 23)

/**
 * TalkBack does not read the date correctly on some devices,
 * for example we are using dd.MM.YYYY format, and if we select 01.09.2023,
 * instead of reading "1st of September 2023", it reads "9 january 2023".
 * That's why we need to set content description for the date field, with a spoken format which
 * is constructed by this function [getSpokenDateForTalkBack]
 * */

fun getSpokenDateForTalkBack(date: String): String {

    val inputFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    val parsedDate = inputFormat.parse(date) ?: return date

    val locale = Locale.getDefault()

    if (locale == Locale.ENGLISH) {
        val calendar = Calendar.getInstance().apply { time = parsedDate }
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val daySuffix = when (day) {
            in ST_DAYS -> ST_KEY_WORD
            in ND_DAYS -> ND_KEY_WORD
            in RD_DAYS -> RD_KEY_WORD
            else -> TH_KEY_WORD
        }

        val monthName = SimpleDateFormat("MMMM", locale).format(parsedDate)
        val year = calendar.get(Calendar.YEAR)

        return "$day$daySuffix of $monthName $year"
    }
    // This is for all the other locals, because we don't need suffix (1st, 3rd etc)
    val spokenFormat = SimpleDateFormat("d. MMMM yyyy", locale)
    return spokenFormat.format(parsedDate)
}
