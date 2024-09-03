package net.gini.android.internal.payment.utils

import java.io.File
import java.io.FileOutputStream

internal fun File.createTempPdfFile(byteArray: ByteArray, fileName: String): File {
    val file = File("${this.path}/", "${fileName}.pdf")

     FileOutputStream(file, false).use { outputStream ->
         outputStream.write(byteArray)
         outputStream.flush()
     }

    return file
}