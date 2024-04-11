package net.gini.android.bank.sdk.exampleapp.test

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

@Throws(IOException::class)
fun getAssetFileStorageUri(assetFilePath: String): Uri {
    val fileDir: File = getFileDir()
    val file = File(fileDir, assetFilePath)
    copyAssetToStorage(assetFilePath, fileDir.path)
    return Uri.fromFile(file)
}

@Throws(IOException::class)
private fun getFileDir(): File {
    val context = ApplicationProvider.getApplicationContext<Context>()
    return context.filesDir
}

@Throws(IOException::class)
private fun copyAssetToStorage(
    assetFilePath: String,
    storageDirPath: String
) {
    val assetManager = ApplicationProvider.getApplicationContext<Context>().assets
    var inputStream: InputStream? = null
    var outputStream: OutputStream? = null
    try {
        inputStream = assetManager.open(assetFilePath)
        val file = File(
            storageDirPath,
            Uri.parse(assetFilePath).lastPathSegment!!
        )
        if (file.exists() || file.createNewFile()) {
            outputStream = FileOutputStream(file)
            copyFile(inputStream, outputStream)
        } else {
            throw IOException("Could not create file: " + file.absolutePath)
        }
    } finally {
        inputStream?.close()
        outputStream?.close()
    }
}

@Throws(IOException::class)
private fun copyFile(inputStream: InputStream, outputStream: OutputStream) {
    val buffer = ByteArray(8192)
    var read: Int
    while (inputStream.read(buffer).also { read = it } != -1) {
        outputStream.write(buffer, 0, read)
    }
}
