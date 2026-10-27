package net.gini.android.bank.sdk.exampleapp.ui.testcases

import android.app.Activity
import android.content.Intent
import androidx.core.net.toFile
import androidx.lifecycle.Lifecycle
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
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @get:Rule(order = Int.MIN_VALUE)
    val retryRule = RetryRule()

    private var idlingResourceForOpenWith: IdlingResource? = null

    @Before
    fun setup() {
        // Multi-image open-with uploads several files over the network; on slower remote
        // (BrowserStack) devices this can exceed Espresso's default 26s idle timeout and
        // throw IdlingResourceTimeoutException. Give the idle wait more headroom.
        IdlingPolicies.setMasterPolicyTimeout(90, TimeUnit.SECONDS)
        IdlingPolicies.setIdlingResourceTimeout(90, TimeUnit.SECONDS)
        resetIdlingResourceForOpenWith()
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
    fun opening_heic_with_SplashActivity_launches_Bank_SDK() {
        launchActivityForOpenWith<SplashActivity>(listOf("invoice.heic"), "image/heic")
    }

    @Test
    fun opening_heic_with_MainActivity_launches_Bank_SDK() {
        launchActivityForOpenWith<MainActivity>(listOf("invoice.heic"), "image/heic")
    }

    @Test
    fun opening_heic_with_CaptureFlowHostActivity_launches_Bank_SDK() {
        launchActivityForOpenWith<CaptureFlowHostActivity>(listOf("invoice.heic"), "image/heic")
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

    /**
     * PP-2910 (adapted) — a file type the SDK does not accept never starts the capture flow.
     *
     * The Xray case expects an error dialog, but Android has none on this path: the SDK's Files
     * picker only offers PDF/XML, so an unsupported type can only arrive through "open with". There
     * the SDK rejects it and the example app shows a Toast and finishes `MainActivity` — both with
     * the default Intent-based API (`onCaptureResult`) and the Uri-based one (`abortOpenWith`).
     * The Toast is not read: Toasts are unreliable to find on API 30+.
     *
     * No Espresso `onView` here. On the Intent-based path nothing decrements the open-with idling
     * resource after an error, so Espresso would wait for it until its timeout.
     */
    @Test
    fun opening_unsupported_file_with_MainActivity_does_not_start_Bank_SDK() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            setDataAndType(getAssetFileStorageUri("unsupported.txt"), "text/plain")
        }

        val scenario = ActivityScenario.launch<MainActivity>(intent)

        val deadline = System.currentTimeMillis() + REJECTED_FILE_TIMEOUT_MS
        while (scenario.state != Lifecycle.State.DESTROYED && System.currentTimeMillis() < deadline) {
            Thread.sleep(POLL_INTERVAL_MS)
        }
        assertEquals(
            "MainActivity should finish after the SDK rejects a text/plain file",
            Lifecycle.State.DESTROYED,
            scenario.state
        )
        val extractions = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            .findObject(UiSelector().resourceId("net.gini.android.bank.sdk.exampleapp:id/recyclerview_extractions"))
        assertFalse("The extractions screen must not open for a rejected file", extractions.exists())
    }

    /**
     * Drains the "OpenWith" counting idling resource so this attempt starts from an idle state.
     *
     * The resource lives on the [ExampleApp] instance, so its count outlives a single attempt: the
     * entry activity increments it on every launch, while `ExtractionsActivity` decrements it only
     * once — when the extractions screen finally appears. An attempt that times out therefore
     * leaves the count at 1, and because [RetryRule] re-launches the activity (incrementing again)
     * without the count ever returning to zero, every retry would be guaranteed to time out too.
     * Draining here is what lets a retry recover from a transient timeout.
     */
    private fun resetIdlingResourceForOpenWith() {
        val app = ApplicationProvider.getApplicationContext<ExampleApp>()
        var remainingDecrements = MAX_IDLING_DRAIN_DECREMENTS
        while (!app.idlingResourceForOpenWith.isIdleNow && remainingDecrements-- > 0) {
            app.decrementIdlingResourceForOpenWith()
        }
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

    private companion object {
        /**
         * A leaked attempt can only add one count per activity launch, so a small bound is ample.
         * It exists purely so a broken increment/decrement pairing can never spin here forever.
         */
        const val MAX_IDLING_DRAIN_DECREMENTS = 10

        /** How long the rejected-file test waits for `MainActivity` to finish. */
        const val REJECTED_FILE_TIMEOUT_MS = 10_000L

        const val POLL_INTERVAL_MS = 250L
    }
}