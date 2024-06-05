package net.gini.android.health.sdk.util.extensions

import java.io.File
import java.io.FileOutputStream
import java.io.IOException

fun File.createTempPdfFile(byteArray: ByteArray, fileName: String): File {
    val file = File("${this.path}/", "${fileName}.pdf")

     FileOutputStream(file, false).use { outputStream ->
         outputStream.write(byteArray)
         outputStream.flush()
     }

    return file
}