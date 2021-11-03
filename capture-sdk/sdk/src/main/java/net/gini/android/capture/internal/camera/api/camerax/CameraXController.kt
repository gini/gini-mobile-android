package net.gini.android.capture.internal.camera.api.camerax

import android.app.Activity
import android.content.Context
import android.graphics.*
import android.media.MediaActionSound
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.MotionEvent
import android.view.Surface
import android.view.View
import android.view.ViewConfiguration
import androidx.annotation.RequiresApi
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import jersey.repackaged.jsr166e.CompletableFuture
import net.gini.android.capture.Document
import net.gini.android.capture.internal.camera.api.CameraException
import net.gini.android.capture.internal.camera.api.CameraInterface
import net.gini.android.capture.internal.camera.photo.Photo
import net.gini.android.capture.internal.camera.photo.PhotoFactory
import net.gini.android.capture.internal.camera.view.CameraXPreviewContainer
import net.gini.android.capture.internal.util.ContextHelper
import net.gini.android.capture.internal.util.DeviceHelper
import net.gini.android.capture.internal.util.Size
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.RejectedExecutionException
import kotlin.math.roundToInt

private val LOG: Logger = LoggerFactory.getLogger(CameraXController::class.java)

// Auto focus is 1/6 of the area.
private const val AF_SIZE = 1.0f / 6.0f
private const val AE_SIZE = AF_SIZE * 1.5f

private const val SHUTTER_VIBRATION_DURATION_MS = 50L
private const val FOCUS_VIBRATION_DURATION_MS = 10L

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
internal class CameraXController(val activity: Activity) : CameraInterface {

    private val cameraLifecycle: CameraLifecycle = CameraLifecycle()
    private var camera: androidx.camera.core.Camera? = null

    private var previewUseCase: Preview? = null
    private val previewContainer: CameraXPreviewContainer = CameraXPreviewContainer(activity)

    private var imageCaptureUseCase: ImageCapture? = null
    private var imageAnalysisUseCase: ImageAnalysis? = null
    private var imageAnalyzer: ImageAnalysis.Analyzer? = null

    private var mediaActionSound: MediaActionSound? = null

