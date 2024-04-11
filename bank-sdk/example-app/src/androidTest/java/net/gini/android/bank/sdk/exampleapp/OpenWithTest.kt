package net.gini.android.bank.sdk.exampleapp

import android.app.Activity
import android.content.Intent
import androidx.core.net.toFile
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.gini.android.bank.sdk.exampleapp.test.getAssetFileStorageUri
import net.gini.android.bank.sdk.exampleapp.ui.CaptureFlowHostActivity
import net.gini.android.bank.sdk.exampleapp.ui.MainActivity
import net.gini.android.bank.sdk.exampleapp.ui.SplashActivity
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for the "open with" (file import) feature.
 */
@RunWith(AndroidJUnit4::class)
class OpenWithTest {

    @Test
    fun opening_pdf_with_SplashActivity_launches_Bank_SDK() {
        launchActivityForOpenWith<SplashActivity>(listOf("test_pdf.pdf"), "application/pdf")
    }

    @Test
    fun opening_pdf_with_MainActivity_launches_Bank_SDK() {
        launchActivityForOpenWith<MainActivity>(listOf("test_pdf.pdf"), "application/pdf")
    }

    @Test
    fun opening_pdf_with_CaptureFlowHostActivity_launches_Bank_SDK() {
        launchActivityForOpenWith<CaptureFlowHostActivity>(listOf("test_pdf.pdf"), "application/pdf")
    }

    @Test
    fun opening_image_with_SplashActivity_launches_Bank_SDK() {
        launchActivityForOpenWith<SplashActivity>(listOf("test_image.jpeg"), "image/jpeg")
    }

    @Test
    fun opening_image_with_MainActivity_launches_Bank_SDK() {
        launchActivityForOpenWith<MainActivity>(listOf("test_image.jpeg"), "image/jpeg")
    }

    @Test
    fun opening_image_with_CaptureFlowHostActivity_launches_Bank_SDK() {
        launchActivityForOpenWith<CaptureFlowHostActivity>(listOf("test_image.jpeg"), "image/jpeg")
    }

    @Test
    fun opening_images_with_SplashActivity_launches_Bank_SDK() {
        launchActivityForOpenWith<SplashActivity>(listOf("test_image.jpeg", "test_image_2.jpeg"), "image/jpeg")
    }

    @Test
    fun opening_images_with_MainActivity_launches_Bank_SDK() {
        launchActivityForOpenWith<MainActivity>(listOf("test_image.jpeg", "test_image_2.jpeg"), "image/jpeg")
    }

    @Test
    fun opening_images_with_CaptureFlowHostActivity_launches_Bank_SDK() {
        launchActivityForOpenWith<CaptureFlowHostActivity>(listOf("test_image.jpeg", "test_image_2.jpeg"), "image/jpeg")
    }

    private inline fun <reified T: Activity> launchActivityForOpenWith(assetFilePaths: List<String>, assetFileMimeType: String) {
        // Given
        val storageUris = assetFilePaths.map { getAssetFileStorageUri(it) }

        val intent = Intent(ApplicationProvider.getApplicationContext(), T::class.java).apply {
            action = Intent.ACTION_VIEW
            if (storageUris.size == 1) {
                setDataAndType(storageUris[0], assetFileMimeType)
            } else {
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(storageUris))
            }
        }

        // When
        ActivityScenario.launch<T>(intent).use {
            // Simulate PermissionDenied scenario by deleting the file before the Bank SDK is launched (revoke permission)
            Thread.sleep(200)
            storageUris.forEach { it.toFile().delete() }

            // Give time for the Bank SDK to launch
            Thread.sleep(1000)

            // Then
            onView(withId(net.gini.android.capture.R.id.gc_analysis_message)).check(matches(isDisplayed()))
        }
    }
}