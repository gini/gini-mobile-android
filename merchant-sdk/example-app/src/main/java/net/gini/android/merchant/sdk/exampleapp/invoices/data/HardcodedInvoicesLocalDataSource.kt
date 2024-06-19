package net.gini.android.merchant.sdk.exampleapp.invoices.data

import android.content.Context

private val HARDCODED_INVOICES = listOf(
    "health-invoice-1.jpg",
    "health-invoice-2.jpg",
    "health-invoice-3.jpg",
    "health-invoice-4.jpg",
    "health-invoice-5.jpg",
)

class HardcodedInvoicesLocalDataSource(
    private val context: Context,
    private val hardcodedInvoiceFileNames: List<String> = HARDCODED_INVOICES
) {

    fun getHardcodedInvoices(): List<ByteArray> {
        return hardcodedInvoiceFileNames.map { filename ->
            context.resources.assets.open(filename).use { it.readBytes() }
        }
    }
}
