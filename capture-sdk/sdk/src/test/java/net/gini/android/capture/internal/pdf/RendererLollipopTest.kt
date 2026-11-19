package net.gini.android.capture.internal.pdf

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import net.gini.android.capture.internal.util.Size
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Covers how [RendererLollipop] deals with a pdf that cannot be read. Rendering an actual pdf
 * needs the native PdfRenderer, which is not available in JVM unit tests.
 */
@RunWith(AndroidJUnit4::class)
class RendererLollipopTest {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private val renderer: RendererLollipop
        get() = RendererLollipop(Uri.fromFile(File(context.cacheDir, "missing.pdf")), context)

    @Test
    fun `renders no bitmap when the pdf cannot be read`() {
        assertThat(renderer.toBitmap(Size(PREVIEW_SIZE, PREVIEW_SIZE))).isNull()
    }

    @Test
    fun `reports no pages when the pdf cannot be read`() {
        assertThat(renderer.pageCount).isEqualTo(0)
    }

    @Test
    fun `does not report a pdf that cannot be read as password protected`() {
        assertThat(renderer.isPdfPasswordProtected).isFalse()
    }

    private companion object {
        const val PREVIEW_SIZE = 100
    }
}
