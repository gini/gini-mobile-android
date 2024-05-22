package net.gini.android.health.api

import android.content.Context

private val FILE_NAME = "test_pdf.pdf"

//TODO - remove this when no longer needed
class HardcodedFilesLocalDataSource(
    private val context: Context
) {
    fun loadTestPdf(): ByteArray = context.resources.assets.open(FILE_NAME).use { it.readBytes() }
}