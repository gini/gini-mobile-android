package net.gini.android.capture

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import net.gini.android.capture.internal.ui.runtimecolors.GiniColorResolver
import net.gini.android.capture.ui.theme.colors.CustomResourceProvider
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for the [CustomResourceProvider] round-trip through [GiniCapture.Builder] and the
 * per-instance [GiniColorResolver].
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class GiniCaptureCustomResourceProviderTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        GiniCaptureHelper.setGiniCaptureInstance(null)
    }

    @Test
    fun `no custom resource provider by default`() {
        GiniCapture.newInstance(context)
            .setGiniCaptureNetworkService(mockk(relaxed = true))
            .build()

        assertThat(GiniCapture.getInstance().customResourceProvider).isNull()
        assertThat(GiniColorResolver.current()?.isActive).isFalse()
    }

    @Test
    fun `custom resource provider is set through the builder`() {
        val provider = CustomResourceProvider { _, _ -> null }

        GiniCapture.newInstance(context)
            .setGiniCaptureNetworkService(mockk(relaxed = true))
            .setCustomResourceProvider(provider)
            .build()

        assertThat(GiniCapture.getInstance().customResourceProvider).isSameInstanceAs(provider)
        assertThat(GiniColorResolver.current()?.isActive).isTrue()
    }

    @Test
    fun `a new instance uses the new provider and no cached color of the old one`() {
        GiniCapture.newInstance(context)
            .setGiniCaptureNetworkService(mockk(relaxed = true))
            .setCustomResourceProvider { _, _ -> OLD_COLOR }
            .build()
        val oldColor = GiniColorResolver.current()!!.color(context, R.color.gc_accent_01)

        GiniCapture.newInstance(context)
            .setGiniCaptureNetworkService(mockk(relaxed = true))
            .setCustomResourceProvider { _, _ -> NEW_COLOR }
            .build()
        val newColor = GiniColorResolver.current()!!.color(context, R.color.gc_accent_01)

        assertThat(oldColor).isEqualTo(OLD_COLOR)
        assertThat(newColor).isEqualTo(NEW_COLOR)
    }

    private companion object {
        const val OLD_COLOR = 0xFF111111.toInt()
        const val NEW_COLOR = 0xFF222222.toInt()
    }
}
