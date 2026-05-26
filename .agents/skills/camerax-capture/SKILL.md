---
name: camerax-capture
description: Guide for CameraX and document capture patterns in Android. Use this when working with the capture-sdk, implementing camera features, handling image capture/analysis, integrating ML Kit, or debugging camera lifecycle issues.
---

The `capture-sdk` module implements document capture using CameraX (camera2, lifecycle, view). ML Kit is used for barcode scanning and text recognition. This skill covers CameraX patterns relevant to this codebase.

## CameraX use cases in this repo

| Use case | CameraX class | Purpose |
|---|---|---|
| Camera preview | `Preview` | Live viewfinder shown to the user |
| Photo capture | `ImageCapture` | Single still image capture |
| Image analysis | `ImageAnalysis` | Real-time frame analysis (ML Kit barcode/text) |

## Setting up CameraX

```kotlin
private lateinit var cameraProvider: ProcessCameraProvider
private lateinit var imageCapture: ImageCapture
private lateinit var imageAnalysis: ImageAnalysis

private fun startCamera() {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

    cameraProviderFuture.addListener({
        cameraProvider = cameraProviderFuture.get()

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
        }

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { it.setAnalyzer(cameraExecutor, MyAnalyzer()) }

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                viewLifecycleOwner, cameraSelector, preview, imageCapture, imageAnalysis
            )
        } catch (e: Exception) {
            Log.e(TAG, "Camera binding failed", e)
        }
    }, ContextCompat.getMainExecutor(requireContext()))
}
```

## Image capture

```kotlin
fun captureDocument() {
    val outputOptions = ImageCapture.OutputFileOptions
        .Builder(createTempFile())
        .build()

    imageCapture.takePicture(
        outputOptions,
        cameraExecutor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val uri = output.savedUri ?: return
                processDocument(uri)
            }
            override fun onError(exception: ImageCaptureException) {
                handleCaptureError(exception)
            }
        }
    )
}
```

## ML Kit integration — image analysis

```kotlin
class DocumentAnalyzer(
    private val onBarcodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val barcodeScanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE, Barcode.FORMAT_DATA_MATRIX)
            .build()
    )

    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: run { imageProxy.close(); return }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                barcodes.firstOrNull()?.rawValue?.let(onBarcodeDetected)
            }
            .addOnCompleteListener {
                imageProxy.close()  // ALWAYS close — camera will stall otherwise
            }
    }
}
```

**Critical**: Always call `imageProxy.close()` in `addOnCompleteListener` (not `addOnSuccessListener`). Failing to close causes the camera preview to freeze.

## Camera permissions

```kotlin
private val requestPermissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestPermission()
) { isGranted ->
    if (isGranted) startCamera() else showPermissionDeniedUI()
}

private fun checkAndRequestCameraPermission() {
    when {
        ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED -> startCamera()
        shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) ->
            showRationale()
        else -> requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }
}
```

## Camera lifecycle management

- Always bind use cases to `viewLifecycleOwner` (not the Fragment's own lifecycle) to prevent stale bindings
- Call `cameraProvider.unbindAll()` before rebinding (e.g., when switching cameras)
- Use a dedicated `ExecutorService` for analysis:
  ```kotlin
  private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

  override fun onDestroyView() {
      super.onDestroyView()
      cameraExecutor.shutdown()
  }
  ```

## Image rotation handling

CameraX images may arrive rotated. Always use `imageProxy.imageInfo.rotationDegrees` when passing to ML Kit, and apply `ExifInterface` correction when saving to disk:

```kotlin
val rotation = imageProxy.imageInfo.rotationDegrees
val inputImage = InputImage.fromMediaImage(mediaImage, rotation)
```

## Capture quality settings

```kotlin
ImageCapture.Builder()
    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)   // slower but sharper
    .setJpegQuality(95)
    .setTargetRotation(binding.viewFinder.display.rotation)       // match display rotation
    .build()
```

## Common CameraX pitfalls

- **Not closing `ImageProxy`** — causes camera stall; always close in `addOnCompleteListener`
- **Binding to `this` (Fragment)** instead of `viewLifecycleOwner` — causes memory leaks
- **Running camera on the main thread** — use `cameraExecutor` for analysis callbacks
- **Not handling `CameraState`** — check for `ERROR` states (e.g., camera in use by another app)
- **Ignoring rotation** — document captures will be upside-down or sideways on landscape devices without rotation handling
