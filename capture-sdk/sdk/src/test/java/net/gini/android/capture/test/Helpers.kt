package net.gini.android.capture.test

import android.content.Context
import android.net.Uri
import android.os.Looper
import android.os.Parcel
import android.os.Parcelable
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import net.gini.android.capture.internal.util.ContextHelper
import org.robolectric.Shadows
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

object Helpers {
    @Throws(IOException::class)
    @JvmStatic
    fun getTestJpeg() = loadAsset("invoice.jpg")

    @Throws(Exception::class)
    fun loadJavaResource(filename: String?): ByteArray {
        val classLoader = Helpers::class.java.classLoader
        return File(classLoader!!.getResource(filename)!!.file).readBytes()
    }
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

    @Throws(IOException::class)
    @JvmStatic
    fun copyAssetToStorage(
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
                Uri.parse(assetFilePath).lastPathSegment
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

    fun <T : Parcelable?, C : Parcelable.Creator<T>?> doParcelingRoundTrip(
        payload: T, creator: C
    ): T {
        val parcel = Parcel.obtain()
        payload!!.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)
        // execute all tasks posted to main looper (https://robolectric.org/blog/2019/06/04/paused-looper/)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        return creator!!.createFromParcel(parcel)
    }

    fun isTablet(): Boolean {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        return ContextHelper.isTablet(instrumentation.targetContext)
    }
}