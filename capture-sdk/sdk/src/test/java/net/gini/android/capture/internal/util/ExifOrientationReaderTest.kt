package net.gini.android.capture.internal.util

import android.content.ContentResolver
import android.content.Context
import android.media.ExifInterface
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File

/**
 * Unit tests for [ExifOrientationReader].
 *
 * Reading a real orientation tag out of a HEIF container needs the platform
 * decoder, so that is covered by the instrumented test. What is pinned here is
 * the degree mapping — it has to agree with
 * [net.gini.android.capture.internal.camera.photo.ExifReader], or a HEIC and a
 * JPEG of the same photo would be rotated differently.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ExifOrientationReaderTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `maps the rotated orientations to clockwise degrees`() {
        assertThat(ExifOrientationReader.toRotationForDisplay(ExifInterface.ORIENTATION_ROTATE_90))
            .isEqualTo(90)
        assertThat(ExifOrientationReader.toRotationForDisplay(ExifInterface.ORIENTATION_ROTATE_180))
            .isEqualTo(180)
        assertThat(ExifOrientationReader.toRotationForDisplay(ExifInterface.ORIENTATION_ROTATE_270))
            .isEqualTo(270)
    }

    @Test
    fun `maps a normal or absent orientation to no rotation`() {
        assertThat(ExifOrientationReader.toRotationForDisplay(ExifInterface.ORIENTATION_NORMAL))
            .isEqualTo(0)
        assertThat(ExifOrientationReader.toRotationForDisplay(ExifInterface.ORIENTATION_UNDEFINED))
            .isEqualTo(0)
    }

    @Test
    fun `treats mirrored orientations as their unmirrored equivalent`() {
        assertThat(ExifOrientationReader.toRotationForDisplay(ExifInterface.ORIENTATION_FLIP_HORIZONTAL))
            .isEqualTo(0)
        assertThat(ExifOrientationReader.toRotationForDisplay(ExifInterface.ORIENTATION_TRANSPOSE))
            .isEqualTo(0)
    }

    @Test
    fun `returns no rotation for a uri that cannot be opened`() {
        val missing = Uri.fromFile(File(context.cacheDir, "gini-does-not-exist.heic"))

        assertThat(ExifOrientationReader.readRotationForDisplay(missing, context)).isEqualTo(0)
    }

    @Test
    fun `returns no rotation for a file without exif data`() {
        val file = File.createTempFile("gini-exif-orientation-test", ".heic", context.cacheDir)
        file.writeBytes(ByteArray(64))

        try {
            assertThat(ExifOrientationReader.readRotationForDisplay(Uri.fromFile(file), context))
                .isEqualTo(0)
        } finally {
            file.delete()
        }
    }

    @Test
    fun `returns no rotation for a uri that yields no stream`() {
        val uri = Uri.parse("content://net.gini.android.capture.test/no-stream.heic")
        // Robolectric's shadow resolver never answers null, so this needs a stand-in
        val nullResolver = mockk<ContentResolver> { every { openInputStream(uri) } returns null }
        val nullContext = mockk<Context> { every { contentResolver } returns nullResolver }

        assertThat(ExifOrientationReader.readRotationForDisplay(uri, nullContext)).isEqualTo(0)
    }

    /**
     * A Uri the app may not read answers "no rotation" instead of throwing: the import pipeline
     * rejects such a Uri with a proper validation error, which a crash here would pre-empt.
     */
    @Test
    fun `returns no rotation for a uri the app is not permitted to read`() {
        val uri = Uri.parse("content://net.gini.android.capture.test/forbidden.heic")
        shadowOf(context.contentResolver).registerInputStreamSupplier(uri) {
            throw SecurityException("no permission")
        }

        assertThat(ExifOrientationReader.readRotationForDisplay(uri, context)).isEqualTo(0)
    }

    /** Before Android 7 the platform reader cannot read from a stream, and there is no HEIC anyway. */
    @Test
    @Config(sdk = [23])
    fun `returns no rotation before Android 7 without touching the uri`() {
        val uri = Uri.parse("content://net.gini.android.capture.test/never-opened.heic")
        shadowOf(context.contentResolver).registerInputStreamSupplier(uri) {
            throw AssertionError("the uri must not be opened before Android 7")
        }

        assertThat(ExifOrientationReader.readRotationForDisplay(uri, context)).isEqualTo(0)
    }
}
