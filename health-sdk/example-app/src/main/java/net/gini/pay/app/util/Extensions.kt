package net.gini.pay.app.util

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