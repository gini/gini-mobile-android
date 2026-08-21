package net.gini.android.capture

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for the credit note hint SDK flag round-trip through [GiniCapture.Builder]:
 * [GiniCapture.Builder.setCreditNoteHintEnabled] and [GiniCapture.isCreditNoteHintEnabled].
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class GiniCaptureCreditNoteHintTest {

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
    fun `credit note hint is enabled by default`() {
        GiniCapture.newInstance(context)
            .setGiniCaptureNetworkService(mockk())
            .build()

        assertThat(GiniCapture.getInstance().isCreditNoteHintEnabled).isTrue()
    }

    @Test
    fun `credit note hint can be disabled through the builder`() {
        GiniCapture.newInstance(context)
            .setGiniCaptureNetworkService(mockk())
            .setCreditNoteHintEnabled(false)
            .build()

        assertThat(GiniCapture.getInstance().isCreditNoteHintEnabled).isFalse()
    }

    @Test
    fun `credit note hint can be enabled explicitly through the builder`() {
        GiniCapture.newInstance(context)
            .setGiniCaptureNetworkService(mockk())
            .setCreditNoteHintEnabled(true)
            .build()

        assertThat(GiniCapture.getInstance().isCreditNoteHintEnabled).isTrue()
    }
}
