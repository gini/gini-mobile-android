package net.gini.android.bank.sdk.exampleapp.ui.testcases

import android.app.Activity
import android.content.Intent
import androidx.core.net.toFile
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingPolicies
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import net.gini.android.bank.sdk.GiniBank
import net.gini.android.bank.sdk.exampleapp.ExampleApp
import net.gini.android.bank.sdk.exampleapp.ui.resources.RetryRule
import net.gini.android.bank.sdk.exampleapp.R
import net.gini.android.bank.sdk.exampleapp.test.getAssetFileStorageUri
import net.gini.android.bank.sdk.exampleapp.ui.CaptureFlowHostActivity
import net.gini.android.bank.sdk.exampleapp.ui.MainActivity
import net.gini.android.bank.sdk.exampleapp.ui.SplashActivity
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/**
 * Tests for the "open with" (file import) feature.
 */
@RunWith(AndroidJUnit4::class)
class OpenWithTest {

    @get:Rule
    val retryRule = RetryRule()

    private var idlingResourceForOpenWith: IdlingResource? = null

    @Before
    fun setup() {
        // Multi-image open-with uploads several files over the network; on slower remote
        // (BrowserStack) devices this can exceed Espresso's default 26s idle timeout and
        // throw IdlingResourceTimeoutException. Give the idle wait more headroom.
        IdlingPolicies.setMasterPolicyTimeout(90, TimeUnit.SECONDS)
        IdlingPolicies.setIdlingResourceTimeout(90, TimeUnit.SECONDS)
        idlingResourceForOpenWith = ApplicationProvider.getApplicationContext<ExampleApp>().idlingResourceForOpenWith
        IdlingRegistry.getInstance().register(idlingResourceForOpenWith)
    }

    @After
    fun tearDown() {
        IdlingRegistry.getInstance().unregister(idlingResourceForOpenWith)
    }

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
        ActivityScenario.launch<T>(intent)

        // Simulate PermissionDenied scenario by deleting the file before the Bank SDK is launched (revoke permission)
        Thread.sleep(200)
        storageUris.forEach { it.toFile().delete() }
        runBlocking { GiniBank.giniTransactionDocs.transactionDocsSettings.setAlwaysAttachSetting(true) }

        // Then
        onView(withId(R.id.recyclerview_extractions)).check(matches(isDisplayed()))
        }

}