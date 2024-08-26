package net.gini.android.capture.util

import android.os.Build
import net.gini.android.capture.BuildConfig
import net.gini.android.capture.EntryPoint
import net.gini.android.capture.GiniCapture

internal object UploadMetadataBuilder {

    private const val USER_COMMENT_PLATFORM = "Platform"
    private const val USER_COMMENT_OS_VERSION = "OSVer"
    private const val USER_COMMENT_GINI_CAPTURE_VERSION = "GiniVisionVer"
    private const val USER_COMMENT_DEVICE_ORIENTATION = "DeviceOrientation"
    private const val USER_COMMENT_DEVICE_TYPE = "DeviceType"
    private const val USER_COMMENT_SOURCE = "Source"
    private const val USER_COMMENT_IMPORT_METHOD = "ImportMethod"
    private const val USER_COMMENT_ENTRY_POINT = "EntryPoint"

    private var giniCaptureVersion: String = ""
    private var deviceOrientation: String = ""
    private var deviceType: String = ""
    private var source: String = ""
    private var importMethod: String = ""

    private fun convertMapToCSV(keyValueMap: Map<String, String>): String {
        val csvBuilder = StringBuilder()
        var isFirst = true
        for ((key, value) in keyValueMap) {
            if (!isFirst) {
                csvBuilder.append(',')
            }
            isFirst = false
            csvBuilder.append(key)
                .append('=')
                .append(value)
        }
        return csvBuilder.toString()
    }

    fun setDeviceOrientation(deviceOrientation: String): UploadMetadataBuilder =
        this.also { it.deviceOrientation = deviceOrientation }

    fun setDeviceType(deviceType: String): UploadMetadataBuilder =
        this.also { it.deviceType = deviceType }

    fun setSource(source: String): UploadMetadataBuilder = this.also { it.source = source }

    fun setImportMethod(importMethod: String): UploadMetadataBuilder = this.also { it.importMethod = importMethod }

    fun build(): String {
        val metadataMap = mutableMapOf<String, String>()

        metadataMap[USER_COMMENT_PLATFORM] = "Android"
        metadataMap[USER_COMMENT_OS_VERSION] = Build.VERSION.RELEASE.toString()
        if (giniCaptureVersion.isNotEmpty()) {
            metadataMap[USER_COMMENT_GINI_CAPTURE_VERSION] = giniCaptureVersion
        }
        if (deviceOrientation.isNotEmpty()) {
            metadataMap[USER_COMMENT_DEVICE_ORIENTATION] = deviceOrientation
        }
        if (deviceType.isNotEmpty()) {
            metadataMap[USER_COMMENT_DEVICE_TYPE] = deviceType
        }
        if (source.isNotEmpty()) {
            metadataMap[USER_COMMENT_SOURCE] = source
        }
        if (importMethod.isNotEmpty()) {
            metadataMap[USER_COMMENT_IMPORT_METHOD] = importMethod
        }
        metadataMap[USER_COMMENT_GINI_CAPTURE_VERSION] = BuildConfig.VERSION_NAME.replace(" ", "")

        if (GiniCapture.hasInstance()) {
            metadataMap[USER_COMMENT_ENTRY_POINT] = entryPointToString(GiniCapture.getInstance().getEntryPoint())
        } else {
            metadataMap[USER_COMMENT_ENTRY_POINT] = entryPointToString(GiniCapture.Internal.DEFAULT_ENTRY_POINT)
        }
        return convertMapToCSV(metadataMap)
    }

    private fun entryPointToString(entryPoint: EntryPoint) = when (entryPoint) {
            EntryPoint.FIELD -> "field"
            EntryPoint.BUTTON -> "button"
    }
}