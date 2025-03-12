package net.gini.android.health.sdk.exampleapp.util

import android.icu.text.SimpleDateFormat
import android.os.Build
import androidx.annotation.RequiresApi
import java.io.ByteArrayOutputStream
import java.io.InputStream

fun InputStream.getBytes(): ByteArray {
    val byteBuffer = ByteArrayOutputStream()
    val buffer = ByteArray(1024)
    var len: Int
    while (this.read(buffer).also { len = it } != -1) {
        byteBuffer.write(buffer, 0, len)
    }
    return byteBuffer.toByteArray()
}

@RequiresApi(Build.VERSION_CODES.N)
fun String.prettifyDate(): String {
    val format = SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSS")
    val localDateFormat = SimpleDateFormat("dd MMM yyyy")

    val date = format.parse(this)
    return localDateFormat.format(date)
}