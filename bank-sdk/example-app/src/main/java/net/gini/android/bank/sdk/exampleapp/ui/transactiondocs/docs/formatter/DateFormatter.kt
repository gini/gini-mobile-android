package net.gini.android.bank.sdk.exampleapp.ui.transactiondocs.docs.formatter

import java.text.SimpleDateFormat

internal class DateFormatter {

    private val formatter = SimpleDateFormat("MMM dd, yyyy")

    fun format(time: Long): String = formatter.format(time)
}
