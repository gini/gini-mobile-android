package net.gini.android.health.api.test

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

object Helpers {

    @Throws(IOException::class)
    fun loadAsset(filename: String?): ByteArray {
        val assetManager = ApplicationProvider.getApplicationContext<Context>().assets
        var inputStream: InputStream? = null
        return try {
            inputStream = assetManager.open(filename!!)
            inputStreamToByteArray(inputStream)
        } finally {
            inputStream?.close()
        }
    }

    @Throws(IOException::class)
    private fun inputStreamToByteArray(inputStream: InputStream?): ByteArray {
        val outputStream = ByteArrayOutputStream()
        val bytes: ByteArray
        try {
            val buffer = ByteArray(8192)
            var readBytes: Int
            while (inputStream!!.read(buffer).also { readBytes = it } != -1) {
                outputStream.write(buffer, 0, readBytes)
            }
            bytes = outputStream.toByteArray()
        } finally {
            outputStream.close()
        }
        return bytes
    }

}