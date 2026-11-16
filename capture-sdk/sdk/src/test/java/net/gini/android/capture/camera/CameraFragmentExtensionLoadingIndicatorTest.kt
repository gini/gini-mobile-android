package net.gini.android.capture.camera

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import net.gini.android.capture.di.getGiniCaptureKoin
import net.gini.android.capture.internal.provider.GiniBankConfigurationProvider
import net.gini.android.capture.view.CustomLoadingIndicatorAdapter
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.module.Module
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner

/**
 * The decision the camera's loading indicator makes every time it is shown: the Gini brand mark
 * only while an invoice is retrieved for a scanned QR code *and* the client configuration lists
 * the Analysis screen; the integrator's indicator for every other busy state.
 *
 * Needs Robolectric because the decision is taken inside the indicator's `onVisible()`, which
 * works on real views. The rest of [CameraFragmentExtension] is covered by
 * [CameraFragmentExtensionTest] on the plain JVM.
 */
@RunWith(RobolectricTestRunner::class)
class CameraFragmentExtensionLoadingIndicatorTest {

    private class TestExtension : CameraFragmentExtension() {
        override fun hideImageCorners() = Unit
        override fun blockInteractionForQrCodeStep() = Unit
        override fun setPoweredByGiniVisible(visible: Boolean) = Unit
        override fun isOnlyQRCodeScanningEnabled() = false
    }

    /** The integrator's indicator; exposes the view it created so a test can tell it apart. */
    private class IntegratorIndicator : CustomLoadingIndicatorAdapter {
        var view: View? = null
            private set

        override fun onCreateView(container: ViewGroup): View =
            View(container.context).also { view = it }

        override fun onVisible() = Unit
        override fun onHidden() = Unit
        override fun onDestroy() = Unit
    }

    private lateinit var context: Context
    private lateinit var configurationProvider: GiniBankConfigurationProvider
    private lateinit var koinTestModule: Module
    private lateinit var extension: TestExtension

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // Fresh per test: the provider is a singleton in the SDK's isolated Koin context, which
        // every test in this JVM shares.
        configurationProvider = GiniBankConfigurationProvider()
        koinTestModule = module { single { configurationProvider } }
        getGiniCaptureKoin().loadModules(listOf(koinTestModule))
        extension = TestExtension()
    }

    @After
    fun tearDown() {
        getGiniCaptureKoin().unloadModules(listOf(koinTestModule))
        // unloadModules drops the overriding definition without restoring the production one.
        getGiniCaptureKoin().loadModules(
            listOf(module { single { GiniBankConfigurationProvider() } })
        )
    }

    @Test
    fun `shows the Gini mark while the QR invoice is retrieved and the screen is branded`() {
        configurationProvider.update { it.copy(ingredientBrandScreens = setOf("Analysis")) }
        extension.setQrInvoiceRetrievalRunning(true)

        val host = showIndicator(IntegratorIndicator())

        assertThat(host.childCount).isEqualTo(1)
        assertThat(host.getChildAt(0)).isInstanceOf(ImageView::class.java)
    }

    @Test
    fun `shows the integrator indicator while the QR invoice is retrieved but the screen is not branded`() {
        extension.setQrInvoiceRetrievalRunning(true)
        val integrator = IntegratorIndicator()

        val host = showIndicator(integrator)

        assertThat(host.childCount).isEqualTo(1)
        assertThat(host.getChildAt(0)).isSameInstanceAs(integrator.view)
    }

    @Test
    fun `shows the integrator indicator for the other busy states even when the screen is branded`() {
        configurationProvider.update { it.copy(ingredientBrandScreens = setOf("Analysis")) }
        extension.setQrInvoiceRetrievalRunning(false)
        val integrator = IntegratorIndicator()

        val host = showIndicator(integrator)

        assertThat(host.childCount).isEqualTo(1)
        assertThat(host.getChildAt(0)).isSameInstanceAs(integrator.view)
    }

    /** Creates the camera's indicator view and shows it, the way InjectedViewContainer does. */
    private fun showIndicator(integrator: IntegratorIndicator): FrameLayout {
        val adapter = extension.loadingIndicatorAdapterInstance(integrator).viewAdapter
        val host = adapter.onCreateView(FrameLayout(context)) as FrameLayout
        adapter.onVisible()
        return host
    }
}
