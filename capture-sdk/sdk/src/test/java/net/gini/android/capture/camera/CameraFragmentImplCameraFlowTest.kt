package net.gini.android.capture.camera

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.media.Image
import android.provider.Settings
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.widget.Toolbar
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.testing.FragmentScenario
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import jersey.repackaged.jsr166e.CompletableFuture
import net.gini.android.capture.Document
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.GiniCaptureError
import net.gini.android.capture.GiniCaptureHelper
import net.gini.android.capture.R
import net.gini.android.capture.di.CaptureSdkIsolatedKoinContext
import net.gini.android.capture.document.ImageDocument
import net.gini.android.capture.document.ImageDocumentFake
import net.gini.android.capture.document.ImageMultiPageDocument
import net.gini.android.capture.internal.camera.api.CameraInterface
import net.gini.android.capture.internal.camera.photo.Photo
import net.gini.android.capture.internal.camera.photo.PhotoEdit
import net.gini.android.capture.internal.fileimport.FileChooserResult
import net.gini.android.capture.internal.provider.GiniBankConfigurationProvider
import net.gini.android.capture.internal.util.CancelListener
import net.gini.android.capture.internal.util.Size
import net.gini.android.capture.tracking.useranalytics.UserAnalytics
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEvent
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEventTracker
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.module
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog
import org.robolectric.shadows.ShadowLooper

/**
 * Runs the real [CameraFragment] under Robolectric with a camera controller double, so that the
 * camera driven paths of [CameraFragmentImpl] (taking pictures, tap to focus, preview frames, the
 * camera buttons and the top bar) can be exercised without camera hardware.
 */
@RunWith(AndroidJUnit4::class)
class CameraFragmentImplCameraFlowTest {

    private val koinTestModule = module {
        single { GiniBankConfigurationProvider() }
    }

    private lateinit var cameraController: CameraInterface
    private lateinit var navController: NavController
    private lateinit var fragmentListener: CameraFragmentListener
    private lateinit var analyticsTracker: UserAnalyticsEventTracker

