package net.gini.android.capture.internal.camera.api

import android.hardware.Camera
import net.gini.android.capture.internal.util.Size
import java.util.function.Function
import java.util.stream.Collectors

/**
 * Internal use only.
 *
 * @suppress
 */
internal object CameraParametersHelper {
    @JvmStatic
    fun isFocusModeSupported(focusMode: String, camera: Camera): Boolean {
        return camera.parameters.supportedFocusModes.contains(focusMode)
    }

    @JvmStatic
    fun isUsingFocusMode(focusMode: String, camera: Camera): Boolean {
        return camera.parameters.focusMode == focusMode
    }

    @JvmStatic
    fun isFlashModeSupported(flashMode: String, camera: Camera): Boolean {
        val supportedFlashModes = camera.parameters.supportedFlashModes
        return supportedFlashModes != null && supportedFlashModes.contains(flashMode)
    }

    @JvmStatic
    fun getSupportedPictureSizes(cameraParams: Camera.Parameters): List<Size> {
        return cameraParams.supportedPictureSizes
            .map { size: Camera.Size ->
                Size(
                    size.width,
                    size.height
                )
            }
    }

    @JvmStatic
    fun getSupportedPreviewSizes(cameraParams: Camera.Parameters): List<Size> {
        return cameraParams.supportedPreviewSizes
            .map { size: Camera.Size ->
                Size(
                    size.width,
                    size.height
                )
            }
    }
}