    override fun open(): CompletableFuture<Void> {
        val openFuture = CompletableFuture<Void>()

        val cameraProviderFuture = try {
            ProcessCameraProvider.getInstance(activity)
        } catch (e: IllegalStateException) {
            LOG.error("Failed to get ProcessCameraProvider instance future", e)
            openFuture.completeExceptionally(CameraException(e, CameraException.Type.OPEN_FAILED))
            return openFuture
        }

        try {
            cameraProviderFuture.addListener({
                val cameraProvider: ProcessCameraProvider = try {
                    cameraProviderFuture.get()
                } catch (e: Exception) {
                    LOG.error("Failed to get ProcessCameraProvider instance", e)
                    openFuture.completeExceptionally(
                        CameraException(e, CameraException.Type.OPEN_FAILED)
                    )
                    return@addListener
                }

                val targetRotation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    activity.display?.rotation ?: Surface.ROTATION_0
                } else {
                    activity.windowManager.defaultDisplay.rotation
                }
                val isPortrait = ContextHelper.isPortraitOrientation(activity)
                
                // We require an image between 8MP and 12MP with at least 4:3 ratio
                // so using 3648x2736 fits that nicely (see CameraResolutionRequirement)
                val targetResolution = android.util.Size(3648, 2736)
                    .forOrientation(isPortrait)

                LOG.debug(
                    "Opening camera for target rotation {} and target resolution {}",
                    targetRotation, targetResolution
                )

                val preview = Preview.Builder()
                    .setTargetResolution(targetResolution)
                    .setTargetRotation(targetRotation)
                    .build()
                previewUseCase = preview

                preview.setSurfaceProvider { request ->
                    LOG.debug("Using preview resolution {}", request.resolution)
                    previewContainer.setPreviewSize(
                        android.util.Size(
                            request.resolution.width,
                            request.resolution.height
                        ).forOrientation(isPortrait)
                    )
                    previewContainer.previewView.surfaceProvider.onSurfaceRequested(request)
                }

                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .setTargetResolution(targetResolution)
                    .setTargetRotation(targetRotation)
                    .build()
                imageCaptureUseCase = imageCapture

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setTargetResolution(targetResolution)
                    .setTargetRotation(targetRotation)
                    .build()
                imageAnalysisUseCase = imageAnalysis

                imageAnalyzer?.let {
                    imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(activity), it)
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()

                    camera = cameraProvider.bindToLifecycle(
                        cameraLifecycle,
                        cameraSelector,
                        preview, imageCapture, imageAnalysis
                    )

                    // Not playing sounds because on some devices (for eg. Samsung Galaxy S9) it
                    // always plays at 100% volume
                    // mediaActionSound = MediaActionSound()

                    LOG.info("Camera is open")

                    openFuture.complete(null)
                } catch (e: Exception) {
                    LOG.error("Use cases binding failed", e)
                    openFuture.completeExceptionally(
                        CameraException(
                            e,
                            CameraException.Type.NO_PREVIEW
                        )
                    )
                }
            }, ContextCompat.getMainExecutor(activity))
        } catch (e: RejectedExecutionException) {
            LOG.error("Failed to add ProcessCameraProvider listener", e)
            openFuture.completeExceptionally(CameraException(e, CameraException.Type.OPEN_FAILED))
            return openFuture
        }

        cameraLifecycle.start()

        return openFuture
    }

    override fun close() {
        cameraLifecycle.stop()
        previewUseCase = null
        imageCaptureUseCase = null
        imageAnalysisUseCase = null
        camera = null
        mediaActionSound?.release()
        mediaActionSound = null
    }

    override fun startPreview(): CompletableFuture<Void> {
        // Not needed for CameraX
        return CompletableFuture.completedFuture(null)
    }

    override fun stopPreview() {
        // Not needed for CameraX
    }

    override fun isPreviewRunning(): Boolean {
        return camera != null
    }

    override fun enableTapToFocus(listener: CameraInterface.TapToFocusListener?) {
        LOG.info("Tap to focus enabled")
        previewContainer.previewView.setOnTouchListener { _, event ->
            val isSingleTouch = event.pointerCount == 1
            val isUpEvent = event.action == MotionEvent.ACTION_UP
            val notALongPress = (event.eventTime - event.downTime
                    < ViewConfiguration.getLongPressTimeout())
            if (isSingleTouch && isUpEvent && notALongPress) {
                focusAtPoint(event.x, event.y, listener)
            }
            true
        }
    }

    private fun focusAtPoint(
        x: Float,
        y: Float,
        listener: CameraInterface.TapToFocusListener? = null
    ) {
        LOG.debug("Focusing at point ({}, {})", x, y)

        val meteringPointFactory = previewContainer.previewView.meteringPointFactory
        val afPoint = meteringPointFactory.createPoint(x, y, AF_SIZE)
        val aePoint = meteringPointFactory.createPoint(x, y, AE_SIZE)

        val focusMeteringAction =
            FocusMeteringAction.Builder(afPoint, FocusMeteringAction.FLAG_AF)
                .addPoint(aePoint, FocusMeteringAction.FLAG_AE)
                .build()

        listener?.onFocusing(
            Point(x.roundToInt(), y.roundToInt()),
            Size(previewContainer.previewView.width, previewContainer.previewView.height)
        )

        val focusFuture = camera?.cameraControl?.startFocusAndMetering(focusMeteringAction)

        focusFuture?.addListener({
            vibrate(FOCUS_VIBRATION_DURATION_MS)
            try {
                val result = focusFuture.get()
                LOG.debug("Focus result: {}", result.isFocusSuccessful)
                listener?.onFocused(result.isFocusSuccessful)
            } catch (e: Exception) {
                LOG.warn("Focus failed", e)
                listener?.onFocused(false)
            }
        }, ContextCompat.getMainExecutor(activity))
    }

    override fun disableTapToFocus() {
        LOG.info("Tap to focus disabled")
        previewContainer.previewView.setOnTouchListener(null)
    }

    override fun focus(): CompletableFuture<Boolean> {
        LOG.info("Focus at preview center")
        val focusFuture = CompletableFuture<Boolean>()

        focusAtPoint(
            previewContainer.previewView.width / 2f,
            previewContainer.previewView.height / 2f,
            object : CameraInterface.TapToFocusListener {
                override fun onFocusing(point: Point, previewViewSize: Size) {}

                override fun onFocused(success: Boolean) {
                    focusFuture.complete(success)
                }
            })

        return focusFuture
    }

    override fun takePicture(): CompletableFuture<Photo> {
        LOG.info("Take picture")

        val pictureFuture = CompletableFuture<Photo>()

        if (camera == null) {
            LOG.error("Cannot take picture: camera not open")
            pictureFuture.completeExceptionally(
                CameraException(
                    "Cannot take picture: camera not open",
                    CameraException.Type.SHOT_FAILED
                )
            )
            return pictureFuture
        }


        if (imageCaptureUseCase == null) {
            LOG.error("Cannot take picture: no image capture use case")
            pictureFuture.completeExceptionally(
                CameraException(
                    "Cannot take picture: no image capture use case",
                    CameraException.Type.SHOT_FAILED
                )
            )
            return pictureFuture
        }

        imageCaptureUseCase?.takePicture(ContextCompat.getMainExecutor(activity),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    // Not playing shutter sound because on some devices (for eg. Samsung Galaxy S9) it
                    // always plays at 100% volume
                    // mediaActionSound?.play(MediaActionSound.SHUTTER_CLICK)

                    vibrate(SHUTTER_VIBRATION_DURATION_MS)

                    val byteArray = try {
                        image.toCroppedByteArray()
                    } catch (e: CameraException) {
                        LOG.error("Failed to take picture", e)
                        pictureFuture.completeExceptionally(e)
                        image.close()
                        return
                    }

                    val photo = PhotoFactory.newPhotoFromJpeg(
                        byteArray,
                        image.imageInfo.rotationDegrees,
                        DeviceHelper.getDeviceOrientation(activity),
                        DeviceHelper.getDeviceType(activity),
                        Document.Source.newCameraSource()
                    )

                    LOG.info("Picture taken with resolution {}x{}", image.cropRect.width(),
                        image.cropRect.height())

                    pictureFuture.complete(photo)
                    image.close()
                }

                override fun onError(exception: ImageCaptureException) {
                    LOG.error("Failed to take picture", exception)
                    pictureFuture.completeExceptionally(
                        CameraException(
                            exception,
                            CameraException.Type.SHOT_FAILED
                        )
                    )
                }
            })

        return pictureFuture
    }

    override fun setPreviewCallback(previewCallback: CameraInterface.PreviewCallback?) {
        if (previewCallback == null) {
            imageAnalyzer = null
            imageAnalysisUseCase?.clearAnalyzer()
            return
        }

        imageAnalyzer = ImageAnalysis.Analyzer { imageProxy ->
            previewCallback.onPreviewFrame(
                imageProxy.toByteArray(),
                Size(imageProxy.width, imageProxy.height),
                imageProxy.imageInfo.rotationDegrees
            )
            imageProxy.close()
        }.also {
            imageAnalysisUseCase?.setAnalyzer(ContextCompat.getMainExecutor(activity), it)
        }
    }

    override fun getPreviewView(context: Context): View = previewContainer

    override fun isFlashAvailable(): Boolean {
        return camera?.cameraInfo?.hasFlashUnit() ?: false
    }

    override fun isFlashEnabled(): Boolean {
        return imageCaptureUseCase?.flashMode == ImageCapture.FLASH_MODE_ON
    }

    override fun setFlashEnabled(enabled: Boolean) {
        imageCaptureUseCase?.flashMode =
            if (enabled) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
    }

    private fun vibrate(duration: Long) {
        with((getSystemService(activity, Vibrator::class.java) as Vibrator)) {
            if (!hasVibrator()) {
                return
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                vibrate(duration);
            }
        }
    }
}

internal class CameraLifecycle : LifecycleOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)

    override fun getLifecycle(): Lifecycle = lifecycleRegistry

    fun start() {
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }

    fun stop() {
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }
}