    private val application: Application
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        CaptureSdkIsolatedKoinContext.koin.loadModules(listOf(koinTestModule))
        shadowOf(ApplicationProvider.getApplicationContext<Application>())
            .grantPermissions(Manifest.permission.CAMERA)
        cameraController = mock {
            on { open() } doReturn CompletableFuture.completedFuture<Void>(null)
            on { startPreview() } doReturn CompletableFuture.completedFuture<Void>(null)
            on { isPreviewRunning } doReturn true
            on { isFlashAvailable } doReturn true
            on { getPreviewView(any()) } doAnswer { View(it.getArgument<Context>(0)) }
        }
        TestCameraFragment.cameraController = cameraController
        navController = mock()
        fragmentListener = mock()
        analyticsTracker = mock {
            on { trackEvent(any()) } doReturn true
            on { trackEvent(any(), any<Set<Nothing>>()) } doReturn true
        }
    }

    @After
    fun tearDown() {
        GiniCaptureHelper.setGiniCaptureInstance(null)
        UserAnalytics.cleanup()
        CaptureSdkIsolatedKoinContext.koin.unloadModules(listOf(koinTestModule))
    }

    // region Taking pictures

    @Test
    fun `stores the picture and opens the review screen when multi-page is disabled`() {
        GiniCapture.Builder().setMultiPageEnabled(false).build()
        val photo = newPhoto()
        whenever(cameraController.takePicture()).thenReturn(CompletableFuture.completedFuture(photo))

        launchCameraFragment { fragment ->
            takePicture(fragment)

            assertThat(storedMultiPageDocument()!!.documents).hasSize(1)
            verify(navController).navigate(CameraFragmentDirections.toReviewFragment(false))
            verify(cameraController, atLeastOnce()).startPreview()
        }
    }

    @Test
    fun `adds every picture as a page and opens the review screen when multi-page is enabled`() {
        GiniCapture.Builder().setMultiPageEnabled(true).build()
        val firstPhoto = newPhoto()
        val secondPhoto = newPhoto()
        whenever(cameraController.takePicture())
            .thenReturn(CompletableFuture.completedFuture(firstPhoto))
            .thenReturn(CompletableFuture.completedFuture(secondPhoto))

        launchCameraFragment { fragment ->
            takePicture(fragment)
            assertThat(storedMultiPageDocument()!!.documents).hasSize(1)

            takePicture(fragment)
            assertThat(storedMultiPageDocument()!!.documents).hasSize(2)

            verify(navController, atLeastOnce())
                .navigate(CameraFragmentDirections.toReviewFragment(true))
        }
    }

    @Test
    fun `reports an error and restarts the preview when taking the picture failed`() {
        GiniCapture.Builder().build()
        val failedPicture = CompletableFuture<Photo>().apply {
            completeExceptionally(IllegalStateException("camera broke"))
        }
        whenever(cameraController.takePicture()).thenReturn(failedPicture)

        launchCameraFragment { fragment ->
            takePicture(fragment)

            assertCameraShotFailedWasReported()
            verify(navController, never()).navigate(any<NavDirections>())
        }
    }

    @Test
    fun `reports an error and restarts the preview when the camera returned no picture`() {
        GiniCapture.Builder().build()
        whenever(cameraController.takePicture()).thenReturn(CompletableFuture.completedFuture<Photo>(null))

        launchCameraFragment { fragment ->
            takePicture(fragment)

            assertCameraShotFailedWasReported()
            verify(navController, never()).navigate(any<NavDirections>())
        }
    }

    @Test
    fun `reports an error and restarts the preview when the picture could not be compressed`() {
        GiniCapture.Builder().build()
        val photo = newPhoto(editSucceeds = false)
        whenever(cameraController.takePicture()).thenReturn(CompletableFuture.completedFuture(photo))

        launchCameraFragment { fragment ->
            takePicture(fragment)

            assertCameraShotFailedWasReported()
            verify(navController, never()).navigate(any<NavDirections>())
        }
    }

    @Test
    fun `reports an error when the picture cannot be saved because GiniCapture was cleaned up`() {
        GiniCapture.Builder().build()
        val photo = newPhoto()
        whenever(cameraController.takePicture()).thenReturn(CompletableFuture.completedFuture(photo))

        launchCameraFragment { fragment ->
            GiniCaptureHelper.setGiniCaptureInstance(null)

            takePicture(fragment)

            assertCameraShotFailedWasReported()
            verify(navController, never()).navigate(CameraFragmentDirections.toReviewFragment(false))
        }
    }

    @Test
    fun `adds the picture to the pages that were already taken`() {
        GiniCapture.Builder().setMultiPageEnabled(true).build()
        val existingPages = ImageMultiPageDocument(Document.Source.newCameraSource(), Document.ImportMethod.NONE)
        existingPages.addDocument(ImageDocumentFake())
        GiniCapture.getInstance().internal().imageMultiPageDocumentMemoryStore.setMultiPageDocument(existingPages)
        val photo = newPhoto()
        whenever(cameraController.takePicture()).thenReturn(CompletableFuture.completedFuture(photo))

        launchCameraFragment { fragment ->
            takePicture(fragment)

            assertThat(existingPages.documents).hasSize(2)
            assertThat(storedMultiPageDocument()).isSameInstanceAs(existingPages)
        }
    }

    // endregion

    // region Camera buttons

    @Test
    fun `tracks the capture tap and takes a picture when the trigger button is clicked`() {
        GiniCapture.Builder().build()
        val photo = newPhoto()
        whenever(cameraController.takePicture()).thenReturn(CompletableFuture.completedFuture(photo))

        launchCameraFragment { fragment ->
            fragment.impl.mUserAnalyticsEventTracker = analyticsTracker

            fragment.requireView().findViewById<View>(R.id.gc_button_camera_trigger).performClick()
            ShadowLooper.idleMainLooper()

            verify(analyticsTracker).trackEvent(eq(UserAnalyticsEvent.CAPTURE_TAPPED), any<Set<Nothing>>())
            verify(cameraController).takePicture()
        }
    }

    @Test
    fun `tracks the flash tap and turns the flash on when the flash button is clicked`() {
        GiniCapture.Builder().build()

        launchCameraFragment { fragment ->
            fragment.impl.mUserAnalyticsEventTracker = analyticsTracker

            fragment.requireView().findViewById<View>(R.id.gc_button_flash).performClick()

            verify(analyticsTracker).trackEvent(eq(UserAnalyticsEvent.FLASH_TAPPED), any<Set<Nothing>>())
            verify(cameraController).setFlashEnabled(true)
        }
    }

    @Test
    fun `tracks the import tap when the import button is clicked`() {
        GiniCapture.Builder().build()

        launchCameraFragment { fragment ->
            fragment.impl.mUserAnalyticsEventTracker = analyticsTracker

            fragment.requireView().findViewById<View>(R.id.gc_button_import).performClick()

            verify(analyticsTracker)
                .trackEvent(eq(UserAnalyticsEvent.IMPORT_FILES_TAPPED), any<Set<Nothing>>())
        }
    }

    @Test
    fun `tracks the thumbnail tap and goes back when the photo thumbnail is clicked`() {
        GiniCapture.Builder().setMultiPageEnabled(true).build()
        val photo = newPhoto()
        whenever(cameraController.takePicture()).thenReturn(CompletableFuture.completedFuture(photo))

        launchCameraFragment { fragment ->
            takePicture(fragment)
            fragment.impl.mUserAnalyticsEventTracker = analyticsTracker

            // PhotoThumbnail forwards its click listener to the thumbnail image
            fragment.requireView()
                .findViewById<View>(R.id.gc_photo_thumbnail)
                .findViewById<View>(R.id.gc_thumbnail)
                .performClick()

            verify(analyticsTracker)
                .trackEvent(eq(UserAnalyticsEvent.MULTIPLE_PAGES_CAPTURED_TAPPED), any<Set<Nothing>>())
            verify(navController).popBackStack()
        }
    }

    @Test
    fun `hides the detection error popup when its dismiss button is clicked`() {
        GiniCapture.Builder().build()

        launchCameraFragment { fragment ->
            val detectionErrorLayout =
                fragment.requireView().findViewById<View>(R.id.gc_detection_error_layout)
            detectionErrorLayout.visibility = View.VISIBLE

            detectionErrorLayout
                .findViewById<View>(R.id.gc_detection_error_popup_dismiss_button)
                .performClick()

            assertThat(detectionErrorLayout.visibility).isEqualTo(View.GONE)
        }
    }

    // endregion

    // region Tap to focus and preview frames

    @Test
    fun `centers the focus indicator on the tapped point`() {
        GiniCapture.Builder().build()

        launchCameraFragment { fragment ->
            val tapToFocusListener = argumentCaptor<CameraInterface.TapToFocusListener>().run {
                verify(cameraController).enableTapToFocus(capture())
                firstValue
            }
            val view = fragment.requireView()
            val previewContainer = view.findViewById<View>(R.id.gc_camera_preview_container)
            val focusIndicator = view.findViewById<View>(R.id.gc_camera_focus_indicator)
            val previewSize = Size(previewContainer.width + PREVIEW_OVERFLOW, previewContainer.height)

            tapToFocusListener.onFocusing(Point(TAP_X, TAP_Y), previewSize)
            tapToFocusListener.onFocused(true)

            // The preview is wider than its container, so half of the overflow is cut off on the left
            assertThat(focusIndicator.x)
                .isEqualTo(TAP_X - PREVIEW_OVERFLOW / 2f - focusIndicator.width / 2f)
            assertThat(focusIndicator.y).isEqualTo(TAP_Y - focusIndicator.height / 2f)
        }
    }

    @Test
    fun `does not release a preview frame while no frame consumer is set up`() {
        GiniCapture.Builder().build()

        launchCameraFragment {
            val previewCallback = argumentCaptor<CameraInterface.PreviewCallback>().run {
                verify(cameraController).setPreviewCallback(capture())
                firstValue
            }
            val frameCallback = mock<CameraInterface.PreviewFrameCallback>()

            previewCallback.onPreviewFrame(mock<Image>(), Size(FRAME_SIZE, FRAME_SIZE), 0, frameCallback)
            previewCallback.onPreviewFrame(ByteArray(FRAME_SIZE), Size(FRAME_SIZE, FRAME_SIZE), 0)

            verify(frameCallback, never()).onReleaseFrame()
        }
    }

    // endregion

    // region Top bar

    @Test
    fun `shows the camera title on phones in portrait`() {
        GiniCapture.Builder().build()

        launchCameraFragment { fragment ->
            assertThat(topBarTitle(fragment)).isEqualTo(fragment.getString(R.string.gc_title_camera))
        }
    }

    @Test
    @Config(qualifiers = "land")
    fun `shows the invoice only title on phones in landscape without QR code scanning`() {
        GiniCapture.Builder().build()

        launchCameraFragment { fragment ->
            assertThat(topBarTitle(fragment))
                .isEqualTo(fragment.getString(R.string.gc_camera_info_label_only_invoice))
        }
    }

    @Test
    @Config(qualifiers = "sw600dp")
    fun `shows the invoice only title on tablets without QR code scanning`() {
        GiniCapture.Builder().build()

        launchCameraFragment { fragment ->
            assertThat(topBarTitle(fragment))
                .isEqualTo(fragment.getString(R.string.gc_camera_info_label_only_invoice))
        }
    }

    @Test
    @Config(qualifiers = "land")
    fun `shows the landscape title on phones in landscape with QR code scanning`() {
        GiniCapture.Builder().setQRCodeScanningEnabled(true).build()

        launchCameraFragment { fragment ->
            assertThat(topBarTitle(fragment))
                .isEqualTo(fragment.getString(R.string.gc_camera_top_bar_title_landscape))
        }
    }

    @Test
    @Config(qualifiers = "sw600dp")
    fun `shows the invoice and QR code title on tablets with QR code scanning`() {
        GiniCapture.Builder().setQRCodeScanningEnabled(true).build()

        launchCameraFragment { fragment ->
            assertThat(topBarTitle(fragment))
                .isEqualTo(fragment.getString(R.string.gc_camera_info_label_invoice_and_qr))
        }
    }

    @Test
    fun `goes back when the top bar navigation button is clicked`() {
        GiniCapture.Builder().build()
        UserAnalytics.initialize(application)

        launchCameraFragment { fragment ->
            val toolbar = fragment.requireView().findViewById<Toolbar>(R.id.gc_navigation_bar)

            toolbar.children.filterIsInstance<ImageButton>().first().performClick()

            verify(navController).popBackStack()
        }
    }

    // endregion

    // region Missing camera permission

    @Test
    fun `opens the app settings when access is requested on the missing permission view`() {
        GiniCapture.Builder().build()
        UserAnalytics.initialize(application)
        shadowOf(application).denyPermissions(Manifest.permission.CAMERA)

        launchCameraFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.gc_button_camera_no_permission).performClick()

            assertThat(shadowOf(application).nextStartedActivity.action)
                .isEqualTo(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        }
    }

    @Test
    fun `opens the help screen from the missing permission view`() {
        GiniCapture.Builder().build()
        UserAnalytics.initialize(application)
        shadowOf(application).denyPermissions(Manifest.permission.CAMERA)

        launchCameraFragment { fragment ->
            fragment.impl.startHelpActivity()

            verify(navController).navigate(CameraFragmentDirections.toHelpFragment())
        }
    }

    @Test
    fun `opens the help screen from the camera`() {
        GiniCapture.Builder().build()
        UserAnalytics.initialize(application)

        launchCameraFragment { fragment ->
            fragment.impl.startHelpActivity()

            verify(navController).navigate(CameraFragmentDirections.toHelpFragment())
        }
    }

    // endregion

    // region Top bar in QR code only mode

    @Test
    fun `shows the QR code only title when only QR code scanning is enabled`() {
        GiniCapture.Builder().setQRCodeScanningEnabled(true).setOnlyQRCodeScanning(true).build()

        launchCameraFragment { fragment ->
            assertThat(topBarTitle(fragment))
                .isEqualTo(fragment.getString(R.string.gc_camera_info_label_only_qr))
        }
    }

    // endregion

    // region File import

    @Test
    fun `shows the invalid file dialog when the file chooser failed`() {
        GiniCapture.Builder().build()

        launchCameraFragment { fragment ->
            fragment.impl.mUserAnalyticsEventTracker = analyticsTracker

            fragment.impl.handleFileChooserResult(
                FileChooserResult.Error(
                    GiniCaptureError(GiniCaptureError.ErrorCode.DOCUMENT_IMPORT, "import failed")
                )
            )

            verify(analyticsTracker)
                .trackEvent(eq(UserAnalyticsEvent.ERROR_DIALOG_SHOWN), any<Set<Nothing>>())
            val dialog = ShadowDialog.getLatestDialog()
            assertThat(dialog.isShowing).isTrue()

            dialog.cancel()
            assertThat(dialog.isShowing).isFalse()
        }
    }

    // endregion

    private fun launchCameraFragment(block: (TestCameraFragment) -> Unit) {
        FragmentScenario.launchInContainer(
            fragmentClass = TestCameraFragment::class.java,
            themeResId = R.style.GiniCaptureTheme,
            factory = object : FragmentFactory() {
                override fun instantiate(classLoader: ClassLoader, className: String): Fragment =
                    TestCameraFragment().apply {
                        setListener(fragmentListener)
                        setCancelListener(mock())
                    }.also { fragment ->
                        fragment.viewLifecycleOwnerLiveData.observeForever { viewLifecycleOwner ->
                            if (viewLifecycleOwner != null) {
                                Navigation.setViewNavController(fragment.requireView(), navController)
                            }
                        }
                    }
            }
        ).use { scenario ->
            scenario.moveToState(Lifecycle.State.RESUMED)
            scenario.onFragment(block)
        }
    }

    private fun takePicture(fragment: TestCameraFragment) {
        fragment.impl.onCameraTriggerClicked()
        ShadowLooper.idleMainLooper()
    }

    private fun storedMultiPageDocument() =
        GiniCapture.getInstance().internal().imageMultiPageDocumentMemoryStore.multiPageDocument

    private fun topBarTitle(fragment: Fragment): CharSequence? =
        fragment.requireView().findViewById<Toolbar>(R.id.gc_navigation_bar).title

    private fun assertCameraShotFailedWasReported() {
        val error = argumentCaptor<GiniCaptureError>().run {
            verify(fragmentListener).onError(capture())
            firstValue
        }
        assertThat(error.errorCode).isEqualTo(GiniCaptureError.ErrorCode.CAMERA_SHOT_FAILED)
        verify(cameraController, atLeastOnce()).startPreview()
    }

    /**
     * A photo whose edit finishes immediately, either successfully or with a failure.
     */
    private fun newPhoto(editSucceeds: Boolean = true): Photo {
        val photoEdit = mock<PhotoEdit>()
        val photo = mock<Photo> {
            on { data } doReturn byteArrayOf(1, 2, 3)
            on { imageFormat } doReturn ImageDocument.ImageFormat.JPEG
            on { bitmapPreview } doReturn Bitmap.createBitmap(
                PREVIEW_BITMAP_SIZE, PREVIEW_BITMAP_SIZE, Bitmap.Config.ARGB_8888
            )
            on { edit() } doReturn photoEdit
        }
        whenever(photoEdit.crop(any(), any())).thenReturn(photoEdit)
        whenever(photoEdit.compressByDefault()).thenReturn(photoEdit)
        whenever(photoEdit.applyAsync(any())).thenAnswer {
            val callback = it.getArgument<PhotoEdit.PhotoEditCallback>(0)
            if (editSucceeds) callback.onDone(photo) else callback.onFailed()
        }
        return photo
    }

    internal class TestCameraFragment : CameraFragment() {

        lateinit var impl: CameraFragmentImpl

        override fun createFragmentImpl(
            cancelListener: CancelListener,
            addPages: Boolean
        ): CameraFragmentImpl =
            object : CameraFragmentImpl(this, cancelListener, false) {
                override fun initQRCodeReader() {
                    // No QR code reader is available in JVM tests
                }

                override fun createCameraController(activity: Activity?): CameraInterface =
                    cameraController
            }.also { impl = it }

        companion object {
            lateinit var cameraController: CameraInterface
        }
    }

    private companion object {
        const val TAP_X = 300
        const val TAP_Y = 400
        const val PREVIEW_OVERFLOW = 100
        const val FRAME_SIZE = 16
        const val PREVIEW_BITMAP_SIZE = 10
    }
}
