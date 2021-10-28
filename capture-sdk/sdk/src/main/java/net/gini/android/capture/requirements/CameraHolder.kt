package net.gini.android.capture.requirements

import net.gini.android.capture.internal.util.Size

interface CameraHolder {
    fun hasCamera(): Boolean
    fun hasAutoFocus(): Pair<Boolean, String>
    fun hasFlash(): Pair<Boolean, String>
    fun getSupportedPictureSizes(): List<Size>?
    fun getSupportedPreviewSizes(): List<Size>?
    fun closeCamera()
}