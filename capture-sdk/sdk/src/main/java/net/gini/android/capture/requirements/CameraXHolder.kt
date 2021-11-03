package net.gini.android.capture.requirements

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.view.SurfaceHolder
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import net.gini.android.capture.internal.camera.api.camerax.CameraLifecycle
import net.gini.android.capture.internal.util.Size
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.RejectedExecutionException

private val LOG: Logger = LoggerFactory.getLogger(CameraXHolder::class.java)

class CameraXHolder(context: Context) : CameraHolder {

    private val cameraLifecycle = CameraLifecycle()
    private var camera: Camera? = null
    private var cameraCharacteristics: CameraCharacteristics? = null

    init {
        val cameraProviderFuture = try {
            ProcessCameraProvider.getInstance(context)
        } catch (e: IllegalStateException) {
            LOG.error("Failed to get ProcessCameraProvider instance future", e)
            null
        }

        try {
            val cameraProvider = try {
                cameraProviderFuture?.get()
            } catch (e: Exception) {
                LOG.error("Failed to get ProcessCameraProvider instance", e)
                null
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider?.unbindAll()

                camera = cameraProvider?.bindToLifecycle(
                    cameraLifecycle,
                    cameraSelector,
                    Preview.Builder().build()
                )

                camera?.let {
                    @SuppressLint("RestrictedApi", "UnsafeOptInUsageError")
                    cameraCharacteristics =
                        Camera2CameraInfo.extractCameraCharacteristics(it.cameraInfo)
                }

                camera?.let {
                    LOG.info("Camera is open")
                } ?: run {
                    LOG.error("Camera open failed")
                }
            } catch (e: Exception) {
                LOG.error("Opening camera failed", e)
            }
        } catch (e: RejectedExecutionException) {
            LOG.error("Failed to add ProcessCameraProvider listener", e)
        }

        cameraLifecycle.start()
    }

    override fun hasCamera(): Boolean {
        return camera != null
    }

    override fun hasAutoFocus(): Pair<Boolean, String> {
        return cameraCharacteristics?.let {
            val focusModes = it.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES)
            val hasAutoFocus = focusModes?.contains(CameraCharacteristics.CONTROL_AF_MODE_AUTO)
                ?: false
            return Pair(hasAutoFocus, "")
        } ?: Pair(false, "Camera not open")
    }

    override fun hasFlash(): Pair<Boolean, String> {
        return cameraCharacteristics?.let {
            val hasFlash =
                cameraCharacteristics?.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)
                    ?: false
            return Pair(hasFlash, "")
        } ?: Pair(false, "Camera not open")
    }

    override fun getSupportedPictureSizes(): List<Size>? {
        val streamConfigurationMap =
            cameraCharacteristics?.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        return streamConfigurationMap?.getOutputSizes(ImageFormat.JPEG)
            ?.map { Size(it.width, it.height) }
    }

    override fun getSupportedPreviewSizes(): List<Size>? {
        val streamConfigurationMap =
            cameraCharacteristics?.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        return streamConfigurationMap?.getOutputSizes(SurfaceHolder::class.java)
            ?.map { Size(it.width, it.height) }
    }

    override fun closeCamera() {
        cameraLifecycle.stop()
        camera = null
        cameraCharacteristics = null
    }

}