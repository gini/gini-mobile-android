package net.gini.android.health.sdk.util.extensions

import java.io.File
import java.io.FileOutputStream
import java.io.IOException

fun File.createTempPdfFile(byteArray: ByteArray, fileName: String): File? {
    val file = File("${this.path}/", "${fileName}.pdf")

    try {
        val os = FileOutputStream(file, false)
        os.write(byteArray)
        os.flush()
        os.close()

        return file
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return null
}